
package com.nexora.nexora_backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nexora.nexora_backend.dto.InterviewEvaluationAiResponse;
import com.nexora.nexora_backend.entity.EvaluationRating;
import com.nexora.nexora_backend.entity.EvaluationStatus;
import com.nexora.nexora_backend.entity.InterviewEvaluation;
import com.nexora.nexora_backend.entity.InterviewQuestion;
import com.nexora.nexora_backend.entity.InterviewSession;
import com.nexora.nexora_backend.entity.QuestionEvaluation;
import com.nexora.nexora_backend.repository.InterviewEvaluationRepository;
import com.nexora.nexora_backend.repository.QuestionEvaluationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InterviewEvaluationPersistenceService {

    private final InterviewEvaluationRepository evaluationRepository;
    private final QuestionEvaluationRepository questionEvaluationRepository;

    // Create a stable placeholder when a submitted session has no scorable answers.
    // This is an expected interview outcome, not an application error.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InterviewEvaluation saveNotGeneratedEvaluation(
            InterviewSession session, String explanation) {
        InterviewEvaluation evaluation = evaluationRepository.findBySessionId(session.getId())
                .orElseGet(() -> {
                    InterviewEvaluation created = new InterviewEvaluation();
                    created.setSession(session);
                    return created;
                });

        if (evaluation.getStatus() == EvaluationStatus.COMPLETED) {
            return evaluation;
        }

        evaluation.setStatus(EvaluationStatus.NOT_GENERATED);
        evaluation.setOverallScore(null);
        evaluation.setOverallRating(null);
        evaluation.setOverallFeedback(explanation);
        evaluation.setStrengths("No candidate answers were available to assess.");
        evaluation.setImprovementAreas("A performance assessment cannot be produced without at least one submitted answer.");
        evaluation.setRecommendations("Start a new interview and submit at least one answer to receive an AI evaluation.");
        evaluation.setEvaluatedAt(null);
        return evaluationRepository.save(evaluation);
    }

    // Create a PENDING placeholder so the report endpoint never returns 404
    // for a completed session whose evaluation request has not run yet.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InterviewEvaluation createPendingEvaluation(InterviewSession session) {
        return evaluationRepository.findBySessionId(session.getId()).orElseGet(() -> {
            InterviewEvaluation evaluation = new InterviewEvaluation();
            evaluation.setSession(session);
            evaluation.setStatus(EvaluationStatus.PENDING);
            evaluation.setOverallFeedback("Your submitted answers are ready for evaluation.");
            return evaluationRepository.save(evaluation);
        });
    }

    // CREATE EVALUATION RECORD
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InterviewEvaluation createEvaluation(InterviewSession session) {

        if (session == null || session.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid interview session is required"
            );
        }

        var existing = evaluationRepository.findBySessionId(session.getId());
        if (existing.isPresent()) {
            InterviewEvaluation evaluation = existing.get();
            if (evaluation.getStatus() == EvaluationStatus.COMPLETED
                    || evaluation.getStatus() == EvaluationStatus.IN_PROGRESS) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "An evaluation is already completed or currently in progress for this interview session"
                );
            }

            // A failed AI-provider call (for example, an exhausted Gemini quota)
            // can be retried later using the same evaluation record.
            questionEvaluationRepository.deleteAllByEvaluationId(evaluation.getId());
            evaluation.setStatus(EvaluationStatus.IN_PROGRESS);
            evaluation.setOverallScore(null);
            evaluation.setOverallRating(null);
            evaluation.setOverallFeedback(null);
            evaluation.setStrengths(null);
            evaluation.setImprovementAreas(null);
            evaluation.setRecommendations(null);
            evaluation.setEvaluatedAt(null);
            return evaluationRepository.save(evaluation);
        }

        InterviewEvaluation evaluation = new InterviewEvaluation();
        evaluation.setSession(session);
        evaluation.setStatus(EvaluationStatus.IN_PROGRESS);
        return evaluationRepository.save(evaluation);
    }

    // SAVE COMPLETED EVALUATION
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InterviewEvaluation saveCompletedEvaluation(
            Long evaluationId,
            InterviewEvaluationAiResponse aiResponse,
            List<InterviewQuestion> questions) {

        if (evaluationId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Evaluation ID is required"
            );
        }

        if (questions == null || questions.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot evaluate an interview without questions"
            );
        }

        if (aiResponse == null
                || aiResponse.getQuestionEvaluations() == null
                || aiResponse.getQuestionEvaluations().size() != questions.size()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI evaluation does not contain the expected number of question results"
            );
        }

        InterviewEvaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Interview evaluation not found"
                ));

        if (evaluation.getStatus() != EvaluationStatus.IN_PROGRESS) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Evaluation is not in progress"
            );
        }

        // Map expected questions by their IDs
        Map<Long, InterviewQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(
                        InterviewQuestion::getId,
                        Function.identity()
                ));

        // Check for duplicate question evaluations before saving anything
        if (questionEvaluationRepository.existsByEvaluationId(evaluationId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Question evaluations already exist for this evaluation"
            );
        }

        // Validate AI question results and prevent duplicate IDs
        Map<Long, InterviewEvaluationAiResponse.EvaluatedQuestion> aiQuestionMap =
                aiResponse.getQuestionEvaluations().stream()
                        .collect(Collectors.toMap(
                                InterviewEvaluationAiResponse.EvaluatedQuestion::getQuestionId,
                                Function.identity(),
                                (first, second) -> {
                                    throw new ResponseStatusException(
                                            HttpStatus.BAD_GATEWAY,
                                            "AI returned duplicate question evaluations"
                                    );
                                }
                        ));

        if (!aiQuestionMap.keySet().equals(questionMap.keySet())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI evaluation question IDs do not match the interview questions"
            );
        }

        BigDecimal totalScore = BigDecimal.ZERO;

        // Save individual question evaluations
        for (InterviewQuestion question : questions) {

            InterviewEvaluationAiResponse.EvaluatedQuestion aiQuestion =
                    aiQuestionMap.get(question.getId());

            BigDecimal score = aiQuestion.getScore();

            // Validate score
            if (score == null
                    || score.compareTo(BigDecimal.ZERO) < 0
                    || score.compareTo(BigDecimal.valueOf(100)) > 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI returned an invalid question score"
                );
            }

            // Validate required feedback fields
            if (isBlank(aiQuestion.getFeedback())
                    || isBlank(aiQuestion.getStrengths())
                    || isBlank(aiQuestion.getImprovementAreas())
                    || isBlank(aiQuestion.getExpectedConcepts())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI returned incomplete question feedback"
                );
            }

            QuestionEvaluation questionEvaluation = new QuestionEvaluation();

            questionEvaluation.setEvaluation(evaluation);
            questionEvaluation.setQuestion(question);
            questionEvaluation.setScore(score);

            // Rating is calculated by the backend
            questionEvaluation.setRating(calculateRating(score));

            questionEvaluation.setFeedback(aiQuestion.getFeedback());
            questionEvaluation.setStrengths(aiQuestion.getStrengths());
            questionEvaluation.setImprovementAreas(aiQuestion.getImprovementAreas());
            questionEvaluation.setExpectedConcepts(aiQuestion.getExpectedConcepts());

            questionEvaluationRepository.save(questionEvaluation);

            totalScore = totalScore.add(score);
        }

        // Validate overall feedback
        if (isBlank(aiResponse.getOverallFeedback())
                || isBlank(aiResponse.getStrengths())
                || isBlank(aiResponse.getImprovementAreas())
                || isBlank(aiResponse.getRecommendations())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI returned incomplete overall evaluation feedback"
            );
        }

        // Calculate overall score using equal weighting for every question
        BigDecimal overallScore = totalScore.divide(
                BigDecimal.valueOf(questions.size()),
                2,
                RoundingMode.HALF_UP
        );

        evaluation.setOverallScore(overallScore);
        evaluation.setOverallRating(calculateRating(overallScore));
        evaluation.setOverallFeedback(aiResponse.getOverallFeedback());
        evaluation.setStrengths(aiResponse.getStrengths());
        evaluation.setImprovementAreas(aiResponse.getImprovementAreas());
        evaluation.setRecommendations(aiResponse.getRecommendations());

        evaluation.setStatus(EvaluationStatus.COMPLETED);
        evaluation.setEvaluatedAt(LocalDateTime.now());

        return evaluationRepository.save(evaluation);
    }

    // MARK EVALUATION AS FAILED
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markEvaluationFailed(Long evaluationId) {

        InterviewEvaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Interview evaluation not found"
                ));

        if (evaluation.getStatus() == EvaluationStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A completed evaluation cannot be marked as failed"
            );
        }

        evaluation.setStatus(EvaluationStatus.FAILED);

        evaluationRepository.save(evaluation);
    }

    // CALCULATE RATING FROM SCORE
    private EvaluationRating calculateRating(BigDecimal score) {

        if (score.compareTo(BigDecimal.valueOf(90)) >= 0) {
            return EvaluationRating.EXCELLENT;
        }

        if (score.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return EvaluationRating.GOOD;
        }

        if (score.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return EvaluationRating.AVERAGE;
        }

        if (score.compareTo(BigDecimal.valueOf(40)) >= 0) {
            return EvaluationRating.BELOW_AVERAGE;
        }

        return EvaluationRating.POOR;
    }

    // CHECK EMPTY OR NULL STRINGS
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
    
    public java.util.Optional<InterviewEvaluation> findOptionalEvaluationBySessionId(Long sessionId) {
        return evaluationRepository.findBySessionId(sessionId);
    }

    public InterviewEvaluation findEvaluationBySessionId(Long sessionId) {
        return evaluationRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Interview evaluation not found"
                ));
    }
}
