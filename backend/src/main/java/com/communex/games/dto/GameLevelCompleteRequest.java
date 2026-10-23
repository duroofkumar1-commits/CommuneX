package com.communex.games.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GameLevelCompleteRequest(
    @Min(1) @Max(50) int level,
    @Min(0) @Max(1000000) int score,
    @Min(0) @Max(1000000) int xp
) { }
