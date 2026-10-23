package com.communex.games;

import com.communex.games.dto.GameProgressRequest;
import com.communex.games.dto.GameProgressResponse;
import com.communex.users.User;
import com.communex.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class GameProgressService {
    private static final Set<String> SUPPORTED_GAMES = Set.of(
        "battle-royale", "zombie-survival", "neon-racer", "dungeon-run", "tower-defense",
        "space-survivor", "shadow-heist", "cyber-runner", "arena-tactics", "resource-frontier"
    );

    private final GameProgressRepository progressRepository;
    private final UserRepository users;

    public GameProgressService(GameProgressRepository progressRepository, UserRepository users) {
        this.progressRepository = progressRepository;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<GameProgressResponse> list(String email) {
        return progressRepository.findAllByUserEmailOrderByGameAsc(email).stream()
            .map(GameProgressResponse::from).toList();
    }

    @Transactional
    public GameProgressResponse save(String email, String requestedGame, GameProgressRequest request) {
        String game = normalizeGame(requestedGame);
        GameProgress progress = progressRepository.findByUserEmailAndGame(email, game).orElseGet(() -> {
            User user = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            GameProgress fresh = new GameProgress();
            fresh.setUser(user);
            fresh.setGame(game);
            return fresh;
        });

        int highest = Math.max(progress.getHighestUnlockedLevel(), request.highestUnlockedLevel());
        int current = Math.min(highest, request.currentLevel());
        progress.setHighestUnlockedLevel(Math.max(1, Math.min(50, highest)));
        progress.setCurrentLevel(Math.max(1, Math.min(50, current)));
        progress.setTotalScore(Math.max(progress.getTotalScore(), request.totalScore()));
        progress.setVictories(Math.max(progress.getVictories(), request.victories()));
        return GameProgressResponse.from(progressRepository.save(progress));
    }

    @Transactional
    public void reset(String email) {
        progressRepository.deleteByUserEmail(email);
    }

    private String normalizeGame(String value) {
        String game = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_GAMES.contains(game)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown arcade game");
        }
        return game;
    }
}
