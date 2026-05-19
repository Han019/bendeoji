package com.bendeoji.game.play;

import java.util.List;

public record GameConfigResponse(
        List<Integer> rows,
        int totalHoles,
        int durationSeconds,
        int spawnIntervalMillis,
        int visibleMillis,
        int pointsPerHit,
        AssetPaths assets
) {

    public record AssetPaths(
            String mole,
            String hammerCursor,
            String hammerCursorDown,
            String hitSound
    ) {
    }
}
