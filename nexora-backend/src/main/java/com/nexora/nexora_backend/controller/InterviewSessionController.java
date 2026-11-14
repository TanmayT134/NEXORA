package com.nexora.nexora_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.nexora.nexora_backend.dto.CreateInterviewSessionRequest;
import com.nexora.nexora_backend.dto.InterviewSessionResponse;
import com.nexora.nexora_backend.service.InterviewSessionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/candidate/interview-sessions")
@RequiredArgsConstructor
public class InterviewSessionController {

    private final InterviewSessionService sessionService;

    // CREATE INTERVIEW SESSION
    @PostMapping
    public ResponseEntity<InterviewSessionResponse> createSession(
            Authentication authentication,
            @Valid @RequestBody CreateInterviewSessionRequest request) {

        InterviewSessionResponse response =
                sessionService.createSession(
                        authentication.getName(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // GET ALL INTERVIEW SESSIONS
    @GetMapping
    public ResponseEntity<List<InterviewSessionResponse>> getAllSessions(
            Authentication authentication) {

        List<InterviewSessionResponse> response =
                sessionService.getAllSessions(
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }


    // PERMANENTLY CLEAR ALL INTERVIEW HISTORY FOR THE AUTHENTICATED CANDIDATE
    @DeleteMapping("/history")
    public ResponseEntity<java.util.Map<String, Object>> clearHistory(Authentication authentication) {
        int deleted = sessionService.clearAllSessions(authentication.getName());
        return ResponseEntity.ok(java.util.Map.of(
                "deletedCount", deleted,
                "message", "Interview history cleared successfully"
        ));
    }

    // GET INTERVIEW SESSION BY ID
    @GetMapping("/{sessionId}")
    public ResponseEntity<InterviewSessionResponse> getSessionById(
            Authentication authentication,
            @PathVariable Long sessionId) {

        InterviewSessionResponse response =
                sessionService.getSessionById(
                        authentication.getName(),
                        sessionId
                );

        return ResponseEntity.ok(response);
    }

    // START INTERVIEW
    @PatchMapping("/{sessionId}/start")
    public ResponseEntity<InterviewSessionResponse> startSession(
            Authentication authentication,
            @PathVariable Long sessionId) {

        InterviewSessionResponse response =
                sessionService.startSession(
                        authentication.getName(),
                        sessionId
                );

        return ResponseEntity.ok(response);
    }

    // COMPLETE INTERVIEW
    @PatchMapping("/{sessionId}/complete")
    public ResponseEntity<InterviewSessionResponse> completeSession(
            Authentication authentication,
            @PathVariable Long sessionId) {

        InterviewSessionResponse response =
                sessionService.completeSession(
                        authentication.getName(),
                        sessionId
                );

        return ResponseEntity.ok(response);
    }

    // CANCEL INTERVIEW
    @PatchMapping("/{sessionId}/cancel")
    public ResponseEntity<InterviewSessionResponse> cancelSession(
            Authentication authentication,
            @PathVariable Long sessionId) {

        InterviewSessionResponse response =
                sessionService.cancelSession(
                        authentication.getName(),
                        sessionId
                );

        return ResponseEntity.ok(response);
    }
}