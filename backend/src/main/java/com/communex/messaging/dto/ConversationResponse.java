package com.communex.messaging.dto;

import java.time.LocalDateTime;

public record ConversationResponse(

        String email,

        String name,

        String lastMessage,

        LocalDateTime lastMessageAt,

        long unreadCount

) {
}