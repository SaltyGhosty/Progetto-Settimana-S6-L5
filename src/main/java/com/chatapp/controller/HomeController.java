package com.chatapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String root() {
        // Spring Security manda i visitatori non autenticati a /login per qualsiasi URL
        // protetto, quindi basta reindirizzare qui a /dashboard per coprire entrambi i casi
        // (utente già loggato e utente anonimo).
        return "redirect:/dashboard";
    }
}
