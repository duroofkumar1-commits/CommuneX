package com.communex.documents.dto;

import com.communex.documents.UserDocument;
import java.time.LocalDateTime;

public record DocumentResponse(

        Long id,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    // Convert a UserDocument entity into a response DTO
    public static DocumentResponse from(UserDocument document) {

        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getContent(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}