package com.communex.games.dto;

import com.communex.games.GameProgress;
import java.time.LocalDateTime;

public record GameProgressResponse(
    String game,
    int currentLevel,
    int highestUnlockedLevel,
    long totalScore,
    int victories,
    LocalDateTime updatedAt
) {
    public static GameProgressResponse from(GameProgress progress) {
        return new GameProgressResponse(
            progress.getGame(), progress.getCurrentLevel(), progress.getHighestUnlockedLevel(),
            progress.getTotalScore(), progress.getVictories(), progress.getUpdatedAt()
        );
    }
}
