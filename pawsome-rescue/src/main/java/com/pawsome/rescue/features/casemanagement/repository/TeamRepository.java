package com.pawsome.rescue.features.casemanagement.repository;

import com.pawsome.rescue.features.casemanagement.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    // This query finds teams that have the exact same set of members.
    @Query("SELECT t FROM Team t JOIN t.teamMembers tm GROUP BY t HAVING COUNT(tm.user.id) = ?1 AND SUM(CASE WHEN tm.user.id IN ?2 THEN 1 ELSE 0 END) = ?1")
    List<Team> findTeamsByExactMembers(long memberCount, List<Long> userIds);

    // --- ADD THIS METHOD ---
    @Modifying
    @Query(value = "ALTER TABLE teams AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement(); // Ensure this method exists
}