package com.nexora.nexora_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nexora.nexora_backend.entity.InterviewConfiguration;

public interface InterviewConfigurationRepository
        extends JpaRepository<InterviewConfiguration, Long> {

    // Retrieve all configurations belonging to a candidate
    List<InterviewConfiguration> findAllByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    // Retrieve a specific configuration belonging to a candidate
    Optional<InterviewConfiguration> findByIdAndUserId(
            Long id,
            Long userId
    );
}