package com.bendeoji.game.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.game")
public class GameProperties {

    private final List<Integer> rows = List.of(8, 7, 8);

    @Min(5)
    @Max(120)
    private int durationSeconds = 30;

    @Min(250)
    @Max(5_000)
    private int spawnIntervalMillis = 700;

    @Min(250)
    @Max(5_000)
    private int visibleMillis = 950;

    @Min(1)
    @Max(1_000)
    private int pointsPerHit = 10;

    @Min(0)
    @Max(2_000)
    private int hitLeniencyMillis = 450;

    public List<Integer> getRows() {
        return rows;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public int getSpawnIntervalMillis() {
        return spawnIntervalMillis;
    }

    public void setSpawnIntervalMillis(int spawnIntervalMillis) {
        this.spawnIntervalMillis = spawnIntervalMillis;
    }

    public int getVisibleMillis() {
        return visibleMillis;
    }

    public void setVisibleMillis(int visibleMillis) {
        this.visibleMillis = visibleMillis;
    }

    public int getPointsPerHit() {
        return pointsPerHit;
    }

    public void setPointsPerHit(int pointsPerHit) {
        this.pointsPerHit = pointsPerHit;
    }

    public int getHitLeniencyMillis() {
        return hitLeniencyMillis;
    }

    public void setHitLeniencyMillis(int hitLeniencyMillis) {
        this.hitLeniencyMillis = hitLeniencyMillis;
    }

    public int totalHoles() {
        return rows.stream().mapToInt(Integer::intValue).sum();
    }

    public long durationMillis() {
        return durationSeconds * 1_000L;
    }
}
