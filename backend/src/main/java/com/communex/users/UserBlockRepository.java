package com.communex.users;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBlockRepository
        extends JpaRepository<UserBlock, Long> {

    // Check whether a user has blocked another user
    boolean existsByBlockerIdAndBlockedId(
            Long blockerId,
            Long blockedId
    );

    // Remove a block between two users
    void deleteByBlockerIdAndBlockedId(
            Long blockerId,
            Long blockedId
    );

    // Find all users blocked by a specific user, newest first
    List<UserBlock> findByBlockerEmailOrderByCreatedAtDesc(
            String email
    );
}