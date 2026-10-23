
package com.communex.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating a community announcement.
 *
 * @param title announcement title; required and limited to 160 characters
 * @param body announcement content; required and limited to 3000 characters
 * @param category announcement category; limited to 60 characters
 */
public record AnnouncementRequest(

    @NotBlank
    @Size(max = 160)
    String title,

    @NotBlank
    @Size(max = 3000)
    String body,

    @Size(max = 60)
    String category

) {
}
