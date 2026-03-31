package com.pharmaCx.dms.domain.repository;

import com.pharmaCx.dms.domain.model.DocumentTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DocumentTemplateRepository extends MongoRepository<DocumentTemplate, String> {

    List<DocumentTemplate> findByDocumentTypeId(String documentTypeId);

    List<DocumentTemplate> findByDocumentTypeIdAndLatestTrueAndActiveTrue(String documentTypeId);

    List<DocumentTemplate> findByLatestTrueAndActiveTrue();

    List<DocumentTemplate> findByNameAndDocumentTypeIdOrderByVersionDesc(String name, String documentTypeId);

    List<DocumentTemplate> findAllByOrderByNameAscVersionDesc();

    List<DocumentTemplate> findByDocumentTypeIdOrderByNameAscVersionDesc(String documentTypeId);
}
