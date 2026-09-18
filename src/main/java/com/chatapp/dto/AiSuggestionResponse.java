package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un suggerimento di risposta generato dall'IA, temporaneo e non persistito.
// Non viene mai scritto nella tabella dei messaggi: esiste solo in questa risposta HTTP.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiSuggestionResponse {
    private String suggestion;
}
