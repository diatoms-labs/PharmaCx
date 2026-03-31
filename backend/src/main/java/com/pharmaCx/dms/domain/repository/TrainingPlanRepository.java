package com.pharmaCx.dms.domain.repository;

import com.pharmaCx.dms.domain.model.TrainingPlan;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TrainingPlanRepository extends MongoRepository<TrainingPlan, String> {

    Optional<TrainingPlan> findByDocumentId(String documentId);
}
