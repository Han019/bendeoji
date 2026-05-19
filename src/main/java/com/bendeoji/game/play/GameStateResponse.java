package com.bendeoji.game.play;

public record GameStateResponse(
        boolean accepted,
        int score,
        int hits,
        int misses,
        String message
) {
}
