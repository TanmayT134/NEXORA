
package com.nexora.nexora_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "resume_skills",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_resume_analysis_skill",
            columnNames = {"resume_analysis_id", "skill_name"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ResumeSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "resume_analysis_id",
        nullable = false
    )
    private ResumeAnalysis resumeAnalysis;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private SkillCategory category;

    @Column(name = "proficiency_level", length = 30)
    private String proficiencyLevel;

    @Column(name = "years_of_experience")
    private Double yearsOfExperience;
}
