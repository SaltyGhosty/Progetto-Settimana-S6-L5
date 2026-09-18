package com.chatapp.dto;

import com.chatapp.model.Message;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Ciò che sia l'endpoint REST della cronologia sia il broadcast WebSocket inviano al browser.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseDTO {
    private Long id;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String content;
    private Instant sentAt;

    public static MessageResponseDTO fromEntity(Message m) {
        return new MessageResponseDTO(
                m.getId(),
                m.getSender().getId(),
                m.getSender().getFullName(),
                m.getReceiver().getId(),
                m.getContent(),
                m.getSentAt()
        );
    }
}
