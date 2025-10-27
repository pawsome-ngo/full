package com.pawsome.rescue.features.user.repository;

import com.pawsome.rescue.features.user.entity.UserStats;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserStatsRepository extends JpaRepository<UserStats, Long> {
    // Method to fetch all stats ordered by points for the leaderboard
    List<UserStats> findAllByOrderByPointsDesc();
}