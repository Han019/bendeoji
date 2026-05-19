package com.bendeoji.game.play;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Game", description = "Game session and scoring APIs")
@RestController
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @Operation(summary = "Get game config", description = "Returns board layout, timing, score, and asset information.")
    @GetMapping("/api/game/config")
    public GameConfigResponse config() {
        return gameService.config();
    }

    @Operation(summary = "Start game", description = "Creates a server-side session and a random mole spawn schedule.")
    @PostMapping("/api/games")
    public StartGameResponse start() {
        return gameService.start();
    }

    @Operation(summary = "Get game state", description = "Returns the current server-calculated score for a session.")
    @GetMapping("/api/games/{sessionId}")
    public GameStateResponse state(@PathVariable UUID sessionId) {
        return gameService.state(sessionId);
    }

    @Operation(summary = "Hit mole", description = "Attempts to score a visible mole event. Score is calculated by the server.")
    @PostMapping("/api/games/{sessionId}/hits/{eventId}")
    public GameStateResponse hit(
            @PathVariable UUID sessionId,
            @PathVariable int eventId,
            @Valid @RequestBody HitRequest request
    ) {
        return gameService.hit(sessionId, eventId);
    }

    @Operation(summary = "Finish game", description = "Finishes a session and records the server-calculated score on the leaderboard.")
    @PostMapping("/api/games/{sessionId}/finish")
    public FinishGameResponse finish(
            @PathVariable UUID sessionId,
            @Valid @RequestBody FinishGameRequest request
    ) {
        return gameService.finish(sessionId, request.playerName());
    }
}
