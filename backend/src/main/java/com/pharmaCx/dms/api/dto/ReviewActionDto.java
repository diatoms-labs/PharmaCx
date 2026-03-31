package com.pharmaCx.dms.api.dto;

public class ReviewActionDto {

    private boolean approved;
    private String comment;
    private String nextAssigneeUserId;
    private String signatureData;
    private String rejectionReason;

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getNextAssigneeUserId() {
        return nextAssigneeUserId;
    }

    public void setNextAssigneeUserId(String nextAssigneeUserId) {
        this.nextAssigneeUserId = nextAssigneeUserId;
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
}
