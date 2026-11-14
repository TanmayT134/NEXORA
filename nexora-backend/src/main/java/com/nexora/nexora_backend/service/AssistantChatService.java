package com.nexora.nexora_backend.service;

import org.springframework.stereotype.Service;
import com.nexora.nexora_backend.dto.AssistantChatRequest;
import com.nexora.nexora_backend.dto.AssistantChatResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssistantChatService {
    private final GeminiService geminiService;

    public AssistantChatResponse reply(String candidateEmail, AssistantChatRequest request) {
        String prompt = """
                You are NEXORA Copilot, the helpful assistant inside an interview preparation platform.
                Help the signed-in candidate understand their preparation workflow, plan practice, interpret general concepts,
                and navigate NEXORA. Be concise, practical, and friendly. Do not claim to have changed account data,
                deleted history, created sessions, uploaded files, or performed actions unless a tool actually did so.
                Never ask for passwords, JWTs, API keys, or other secrets. If an action is destructive or changes account data,
                explain the relevant page and ask the candidate to confirm it there. During a live interview this assistant
                must not be used; the frontend disables it and this endpoint is not a substitute for interview controls.
                Candidate account: %s
                Current application page: %s
                Candidate message: %s
                Reply in plain text, with short steps when useful.
                """.formatted(candidateEmail, request.getPageContext() == null ? "workspace" : request.getPageContext(), request.getMessage());
        return new AssistantChatResponse(geminiService.generateResponse(prompt));
    }
}
