package com.chatapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Punto di ingresso dell'applicazione Spring Boot: avvia il container Spring,
// la configurazione automatica e il server web embedded (Tomcat) sulla porta 8080.
@SpringBootApplication
public class ChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }
}
