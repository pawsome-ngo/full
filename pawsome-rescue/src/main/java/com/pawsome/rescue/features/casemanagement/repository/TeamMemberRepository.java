package com.pawsome.rescue.features.casemanagement.repository;

import com.pawsome.rescue.features.casemanagement.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // Add import
import org.springframework.data.jpa.repository.Query; // Add import
import java.util.List;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByUserId(Long userId);

    // --- ADD THIS METHOD ---
    @Modifying
    @Query(value = "ALTER TABLE team_members AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
    // --- END ADD ---
}