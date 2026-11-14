package com.nexora.nexora_backend.dto;

import java.util.List;

import com.nexora.nexora_backend.entity.InterviewDifficulty;
import com.nexora.nexora_backend.entity.InterviewExperienceLevel;
import com.nexora.nexora_backend.entity.InterviewType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InterviewConfigurationRequest {

    @NotNull(message = "Interview type is required")
    private InterviewType interviewType;

    @NotBlank(message = "Target role is required")
    @Size(max = 150, message = "Target role cannot exceed 150 characters")
    private String targetRole;

    @NotNull(message = "Difficulty is required")
    private InterviewDifficulty difficulty;

    @NotNull(message = "Experience level is required")
    private InterviewExperienceLevel experienceLevel;

    @NotNull(message = "Number of questions is required")
    @Min(value = 5, message = "Minimum 5 questions are required")
    @Max(value = 30, message = "Maximum 30 questions are allowed")
    private Integer numberOfQuestions;

    @NotNull(message = "Duration is required")
    @Min(value = 10, message = "Minimum duration is 10 minutes")
    @Max(value = 90, message = "Maximum duration is 90 minutes")
    private Integer durationMinutes;

    @NotEmpty(message = "Select at least one skill")
    @Size(max = 15, message = "Maximum 15 skills can be selected")
    private List<
        @NotBlank(message = "Skill name cannot be blank")
        @Size(max = 100, message = "Skill name cannot exceed 100 characters")
        String
    > selectedSkills;

    // Optional: link the configuration to an existing resume analysis
    private Long resumeAnalysisId;
}