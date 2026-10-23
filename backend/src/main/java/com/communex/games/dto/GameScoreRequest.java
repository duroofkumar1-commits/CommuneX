package com.communex.games.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GameScoreRequest(

        @NotBlank
        @Size(max = 40)
        String game,

        @Min(0)
        @Max(1000000)
        int score,

        @Min(0)
        @Max(86400)
        int durationSeconds

) {
}