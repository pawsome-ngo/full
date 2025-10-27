package com.pawsome.rescue.features.chat.repository;

import com.pawsome.rescue.features.chat.entity.Message;
import com.pawsome.rescue.features.chat.entity.ReadReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface ReadReceiptRepository extends JpaRepository<ReadReceipt, Long> {
    Optional<ReadReceipt> findByMessageIdAndUserId(String messageId, Long userId);
    List<ReadReceipt> findByMessageIn(List<Message> messages);
    List<ReadReceipt> findByMessageId(String messageId); // Add this line

    // --- New method to delete all read receipts in a chat group ---
    @Transactional
    @Modifying
    @Query("DELETE FROM ReadReceipt rr WHERE rr.message.id IN (SELECT m.id FROM Message m WHERE m.chatGroup.id = ?1)")
    void deleteAllByChatGroupId(String chatGroupId);
}