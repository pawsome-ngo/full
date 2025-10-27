package com.pawsome.rescue.features.inventory.repository;

import com.pawsome.rescue.features.inventory.entity.Requisition;
import com.pawsome.rescue.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // Add import
import org.springframework.data.jpa.repository.Query; // Add import
import java.util.List;

public interface RequisitionRepository extends JpaRepository<Requisition, Long> {
    List<Requisition> findByStatus(Requisition.RequestStatus status);
    long countByStatus(Requisition.RequestStatus status);
    List<Requisition> findByUserAndStatusIn(User user, List<Requisition.RequestStatus> statuses);

    // --- ADD THIS METHOD ---
    @Modifying
    @Query(value = "ALTER TABLE requisitions AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
    // --- END ADD ---
}