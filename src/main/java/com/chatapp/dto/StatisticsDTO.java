package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Numeri mostrati nella pagina delle statistiche e nell'email del report.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsDTO {
    private long messagesSent;
    private long messagesReceived;
    private long openChats;
}
