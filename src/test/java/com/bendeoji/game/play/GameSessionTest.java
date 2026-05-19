package com.bendeoji.game.play;

import static org.assertj.core.api.Assertions.assertThat;

import com.bendeoji.game.config.GameProperties;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GameSessionTest {

    @Test
    void acceptsVisibleHitOnlyOnce() {
        GameProperties properties = new GameProperties();
        GameSession session = new GameSession(
                UUID.randomUUID(),
                Instant.EPOCH,
                Instant.EPOCH.plusSeconds(40),
                List.of(new SpawnEvent(1, 3, 1_000, 900)),
                properties
        );

        GameStateResponse firstHit = session.hit(1, Instant.EPOCH.plusMillis(1_100));
        GameStateResponse duplicateHit = session.hit(1, Instant.EPOCH.plusMillis(1_200));

        assertThat(firstHit.accepted()).isTrue();
        assertThat(firstHit.score()).isEqualTo(10);
        assertThat(duplicateHit.accepted()).isFalse();
        assertThat(duplicateHit.score()).isEqualTo(10);
    }

    @Test
    void rejectsHitOutsideVisibleWindow() {
        GameProperties properties = new GameProperties();
        properties.setHitLeniencyMillis(0);
        GameSession session = new GameSession(
                UUID.randomUUID(),
                Instant.EPOCH,
                Instant.EPOCH.plusSeconds(40),
                List.of(new SpawnEvent(1, 3, 1_000, 900)),
                properties
        );

        GameStateResponse response = session.hit(1, Instant.EPOCH.plusMillis(2_100));

        assertThat(response.accepted()).isFalse();
        assertThat(response.score()).isZero();
        assertThat(response.misses()).isEqualTo(1);
    }
}
