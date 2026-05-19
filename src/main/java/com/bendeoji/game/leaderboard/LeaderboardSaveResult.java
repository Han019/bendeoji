package com.bendeoji.game.leaderboard;

public record LeaderboardSaveResult(
        LeaderboardEntry entry,
        boolean updated
) {
}
