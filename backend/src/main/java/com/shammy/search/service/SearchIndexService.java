package com.shammy.search.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.shammy.search.config.SearchProperties;
import com.shammy.search.model.EnterpriseDocument;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;

/**
 * Owns the Elasticsearch index: creates it with an explicit mapping and keeps it in
 * sync with MongoDB (MongoDB is the source of truth).
 */
@Service
public class SearchIndexService {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexService.class);

    private final ElasticsearchClient client;
    private final String index;

    public SearchIndexService(ElasticsearchClient client, SearchProperties properties) {
        this.client = client;
        this.index = properties.indexName();
    }

    public String indexName() {
        return index;
    }

    /** Creates the index with explicit mappings if it does not exist yet. */
    public void ensureIndex() {
        try {
            if (client.indices().exists(e -> e.index(index)).value()) {
                return;
            }
            client.indices().create(c -> c
                    .index(index)
                    .mappings(m -> m
                            .properties("title", p -> p.text(t -> t.analyzer("english")
                                    .fields("raw", f -> f.keyword(k -> k.ignoreAbove(256)))))
                            .properties("content", p -> p.text(t -> t.analyzer("english")))
                            .properties("category", p -> p.keyword(k -> k))
                            .properties("tags", p -> p.keyword(k -> k))
                            .properties("author", p -> p.keyword(k -> k))
                            .properties("createdAt", p -> p.date(d -> d))
                            .properties("suggest", p -> p.completion(cp -> cp.analyzer("simple")))));
            log.info("Created Elasticsearch index '{}'", index);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create index " + index, e);
        }
    }

    /** Drops and recreates the index, then loads every document from the given list. */
    public void rebuild(List<EnterpriseDocument> docs) {
        try {
            client.indices().delete(d -> d.index(index).ignoreUnavailable(true));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ensureIndex();
        bulkIndex(docs);
    }

    public long count() {
        try {
            return client.count(c -> c.index(index)).count();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void index(EnterpriseDocument doc) {
        try {
            client.index(i -> i.index(index).id(doc.getId()).document(toSource(doc)).refresh(Refresh.WaitFor));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to index document " + doc.getId(), e);
        }
    }

    public void delete(String id) {
        try {
            client.delete(d -> d.index(index).id(id).refresh(Refresh.WaitFor));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete document " + id, e);
        }
    }

    public void bulkIndex(List<EnterpriseDocument> docs) {
        if (docs.isEmpty()) {
            return;
        }
        BulkRequest.Builder bulk = new BulkRequest.Builder().refresh(Refresh.WaitFor);
        for (EnterpriseDocument doc : docs) {
            bulk.operations(op -> op.index(i -> i.index(index).id(doc.getId()).document(toSource(doc))));
        }
        try {
            BulkResponse response = client.bulk(bulk.build());
            if (response.errors()) {
                throw new IllegalStateException("Bulk indexing reported errors");
            }
            log.info("Bulk indexed {} documents into '{}'", docs.size(), index);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Map<String, Object> toSource(EnterpriseDocument doc) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("title", doc.getTitle());
        source.put("content", doc.getContent());
        source.put("category", doc.getCategory());
        source.put("tags", doc.getTags());
        source.put("author", doc.getAuthor());
        source.put("createdAt", doc.getCreatedAt() == null ? null : doc.getCreatedAt().toString());
        source.put("suggest", Map.of("input", suggestInputs(doc)));
        return source;
    }

    /**
     * Completion suggesters match on prefixes, so besides the full title we also index
     * every word-suffix of it ("kubernetes rollout guide", "rollout guide", ...). That lets "rollout" complete to the title even though it is mid-sentence.
     */
    static List<String> suggestInputs(EnterpriseDocument doc) {
        Set<String> inputs = new LinkedHashSet<>();
        String title = doc.getTitle() == null ? "" : doc.getTitle().trim();
        if (!title.isEmpty()) {
            inputs.add(title);
            String[] words = title.split("\\s+");
            for (int i = 1; i < words.length; i++) {
                if (words[i].length() > 2) {
                    inputs.add(String.join(" ", java.util.Arrays.copyOfRange(words, i, words.length)));
                }
            }
        }
        return new ArrayList<>(inputs);
    }
}
