package com.chatapp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// Legge tutte le proprietà con prefisso "openrouter" da application.yml (a loro volta
// valorizzate dalle variabili d'ambiente OPENROUTER_API_KEY, OPENROUTER_MODEL, ecc.).
@Component
@ConfigurationProperties(prefix = "openrouter")
@Getter
@Setter
public class OpenRouterProperties {

    // Chiave API di OpenRouter (se ne ottiene una gratuita su https://openrouter.ai/keys).
    // Letta dalla variabile d'ambiente OPENROUTER_API_KEY.
    private String apiKey;

    private String baseUrl = "https://openrouter.ai/api/v1";

    // Modello gratuito (con suffisso "...:free") preso da https://openrouter.ai/models?max_price=0.
    // Valore di default usato solo se OPENROUTER_MODEL non è impostata.
    private String model = "qwen/qwen3.8-27b:free";

    private String siteUrl = "http://localhost:8080";

    private String siteName = "Realtime Chat App";

    private int historyWindow = 10;

    private int timeoutSeconds = 20;
}
