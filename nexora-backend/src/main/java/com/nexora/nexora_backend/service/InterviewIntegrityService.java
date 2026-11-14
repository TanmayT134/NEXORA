package com.nexora.nexora_backend.service;

import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.nexora.nexora_backend.dto.IntegrityEventRequest;
import com.nexora.nexora_backend.entity.*;
import com.nexora.nexora_backend.repository.*;
import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor
public class InterviewIntegrityService {
    private final UserRepository users;
    private final InterviewSessionRepository sessions;
    private final InterviewIntegrityEventRepository events;


    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> getEvents(String email, Long sessionId) {
        User user = users.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        sessions.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview session not found"));
        return events.findAllBySessionIdOrderByOccurredAtAsc(sessionId).stream().map(event -> {
            java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", event.getId()); item.put("eventType", event.getEventType());
            item.put("severity", event.getSeverity()); item.put("details", event.getDetails());
            item.put("occurredAt", event.getOccurredAt()); return item;
        }).toList();
    }

    @Transactional
    public Object record(String email, Long sessionId, IntegrityEventRequest request) {
        User user = users.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        InterviewSession session = sessions.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview session not found"));
        if (session.getStatus() != InterviewSessionStatus.IN_PROGRESS)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Integrity events can only be recorded during an active interview");
        String type = request.getEventType().trim().toUpperCase(Locale.ROOT);
        if (!java.util.Set.of("GAZE_AWAY", "FACE_NOT_VISIBLE", "MULTIPLE_FACES", "CAMERA_INTERRUPTED",
                "TAB_HIDDEN", "WINDOW_BLUR", "FULLSCREEN_EXIT", "PHONE_DETECTED").contains(type))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported integrity event type");
        InterviewIntegrityEvent event = new InterviewIntegrityEvent();
        event.setSession(session); event.setEventType(type);
        event.setSeverity(request.getSeverity().trim().toUpperCase(Locale.ROOT));
        event.setDetails(request.getDetails());
        events.save(event);
        long eventCount = events.countBySessionId(sessionId);
        // All supported integrity event types share one combined allowance.
        // A single event (including tab/window focus loss) is only a warning;
        // the third recorded event completes the session and triggers evaluation.
        boolean terminate = eventCount >= 3;
        String terminationReason = terminate ? "INTEGRITY_REVIEW" : "NONE";
        if (terminate) {
            session.setStatus(InterviewSessionStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            session.setTerminationReason(terminationReason);
            session.setTerminationDetails(
                    "The combined integrity warning limit of three events was reached. "
                    + "All event types share the same limit. This is an automated review flag, "
                    + "not definitive proof of misconduct.");
            sessions.save(session);
        }
        return java.util.Map.of("eventId", event.getId(), "eventType", type,
                "eventCount", eventCount, "warningLimit", 3,
                "terminated", terminate, "terminationReason", terminate ? terminationReason : "NONE");
    }
}
