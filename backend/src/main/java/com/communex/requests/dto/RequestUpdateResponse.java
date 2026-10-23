package com.communex.requests.dto;

import com.communex.requests.HelpRequestUpdate;
import java.time.LocalDateTime;

public record RequestUpdateResponse(

        Long id,
        String fromStatus,
        String toStatus,
        String note,
        String actorName,
        LocalDateTime createdAt

) {

    // Convert a HelpRequestUpdate entity into a response DTO
    public static RequestUpdateResponse from(
            HelpRequestUpdate update
    ) {

        String previousStatus = null;

        // Handle the initial status, which may be null
        if (update.getFromStatus() != null) {
            previousStatus = update.getFromStatus().name();
        }

        return new RequestUpdateResponse(
                update.getId(),
                previousStatus,
                update.getToStatus().name(),
                update.getNote(),
                update.getActor().getName(),
                update.getCreatedAt()
        );
    }
}