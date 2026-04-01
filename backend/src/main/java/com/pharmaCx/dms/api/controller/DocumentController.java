package com.pharmaCx.dms.api.controller;

import com.pharmaCx.dms.api.dto.*;
import com.pharmaCx.dms.domain.enums.DocumentStatus;
import com.pharmaCx.dms.domain.model.AppUser;
import com.pharmaCx.dms.domain.model.ControlledDocument;
import com.pharmaCx.dms.service.DocumentWorkflowService;
import com.pharmaCx.dms.service.OnlyOfficeService;
import com.pharmaCx.dms.service.AuthService;
import com.pharmaCx.dms.service.DocumentNumberService;
import com.pharmaCx.dms.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentWorkflowService workflowService;
    private final OnlyOfficeService onlyOfficeService;
    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final DocumentNumberService documentNumberService;

    public DocumentController(DocumentWorkflowService workflowService,
                              OnlyOfficeService onlyOfficeService, AuthService authService,
                              CurrentUserService currentUserService, DocumentNumberService documentNumberService) {
        this.workflowService = workflowService;
        this.onlyOfficeService = onlyOfficeService;
        this.authService = authService;
        this.currentUserService = currentUserService;
        this.documentNumberService = documentNumberService;
    }

    @PostMapping
    public ResponseEntity<ControlledDocument> submitRequest(@Valid @RequestBody DocumentRequestDto dto) {
        return ResponseEntity.ok(workflowService.submitRequest(
                dto.getTitle(), dto.getDocumentTypeId(), dto.getUnitId(), dto.getJustification()));
    }

    @PostMapping("/{id}/prepare")
    public ResponseEntity<ControlledDocument> prepare(@PathVariable String id, @Valid @RequestBody QAPreparationDto dto) {
        return ResponseEntity.ok(workflowService.prepareDocument(id, dto.getDocumentNumber(), dto.getTemplateId()));
    }

    @PostMapping("/{id}/submit-draft")
    public ResponseEntity<ControlledDocument> submitDraft(@PathVariable String id, @Valid @RequestBody SubmitDraftDto dto) {
        return ResponseEntity.ok(workflowService.submitDraft(
                id, dto.getPeerReviewerUserId(), dto.getQaReviewerUserId(), dto.getApproverUserId(),
                dto.getTrainingQuestions()));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<ControlledDocument> review(@PathVariable String id, @RequestBody ReviewActionDto dto) {
        return ResponseEntity.ok(workflowService.submitReview(
                id, dto.isApproved(), dto.getComment(), dto.getRejectionReason()));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ControlledDocument> approve(@PathVariable String id, @RequestBody ReviewActionDto dto) {
        return ResponseEntity.ok(workflowService.submitApproval(
                id, dto.isApproved(), dto.getSignatureData(), dto.getComment(), dto.getRejectionReason()));
    }

    @PostMapping("/{id}/retire")
    public ResponseEntity<ControlledDocument> retire(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.retireDocument(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDetailResponse> getDocumentDetail(@PathVariable String id) {
        ControlledDocument doc = workflowService.getDocument(id);

        EditorConfigResponse editorConfig = null;
        Map<String, Boolean> features = null;

        if (doc.getDocumentFileId() != null || doc.getExternalPath() != null) {
            String userId = currentUserService.getCurrentUserId();
            AppUser user = authService.findUserById(userId).orElse(null);
            if (user == null) {
                throw new com.pharmaCx.dms.exception.AccessDeniedException("Valid user record not found. Please log in again.");
            }
            boolean canEdit = isUserAssignedToCurrentStep(doc, userId);
            boolean isRevision = OnlyOfficeService.isRevisionDraft(doc);

            System.out.println("DEBUG: Editor Request - docId=" + id + " userId=" + userId + " canEdit=" + canEdit + " status=" + doc.getStatus());

            Map<String, Object> rawConfig = onlyOfficeService.generateEditorConfig(doc, user, canEdit, isRevision);

            editorConfig = new EditorConfigResponse(
                    (String) rawConfig.get("documentServerUrl"),
                    (Map<String, Object>) rawConfig.get("config"),
                    (String) rawConfig.get("mode"));

            features = (Map<String, Boolean>) rawConfig.get("features");
        }

        return ResponseEntity.ok(new DocumentDetailResponse(doc, editorConfig, features));
    }

    @GetMapping
    public ResponseEntity<Page<ControlledDocument>> listDocuments(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String documentTypeId,
            @RequestParam(required = false) String unitId,
            Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(workflowService.getByStatus(status, pageable));
        }
        if (documentTypeId != null && unitId != null) {
            return ResponseEntity.ok(workflowService.getByTypeAndUnit(documentTypeId, unitId, pageable));
        }
        return ResponseEntity.ok(workflowService.getByStatus(DocumentStatus.PUBLISHED, pageable));
    }

    @GetMapping("/my-tasks")
    public ResponseEntity<List<ControlledDocument>> getMyTasks() {
        return ResponseEntity.ok(workflowService.getMyTasks());
    }

    @GetMapping("/my-documents")
    public ResponseEntity<List<ControlledDocument>> getMyDocuments() {
        return ResponseEntity.ok(workflowService.getMyDocuments());
    }

    @GetMapping("/pending-requests")
    public ResponseEntity<Page<ControlledDocument>> getPendingRequests(Pageable pageable) {
        return ResponseEntity.ok(workflowService.getPendingRequests(pageable));
    }

    @GetMapping("/{id}/eligible-reviewers")
    public ResponseEntity<List<AppUser>> getEligibleReviewers(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.getEligibleReviewers(id));
    }

    @GetMapping("/eligible-approvers")
    public ResponseEntity<List<AppUser>> getEligibleApprovers() {
        return ResponseEntity.ok(workflowService.getEligibleApprovers());
    }

    @GetMapping("/preview-number")
    public ResponseEntity<Map<String, String>> previewDocumentNumber(
            @RequestParam String documentTypeId, @RequestParam String unitId) {
        String preview = documentNumberService.previewNextNumber(documentTypeId, unitId);
        return ResponseEntity.ok(Map.of("documentNumber", preview));
    }

    private boolean isUserAssignedToCurrentStep(ControlledDocument doc, String userId) {
        if (userId == null) return false;
        
        // Final fallback: If it's Author Draft stage and you are the author, you MUST have edit.
        if (doc.getStatus() == DocumentStatus.AUTHOR_DRAFT && userId.equals(doc.getAuthorId())) {
            return true;
        }

        int stepIdx = doc.getCurrentStepIndex();
        if (stepIdx >= 0 && stepIdx < doc.getWorkflowSteps().size()) {
            var currentWs = doc.getWorkflowSteps().get(stepIdx);
            if (userId.equals(currentWs.getAssignedToUserId())) return true;
        }
        
        // Also check if user is in explicit editor list (for multi-author future proofing)
        if (doc.getEditorIds() != null && doc.getEditorIds().contains(userId)) {
            return true;
        }

        return false;
    }
}
