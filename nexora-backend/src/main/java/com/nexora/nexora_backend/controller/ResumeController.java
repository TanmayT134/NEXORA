package com.nexora.nexora_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nexora.nexora_backend.dto.ResumeUploadResponse;
import com.nexora.nexora_backend.service.ResumeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/candidate/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping(
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ResumeUploadResponse> uploadResume(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        ResumeUploadResponse response =
                resumeService.uploadResume(
                        authentication.getName(),
                        file
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}