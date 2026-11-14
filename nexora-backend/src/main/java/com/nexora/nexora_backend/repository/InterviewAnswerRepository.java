package com.nexora.nexora_backend.repository;

import java.util.Optional;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.nexora.nexora_backend.entity.InterviewAnswer;

public interface InterviewAnswerRepository
        extends JpaRepository<InterviewAnswer, Long> {

    @EntityGraph(attributePaths = {"question"})
    Optional<InterviewAnswer> findByQuestionId(Long questionId);

    boolean existsByQuestionId(Long questionId);

    void deleteAllByQuestionIdIn(Collection<Long> questionIds);
}