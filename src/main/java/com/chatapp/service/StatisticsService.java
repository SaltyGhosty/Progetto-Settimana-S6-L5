package com.chatapp.service;

import com.chatapp.dto.StatisticsDTO;
import com.chatapp.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Calcola i conteggi mostrati nella pagina "Statistiche" e nel report via email.
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public StatisticsDTO computeFor(Long userId) {
        long sent = messageRepository.countBySenderId(userId);
        long received = messageRepository.countByReceiverId(userId);
        long openChats = messageRepository.findDistinctConversationPartnerIds(userId).size();
        return new StatisticsDTO(sent, received, openChats);
    }
}
