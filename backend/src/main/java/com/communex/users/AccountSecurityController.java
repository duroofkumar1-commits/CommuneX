package com.communex.users;

import com.communex.users.dto.ForgotPasswordRequest;
import com.communex.users.dto.ResetPasswordRequest;
import com.communex.users.dto.VerifyEmailRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AccountSecurityController {

    private final AccountSecurityService accountSecurity;
    private final UserRepository users;
    private final boolean verificationRequired;

    // Constructor
    public AccountSecurityController(
            AccountSecurityService accountSecurity,
            UserRepository users,
            @Value("${communex.auth.email-verification-required:false}")
            boolean verificationRequired
    ) {
        this.accountSecurity = accountSecurity;
        this.users = users;
        this.verificationRequired = verificationRequired;
    }

    // Verify a user's email address
    @PostMapping("/verify-email")
    public Map<String, String> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        accountSecurity.verifyEmail(request.token());

        return Map.of(
                "message",
                "Email verified. You can now sign in."
        );
    }

    // Request a password-reset email
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        accountSecurity.requestPasswordReset(request.email());

        return Map.of(
                "message",
                "If the account exists and email delivery is configured, "
                        + "a password-reset link will be sent."
        );
    }

    // Reset the user's password
    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        accountSecurity.resetPassword(
                request.token(),
                request.newPassword()
        );

        return Map.of(
                "message",
                "Password reset successfully. Sign in with your new password."
        );
    }

    // Resend the email-verification link
    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> resendVerification(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        accountSecurity.resendVerification(request.email());

        return Map.of(
                "message",
                "If the account needs verification and email delivery is "
                        + "configured, a verification link will be sent."
        );
    }
}