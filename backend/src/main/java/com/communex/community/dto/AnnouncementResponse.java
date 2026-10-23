
package com.communex.community.dto;

import com.communex.community.CommunityAnnouncement;
import java.time.LocalDateTime;

/**
 * Response DTO representing a community announcement.
 *
 * @param id announcement identifier
 * @param title announcement title
 * @param body announcement content
 * @param category announcement category
 * @param authorName name of the announcement creator
 * @param published whether the announcement is published
 * @param createdAt timestamp when the announcement was created
 * @param updatedAt timestamp when the announcement was last updated
 */
public record AnnouncementResponse(
    Long id,
    String title,
    String body,
    String category,
    String authorName,
    boolean published,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    /**
     * Creates an announcement response from an entity.
     *
     * @param announcement source announcement entity
     * @return mapped announcement response
     */
    public static AnnouncementResponse from(
        CommunityAnnouncement announcement
    ) {
        return new AnnouncementResponse(
            announcement.getId(),
            announcement.getTitle(),
            announcement.getBody(),
            announcement.getCategory(),
            announcement.getCreatedBy().getName(),
            announcement.isPublished(),
            announcement.getCreatedAt(),
            announcement.getUpdatedAt()
        );
    }
}
