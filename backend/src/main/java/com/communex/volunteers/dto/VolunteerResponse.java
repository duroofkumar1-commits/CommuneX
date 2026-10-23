package com.communex.volunteers.dto;

public record VolunteerResponse(
        Long id,
        Long userId,
        String name,
        String skills,
        String bio,
        boolean available
) {}