package com.nexora.nexora_backend.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nexora.nexora_backend.dto.InterviewConfigurationRequest;
import com.nexora.nexora_backend.dto.InterviewConfigurationResponse;
import com.nexora.nexora_backend.entity.InterviewConfiguration;
import com.nexora.nexora_backend.entity.ResumeAnalysis;
import com.nexora.nexora_backend.entity.User;
import com.nexora.nexora_backend.repository.InterviewConfigurationRepository;
import com.nexora.nexora_backend.repository.ResumeAnalysisRepository;
import com.nexora.nexora_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InterviewConfigurationService {

    private final InterviewConfigurationRepository configurationRepository;
    private final UserRepository userRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;

    // CREATE CONFIGURATION
    @Transactional
    public InterviewConfigurationResponse createConfiguration(
            String email,
            InterviewConfigurationRequest request) {

        User user = findUserByEmail(email);

        InterviewConfiguration configuration =
                new InterviewConfiguration();

        configuration.setUser(user);

        updateConfigurationFields(configuration, request, user);

        InterviewConfiguration savedConfiguration =
                configurationRepository.save(configuration);

        return mapToResponse(
                savedConfiguration,
                "Interview configuration created successfully"
        );
    }

    // GET ALL CONFIGURATIONS OF LOGGED-IN USER
    @Transactional(readOnly = true)
    public List<InterviewConfigurationResponse> getAllConfigurations(
            String email) {

        User user = findUserByEmail(email);

        return configurationRepository
                .findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(configuration -> mapToResponse(
                        configuration,
                        "Interview configuration retrieved successfully"
                ))
                .collect(Collectors.toList());
    }

    // GET SINGLE CONFIGURATION
    @Transactional(readOnly = true)
    public InterviewConfigurationResponse getConfigurationById(
            String email,
            Long configurationId) {

        User user = findUserByEmail(email);

        InterviewConfiguration configuration =
                findConfigurationByIdAndUser(
                        configurationId,
                        user.getId()
                );

        return mapToResponse(
                configuration,
                "Interview configuration retrieved successfully"
        );
    }

    // UPDATE CONFIGURATION
    @Transactional
    public InterviewConfigurationResponse updateConfiguration(
            String email,
            Long configurationId,
            InterviewConfigurationRequest request) {

        User user = findUserByEmail(email);

        InterviewConfiguration configuration =
                findConfigurationByIdAndUser(
                        configurationId,
                        user.getId()
                );

        updateConfigurationFields(configuration, request, user);

        InterviewConfiguration updatedConfiguration =
                configurationRepository.save(configuration);

        return mapToResponse(
                updatedConfiguration,
                "Interview configuration updated successfully"
        );
    }

    // DELETE CONFIGURATION
    @Transactional
    public void deleteConfiguration(
            String email,
            Long configurationId) {

        User user = findUserByEmail(email);

        InterviewConfiguration configuration =
                findConfigurationByIdAndUser(
                        configurationId,
                        user.getId()
                );

        configurationRepository.delete(configuration);
    }

    // FIND USER
    private User findUserByEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated user not found"
            );
        }

        return userRepository.findByEmail(
                email.trim().toLowerCase(Locale.ROOT)
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found"
        ));
    }

    // FIND CONFIGURATION WITH OWNERSHIP CHECK
    private InterviewConfiguration findConfigurationByIdAndUser(
            Long configurationId,
            Long userId) {

        return configurationRepository
                .findByIdAndUserId(configurationId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Interview configuration not found"
                ));
    }

    // UPDATE ENTITY FIELDS
    private void updateConfigurationFields(
            InterviewConfiguration configuration,
            InterviewConfigurationRequest request,
            User user) {

        configuration.setInterviewType(request.getInterviewType());
        configuration.setTargetRole(request.getTargetRole().trim());
        configuration.setDifficulty(request.getDifficulty());
        configuration.setExperienceLevel(request.getExperienceLevel());
        configuration.setNumberOfQuestions(request.getNumberOfQuestions());
        configuration.setDurationMinutes(request.getDurationMinutes());

        configuration.setSelectedSkills(
                normalizeSkills(request.getSelectedSkills())
        );

        configuration.setResumeAnalysis(
                resolveResumeAnalysis(
                        request.getResumeAnalysisId(),
                        user
                )
        );
    }

    // VALIDATE OPTIONAL RESUME ANALYSIS OWNERSHIP
    private ResumeAnalysis resolveResumeAnalysis(
            Long resumeAnalysisId,
            User user) {

        if (resumeAnalysisId == null) {
            return null;
        }

        ResumeAnalysis analysis = resumeAnalysisRepository
                .findById(resumeAnalysisId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume analysis not found"
                ));

        Long analysisOwnerId =
                analysis.getResume().getUser().getId();

        if (!analysisOwnerId.equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Resume analysis not found"
            );
        }

        return analysis;
    }

    // NORMALIZE AND REMOVE DUPLICATE SKILLS
    private Set<String> normalizeSkills(List<String> skills) {

        Map<String, String> uniqueSkills = new LinkedHashMap<>();

        for (String skill : skills) {

            if (skill == null || skill.isBlank()) {
                continue;
            }

            String normalizedSkill = skill.trim();

            String comparisonKey =
                    normalizedSkill.toLowerCase(Locale.ROOT);

            uniqueSkills.putIfAbsent(
                    comparisonKey,
                    normalizedSkill
            );
        }

        return new LinkedHashSet<>(uniqueSkills.values());
    }

    // MAP ENTITY TO RESPONSE DTO
    private InterviewConfigurationResponse mapToResponse(
            InterviewConfiguration configuration,
            String message) {

        Long resumeAnalysisId = null;

        if (configuration.getResumeAnalysis() != null) {
            resumeAnalysisId =
                    configuration.getResumeAnalysis().getId();
        }

        return new InterviewConfigurationResponse(
                configuration.getId(),
                configuration.getUser().getId(),
                resumeAnalysisId,
                configuration.getInterviewType(),
                configuration.getTargetRole(),
                configuration.getDifficulty(),
                configuration.getExperienceLevel(),
                configuration.getNumberOfQuestions(),
                configuration.getDurationMinutes(),
                new LinkedHashSet<>(
                        configuration.getSelectedSkills()
                ),
                configuration.getCreatedAt(),
                configuration.getUpdatedAt(),
                message
        );
    }
}