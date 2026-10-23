package com.communex.messaging;

import java.time.LocalDateTime;

public interface ConversationSummaryProjection {
    String getEmail();
    String getName();
    String getLastMessage();
    LocalDateTime getLastMessageAt();
    Long getUnreadCount();
}
