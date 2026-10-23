package com.communex.games.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GameProgressRequest(
    @NotNull @Min(1) @Max(50) Integer currentLevel,
    @NotNull @Min(1) @Max(50) Integer highestUnlockedLevel,
    @NotNull @Min(0) @Max(1000000000L) Long totalScore,
    @NotNull @Min(0) @Max(1000000) Integer victories
) {}
