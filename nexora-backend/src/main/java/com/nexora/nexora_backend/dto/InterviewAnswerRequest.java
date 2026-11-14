package com.nexora.nexora_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class InterviewAnswerRequest {

    @NotBlank(message = "Answer cannot be empty")
    @Size(max = 10000, message = "Answer cannot exceed 10000 characters")
    private String answerText;
}