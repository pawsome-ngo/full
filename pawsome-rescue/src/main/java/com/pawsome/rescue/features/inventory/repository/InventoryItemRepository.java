package com.pawsome.rescue.features.inventory.repository;

import com.pawsome.rescue.features.inventory.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // Add import
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    @Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.quantity < i.lowStockThreshold")
    int countLowStockItems();

    boolean existsByCategoryId(Long categoryId);

    // --- ADD THIS METHOD ---
    @Modifying
    @Query(value = "ALTER TABLE inventory_items AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
    // --- END ADD ---
}