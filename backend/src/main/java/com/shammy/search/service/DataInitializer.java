package com.shammy.search.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shammy.search.config.SearchProperties;
import com.shammy.search.model.EnterpriseDocument;
import com.shammy.search.repository.DocumentRepository;

/**
 * On startup: make sure the index exists, seed MongoDB with sample documents when it is
 * empty, and rebuild the Elasticsearch index from MongoDB if the two have drifted apart
 * (e.g. the ES container was recreated without its volume).
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final DocumentRepository repository;
    private final SearchIndexService indexService;
    private final SearchProperties properties;
    private final ObjectMapper objectMapper;

    public DataInitializer(DocumentRepository repository, SearchIndexService indexService,
                           SearchProperties properties, ObjectMapper objectMapper) {
        this.repository = repository;
        this.indexService = indexService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        indexService.ensureIndex();

        if (properties.seedSampleData() && repository.count() == 0) {
            try (InputStream in = new ClassPathResource("sample-documents.json").getInputStream()) {
                List<EnterpriseDocument> docs = objectMapper.readValue(in, new TypeReference<>() { });
                docs.forEach(d -> d.setUpdatedAt(d.getCreatedAt()));
                repository.saveAll(docs);
                log.info("Seeded MongoDB with {} sample documents", docs.size());
            }
        }

        long stored = repository.count();
        if (indexService.count() != stored) {
            log.info("Index out of sync with MongoDB ({} docs), reindexing", stored);
            indexService.rebuild(repository.findAll());
        }
    }
}
