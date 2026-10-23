package com.communex.games;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameScoreRepository
        extends JpaRepository<GameScore, Long> {

    // Retrieve the latest 100 scores for a specific user
    List<GameScore> findTop100ByUserEmailOrderByCreatedAtDesc(
            String email
    );

    // Retrieve the top 100 scores for a specific game
    // Highest score first; earlier submission first when scores are equal
    List<GameScore> findTop100ByGameOrderByScoreDescCreatedAtAsc(
            String game
    );

    // Count the number of score records for a specific game
    long countByGame(String game);
}