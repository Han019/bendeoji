package com.bendeoji.game.leaderboard;

import java.time.Instant;

public record LeaderboardEntry(
        long id,
        String playerName,
        int score,
        int hits,
        int misses,
        long durationMillis,
        Instant createdAt
) {
}
