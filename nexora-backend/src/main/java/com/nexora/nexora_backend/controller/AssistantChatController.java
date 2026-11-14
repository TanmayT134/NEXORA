package com.nexora.nexora_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.nexora.nexora_backend.dto.AssistantChatRequest;
import com.nexora.nexora_backend.dto.AssistantChatResponse;
import com.nexora.nexora_backend.service.AssistantChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class AssistantChatController {
    private final AssistantChatService assistantChatService;

    @PostMapping("/chat")
    public ResponseEntity<AssistantChatResponse> chat(Authentication authentication,
            @Valid @RequestBody AssistantChatRequest request) {
        return ResponseEntity.ok(assistantChatService.reply(authentication.getName(), request));
    }
}
