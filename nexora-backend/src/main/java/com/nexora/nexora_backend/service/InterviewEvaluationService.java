
package com.nexora.nexora_backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import com.nexora.nexora_backend.dto.InterviewEvaluationAiResponse;
import com.nexora.nexora_backend.dto.InterviewEvaluationResponse;
import com.nexora.nexora_backend.dto.QuestionEvaluationResponse;
import com.nexora.nexora_backend.entity.InterviewAnswer;
import com.nexora.nexora_backend.entity.InterviewEvaluation;
import com.nexora.nexora_backend.entity.InterviewQuestion;
import com.nexora.nexora_backend.entity.InterviewSession;
import com.nexora.nexora_backend.entity.InterviewSessionStatus;
import com.nexora.nexora_backend.entity.QuestionEvaluation;
import com.nexora.nexora_backend.repository.InterviewAnswerRepository;
import com.nexora.nexora_backend.repository.InterviewQuestionRepository;
import com.nexora.nexora_backend.repository.InterviewSessionRepository;
import com.nexora.nexora_backend.repository.QuestionEvaluationRepository;
import com.nexora.nexora_backend.repository.UserRepository;
import com.nexora.nexora_backend.entity.EvaluationStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InterviewEvaluationService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final QuestionEvaluationRepository questionEvaluationRepository;
    private final UserRepository userRepository;

    private final InterviewEvaluationAiService evaluationAiService;
    private final InterviewEvaluationPersistenceService persistenceService;

    // EVALUATE COMPLETED INTERVIEW
    public InterviewEvaluationResponse evaluateInterview(
            String email,
            Long sessionId) {

        // 1. Validate authenticated user
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated user not found"
            );
        }

        var user = userRepository.findByEmail(
                email.trim().toLowerCase(java.util.Locale.ROOT)
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found"
        ));

        // 2. Retrieve session and verify ownership
        InterviewSession session = sessionRepository
                .findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Interview session not found"
                ));

        // 3. Only completed sessions can be evaluated
        if (session.getStatus() != InterviewSessionStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only completed interview sessions can be evaluated"
            );
        }

        // 4. Retrieve all interview questions
        List<InterviewQuestion> questions =
                questionRepository.findAllBySessionIdOrderBySequenceNumberAsc(
                        sessionId
                );

        if (questions.isEmpty()) {
            String explanation = buildNoEvaluationExplanation(session, 0);
            InterviewEvaluation notGenerated = persistenceService
                    .saveNotGeneratedEvaluation(session, explanation);
            return mapToResponse(notGenerated, List.of(), List.of(), explanation);
        }

        // 5. Collect only questions with a successfully submitted, non-empty answer.
        // Early termination (for example, an integrity review) can leave later
        // questions unanswered. Those questions must not block evaluation or be
        // sent to the AI as if the candidate had attempted them.
        Map<Long, String> answers = new HashMap<>();
        List<InterviewQuestion> answeredQuestions = new java.util.ArrayList<>();

        for (InterviewQuestion question : questions) {
            var answerOptional = answerRepository.findByQuestionId(question.getId());
            if (answerOptional.isEmpty()) {
                continue;
            }

            InterviewAnswer answer = answerOptional.get();
            if (answer.getAnswerText() == null || answer.getAnswerText().isBlank()) {
                continue;
            }

            answers.put(question.getId(), answer.getAnswerText());
            answeredQuestions.add(question);
        }

        if (answeredQuestions.isEmpty()) {
            String explanation = buildNoEvaluationExplanation(session, questions.size());
            InterviewEvaluation notGenerated = persistenceService
                    .saveNotGeneratedEvaluation(session, explanation);
            return mapToResponse(notGenerated, List.of(), List.of(), explanation);
        }

        // 6. Create evaluation record
        InterviewEvaluation evaluation =
                persistenceService.createEvaluation(session);

        try {

            // 7. Send answers and questions to Gemini
            InterviewEvaluationAiResponse aiResponse =
                    evaluationAiService.evaluateInterview(
                            session,
                            answeredQuestions,
                            answers
                    );

            // 8. Persist scores for attempted questions only.
            evaluation = persistenceService.saveCompletedEvaluation(
                    evaluation.getId(),
                    aiResponse,
                    answeredQuestions
            );

        } catch (RuntimeException exception) {

            // 9. Mark evaluation as failed if processing fails
            try {
                persistenceService.markEvaluationFailed(
                        evaluation.getId()
                );
            } catch (RuntimeException failureException) {
                exception.addSuppressed(failureException);
            }

            throw exception;
        }

        // 10. Retrieve and return completed evaluation
        List<QuestionEvaluation> questionEvaluations =
                questionEvaluationRepository
                        .findAllByEvaluationIdOrderByIdAsc(evaluation.getId());

        return mapToResponse(
                evaluation,
                questionEvaluations,
                answeredQuestions,
                "Interview evaluated successfully. " + answeredQuestions.size()
                        + " of " + questions.size() + " generated questions were evaluated."
        );
    }

    // MAP EVALUATION ENTITY TO RESPONSE DTO
    private InterviewEvaluationResponse mapToResponse(
            InterviewEvaluation evaluation,
            List<QuestionEvaluation> questionEvaluations,
            List<InterviewQuestion> questions,
            String message) {

        // Map question IDs to their sequence numbers
        Map<Long, Integer> sequenceMap = new HashMap<>();

        for (InterviewQuestion question : questions) {
            sequenceMap.put(
                    question.getId(),
                    question.getSequenceNumber()
            );
        }

        // Convert question evaluations into response DTOs
        List<QuestionEvaluationResponse> questionResponses =
                questionEvaluations.stream()
                        .map(questionEvaluation -> {

                            Long questionId =
                                    questionEvaluation.getQuestion().getId();

                            return new QuestionEvaluationResponse(
                                    questionEvaluation.getId(),
                                    questionId,
                                    sequenceMap.get(questionId),
                                    questionEvaluation.getScore(),
                                    questionEvaluation.getRating(),
                                    questionEvaluation.getFeedback(),
                                    questionEvaluation.getStrengths(),
                                    questionEvaluation.getImprovementAreas(),
                                    questionEvaluation.getExpectedConcepts()
                            );
                        })
                        .toList();

        return new InterviewEvaluationResponse(
                evaluation.getId(),
                evaluation.getSession().getId(),
                evaluation.getStatus(),
                evaluation.getOverallScore(),
                evaluation.getOverallRating(),
                evaluation.getOverallFeedback(),
                evaluation.getStrengths(),
                evaluation.getImprovementAreas(),
                evaluation.getRecommendations(),
                evaluation.getEvaluatedAt(),
                questionResponses,
                message
        );
    }
    
    @Transactional
    public InterviewEvaluationResponse getEvaluation(
            String email, Long sessionId) {

        var user = userRepository.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        InterviewSession session = sessionRepository
                .findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Interview session not found"));

        List<InterviewQuestion> questions = questionRepository
                .findAllBySessionIdOrderBySequenceNumberAsc(sessionId);

        if (session.getStatus() != InterviewSessionStatus.COMPLETED
                && session.getStatus() != InterviewSessionStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The interview has not been submitted yet. Submit or finish it before opening the outcome report.");
        }

        InterviewEvaluation evaluation = persistenceService
                .findOptionalEvaluationBySessionId(session.getId())
                .orElse(null);

        if (evaluation == null) {
            List<InterviewQuestion> answeredQuestions = new java.util.ArrayList<>();
            for (InterviewQuestion question : questions) {
                var answer = answerRepository.findByQuestionId(question.getId());
                if (answer.isPresent() && answer.get().getAnswerText() != null
                        && !answer.get().getAnswerText().isBlank()) {
                    answeredQuestions.add(question);
                }
            }

            if (answeredQuestions.isEmpty()) {
                String explanation = buildNoEvaluationExplanation(session, questions.size());
                evaluation = persistenceService.saveNotGeneratedEvaluation(session, explanation);
            } else {
                evaluation = persistenceService.createPendingEvaluation(session);
            }
        }

        List<QuestionEvaluation> questionEvaluations = questionEvaluationRepository
                .findAllByEvaluationIdOrderByIdAsc(evaluation.getId());

        Map<Long, Integer> sequenceMap = new HashMap<>();
        for (InterviewQuestion question : questions) {
            sequenceMap.put(question.getId(), question.getSequenceNumber());
        }

        List<QuestionEvaluationResponse> questionResponses = questionEvaluations.stream()
                .map(item -> new QuestionEvaluationResponse(
                        item.getId(), item.getQuestion().getId(),
                        sequenceMap.get(item.getQuestion().getId()),
                        item.getScore(), item.getRating(), item.getFeedback(),
                        item.getStrengths(), item.getImprovementAreas(),
                        item.getExpectedConcepts()))
                .toList();

        String message = evaluation.getStatus() == EvaluationStatus.COMPLETED
                ? "Interview evaluation retrieved successfully"
                : evaluation.getStatus() == EvaluationStatus.NOT_GENERATED
                    ? evaluation.getOverallFeedback()
                    : evaluation.getStatus() == EvaluationStatus.PENDING
                        ? "Evaluation is ready to be generated from submitted answers"
                        : "Interview evaluation status: " + evaluation.getStatus();

        return new InterviewEvaluationResponse(
                evaluation.getId(), session.getId(), evaluation.getStatus(),
                evaluation.getOverallScore(), evaluation.getOverallRating(),
                evaluation.getOverallFeedback(), evaluation.getStrengths(),
                evaluation.getImprovementAreas(), evaluation.getRecommendations(),
                evaluation.getEvaluatedAt(), questionResponses, message);
    }

    private String buildNoEvaluationExplanation(InterviewSession session, int totalQuestions) {
        String reason = session.getTerminationReason();
        String details = session.getTerminationDetails();
        StringBuilder message = new StringBuilder(
                "Evaluation was not generated because this interview was submitted before any answer was recorded.");
        if (reason != null && !reason.isBlank()) {
            message.append(" Submission reason: ").append(reason.replace('_', ' ')).append('.');
        }
        if (details != null && !details.isBlank()) {
            message.append(" ").append(details.trim());
        }
        if (totalQuestions == 0) {
            message.append(" No interview questions had been generated.");
        }
        message.append(" No performance score has been assigned.");
        return message.toString();
    }

}
