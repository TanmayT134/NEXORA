package com.nexora.nexora_backend.service;


import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nexora.nexora_backend.entity.InterviewQuestion;
import com.nexora.nexora_backend.entity.InterviewQuestionType;
import com.nexora.nexora_backend.entity.InterviewSession;
import com.nexora.nexora_backend.repository.InterviewQuestionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InterviewQuestionPersistenceService {

    private final InterviewQuestionRepository questionRepository;
    private final EntityManager entityManager;

    @Transactional
    public InterviewQuestion saveAdaptiveQuestion(
            InterviewSession session,
            com.nexora.nexora_backend.dto.AdaptiveInterviewAiResponse generated,
            Long parentQuestionId,
            int sequenceNumber,
            int followUpDepth) {

        if (generated == null || generated.getQuestionText() == null
                || generated.getQuestionText().isBlank()
                || generated.getQuestionType() == null
                || generated.getEvaluationCriteria() == null
                || generated.getEvaluationCriteria().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI generated an incomplete adaptive question"
            );
        }

        InterviewQuestion question = new InterviewQuestion();
        question.setSession(entityManager.getReference(InterviewSession.class, session.getId()));
        question.setSequenceNumber(sequenceNumber);
        question.setQuestionText(generated.getQuestionText().trim());
        try {
            question.setQuestionType(InterviewQuestionType.valueOf(
                    generated.getQuestionType().trim().toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI generated an invalid question type"
            );
        }
        question.setSkillName(validateSkillName(
                generated.getSkillName(), session.getSelectedSkills()));
        question.setEvaluationCriteria(generated.getEvaluationCriteria().trim());
        question.setFollowUpDepth(followUpDepth);

        if (parentQuestionId != null) {
            InterviewQuestion parent = questionRepository.findById(parentQuestionId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Parent interview question not found"));
            if (!parent.getSession().getId().equals(session.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Follow-up parent belongs to another session");
            }
            question.setParentQuestion(parent);
        } else if (followUpDepth != 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Main questions must have follow-up depth 0");
        }

        return questionRepository.save(question);
    }

    private String validateSkillName(
            String generatedSkill,
            java.util.Set<String> selectedSkills) {

        if (generatedSkill == null || generatedSkill.isBlank()) {
            return null;
        }

        for (String selectedSkill : selectedSkills) {

            if (selectedSkill.equalsIgnoreCase(generatedSkill.trim())) {
                // Preserve the original spelling from the configuration
                return selectedSkill;
            }
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "AI generated a question for an unselected skill: "
                        + generatedSkill
        );
    }
}