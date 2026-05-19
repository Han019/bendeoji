package com.bendeoji.game.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

class LeaderboardRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void keepsOnlyBestScoreForSamePlayerName() {
        LeaderboardRepository repository = repository("best-score.sqlite");

        LeaderboardSaveResult first = repository.saveBest("player", 30, 3, 5, 30_000);
        LeaderboardSaveResult lower = repository.saveBest("player", 20, 2, 7, 30_000);
        LeaderboardSaveResult higher = repository.saveBest("player", 50, 5, 3, 28_000);

        assertThat(first.updated()).isTrue();
        assertThat(first.entry().score()).isEqualTo(30);
        assertThat(lower.updated()).isFalse();
        assertThat(lower.entry().score()).isEqualTo(30);
        assertThat(higher.updated()).isTrue();
        assertThat(higher.entry().score()).isEqualTo(50);

        List<LeaderboardEntry> rows = repository.findTop(10);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).playerName()).isEqualTo("player");
        assertThat(rows.get(0).score()).isEqualTo(50);
        assertThat(rows.get(0).hits()).isEqualTo(5);
    }

    private LeaderboardRepository repository(String fileName) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve(fileName));
        return new LeaderboardRepository(new JdbcTemplate(dataSource));
    }
}
