
package com.nexora.nexora_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.nexora.nexora_backend.entity.DatePrecision;
import com.nexora.nexora_backend.entity.EmploymentType;
import com.nexora.nexora_backend.entity.ResumeStatus;
import com.nexora.nexora_backend.entity.SkillCategory;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResumeAnalysisResponse {

    private Long analysisId;
    private Long resumeId;
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private String professionalSummary;
    private BigDecimal totalExperienceYears;
    private String currentJobTitle;
    private ResumeStatus status;
    private LocalDateTime analyzedAt;

    private List<SkillResponse> skills;
    private List<ExperienceResponse> experiences;
    private List<ProjectResponse> projects;
    private List<EducationResponse> education;
    private List<CertificationResponse> certifications;

    @Getter
    @AllArgsConstructor
    public static class SkillResponse {

        private Long id;
        private String skillName;
        private SkillCategory category;
        private String proficiencyLevel;
        private Double yearsOfExperience;
    }

    @Getter
    @AllArgsConstructor
    public static class ExperienceResponse {

        private Long id;
        private String jobTitle;
        private String companyName;
        private EmploymentType employmentType;

        private LocalDate startDate;
        private DatePrecision startDatePrecision;

        private LocalDate endDate;
        private DatePrecision endDatePrecision;

        private Boolean currentlyWorking;
        private String description;
        private String technologiesUsed;
    }

    @Getter
    @AllArgsConstructor
    public static class ProjectResponse {

        private Long id;
        private String projectName;
        private String description;
        private String projectRole;
        private String technologiesUsed;
        private String projectUrl;

        private LocalDate startDate;
        private DatePrecision startDatePrecision;

        private LocalDate endDate;
        private DatePrecision endDatePrecision;
    }

    @Getter
    @AllArgsConstructor
    public static class EducationResponse {

        private Long id;
        private String degree;
        private String fieldOfStudy;
        private String institutionName;
        private Integer graduationYear;
        private String gradeOrCgpa;
        private Integer startYear;
        private Integer endYear;
        private String description;
    }

    @Getter
    @AllArgsConstructor
    public static class CertificationResponse {

        private Long id;
        private String certificationName;
        private String issuingOrganization;

        private LocalDate issueDate;
        private DatePrecision issueDatePrecision;

        private LocalDate expiryDate;
        private DatePrecision expiryDatePrecision;

        private String credentialId;
        private String credentialUrl;
    }
}
