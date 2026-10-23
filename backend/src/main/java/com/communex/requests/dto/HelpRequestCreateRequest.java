package com.communex.requests.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HelpRequestCreateRequest(
    @NotBlank @Size(max = 150) String title,
    @NotBlank @Size(max = 3000) String description,
    @NotBlank @Size(max = 100) String category,
    @NotBlank @Size(max = 255) String location
) {}
