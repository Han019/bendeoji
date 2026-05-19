package com.bendeoji.game.play;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FinishGameRequest(
        @NotBlank
        @Size(max = 20)
        String playerName
) {
}
