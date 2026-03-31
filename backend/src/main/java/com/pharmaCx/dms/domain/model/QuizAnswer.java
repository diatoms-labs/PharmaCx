package com.pharmaCx.dms.domain.model;

import java.util.Objects;

public class QuizAnswer {

    private String questionId;
    private int selectedAnswerIndex;

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public int getSelectedAnswerIndex() {
        return selectedAnswerIndex;
    }

    public void setSelectedAnswerIndex(int selectedAnswerIndex) {
        this.selectedAnswerIndex = selectedAnswerIndex;
    }

    @Override
    public String toString() {
        return "QuizAnswer{" +
                "questionId='" + questionId + '\'' +
                ", selectedAnswerIndex=" + selectedAnswerIndex +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuizAnswer that = (QuizAnswer) o;
        return selectedAnswerIndex == that.selectedAnswerIndex &&
                Objects.equals(questionId, that.questionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(questionId, selectedAnswerIndex);
    }
}
