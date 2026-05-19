package com.bendeoji.game.play;

record SpawnEvent(
        int id,
        int holeIndex,
        long appearAtMillis,
        long visibleMillis
) {

    boolean isVisibleAt(long elapsedMillis, long leniencyMillis) {
        return elapsedMillis >= appearAtMillis - leniencyMillis
                && elapsedMillis <= appearAtMillis + visibleMillis + leniencyMillis;
    }

    boolean isMissedAt(long elapsedMillis) {
        return elapsedMillis > appearAtMillis + visibleMillis;
    }
}
