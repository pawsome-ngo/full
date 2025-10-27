package com.pawsome.rescue.features.user;

import com.pawsome.rescue.features.user.dto.LeaderboardDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/standings")
public class StandingsController {

    @Autowired
    private StandingsService standingsService;

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardDto>> getLeaderboard() {
        List<LeaderboardDto> leaderboard = standingsService.getLeaderboard();
        return ResponseEntity.ok(leaderboard);
    }
}