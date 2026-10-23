package com.communex.messaging.dto;

import com.communex.messaging.DirectMessage;
import java.time.LocalDateTime;

public record MessageResponse(

        Long id,

        String senderEmail,

        String senderName,

        String recipientEmail,

        String recipientName,

        String content,

        LocalDateTime sentAt,

        LocalDateTime readAt

) {

    // Convert a DirectMessage entity into a response DTO
    public static MessageResponse from(DirectMessage message) {

        return new MessageResponse(
                message.getId(),
                message.getSender().getEmail(),
                message.getSender().getName(),
                message.getRecipient().getEmail(),
                message.getRecipient().getName(),
                message.getContent(),
                message.getSentAt(),
                message.getReadAt()
        );
    }
}