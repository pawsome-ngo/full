package com.pawsome.rescue.features.notification;

import com.pawsome.rescue.auth.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // Add import
import org.springframework.data.jpa.repository.Query; // Add import
import java.util.List;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    List<PushSubscription> findByUser(User user);


    boolean existsByEndpoint(String endpoint);

    @Transactional void deleteByEndpoint(String endpoint);


    // --- THIS IS THE KEY METHOD ---
    @Transactional // Add @Transactional for the delete operation
    @Modifying     // Add @Modifying because this changes data
    void deleteByUser(User userToDelete);
    // --- END MODIFICATION ---

    // --- ADD THIS METHOD ---
    @Modifying
    @Query(value = "ALTER TABLE push_subscriptions AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
    // --- END ADD ---
}