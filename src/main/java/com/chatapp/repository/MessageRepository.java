package com.chatapp.repository;

import com.chatapp.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // Conversazione completa tra due utenti, dal messaggio più vecchio al più recente.
    @Query("""
            SELECT m FROM Message m
            WHERE (m.sender.id = :userA AND m.receiver.id = :userB)
               OR (m.sender.id = :userB AND m.receiver.id = :userA)
            ORDER BY m.sentAt ASC
            """)
    List<Message> findConversation(@Param("userA") Long userA, @Param("userB") Long userB);

    // Ultimi N messaggi di una conversazione, usati come contesto per l'IA (l'ordinamento finale lo fa chi chiama).
    @Query("""
            SELECT m FROM Message m
            WHERE (m.sender.id = :userA AND m.receiver.id = :userB)
               OR (m.sender.id = :userB AND m.receiver.id = :userA)
            ORDER BY m.sentAt DESC
            """)
    List<Message> findRecentConversationDesc(@Param("userA") Long userA, @Param("userB") Long userB);

    long countBySenderId(Long senderId);

    long countByReceiverId(Long receiverId);

    // Id distinti dei partner di conversazione di un utente (usato per il conteggio "chat aperte" e la dashboard).
    @Query("""
            SELECT DISTINCT CASE WHEN m.sender.id = :userId THEN m.receiver.id ELSE m.sender.id END
            FROM Message m
            WHERE m.sender.id = :userId OR m.receiver.id = :userId
            """)
    List<Long> findDistinctConversationPartnerIds(@Param("userId") Long userId);

    long countBySenderIdAndReceiverIdAndReadFalse(Long senderId, Long receiverId);
}
