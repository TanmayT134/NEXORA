package com.nexora.nexora_backend.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.nexora.nexora_backend.dto.IntegrityEventRequest;
import com.nexora.nexora_backend.service.InterviewIntegrityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
@RestController @RequestMapping("/api/v1/candidate/interview-sessions") @RequiredArgsConstructor
public class InterviewIntegrityController {
    private final InterviewIntegrityService service;
    @GetMapping("/{sessionId}/integrity-events")
    public ResponseEntity<?> getEvents(Authentication auth, @PathVariable Long sessionId) {
        return ResponseEntity.ok(service.getEvents(auth.getName(), sessionId));
    }
    @PostMapping("/{sessionId}/integrity-events")
    public ResponseEntity<?> record(Authentication auth, @PathVariable Long sessionId,
            @Valid @RequestBody IntegrityEventRequest request) {
        return ResponseEntity.ok(service.record(auth.getName(), sessionId, request));
    }
}
