package com.communex.games;

import com.communex.games.dto.GameProgressRequest;
import com.communex.games.dto.GameProgressResponse;
import com.communex.users.User;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games/progress")
public class GameProgressController {
    private final GameProgressService service;

    public GameProgressController(GameProgressService service) {
        this.service = service;
    }

    private String currentEmail(Authentication authentication) {
        return authentication.getPrincipal() instanceof User user ? user.getEmail() : authentication.getName();
    }

    @GetMapping("/me")
    public List<GameProgressResponse> mine(Authentication authentication) {
        return service.list(currentEmail(authentication));
    }

    @PutMapping("/{game}")
    public GameProgressResponse save(
        Authentication authentication,
        @PathVariable String game,
        @Valid @RequestBody GameProgressRequest request
    ) {
        return service.save(currentEmail(authentication), game, request);
    }

    @DeleteMapping
    public void reset(Authentication authentication) {
        service.reset(currentEmail(authentication));
    }
}
