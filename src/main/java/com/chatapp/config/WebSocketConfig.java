package com.chatapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configurazione STOMP-su-WebSocket (con fallback SockJS) per la chat in tempo reale.
 *
 * Flusso:
 *  - Il browser si connette a /ws (SockJS) e si autentica usando lo stesso cookie di
 *    sessione HTTP già ottenuto con il login di Spring Security (non serve un login
 *    separato per il WebSocket).
 *  - Il browser invia i messaggi di chat alla destinazione applicativa "/app/chat.send".
 *  - Il server (vedi ChatWebSocketController) salva il messaggio e lo inoltra alla coda
 *    privata del destinatario tramite convertAndSendToUser(...), che si risolve in
 *    "/user/{email}/queue/messages" e viene ricevuto dalla sottoscrizione del client a
 *    "/user/queue/messages".
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Destinazioni dei messaggi che il server invia verso i client (topic broadcast + code per-utente).
        registry.enableSimpleBroker("/topic", "/queue");
        // Prefisso dei messaggi inviati dal client che devono essere instradati ai metodi @MessageMapping.
        registry.setApplicationDestinationPrefixes("/app");
        // Prefisso usato internamente dalle destinazioni di convertAndSendToUser(...).
        registry.setUserDestinationPrefix("/user");
    }
}
