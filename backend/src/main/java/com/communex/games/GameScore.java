package com.communex.games;

import com.communex.users.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "game_scores",
        indexes = {
                @Index(
                        name = "idx_game_score_leaderboard",
                        columnList = "game, score"
                ),
                @Index(
                        name = "idx_game_score_user",
                        columnList = "user_id, created_at"
                )
        }
)
public class GameScore {

    // Primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User who achieved the score
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Game identifier
    @Column(nullable = false, length = 40)
    private String game;

    // Score achieved by the user
    @Column(nullable = false)
    private int score;

    // Time taken to complete the game, in seconds
    @Column(nullable = false)
    private int durationSeconds;

    // Score creation timestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Default constructor
    public GameScore() {
    }

    // Set creation timestamp before persistence
    @PrePersist
    public void create() {
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

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}