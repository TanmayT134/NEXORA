
package com.nexora.nexora_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexora.nexora_backend.dto.ResumeAnalysisResponse;
import com.nexora.nexora_backend.service.ResumeAnalysisService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/candidate/resumes")
@RequiredArgsConstructor
public class ResumeAnalysisController {

    private final ResumeAnalysisService resumeAnalysisService;

    @GetMapping("/analyses")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> listAnalyses(
            Authentication authentication) {
        return ResponseEntity.ok(resumeAnalysisService.listAnalyses(authentication.getName()));
    }

    @PostMapping("/{resumeId}/analyze")
    public ResponseEntity<ResumeAnalysisResponse> analyzeResume(
            @PathVariable Long resumeId,
            Authentication authentication) {

        ResumeAnalysisResponse response =
                resumeAnalysisService.analyzeResume(
                        authentication.getName(),
                        resumeId
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{resumeId}/analysis")
    public ResponseEntity<ResumeAnalysisResponse> getResumeAnalysis(
            @PathVariable Long resumeId,
            Authentication authentication) {

        ResumeAnalysisResponse response =
                resumeAnalysisService.getResumeAnalysis(
                        authentication.getName(),
                        resumeId
                );

        return ResponseEntity.ok(response);
    }
}
