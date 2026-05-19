package com.bendeoji.game.play;

import com.bendeoji.game.leaderboard.LeaderboardEntry;

public record FinishGameResponse(
        LeaderboardEntry entry,
        GameStateResponse finalState,
        boolean leaderboardUpdated
) {
}
