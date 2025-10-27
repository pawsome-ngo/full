package com.pawsome.rescue.features.casemanagement.repository;

import com.pawsome.rescue.features.casemanagement.entity.RescueCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface RescueCaseRepository extends JpaRepository<RescueCase, Long> {
    List<RescueCase> findByIsActiveTrue();

    // New method to find all active cases for a specific user ID
    @Query("SELECT rc FROM RescueCase rc JOIN rc.assignedTeam t JOIN t.teamMembers tm WHERE tm.user.id = ?1 AND rc.isActive = true")
    List<RescueCase> findActiveCasesByUserId(Long userId);

    Optional<RescueCase> findByIncidentIdAndIsActiveTrue(Long incidentId);
    // --- ADD THIS METHOD ---
    @Modifying
    @Query(value = "ALTER TABLE cases AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement(); // Ensure this method exists
    @Query("SELECT COUNT(rc) > 0 FROM RescueCase rc JOIN rc.assignedTeam t JOIN t.teamMembers tm WHERE tm.user.id = ?1 AND rc.isActive = true")
    boolean isUserInAnyActiveCase(Long userId);
}