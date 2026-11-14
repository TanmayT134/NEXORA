package com.nexora.nexora_backend.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterviewEvaluationAiResponse {

    private List<EvaluatedQuestion> questionEvaluations;

    private String overallFeedback;

    private String strengths;

    private String improvementAreas;

    private String recommendations;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EvaluatedQuestion {

        private Long questionId;

        private BigDecimal score;

        private String rating;

        private String feedback;

        private String strengths;

        private String improvementAreas;

        private String expectedConcepts;
    }
}