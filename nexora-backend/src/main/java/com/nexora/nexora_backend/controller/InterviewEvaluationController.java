
package com.nexora.nexora_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import com.nexora.nexora_backend.dto.InterviewEvaluationResponse;
import com.nexora.nexora_backend.service.InterviewEvaluationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/candidate/interview-sessions")
@RequiredArgsConstructor
public class InterviewEvaluationController {

    private final InterviewEvaluationService evaluationService;

    // EVALUATE COMPLETED INTERVIEW
    @PostMapping("/{sessionId}/evaluate")
    public ResponseEntity<InterviewEvaluationResponse> evaluateInterview(
            @PathVariable Long sessionId,
            Authentication authentication) {

        InterviewEvaluationResponse response =
                evaluationService.evaluateInterview(
                        authentication.getName(),
                        sessionId
                );

        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{sessionId}/evaluation")
    public ResponseEntity<InterviewEvaluationResponse> getEvaluation(
            @PathVariable Long sessionId,
            Authentication authentication) {

        InterviewEvaluationResponse response =
                evaluationService.getEvaluation(
                        authentication.getName(),
                        sessionId
                );

        return ResponseEntity.ok(response);
    }
}
