package com.communex.facilities.dto;

import com.communex.facilities.Facility;
import java.time.LocalDateTime;

public record FacilityResponse(

        Long id,
        String name,
        String category,
        String description,
        String address,
        Double latitude,
        Double longitude,
        String phone,
        String website,
        String openingInformation,
        boolean verified,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    // Convert a Facility entity into a response DTO
    public static FacilityResponse from(Facility facility) {

        return new FacilityResponse(
                facility.getId(),
                facility.getName(),
                facility.getCategory(),
                facility.getDescription(),
                facility.getAddress(),
                facility.getLatitude(),
                facility.getLongitude(),
                facility.getPhone(),
                facility.getWebsite(),
                facility.getOpeningInformation(),
                facility.isVerified(),
                facility.getCreatedAt(),
                facility.getUpdatedAt()
        );
    }
}