package com.nexora.nexora_backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CandidateProfileRequest {

    @Pattern(
        regexp = "^[0-9+()\\-\\s]{7,20}$",
        message = "Please provide a valid phone number"
    )
    private String phoneNumber;

    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;

    @Size(max = 100)
    private String highestQualification;

    @Size(max = 150)
    private String institution;

    @Min(value = 1950, message = "Invalid graduation year")
    @Max(value = 2100, message = "Invalid graduation year")
    private Integer graduationYear;

    @Size(max = 50)
    private String experienceLevel;

    @Size(max = 100)
    private String preferredRole;
}