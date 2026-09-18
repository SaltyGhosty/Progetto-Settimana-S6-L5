package com.chatapp.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Trasforma gli errori di lookup/validazione nei controller MVC (quelli che restituiscono
 * pagine, non JSON) in una pagina di errore leggibile invece di uno stack trace. Ristretto
 * volutamente con assignableTypes ai soli controller che renderizzano viste: gli endpoint
 * @RestController (API chat, suggerimento IA, statistiche) gestiscono e restituiscono i
 * propri errori come JSON, così le chiamate fetch() del browser possono interpretarli.
 */
@ControllerAdvice(assignableTypes = {ChatViewController.class, StatisticsController.class, AuthController.class})
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(IllegalArgumentException ex, Model model, HttpServletRequest request) {
        log.warn("Not found on {}: {}", request.getRequestURI(), ex.getMessage());
        model.addAttribute("message", ex.getMessage());
        return "error-generic";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoHandler(Model model) {
        model.addAttribute("message", "Page not found");
        return "error-generic";
    }
}
