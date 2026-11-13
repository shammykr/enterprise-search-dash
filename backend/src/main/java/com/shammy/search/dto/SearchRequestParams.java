package com.shammy.search.dto;

import java.time.LocalDate;
import java.util.List;

/** Normalised search input built from query-string parameters. */
public record SearchRequestParams(
        String query,
        List<String> categories,
        List<String> tags,
        LocalDate from,
        LocalDate to,
        int page,
        int size) {
}
