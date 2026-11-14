package com.nexora.nexora_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexora.nexora_backend.dto.AdaptiveInterviewTurnResponse;
import com.nexora.nexora_backend.service.AdaptiveInterviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/candidate/interview-sessions")
@RequiredArgsConstructor
public class AdaptiveInterviewController {

    private final AdaptiveInterviewService adaptiveInterviewService;

    /** Call after submitting the current question's answer. */
    @PostMapping("/{sessionId}/next-question")
    public ResponseEntity<AdaptiveInterviewTurnResponse> getNextQuestion(
            Authentication authentication,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(adaptiveInterviewService.generateNextTurn(
                authentication.getName(), sessionId));
    }
}
