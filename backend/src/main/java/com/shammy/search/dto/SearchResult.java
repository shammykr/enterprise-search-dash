package com.shammy.search.dto;

import java.util.List;
import java.util.Map;

public record SearchResult(
        long total,
        int page,
        int size,
        long tookMs,
        List<Hit> hits,
        Map<String, List<FacetBucket>> facets) {

    public record Hit(String id, String title, String snippet, String category, List<String> tags,
                      String author, String createdAt, double score) {
    }

    public record FacetBucket(String value, long count) {
    }
}
