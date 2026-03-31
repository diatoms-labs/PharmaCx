package com.pharmaCx.dms.domain.repository;

import com.pharmaCx.dms.domain.model.DocumentTypeConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentTypeConfigRepository extends MongoRepository<DocumentTypeConfig, String> {

    Optional<DocumentTypeConfig> findByCode(String code);

    List<DocumentTypeConfig> findByActiveTrue();

    List<DocumentTypeConfig> findByOwnerUnitId(String ownerUnitId);

    List<DocumentTypeConfig> findByAllowedUnitIdsContaining(String unitId);

    boolean existsByCode(String code);
}
