package com.communex.users;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountSecurityService {

    private final UserRepository users;
    private final AccountActionTokenRepository tokens;
    private final EmailDeliveryService email;
    private final PasswordEncoder passwords;
    private final String frontendUrl;

    // Constructor
    public AccountSecurityService(
            UserRepository users,
            AccountActionTokenRepository tokens,
            EmailDeliveryService email,
            PasswordEncoder passwords,
            @Value("${communex.frontend-url:http://localhost:5173}")
            String frontendUrl
    ) {
        this.users = users;
        this.tokens = tokens;
        this.email = email;
        this.passwords = passwords;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    // Send email verification link
    @Transactional
    public void sendVerification(User user) {

        if (user.isEmailVerified()) {
            return;
        }

        if (!email.isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Email delivery is not configured. Set MAIL_HOST and mail credentials before requiring email verification."
            );
        }

        sendToken(
                user,
                AccountTokenPurpose.EMAIL_VERIFICATION,
                24,
                "Verify your CommuneX email",
                "/verify-email?token="
        );
    }

    // Verify email using the supplied token
    @Transactional
    public void verifyEmail(String rawToken) {

        AccountActionToken token = validToken(
                rawToken,
                AccountTokenPurpose.EMAIL_VERIFICATION
        );

        token.setConsumedAt(LocalDateTime.now());
        token.getUser().setEmailVerified(true);

        users.save(token.getUser());
        tokens.save(token);
    }

    // Request password reset without revealing whether an account exists
    @Transactional
    public void requestPasswordReset(String rawEmail) {

        if (!email.isConfigured() || rawEmail == null || rawEmail.isBlank()) {
            return;
        }

        Optional<User> user = users.findByEmail(
                rawEmail.trim().toLowerCase()
        );

        user.ifPresent(existingUser ->
                sendToken(
                        existingUser,
                        AccountTokenPurpose.PASSWORD_RESET,
                        15,
                        "Reset your CommuneX password",
                        "/reset-password?token="
                )
        );
    }

    // Reset password and invalidate outstanding reset tokens
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {

        AccountActionToken token = validToken(
                rawToken,
                AccountTokenPurpose.PASSWORD_RESET
        );

        User user = token.getUser();

        user.setPasswordHash(passwords.encode(newPassword));
        users.save(user);

        token.setConsumedAt(LocalDateTime.now());
        tokens.save(token);

        tokens.findByUserIdAndPurposeAndConsumedAtIsNull(
                user.getId(),
                AccountTokenPurpose.PASSWORD_RESET
        ).forEach(otherToken -> {
            otherToken.setConsumedAt(LocalDateTime.now());
            tokens.save(otherToken);
        });
    }

    // Resend email verification link
    @Transactional
    public void resendVerification(String rawEmail) {

        if (!email.isConfigured() || rawEmail == null || rawEmail.isBlank()) {
            return;
        }

        users.findByEmail(rawEmail.trim().toLowerCase())
                .filter(user -> !user.isEmailVerified())
                .ifPresent(user ->
                        sendToken(
                                user,
                                AccountTokenPurpose.EMAIL_VERIFICATION,
                                24,
                                "Verify your CommuneX email",
                                "/verify-email?token="
                        )
                );
    }

    // Generate, store and email an account action token
    private void sendToken(
            User user,
            AccountTokenPurpose purpose,
            int hoursOrMinutes,
            String subject,
            String path
    ) {

        // Invalidate previous unused tokens for the same purpose
        tokens.findByUserIdAndPurposeAndConsumedAtIsNull(
                user.getId(),
                purpose
        ).forEach(oldToken -> {
            oldToken.setConsumedAt(LocalDateTime.now());
            tokens.save(oldToken);
        });

        // Generate a random token
        String raw = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");

        // Create the token entity
        AccountActionToken token = new AccountActionToken();

        token.setUser(user);
        token.setPurpose(purpose);
        token.setTokenHash(hash(raw));

        // Set token expiration time
        token.setExpiresAt(
                LocalDateTime.now().plusMinutes(
                        purpose == AccountTokenPurpose.PASSWORD_RESET
                                ? hoursOrMinutes
                                : hoursOrMinutes * 60L
                )
        );

        tokens.save(token);

        // Build the action link
        String link = frontendUrl + path + raw;

        String duration = purpose == AccountTokenPurpose.PASSWORD_RESET
                ? "15 minutes"
                : "24 hours";

        String body = "Hello " + user.getName()
                + ",\n\nUse this one-time link within "
                + duration + ":\n"
                + link
                + "\n\nIf you did not request this, you can ignore this email."
                + "\n\nCommuneX";

        // Send the email
        try {
            email.send(user.getEmail(), subject, body);
        } catch (RuntimeException ex) {

            token.setConsumedAt(LocalDateTime.now());
            tokens.save(token);

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Email could not be delivered. Check the mail provider configuration and try again."
            );
        }
    }

    // Validate token existence, purpose, usage and expiration
    private AccountActionToken validToken(
            String raw,
            AccountTokenPurpose purpose
    ) {

        if (raw == null || raw.isBlank() || raw.length() > 128) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid or expired link"
            );
        }

        AccountActionToken token = tokens
                .findByTokenHashAndPurposeAndConsumedAtIsNull(
                        hash(raw),
                        purpose
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Invalid or expired link"
                        )
                );

        if (!token.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid or expired link"
            );
        }

        return token;
    }

    // Hash tokens before storing them in the database
    private String hash(String value) {

        try {
            byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);

            byte[] hashBytes = MessageDigest
                    .getInstance("SHA-256")
                    .digest(valueBytes);

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    ex
            );
        }
    }
}