package com.communex.games.dto;

import com.communex.games.GameScore;
import java.time.LocalDateTime;

public record GameScoreResponse(

        Long id,
        String game,
        int score,
        int durationSeconds,
        String playerName,
        LocalDateTime createdAt

) {

    // Convert a GameScore entity into a response DTO
    public static GameScoreResponse from(GameScore gameScore) {

        return new GameScoreResponse(
                gameScore.getId(),
                gameScore.getGame(),
                gameScore.getScore(),
                gameScore.getDurationSeconds(),
                gameScore.getUser().getName(),
                gameScore.getCreatedAt()
        );
    }
}