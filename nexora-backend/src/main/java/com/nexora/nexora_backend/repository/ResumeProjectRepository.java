package com.nexora.nexora_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nexora.nexora_backend.entity.ResumeProject;

public interface ResumeProjectRepository
        extends JpaRepository<ResumeProject, Long> {

    List<ResumeProject> findAllByResumeAnalysisId(Long analysisId);

    @Modifying
    @Query("""
        DELETE FROM ResumeProject p
        WHERE p.resumeAnalysis.id = :analysisId
    """)
    void deleteAllByResumeAnalysisId(
            @Param("analysisId") Long analysisId
    );
}