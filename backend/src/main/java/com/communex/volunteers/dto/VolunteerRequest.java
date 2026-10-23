package com.communex.volunteers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VolunteerRequest(

        @NotBlank(message = "Skills are required")
        @Size(max = 1000, message = "Skills must not exceed 1000 characters")
        String skills,

        @Size(max = 2000, message = "Bio must not exceed 2000 characters")
        String bio

) {}