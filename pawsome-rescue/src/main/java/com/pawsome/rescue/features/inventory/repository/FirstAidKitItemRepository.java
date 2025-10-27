package com.pawsome.rescue.features.inventory.repository;

import com.pawsome.rescue.features.inventory.entity.FirstAidKitItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // Add import
import org.springframework.data.jpa.repository.Query; // Add import
import java.util.List;

public interface FirstAidKitItemRepository extends JpaRepository<FirstAidKitItem, Long> {
    List<FirstAidKitItem> findByInventoryItemId(Long inventoryItemId);

    // --- ADD THIS METHOD ---
    @Modifying
    @Query(value = "ALTER TABLE first_aid_kit_items AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
    // --- END ADD ---
}