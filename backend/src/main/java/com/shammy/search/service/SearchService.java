package com.shammy.search.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shammy.search.dto.SearchRequestParams;
import com.shammy.search.dto.SearchResult;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;

/**
 * Full-text search, facet aggregations and auto-complete against Elasticsearch.
 *
 * <p>Facets use the "post_filter" pattern: the text query and date range go in the main
 * query, while category/tag selections go in {@code post_filter}. Each facet's aggregation is
 * then wrapped in a filter containing every <em>other</em> facet's selection, so the category
 * list still shows sibling categories (multi-select) while reflecting the chosen tags, and
 * vice versa.
 */
@Service
public class SearchService {

    static final String FACET_CATEGORY = "category";
    static final String FACET_TAGS = "tags";
    private static final int FACET_SIZE = 25;

    private final ElasticsearchClient client;
    private final SearchIndexService indexService;

    public SearchService(ElasticsearchClient client, SearchIndexService indexService) {
        this.client = client;
        this.indexService = indexService;
    }

    public SearchResult search(SearchRequestParams p) {
        Query mainQuery = mainQuery(p);
        Query categoryFilter = termsFilter(FACET_CATEGORY, p.categories());
        Query tagFilter = termsFilter(FACET_TAGS, p.tags());

        try {
            SearchResponse<ObjectNode> response = client.search(s -> s
                    .index(indexService.indexName())
                    .query(mainQuery)
                    .postFilter(and(categoryFilter, tagFilter))
                    .from(p.page() * p.size())
                    .size(p.size())
                    .trackTotalHits(t -> t.enabled(true))
                    .highlight(h -> h
                            .preTags("<mark>").postTags("</mark>")
                            .fields("content", f -> f.fragmentSize(160).numberOfFragments(1))
                            .fields("title", f -> f.numberOfFragments(0)))
                    .aggregations(FACET_CATEGORY, facetAgg(FACET_CATEGORY, tagFilter))
                    .aggregations(FACET_TAGS, facetAgg(FACET_TAGS, categoryFilter)),
                    ObjectNode.class);

            List<SearchResult.Hit> hits = response.hits().hits().stream().map(SearchService::toHit).toList();
            Map<String, List<SearchResult.FacetBucket>> facets = new LinkedHashMap<>();
            facets.put(FACET_CATEGORY, buckets(response.aggregations().get(FACET_CATEGORY)));
            facets.put(FACET_TAGS, buckets(response.aggregations().get(FACET_TAGS)));

            long total = response.hits().total() == null ? hits.size() : response.hits().total().value();
            return new SearchResult(total, p.page(), p.size(), response.took(), hits, facets);
        } catch (IOException e) {
            throw new UncheckedIOException("Search failed", e);
        }
    }

    /** Title suggestions for a typed prefix, using the completion suggester (with typo tolerance). */
    public List<String> autocomplete(String prefix, int limit) {
        if (prefix == null || prefix.isBlank()) {
            return List.of();
        }
        try {
            SearchResponse<ObjectNode> response = client.search(s -> s
                    .index(indexService.indexName())
                    .source(src -> src.filter(f -> f.includes("title")))
                    .suggest(sg -> sg.suggesters("title-suggest", fs -> fs
                            .prefix(prefix.trim())
                            .completion(c -> c
                                    .field("suggest")
                                    .size(limit * 3)
                                    .fuzzy(fz -> fz.fuzziness("AUTO").minLength(4))))),
                    ObjectNode.class);

            // Several inputs of the same document can match; collapse to unique titles.
            Set<String> titles = new LinkedHashSet<>();
            response.suggest().getOrDefault("title-suggest", List.of()).forEach(suggestion ->
                    suggestion.completion().options().stream()
                            .map(CompletionSuggestOption::source)
                            .filter(src -> src != null && src.hasNonNull("title"))
                            .forEach(src -> titles.add(src.get("title").asText())));
            return titles.stream().limit(limit).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Autocomplete failed", e);
        }
    }

    private static Query mainQuery(SearchRequestParams p) {
        BoolQuery.Builder bool = new BoolQuery.Builder();
        if (p.query() == null || p.query().isBlank()) {
            bool.must(m -> m.matchAll(a -> a));
        } else {
            bool.must(m -> m.multiMatch(mm -> mm
                    .query(p.query())
                    .fields("title^3", "tags^2", "content")
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO")));
        }
        if (p.from() != null || p.to() != null) {
            bool.filter(f -> f.range(r -> {
                r.field("createdAt");
                if (p.from() != null) {
                    r.gte(JsonData.of(p.from().toString()));
                }
                if (p.to() != null) {
                    r.lte(JsonData.of(p.to().toString() + "||/d"));
                }
                return r;
            }));
        }
        return bool.build()._toQuery();
    }

    /** A terms filter for the selected facet values, or null when nothing is selected. */
    private static Query termsFilter(String field, List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<FieldValue> fieldValues = values.stream().map(FieldValue::of).toList();
        return Query.of(q -> q.terms(t -> t.field(field).terms(tv -> tv.value(fieldValues))));
    }

    private static Query and(Query... filters) {
        List<Query> present = new ArrayList<>();
        for (Query f : filters) {
            if (f != null) {
                present.add(f);
            }
        }
        if (present.isEmpty()) {
            return Query.of(q -> q.matchAll(a -> a));
        }
        return Query.of(q -> q.bool(b -> b.filter(present)));
    }

    private static Aggregation facetAgg(String field, Query otherFacetFilter) {
        return Aggregation.of(a -> a
                .filter(and(otherFacetFilter))
                .aggregations("values", sub -> sub.terms(t -> t.field(field).size(FACET_SIZE))));
    }

    private static List<SearchResult.FacetBucket> buckets(Aggregate agg) {
        return agg.filter().aggregations().get("values").sterms().buckets().array().stream()
                .map(b -> new SearchResult.FacetBucket(b.key().stringValue(), b.docCount()))
                .toList();
    }

    private static SearchResult.Hit toHit(Hit<ObjectNode> hit) {
        ObjectNode src = hit.source();
        Map<String, List<String>> hl = hit.highlight();
        String title = firstOr(hl.get("title"), text(src, "title"));
        String content = text(src, "content");
        String snippet = firstOr(hl.get("content"), content.length() > 200 ? content.substring(0, 200) + "…" : content);

        List<String> tags = new ArrayList<>();
        JsonNode tagNode = src.get("tags");
        if (tagNode != null && tagNode.isArray()) {
            tagNode.forEach(t -> tags.add(t.asText()));
        }
        return new SearchResult.Hit(hit.id(), title, snippet, text(src, "category"), tags,
                text(src, "author"), text(src, "createdAt"), hit.score() == null ? 0 : hit.score());
    }

    private static String firstOr(List<String> values, String fallback) {
        return values == null || values.isEmpty() ? fallback : values.get(0);
    }

    private static String text(ObjectNode src, String field) {
        JsonNode node = src.get(field);
        return node == null || node.isNull() ? "" : node.asText();
    }
}
