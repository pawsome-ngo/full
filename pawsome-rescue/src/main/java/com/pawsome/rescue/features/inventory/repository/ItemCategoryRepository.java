package com.pawsome.rescue.features.inventory.repository;

import com.pawsome.rescue.features.inventory.entity.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // Add import
import org.springframework.data.jpa.repository.Query; // Add import

public interface ItemCategoryRepository extends JpaRepository<ItemCategory, Long> {
    // --- ADD THIS METHOD ---
    @Modifying
    @Query(value = "ALTER TABLE item_categories AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
    // --- END ADD ---
}