package com.shammy.search.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.shammy.search.model.EnterpriseDocument;

public interface DocumentRepository extends MongoRepository<EnterpriseDocument, String> {
}
