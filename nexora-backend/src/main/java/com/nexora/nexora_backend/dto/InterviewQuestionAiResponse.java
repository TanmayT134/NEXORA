package com.nexora.nexora_backend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterviewQuestionAiResponse {

    private List<GeneratedQuestion> questions;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeneratedQuestion {

        private String questionText;

        private String questionType;

        private String skillName;

        private String evaluationCriteria;
    }
}