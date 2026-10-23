package com.communex.community.dto;

import jakarta.validation.constraints.NotNull;

public record PollVoteRequest(

        @NotNull
        Long optionId

) {
}
