package com.nexora.nexora_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nexora.nexora_backend.entity.InterviewEvaluation;

public interface InterviewEvaluationRepository
        extends JpaRepository<InterviewEvaluation, Long> {

    Optional<InterviewEvaluation> findBySessionId(Long sessionId);

    boolean existsBySessionId(Long sessionId);

    void deleteAllBySessionIdIn(java.util.Collection<Long> sessionIds);
}