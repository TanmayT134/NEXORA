package com.nexora.nexora_backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nexora.nexora_backend.dto.InterviewQuestionResponse;
import com.nexora.nexora_backend.entity.InterviewQuestion;
import com.nexora.nexora_backend.entity.InterviewSession;
import com.nexora.nexora_backend.repository.InterviewQuestionRepository;
import com.nexora.nexora_backend.repository.InterviewSessionRepository;
import com.nexora.nexora_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InterviewQuestionService {

    private final InterviewQuestionRepository questionRepository;
    private final InterviewSessionRepository sessionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<InterviewQuestionResponse> getSessionQuestions(
            String email,
            Long sessionId) {

        var user = userRepository.findByEmail(
                email.trim().toLowerCase(java.util.Locale.ROOT)
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found"
        ));

        InterviewSession session = sessionRepository
                .findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Interview session not found"
                ));

        if (session.getStatus()
                == com.nexora.nexora_backend.entity.InterviewSessionStatus.CREATED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Start the interview before retrieving questions"
            );
        }

        List<InterviewQuestion> questions =
                questionRepository
                        .findAllBySessionIdOrderBySequenceNumberAsc(
                                sessionId
                        );

        return questions.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private InterviewQuestionResponse mapToResponse(
            InterviewQuestion question) {

        return new InterviewQuestionResponse(
                question.getId(),
                question.getSequenceNumber(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getSkillName(),
                question.getParentQuestion() == null ? null : question.getParentQuestion().getId(),
                question.getFollowUpDepth() == null ? 0 : question.getFollowUpDepth(),
                question.getParentQuestion() != null
        );
    }
}