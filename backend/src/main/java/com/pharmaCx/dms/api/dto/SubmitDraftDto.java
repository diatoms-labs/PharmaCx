package com.pharmaCx.dms.api.dto;

import com.pharmaCx.dms.domain.model.TrainingQuestion;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class SubmitDraftDto {

    @NotBlank
    private String peerReviewerUserId;

    @NotBlank
    private String qaReviewerUserId;

    @NotBlank
    private String approverUserId;

    private List<TrainingQuestion> trainingQuestions;

    public String getPeerReviewerUserId() {
        return peerReviewerUserId;
    }

    public void setPeerReviewerUserId(String peerReviewerUserId) {
        this.peerReviewerUserId = peerReviewerUserId;
    }

    public String getQaReviewerUserId() {
        return qaReviewerUserId;
    }

    public void setQaReviewerUserId(String qaReviewerUserId) {
        this.qaReviewerUserId = qaReviewerUserId;
    }

    public String getApproverUserId() {
        return approverUserId;
    }

    public void setApproverUserId(String approverUserId) {
        this.approverUserId = approverUserId;
    }

    public List<TrainingQuestion> getTrainingQuestions() {
        return trainingQuestions;
    }

    public void setTrainingQuestions(List<TrainingQuestion> trainingQuestions) {
        this.trainingQuestions = trainingQuestions;
    }
}
