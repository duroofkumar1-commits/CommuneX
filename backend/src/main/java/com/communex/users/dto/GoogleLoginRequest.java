package com.communex.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleLoginRequest(
        @NotBlank(message = "Google credential is required")
        @Size(max = 10000, message = "Google credential is too large")
        String credential
) {
}
