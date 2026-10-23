package com.communex.groups.dto;

import java.time.LocalDateTime;
import java.util.List;

public record GroupMessageResponse(
        Long id,
        Long groupId,
        Long senderId,
        String senderName,
        String content,
        Long parentMessageId,
        LocalDateTime createdAt,
        List<ReactionSummary> reactions) {

    public record ReactionSummary(String emoji, long count, boolean reactedByMe) {
    }
}
