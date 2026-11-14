package com.nexora.nexora_backend.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.nexora.nexora_backend.dto.CandidateProfileRequest;
import com.nexora.nexora_backend.dto.CandidateProfileResponse;
import com.nexora.nexora_backend.entity.CandidateProfile;
import com.nexora.nexora_backend.entity.User;
import com.nexora.nexora_backend.repository.CandidateProfileRepository;
import com.nexora.nexora_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CandidateProfileService {

    private final CandidateProfileRepository profileRepository;
    private final UserRepository userRepository;

    // Create candidate profile
    public CandidateProfileResponse createProfile(
            String email,
            CandidateProfileRequest request) {

        User user = findUserByEmail(email);

        if (profileRepository.existsByUserId(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Candidate profile already exists"
            );
        }

        CandidateProfile profile = new CandidateProfile();

        profile.setUser(user);

        updateProfileFields(profile, request);

        CandidateProfile savedProfile =
                profileRepository.save(profile);

        return mapToResponse(savedProfile);
    }

    // Get candidate profile
    public CandidateProfileResponse getProfile(String email) {

        User user = findUserByEmail(email);

        CandidateProfile profile = profileRepository
                .findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Candidate profile not found"
                ));

        return mapToResponse(profile);
    }

    // Update candidate profile
    public CandidateProfileResponse updateProfile(
            String email,
            CandidateProfileRequest request) {

        User user = findUserByEmail(email);

        CandidateProfile profile = profileRepository
                .findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Candidate profile not found"
                ));

        updateProfileFields(profile, request);

        CandidateProfile updatedProfile =
                profileRepository.save(profile);

        return mapToResponse(updatedProfile);
    }

    // Find user using authenticated email
    private User findUserByEmail(String email) {

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    // Update profile fields
    private void updateProfileFields(
            CandidateProfile profile,
            CandidateProfileRequest request) {

        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setBio(request.getBio());
        profile.setHighestQualification(
                request.getHighestQualification()
        );
        profile.setInstitution(request.getInstitution());
        profile.setGraduationYear(request.getGraduationYear());
        profile.setExperienceLevel(request.getExperienceLevel());
        profile.setPreferredRole(request.getPreferredRole());
    }

    // Convert entity into response DTO
    private CandidateProfileResponse mapToResponse(
            CandidateProfile profile) {

        User user = profile.getUser();

        return new CandidateProfileResponse(
                profile.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                profile.getPhoneNumber(),
                profile.getBio(),
                profile.getHighestQualification(),
                profile.getInstitution(),
                profile.getGraduationYear(),
                profile.getExperienceLevel(),
                profile.getPreferredRole(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}