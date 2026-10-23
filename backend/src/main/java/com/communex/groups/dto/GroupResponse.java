package com.communex.groups.dto;

import java.time.LocalDateTime;

public record GroupResponse(
        Long id,
        String name,
        String description,
        String visibility,
        Long ownerId,
        String ownerName,
        LocalDateTime createdAt,
        long memberCount,
        long pendingRequestCount,
        boolean ownedByMe,
        boolean memberOfMe,
        String membershipStatus) {
}
