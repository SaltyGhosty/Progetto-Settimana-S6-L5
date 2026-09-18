package com.chatapp.service.openrouter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// Forma minima della risposta che ci interessa dall'endpoint chat-completions di OpenRouter.
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouterChatResponse(
        List<Choice> choices
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String role, String content) {
    }
}
