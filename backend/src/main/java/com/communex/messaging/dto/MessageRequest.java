package com.communex.messaging.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageRequest(

        @NotBlank
        @Email
        String recipientEmail,

        @NotBlank
        @Size(max = 4000)
        String content

) {
}