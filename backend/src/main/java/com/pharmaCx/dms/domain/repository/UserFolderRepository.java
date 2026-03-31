package com.pharmaCx.dms.domain.repository;

import com.pharmaCx.dms.domain.model.UserFolder;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface UserFolderRepository extends MongoRepository<UserFolder, String> {

    List<UserFolder> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

    List<UserFolder> findByOwnerIdAndParentFolderIdIsNullOrderByCreatedAtDesc(String ownerId);

    List<UserFolder> findByOwnerIdAndParentFolderIdOrderByCreatedAtDesc(String ownerId, String parentFolderId);

    List<UserFolder> findBySharedWithUserIdsContainingOrderByCreatedAtDesc(String userId);

    // Replaces findByDepartmentAndSharedWithAllTrue — now uses ownerUnitId
    List<UserFolder> findByOwnerUnitIdAndSharedWithAllTrueOrderByCreatedAtDesc(String ownerUnitId);

    // Department (unit) folders — folders of type DEPARTMENT for a given unit
    List<UserFolder> findByOwnerUnitIdAndFolderTypeOrderByCreatedAtDesc(String ownerUnitId, String folderType);

    List<UserFolder> findByFolderTypeOrderByCreatedAtDesc(String folderType);

    List<UserFolder> findBySharedWithAllTrueOrderByCreatedAtDesc();

    List<UserFolder> findByPolicyIdOrderByCreatedAtDesc(String policyId);
}
