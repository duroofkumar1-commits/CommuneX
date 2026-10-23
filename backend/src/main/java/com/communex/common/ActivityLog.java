package com.communex.common;

import com.communex.users.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs", indexes = @Index(name = "idx_activity_user_created", columnList = "user_id, created_at"))
public class ActivityLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, length = 100) private String action;
    @Column(nullable = false, length = 80) private String entityType;
    private Long entityId;
    @Column(length = 500) private String details;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    public ActivityLog() {}
    @PrePersist public void onCreate() { createdAt = LocalDateTime.now(); }
    public Long getId(){return id;} public User getUser(){return user;} public void setUser(User v){user=v;}
    public String getAction(){return action;} public void setAction(String v){action=v;}
    public String getEntityType(){return entityType;} public void setEntityType(String v){entityType=v;}
    public Long getEntityId(){return entityId;} public void setEntityId(Long v){entityId=v;}
    public String getDetails(){return details;} public void setDetails(String v){details=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
