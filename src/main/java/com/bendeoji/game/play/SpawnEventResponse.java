package com.bendeoji.game.play;

public record SpawnEventResponse(
        int id,
        int holeIndex,
        long appearAtMillis,
        long visibleMillis
) {

    static SpawnEventResponse from(SpawnEvent event) {
        return new SpawnEventResponse(event.id(), event.holeIndex(), event.appearAtMillis(), event.visibleMillis());
    }
}
