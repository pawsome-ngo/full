package com.pawsome.rescue.features.inventory.repository;

import com.pawsome.rescue.features.inventory.entity.RequisitionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // Add import
import org.springframework.data.jpa.repository.Query; // Add import

public interface RequisitionItemRepository extends JpaRepository<RequisitionItem, Long> {
    void deleteAllByInventoryItemId(Long inventoryItemId);

    // --- ADD THIS METHOD ---
    @Modifying
    @Query(value = "ALTER TABLE requisition_items AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
    // --- END ADD ---
}