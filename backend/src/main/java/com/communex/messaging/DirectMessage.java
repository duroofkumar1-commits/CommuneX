package com.communex.messaging;

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
        name = "direct_messages",
        indexes = {
                @Index(
                        name = "idx_message_sender_date",
                        columnList = "sender_id, sent_at"
                ),
                @Index(
                        name = "idx_message_recipient_date",
                        columnList = "recipient_id, sent_at"
                )
        }
)
public class DirectMessage {

    // Primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User who sends the message
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // User who receives the message
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    // Message content
    @Column(nullable = false, length = 4000)
    private String content;

    // Message creation timestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    // Timestamp when the recipient reads the message
    private LocalDateTime readAt;

    // Default constructor
    public DirectMessage() {
    }

    // Set sent time before saving the message
    @PrePersist
    public void create() {
        sentAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}