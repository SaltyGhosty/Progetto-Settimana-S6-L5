package com.chatapp.controller;

import com.chatapp.dto.ContactDTO;
import com.chatapp.dto.MessageResponseDTO;
import com.chatapp.security.SecurityUtils;
import com.chatapp.service.MessageService;
import com.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// API REST usata dal JavaScript del client (chat.js) per caricare contatti e
// cronologia messaggi via fetch(), separata dal canale WebSocket usato per l'invio.
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final MessageService messageService;
    private final UserService userService;

    // Tutti gli altri utenti registrati, per la lista "inizia una nuova chat" nella dashboard.
    @GetMapping("/contacts")
    public List<ContactDTO> contacts(Authentication authentication) {
        return userService.listOtherUsers(SecurityUtils.currentUserId(authentication));
    }

    // Cronologia completa dei messaggi tra l'utente corrente e {peerId}, dal più vecchio al più recente.
    @GetMapping("/history/{peerId}")
    public List<MessageResponseDTO> history(@PathVariable Long peerId, Authentication authentication) {
        Long currentUserId = SecurityUtils.currentUserId(authentication);
        return messageService.getConversation(currentUserId, peerId);
    }
}
