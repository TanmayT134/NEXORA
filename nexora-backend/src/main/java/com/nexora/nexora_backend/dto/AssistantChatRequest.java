package com.nexora.nexora_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AssistantChatRequest {
    @NotBlank @Size(max = 2000)
    private String message;
    @Size(max = 80)
    private String pageContext;
}
