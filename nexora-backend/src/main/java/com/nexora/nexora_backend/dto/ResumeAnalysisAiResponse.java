
package com.nexora.nexora_backend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResumeAnalysisAiResponse {

    private String candidateName;

    private String candidateEmail;

    private String candidatePhone;

    private String professionalSummary;

    private String currentJobTitle;

    private List<SkillData> skills;

    private List<ExperienceData> experiences;

    private List<ProjectData> projects;

    private List<EducationData> education;

    private List<CertificationData> certifications;


    // Skill information
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SkillData {

        private String skillName;

        private String category;

        private String proficiencyLevel;

        private Double yearsOfExperience;
    }


    // Work experience information
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExperienceData {

        private String jobTitle;

        private String companyName;

        private String employmentType;

        private String startDate;

        private String startDatePrecision;

        private String endDate;

        private String endDatePrecision;

        private Boolean currentlyWorking;

        private String description;

        private String technologiesUsed;
    }


    // Project information
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProjectData {

        private String projectName;

        private String description;

        private String projectRole;

        private String technologiesUsed;

        private String projectUrl;

        private String startDate;

        private String startDatePrecision;

        private String endDate;

        private String endDatePrecision;
    }


    // Education information
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EducationData {

        private String degree;

        private String fieldOfStudy;

        private String institutionName;

        private Integer graduationYear;

        private String gradeOrCgpa;

        private Integer startYear;

        private Integer endYear;

        private String description;
    }


    // Certification information
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CertificationData {

        private String certificationName;

        private String issuingOrganization;

        private String issueDate;

        private String issueDatePrecision;

        private String expiryDate;

        private String expiryDatePrecision;

        private String credentialId;

        private String credentialUrl;
    }
}
