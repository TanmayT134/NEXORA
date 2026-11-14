package com.nexora.nexora_backend.dto;

import java.math.BigDecimal;

import com.nexora.nexora_backend.entity.EvaluationRating;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionEvaluationResponse {

    private Long id;

    private Long questionId;

    private Integer sequenceNumber;

    private BigDecimal score;

    private EvaluationRating rating;

    private String feedback;

    private String strengths;

    private String improvementAreas;

    private String expectedConcepts;
}