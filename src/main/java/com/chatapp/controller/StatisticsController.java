package com.chatapp.controller;

import com.chatapp.dto.StatisticsDTO;
import com.chatapp.model.User;
import com.chatapp.security.SecurityUtils;
import com.chatapp.service.EmailService;
import com.chatapp.service.StatisticsService;
import com.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final EmailService emailService;
    private final UserService userService;

    // Pagina delle statistiche: conteggio messaggi + pulsante per inviare il report via email.
    @GetMapping("/statistics")
    public String statisticsPage(Authentication authentication, Model model) {
        Long currentUserId = SecurityUtils.currentUserId(authentication);
        model.addAttribute("currentUser", userService.getOrThrow(currentUserId));
        model.addAttribute("stats", statisticsService.computeFor(currentUserId));
        return "statistics";
    }

    // Versione JSON, utile se la pagina vuole aggiornare i conteggi senza ricaricarsi del tutto.
    @GetMapping("/api/statistics/me")
    @ResponseBody
    public StatisticsDTO myStatistics(Authentication authentication) {
        return statisticsService.computeFor(SecurityUtils.currentUserId(authentication));
    }

    // Ricalcola le statistiche aggiornate e le invia via email all'indirizzo dell'utente loggato.
    @PostMapping("/api/statistics/email")
    @ResponseBody
    public ResponseEntity<?> emailMyStatistics(Authentication authentication) {
        Long currentUserId = SecurityUtils.currentUserId(authentication);
        User currentUser = userService.getOrThrow(currentUserId);
        StatisticsDTO stats = statisticsService.computeFor(currentUserId);

        try {
            emailService.sendStatisticsReport(currentUser, stats);
            return ResponseEntity.ok(Map.of("sent", true, "email", currentUser.getEmail()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("sent", false, "error", e.getMessage()));
        }
    }
}
