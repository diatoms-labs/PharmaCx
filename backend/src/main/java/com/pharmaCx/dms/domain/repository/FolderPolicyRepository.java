package com.pharmaCx.dms.domain.repository;

import com.pharmaCx.dms.domain.model.FolderPolicy;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FolderPolicyRepository extends MongoRepository<FolderPolicy, String> {

    Optional<FolderPolicy> findByFolderId(String folderId);

    List<FolderPolicy> findByOwnerId(String ownerId);

    List<FolderPolicy> findByOwnerUnitId(String ownerUnitId);

    List<FolderPolicy> findByAccessScope(String accessScope);

    List<FolderPolicy> findByAllowedUnitIdsContaining(String unitId);
}
