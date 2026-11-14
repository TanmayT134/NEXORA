package com.nexora.nexora_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.nexora.nexora_backend.entity.EvaluationRating;
import com.nexora.nexora_backend.entity.EvaluationStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewEvaluationResponse {

    private Long id;

    private Long sessionId;

    private EvaluationStatus status;

    private BigDecimal overallScore;

    private EvaluationRating overallRating;

    private String overallFeedback;

    private String strengths;

    private String improvementAreas;

    private String recommendations;

    private LocalDateTime evaluatedAt;

    private List<QuestionEvaluationResponse> questionEvaluations;

    private String message;
}