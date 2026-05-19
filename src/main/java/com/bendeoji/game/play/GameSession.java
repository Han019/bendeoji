package com.bendeoji.game.play;

import com.bendeoji.game.config.GameProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

class GameSession {

    private final UUID id;
    private final Instant startedAt;
    private final Instant expiresAt;
    private final List<SpawnEvent> schedule;
    private final GameProperties properties;
    private final Set<Integer> hitEventIds = new HashSet<>();
    private boolean finished;

    GameSession(UUID id, Instant startedAt, Instant expiresAt, List<SpawnEvent> schedule, GameProperties properties) {
        this.id = id;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
        this.schedule = List.copyOf(schedule);
        this.properties = properties;
    }

    UUID id() {
        return id;
    }

    Instant expiresAt() {
        return expiresAt;
    }

    List<SpawnEvent> schedule() {
        return schedule;
    }

    synchronized GameStateResponse hit(int eventId, Instant now) {
        if (finished) {
            return state(false, now, "이미 종료된 게임입니다.");
        }
        if (now.isAfter(expiresAt)) {
            return state(false, now, "만료된 게임입니다.");
        }

        Optional<SpawnEvent> event = schedule.stream()
                .filter(candidate -> candidate.id() == eventId)
                .findFirst();
        if (event.isEmpty()) {
            return state(false, now, "존재하지 않는 두더지입니다.");
        }
        if (hitEventIds.contains(eventId)) {
            return state(false, now, "이미 잡은 두더지입니다.");
        }

        long elapsedMillis = elapsedMillis(now);
        if (!event.get().isVisibleAt(elapsedMillis, properties.getHitLeniencyMillis())) {
            return state(false, now, "두더지가 보이는 시간이 아닙니다.");
        }

        hitEventIds.add(eventId);
        return state(true, now, "hit");
    }

    synchronized GameStateResponse state(Instant now) {
        return state(true, now, "ok");
    }

    synchronized FinishStats finish(Instant now) {
        finished = true;
        long durationMillis = Math.min(properties.durationMillis(), Math.max(0, elapsedMillis(now)));
        return new FinishStats(score(), hitEventIds.size(), missesAt(now), durationMillis);
    }

    synchronized boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    private GameStateResponse state(boolean accepted, Instant now, String message) {
        return new GameStateResponse(accepted, score(), hitEventIds.size(), missesAt(now), message);
    }

    private int score() {
        return hitEventIds.size() * properties.getPointsPerHit();
    }

    private int missesAt(Instant now) {
        long elapsedMillis = elapsedMillis(now);
        return (int) schedule.stream()
                .filter(event -> event.isMissedAt(elapsedMillis))
                .filter(event -> !hitEventIds.contains(event.id()))
                .count();
    }

    private long elapsedMillis(Instant now) {
        return Duration.between(startedAt, now).toMillis();
    }

    record FinishStats(
            int score,
            int hits,
            int misses,
            long durationMillis
    ) {
    }
}
