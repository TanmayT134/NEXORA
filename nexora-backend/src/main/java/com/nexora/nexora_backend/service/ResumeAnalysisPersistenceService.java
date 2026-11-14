
package com.nexora.nexora_backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexora.nexora_backend.dto.ResumeAnalysisAiResponse;
import com.nexora.nexora_backend.entity.DatePrecision;
import com.nexora.nexora_backend.entity.EmploymentType;
import com.nexora.nexora_backend.entity.Resume;
import com.nexora.nexora_backend.entity.ResumeAnalysis;
import com.nexora.nexora_backend.entity.ResumeCertification;
import com.nexora.nexora_backend.entity.ResumeEducation;
import com.nexora.nexora_backend.entity.ResumeExperience;
import com.nexora.nexora_backend.entity.ResumeProject;
import com.nexora.nexora_backend.entity.ResumeSkill;
import com.nexora.nexora_backend.entity.ResumeStatus;
import com.nexora.nexora_backend.entity.SkillCategory;
import com.nexora.nexora_backend.repository.ResumeAnalysisRepository;
import com.nexora.nexora_backend.repository.ResumeCertificationRepository;
import com.nexora.nexora_backend.repository.ResumeEducationRepository;
import com.nexora.nexora_backend.repository.ResumeExperienceRepository;
import com.nexora.nexora_backend.repository.ResumeProjectRepository;
import com.nexora.nexora_backend.repository.ResumeSkillRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResumeAnalysisPersistenceService {

    private final ResumeAnalysisRepository analysisRepository;
    private final ResumeSkillRepository skillRepository;
    private final ResumeExperienceRepository experienceRepository;
    private final ResumeProjectRepository projectRepository;
    private final ResumeEducationRepository educationRepository;
    private final ResumeCertificationRepository certificationRepository;

    @Transactional
    public ResumeAnalysis saveAnalysis(
            Resume resume,
            ResumeAnalysisAiResponse aiResponse) {

        // Find an existing analysis or create a new one.
        ResumeAnalysis analysis = analysisRepository
                .findByResumeId(resume.getId())
                .orElseGet(ResumeAnalysis::new);

        analysis.setResume(resume);
        analysis.setCandidateName(aiResponse.getCandidateName());
        analysis.setCandidateEmail(aiResponse.getCandidateEmail());
        analysis.setCandidatePhone(aiResponse.getCandidatePhone());
        analysis.setProfessionalSummary(
                aiResponse.getProfessionalSummary()
        );
        analysis.setCurrentJobTitle(aiResponse.getCurrentJobTitle());

        analysis.setStatus(ResumeStatus.COMPLETED);
        analysis.setAnalyzedAt(LocalDateTime.now());

        ResumeAnalysis savedAnalysis =
                analysisRepository.save(analysis);

        Long analysisId = savedAnalysis.getId();

        // Remove previous child records before replacing them.
        skillRepository.deleteAllByResumeAnalysisId(analysisId);
        experienceRepository.deleteAllByResumeAnalysisId(analysisId);
        projectRepository.deleteAllByResumeAnalysisId(analysisId);
        educationRepository.deleteAllByResumeAnalysisId(analysisId);
        certificationRepository.deleteAllByResumeAnalysisId(analysisId);

        saveSkills(savedAnalysis, aiResponse.getSkills());

        saveExperiences(savedAnalysis, aiResponse.getExperiences());

        // Calculate total experience from persisted employment records.
        List<ResumeExperience> savedExperiences =
                experienceRepository.findAllByResumeAnalysisId(analysisId);

        // Calculate total employment experience.
        savedAnalysis.setTotalExperienceYears(
                calculateTotalExperienceYears(savedExperiences)
        );

        // Derive current job title from confirmed ongoing employment.
        savedAnalysis.setCurrentJobTitle(
                deriveCurrentJobTitle(savedExperiences)
        );

        analysisRepository.save(savedAnalysis);

        saveProjects(savedAnalysis, aiResponse.getProjects());
        saveEducation(savedAnalysis, aiResponse.getEducation());
        saveCertifications(
                savedAnalysis,
                aiResponse.getCertifications()
        );

        return savedAnalysis;
    }

    private void saveSkills(
            ResumeAnalysis analysis,
            List<ResumeAnalysisAiResponse.SkillData> skills) {

        if (skills == null || skills.isEmpty()) {
            return;
        }

        // Avoid duplicate skill names in the same AI response.
        Map<String, ResumeSkill> uniqueSkills = new LinkedHashMap<>();

        for (ResumeAnalysisAiResponse.SkillData data : skills) {

            if (data.getSkillName() == null
                    || data.getSkillName().isBlank()) {
                continue;
            }

            SkillCategory category = resolveSkillCategory(
                    data.getSkillName(),
                    data.getCategory()
            );

            ResumeSkill skill = new ResumeSkill();
            skill.setResumeAnalysis(analysis);
            skill.setSkillName(data.getSkillName().trim());
            skill.setCategory(
                    category != null ? category : SkillCategory.OTHER
            );
            skill.setProficiencyLevel(data.getProficiencyLevel());
            skill.setYearsOfExperience(data.getYearsOfExperience());

            uniqueSkills.putIfAbsent(
                    skill.getSkillName().toLowerCase(Locale.ROOT),
                    skill
            );
        }

        if (!uniqueSkills.isEmpty()) {
            skillRepository.saveAll(uniqueSkills.values());
        }
    }

    private void saveExperiences(
            ResumeAnalysis analysis,
            List<ResumeAnalysisAiResponse.ExperienceData> experiences) {

        if (experiences == null || experiences.isEmpty()) {
            return;
        }

        List<ResumeExperience> records = new ArrayList<>();

        for (ResumeAnalysisAiResponse.ExperienceData data : experiences) {

            if (data.getJobTitle() == null
                    || data.getJobTitle().isBlank()) {
                continue;
            }

            ResumeExperience experience = new ResumeExperience();

            experience.setResumeAnalysis(analysis);
            experience.setJobTitle(data.getJobTitle());
            experience.setCompanyName(data.getCompanyName());
            experience.setEmploymentType(
                    parseEnum(EmploymentType.class, data.getEmploymentType())
            );
            LocalDate startDate = parseDate(data.getStartDate());
            LocalDate endDate = parseDate(data.getEndDate());

            experience.setStartDate(startDate);
            experience.setEndDate(endDate);

            experience.setStartDatePrecision(
                    startDate == null
                            ? null
                            : parseDatePrecision(data.getStartDatePrecision())
            );

            experience.setEndDatePrecision(
                    endDate == null
                            ? null
                            : parseDatePrecision(data.getEndDatePrecision())
            );
            experience.setCurrentlyWorking(
                    Boolean.TRUE.equals(data.getCurrentlyWorking())
            );
            experience.setDescription(data.getDescription());
            experience.setTechnologiesUsed(data.getTechnologiesUsed());

            records.add(experience);
        }

        if (!records.isEmpty()) {
            experienceRepository.saveAll(records);
        }
    }

    private void saveProjects(
            ResumeAnalysis analysis,
            List<ResumeAnalysisAiResponse.ProjectData> projects) {

        if (projects == null || projects.isEmpty()) {
            return;
        }

        List<ResumeProject> records = new ArrayList<>();

        for (ResumeAnalysisAiResponse.ProjectData data : projects) {

            if (data.getProjectName() == null
                    || data.getProjectName().isBlank()) {
                continue;
            }

            ResumeProject project = new ResumeProject();

            project.setResumeAnalysis(analysis);
            project.setProjectName(data.getProjectName());
            project.setDescription(data.getDescription());
            project.setProjectRole(data.getProjectRole());
            project.setTechnologiesUsed(data.getTechnologiesUsed());
            project.setProjectUrl(data.getProjectUrl());
            LocalDate startDate = parseDate(data.getStartDate());
            LocalDate endDate = parseDate(data.getEndDate());

            project.setStartDate(startDate);
            project.setEndDate(endDate);

            project.setStartDatePrecision(
                    startDate == null
                            ? null
                            : parseDatePrecision(data.getStartDatePrecision())
            );

            project.setEndDatePrecision(
                    endDate == null
                            ? null
                            : parseDatePrecision(data.getEndDatePrecision())
            );

            records.add(project);
        }

        if (!records.isEmpty()) {
            projectRepository.saveAll(records);
        }
    }

    private void saveEducation(
            ResumeAnalysis analysis,
            List<ResumeAnalysisAiResponse.EducationData> educationList) {

        if (educationList == null || educationList.isEmpty()) {
            return;
        }

        List<ResumeEducation> records = new ArrayList<>();

        for (ResumeAnalysisAiResponse.EducationData data : educationList) {

            if (data.getDegree() == null
                    || data.getDegree().isBlank()) {
                continue;
            }

            ResumeEducation education = new ResumeEducation();

            education.setResumeAnalysis(analysis);
            education.setDegree(data.getDegree());
            education.setFieldOfStudy(data.getFieldOfStudy());
            education.setInstitutionName(data.getInstitutionName());
            education.setGraduationYear(data.getGraduationYear());
            education.setGradeOrCgpa(data.getGradeOrCgpa());
            education.setStartYear(data.getStartYear());
            education.setEndYear(data.getEndYear());
            education.setDescription(data.getDescription());

            records.add(education);
        }

        if (!records.isEmpty()) {
            educationRepository.saveAll(records);
        }
    }

    private void saveCertifications(
            ResumeAnalysis analysis,
            List<ResumeAnalysisAiResponse.CertificationData> certifications) {

        if (certifications == null || certifications.isEmpty()) {
            return;
        }

        List<ResumeCertification> records = new ArrayList<>();

        for (ResumeAnalysisAiResponse.CertificationData data : certifications) {

            if (data.getCertificationName() == null
                    || data.getCertificationName().isBlank()) {
                continue;
            }

            ResumeCertification certification =
                    new ResumeCertification();

            certification.setResumeAnalysis(analysis);
            certification.setCertificationName(
                    data.getCertificationName()
            );
            certification.setIssuingOrganization(
                    data.getIssuingOrganization()
            );
            LocalDate issueDate = parseDate(data.getIssueDate());
            LocalDate expiryDate = parseDate(data.getExpiryDate());

            certification.setIssueDate(issueDate);
            certification.setExpiryDate(expiryDate);

            certification.setIssueDatePrecision(
                    issueDate == null
                            ? null
                            : parseDatePrecision(data.getIssueDatePrecision())
            );

            certification.setExpiryDatePrecision(
                    expiryDate == null
                            ? null
                            : parseDatePrecision(data.getExpiryDatePrecision())
            );
            certification.setCredentialId(data.getCredentialId());
            certification.setCredentialUrl(data.getCredentialUrl());

            records.add(certification);
        }

        if (!records.isEmpty()) {
            certificationRepository.saveAll(records);
        }
    }

    private String deriveCurrentJobTitle(
            List<ResumeExperience> experiences) {

        if (experiences == null || experiences.isEmpty()) {
            return null;
        }

        return experiences.stream()
                .filter(experience ->
                        Boolean.TRUE.equals(experience.getCurrentlyWorking())
                )
                .filter(experience ->
                        experience.getJobTitle() != null
                        && !experience.getJobTitle().isBlank()
                )
                .sorted(
                        Comparator.comparing(
                                ResumeExperience::getStartDate,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                )
                .map(ResumeExperience::getJobTitle)
                .findFirst()
                .orElse(null);
    }

    private SkillCategory resolveSkillCategory(
            String skillName,
            String aiCategory) {

        if (skillName == null || skillName.isBlank()) {
            return SkillCategory.OTHER;
        }

        String normalizedSkill = skillName.trim()
                .toLowerCase(Locale.ROOT);

        // Backend technologies
        switch (normalizedSkill) {

            case "jdbc",
                 "java database connectivity",
                 "servlet",
                 "servlets",
                 "jakarta servlet",
                 "jakarta servlets",
                 "jsp",
                 "java server pages",
                 "rest api",
                 "rest apis",
                 "restful api",
                 "restful apis":

                return SkillCategory.BACKEND_TECHNOLOGY;

            // Web technologies
            case "axios",
                 "html",
                 "html5",
                 "css",
                 "css3",
                 "dom manipulation",
                 "responsive web design":

                return SkillCategory.WEB_TECHNOLOGY;

            default:
                SkillCategory category =
                        parseEnum(SkillCategory.class, aiCategory);

                return category != null
                        ? category
                        : SkillCategory.OTHER;
        }
    }

	private BigDecimal calculateTotalExperienceYears(
	        List<ResumeExperience> experiences) {

	    if (experiences == null || experiences.isEmpty()) {
	        return null;
	    }

	    List<DateRange> ranges = new ArrayList<>();

	    LocalDate today = LocalDate.now();

	    for (ResumeExperience experience : experiences) {

	        LocalDate startDate = experience.getStartDate();

	        DatePrecision startPrecision =
	                experience.getStartDatePrecision();

	        // A reliable start date and precision are required.
	        if (startDate == null
	                || startPrecision == null
	                || startPrecision == DatePrecision.UNKNOWN) {
	            continue;
	        }

	        LocalDate normalizedStart =
	                normalizeStartDate(startDate, startPrecision);

	        LocalDate endDate;
	        DatePrecision endPrecision;

	        if (Boolean.TRUE.equals(experience.getCurrentlyWorking())) {

	            // Ongoing employment ends today for calculation purposes.
	            endDate = today;

	        } else {

	            endDate = experience.getEndDate();
	            endPrecision = experience.getEndDatePrecision();

	            // Past employment needs a reliable end date and precision.
	            if (endDate == null
	                    || endPrecision == null
	                    || endPrecision == DatePrecision.UNKNOWN) {
	                continue;
	            }

	            endDate = normalizeEndDate(endDate, endPrecision);
	        }

	        // Ignore invalid or future employment periods.
	        if (normalizedStart.isAfter(endDate)
	                || normalizedStart.isAfter(today)) {
	            continue;
	        }

	        // Do not count dates beyond today.
	        if (endDate.isAfter(today)) {
	            endDate = today;
	        }

	        ranges.add(new DateRange(normalizedStart, endDate));
	    }

	    if (ranges.isEmpty()) {
	        return null;
	    }

	    // Sort employment periods by their start date.
	    ranges.sort(Comparator.comparing(DateRange::start));

	    // Merge overlapping or consecutive employment periods.
	    LocalDate mergedStart = ranges.get(0).start();
	    LocalDate mergedEnd = ranges.get(0).end();

	    long totalDays = 0;

	    for (int i = 1; i < ranges.size(); i++) {

	        DateRange current = ranges.get(i);

	        if (!current.start().isAfter(mergedEnd.plusDays(1))) {

	            if (current.end().isAfter(mergedEnd)) {
	                mergedEnd = current.end();
	            }

	        } else {

	            totalDays += ChronoUnit.DAYS.between(
	                    mergedStart,
	                    mergedEnd
	            ) + 1;

	            mergedStart = current.start();
	            mergedEnd = current.end();
	        }
	    }

	    // Add the final merged period.
	    totalDays += ChronoUnit.DAYS.between(
	            mergedStart,
	            mergedEnd
	    ) + 1;

	    // Convert total days into years using the average Gregorian year.
	    return BigDecimal.valueOf(totalDays)
	            .divide(
	                    BigDecimal.valueOf(365.2425),
	                    2,
	                    RoundingMode.HALF_UP
	            );
	}

	private LocalDate normalizeStartDate(
	        LocalDate date,
	        DatePrecision precision) {

	    return switch (precision) {

	        case YEAR -> LocalDate.of(date.getYear(), 1, 1);

	        case MONTH -> date.withDayOfMonth(1);

	        case EXACT -> date;

	        case UNKNOWN -> null;
	    };
	}

	private LocalDate normalizeEndDate(
	        LocalDate date,
	        DatePrecision precision) {

	    return switch (precision) {

	        case YEAR -> LocalDate.of(date.getYear(), 12, 31);

	        case MONTH -> YearMonth.from(date).atEndOfMonth();

	        case EXACT -> date;

	        case UNKNOWN -> null;
	    };
	}

	private record DateRange(
	        LocalDate start,
	        LocalDate end) {
	}


    private LocalDate parseDate(String date) {

        if (date == null || date.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private <E extends Enum<E>> E parseEnum(
            Class<E> enumClass,
            String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            String normalized = value.trim()
                    .replace('-', '_')
                    .replace(' ', '_')
                    .toUpperCase(Locale.ROOT);

            return Enum.valueOf(enumClass, normalized);

        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private DatePrecision parseDatePrecision(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return DatePrecision.valueOf(
                    value.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            return DatePrecision.UNKNOWN;
        }
    }
}
