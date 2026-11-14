package com.nexora.nexora_backend.service;

import java.util.Locale;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GeminiService {

    private final ChatClient chatClient;

    public GeminiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String generateResponse(String prompt) {
        try {
            return chatClient.prompt().user(prompt).call().content();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            String providerMessage = collectMessages(exception).toLowerCase(Locale.ROOT);

            if (isDailyQuotaExhausted(providerMessage)) {
                String retryHint = extractRetryHint(providerMessage);
                String retryText = retryHint == null ? "" : " Provider retry hint: " + retryHint + ".";
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "NEXORA's Gemini API quota for the configured project/model has been exhausted. "
                        + "This is a provider quota limit, not an authentication error. Wait for the quota reset, "
                        + "or link billing / move to a higher Gemini API usage tier in Google AI Studio. "
                        + "Your interview session has been kept so you can retry later." + retryText,
                        exception);
            }

            if (isRateLimited(providerMessage)) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Gemini is temporarily rate-limiting requests. Wait briefly before trying again. "
                        + "NEXORA did not repeat the request automatically.",
                        exception);
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "The Gemini AI service could not complete this request. Check the backend log for provider details.",
                    exception);
        }
    }

    private boolean isDailyQuotaExhausted(String message) {
        return message.contains("quota_exceeded")
                || message.contains("quota exceeded")
                || message.contains("generate_content_free_tier_requests")
                || message.contains("requests per day");
    }

    private boolean isRateLimited(String message) {
        return message.contains("429")
                || message.contains("resource_exhausted")
                || message.contains("rate limit")
                || message.contains("too many requests");
    }

    private String extractRetryHint(String message) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("retry\\s+in\\s+([^\\r\\n.;]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(message);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String collectMessages(Throwable throwable) {
        StringBuilder messages = new StringBuilder();
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth++ < 10) {
            if (current.getMessage() != null) {
                messages.append(' ').append(current.getMessage());
            }
            current = current.getCause();
        }
        return messages.toString();
    }
}
