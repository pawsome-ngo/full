package com.pawsome.rescue.features.user;

import com.pawsome.rescue.features.user.dto.LeaderboardDto;
import com.pawsome.rescue.features.user.entity.UserStats;
import com.pawsome.rescue.features.user.repository.UserStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class StandingsService {

    @Autowired
    private UserStatsRepository userStatsRepository;

    public List<LeaderboardDto> getLeaderboard() {
        List<UserStats> allStats = userStatsRepository.findAllByOrderByPointsDesc().stream()
                .toList();

        List<LeaderboardDto> leaderboard = new ArrayList<>();

        for (int i = 0; i < allStats.size(); i++) {
            UserStats stats = allStats.get(i);
            LeaderboardDto dto = new LeaderboardDto();
            dto.setRank(i + 1);
            dto.setId(stats.getId());
            dto.setFirstName(stats.getUser().getFirstName());
            dto.setPoints(stats.getPoints());
            dto.setHearts(stats.getHearts());
            dto.setDistanceTraveled(Math.round(stats.getDistanceTraveled() * 10.0) / 10.0); // Round to 1 decimal place
            dto.setCasesCompleted(stats.getCasesCompleted());
            leaderboard.add(dto);
        }
        return leaderboard;
    }
}