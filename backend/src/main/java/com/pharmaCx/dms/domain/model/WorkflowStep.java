package com.pharmaCx.dms.domain.model;

import com.pharmaCx.dms.domain.enums.StepStatus;
import com.pharmaCx.dms.domain.enums.StepType;

import java.time.Instant;
import java.util.Objects;

public class WorkflowStep {

    private int stepIndex;
    private String name;
    private StepType type;

    // Assignment
    private String assignedToUserId;
    private String assignedToUsername;
    private String assignedByUserId;

    private StepStatus status = StepStatus.PENDING;
    private String comment;
    private String signatureData;
    private String rejectionReason;

    private Instant startedAt;
    private Instant completedAt;

    public int getStepIndex() {
        return stepIndex;
    }

    public void setStepIndex(int stepIndex) {
        this.stepIndex = stepIndex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StepType getType() {
        return type;
    }

    public void setType(StepType type) {
        this.type = type;
    }

    public String getAssignedToUserId() {
        return assignedToUserId;
    }

    public void setAssignedToUserId(String assignedToUserId) {
        this.assignedToUserId = assignedToUserId;
    }

    public String getAssignedToUsername() {
        return assignedToUsername;
    }

    public void setAssignedToUsername(String assignedToUsername) {
        this.assignedToUsername = assignedToUsername;
    }

    public String getAssignedByUserId() {
        return assignedByUserId;
    }

    public void setAssignedByUserId(String assignedByUserId) {
        this.assignedByUserId = assignedByUserId;
    }

    public StepStatus getStatus() {
        return status;
    }

    public void setStatus(StepStatus status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getSignatureData() {
        return signatureData;
    }

    public void setSignatureData(String signatureData) {
        this.signatureData = signatureData;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public String toString() {
        return "WorkflowStep{" +
                "stepIndex=" + stepIndex +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", assignedToUserId='" + assignedToUserId + '\'' +
                ", assignedToUsername='" + assignedToUsername + '\'' +
                ", assignedByUserId='" + assignedByUserId + '\'' +
                ", status=" + status +
                ", comment='" + comment + '\'' +
                ", signatureData='" + signatureData + '\'' +
                ", rejectionReason='" + rejectionReason + '\'' +
                ", startedAt=" + startedAt +
                ", completedAt=" + completedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkflowStep that = (WorkflowStep) o;
        return stepIndex == that.stepIndex &&
                Objects.equals(name, that.name) &&
                Objects.equals(type, that.type) &&
                Objects.equals(assignedToUserId, that.assignedToUserId) &&
                Objects.equals(assignedToUsername, that.assignedToUsername) &&
                Objects.equals(assignedByUserId, that.assignedByUserId) &&
                Objects.equals(status, that.status) &&
                Objects.equals(comment, that.comment) &&
                Objects.equals(signatureData, that.signatureData) &&
                Objects.equals(rejectionReason, that.rejectionReason) &&
                Objects.equals(startedAt, that.startedAt) &&
                Objects.equals(completedAt, that.completedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stepIndex, name, type, assignedToUserId, assignedToUsername,
                assignedByUserId, status, comment, signatureData, rejectionReason,
                startedAt, completedAt);
    }
}
