
package com.nexora.nexora_backend.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nexora.nexora_backend.dto.ResumeAnalysisAiResponse;
import com.nexora.nexora_backend.dto.ResumeAnalysisResponse;
import com.nexora.nexora_backend.entity.Resume;
import com.nexora.nexora_backend.entity.ResumeAnalysis;
import com.nexora.nexora_backend.entity.ResumeCertification;
import com.nexora.nexora_backend.entity.ResumeEducation;
import com.nexora.nexora_backend.entity.ResumeExperience;
import com.nexora.nexora_backend.entity.ResumeProject;
import com.nexora.nexora_backend.entity.ResumeSkill;
import com.nexora.nexora_backend.entity.User;
import com.nexora.nexora_backend.repository.ResumeAnalysisRepository;
import com.nexora.nexora_backend.repository.ResumeCertificationRepository;
import com.nexora.nexora_backend.repository.ResumeEducationRepository;
import com.nexora.nexora_backend.repository.ResumeExperienceRepository;
import com.nexora.nexora_backend.repository.ResumeProjectRepository;
import com.nexora.nexora_backend.repository.ResumeRepository;
import com.nexora.nexora_backend.repository.ResumeSkillRepository;
import com.nexora.nexora_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResumeAnalysisService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;

    private final ResumeSkillRepository skillRepository;
    private final ResumeExperienceRepository experienceRepository;
    private final ResumeProjectRepository projectRepository;
    private final ResumeEducationRepository educationRepository;
    private final ResumeCertificationRepository certificationRepository;
    private final ResumeAnalysisRepository analysisRepository;

    private final ResumeAnalysisAiService resumeAnalysisAiService;
    private final ResumeAnalysisPersistenceService persistenceService;

    public ResumeAnalysisResponse analyzeResume(
            String email,
            Long resumeId) {

        // 1. Find the authenticated candidate.
        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        // 2. Verify resume ownership.
        Resume resume = resumeRepository
                .findByIdAndUserId(resumeId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume not found"
                ));

        // 3. Verify extracted text.
        String resumeText = resume.getExtractedText();

        if (resumeText == null || resumeText.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Resume does not contain extracted text"
            );
        }

        // 4. Analyse resume using Gemini.
        ResumeAnalysisAiResponse aiResponse =
                resumeAnalysisAiService.analyzeResume(resumeText);

        // 5. Persist the analysis.
        ResumeAnalysis savedAnalysis =
                persistenceService.saveAnalysis(resume, aiResponse);

        // 6. Retrieve all saved child records.
        Long analysisId = savedAnalysis.getId();

        List<ResumeSkill> skills =
                skillRepository.findAllByResumeAnalysisId(analysisId);

        List<ResumeExperience> experiences =
                experienceRepository.findAllByResumeAnalysisId(analysisId);

        List<ResumeProject> projects =
                projectRepository.findAllByResumeAnalysisId(analysisId);

        List<ResumeEducation> education =
                educationRepository.findAllByResumeAnalysisId(analysisId);

        List<ResumeCertification> certifications =
                certificationRepository.findAllByResumeAnalysisId(analysisId);

        // 7. Convert entities into the API response DTO.
        return mapToResponse(
                savedAnalysis,
                skills,
                experiences,
                projects,
                education,
                certifications
        );
    }

    @Transactional(readOnly = true)
    public ResumeAnalysisResponse getResumeAnalysis(
            String email,
            Long resumeId) {

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        // Verify that this resume belongs to the logged-in user.
        resumeRepository.findByIdAndUserId(resumeId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume not found"
                ));

        // Retrieve the existing analysis. Do not call Gemini again.
        ResumeAnalysis analysis = analysisRepository
                .findByResumeId(resumeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume analysis not found. Please analyze the resume first."
                ));

        Long analysisId = analysis.getId();

        List<ResumeSkill> skills =
                skillRepository.findAllByResumeAnalysisId(analysisId);

        List<ResumeExperience> experiences =
                experienceRepository.findAllByResumeAnalysisId(analysisId);

        List<ResumeProject> projects =
                projectRepository.findAllByResumeAnalysisId(analysisId);

        List<ResumeEducation> education =
                educationRepository.findAllByResumeAnalysisId(analysisId);

        List<ResumeCertification> certifications =
                certificationRepository.findAllByResumeAnalysisId(analysisId);

        return mapToResponse(
                analysis,
                skills,
                experiences,
                projects,
                education,
                certifications
        );
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> listAnalyses(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return analysisRepository.findAllByResume_User_IdOrderByAnalyzedAtDesc(user.getId())
                .stream().map(analysis -> {
                    java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
                    item.put("id", analysis.getId());
                    item.put("resumeId", analysis.getResume().getId());
                    item.put("candidateName", analysis.getCandidateName());
                    item.put("currentJobTitle", analysis.getCurrentJobTitle());
                    item.put("status", analysis.getStatus());
                    item.put("analyzedAt", analysis.getAnalyzedAt());
                    return item;
                }).toList();
    }

    private ResumeAnalysisResponse mapToResponse(
            ResumeAnalysis analysis,
            List<ResumeSkill> skills,
            List<ResumeExperience> experiences,
            List<ResumeProject> projects,
            List<ResumeEducation> education,
            List<ResumeCertification> certifications) {

        return new ResumeAnalysisResponse(
                analysis.getId(),
                analysis.getResume().getId(),

                analysis.getCandidateName(),
                analysis.getCandidateEmail(),
                analysis.getCandidatePhone(),

                analysis.getProfessionalSummary(),
                analysis.getTotalExperienceYears(),
                analysis.getCurrentJobTitle(),

                analysis.getStatus(),
                analysis.getAnalyzedAt(),

                // Skills
                skills.stream()
                        .map(skill -> new ResumeAnalysisResponse.SkillResponse(
                                skill.getId(),
                                skill.getSkillName(),
                                skill.getCategory(),
                                skill.getProficiencyLevel(),
                                skill.getYearsOfExperience()
                        ))
                        .toList(),

                // Experiences
                experiences.stream()
                        .map(experience -> new ResumeAnalysisResponse.ExperienceResponse(
                                experience.getId(),
                                experience.getJobTitle(),
                                experience.getCompanyName(),
                                experience.getEmploymentType(),

                                experience.getStartDate(),
                                experience.getStartDatePrecision(),

                                experience.getEndDate(),
                                experience.getEndDatePrecision(),

                                experience.getCurrentlyWorking(),
                                experience.getDescription(),
                                experience.getTechnologiesUsed()
                        ))
                        .toList(),

                // Projects
                projects.stream()
                        .map(project -> new ResumeAnalysisResponse.ProjectResponse(
                                project.getId(),
                                project.getProjectName(),
                                project.getDescription(),
                                project.getProjectRole(),
                                project.getTechnologiesUsed(),
                                project.getProjectUrl(),

                                project.getStartDate(),
                                project.getStartDatePrecision(),

                                project.getEndDate(),
                                project.getEndDatePrecision()
                        ))
                        .toList(),

                // Education
                education.stream()
                        .map(item -> new ResumeAnalysisResponse.EducationResponse(
                                item.getId(),
                                item.getDegree(),
                                item.getFieldOfStudy(),
                                item.getInstitutionName(),
                                item.getGraduationYear(),
                                item.getGradeOrCgpa(),
                                item.getStartYear(),
                                item.getEndYear(),
                                item.getDescription()
                        ))
                        .toList(),

                // Certifications
                certifications.stream()
                        .map(certification -> new ResumeAnalysisResponse.CertificationResponse(
                                certification.getId(),
                                certification.getCertificationName(),
                                certification.getIssuingOrganization(),

                                certification.getIssueDate(),
                                certification.getIssueDatePrecision(),

                                certification.getExpiryDate(),
                                certification.getExpiryDatePrecision(),

                                certification.getCredentialId(),
                                certification.getCredentialUrl()
                        ))
                        .toList()
        );
    }
}
