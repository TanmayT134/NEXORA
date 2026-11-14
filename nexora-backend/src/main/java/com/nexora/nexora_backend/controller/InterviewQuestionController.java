package com.nexora.nexora_backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.nexora.nexora_backend.dto.InterviewQuestionResponse;
import com.nexora.nexora_backend.service.InterviewQuestionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/candidate/interview-sessions")
@RequiredArgsConstructor
public class InterviewQuestionController {

    private final InterviewQuestionService questionService;

    // GET QUESTIONS FOR AN INTERVIEW SESSION
    @GetMapping("/{sessionId}/questions")
    public ResponseEntity<List<InterviewQuestionResponse>> getSessionQuestions(
            Authentication authentication,
            @PathVariable Long sessionId) {

        List<InterviewQuestionResponse> questions =
                questionService.getSessionQuestions(
                        authentication.getName(),
                        sessionId
                );

        return ResponseEntity.ok(questions);
    }
}