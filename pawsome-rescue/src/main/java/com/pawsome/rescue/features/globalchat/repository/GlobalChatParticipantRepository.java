package com.pawsome.rescue.features.globalchat.repository;

import com.pawsome.rescue.auth.entity.User;
import com.pawsome.rescue.features.globalchat.entity.GlobalChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GlobalChatParticipantRepository extends JpaRepository<GlobalChatParticipant, Long> {
    // Find all participants (to know who to send notifications to)
    List<GlobalChatParticipant> findAll();

    // Check if a user is a participant
    boolean existsByUserId(Long userId);
}