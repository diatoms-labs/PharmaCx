package com.pharmaCx.dms.api.dto;

import java.util.List;

public class QuizSubmissionDto {

    private List<AnswerDto> answers;

    public List<AnswerDto> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AnswerDto> answers) {
        this.answers = answers;
    }

    public static class AnswerDto {

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
    }
}
