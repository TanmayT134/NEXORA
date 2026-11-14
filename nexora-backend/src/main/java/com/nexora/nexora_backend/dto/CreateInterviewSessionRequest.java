package com.nexora.nexora_backend.dto;

import com.nexora.nexora_backend.entity.InterviewSessionMode;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateInterviewSessionRequest {

    @NotNull(message = "Interview configuration ID is required")
    @Positive(message = "Interview configuration ID must be positive")
    private Long configurationId;

    // Defaults to TEXT for older clients that do not send a mode.
    private InterviewSessionMode mode = InterviewSessionMode.TEXT;
}