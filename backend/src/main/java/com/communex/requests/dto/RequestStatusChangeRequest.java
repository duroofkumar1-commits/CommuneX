package com.communex.requests.dto;

import com.communex.requests.HelpRequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RequestStatusChangeRequest(
        @NotNull HelpRequestStatus status,
        @NotBlank @Size(max = 500) String note
) { }
