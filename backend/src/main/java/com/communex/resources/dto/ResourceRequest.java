package com.communex.resources.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ResourceRequest(

        @NotBlank
        @Size(max = 160)
        String name,

        @NotBlank
        @Size(max = 80)
        String category,

        @Size(max = 2000)
        String description,

        @Size(max = 255)
        String location,

        @NotNull
        @DecimalMin("0.0")
        BigDecimal quantity,

        @NotBlank
        @Size(max = 40)
        String unit,

        @NotNull
        @DecimalMin("0.0")
        BigDecimal lowStockThreshold

) {
}