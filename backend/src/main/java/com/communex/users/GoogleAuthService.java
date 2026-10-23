package com.communex.users;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GoogleAuthService {

    // OAuth client IDs are public identifiers. The default matches frontend/.env in
    // the supplied project; GOOGLE_CLIENT_ID can override it per environment.
    private static final String DEFAULT_CLIENT_ID =
            "320462463058-434slfjjbv8clcpg1r6d3u2gra0pgkg2.apps.googleusercontent.com";

    private final UserRepository userRepository;
    private final String clientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthService(
            UserRepository userRepository,
            @Value("${GOOGLE_CLIENT_ID:" + DEFAULT_CLIENT_ID + "}") String clientId) {
        this.userRepository = userRepository;
        this.clientId = clientId == null ? "" : clientId.trim();

        try {
            this.verifier = this.clientId.isBlank()
                    ? null
                    : new GoogleIdTokenVerifier.Builder(
                            GoogleNetHttpTransport.newTrustedTransport(),
                            GsonFactory.getDefaultInstance())
                            .setAudience(Collections.singletonList(this.clientId))
                            .build();
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalStateException("Google token verification could not be initialized", exception);
        }
    }

    public User login(String credential) {
        if (credential == null || credential.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google credential is required.");
        }
        if (verifier == null || clientId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Google sign-in is not configured on the backend.");
        }

        final GoogleIdToken idToken;
        try {
            idToken = GoogleIdToken.parse(GsonFactory.getDefaultInstance(), credential);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google credential is invalid or expired.");
        }

        final boolean valid;
        try {
            valid = idToken != null && verifier.verify(idToken);
        } catch (GeneralSecurityException | IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Google token verification is temporarily unavailable.");
        }
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google credential is invalid or expired.");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String subject = payload.getSubject();
        Object verifiedClaim = payload.get("email_verified");
        boolean emailVerified = Boolean.TRUE.equals(verifiedClaim)
                || "true".equalsIgnoreCase(String.valueOf(verifiedClaim));

        if (email == null || email.isBlank() || subject == null || subject.isBlank() || !emailVerified) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Google account must provide a verified email address.");
        }

        email = email.trim().toLowerCase();
        String displayName = stringClaim(payload, "name");
        String picture = stringClaim(payload, "picture");

        Optional<User> found = userRepository.findByEmail(email);
        User user;
        if (found.isPresent()) {
            user = found.get();
            if (user.getProvider() == AuthProvider.GOOGLE
                    && user.getProviderId() != null
                    && !user.getProviderId().equals(subject)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This email is already linked to a different Google account.");
            }
            // A Google-verified email proves ownership of the same email address,
            // so an existing account can continue without creating a duplicate user.
            user.setProvider(AuthProvider.GOOGLE);
            user.setProviderId(subject);
            user.setEmailVerified(true);
            if (user.getName() == null || user.getName().isBlank()) {
                user.setName(displayName == null || displayName.isBlank() ? email.split("@", 2)[0] : displayName);
            }
            if (picture != null && !picture.isBlank()) {
                user.setProfileImageUrl(picture);
            }
        } else {
            user = new User();
            user.setEmail(email);
            user.setName(displayName == null || displayName.isBlank() ? email.split("@", 2)[0] : displayName);
            user.setRole(Role.USER);
            user.setProvider(AuthProvider.GOOGLE);
            user.setProviderId(subject);
            user.setEmailVerified(true);
            if (picture != null && !picture.isBlank()) {
                user.setProfileImageUrl(picture);
            }
        }

        return userRepository.save(user);
    }

    private String stringClaim(GoogleIdToken.Payload payload, String claimName) {
        Object value = payload.get(claimName);
        return value instanceof String text ? text : null;
    }
}
