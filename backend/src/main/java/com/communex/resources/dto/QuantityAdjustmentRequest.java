package com.communex.resources.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record QuantityAdjustmentRequest(

        @NotNull
        @DecimalMin(
                value = "-1000000000.0",
                inclusive = true
        )
        @DecimalMax("1000000000.0")
        BigDecimal delta,

        @NotBlank
        @Size(max = 300)
        String reason

) {
}
