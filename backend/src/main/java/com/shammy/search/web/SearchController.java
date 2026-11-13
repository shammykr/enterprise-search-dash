package com.shammy.search.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shammy.search.dto.SearchRequestParams;
import com.shammy.search.dto.SearchResult;
import com.shammy.search.service.SearchService;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Example: GET /api/search?q=security&category=Security&tag=policy&from=2025-01-01&page=0&size=10
     * Repeat {@code category} / {@code tag} to select several facet values.
     */
    @GetMapping("/search")
    public SearchResult search(@RequestParam(name = "q", required = false) String query,
                               @RequestParam(name = "category", required = false) List<String> categories,
                               @RequestParam(name = "tag", required = false) List<String> tags,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size) {
        SearchRequestParams params = new SearchRequestParams(query, categories, tags, from, to,
                Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        return searchService.search(params);
    }

    @GetMapping("/autocomplete")
    public List<String> autocomplete(@RequestParam("q") String prefix,
                                     @RequestParam(defaultValue = "8") int limit) {
        return searchService.autocomplete(prefix, Math.min(Math.max(limit, 1), 20));
    }
}
