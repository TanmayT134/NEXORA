
package com.nexora.nexora_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nexora.nexora_backend.entity.ResumeAnalysis;

public interface ResumeAnalysisRepository
        extends JpaRepository<ResumeAnalysis, Long> {

    Optional<ResumeAnalysis> findByResumeId(Long resumeId);

    boolean existsByResumeId(Long resumeId);

    java.util.List<ResumeAnalysis> findAllByResume_User_IdOrderByAnalyzedAtDesc(Long userId);
}
