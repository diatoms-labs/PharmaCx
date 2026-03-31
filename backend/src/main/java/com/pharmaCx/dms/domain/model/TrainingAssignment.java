package com.pharmaCx.dms.domain.model;

import com.pharmaCx.dms.domain.enums.TrainingStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Document(collection = "training_assignments")
public class TrainingAssignment {

    @Id
    private String id;

    @Indexed
    private String documentId;
    private String documentTitle;
    private String documentNumber;

    // References document_type_configs._id (replaces DocumentType enum)
    private String documentTypeId;
    private int documentVersion;

    @Indexed
    private String traineeUserId;
    private String traineeUsername;

    // References organizational_units._id (replaces Department enum)
    private String unitId;

    private String assignedByUserId;
    private String assignedByUsername;

    @Indexed
    private TrainingStatus status = TrainingStatus.ASSIGNED;

    private Instant assignedAt;
    private Instant dueDate;
    private Instant failedAt;

    private Instant readAt;
    private long readDurationSeconds;

    private List<QuizAttempt> quizAttempts = new ArrayList<>();
    private Instant quizPassedAt;

    private Instant acknowledgedAt;
    private String signatureData;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    public String getDocumentTypeId() { return documentTypeId; }
    public void setDocumentTypeId(String documentTypeId) { this.documentTypeId = documentTypeId; }

    public int getDocumentVersion() { return documentVersion; }
    public void setDocumentVersion(int documentVersion) { this.documentVersion = documentVersion; }

    public String getTraineeUserId() { return traineeUserId; }
    public void setTraineeUserId(String traineeUserId) { this.traineeUserId = traineeUserId; }

    public String getTraineeUsername() { return traineeUsername; }
    public void setTraineeUsername(String traineeUsername) { this.traineeUsername = traineeUsername; }

    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }

    public String getAssignedByUserId() { return assignedByUserId; }
    public void setAssignedByUserId(String assignedByUserId) { this.assignedByUserId = assignedByUserId; }

    public String getAssignedByUsername() { return assignedByUsername; }
    public void setAssignedByUsername(String assignedByUsername) { this.assignedByUsername = assignedByUsername; }

    public Instant getFailedAt() { return failedAt; }
    public void setFailedAt(Instant failedAt) { this.failedAt = failedAt; }

    public TrainingStatus getStatus() { return status; }
    public void setStatus(TrainingStatus status) { this.status = status; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }

    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant dueDate) { this.dueDate = dueDate; }

    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }

    public long getReadDurationSeconds() { return readDurationSeconds; }
    public void setReadDurationSeconds(long readDurationSeconds) { this.readDurationSeconds = readDurationSeconds; }

    public List<QuizAttempt> getQuizAttempts() { return quizAttempts; }
    public void setQuizAttempts(List<QuizAttempt> quizAttempts) { this.quizAttempts = quizAttempts; }

    public Instant getQuizPassedAt() { return quizPassedAt; }
    public void setQuizPassedAt(Instant quizPassedAt) { this.quizPassedAt = quizPassedAt; }

    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    public String getSignatureData() { return signatureData; }
    public void setSignatureData(String signatureData) { this.signatureData = signatureData; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrainingAssignment that = (TrainingAssignment) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(documentId, that.documentId) &&
                Objects.equals(traineeUserId, that.traineeUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, documentId, traineeUserId);
    }

    @Override
    public String toString() {
        return "TrainingAssignment{id='" + id + "', documentId='" + documentId +
                "', traineeUserId='" + traineeUserId + "', status=" + status + "}";
    }
}
