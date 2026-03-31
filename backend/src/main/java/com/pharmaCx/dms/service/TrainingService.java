package com.pharmaCx.dms.service;

import com.pharmaCx.dms.domain.enums.*;
import com.pharmaCx.dms.domain.model.*;
import com.pharmaCx.dms.domain.repository.AppUserRepository;
import com.pharmaCx.dms.domain.repository.ControlledDocumentRepository;
import com.pharmaCx.dms.domain.repository.TrainingAssignmentRepository;
import com.pharmaCx.dms.domain.repository.TrainingPlanRepository;
import com.pharmaCx.dms.exception.AccessDeniedException;
import com.pharmaCx.dms.exception.ResourceNotFoundException;
import com.pharmaCx.dms.exception.TrainingStateException;
import com.pharmaCx.dms.exception.WorkflowStateException;
import com.pharmaCx.dms.security.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
public class TrainingService {

    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

    private static final EnumSet<UserRole> MANAGER_ROLES = EnumSet.of(
            UserRole.MANAGER, UserRole.HEAD_OF_DEPARTMENT, UserRole.DIRECTOR, UserRole.SYSTEM_ADMIN);

    private final TrainingAssignmentRepository assignmentRepo;
    private final TrainingPlanRepository planRepo;
    private final AppUserRepository userRepo;
    private final ControlledDocumentRepository documentRepo;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public TrainingService(TrainingAssignmentRepository assignmentRepo, TrainingPlanRepository planRepo,
                           AppUserRepository userRepo, ControlledDocumentRepository documentRepo,
                           AuditService auditService, CurrentUserService currentUserService) {
        this.assignmentRepo = assignmentRepo;
        this.planRepo = planRepo;
        this.userRepo = userRepo;
        this.documentRepo = documentRepo;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
    }

    public void triggerTraining(ControlledDocument doc) {
        auditService.logSystem(AuditAction.TRAINING_ASSIGNED, ResourceType.TRAINING, doc.getId(),
                doc.getDocumentNumber() + " - Training available for assignment");
        log.info("Training available for document: {} ({})", doc.getDocumentNumber(), doc.getTitle());
    }

    public List<TrainingAssignment> assignTraining(String documentId, List<String> traineeUserIds, int dueDays) {
        String managerId = currentUserService.getCurrentUserId();
        AppUser manager = findUserOrThrow(managerId);
        requireManagerRole(manager);

        ControlledDocument doc = documentRepo.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        if (doc.getStatus() != DocumentStatus.PUBLISHED) {
            throw new WorkflowStateException("Can only assign training for published documents");
        }

        if (dueDays <= 0) dueDays = 14;
        Instant now = Instant.now();
        Instant dueDate = now.plus(dueDays, ChronoUnit.DAYS);

        List<TrainingAssignment> created = new ArrayList<>();

        for (String traineeUserId : traineeUserIds) {
            List<TrainingAssignment> existing = assignmentRepo.findByDocumentIdAndTraineeUserId(documentId, traineeUserId);
            boolean alreadyActive = existing.stream()
                    .anyMatch(a -> a.getStatus() != TrainingStatus.FAILED);
            if (alreadyActive) continue;

            AppUser trainee = userRepo.findById(traineeUserId).orElse(null);
            if (trainee == null || !trainee.isActive()) continue;

            TrainingAssignment assignment = new TrainingAssignment();
            assignment.setDocumentId(doc.getId());
            assignment.setDocumentTitle(doc.getTitle());
            assignment.setDocumentNumber(doc.getDocumentNumber());
            assignment.setDocumentTypeId(doc.getDocumentTypeId());
            assignment.setDocumentVersion(doc.getVersion());
            assignment.setTraineeUserId(trainee.getId());
            assignment.setTraineeUsername(trainee.getFullName());
            assignment.setUnitId(trainee.getUnitId());
            assignment.setAssignedByUserId(managerId);
            assignment.setAssignedByUsername(manager.getFullName());
            assignment.setStatus(TrainingStatus.ASSIGNED);
            assignment.setAssignedAt(now);
            assignment.setDueDate(dueDate);
            assignment.setQuizAttempts(new ArrayList<>());
            assignmentRepo.save(assignment);
            created.add(assignment);
        }

        auditService.log(AuditAction.TRAINING_ASSIGNED, ResourceType.TRAINING, documentId,
                doc.getDocumentNumber() + " - Assigned to " + created.size() + " users by " + manager.getFullName(), null);

        return created;
    }

