package com.pawsome.rescue.features.globalchat.repository;

import com.pawsome.rescue.features.globalchat.entity.GlobalChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GlobalChatMessageRepository extends JpaRepository<GlobalChatMessage, String> {
    // Find top N messages ordered by timestamp descending
    List<GlobalChatMessage> findTop50ByOrderByTimestampDesc();

    // Find all messages ordered by timestamp ascending (for loading chat history)
    List<GlobalChatMessage> findAllByOrderByTimestampAsc();

    // Find all messages older than a specific timestamp
    List<GlobalChatMessage> findByTimestampBeforeOrderByTimestampAsc(LocalDateTime timestamp);

    // Find the timestamp of the Nth most recent message
    @Query(value = "SELECT timestamp FROM gchat_messages ORDER BY timestamp DESC LIMIT 1 OFFSET :offset", nativeQuery = true)
    Optional<LocalDateTime> findNthMostRecentTimestamp(@Param("offset") int offset);

    // Find all messages ordered by timestamp descending (useful for simpler deletion logic if preferred)
    // List<GlobalChatMessage> findAllByOrderByTimestampDesc();
}