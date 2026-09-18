package com.chatapp.controller;

import com.chatapp.dto.RegisterRequest;
import com.chatapp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

// Gestisce le pagine di login e registrazione (non l'autenticazione vera e propria,
// che è delegata a Spring Security tramite SecurityConfig).
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                            BindingResult bindingResult,
                            Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        try {
            userService.register(registerRequest);
        } catch (UserService.EmailAlreadyUsedException e) {
            bindingResult.rejectValue("email", "email.exists", e.getMessage());
            return "register";
        }
        model.addAttribute("registered", true);
        return "login";
    }
}
