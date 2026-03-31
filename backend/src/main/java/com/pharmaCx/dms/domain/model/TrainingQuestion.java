package com.pharmaCx.dms.domain.model;

import com.pharmaCx.dms.domain.enums.QuestionType;

import java.util.List;
import java.util.Objects;

public class TrainingQuestion {

    private String questionId;
    private String questionText;
    private QuestionType questionType;
    private List<String> options;
    private int correctAnswerIndex;
    private String explanation;

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }

    public void setCorrectAnswerIndex(int correctAnswerIndex) {
        this.correctAnswerIndex = correctAnswerIndex;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    @Override
    public String toString() {
        return "TrainingQuestion{" +
                "questionId='" + questionId + '\'' +
                ", questionText='" + questionText + '\'' +
                ", questionType=" + questionType +
                ", options=" + options +
                ", correctAnswerIndex=" + correctAnswerIndex +
                ", explanation='" + explanation + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrainingQuestion that = (TrainingQuestion) o;
        return correctAnswerIndex == that.correctAnswerIndex &&
                Objects.equals(questionId, that.questionId) &&
                Objects.equals(questionText, that.questionText) &&
                Objects.equals(questionType, that.questionType) &&
                Objects.equals(options, that.options) &&
                Objects.equals(explanation, that.explanation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(questionId, questionText, questionType, options, correctAnswerIndex, explanation);
    }
}
