package com.nexora.nexora_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nexora.nexora_backend.entity.ResumeExperience;

public interface ResumeExperienceRepository
        extends JpaRepository<ResumeExperience, Long> {

    List<ResumeExperience> findAllByResumeAnalysisId(Long analysisId);

    @Modifying
    @Query("""
        DELETE FROM ResumeExperience e
        WHERE e.resumeAnalysis.id = :analysisId
    """)
    void deleteAllByResumeAnalysisId(
            @Param("analysisId") Long analysisId
    );
}