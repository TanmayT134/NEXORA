package com.nexora.nexora_backend.dto;

import com.nexora.nexora_backend.entity.InterviewQuestionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestionResponse {
    private Long id;
    private Integer sequenceNumber;
    private String questionText;
    private InterviewQuestionType questionType;
    private String skillName;
    private Long parentQuestionId;
    private Integer followUpDepth;
    private boolean followUp;
}
