package com.communex.games.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GameLevelRequest(@Min(1) @Max(50) int level) { }
