package com.chatapp.service.openrouter;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

// Corpo minimo della richiesta per POST {baseUrl}/chat/completions (schema compatibile OpenAI).
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenRouterChatRequest(
        String model,
        List<ChatMessage> messages,
        Double temperature,
        Integer max_tokens,
        ReasoningConfig reasoning
) {
    public record ChatMessage(String role, String content) {
    }

    // Controllo unificato del "ragionamento" di OpenRouter. Alcuni modelli gratuiti/di
    // ragionamento (es. i modelli DeepSeek usati in precedenza qui) spendono parte del
    // budget di max_tokens in un "pensiero" nascosto prima di scrivere la risposta vera.
    // Con un budget piccolo questo può lasciare quasi nulla per il contenuto visibile
    // (osservato in pratica come un suggerimento fatto solo da una cifra o un simbolo a
    // caso). Disabilitare il ragionamento lascia tutto il budget al testo del suggerimento.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReasoningConfig(Boolean enabled) {
    }
}
