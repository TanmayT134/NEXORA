package com.nexora.nexora_backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewAnswerResponse {

    private Long id;
    private Long questionId;
    private String answerText;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
    private String message;
}