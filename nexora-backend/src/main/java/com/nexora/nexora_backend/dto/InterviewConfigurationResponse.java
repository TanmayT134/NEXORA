package com.nexora.nexora_backend.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.nexora.nexora_backend.entity.InterviewDifficulty;
import com.nexora.nexora_backend.entity.InterviewExperienceLevel;
import com.nexora.nexora_backend.entity.InterviewType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewConfigurationResponse {

    private Long id;

    private Long userId;

    private Long resumeAnalysisId;

    private InterviewType interviewType;

    private String targetRole;

    private InterviewDifficulty difficulty;

    private InterviewExperienceLevel experienceLevel;

    private Integer numberOfQuestions;

    private Integer durationMinutes;

    private Set<String> selectedSkills;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String message;
}