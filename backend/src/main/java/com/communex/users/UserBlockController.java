package com.communex.users;

import com.communex.common.ActivityLogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users/blocks")
public class UserBlockController {

    private final UserBlockRepository blocks;
    private final UserRepository users;
    private final ActivityLogService activity;

    // Constructor
    public UserBlockController(
            UserBlockRepository blocks,
            UserRepository users,
            ActivityLogService activity
    ) {
        this.blocks = blocks;
        this.users = users;
        this.activity = activity;
    }

    // Retrieve the currently authenticated user
    private User current(Authentication authentication) {
        if (authentication.getPrincipal() instanceof User user) {
            return users.findById(user.getId()).orElseThrow();
        }

        return users.findByEmail(authentication.getName()).orElseThrow();
    }

    // Get the list of users blocked by the current user
    @GetMapping
    public List<Map<String, Object>> list(Authentication authentication) {
        User me = current(authentication);

        return blocks.findByBlockerEmailOrderByCreatedAtDesc(me.getEmail())
                .stream()
                .map(block -> Map.<String, Object>of(
                        "id", block.getId(),
                        "email", block.getBlocked().getEmail(),
                        "name", block.getBlocked().getName(),
                        "createdAt", block.getCreatedAt()
                ))
                .toList();
    }

    // Block another user
    @PostMapping
    public Map<String, String> block(
            Authentication authentication,
            @Valid @RequestBody BlockRequest request
    ) {
        User me = current(authentication);

        User target = users.findByEmail(
                        request.email().trim().toLowerCase()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        if (me.getId().equals(target.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot block yourself"
            );
        }

        if (!blocks.existsByBlockerIdAndBlockedId(
                me.getId(),
                target.getId()
        )) {
            UserBlock userBlock = new UserBlock();
            userBlock.setBlocker(me);
            userBlock.setBlocked(target);

            blocks.save(userBlock);

            activity.record(
                    me.getEmail(),
                    "USER_BLOCKED",
                    "USER",
                    target.getId(),
                    "Blocked a user"
            );
        }

        return Map.of("message", "User blocked");
    }

    // Unblock a user
    @DeleteMapping("/{email}")
    public Map<String, String> unblock(
            Authentication authentication,
            @PathVariable String email
    ) {
        User me = current(authentication);

        User target = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        blocks.deleteByBlockerIdAndBlockedId(
                me.getId(),
                target.getId()
        );

        activity.record(
                me.getEmail(),
                "USER_UNBLOCKED",
                "USER",
                target.getId(),
                "Unblocked a user"
        );

        return Map.of("message", "User unblocked");
    }

    // Request DTO for blocking a user
    public record BlockRequest(
            @NotBlank
            @Email
            String email
    ) {
    }
}