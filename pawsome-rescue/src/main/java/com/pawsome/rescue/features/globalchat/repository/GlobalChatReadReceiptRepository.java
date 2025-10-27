package com.pawsome.rescue.features.globalchat.repository;

import com.pawsome.rescue.features.globalchat.entity.GlobalChatReadReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GlobalChatReadReceiptRepository extends JpaRepository<GlobalChatReadReceipt, Long> {
    List<GlobalChatReadReceipt> findByMessageId(String messageId);

    // Check if a specific user has read a specific message
    Optional<GlobalChatReadReceipt> findByMessageIdAndUserId(String messageId, Long userId);
}