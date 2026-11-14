package com.nexora.nexora_backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CandidateProfileResponse {

    private Long profileId;
    private Long userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String bio;
    private String highestQualification;
    private String institution;
    private Integer graduationYear;
    private String experienceLevel;
    private String preferredRole;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}