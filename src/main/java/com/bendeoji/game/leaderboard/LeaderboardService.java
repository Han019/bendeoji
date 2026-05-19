package com.bendeoji.game.leaderboard;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardService {

    private static final String FALLBACK_PLAYER_NAME = "Anonymous";

    private final LeaderboardRepository leaderboardRepository;

    public LeaderboardService(LeaderboardRepository leaderboardRepository) {
        this.leaderboardRepository = leaderboardRepository;
    }

    public LeaderboardSaveResult recordScore(String playerName, int score, int hits, int misses, long durationMillis) {
        return leaderboardRepository.saveBest(cleanPlayerName(playerName), score, hits, misses, durationMillis);
    }

    public List<LeaderboardEntry> top(int limit) {
        return leaderboardRepository.findTop(limit);
    }

    private String cleanPlayerName(String playerName) {
        String cleaned = playerName == null ? "" : playerName.trim().replaceAll("\\s+", " ");
        if (cleaned.isBlank()) {
            return FALLBACK_PLAYER_NAME;
        }
        return cleaned.length() > 20 ? cleaned.substring(0, 20) : cleaned;
    }
}
