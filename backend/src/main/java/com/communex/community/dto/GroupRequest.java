
package com.communex.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating a community group.
 *
 * @param name group name; must not be blank and must not exceed 120 characters
 * @param description group description; must not be blank and must not exceed 1500 characters
 * @param visibility group visibility; must be PUBLIC or PRIVATE when provided
 */
public record GroupRequest(

    @NotBlank
    @Size(max = 120)
    String name,

    @NotBlank
    @Size(max = 1500)
    String description,

    @Pattern(
        regexp = "PUBLIC|PRIVATE",
        message = "Visibility must be PUBLIC or PRIVATE"
    )
    String visibility

) {
}
