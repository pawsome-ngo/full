package com.pawsome.rescue.features.globalchat.repository;

import com.pawsome.rescue.features.globalchat.entity.GlobalChatMessage;
import com.pawsome.rescue.features.globalchat.entity.GlobalChatReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GlobalChatReactionRepository extends JpaRepository<GlobalChatReaction, Long> {
    List<GlobalChatReaction> findByMessageId(String messageId);

    // Find a specific user's reaction on a specific message
    Optional<GlobalChatReaction> findByMessageAndUser(GlobalChatMessage message, com.pawsome.rescue.auth.entity.User user);
}