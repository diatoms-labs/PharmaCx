package com.pharmaCx.dms.ai.repository;

import com.pharmaCx.dms.ai.model.DocumentChunk;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface DocumentChunkRepository extends MongoRepository<DocumentChunk, String> {

    List<DocumentChunk> findByDocumentId(String documentId);

    List<DocumentChunk> findByDocumentIdIn(Collection<String> documentIds);

    void deleteByDocumentId(String documentId);

    void deleteBySource(String source);

    long countByDocumentId(String documentId);
    
    List<DocumentChunk> findBySource(String source);
}
