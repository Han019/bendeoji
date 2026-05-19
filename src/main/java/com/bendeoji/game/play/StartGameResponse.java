package com.bendeoji.game.play;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StartGameResponse(
        UUID sessionId,
        GameConfigResponse config,
        List<SpawnEventResponse> schedule,
        Instant expiresAt
) {
}
