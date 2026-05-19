package com.bendeoji.game.leaderboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Leaderboard", description = "Shared score ranking APIs")
@RestController
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @Operation(summary = "Get leaderboard", description = "Returns top scores ordered by score, hits, duration, and submission time.")
    @GetMapping("/api/leaderboard")
    public List<LeaderboardEntry> leaderboard(@RequestParam(defaultValue = "10") int limit) {
        return leaderboardService.top(limit);
    }
}
