package com.shammy.search.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.shammy.search.dto.DocumentRequest;
import com.shammy.search.model.EnterpriseDocument;
import com.shammy.search.repository.DocumentRepository;

/** Document CRUD: writes go to MongoDB first, then the change is mirrored into Elasticsearch. */
@Service
public class DocumentService {

    private final DocumentRepository repository;
    private final SearchIndexService indexService;

    public DocumentService(DocumentRepository repository, SearchIndexService indexService) {
        this.repository = repository;
        this.indexService = indexService;
    }

    public Page<EnterpriseDocument> list(int page, int size) {
        return repository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public EnterpriseDocument get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document " + id + " not found"));
    }

    public EnterpriseDocument create(DocumentRequest request) {
        EnterpriseDocument doc = new EnterpriseDocument();
        apply(doc, request);
        Instant now = Instant.now();
        doc.setCreatedAt(request.createdAt() != null ? request.createdAt() : now);
        doc.setUpdatedAt(now);
        EnterpriseDocument saved = repository.save(doc);
        indexService.index(saved);
        return saved;
    }

    public EnterpriseDocument update(String id, DocumentRequest request) {
        EnterpriseDocument doc = get(id);
        apply(doc, request);
        if (request.createdAt() != null) {
            doc.setCreatedAt(request.createdAt());
        }
        doc.setUpdatedAt(Instant.now());
        EnterpriseDocument saved = repository.save(doc);
        indexService.index(saved);
        return saved;
    }

    public void delete(String id) {
        EnterpriseDocument doc = get(id);
        repository.delete(doc);
        indexService.delete(doc.getId());
    }

    private static void apply(EnterpriseDocument doc, DocumentRequest request) {
        doc.setTitle(request.title().trim());
        doc.setContent(request.content());
        doc.setCategory(request.category().trim());
        doc.setTags(request.tags() == null ? List.of() : request.tags().stream().map(String::trim).distinct().toList());
        doc.setAuthor(request.author());
    }
}
