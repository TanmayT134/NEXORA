package com.nexora.nexora_backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.nexora.nexora_backend.entity.InterviewQuestion;

public interface InterviewQuestionRepository
        extends JpaRepository<InterviewQuestion, Long> {

    @EntityGraph(attributePaths = {"parentQuestion"})
    List<InterviewQuestion> findAllBySessionIdOrderBySequenceNumberAsc(
            Long sessionId
    );

    Optional<InterviewQuestion> findByIdAndSessionId(
            Long questionId,
            Long sessionId
    );

    boolean existsBySessionId(Long sessionId);

    List<InterviewQuestion> findAllBySessionIdIn(Collection<Long> sessionIds);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update InterviewQuestion q set q.parentQuestion = null where q.session.id in :sessionIds")
    int clearParentReferencesBySessionIds(@org.springframework.data.repository.query.Param("sessionIds") Collection<Long> sessionIds);

    void deleteAllBySessionIdIn(Collection<Long> sessionIds);

    long countBySessionId(Long sessionId);
}