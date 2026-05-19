package com.bendeoji.game.play;

import jakarta.validation.constraints.Min;

public record HitRequest(
        @Min(0)
        long clientHitAtMillis
) {
}
