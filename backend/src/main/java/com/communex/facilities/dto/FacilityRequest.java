package com.communex.facilities.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FacilityRequest(

        @NotBlank
        @Size(max = 160)
        String name,

        @NotBlank
        @Size(max = 60)
        String category,

        @Size(max = 2000)
        String description,

        @Size(max = 500)
        String address,

        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        Double latitude,

        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        Double longitude,

        @Size(max = 80)
        String phone,

        @Size(max = 300)
        String website,

        @Size(max = 500)
        String openingInformation,

        Boolean verified

) {
}