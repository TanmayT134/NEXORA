package com.nexora.nexora_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexora.nexora_backend.dto.CandidateProfileRequest;
import com.nexora.nexora_backend.dto.CandidateProfileResponse;
import com.nexora.nexora_backend.service.CandidateProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/candidate/profile")
@RequiredArgsConstructor
public class CandidateProfileController {

    private final CandidateProfileService profileService;

    // Create candidate profile
    @PostMapping
    public ResponseEntity<CandidateProfileResponse> createProfile(
            @Valid @RequestBody CandidateProfileRequest request,
            Authentication authentication) {

        CandidateProfileResponse response =
                profileService.createProfile(
                        authentication.getName(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Get logged-in candidate profile
    @GetMapping
    public ResponseEntity<CandidateProfileResponse> getProfile(
            Authentication authentication) {

        CandidateProfileResponse response =
                profileService.getProfile(authentication.getName());

        return ResponseEntity.ok(response);
    }

    // Update candidate profile
    @PutMapping
    public ResponseEntity<CandidateProfileResponse> updateProfile(
            @Valid @RequestBody CandidateProfileRequest request,
            Authentication authentication) {

        CandidateProfileResponse response =
                profileService.updateProfile(
                        authentication.getName(),
                        request
                );

        return ResponseEntity.ok(response);
    }
}