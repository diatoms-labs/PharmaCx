package com.pharmaCx.dms.domain.repository;

import com.pharmaCx.dms.domain.enums.TrainingStatus;
import com.pharmaCx.dms.domain.model.TrainingAssignment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TrainingAssignmentRepository extends MongoRepository<TrainingAssignment, String> {

    List<TrainingAssignment> findByTraineeUserId(String userId);

    List<TrainingAssignment> findByTraineeUserIdAndStatus(String userId, TrainingStatus status);

    List<TrainingAssignment> findByDocumentId(String documentId);

    List<TrainingAssignment> findByTraineeUserIdAndDocumentTypeIdAndStatus(
            String userId, String documentTypeId, TrainingStatus status);

    List<TrainingAssignment> findByTraineeUserIdOrderByAssignedAtDesc(String userId);

    long countByDocumentIdAndStatus(String documentId, TrainingStatus status);

    long countByDocumentId(String documentId);

    List<TrainingAssignment> findByAssignedByUserIdOrderByAssignedAtDesc(String userId);

    List<TrainingAssignment> findByDocumentIdAndTraineeUserId(String documentId, String traineeUserId);

    List<TrainingAssignment> findByUnitIdAndStatusIn(String unitId, List<TrainingStatus> statuses);
}
