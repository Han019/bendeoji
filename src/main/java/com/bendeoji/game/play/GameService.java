package com.bendeoji.game.play;

import com.bendeoji.game.api.ApiException;
import com.bendeoji.game.config.GameProperties;
import com.bendeoji.game.leaderboard.LeaderboardSaveResult;
import com.bendeoji.game.leaderboard.LeaderboardService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class GameService {

    private static final long START_DELAY_MILLIS = 900L;
    private static final long SESSION_GRACE_MILLIS = 20_000L;

    private final GameProperties properties;
    private final LeaderboardService leaderboardService;
    private final Map<UUID, GameSession> sessions = new ConcurrentHashMap<>();

    public GameService(GameProperties properties, LeaderboardService leaderboardService) {
        this.properties = properties;
        this.leaderboardService = leaderboardService;
    }

    public GameConfigResponse config() {
        return new GameConfigResponse(
                properties.getRows(),
                properties.totalHoles(),
                properties.getDurationSeconds(),
                properties.getSpawnIntervalMillis(),
                properties.getVisibleMillis(),
                properties.getPointsPerHit(),
                new GameConfigResponse.AssetPaths(
                        "/assets/normal_benti.png",
                        "/assets/hit_benti.png",
                        "/assets/mallet-cursor.svg",
                        "/assets/mallet-cursor-down.svg",
                        "/assets/hit.wav"
                )
        );
    }

    public StartGameResponse start() {
        Instant now = Instant.now();
        UUID sessionId = UUID.randomUUID();
        List<SpawnEvent> schedule = generateSchedule();
        Instant expiresAt = now.plusMillis(properties.durationMillis() + SESSION_GRACE_MILLIS);
        GameSession session = new GameSession(sessionId, now, expiresAt, schedule, properties);
        sessions.put(sessionId, session);

        List<SpawnEventResponse> responseSchedule = schedule.stream()
                .map(SpawnEventResponse::from)
                .toList();
        return new StartGameResponse(sessionId, config(), responseSchedule, expiresAt);
    }

    public GameStateResponse hit(UUID sessionId, int eventId) {
        return session(sessionId).hit(eventId, Instant.now());
    }

    public GameStateResponse state(UUID sessionId) {
        return session(sessionId).state(Instant.now());
    }

    public FinishGameResponse finish(UUID sessionId, String playerName) {
        GameSession session = session(sessionId);
        GameSession.FinishStats stats = session.finish(Instant.now());
        LeaderboardSaveResult saveResult = leaderboardService.recordScore(
                playerName,
                stats.score(),
                stats.hits(),
                stats.misses(),
                stats.durationMillis()
        );
        sessions.remove(sessionId);
        return new FinishGameResponse(
                saveResult.entry(),
                new GameStateResponse(true, stats.score(), stats.hits(), stats.misses(), "finished"),
                saveResult.updated()
        );
    }

    @Scheduled(fixedDelay = 60_000)
    void removeExpiredSessions() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private GameSession session(UUID sessionId) {
        GameSession session = sessions.get(sessionId);
        if (session == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "게임 세션을 찾을 수 없습니다.");
        }
        return session;
    }

    private List<SpawnEvent> generateSchedule() {
        List<SpawnEvent> schedule = new ArrayList<>();
        int previousHole = -1;
        int eventId = 1;

        for (long appearAt = START_DELAY_MILLIS;
             appearAt + properties.getVisibleMillis() <= properties.durationMillis();
             appearAt += properties.getSpawnIntervalMillis()) {
            int hole = randomHole(previousHole);
            schedule.add(new SpawnEvent(eventId++, hole, appearAt, properties.getVisibleMillis()));
            previousHole = hole;
        }
        return schedule;
    }

    private int randomHole(int previousHole) {
        int totalHoles = properties.totalHoles();
        if (totalHoles <= 1) {
            return 0;
        }

        int hole;
        do {
            hole = ThreadLocalRandom.current().nextInt(totalHoles);
        } while (hole == previousHole);
        return hole;
    }
}
