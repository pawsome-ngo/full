package com.pawsome.rescue.features.chat.repository;

import com.pawsome.rescue.features.chat.entity.Participant;
import com.pawsome.rescue.features.chat.entity.Participant.ParticipantId;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ParticipantRepository extends JpaRepository<Participant, ParticipantId> {
    List<Participant> findByUserId(Long userId);
    List<Participant> findByUserIdIn(List<Long> userIds);

    // --- New method to delete all participants by chat group ID ---
    @Transactional
    @Modifying
    @Query("DELETE FROM Participant p WHERE p.id.chatId = ?1")
    void deleteAllByChatGroupId(String chatGroupId);

    // --- ADD THIS METHOD ---
    @Query("SELECT p FROM Participant p JOIN FETCH p.user WHERE p.id.chatId = ?1") // Fetch User eagerly
    List<Participant> findByChatGroupId(String chatId);
    // --- END ADD ---
}