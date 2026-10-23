package com.communex.games;

import com.communex.common.ActivityLogService;
import com.communex.games.dto.GameScoreRequest;
import com.communex.games.dto.GameScoreResponse;
import com.communex.users.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GameService {

    private final GameScoreRepository scores;
    private final UserRepository users;
    private final ActivityLogService activity;

    // Constructor
    public GameService(
            GameScoreRepository scores,
            UserRepository users,
            ActivityLogService activity
    ) {
        this.scores = scores;
        this.users = users;
        this.activity = activity;
    }

    // Submit and save a game score
    @Transactional
    public GameScoreResponse submit(
            String email,
            GameScoreRequest request
    ) {
        // Normalize the game identifier
        String game = request.game()
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]", "");

        // Validate the requested game
        Set<String> supportedGames = Set.of(
                "tic",
                "memory",
                "quiz",
                "hanoi",
                "sliding",
                "word",
                "snake",
                "connect-four",
                "sudoku",
                "maze",
                "battle-royale",
                "zombie-survival",
                "neon-racer",
                "dungeon-run",
                "tower-defense",
                "space-survivor",
                "shadow-heist",
                "cyber-runner",
                "arena-tactics",
                "resource-frontier"
        );

        if (!supportedGames.contains(game)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown game"
            );
        }

        // Find the user submitting the score
        var user = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        // Create the game score record
        GameScore score = new GameScore();

        score.setUser(user);
        score.setGame(game);
        score.setScore(request.score());
        score.setDurationSeconds(request.durationSeconds());

        // Persist the score
        score = scores.save(score);

        // Record the score submission in the activity log
        activity.record(
                email,
                "GAME_SCORE_SUBMITTED",
                "GAME",
                score.getId(),
                game + " score: " + request.score()
        );

        return GameScoreResponse.from(score);
    }

    // Retrieve the current user's latest scores
    @Transactional(readOnly = true)
    public List<GameScoreResponse> mine(String email) {

        return scores
                .findTop100ByUserEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(GameScoreResponse::from)
                .toList();
    }

    // Retrieve the leaderboard for a specific game
    @Transactional(readOnly = true)
    public List<GameScoreResponse> leaderboard(String game) {

        String normalizedGame = game.toLowerCase(Locale.ROOT);

        return scores
                .findTop100ByGameOrderByScoreDescCreatedAtAsc(
                        normalizedGame
                )
                .stream()
                .map(GameScoreResponse::from)
                .toList();
    }
}