    public TrainingAssignment reassignTraining(String assignmentId) {
        String managerId = currentUserService.getCurrentUserId();
        AppUser manager = findUserOrThrow(managerId);
        requireManagerRole(manager);

        TrainingAssignment a = getAssignment(assignmentId);
        if (a.getStatus() != TrainingStatus.FAILED) {
            throw new TrainingStateException("Can only reassign FAILED training assignments");
        }

        a.setStatus(TrainingStatus.ASSIGNED);
        a.setQuizAttempts(new ArrayList<>());
        a.setQuizPassedAt(null);
        a.setFailedAt(null);
        a.setReadAt(null);
        a.setReadDurationSeconds(0);
        a.setAcknowledgedAt(null);
        a.setSignatureData(null);
        a.setAssignedByUserId(managerId);
        a.setAssignedByUsername(manager.getFullName());
        a.setAssignedAt(Instant.now());
        a.setDueDate(Instant.now().plus(14, ChronoUnit.DAYS));
        a = assignmentRepo.save(a);

        auditService.log(AuditAction.TRAINING_ASSIGNED, ResourceType.TRAINING, assignmentId,
                a.getDocumentNumber() + " - Re-assigned to " + a.getTraineeUsername() + " by " + manager.getFullName(), null);

        return a;
    }

    public List<ControlledDocument> getManageableDocuments() {
        String userId = currentUserService.getCurrentUserId();
        AppUser user = findUserOrThrow(userId);

        // Directors and System Admins can manage training for all published documents
        if (user.getRole() == UserRole.DIRECTOR || user.getRole() == UserRole.SYSTEM_ADMIN) {
            return documentRepo.findByStatus(DocumentStatus.PUBLISHED);
        }
        // Other managers see only their department's published documents
        return documentRepo.findByStatusAndUnitId(DocumentStatus.PUBLISHED, user.getUnitId());
    }

    public TrainingAssignment startReading(String assignmentId) {
        TrainingAssignment a = getAssignment(assignmentId);
        a.setStatus(TrainingStatus.IN_PROGRESS);
        a.setReadAt(Instant.now());
        a = assignmentRepo.save(a);

        auditService.log(AuditAction.TRAINING_STARTED, ResourceType.TRAINING, assignmentId,
                a.getDocumentNumber() + " - Reading started by " + a.getTraineeUsername(), null);

        return a;
    }

    public TrainingAssignment completeReading(String assignmentId, long durationSeconds) {
        TrainingAssignment a = getAssignment(assignmentId);
        a.setStatus(TrainingStatus.READ);
        a.setReadDurationSeconds(durationSeconds);
        a = assignmentRepo.save(a);

        auditService.log(AuditAction.TRAINING_DOCUMENT_READ, ResourceType.TRAINING, assignmentId,
                a.getDocumentNumber() + " - Document read by " + a.getTraineeUsername(), null);

        return a;
    }

