package com.chatapp.controller;

import com.chatapp.dto.ChatMessageDTO;
import com.chatapp.dto.MessageResponseDTO;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.chatapp.repository.UserRepository;
import com.chatapp.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * Gestisce i frame STOMP in arrivo dal browser (destinazione "/app/chat.send"):
 * salva il messaggio nel database e poi lo inoltra alle code private di entrambi
 * i partecipanti, così ogni scheda/dispositivo aperto da uno dei due utenti si
 * aggiorna in tempo reale.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final MessageService messageService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessageDTO payload, Principal principal) {
        if (principal == null) {
            // Può succedere solo se qualcuno apre una connessione WebSocket "grezza" senza
            // aver mai fatto login tramite il normale form-login; non esiste una coda
            // autenticata a cui rispondere, quindi scartiamo semplicemente il frame.
            log.warn("Rejected chat.send from an unauthenticated WebSocket session");
            return;
        }
        // principal.getName() è l'email dell'utente loggato (vedi
        // CustomUserPrincipal.getUsername()), valorizzata automaticamente da Spring Security
        // a partire dalla sessione HTTP autenticata che ha accompagnato l'handshake SockJS:
        // non serve nessuna configurazione manuale di autenticazione per il WebSocket.
        User sender = userRepository.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + principal.getName()));

        Message saved = messageService.saveMessage(sender.getId(), payload.getReceiverId(), payload.getContent());
        MessageResponseDTO dto = MessageResponseDTO.fromEntity(saved);

        // Consegna alla coda privata del destinatario...
        messagingTemplate.convertAndSendToUser(saved.getReceiver().getEmail(), "/queue/messages", dto);
        // ...e la rimanda anche al mittente (così le sue altre schede/dispositivi restano sincronizzati).
        messagingTemplate.convertAndSendToUser(saved.getSender().getEmail(), "/queue/messages", dto);

        log.debug("Message {} delivered: {} -> {}", saved.getId(), saved.getSender().getEmail(), saved.getReceiver().getEmail());
    }

    // Rimanda gli errori di validazione/lookup solo al mittente, invece di scartarli in silenzio.
    @MessageExceptionHandler
    public void handleException(Throwable exception, Principal principal) {
        String who = principal == null ? "<anonymous>" : principal.getName();
        log.warn("Error handling chat message from {}: {}", who, exception.getMessage());
        if (principal != null) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors",
                    Map.of("error", exception.getMessage() == null ? "Could not send message" : exception.getMessage()));
        }
    }
}
