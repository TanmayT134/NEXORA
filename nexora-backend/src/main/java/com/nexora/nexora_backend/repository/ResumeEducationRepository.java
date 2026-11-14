package com.nexora.nexora_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nexora.nexora_backend.entity.ResumeEducation;

public interface ResumeEducationRepository
        extends JpaRepository<ResumeEducation, Long> {

    List<ResumeEducation> findAllByResumeAnalysisId(Long analysisId);

    @Modifying
    @Query("""
        DELETE FROM ResumeEducation e
        WHERE e.resumeAnalysis.id = :analysisId
    """)
    void deleteAllByResumeAnalysisId(
            @Param("analysisId") Long analysisId
    );
}