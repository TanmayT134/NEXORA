package com.nexora.nexora_backend.service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nexora.nexora_backend.dto.CreateInterviewSessionRequest;
import com.nexora.nexora_backend.dto.InterviewSessionResponse;
import com.nexora.nexora_backend.entity.InterviewConfiguration;
import com.nexora.nexora_backend.entity.InterviewSession;
import com.nexora.nexora_backend.entity.InterviewSessionStatus;
import com.nexora.nexora_backend.entity.InterviewSessionMode;
import com.nexora.nexora_backend.repository.InterviewConfigurationRepository;
import com.nexora.nexora_backend.repository.InterviewSessionRepository;
import com.nexora.nexora_backend.repository.UserRepository;
import com.nexora.nexora_backend.repository.InterviewQuestionRepository;
import com.nexora.nexora_backend.repository.InterviewAnswerRepository;
import com.nexora.nexora_backend.repository.InterviewEvaluationRepository;
import com.nexora.nexora_backend.repository.QuestionEvaluationRepository;
import com.nexora.nexora_backend.repository.InterviewIntegrityEventRepository;
import com.nexora.nexora_backend.repository.ResumeSkillRepository;
import com.nexora.nexora_backend.repository.ResumeProjectRepository;
import com.nexora.nexora_backend.repository.ResumeExperienceRepository;
import com.nexora.nexora_backend.repository.ResumeCertificationRepository;
import com.nexora.nexora_backend.repository.ResumeEducationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.LinkedHashMap;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InterviewSessionService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewConfigurationRepository configurationRepository;
    private final UserRepository userRepository;
    private final AdaptiveInterviewService adaptiveInterviewService;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final InterviewEvaluationRepository evaluationRepository;
    private final QuestionEvaluationRepository questionEvaluationRepository;
    private final InterviewIntegrityEventRepository integrityEventRepository;
    private final ResumeSkillRepository resumeSkillRepository;
    private final ResumeProjectRepository resumeProjectRepository;
    private final ResumeExperienceRepository resumeExperienceRepository;
    private final ResumeCertificationRepository resumeCertificationRepository;
    private final ResumeEducationRepository resumeEducationRepository;
    private final ObjectMapper objectMapper;

    // CREATE SESSION FROM SAVED CONFIGURATION
    @Transactional
    public InterviewSessionResponse createSession(
            String email,
            CreateInterviewSessionRequest request) {

        var user = findUserByEmail(email);

        InterviewConfiguration configuration =
                configurationRepository
                    .findByIdAndUserId(
                        request.getConfigurationId(),
                        user.getId()
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Interview configuration not found"
                    ));

        InterviewSession session = new InterviewSession();

        session.setUser(user);
        session.setSourceConfigurationId(configuration.getId());

        // Copy configuration settings into the session
        session.setInterviewType(configuration.getInterviewType());
        session.setTargetRole(configuration.getTargetRole());
        session.setDifficulty(configuration.getDifficulty());
        session.setExperienceLevel(configuration.getExperienceLevel());
        session.setNumberOfQuestions(configuration.getNumberOfQuestions());
        session.setDurationMinutes(configuration.getDurationMinutes());

        // Preserve selected skills as a snapshot
        LinkedHashSet<String> sessionSkills = new LinkedHashSet<>(configuration.getSelectedSkills());
        if (configuration.getResumeAnalysis() != null) {
            Long analysisId = configuration.getResumeAnalysis().getId();
            var skills = resumeSkillRepository.findAllByResumeAnalysisId(analysisId);
            skills.forEach(skill -> sessionSkills.add(skill.getSkillName()));
            session.setResumeContextSnapshot(buildResumeSnapshot(configuration.getResumeAnalysis(), analysisId));
        }
        session.setSelectedSkills(sessionSkills);

        session.setMode(request.getMode() == null
                ? InterviewSessionMode.TEXT
                : request.getMode());
        session.setStatus(InterviewSessionStatus.CREATED);

        InterviewSession savedSession =
                sessionRepository.save(session);

        return mapToResponse(
            savedSession,
            "Interview session created successfully"
        );
    }

    // GET ALL SESSIONS OF LOGGED-IN USER
    @Transactional(readOnly = true)
    public List<InterviewSessionResponse> getAllSessions(String email) {

        var user = findUserByEmail(email);

        return sessionRepository
            .findAllByUserIdOrderByCreatedAtDesc(user.getId())
            .stream()
            .map(session -> mapToResponse(
                session,
                "Interview session retrieved successfully"
            ))
            .collect(Collectors.toList());
    }

    // PERMANENTLY CLEAR ALL INTERVIEW HISTORY FOR THE AUTHENTICATED CANDIDATE
    @Transactional
    public int clearAllSessions(String email) {
        var user = findUserByEmail(email);
        List<InterviewSession> sessions = sessionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        if (sessions.isEmpty()) return 0;

        List<Long> sessionIds = sessions.stream().map(InterviewSession::getId).toList();
        List<Long> questionIds = questionRepository.findAllBySessionIdIn(sessionIds)
                .stream().map(q -> q.getId()).toList();

        // Delete dependants explicitly to keep this safe with the current FK schema.
        integrityEventRepository.deleteAllBySessionIdIn(sessionIds);
        if (!questionIds.isEmpty()) {
            questionEvaluationRepository.deleteAllByQuestionIdIn(questionIds);
            answerRepository.deleteAllByQuestionIdIn(questionIds);
            // Follow-up questions can point to a parent question in the same session.
            questionRepository.clearParentReferencesBySessionIds(sessionIds);
            questionRepository.deleteAllBySessionIdIn(sessionIds);
        }
        evaluationRepository.deleteAllBySessionIdIn(sessionIds);
        sessionRepository.deleteAll(sessions);
        return sessions.size();
    }

    // GET SINGLE SESSION
    @Transactional(readOnly = true)
    public InterviewSessionResponse getSessionById(
            String email,
            Long sessionId) {

        var user = findUserByEmail(email);

        InterviewSession session =
                findSessionByIdAndUser(sessionId, user.getId());

        return mapToResponse(
            session,
            "Interview session retrieved successfully"
        );
    }

    // START INTERVIEW SESSION
 // START INTERVIEW SESSION
    public InterviewSessionResponse startSession(
            String email,
            Long sessionId) {

        // Find authenticated user
        var user = findUserByEmail(email);

        // Verify session ownership
        InterviewSession session =
                findSessionByIdAndUser(sessionId, user.getId());

        if (session.getStatus() != InterviewSessionStatus.CREATED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only created interviews can be started"
            );
        }

        // Generate only the first question. Every subsequent question is selected
        // dynamically after the candidate submits the preceding answer.
        if (questionRepository.existsBySessionId(session.getId())) {
            // Recover safely if the first question was persisted but the session
            // status update failed during an earlier start request.
            if (questionRepository.countBySessionId(session.getId()) != 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Questions already exist for this session. Continue the existing interview instead."
                );
            }
        } else {
            adaptiveInterviewService.createInitialQuestion(session);
        }

        // Update session status
        session.setStatus(InterviewSessionStatus.IN_PROGRESS);
        session.setStartedAt(LocalDateTime.now());

        InterviewSession updatedSession =
                sessionRepository.save(session);

        return mapToResponse(
                updatedSession,
                "Interview started successfully"
        );
    }

 // COMPLETE INTERVIEW SESSION
    @Transactional
    public InterviewSessionResponse completeSession(
            String email,
            Long sessionId) {

        var user = findUserByEmail(email);

        InterviewSession session =
                findSessionByIdAndUser(sessionId, user.getId());

        if (session.getStatus() != InterviewSessionStatus.IN_PROGRESS) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Only ongoing sessions can be completed"
            );
        }

        // Retrieve all questions belonging to this session
        var questions = questionRepository
                .findAllBySessionIdOrderBySequenceNumberAsc(sessionId);

        if (questions.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Cannot complete an interview without questions"
            );
        }

        // Count questions that have submitted answers
        long answeredCount = questions.stream()
                .filter(question ->
                        answerRepository.existsByQuestionId(question.getId()))
                .count();

        if (answeredCount != questions.size()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Please answer all interview questions before completing the session"
            );
        }

        // Complete the interview
        session.setStatus(InterviewSessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        session.setTerminationReason("CANDIDATE_COMPLETED");
        session.setTerminationDetails("Candidate completed all generated interview questions.");

        return mapToResponse(
            sessionRepository.save(session),
            "Interview session completed successfully"
        );
    }

    // CANCEL INTERVIEW SESSION
    @Transactional
    public InterviewSessionResponse cancelSession(
            String email,
            Long sessionId) {

        var user = findUserByEmail(email);

        InterviewSession session =
                findSessionByIdAndUser(sessionId, user.getId());

        if (session.getStatus() == InterviewSessionStatus.COMPLETED
                || session.getStatus() == InterviewSessionStatus.CANCELLED) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Completed or cancelled sessions cannot be cancelled again"
            );
        }

        if (session.getStatus() != InterviewSessionStatus.CREATED
                && session.getStatus() != InterviewSessionStatus.IN_PROGRESS) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Only created or in-progress sessions can be cancelled"
            );
        }

        session.setStatus(InterviewSessionStatus.CANCELLED);
        session.setTerminationReason("CANDIDATE_CANCELLED");
        session.setTerminationDetails("Candidate cancelled this interview session.");

        return mapToResponse(
            sessionRepository.save(session),
            "Interview session cancelled successfully"
        );
    }


    private String buildResumeSnapshot(com.nexora.nexora_backend.entity.ResumeAnalysis analysis, Long analysisId) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("candidateName", analysis.getCandidateName());
            snapshot.put("professionalSummary", analysis.getProfessionalSummary());
            snapshot.put("currentJobTitle", analysis.getCurrentJobTitle());
            snapshot.put("totalExperienceYears", analysis.getTotalExperienceYears());
            snapshot.put("skills", resumeSkillRepository.findAllByResumeAnalysisId(analysisId).stream()
                    .map(x -> Map.of("name", x.getSkillName(), "category", x.getCategory().name(),
                            "proficiency", x.getProficiencyLevel() == null ? "unspecified" : x.getProficiencyLevel()))
                    .toList());
            snapshot.put("projects", resumeProjectRepository.findAllByResumeAnalysisId(analysisId).stream()
                    .map(x -> Map.of("name", x.getProjectName(), "description", x.getDescription() == null ? "" : x.getDescription(),
                            "role", x.getProjectRole() == null ? "" : x.getProjectRole(),
                            "technologies", x.getTechnologiesUsed() == null ? "" : x.getTechnologiesUsed()))
                    .toList());
            snapshot.put("experience", resumeExperienceRepository.findAllByResumeAnalysisId(analysisId).stream()
                    .map(x -> Map.of("title", x.getJobTitle(), "company", x.getCompanyName() == null ? "" : x.getCompanyName(),
                            "description", x.getDescription() == null ? "" : x.getDescription(),
                            "technologies", x.getTechnologiesUsed() == null ? "" : x.getTechnologiesUsed()))
                    .toList());
            snapshot.put("certifications", resumeCertificationRepository.findAllByResumeAnalysisId(analysisId).stream()
                    .map(x -> x.getCertificationName()).toList());
            snapshot.put("education", resumeEducationRepository.findAllByResumeAnalysisId(analysisId).stream()
                    .map(x -> Map.of("degree", x.getDegree(), "field", x.getFieldOfStudy() == null ? "" : x.getFieldOfStudy(),
                            "institution", x.getInstitutionName() == null ? "" : x.getInstitutionName()))
                    .toList());
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not snapshot the analyzed resume context");
        }
    }

    // FIND USER BY EMAIL
    private com.nexora.nexora_backend.entity.User findUserByEmail(
            String email) {

        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Authenticated user not found"
            );
        }

        return userRepository.findByEmail(
            email.trim().toLowerCase(java.util.Locale.ROOT)
        ).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "User not found"
        ));
    }

    // FIND SESSION WITH OWNERSHIP CHECK
    private InterviewSession findSessionByIdAndUser(
            Long sessionId,
            Long userId) {

        return sessionRepository
            .findByIdAndUserId(sessionId, userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Interview session not found"
            ));
    }

    // MAP ENTITY TO RESPONSE DTO
    private InterviewSessionResponse mapToResponse(
            InterviewSession session,
            String message) {

        return new InterviewSessionResponse(
            session.getId(),
            session.getUser().getId(),
            session.getSourceConfigurationId(),
            session.getInterviewType(),
            session.getTargetRole(),
            session.getDifficulty(),
            session.getExperienceLevel(),
            session.getNumberOfQuestions(),
            session.getDurationMinutes(),
            session.getStatus(),
            session.getMode(),
            new LinkedHashSet<>(session.getSelectedSkills()),
            session.getStartedAt(),
            session.getCompletedAt(),
            session.getCreatedAt(),
            session.getUpdatedAt(),
            message,
            session.getTerminationReason(),
            session.getTerminationDetails(),
            session.getResumeContextSnapshot() != null && !session.getResumeContextSnapshot().isBlank()
        );
    }
}