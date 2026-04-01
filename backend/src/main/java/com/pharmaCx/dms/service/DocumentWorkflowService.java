package com.pharmaCx.dms.service;

import com.pharmaCx.dms.ai.service.DocumentIndexService;
import com.pharmaCx.dms.domain.enums.*;
import com.pharmaCx.dms.domain.model.*;
import com.pharmaCx.dms.domain.repository.*;
import com.pharmaCx.dms.exception.AccessDeniedException;
import com.pharmaCx.dms.exception.ResourceNotFoundException;
import com.pharmaCx.dms.exception.ValidationException;
import com.pharmaCx.dms.exception.WorkflowStateException;
import com.pharmaCx.dms.security.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentWorkflowService {

    private final ControlledDocumentRepository documentRepo;
    private final CurrentUserService currentUserService;
    private final AuthService authService;
    private final AuditService auditService;
    private final TrainingEligibilityService eligibilityService;
    private final TrainingService trainingService;
    private final DocumentNumberService documentNumberService;
    private final TemplateProcessingService templateProcessingService;
    private final DocumentProtectionService documentProtectionService;
    private final OnlyOfficeService onlyOfficeService;
    private final DocumentIndexService documentIndexService;
    private final DocumentTemplateRepository templateRepo;
    private final UserFolderRepository folderRepo;
    private final AppUserRepository userRepo;
    private final OrganizationalUnitRepository orgUnitRepo;

    private static final String QA_UNIT_CODE = "QA";

    private static final String[] STEP_NAMES = {
            "Request", "Request Selection", "Author Draft", "Peer Review", "QA Review", "Approval", "Publish"
    };
    private static final StepType[] STEP_TYPES = {
            StepType.REQUEST, StepType.QA_PREPARE, StepType.AUTHOR_EDIT, StepType.PEER_REVIEW,
            StepType.QA_REVIEW, StepType.APPROVE, StepType.PUBLISH
    };

    public DocumentWorkflowService(ControlledDocumentRepository documentRepo,
                                   CurrentUserService currentUserService,
                                   AuthService authService,
                                   AuditService auditService,
                                   TrainingEligibilityService eligibilityService,
                                   TrainingService trainingService,
                                   DocumentNumberService documentNumberService,
                                   TemplateProcessingService templateProcessingService,
                                   DocumentProtectionService documentProtectionService,
                                   OnlyOfficeService onlyOfficeService,
                                   DocumentIndexService documentIndexService,
                                   DocumentTemplateRepository templateRepo,
                                   UserFolderRepository folderRepo,
                                   AppUserRepository userRepo,
                                   OrganizationalUnitRepository orgUnitRepo) {
        this.documentRepo = documentRepo;
        this.currentUserService = currentUserService;
        this.authService = authService;
        this.auditService = auditService;
        this.eligibilityService = eligibilityService;
        this.trainingService = trainingService;
        this.documentNumberService = documentNumberService;
        this.templateProcessingService = templateProcessingService;
        this.documentProtectionService = documentProtectionService;
        this.onlyOfficeService = onlyOfficeService;
        this.documentIndexService = documentIndexService;
        this.templateRepo = templateRepo;
        this.folderRepo = folderRepo;
        this.userRepo = userRepo;
        this.orgUnitRepo = orgUnitRepo;
    }

    public ControlledDocument submitRequest(String title, String documentTypeId, String unitId, String justification) {
        String userId = currentUserService.getCurrentUserId();
        String username = currentUserService.getCurrentUsername();

        ControlledDocument doc = new ControlledDocument();
        doc.setTitle(title);
        doc.setDocumentTypeId(documentTypeId);
        doc.setUnitId(unitId);
        doc.setVersion(1);
        doc.setStatus(DocumentStatus.REQUESTED);
        doc.setCurrentStepIndex(0);
        doc.setRequestedBy(userId);
        doc.setAuthorId(userId);
        doc.setCreatedAt(Instant.now());
        doc.setUpdatedAt(Instant.now());

        List<WorkflowStep> steps = new ArrayList<>();
        for (int i = 0; i < STEP_NAMES.length; i++) {
            WorkflowStep step = new WorkflowStep();
            step.setStepIndex(i);
            step.setName(STEP_NAMES[i]);
            step.setType(STEP_TYPES[i]);
            step.setStatus(i == 0 ? StepStatus.COMPLETED : StepStatus.PENDING);
            if (i == 0) {
                step.setAssignedToUserId(userId);
                step.setAssignedToUsername(username);
                step.setStartedAt(Instant.now());
                step.setCompletedAt(Instant.now());
                step.setComment(justification);
            }
            steps.add(step);
        }
        doc.setWorkflowSteps(steps);
        doc.setTrainingQuestions(new ArrayList<>());

        doc.setCurrentStepIndex(1);
        doc.setStatus(DocumentStatus.QA_PREPARATION);
        WorkflowStep initiationStep = doc.getWorkflowSteps().get(1);
        initiationStep.setStatus(StepStatus.IN_PROGRESS);
        initiationStep.setStartedAt(Instant.now());

        // Auto-assign Request Selection to a QA unit user
        String qaUnitId = resolveQaUnitId();
        List<AppUser> qaUsers = qaUnitId != null
                ? userRepo.findByUnitIdAndActiveTrue(qaUnitId)
                : new ArrayList<>();

        AppUser assignedQa = null;
        AppUser requester = userRepo.findById(userId).orElse(null);
        boolean requesterIsQa = requester != null && qaUnitId != null
                && qaUnitId.equals(requester.getUnitId());

        if (requesterIsQa) {
            assignedQa = qaUsers.stream()
                    .filter(u -> !u.getId().equals(userId))
                    .findFirst().orElse(null);
        }
        if (assignedQa == null && !qaUsers.isEmpty()) {
            assignedQa = qaUsers.get(0);
        }
        if (assignedQa != null) {
            initiationStep.setAssignedToUserId(assignedQa.getId());
            initiationStep.setAssignedToUsername(assignedQa.getFullName());
        }

        doc = documentRepo.save(doc);
        auditService.log(AuditAction.DOCUMENT_REQUESTED, ResourceType.DOCUMENT, doc.getId(), doc.getTitle(), justification);
        return doc;
    }

    public ControlledDocument prepareDocument(String docId, String documentNumber, String templateId) {
        ControlledDocument doc = getDocument(docId);
        validateStep(doc, 1, DocumentStatus.QA_PREPARATION);
        validateAssignment(doc, 1);

        String userId = currentUserService.getCurrentUserId();
        String username = currentUserService.getCurrentUsername();

        if (documentNumber == null || documentNumber.isBlank()) {
            documentNumber = documentNumberService.generateNumber(doc.getDocumentTypeId(), doc.getUnitId());
        }
        doc.setDocumentNumber(documentNumber);
        doc.setQaPreparerId(userId);

        DocumentTemplate selectedTemplate = resolveTemplate(templateId, doc.getDocumentTypeId());

        if (selectedTemplate != null) {
            doc.setTemplateFileId(selectedTemplate.getFileStorageId());
            String newFileId = templateProcessingService.processTemplate(selectedTemplate, doc, documentNumber);
            if (newFileId != null) {
                doc.setDocumentFileId(newFileId);
                documentProtectionService.stripProtection(newFileId);
            }
        }

        WorkflowStep qaStep = doc.getWorkflowSteps().get(1);
        qaStep.setAssignedToUserId(userId);
        qaStep.setAssignedToUsername(username);
        qaStep.setStatus(StepStatus.COMPLETED);
        qaStep.setCompletedAt(Instant.now());

        // Next Step: Author Draft
        doc.setCurrentStepIndex(2);
        doc.setStatus(DocumentStatus.AUTHOR_DRAFT);
        WorkflowStep authorStep = doc.getWorkflowSteps().get(2);
        authorStep.setAssignedToUserId(doc.getAuthorId());
        
        // Use userRepo directly to handle cases where author might be missing (e.g. data mismatch or deleted user)
        String authorName = userRepo.findById(doc.getAuthorId())
                .map(AppUser::getFullName)
                .orElse("Original Author (" + doc.getAuthorId() + ")");
                
        authorStep.setAssignedToUsername(authorName);
        authorStep.setAssignedByUserId(userId);
        authorStep.setStatus(StepStatus.IN_PROGRESS);
        authorStep.setStartedAt(Instant.now());

        doc.setUpdatedAt(Instant.now());
        doc = documentRepo.save(doc);

        auditService.log(AuditAction.DOCUMENT_PREPARED, ResourceType.DOCUMENT, doc.getId(),
                doc.getDocumentNumber() + " - " + doc.getTitle(), null);
        return doc;
    }

    private DocumentTemplate resolveTemplate(String templateId, String documentTypeId) {
        if (templateId != null && !templateId.isBlank()) {
            return templateRepo.findById(templateId).orElse(null);
        }
        List<DocumentTemplate> templates = templateRepo.findByDocumentTypeIdAndLatestTrueAndActiveTrue(documentTypeId);
        if (!templates.isEmpty()) return templates.get(0);
        templates = templateRepo.findByDocumentTypeId(documentTypeId);
        return templates.isEmpty() ? null : templates.get(0);
    }

    public ControlledDocument submitDraft(String docId, String peerReviewerUserId,
                                           String qaReviewerUserId, String approverUserId,
                                           List<TrainingQuestion> questions) {
        ControlledDocument doc = getDocument(docId);
        validateStep(doc, 2, DocumentStatus.AUTHOR_DRAFT);
        validateAssignment(doc, 2);

        if (!eligibilityService.isEligible(peerReviewerUserId, doc.getDocumentTypeId())) {
            throw new ValidationException("Peer reviewer has not completed TMS training for this document type");
        }
        if (!eligibilityService.isEligible(qaReviewerUserId, doc.getDocumentTypeId())) {
            throw new ValidationException("QA reviewer has not completed TMS training for this document type");
        }

        String userId = currentUserService.getCurrentUserId();

        if (questions != null && !questions.isEmpty()) {
            doc.setTrainingQuestions(questions);
        }

        doc.setPeerReviewerUserId(peerReviewerUserId);
        doc.setQaReviewerUserId(qaReviewerUserId);
        doc.setApproverUserId(approverUserId);

        WorkflowStep qaReviewStep = doc.getWorkflowSteps().get(4);
        AppUser qaReviewer = authService.getUserById(qaReviewerUserId);
        qaReviewStep.setAssignedToUserId(qaReviewerUserId);
        qaReviewStep.setAssignedToUsername(qaReviewer.getUsername());
        qaReviewStep.setAssignedByUserId(userId);

        WorkflowStep approvalStep = doc.getWorkflowSteps().get(5);
        AppUser approver = authService.getUserById(approverUserId);
        approvalStep.setAssignedToUserId(approverUserId);
        approvalStep.setAssignedToUsername(approver.getUsername());
        approvalStep.setAssignedByUserId(userId);

        WorkflowStep authorStep = doc.getWorkflowSteps().get(2);
        authorStep.setStatus(StepStatus.COMPLETED);
        authorStep.setCompletedAt(Instant.now());

        doc.setCurrentStepIndex(3);
        doc.setStatus(DocumentStatus.PEER_REVIEW);
        WorkflowStep peerStep = doc.getWorkflowSteps().get(3);
        AppUser reviewer = authService.getUserById(peerReviewerUserId);
        peerStep.setAssignedToUserId(peerReviewerUserId);
        peerStep.setAssignedToUsername(reviewer.getUsername());
        peerStep.setAssignedByUserId(userId);
        peerStep.setStatus(StepStatus.IN_PROGRESS);
        peerStep.setStartedAt(Instant.now());

        onlyOfficeService.forceSave(doc);
        documentProtectionService.applyTrackedChangesProtection(doc.getDocumentFileId());

        doc.setUpdatedAt(Instant.now());
        doc = documentRepo.save(doc);

        auditService.log(AuditAction.DOCUMENT_SUBMITTED_FOR_REVIEW, ResourceType.DOCUMENT, doc.getId(),
                doc.getDocumentNumber() + " - " + doc.getTitle(), null);
        return doc;
    }

    public ControlledDocument submitReview(String docId, boolean approved, String comment, String rejectionReason) {
        ControlledDocument doc = getDocument(docId);
        int currentStep = doc.getCurrentStepIndex();

        if (currentStep != 3 && currentStep != 4) {
            throw new WorkflowStateException("Document is not in a review step");
        }
        validateAssignment(doc, currentStep);

        WorkflowStep step = doc.getWorkflowSteps().get(currentStep);
        onlyOfficeService.forceSave(doc);

        if (!approved) {
            step.setStatus(StepStatus.REJECTED);
            step.setRejectionReason(rejectionReason);
            step.setComment(comment);
            step.setCompletedAt(Instant.now());

            doc.setCurrentStepIndex(2);
            doc.setStatus(DocumentStatus.AUTHOR_DRAFT);
            WorkflowStep authorStep = doc.getWorkflowSteps().get(2);
            authorStep.setStatus(StepStatus.IN_PROGRESS);
            authorStep.setStartedAt(Instant.now());

            documentProtectionService.stripProtection(doc.getDocumentFileId());

            doc.setUpdatedAt(Instant.now());
            doc = documentRepo.save(doc);

            auditService.log(AuditAction.DOCUMENT_REJECTED, ResourceType.DOCUMENT, doc.getId(),
                    doc.getDocumentNumber() + " - " + doc.getTitle(), rejectionReason);
            return doc;
        }

        step.setStatus(StepStatus.COMPLETED);
        step.setComment(comment);
        step.setCompletedAt(Instant.now());

        documentProtectionService.acceptAllTrackedChanges(doc.getDocumentFileId());

        int nextStep = currentStep + 1;
        doc.setCurrentStepIndex(nextStep);
        DocumentStatus nextStatus = statusForStep(nextStep);
        doc.setStatus(nextStatus);

        WorkflowStep nextWorkflowStep = doc.getWorkflowSteps().get(nextStep);
        nextWorkflowStep.setStatus(StepStatus.IN_PROGRESS);
        nextWorkflowStep.setStartedAt(Instant.now());

        if (nextStatus == DocumentStatus.QA_REVIEW) {
            documentProtectionService.applyTrackedChangesProtection(doc.getDocumentFileId());
        } else if (nextStatus == DocumentStatus.APPROVAL) {
            documentProtectionService.applyReadOnlyProtection(doc.getDocumentFileId());
        }

        doc.setUpdatedAt(Instant.now());
        doc = documentRepo.save(doc);

        AuditAction action = currentStep == 3 ? AuditAction.DOCUMENT_PEER_REVIEWED : AuditAction.DOCUMENT_QA_REVIEWED;
        auditService.log(action, ResourceType.DOCUMENT, doc.getId(),
                doc.getDocumentNumber() + " - " + doc.getTitle(), comment);
        return doc;
    }

    public ControlledDocument submitApproval(String docId, boolean approved, String signatureData,
                                              String comment, String rejectionReason) {
        ControlledDocument doc = getDocument(docId);
        validateStep(doc, 5, DocumentStatus.APPROVAL);
        validateAssignment(doc, 5);

        WorkflowStep approvalStep = doc.getWorkflowSteps().get(5);
        onlyOfficeService.forceSave(doc);

        if (!approved) {
            approvalStep.setStatus(StepStatus.REJECTED);
            approvalStep.setRejectionReason(rejectionReason);
            approvalStep.setComment(comment);
            approvalStep.setCompletedAt(Instant.now());

            doc.setCurrentStepIndex(2);
            doc.setStatus(DocumentStatus.AUTHOR_DRAFT);
            doc.getWorkflowSteps().get(2).setStatus(StepStatus.IN_PROGRESS);
            doc.getWorkflowSteps().get(2).setStartedAt(Instant.now());

            documentProtectionService.stripProtection(doc.getDocumentFileId());

            doc.setUpdatedAt(Instant.now());
            doc = documentRepo.save(doc);

            auditService.log(AuditAction.DOCUMENT_REJECTED, ResourceType.DOCUMENT, doc.getId(),
                    doc.getDocumentNumber() + " - " + doc.getTitle(), rejectionReason);
            return doc;
        }

        approvalStep.setStatus(StepStatus.COMPLETED);
        approvalStep.setSignatureData(signatureData);
        approvalStep.setComment(comment);
        approvalStep.setCompletedAt(Instant.now());

        auditService.log(AuditAction.DOCUMENT_APPROVED, ResourceType.DOCUMENT, doc.getId(),
                doc.getDocumentNumber() + " - " + doc.getTitle(), comment);
        return publishDocument(doc);
    }

    private ControlledDocument publishDocument(ControlledDocument doc) {
        doc.setCurrentStepIndex(6);
        doc.setStatus(DocumentStatus.PUBLISHED);
        doc.setEffectiveDate(LocalDate.now());
        doc.setNextReviewDate(LocalDate.now().plusYears(2));

        documentProtectionService.acceptAllTrackedChanges(doc.getDocumentFileId());
        documentProtectionService.clearComments(doc.getDocumentFileId());
        documentProtectionService.applyReadOnlyProtection(doc.getDocumentFileId());

        WorkflowStep publishStep = doc.getWorkflowSteps().get(6);
        publishStep.setStatus(StepStatus.COMPLETED);
        publishStep.setAssignedToUserId("SYSTEM");
        publishStep.setAssignedToUsername("System");
        publishStep.setStartedAt(Instant.now());
        publishStep.setCompletedAt(Instant.now());

        doc.setUpdatedAt(Instant.now());
        doc = documentRepo.save(doc);

        auditService.logSystem(AuditAction.DOCUMENT_PUBLISHED, ResourceType.DOCUMENT, doc.getId(),
                doc.getDocumentNumber() + " - " + doc.getTitle());

        trainingService.triggerTraining(doc);
        moveToSharedFolder(doc);
        // Trigger async AI indexing — runs in background, never blocks document publish
        documentIndexService.indexDocumentAsync(doc.getId());
        return doc;
    }

    /**
     * Auto-moves published document into its department's shared folder.
     * Folder name is derived dynamically from unit displayName and doc type code.
     */
    private void moveToSharedFolder(ControlledDocument doc) {
        // Resolve unit display name and doc type code dynamically
        String unitDisplayName = orgUnitRepo.findById(doc.getUnitId())
                .map(OrganizationalUnit::getDisplayName)
                .orElse(doc.getUnitId());

        String docTypeCode = doc.getDocumentTypeId(); // fallback to ID
        // Note: DocumentTypeConfigRepository not injected here to keep constructor manageable.
        // The folder name uses unitId as ownerUnitId reference — more reliable than display name.

        String folderName = unitDisplayName + " Documents";
        String ownerUnitId = doc.getUnitId();

        List<UserFolder> existing = folderRepo.findByOwnerUnitIdAndFolderTypeOrderByCreatedAtDesc(
                ownerUnitId, "DEPARTMENT");
        UserFolder targetFolder = existing.stream()
                .filter(f -> f.getName().equals(folderName))
                .findFirst()
                .orElse(null);

        if (targetFolder == null) {
            targetFolder = new UserFolder();
            targetFolder.setName(folderName);
            targetFolder.setOwnerId("SYSTEM");
            targetFolder.setOwnerUsername("System");
            targetFolder.setOwnerUnitId(ownerUnitId);
            targetFolder.setFolderType("DEPARTMENT");
            targetFolder.setSharedWithAll(true);
            targetFolder.setSharedWithUserIds(new ArrayList<>());
            targetFolder.setDocumentIds(new ArrayList<>());
            targetFolder.setCreatedAt(Instant.now());
            targetFolder.setUpdatedAt(Instant.now());
        }

        if (!targetFolder.getDocumentIds().contains(doc.getId())) {
            targetFolder.getDocumentIds().add(doc.getId());
            targetFolder.setUpdatedAt(Instant.now());
        }
        folderRepo.save(targetFolder);

        List<UserFolder> authorFolders = folderRepo.findByOwnerIdOrderByCreatedAtDesc(doc.getAuthorId());
        for (UserFolder folder : authorFolders) {
            if (folder.getDocumentIds().remove(doc.getId())) {
                folder.setUpdatedAt(Instant.now());
                folderRepo.save(folder);
            }
        }
    }

    public ControlledDocument retireDocument(String docId) {
        ControlledDocument doc = getDocument(docId);
        doc.setStatus(DocumentStatus.RETIRED);
        doc.setUpdatedAt(Instant.now());
        doc = documentRepo.save(doc);
        auditService.log(AuditAction.DOCUMENT_RETIRED, ResourceType.DOCUMENT, doc.getId(),
                doc.getDocumentNumber() + " - " + doc.getTitle(), null);
        return doc;
    }

    public ControlledDocument getDocument(String docId) {
        ControlledDocument doc = documentRepo.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", docId));
        if (!canUserViewDocument(doc)) {
            throw new AccessDeniedException("You do not have permission to view this document");
        }
        return doc;
    }

    public Page<ControlledDocument> getByStatus(DocumentStatus status, Pageable pageable) {
        return documentRepo.findByStatus(status, pageable);
    }

    public Page<ControlledDocument> getByTypeAndUnit(String documentTypeId, String unitId, Pageable pageable) {
        return documentRepo.findByDocumentTypeIdAndUnitId(documentTypeId, unitId, pageable);
    }

    public List<ControlledDocument> getMyTasks() {
        String userId = currentUserService.getCurrentUserId();
        return documentRepo.findByWorkflowStepAssignee(userId).stream()
                .filter(doc -> {
                    int step = doc.getCurrentStepIndex();
                    if (step >= 0 && step < doc.getWorkflowSteps().size()) {
                        WorkflowStep ws = doc.getWorkflowSteps().get(step);
                        return userId.equals(ws.getAssignedToUserId()) && ws.getStatus() == StepStatus.IN_PROGRESS;
                    }
                    return false;
                })
                .toList();
    }

    public List<ControlledDocument> getMyDocuments() {
        String userId = currentUserService.getCurrentUserId();
        return documentRepo.findActiveDocumentsByUser(userId);
    }

    public Page<ControlledDocument> getPendingRequests(Pageable pageable) {
        return documentRepo.findByStatus(DocumentStatus.QA_PREPARATION, pageable);
    }

    public List<AppUser> getEligibleReviewers(String docId) {
        ControlledDocument doc = getDocument(docId);
        return eligibilityService.getEligibleUsers(doc.getDocumentTypeId());
    }

    public List<AppUser> getEligibleApprovers() {
        List<AppUser> directors = userRepo.findByRoleAndActiveTrue(UserRole.DIRECTOR);
        List<AppUser> hods = userRepo.findByRoleAndActiveTrue(UserRole.HEAD_OF_DEPARTMENT);
        List<AppUser> result = new ArrayList<>(directors);
        result.addAll(hods);
        return result;
    }

    private void validateStep(ControlledDocument doc, int expectedStep, DocumentStatus expectedStatus) {
        if (doc.getCurrentStepIndex() != expectedStep || doc.getStatus() != expectedStatus) {
            throw new WorkflowStateException("Document is not at the expected workflow step");
        }
    }

    private void validateAssignment(ControlledDocument doc, int stepIndex) {
        String userId = currentUserService.getCurrentUserId();
        WorkflowStep step = doc.getWorkflowSteps().get(stepIndex);

        if (stepIndex == 1) {
            AppUser actor = userRepo.findById(userId).orElse(null);
            if (actor == null) {
                throw new AccessDeniedException("Assigned user record not found. Please log in again.");
            }
            String qaUnitId = resolveQaUnitId();
            if (qaUnitId == null || !qaUnitId.equals(actor.getUnitId())) {
                throw new AccessDeniedException("Only QA department users can perform Request Selection");
            }
            if (userId.equals(doc.getRequestedBy())) {
                throw new AccessDeniedException("The person who made the request cannot perform Request Selection");
            }
            return;
        }

        if (stepIndex == 2 && userId.equals(doc.getAuthorId())) {
            return;
        }

        if (!userId.equals(step.getAssignedToUserId())) {
            throw new AccessDeniedException("You are not assigned to this workflow step");
        }
    }

    private boolean canUserViewDocument(ControlledDocument doc) {
        if (doc.getStatus() == DocumentStatus.PUBLISHED || doc.getStatus() == DocumentStatus.RETIRED) {
            return true;
        }
        String userId = currentUserService.getCurrentUserId();
        if (userId.equals(doc.getRequestedBy()) || userId.equals(doc.getAuthorId())) {
            return true;
        }
        for (WorkflowStep ws : doc.getWorkflowSteps()) {
            if (userId.equals(ws.getAssignedToUserId())) return true;
        }
        
        AppUser viewer = userRepo.findById(userId).orElse(null);
        if (viewer == null) return false;
        
        String qaUnitId = resolveQaUnitId();
        if (qaUnitId != null && qaUnitId.equals(viewer.getUnitId())) return true;
        if (viewer.getRole() == UserRole.HEAD_OF_DEPARTMENT
                || viewer.getRole() == UserRole.DIRECTOR
                || viewer.getRole() == UserRole.SYSTEM_ADMIN) {
            return true;
        }
        return false;
    }

    private String resolveQaUnitId() {
        return orgUnitRepo.findByCode(QA_UNIT_CODE)
                .map(OrganizationalUnit::getId)
                .orElse(null);
    }

    private DocumentStatus statusForStep(int step) {
        return switch (step) {
            case 0 -> DocumentStatus.REQUESTED;
            case 1 -> DocumentStatus.QA_PREPARATION;
            case 2 -> DocumentStatus.AUTHOR_DRAFT;
            case 3 -> DocumentStatus.PEER_REVIEW;
            case 4 -> DocumentStatus.QA_REVIEW;
            case 5 -> DocumentStatus.APPROVAL;
            case 6 -> DocumentStatus.PUBLISHED;
            default -> throw new WorkflowStateException("Invalid workflow step: " + step);
        };
    }
}
