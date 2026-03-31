package com.pharmaCx.dms.domain.model;

import com.pharmaCx.dms.domain.enums.DocumentStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Document(collection = "controlled_documents")
@CompoundIndex(name = "ext_path_idx", def = "{'externalPath': 1}")
public class ControlledDocument {

    @Id
    private String id;
    private String documentNumber;
    private String title;
    private String documentTypeId;
    private String unitId;
    private String authorId;
    private DocumentStatus status;
    private int version;
    private String currentVersion;
    private String requestedBy;
    private LocalDate effectiveDate;
    private LocalDate nextReviewDate;
    private int currentStepIndex;
    
    private String documentFileId; 
    private String pdfFileId;      
    private String sourceFileId;   
    private String templateFileId;

    private Instant createdAt;
    private Instant updatedAt;
    private String lastModifiedBy;

    private List<String> readerIds = new ArrayList<>();
    private List<String> editorIds = new ArrayList<>();
    
    private List<WorkflowStep> workflowSteps = new ArrayList<>();
    private List<TrainingQuestion> trainingQuestions = new ArrayList<>();

    private String qaPreparerId;
    private String peerReviewerUserId;
    private String qaReviewerUserId;
    private String approverUserId;

    private String parentDocumentId;
    private String externalPath; 

    public ControlledDocument() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDocumentTypeId() { return documentTypeId; }
    public void setDocumentTypeId(String documentTypeId) { this.documentTypeId = documentTypeId; }

    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(String currentVersion) { this.currentVersion = currentVersion; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public LocalDate getNextReviewDate() { return nextReviewDate; }
    public void setNextReviewDate(LocalDate nextReviewDate) { this.nextReviewDate = nextReviewDate; }

    public int getCurrentStepIndex() { return currentStepIndex; }
    public void setCurrentStepIndex(int currentStepIndex) { this.currentStepIndex = currentStepIndex; }

    public String getDocumentFileId() { return documentFileId; }
    public void setDocumentFileId(String documentFileId) { this.documentFileId = documentFileId; }

    public String getPdfFileId() { return pdfFileId; }
    public void setPdfFileId(String pdfFileId) { this.pdfFileId = pdfFileId; }

    public String getSourceFileId() { return sourceFileId; }
    public void setSourceFileId(String sourceFileId) { this.sourceFileId = sourceFileId; }

    public String getTemplateFileId() { return templateFileId; }
    public void setTemplateFileId(String templateFileId) { this.templateFileId = templateFileId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getLastModifiedBy() { return lastModifiedBy; }
    public void setLastModifiedBy(String lastModifiedBy) { this.lastModifiedBy = lastModifiedBy; }

    public List<String> getReaderIds() { return readerIds; }
    public void setReaderIds(List<String> readerIds) { this.readerIds = readerIds; }

    public List<String> getEditorIds() { return editorIds; }
    public void setEditorIds(List<String> editorIds) { this.editorIds = editorIds; }

    public List<WorkflowStep> getWorkflowSteps() { return workflowSteps; }
    public void setWorkflowSteps(List<WorkflowStep> workflowSteps) { this.workflowSteps = workflowSteps; }

    public List<TrainingQuestion> getTrainingQuestions() { return trainingQuestions; }
    public void setTrainingQuestions(List<TrainingQuestion> trainingQuestions) { this.trainingQuestions = trainingQuestions; }

    public String getQaPreparerId() { return qaPreparerId; }
    public void setQaPreparerId(String qaPreparerId) { this.qaPreparerId = qaPreparerId; }

    public String getPeerReviewerUserId() { return peerReviewerUserId; }
    public void setPeerReviewerUserId(String peerReviewerUserId) { this.peerReviewerUserId = peerReviewerUserId; }

    public String getQaReviewerUserId() { return qaReviewerUserId; }
    public void setQaReviewerUserId(String qaReviewerUserId) { this.qaReviewerUserId = qaReviewerUserId; }

    public String getApproverUserId() { return approverUserId; }
    public void setApproverUserId(String approverUserId) { this.approverUserId = approverUserId; }

    public String getParentDocumentId() { return parentDocumentId; }
    public void setParentDocumentId(String parentDocumentId) { this.parentDocumentId = parentDocumentId; }

    public String getExternalPath() { return externalPath; }
    public void setExternalPath(String externalPath) { this.externalPath = externalPath; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ControlledDocument that = (ControlledDocument) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
