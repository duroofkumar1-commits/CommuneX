package com.communex.groups.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GroupMessageRequest(
        @NotBlank(message = "Message content is required")
        @Size(max = 4000, message = "Message must not exceed 4000 characters")
        String content,
        Long parentMessageId) {
}
