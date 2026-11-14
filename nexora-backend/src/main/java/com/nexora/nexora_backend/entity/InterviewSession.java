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
@Table(name = "interview_sessions")
@Getter
@Setter
@NoArgsConstructor
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Candidate who owns this interview session
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ID of the configuration used to create this session.
    // Stored as a reference value, not a foreign-key relationship.
    @Column(name = "source_configuration_id")
    private Long sourceConfigurationId;

    // Configuration snapshot
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

    // Session details
    @Enumerated(EnumType.STRING)
    @Column(name = "session_status", nullable = false, length = 20)
    private InterviewSessionStatus status = InterviewSessionStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_mode", nullable = false, length = 10)
    private InterviewSessionMode mode = InterviewSessionMode.TEXT;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Immutable resume context captured when this interview session is created.
    @Column(name = "resume_context_snapshot", columnDefinition = "LONGTEXT")
    private String resumeContextSnapshot;

    @Column(name = "termination_reason", length = 40)
    private String terminationReason;

    @Column(name = "termination_details", columnDefinition = "TEXT")
    private String terminationDetails;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "interview_session_skills",
        joinColumns = @JoinColumn(name = "session_id"),
        uniqueConstraints = @UniqueConstraint(
            columnNames = {"session_id", "skill_name"}
        )
    )
    @Column(name = "skill_name", nullable = false, length = 100)
    private Set<String> selectedSkills = new LinkedHashSet<>();

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