package com.communex.messaging.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MessageReadRequest(

        @NotBlank
        @Email
        String withEmail

) {
}