package com.nexora.nexora_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdaptiveInterviewTurnResponse {
    // QUESTION or INTERVIEW_FINISHED
    private String action;
    private Long sessionId;
    private InterviewQuestionResponse question;
    private Integer mainQuestionsAsked;
    private Integer mainQuestionLimit;
    private Integer followUpDepth;
    private String message;
}
