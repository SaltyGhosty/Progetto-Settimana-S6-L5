package com.chatapp.controller;

import com.chatapp.config.OpenRouterProperties;
import com.chatapp.dto.AiSuggestionResponse;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.chatapp.security.SecurityUtils;
import com.chatapp.service.MessageService;
import com.chatapp.service.OpenRouterService;
import com.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Espone la funzionalità "suggerisci una risposta" basata sull'IA. Il suggerimento
// restituito qui è solo temporaneo (vedi il commento su OpenRouterService): questo
// controller non scrive mai nulla nella tabella dei messaggi.
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiSuggestionController {

    private final OpenRouterService openRouterService;
    private final MessageService messageService;
    private final UserService userService;
    private final OpenRouterProperties openRouterProperties;

    @GetMapping("/suggest/{peerId}")
    public ResponseEntity<?> suggest(@PathVariable Long peerId, Authentication authentication) {
        Long currentUserId = SecurityUtils.currentUserId(authentication);
        User currentUser = userService.getOrThrow(currentUserId);
        User peer = userService.getOrThrow(peerId);

        // Passiamo all'IA solo le ultime N righe della conversazione (configurabile in
        // application.yml) come contesto, per non superare il limite di token del modello.
        List<Message> history = messageService.getRecentConversation(
                currentUserId, peerId, openRouterProperties.getHistoryWindow());

        try {
            String suggestion = openRouterService.suggestReply(currentUser, peer, history);
            return ResponseEntity.ok(new AiSuggestionResponse(suggestion));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", e.getMessage()));
        }
    }
}
