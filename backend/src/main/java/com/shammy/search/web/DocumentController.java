package com.shammy.search.web;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.shammy.search.dto.DocumentRequest;
import com.shammy.search.dto.PageResponse;
import com.shammy.search.model.EnterpriseDocument;
import com.shammy.search.service.DocumentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<EnterpriseDocument> list(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        Page<EnterpriseDocument> result = service.list(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return new PageResponse<>(result.getContent(), result.getTotalElements(), result.getNumber(), result.getSize());
    }

    @GetMapping("/{id}")
    public EnterpriseDocument get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnterpriseDocument create(@Valid @RequestBody DocumentRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public EnterpriseDocument update(@PathVariable String id, @Valid @RequestBody DocumentRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
