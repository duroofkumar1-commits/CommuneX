package com.communex.requests.dto;

import com.communex.requests.HelpRequest;
import com.communex.users.User;
import java.time.LocalDateTime;

public record HelpRequestResponse(

        Long id,
        String title,
        String description,
        String category,
        String location,
        String status,
        String priority,
        Long createdById,
        String createdByName,
        Long assignedVolunteerId,
        String assignedVolunteerName,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt

) {

    // Convert a HelpRequest entity into a response DTO
    public static HelpRequestResponse from(HelpRequest request) {

        User creator = request.getCreatedBy();
        User volunteer = request.getAssignedVolunteer();

        Long assignedVolunteerId = null;
        String assignedVolunteerName = null;

        // Include volunteer information only when a volunteer is assigned
        if (volunteer != null) {
            assignedVolunteerId = volunteer.getId();
            assignedVolunteerName = volunteer.getName();
        }

        return new HelpRequestResponse(
                request.getId(),
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                request.getLocation(),
                request.getStatus().name(),
                request.getPriority().name(),
                creator.getId(),
                creator.getName(),
                assignedVolunteerId,
                assignedVolunteerName,
                request.getCreatedAt(),
                request.getResolvedAt()
        );
    }
}