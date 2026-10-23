
package com.communex.events.dto;

import com.communex.events.CommunityEvent;
import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String title,
        String description,
        String location,
        LocalDateTime startsAt,
        Integer capacity,
        String status,
        String organizerName,
        long registrationCount,
        boolean registeredByMe
) {

    public static EventResponse from(
            CommunityEvent event,
            long registrations,
            boolean registered
    ) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getStartsAt(),
                event.getCapacity(),
                event.getStatus(),
                event.getOrganizer().getName(),
                registrations,
                registered
        );
    }
}
