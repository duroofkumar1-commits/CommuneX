package com.communex.resources.dto;

import com.communex.resources.ResourceItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ResourceResponse(

        Long id,
        String name,
        String category,
        String description,
        String location,
        BigDecimal quantity,
        String unit,
        BigDecimal lowStockThreshold,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    // Convert a ResourceItem entity into a response DTO
    public static ResourceResponse from(ResourceItem resource) {

        String status;

        // Determine the current stock status
        if (resource.getQuantity().signum() <= 0) {
            status = "OUT_OF_STOCK";
        } else if (
                resource.getQuantity().compareTo(
                        resource.getLowStockThreshold()
                ) <= 0
        ) {
            status = "LOW_STOCK";
        } else {
            status = "AVAILABLE";
        }

        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getCategory(),
                resource.getDescription(),
                resource.getLocation(),
                resource.getQuantity(),
                resource.getUnit(),
                resource.getLowStockThreshold(),
                status,
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }
}