    public TrainingAssignment submitQuiz(String assignmentId, List<QuizAnswer> answers, List<TrainingQuestion> questions) {
        TrainingAssignment a = getAssignment(assignmentId);

        TrainingPlan plan = planRepo.findByDocumentId(a.getDocumentId()).orElse(null);
        int passingPercent = plan != null ? plan.getPassingScorePercent() : 80;
        int maxAttempts = plan != null ? plan.getMaxQuizAttempts() : 3;

        if (a.getQuizAttempts().size() >= maxAttempts) {
            throw new TrainingStateException("Maximum quiz attempts reached");
        }

        if (a.getStatus() == TrainingStatus.FAILED) {
            throw new TrainingStateException("Training has been marked as failed. Contact your manager for reassignment.");
        }

        int correct = 0;
        for (QuizAnswer answer : answers) {
            for (TrainingQuestion q : questions) {
                if (q.getQuestionId().equals(answer.getQuestionId())) {
                    if (answer.getSelectedAnswerIndex() == q.getCorrectAnswerIndex()) {
                        correct++;
                    }
                    break;
                }
            }
        }

        boolean passed = questions.isEmpty() || ((correct * 100) / questions.size()) >= passingPercent;

        QuizAttempt attempt = new QuizAttempt();
        attempt.setAttemptNumber(a.getQuizAttempts().size() + 1);
        attempt.setAnswers(answers);
        attempt.setScore(correct);
        attempt.setTotalQuestions(questions.size());
        attempt.setPassed(passed);
        attempt.setAttemptedAt(Instant.now());

        a.getQuizAttempts().add(attempt);

        if (passed) {
            a.setStatus(TrainingStatus.QUIZ_PASSED);
            a.setQuizPassedAt(Instant.now());

            auditService.log(AuditAction.TRAINING_QUIZ_PASSED, ResourceType.TRAINING, assignmentId,
                    a.getDocumentNumber() + " - Quiz passed by " + a.getTraineeUsername()
                            + " (score: " + correct + "/" + questions.size() + ", attempt " + attempt.getAttemptNumber() + ")", null);
        } else if (a.getQuizAttempts().size() >= maxAttempts) {
            a.setStatus(TrainingStatus.FAILED);
            a.setFailedAt(Instant.now());

            auditService.log(AuditAction.TRAINING_QUIZ_FAILED, ResourceType.TRAINING, assignmentId,
                    a.getDocumentNumber() + " - Training FAILED by " + a.getTraineeUsername()
                            + " after " + maxAttempts + " attempts (final score: " + correct + "/" + questions.size() + ")", null);

            log.warn("Training FAILED: {} for document {} — all {} attempts exhausted",
                    a.getTraineeUsername(), a.getDocumentNumber(), maxAttempts);
        } else {
            auditService.log(AuditAction.TRAINING_QUIZ_ATTEMPTED, ResourceType.TRAINING, assignmentId,
                    a.getDocumentNumber() + " - Quiz attempt " + attempt.getAttemptNumber()
                            + " by " + a.getTraineeUsername() + " (score: " + correct + "/" + questions.size() + ")", null);
        }

        return assignmentRepo.save(a);
    }

    public TrainingAssignment acknowledge(String assignmentId, String signatureData) {
        TrainingAssignment a = getAssignment(assignmentId);
        if (a.getStatus() != TrainingStatus.QUIZ_PASSED) {
            throw new TrainingStateException("Must pass quiz before acknowledging");
        }
        a.setStatus(TrainingStatus.COMPLETED);
        a.setAcknowledgedAt(Instant.now());
        a.setSignatureData(signatureData);
        a = assignmentRepo.save(a);

        auditService.log(AuditAction.TRAINING_COMPLETED, ResourceType.TRAINING, assignmentId,
                a.getDocumentNumber() + " - Training completed by " + a.getTraineeUsername(), null);

        return a;
    }

    public List<TrainingAssignment> getMyAssignments() {
        String userId = currentUserService.getCurrentUserId();
        return assignmentRepo.findByTraineeUserIdOrderByAssignedAtDesc(userId);
    }

    public List<TrainingAssignment> getByDocument(String documentId) {
        return assignmentRepo.findByDocumentId(documentId);
    }

    public List<TrainingAssignment> getMyManagedAssignments() {
        String userId = currentUserService.getCurrentUserId();
        return assignmentRepo.findByAssignedByUserIdOrderByAssignedAtDesc(userId);
    }

    public TrainingAssignment getAssignment(String id) {
        return assignmentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Training assignment", id));
    }

    private AppUser findUserOrThrow(String userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private void requireManagerRole(AppUser user) {
        if (!MANAGER_ROLES.contains(user.getRole())) {
            throw new AccessDeniedException("Manager or above role required");
        }
    }
}
