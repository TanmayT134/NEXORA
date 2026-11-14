package com.nexora.nexora_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.nexora.nexora_backend.dto.InterviewAnswerRequest;
import com.nexora.nexora_backend.dto.InterviewAnswerResponse;
import com.nexora.nexora_backend.service.InterviewAnswerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/candidate/interview-questions")
@RequiredArgsConstructor
public class InterviewAnswerController {

    private final InterviewAnswerService answerService;

    // SUBMIT ANSWER
    @PostMapping("/{questionId}/answer")
    public ResponseEntity<InterviewAnswerResponse> submitAnswer(
            Authentication authentication,
            @PathVariable Long questionId,
            @Valid @RequestBody InterviewAnswerRequest request) {

        InterviewAnswerResponse response =
                answerService.submitAnswer(
                        authentication.getName(),
                        questionId,
                        request
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET ANSWER
    @GetMapping("/{questionId}/answer")
    public ResponseEntity<InterviewAnswerResponse> getAnswer(
            Authentication authentication,
            @PathVariable Long questionId) {

        InterviewAnswerResponse response =
                answerService.getAnswer(
                        authentication.getName(),
                        questionId
                );

        return ResponseEntity.ok(response);
    }

    // UPDATE ANSWER
    @PutMapping("/{questionId}/answer")
    public ResponseEntity<InterviewAnswerResponse> updateAnswer(
            Authentication authentication,
            @PathVariable Long questionId,
            @Valid @RequestBody InterviewAnswerRequest request) {

        InterviewAnswerResponse response =
                answerService.updateAnswer(
                        authentication.getName(),
                        questionId,
                        request
                );

        return ResponseEntity.ok(response);
    }
}