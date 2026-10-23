package com.communex.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "account_action_tokens",
    indexes = {
        @Index(
            name = "idx_account_token_user_purpose",
            columnList = "user_id, purpose, expires_at"
        )
    }
)
public class AccountActionToken {

    // Primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Associated user
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Secure token hash
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    // Purpose of the account action
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AccountTokenPurpose purpose;

    // Token expiration timestamp
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Timestamp when the token was consumed
    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    // Record creation timestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Default constructor
    public AccountActionToken() {
    }

    // Automatically set creation time before persistence
    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public AccountTokenPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(AccountTokenPurpose purpose) {
        this.purpose = purpose;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(LocalDateTime consumedAt) {
        this.consumedAt = consumedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}