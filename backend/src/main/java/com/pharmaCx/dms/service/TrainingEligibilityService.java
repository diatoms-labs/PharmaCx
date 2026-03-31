package com.pharmaCx.dms.service;

import com.pharmaCx.dms.domain.enums.DocumentStatus;
import com.pharmaCx.dms.domain.enums.TrainingStatus;
import com.pharmaCx.dms.domain.enums.UserRole;
import com.pharmaCx.dms.domain.model.AppUser;
import com.pharmaCx.dms.domain.repository.AppUserRepository;
import com.pharmaCx.dms.domain.repository.ControlledDocumentRepository;
import com.pharmaCx.dms.domain.repository.TrainingAssignmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrainingEligibilityService {

    private final TrainingAssignmentRepository trainingRepo;
    private final ControlledDocumentRepository documentRepo;
    private final AppUserRepository userRepo;

    public TrainingEligibilityService(TrainingAssignmentRepository trainingRepo,
                                      ControlledDocumentRepository documentRepo,
                                      AppUserRepository userRepo) {
        this.trainingRepo = trainingRepo;
        this.documentRepo = documentRepo;
        this.userRepo = userRepo;
    }

    public boolean isEligible(String userId, String documentTypeId) {
        AppUser user = userRepo.findById(userId).orElse(null);
        if (user != null && (user.getRole() == UserRole.SYSTEM_ADMIN || user.getRole() == UserRole.DIRECTOR)) {
            return true;
        }

        long publishedCount = documentRepo.countByDocumentTypeIdAndStatus(
                documentTypeId, DocumentStatus.PUBLISHED);
        if (publishedCount == 0) {
            return true;
        }

        return !trainingRepo.findByTraineeUserIdAndDocumentTypeIdAndStatus(
                userId, documentTypeId, TrainingStatus.COMPLETED).isEmpty();
    }

    public List<AppUser> getEligibleUsers(String documentTypeId) {
        return userRepo.findByActiveTrue().stream()
                .filter(u -> isEligible(u.getId(), documentTypeId))
                .collect(Collectors.toList());
    }
}
