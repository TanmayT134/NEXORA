package com.nexora.nexora_backend.dto;

import java.time.LocalDateTime;

import com.nexora.nexora_backend.entity.ResumeStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResumeUploadResponse {

    private Long resumeId;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private ResumeStatus status;
    private LocalDateTime uploadedAt;
    private String message;
}