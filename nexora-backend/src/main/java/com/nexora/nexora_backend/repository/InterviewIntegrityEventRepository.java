package com.nexora.nexora_backend.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.nexora.nexora_backend.entity.InterviewIntegrityEvent;
public interface InterviewIntegrityEventRepository extends JpaRepository<InterviewIntegrityEvent, Long> {
    List<InterviewIntegrityEvent> findAllBySessionIdOrderByOccurredAtAsc(Long sessionId);
    long countBySessionIdAndEventType(Long sessionId, String eventType);
    long countBySessionId(Long sessionId);
    void deleteAllBySessionIdIn(java.util.Collection<Long> sessionIds);
}
