package com.chatapp.controller;

import com.chatapp.dto.ContactDTO;
import com.chatapp.model.User;
import com.chatapp.security.SecurityUtils;
import com.chatapp.service.MessageService;
import com.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Set;

// Controller MVC "classico" (restituisce nomi di view Thymeleaf, non JSON) per la
// dashboard e la pagina di una singola chat.
@Controller
@RequiredArgsConstructor
public class ChatViewController {

    private final UserService userService;
    private final MessageService messageService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        Long currentUserId = SecurityUtils.currentUserId(authentication);
        User currentUser = userService.getOrThrow(currentUserId);

        // Separiamo i contatti in due liste: quelli con cui esiste già una conversazione
        // ("chat aperte") e quelli nuovi con cui non si è mai scritto ("nuova chat").
        Set<Long> partnerIds = Set.copyOf(messageService.getConversationPartnerIds(currentUserId));
        List<ContactDTO> allOthers = userService.listOtherUsers(currentUserId);

        List<ContactDTO> openChats = allOthers.stream().filter(c -> partnerIds.contains(c.getId())).toList();
        List<ContactDTO> newContacts = allOthers.stream().filter(c -> !partnerIds.contains(c.getId())).toList();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("openChats", openChats);
        model.addAttribute("newContacts", newContacts);
        return "dashboard";
    }

    @GetMapping("/chat/{peerId}")
    public String chatRoom(@PathVariable Long peerId, Authentication authentication, Model model) {
        Long currentUserId = SecurityUtils.currentUserId(authentication);
        User currentUser = userService.getOrThrow(currentUserId);
        User peer = userService.getOrThrow(peerId);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("peer", peer);
        return "chat";
    }
}
