package com.communex.games;

import com.communex.users.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "game_progress",
    uniqueConstraints = @UniqueConstraint(name = "uq_game_progress_user_game", columnNames = {"user_id", "game"}),
    indexes = @Index(name = "idx_game_progress_user", columnList = "user_id, updated_at")
)
public class GameProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 40)
    private String game;

    @Column(name = "current_level", nullable = false)
    private int currentLevel = 1;

    @Column(name = "highest_unlocked_level", nullable = false)
    private int highestUnlockedLevel = 1;

    @Column(name = "total_score", nullable = false)
    private long totalScore = 0;

    @Column(nullable = false)
    private int victories = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public GameProgress() {}

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getGame() { return game; }
    public void setGame(String game) { this.game = game; }
    public int getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(int currentLevel) { this.currentLevel = currentLevel; }
    public int getHighestUnlockedLevel() { return highestUnlockedLevel; }
    public void setHighestUnlockedLevel(int highestUnlockedLevel) { this.highestUnlockedLevel = highestUnlockedLevel; }
    public long getTotalScore() { return totalScore; }
    public void setTotalScore(long totalScore) { this.totalScore = totalScore; }
    public int getVictories() { return victories; }
    public void setVictories(int victories) { this.victories = victories; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
