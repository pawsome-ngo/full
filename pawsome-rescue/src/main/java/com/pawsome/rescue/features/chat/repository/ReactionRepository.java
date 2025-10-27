package com.pawsome.rescue.features.chat.repository;

import com.pawsome.rescue.features.chat.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    List<Reaction> findByMessageId(String messageId);
    // --- New method to delete all reactions in a chat group ---
    @Transactional
    @Modifying
    @Query("DELETE FROM Reaction r WHERE r.message.id IN (SELECT m.id FROM Message m WHERE m.chatGroup.id = ?1)")
    void deleteAllByChatGroupId(String chatGroupId);
}