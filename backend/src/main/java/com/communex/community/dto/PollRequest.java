
package com.communex.community.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PollRequest(

        @NotBlank
        @Size(max = 300)
        String question,

        @NotNull
        @Size(min = 2, max = 6)
        List<
                @NotBlank
                @Size(max = 150)
                String
        > options

) {
}
