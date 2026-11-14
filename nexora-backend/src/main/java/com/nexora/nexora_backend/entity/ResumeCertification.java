
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
@Table(name = "resume_certifications")
@Getter
@Setter
@NoArgsConstructor
public class ResumeCertification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "resume_analysis_id",
        nullable = false
    )
    private ResumeAnalysis resumeAnalysis;

    @Column(name = "certification_name", nullable = false, length = 200)
    private String certificationName;

    @Column(name = "issuing_organization", length = 200)
    private String issuingOrganization;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_date_precision", length = 10)
    private DatePrecision issueDatePrecision;

    @Enumerated(EnumType.STRING)
    @Column(name = "expiry_date_precision", length = 10)
    private DatePrecision expiryDatePrecision;

    @Column(name = "credential_id", length = 150)
    private String credentialId;

    @Column(name = "credential_url", length = 500)
    private String credentialUrl;
}
