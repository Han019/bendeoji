package com.bendeoji.game.leaderboard;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class LeaderboardRepository {

    private static final RowMapper<LeaderboardEntry> ROW_MAPPER = (rs, rowNum) -> new LeaderboardEntry(
            rs.getLong("id"),
            rs.getString("player_name"),
            rs.getInt("score"),
            rs.getInt("hits"),
            rs.getInt("misses"),
            rs.getLong("duration_millis"),
            Instant.parse(rs.getString("created_at"))
    );

    private final JdbcTemplate jdbcTemplate;

    public LeaderboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        initialize();
    }

    public LeaderboardSaveResult saveBest(String playerName, int score, int hits, int misses, long durationMillis) {
        Optional<LeaderboardEntry> existing = findByPlayerName(playerName);
        if (existing.isPresent() && score <= existing.get().score()) {
            return new LeaderboardSaveResult(existing.get(), false);
        }

        Instant createdAt = Instant.now();

        jdbcTemplate.update("""
                INSERT INTO leaderboard_entries
                    (player_name, score, hits, misses, duration_millis, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(player_name) DO UPDATE SET
                    score = excluded.score,
                    hits = excluded.hits,
                    misses = excluded.misses,
                    duration_millis = excluded.duration_millis,
                    created_at = excluded.created_at
                WHERE excluded.score > leaderboard_entries.score
                """,
                playerName,
                score,
                hits,
                misses,
                durationMillis,
                createdAt.toString()
        );

        return new LeaderboardSaveResult(findByPlayerName(playerName).orElseThrow(), true);
    }

    public List<LeaderboardEntry> findTop(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return jdbcTemplate.query("""
                SELECT id, player_name, score, hits, misses, duration_millis, created_at
                FROM leaderboard_entries
                ORDER BY score DESC, hits DESC, duration_millis ASC, created_at ASC
                LIMIT ?
                """, ROW_MAPPER, safeLimit);
    }

    private void initialize() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS leaderboard_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_name TEXT NOT NULL,
                    score INTEGER NOT NULL,
                    hits INTEGER NOT NULL,
                    misses INTEGER NOT NULL,
                    duration_millis INTEGER NOT NULL,
                    created_at TEXT NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                DELETE FROM leaderboard_entries
                WHERE id IN (
                    SELECT id
                    FROM (
                        SELECT
                            id,
                            ROW_NUMBER() OVER (
                                PARTITION BY player_name
                                ORDER BY score DESC, hits DESC, duration_millis ASC, created_at ASC, id ASC
                            ) AS row_rank
                        FROM leaderboard_entries
                    )
                    WHERE row_rank > 1
                )
                """);
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS ux_leaderboard_entries_player_name
                ON leaderboard_entries (player_name)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_leaderboard_entries_rank
                ON leaderboard_entries (score DESC, hits DESC, duration_millis ASC, created_at ASC)
                """);
    }

    private Optional<LeaderboardEntry> findByPlayerName(String playerName) {
        List<LeaderboardEntry> rows = jdbcTemplate.query("""
                SELECT id, player_name, score, hits, misses, duration_millis, created_at
                FROM leaderboard_entries
                WHERE player_name = ?
                """, ROW_MAPPER, playerName);
        return rows.stream().findFirst();
    }
}
