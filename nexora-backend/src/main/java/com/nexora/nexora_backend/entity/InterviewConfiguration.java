package com.nexora.nexora_backend.entity;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "interview_configurations")
@Getter
@Setter
@NoArgsConstructor
public class InterviewConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Candidate who owns this configuration
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Optional resume used as preparation context
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_analysis_id")
    private ResumeAnalysis resumeAnalysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type", nullable = false, length = 20)
    private InterviewType interviewType;

    @Column(name = "target_role", nullable = false, length = 150)
    private String targetRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", nullable = false, length = 30)
    private InterviewExperienceLevel experienceLevel;

    @Column(name = "number_of_questions", nullable = false)
    private Integer numberOfQuestions;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    // Skills selected by the candidate
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "interview_configuration_skills",
        joinColumns = @JoinColumn(name = "configuration_id"),
        uniqueConstraints = @UniqueConstraint(
            columnNames = {"configuration_id", "skill_name"}
        )
    )
    @Column(name = "skill_name", nullable = false, length = 100)
    private Set<String> selectedSkills = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}