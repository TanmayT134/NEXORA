package com.nexora.nexora_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdaptiveInterviewAiResponse {
    private String action;
    private String questionText;
    private String questionType;
    private String skillName;
    private String evaluationCriteria;
    private String rationale;
}
