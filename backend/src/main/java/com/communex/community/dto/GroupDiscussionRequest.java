
package com.communex.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a discussion within a community group.
 *
 * @param content discussion content; must not be blank and must not
 *                 exceed 5000 characters
 */
public record GroupDiscussionRequest(

    @NotBlank
    @Size(max = 5000)
    String content

) {
}
