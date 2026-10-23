package com.communex.games;

import com.communex.games.dto.GameScoreRequest;
import com.communex.games.dto.GameScoreResponse;
import com.communex.users.User;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService service;

    // Constructor
    public GameController(GameService service) {
        this.service = service;
    }

    // Retrieve the authenticated user's email
    private String email(Authentication authentication) {

        if (authentication.getPrincipal() instanceof User user) {
            return user.getEmail();
        }

        return authentication.getName();
    }

    // Submit a game score
    @PostMapping("/scores")
    public GameScoreResponse submit(
            Authentication authentication,
            @Valid @RequestBody GameScoreRequest request
    ) {
        return service.submit(
                email(authentication),
                request
        );
    }

    // Retrieve the current user's recent scores
    @GetMapping("/scores/me")
    public List<GameScoreResponse> mine(
            Authentication authentication
    ) {
        return service.mine(
                email(authentication)
        );
    }

    // Retrieve the leaderboard for a selected game
    @GetMapping("/leaderboard")
    public List<GameScoreResponse> leaderboard(
            @RequestParam(defaultValue = "tic") String game
    ) {
        return service.leaderboard(game);
    }
}