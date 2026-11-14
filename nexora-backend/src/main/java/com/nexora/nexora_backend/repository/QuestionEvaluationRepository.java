package com.nexora.nexora_backend.repository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nexora.nexora_backend.entity.QuestionEvaluation;

public interface QuestionEvaluationRepository
        extends JpaRepository<QuestionEvaluation, Long> {

    List<QuestionEvaluation> findAllByEvaluationIdOrderByIdAsc(
            Long evaluationId
    );

    Optional<QuestionEvaluation> findByQuestionId(Long questionId);

    boolean existsByEvaluationId(Long evaluationId);

    void deleteAllByEvaluationId(Long evaluationId);

    void deleteAllByQuestionIdIn(Collection<Long> questionIds);
}