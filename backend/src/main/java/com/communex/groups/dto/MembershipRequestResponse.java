package com.communex.groups.dto;

import java.time.LocalDateTime;

public record MembershipRequestResponse(
        Long id,
        Long groupId,
        Long userId,
        String userName,
        String userEmail,
        LocalDateTime createdAt,
        String status) {
}
