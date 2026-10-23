package com.communex.users;

import com.communex.users.dto.ProfileUpdateRequest;
import com.communex.users.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository users;

    // Constructor
    public UserController(UserRepository users) {
        this.users = users;
    }

    // Get the current user's profile
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        User user = current(authentication);
        return toResponse(user);
    }

    // Update the current user's profile
    @PutMapping("/me")
    public UserResponse updateMe(
            Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        User user = current(authentication);

        user.setName(request.name().trim());

        user.setProfileImageUrl(
                request.profileImageUrl() == null
                        || request.profileImageUrl().isBlank()
                        ? null
                        : request.profileImageUrl().trim()
        );

        User updatedUser = users.save(user);

        return toResponse(updatedUser);
    }

    // USER role access
    @GetMapping("/user-only")
    @PreAuthorize("hasRole('USER')")
    public String userOnly() {
        return "USER role access granted!";
    }

    // VOLUNTEER role access
    @GetMapping("/volunteer-only")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public String volunteerOnly() {
        return "VOLUNTEER role access granted!";
    }

    // ADMIN role access
    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminOnly() {
        return "ADMIN role access granted!";
    }

    // Retrieve the authenticated user from the database
    private User current(Authentication authentication) {

        if (authentication.getPrincipal() instanceof User user) {
            return users.findById(user.getId())
                    .orElseThrow(() ->
                            new IllegalArgumentException("User not found")
                    );
        }

        return users.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );
    }

    // Convert User entity into a response DTO
    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getProvider(),
                user.getProfileImageUrl()
        );
    }
}