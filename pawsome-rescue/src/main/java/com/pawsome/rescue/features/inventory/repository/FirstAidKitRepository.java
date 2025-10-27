package com.pawsome.rescue.features.inventory.repository;

import com.pawsome.rescue.features.inventory.entity.FirstAidKit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // Add import
import org.springframework.data.jpa.repository.Query; // Add import

import java.util.List;
import java.util.Optional;

public interface FirstAidKitRepository extends JpaRepository<FirstAidKit, Long> {
    Optional<FirstAidKit> findByUserId(Long userId);

    List<FirstAidKit> findByUserIdIn(List<Long> userIds);

    // --- ADD THIS METHOD ---
    @Modifying
    @Query(value = "ALTER TABLE first_aid_kits AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
    // --- END ADD ---
}