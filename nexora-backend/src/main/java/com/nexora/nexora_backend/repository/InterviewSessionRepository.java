package com.nexora.nexora_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.nexora.nexora_backend.entity.InterviewSession;
import com.nexora.nexora_backend.entity.InterviewSessionStatus;

public interface InterviewSessionRepository
        extends JpaRepository<InterviewSession, Long> {

    // Retrieve all interview sessions belonging to a candidate
    List<InterviewSession> findAllByUserIdOrderByCreatedAtDesc(
            Long userId
    );
    
    // Retrieve a specific session with ownership verification
    @EntityGraph(attributePaths = {"selectedSkills"})
    Optional<InterviewSession> findByIdAndUserId(
            Long id,
            Long userId
    );

    // Check whether a candidate has any active interview session
    boolean existsByUserIdAndStatus(
            Long userId,
            InterviewSessionStatus status
    );

    // Count completed interviews for a candidate
    long countByUserIdAndStatus(
            Long userId,
            InterviewSessionStatus status
    );
}