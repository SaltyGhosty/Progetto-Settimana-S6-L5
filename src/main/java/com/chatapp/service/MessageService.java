package com.chatapp.service;

import com.chatapp.dto.MessageResponseDTO;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

// Contiene tutta la logica di business sui messaggi: salvataggio, lettura della
// cronologia e calcolo dei partner di conversazione. Sia il controller REST che
// quello WebSocket passano da qui, così le regole restano in un unico posto.
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional
    public Message saveMessage(Long senderId, Long receiverId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        // Protezione contro l'invio di un messaggio a se stessi (può succedere per errore
        // se due schede del browser condividono la stessa sessione/cookie autenticato).
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Cannot send a message to yourself");
        }
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found: " + senderId));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found: " + receiverId));

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(content.trim())
                .read(false)
                .build();
        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<MessageResponseDTO> getConversation(Long userA, Long userB) {
        return messageRepository.findConversation(userA, userB).stream()
                .map(MessageResponseDTO::fromEntity)
                .toList();
    }

    // Ultimi N messaggi, in ordine cronologico (dal più vecchio) - usati come contesto per l'IA.
    @Transactional(readOnly = true)
    public List<Message> getRecentConversation(Long userA, Long userB, int limit) {
        List<Message> recentDesc = messageRepository.findRecentConversationDesc(userA, userB);
        return recentDesc.stream()
                .limit(limit)
                .sorted(Comparator.comparing(Message::getSentAt))
                .toList();
    }

    // Id di ogni utente con cui l'utente dato ha scambiato almeno un messaggio ("chat aperte").
    @Transactional(readOnly = true)
    public List<Long> getConversationPartnerIds(Long userId) {
        return messageRepository.findDistinctConversationPartnerIds(userId);
    }
}
