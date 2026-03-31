package com.pharmaCx.dms.domain.repository;

import com.pharmaCx.dms.domain.enums.DocumentStatus;
import com.pharmaCx.dms.domain.model.ControlledDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ControlledDocumentRepository extends MongoRepository<ControlledDocument, String> {

    List<ControlledDocument> findByStatus(DocumentStatus status);

    Optional<ControlledDocument> findByExternalPath(String externalPath);

    Page<ControlledDocument> findByStatus(DocumentStatus status, Pageable pageable);

    List<ControlledDocument> findByDocumentTypeIdAndUnitId(String documentTypeId, String unitId);

    Page<ControlledDocument> findByDocumentTypeIdAndUnitId(String documentTypeId, String unitId, Pageable pageable);

    List<ControlledDocument> findByDocumentTypeId(String documentTypeId);

    List<ControlledDocument> findByUnitId(String unitId);

    List<ControlledDocument> findByRequestedBy(String userId);

    List<ControlledDocument> findByAuthorId(String userId);

    Optional<ControlledDocument> findByDocumentNumber(String documentNumber);

    long countByDocumentTypeIdAndStatus(String documentTypeId, DocumentStatus status);

    long countByStatus(DocumentStatus status);

    List<ControlledDocument> findByStatusIn(List<DocumentStatus> statuses);

    List<ControlledDocument> findByStatusAndUnitId(DocumentStatus status, String unitId);

    @Query("{ 'workflowSteps': { $elemMatch: { 'assignedToUserId': ?0, 'status': 'IN_PROGRESS' } } }")
    List<ControlledDocument> findByWorkflowStepAssignee(String userId);

    @Query("{ 'status': { $nin: ['PUBLISHED', 'RETIRED'] }, $or: [ { 'requestedBy': ?0 }, { 'authorId': ?0 }, { 'workflowSteps.assignedToUserId': ?0 } ] }")
    List<ControlledDocument> findActiveDocumentsByUser(String userId);
}
