package com.chatapp.service;

import com.chatapp.config.OpenRouterProperties;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.chatapp.service.openrouter.OpenRouterChatRequest;
import com.chatapp.service.openrouter.OpenRouterChatRequest.ChatMessage;
import com.chatapp.service.openrouter.OpenRouterChatRequest.ReasoningConfig;
import com.chatapp.service.openrouter.OpenRouterChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Chiama l'API "chat completions" (compatibile OpenAI) di OpenRouter per suggerire il
 * prossimo messaggio dell'utente corrente, dato lo storico recente della conversazione.
 *
 * IMPORTANTE: il suggerimento restituito è puramente temporaneo - chi chiama
 * (AiSuggestionController) lo restituisce direttamente al browser e non viene mai
 * scritto nel database. Diventa un vero Message solo se l'utente decide di inviarlo
 * davvero, passando per il normale flusso WebSocket come qualsiasi altro messaggio.
 */
@Service
@Slf4j
public class OpenRouterService {

    private static final String SYSTEM_PROMPT = """
            You are helping a user continue a private one-on-one chat conversation.
            Given the recent message history, suggest ONE short, natural, friendly next
            message for the user to send, written in the first person as that user.
            Reply with ONLY the suggested message text - no quotes, no explanation,
            no "Suggestion:" prefix, no markdown.
            """;

    // Alcuni modelli gratuiti/di "ragionamento" su OpenRouter usano parte del budget di
    // token per un "pensiero" interno nascosto prima di scrivere la risposta vera. Se il
    // primo tentativo torna troncato o degenerato (es. un solo carattere a caso), si
    // riprova una volta con un budget più ampio prima di arrendersi.
    private static final int MAX_TOKENS = 400;
    private static final int RETRY_MAX_TOKENS = 800;

    private final WebClient webClient;
    private final OpenRouterProperties properties;

    public OpenRouterService(OpenRouterProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    /**
     * @param currentUser l'utente a cui verrà proposto il suggerimento
     * @param peer        l'altro partecipante della conversazione
     * @param history     messaggi recenti tra i due, dal più vecchio (può essere vuota per una chat nuova)
     */
    public String suggestReply(User currentUser, User peer, List<Message> history) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "OpenRouter API key is not configured. Set the OPENROUTER_API_KEY environment variable " +
                            "(get a free key at https://openrouter.ai/keys).");
        }

        List<ChatMessage> messages = buildMessages(currentUser, peer, history);

        String content = callAndExtract(messages, MAX_TOKENS);

        if (isDegenerate(content)) {
            log.warn("OpenRouter returned a degenerate suggestion ('{}'); retrying with a larger token budget",
                    content);
            content = callAndExtract(messages, RETRY_MAX_TOKENS);
        }

        if (isDegenerate(content)) {
            log.error("OpenRouter still returned a degenerate suggestion after retry ('{}')", content);
            throw new IllegalStateException("Could not get an AI suggestion right now. Please try again.");
        }

        return content;
    }

    // Un suggerimento vero è lungo almeno un paio di parole. Qualsiasi cosa più corta, o
    // senza nemmeno una lettera (un numero isolato, un simbolo...), è quasi certamente
    // una risposta troncata o corrotta, non un vero suggerimento.
    private boolean isDegenerate(String content) {
        if (content == null) {
            return true;
        }
        String stripped = content.trim();
        return stripped.length() < 3 || stripped.chars().noneMatch(Character::isLetter);
    }

    private List<ChatMessage> buildMessages(User currentUser, User peer, List<Message> history) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", SYSTEM_PROMPT));

        if (history.isEmpty()) {
            messages.add(new ChatMessage("user",
                    "There is no message history yet with " + peer.getFullName() +
                            ". Suggest a friendly opening message " + currentUser.getFullName() + " could send."));
        } else {
            for (Message m : history) {
                boolean fromCurrentUser = m.getSender().getId().equals(currentUser.getId());
                // Dal punto di vista del modello: i messaggi passati dell'utente corrente sono
                // turni "assistant" (lo stile che deve mantenere); quelli del peer sono turni "user".
                messages.add(new ChatMessage(fromCurrentUser ? "assistant" : "user", m.getContent()));
            }
        }
        return messages;
    }

    private String callAndExtract(List<ChatMessage> messages, int maxTokens) {
        OpenRouterChatRequest request = new OpenRouterChatRequest(
                properties.getModel(),
                messages,
                0.8,
                maxTokens,
                new ReasoningConfig(false)
        );

        try {
            OpenRouterChatResponse response = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .header("HTTP-Referer", properties.getSiteUrl())
                    .header("X-Title", properties.getSiteName())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpenRouterChatResponse.class)
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .block();

            if (response == null || response.choices() == null || response.choices().isEmpty()
                    || response.choices().get(0).message() == null) {
                throw new IllegalStateException("OpenRouter returned no suggestion");
            }

            String content = response.choices().get(0).message().content();
            return content == null ? "" : content.trim();

        } catch (WebClientResponseException e) {
            log.error("OpenRouter API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("OpenRouter API call failed (" + e.getStatusCode() + "). " +
                    "Check your OPENROUTER_API_KEY and that the configured model is still available for free.", e);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error calling OpenRouter", e);
            throw new IllegalStateException("Could not get an AI suggestion right now. Please try again.", e);
        }
    }
}
