package com.pawsome.rescue.features.inventory.repository;

import com.pawsome.rescue.features.inventory.entity.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // Add import
import org.springframework.data.jpa.repository.Query; // Add import
import java.util.List;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    List<InventoryLog> findTop10ByOrderByTimestampDesc();
    void deleteAllByInventoryItemId(Long inventoryItemId);

    // --- ADD THIS METHOD ---
    @Modifying
    @Query(value = "ALTER TABLE inventory_logs AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
    // --- END ADD ---
}