package com.pharmaCx.dms.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class QuizAttempt {

    private int attemptNumber;
    private List<QuizAnswer> answers;
    private int score;
    private int totalQuestions;
    private boolean passed;
    private Instant attemptedAt;

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public List<QuizAnswer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<QuizAnswer> answers) {
        this.answers = answers;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(Instant attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    @Override
    public String toString() {
        return "QuizAttempt{" +
                "attemptNumber=" + attemptNumber +
                ", answers=" + answers +
                ", score=" + score +
                ", totalQuestions=" + totalQuestions +
                ", passed=" + passed +
                ", attemptedAt=" + attemptedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuizAttempt that = (QuizAttempt) o;
        return attemptNumber == that.attemptNumber &&
                score == that.score &&
                totalQuestions == that.totalQuestions &&
                passed == that.passed &&
                Objects.equals(answers, that.answers) &&
                Objects.equals(attemptedAt, that.attemptedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attemptNumber, answers, score, totalQuestions, passed, attemptedAt);
    }
}
