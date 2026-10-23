package com.communex.requests;

import com.communex.users.User;
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
        name = "help_request_updates",
        indexes = {
                @Index(
                        name = "idx_request_updates_request_date",
                        columnList = "request_id, created_at"
                )
        }
)
public class HelpRequestUpdate {

    // Primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Associated help request
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private HelpRequest request;

    // Previous request status
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private HelpRequestStatus fromStatus;

    // New request status
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private HelpRequestStatus toStatus;

    // Explanation of the status change
    @Column(nullable = false, length = 500)
    private String note;

    // User who performed the action
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    // Record creation timestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Default constructor
    public HelpRequestUpdate() {
    }

    // Set creation timestamp before persistence
    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public HelpRequest getRequest() {
        return request;
    }

    public void setRequest(HelpRequest request) {
        this.request = request;
    }

    public HelpRequestStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(HelpRequestStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public HelpRequestStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(HelpRequestStatus toStatus) {
        this.toStatus = toStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public User getActor() {
        return actor;
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}