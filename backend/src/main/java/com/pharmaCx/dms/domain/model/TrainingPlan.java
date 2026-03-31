package com.pharmaCx.dms.domain.model;

import com.pharmaCx.dms.domain.enums.UserRole;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Document(collection = "training_plans")
public class TrainingPlan {

    @Id
    private String id;

    @Indexed
    private String documentId;

    private List<String> targetUnitIds;
    private List<UserRole> targetRoles;
    private boolean autoAssign = true;
    private int passingScorePercent = 80;
    private int maxQuizAttempts = 3;
    private int dueDays = 14;
    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public List<String> getTargetUnitIds() {
        return targetUnitIds;
    }

    public void setTargetUnitIds(List<String> targetUnitIds) {
        this.targetUnitIds = targetUnitIds;
    }

    public List<UserRole> getTargetRoles() {
        return targetRoles;
    }

    public void setTargetRoles(List<UserRole> targetRoles) {
        this.targetRoles = targetRoles;
    }

    public boolean isAutoAssign() {
        return autoAssign;
    }

    public void setAutoAssign(boolean autoAssign) {
        this.autoAssign = autoAssign;
    }

    public int getPassingScorePercent() {
        return passingScorePercent;
    }

    public void setPassingScorePercent(int passingScorePercent) {
        this.passingScorePercent = passingScorePercent;
    }

    public int getMaxQuizAttempts() {
        return maxQuizAttempts;
    }

    public void setMaxQuizAttempts(int maxQuizAttempts) {
        this.maxQuizAttempts = maxQuizAttempts;
    }

    public int getDueDays() {
        return dueDays;
    }

    public void setDueDays(int dueDays) {
        this.dueDays = dueDays;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "TrainingPlan{" +
                "id='" + id + '\'' +
                ", documentId='" + documentId + '\'' +
                ", targetUnitIds=" + targetUnitIds +
                ", targetRoles=" + targetRoles +
                ", autoAssign=" + autoAssign +
                ", passingScorePercent=" + passingScorePercent +
                ", maxQuizAttempts=" + maxQuizAttempts +
                ", dueDays=" + dueDays +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrainingPlan that = (TrainingPlan) o;
        return autoAssign == that.autoAssign &&
                passingScorePercent == that.passingScorePercent &&
                maxQuizAttempts == that.maxQuizAttempts &&
                dueDays == that.dueDays &&
                Objects.equals(id, that.id) &&
                Objects.equals(documentId, that.documentId) &&
                Objects.equals(targetUnitIds, that.targetUnitIds) &&
                Objects.equals(targetRoles, that.targetRoles) &&
                Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, documentId, targetUnitIds, targetRoles, autoAssign,
                passingScorePercent, maxQuizAttempts, dueDays, createdAt);
    }
}
