package com.nexora.nexora_backend.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.nexora.nexora_backend.entity.InterviewDifficulty;
import com.nexora.nexora_backend.entity.InterviewExperienceLevel;
import com.nexora.nexora_backend.entity.InterviewSessionMode;
import com.nexora.nexora_backend.entity.InterviewSessionStatus;
import com.nexora.nexora_backend.entity.InterviewType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSessionResponse {

    private Long id;

    private Long userId;

    private Long sourceConfigurationId;

    private InterviewType interviewType;

    private String targetRole;

    private InterviewDifficulty difficulty;

    private InterviewExperienceLevel experienceLevel;

    private Integer numberOfQuestions;

    private Integer durationMinutes;

    private InterviewSessionStatus status;

    private InterviewSessionMode mode;

    // Keep this field immediately after mode
    private Set<String> selectedSkills;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String message;

    private String terminationReason;

    private String terminationDetails;

    private boolean resumeBased;
}