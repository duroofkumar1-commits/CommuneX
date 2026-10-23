package com.communex.users;

import com.communex.users.dto.AuthResponse;
import com.communex.users.dto.GoogleLoginRequest;
import com.communex.users.dto.LoginRequest;
import com.communex.users.dto.RegisterRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AccountSecurityService accountSecurity;
    private final GoogleAuthService googleAuthService;

    // Constructor
    public AuthController(
            AuthService authService,
            AccountSecurityService accountSecurity,
            GoogleAuthService googleAuthService
    ) {
        this.authService = authService;
        this.accountSecurity = accountSecurity;
        this.googleAuthService = googleAuthService;
    }

    // Register a new user
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        User user = authService.register(
                request.name().trim(),
                request.email().trim().toLowerCase(),
                request.password()
        );

        // Send verification email when the account is unverified
        if (!user.isEmailVerified()) {
            accountSecurity.sendVerification(user);
        }

        AuthResponse response = new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getProvider(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Authenticate a user using email and password
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        User user = authService.login(
                request.email().trim().toLowerCase(),
                request.password()
        );

        String token = authService.generateToken(user);

        return ResponseEntity.ok(
                toResponse(user, token)
        );
    }

    // Authenticate a user using Google credentials
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        User user = googleAuthService.login(
                request.credential()
        );

        String token = authService.generateToken(user);

        return ResponseEntity.ok(
                toResponse(user, token)
        );
    }

    // Build a consistent authentication response
    private AuthResponse toResponse(
            User user,
            String token
    ) {
        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getProvider(),
                token
        );
    }
}