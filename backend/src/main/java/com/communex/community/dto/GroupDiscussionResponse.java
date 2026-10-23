
package com.communex.community.dto;

import com.communex.community.GroupDiscussion;
import java.time.LocalDateTime;

/**
 * Response DTO representing a discussion within a community group.
 *
 * @param id discussion identifier
 * @param groupId identifier of the associated community group
 * @param authorId identifier of the discussion author
 * @param authorName name of the discussion author
 * @param content discussion content
 * @param createdAt timestamp when the discussion was created
 * @param updatedAt timestamp when the discussion was last updated
 */
public record GroupDiscussionResponse(
    Long id,
    Long groupId,
    Long authorId,
    String authorName,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    /**
     * Creates a discussion response from a GroupDiscussion entity.
     *
     * @param value source discussion entity
     * @return mapped discussion response
     */
    public static GroupDiscussionResponse from(GroupDiscussion value) {
        return new GroupDiscussionResponse(
            value.getId(),
            value.getGroup().getId(),
            value.getAuthor().getId(),
            value.getAuthor().getName(),
            value.getContent(),
            value.getCreatedAt(),
            value.getUpdatedAt()
        );
    }
}
