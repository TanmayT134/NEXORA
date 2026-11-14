package com.nexora.nexora_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexora.nexora_backend.dto.InterviewConfigurationRequest;
import com.nexora.nexora_backend.dto.InterviewConfigurationResponse;
import com.nexora.nexora_backend.service.InterviewConfigurationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/candidate/interview-configurations")
@RequiredArgsConstructor
public class InterviewConfigurationController {

    private final InterviewConfigurationService configurationService;

    // CREATE CONFIGURATION
    @PostMapping
    public ResponseEntity<InterviewConfigurationResponse> createConfiguration(
            Authentication authentication,
            @Valid @RequestBody InterviewConfigurationRequest request) {

        InterviewConfigurationResponse response =
                configurationService.createConfiguration(
                        authentication.getName(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // GET ALL CONFIGURATIONS
    @GetMapping
    public ResponseEntity<List<InterviewConfigurationResponse>>
            getAllConfigurations(Authentication authentication) {

        List<InterviewConfigurationResponse> response =
                configurationService.getAllConfigurations(
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }

    // GET CONFIGURATION BY ID
    @GetMapping("/{configurationId}")
    public ResponseEntity<InterviewConfigurationResponse>
            getConfigurationById(
                    Authentication authentication,
                    @PathVariable Long configurationId) {

        InterviewConfigurationResponse response =
                configurationService.getConfigurationById(
                        authentication.getName(),
                        configurationId
                );

        return ResponseEntity.ok(response);
    }

    // UPDATE CONFIGURATION
    @PutMapping("/{configurationId}")
    public ResponseEntity<InterviewConfigurationResponse>
            updateConfiguration(
                    Authentication authentication,
                    @PathVariable Long configurationId,
                    @Valid @RequestBody InterviewConfigurationRequest request) {

        InterviewConfigurationResponse response =
                configurationService.updateConfiguration(
                        authentication.getName(),
                        configurationId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    // DELETE CONFIGURATION
    @DeleteMapping("/{configurationId}")
    public ResponseEntity<Void> deleteConfiguration(
            Authentication authentication,
            @PathVariable Long configurationId) {

        configurationService.deleteConfiguration(
                authentication.getName(),
                configurationId
        );

        return ResponseEntity.noContent().build();
    }
}