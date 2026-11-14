package com.nexora.nexora_backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "interview_answers",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_interview_answer_question",
            columnNames = {"question_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class InterviewAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Question being answered
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private InterviewQuestion question;

    // Candidate's submitted answer
    @Column(name = "answer_text", columnDefinition = "LONGTEXT")
    private String answerText;

    // Time when the candidate submits the answer
    private LocalDateTime submittedAt;

    // Time when the answer was last modified
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}