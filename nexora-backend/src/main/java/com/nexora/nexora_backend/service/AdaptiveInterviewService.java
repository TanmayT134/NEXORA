package com.nexora.nexora_backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.nexora.nexora_backend.dto.AdaptiveInterviewAiResponse;
import com.nexora.nexora_backend.dto.AdaptiveInterviewTurnResponse;
import com.nexora.nexora_backend.dto.InterviewQuestionResponse;
import com.nexora.nexora_backend.entity.InterviewAnswer;
import com.nexora.nexora_backend.entity.InterviewQuestion;
import com.nexora.nexora_backend.entity.InterviewSession;
import com.nexora.nexora_backend.entity.InterviewSessionStatus;
import com.nexora.nexora_backend.repository.InterviewAnswerRepository;
import com.nexora.nexora_backend.repository.InterviewQuestionRepository;
import com.nexora.nexora_backend.repository.InterviewSessionRepository;
import com.nexora.nexora_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdaptiveInterviewService {

    private static final int MAX_FOLLOW_UPS_PER_TOPIC = 2;
    private static final int MAX_CONTEXT_TURNS = 12;

    private final UserRepository userRepository;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final AdaptiveInterviewAiService adaptiveAiService;
    private final InterviewQuestionPersistenceService persistenceService;

    public void createInitialQuestion(InterviewSession session) {
        AdaptiveInterviewAiResponse generated = adaptiveAiService.generateInitialQuestion(session);
        persistenceService.saveAdaptiveQuestion(session, generated, null, 1, 0);
    }

    public AdaptiveInterviewTurnResponse generateNextTurn(String email, Long sessionId) {
        var user = userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview session not found"));

        if (session.getStatus() != InterviewSessionStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only an ongoing interview can request the next question");
        }

        List<InterviewQuestion> questions = questionRepository.findAllBySessionIdOrderBySequenceNumberAsc(sessionId);
        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No question exists for this interview yet");
        }

        InterviewQuestion latest = questions.get(questions.size() - 1);
        if (!answerRepository.existsByQuestionId(latest.getId())) {
            // Idempotent retry: a previous request may already have generated
            // the current unanswered question. Return it instead of creating another.
            if (questions.size() > 1) {
                InterviewQuestion previous = questions.get(questions.size() - 2);
                if (answerRepository.existsByQuestionId(previous.getId())) {
                    return questionTurn(session, latest, countMainQuestions(questions),
                            "Existing next question returned");
                }
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Submit an answer to the current question before requesting the next one");
        }

        int mainQuestionsAsked = countMainQuestions(questions);
        boolean canAskFollowUp = latest.getFollowUpDepth() < MAX_FOLLOW_UPS_PER_TOPIC;
        boolean canAskNewTopic = mainQuestionsAsked < session.getNumberOfQuestions();
        List<Map<String, Object>> conversation = buildConversation(questions);

        AdaptiveInterviewAiResponse decision = adaptiveAiService.decideNextQuestion(
                session, conversation, mainQuestionsAsked, canAskFollowUp, canAskNewTopic);

        if ("END".equals(decision.getAction())
                && mainQuestionsAsked < session.getNumberOfQuestions()
                && (mainQuestionsAsked < 3 || !hasPoorPerformanceEvidence(decision.getRationale()))) {
            // Do not allow early termination before at least three substantive main questions,
            // or without a concrete poor-performance rationale. Audio failures are not evidence.
            decision = adaptiveAiService.generateContinuationQuestion(session, conversation, mainQuestionsAsked);
        }

        if ("END".equals(decision.getAction())) {
            session.setStatus(InterviewSessionStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            boolean limitReached = mainQuestionsAsked >= session.getNumberOfQuestions();
            session.setTerminationReason(limitReached ? "QUESTION_LIMIT_REACHED" : "LOW_PERFORMANCE");
            session.setTerminationDetails(limitReached
                    ? "Configured main-question limit reached."
                    : "The interview ended early after multiple substantive answers showed significant gaps in the required knowledge. Technical or speech-recognition failures are not counted as poor answers.");
            sessionRepository.save(session);
            return new AdaptiveInterviewTurnResponse(
                    "INTERVIEW_FINISHED", sessionId, null, mainQuestionsAsked,
                    session.getNumberOfQuestions(), latest.getFollowUpDepth(),
                    "The AI interviewer has concluded the interview. You can now request your evaluation.");
        }

        boolean followUp = "FOLLOW_UP".equals(decision.getAction());
        Long parentId = followUp ? latest.getId() : null;
        int depth = followUp ? latest.getFollowUpDepth() + 1 : 0;
        int sequence = questions.size() + 1;
        InterviewQuestion saved;
        try {
            saved = persistenceService.saveAdaptiveQuestion(
                    session, decision, parentId, sequence, depth);
        } catch (DataIntegrityViolationException concurrentRequest) {
            // The unique (session_id, sequence_number) constraint is the final
            // guard if two clients request the next turn at almost the same time.
            List<InterviewQuestion> refreshed = questionRepository
                    .findAllBySessionIdOrderBySequenceNumberAsc(sessionId);
            if (!refreshed.isEmpty()) {
                InterviewQuestion current = refreshed.get(refreshed.size() - 1);
                if (!answerRepository.existsByQuestionId(current.getId())) {
                    return questionTurn(session, current, countMainQuestions(refreshed),
                            "The next question was already generated by another request");
                }
            }
            throw concurrentRequest;
        }
        return questionTurn(session, saved, mainQuestionsAsked + (followUp ? 0 : 1),
                followUp ? "A contextual follow-up question has been generated from your previous answer" : "Next interview topic generated");
    }


    private boolean hasPoorPerformanceEvidence(String rationale) {
        if (rationale == null || rationale.trim().length() < 20) return false;
        String value = rationale.toLowerCase(Locale.ROOT);
        return List.of("poor", "weak", "limited", "incorrect", "inadequate", "insufficient",
                "unable", "misunderstood", "fundamental", "lacked").stream().anyMatch(value::contains);
    }

    private List<Map<String, Object>> buildConversation(List<InterviewQuestion> questions) {
        List<Map<String, Object>> turns = new ArrayList<>();
        int start = Math.max(0, questions.size() - MAX_CONTEXT_TURNS);
        for (int i = start; i < questions.size(); i++) {
            InterviewQuestion question = questions.get(i);
            InterviewAnswer answer = answerRepository.findByQuestionId(question.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Every previous question must be answered first"));
            Map<String, Object> turn = new LinkedHashMap<>();
            turn.put("sequence", question.getSequenceNumber());
            turn.put("question", question.getQuestionText());
            turn.put("questionType", question.getQuestionType().name());
            turn.put("skill", question.getSkillName());
            turn.put("followUpDepth", question.getFollowUpDepth());
            turn.put("candidateAnswer", answer.getAnswerText());
            turns.add(turn);
        }
        return turns;
    }

    private int countMainQuestions(List<InterviewQuestion> questions) {
        return (int) questions.stream().filter(question -> question.getParentQuestion() == null).count();
    }

    private AdaptiveInterviewTurnResponse questionTurn(
            InterviewSession session, InterviewQuestion question, int mainCount, String message) {
        Long parentId = question.getParentQuestion() == null ? null : question.getParentQuestion().getId();
        InterviewQuestionResponse response = new InterviewQuestionResponse(
                question.getId(), question.getSequenceNumber(), question.getQuestionText(),
                question.getQuestionType(), question.getSkillName(), parentId,
                question.getFollowUpDepth(), parentId != null);
        return new AdaptiveInterviewTurnResponse(
                "QUESTION", session.getId(), response, mainCount,
                session.getNumberOfQuestions(), question.getFollowUpDepth(), message);
    }

    private String normalize(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
