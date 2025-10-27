package com.pawsome.rescue.features.notification;

import com.pawsome.rescue.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientUserOrderByCreatedAtDesc(User recipientUser);
    List<Notification> findByRecipientUserAndIsReadFalseOrderByCreatedAtDesc(User recipientUser);
    Optional<Notification> findByIdAndRecipientUser(Long id, User recipientUser);
    List<Notification> findByRecipientUserAndIsReadFalse(User recipientUser);
    @Transactional @Modifying void deleteByRecipientUser(User recipientUser);
    @Transactional @Modifying void deleteByTriggeringUser(User triggeringUser);
    @Transactional @Modifying @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    int deleteByCreatedAtBefore(LocalDateTime cutoffDate);

    // --- ADD THIS METHOD ---
    @Modifying
    @Query(value = "ALTER TABLE notifications AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
    // --- END ADD ---
}