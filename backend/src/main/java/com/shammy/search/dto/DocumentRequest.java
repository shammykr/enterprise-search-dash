package com.shammy.search.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload for creating or updating a document. */
public record DocumentRequest(
        @NotBlank @Size(max = 300) String title,
        @NotBlank String content,
        @NotBlank @Size(max = 80) String category,
        List<@NotBlank String> tags,
        @Size(max = 120) String author,
        Instant createdAt) {
}
