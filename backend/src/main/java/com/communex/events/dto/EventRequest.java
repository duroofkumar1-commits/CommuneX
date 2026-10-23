
package com.communex.events.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record EventRequest(

        @NotBlank
        @Size(max = 160)
        String title,

        @NotBlank
        @Size(max = 3000)
        String description,

        @NotBlank
        @Size(max = 255)
        String location,

        @NotNull
        @Future
        LocalDateTime startsAt,

        @Min(1)
        @Max(100000)
        Integer capacity

) {
}
