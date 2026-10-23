package com.communex.map;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public final class MapDtos {

    private MapDtos() {
    }

    public record LocationUpdate(
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude
    ) {
    }

    public record LiveLocationUpdate(
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
            Double accuracy
    ) {
    }

    /** Coordinates are null when an item only has a text address. */
    public record MapItem(
            String type,
            Long id,
            String title,
            String description,
            String category,
            String status,
            String priority,
            String locationText,
            Double latitude,
            Double longitude,
            boolean pinned,
            boolean canPin,
            boolean mine,
            String ownerName,
            String extra,
            LocalDateTime time
    ) {
    }

    public record VolunteerPin(
            Long userId,
            String name,
            String skills,
            Double latitude,
            Double longitude,
            Double accuracy,
            LocalDateTime updatedAt
    ) {
    }

    public record Overview(
            List<MapItem> helpRequests,
            List<MapItem> events,
            List<MapItem> resources,
            List<MapItem> facilities,
            List<VolunteerPin> volunteers,
            boolean sharingLocation
    ) {
    }
}
