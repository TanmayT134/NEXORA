
package com.nexora.nexora_backend.entity;

import java.time.LocalDate;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resume_projects")
@Getter
@Setter
@NoArgsConstructor
public class ResumeProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "resume_analysis_id",
        nullable = false
    )
    private ResumeAnalysis resumeAnalysis;

    @Column(name = "project_name", nullable = false, length = 200)
    private String projectName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "project_role", length = 150)
    private String projectRole;

    @Column(name = "technologies_used", columnDefinition = "TEXT")
    private String technologiesUsed;

    @Column(name = "project_url", length = 500)
    private String projectUrl;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "start_date_precision", length = 10)
    private DatePrecision startDatePrecision;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_date_precision", length = 10)
    private DatePrecision endDatePrecision;
}
