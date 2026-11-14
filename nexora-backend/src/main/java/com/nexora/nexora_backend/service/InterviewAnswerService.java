package com.nexora.nexora_backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nexora.nexora_backend.dto.InterviewAnswerRequest;
import com.nexora.nexora_backend.dto.InterviewAnswerResponse;
import com.nexora.nexora_backend.entity.InterviewAnswer;
import com.nexora.nexora_backend.entity.InterviewQuestion;
import com.nexora.nexora_backend.entity.InterviewSession;
import com.nexora.nexora_backend.entity.InterviewSessionStatus;
import com.nexora.nexora_backend.repository.InterviewAnswerRepository;
import com.nexora.nexora_backend.repository.InterviewQuestionRepository;
import com.nexora.nexora_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InterviewAnswerService {

    private final InterviewAnswerRepository answerRepository;
    private final InterviewQuestionRepository questionRepository;
    private final UserRepository userRepository;

    // SUBMIT ANSWER
    @Transactional
    public InterviewAnswerResponse submitAnswer(
            String email,
            Long questionId,
            InterviewAnswerRequest request) {

        InterviewQuestion question =
                findQuestionForUser(email, questionId);

        validateSessionIsActive(question.getSession());

        if (answerRepository.existsByQuestionId(questionId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An answer has already been submitted for this question"
            );
        }

        InterviewAnswer answer = new InterviewAnswer();
        answer.setQuestion(question);
        answer.setAnswerText(request.getAnswerText().trim());

        InterviewAnswer savedAnswer =
                answerRepository.save(answer);

        return mapToResponse(
                savedAnswer,
                "Answer submitted successfully"
        );
    }

    // GET ANSWER
    @Transactional(readOnly = true)
    public InterviewAnswerResponse getAnswer(
            String email,
            Long questionId) {

        InterviewQuestion question =
                findQuestionForUser(email, questionId);

        InterviewAnswer answer =
                answerRepository.findByQuestionId(questionId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Answer not found"
                        ));

        return mapToResponse(
                answer,
                "Answer retrieved successfully"
        );
    }

    // UPDATE ANSWER
    @Transactional
    public InterviewAnswerResponse updateAnswer(
            String email,
            Long questionId,
            InterviewAnswerRequest request) {

        InterviewQuestion question =
                findQuestionForUser(email, questionId);

        validateSessionIsActive(question.getSession());

        InterviewAnswer answer =
                answerRepository.findByQuestionId(questionId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Answer not found"
                        ));

        answer.setAnswerText(request.getAnswerText().trim());

        InterviewAnswer updatedAnswer =
                answerRepository.save(answer);

        return mapToResponse(
                updatedAnswer,
                "Answer updated successfully"
        );
    }

    // FIND QUESTION AND VERIFY OWNERSHIP
    private InterviewQuestion findQuestionForUser(
            String email,
            Long questionId) {

        var user = userRepository.findByEmail(
                email.trim().toLowerCase(java.util.Locale.ROOT)
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found"
        ));

        InterviewQuestion question =
                questionRepository.findById(questionId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Interview question not found"
                        ));

        InterviewSession session = question.getSession();

        if (!session.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Interview question not found"
            );
        }

        return question;
    }

    // VALIDATE INTERVIEW SESSION STATUS
    private void validateSessionIsActive(InterviewSession session) {

        if (session.getStatus() != InterviewSessionStatus.IN_PROGRESS) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Answers can only be submitted or updated during an active interview"
            );
        }
    }

    // MAP ENTITY TO RESPONSE DTO
    private InterviewAnswerResponse mapToResponse(
            InterviewAnswer answer,
            String message) {

        return new InterviewAnswerResponse(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.getAnswerText(),
                answer.getSubmittedAt(),
                answer.getUpdatedAt(),
                message
        );
    }
}