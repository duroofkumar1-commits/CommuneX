package com.communex.documents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentRequest(

        @NotBlank
        @Size(max = 160)
        String title,

        @Size(max = 100000)
        String content

) {
}