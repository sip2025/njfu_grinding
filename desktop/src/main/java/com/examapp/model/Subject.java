package com.examapp.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 题库科目数据模型
 */
public class Subject implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String name;
    private String displayName;
    private List<Question> questions;
    private int lastPosition;
    private int sequentialLastPosition;
    private int reviewLastPosition;
    private int wrongReviewLastPosition;
    private int correctCount;
    private int attemptedCount;
    private int sortOrder;
    private int endlessBestStreak;
    private long lastModified;

    public Subject() {
        this.questions = new ArrayList<>();
        this.lastPosition = 0;
        this.sequentialLastPosition = 0;
        this.reviewLastPosition = 0;
        this.wrongReviewLastPosition = 0;
        this.correctCount = 0;
        this.attemptedCount = 0;
        this.sortOrder = 0;
        this.endlessBestStreak = 0;
        this.lastModified = System.currentTimeMillis();
    }

    public Subject(String id, String name) {
        this();
        this.id = id;
        this.name = name;
        this.displayName = name;
        this.lastModified = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public int getLastPosition() {
        return lastPosition;
    }

    public void setLastPosition(int lastPosition) {
        this.lastPosition = lastPosition;
    }

    public int getSequentialLastPosition() {
        return sequentialLastPosition;
    }

    public void setSequentialLastPosition(int sequentialLastPosition) {
        this.sequentialLastPosition = sequentialLastPosition;
    }

    public int getReviewLastPosition() {
        return reviewLastPosition;
    }

    public void setReviewLastPosition(int reviewLastPosition) {
        this.reviewLastPosition = reviewLastPosition;
    }

    public int getWrongReviewLastPosition() {
        return wrongReviewLastPosition;
    }

    public void setWrongReviewLastPosition(int wrongReviewLastPosition) {
        this.wrongReviewLastPosition = wrongReviewLastPosition;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }

    public int getAttemptedCount() {
        return attemptedCount;
    }

    public void setAttemptedCount(int attemptedCount) {
        this.attemptedCount = attemptedCount;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int getEndlessBestStreak() {
        return endlessBestStreak;
    }

    public void setEndlessBestStreak(int endlessBestStreak) {
        this.endlessBestStreak = endlessBestStreak;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * 获取题目总数
     */
    public int getTotalQuestions() {
        return questions != null ? questions.size() : 0;
    }

    /**
     * 获取正确率
     */
    public double getAccuracy() {
        if (attemptedCount == 0) {
            return 0.0;
        }
        return (double) correctCount / attemptedCount * 100;
    }

    /**
     * 获取错题数量
     */
    public int getWrongQuestionCount() {
        if (questions == null) {
            return 0;
        }
        int count = 0;
        for (Question q : questions) {
            if (q.isWrong()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取各类型题目数量
     */
    public int getQuestionCountByType(String type) {
        if (questions == null) {
            return 0;
        }
        int count = 0;
        for (Question q : questions) {
            if (type.equals(q.getType())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取单选题数量
     */
    public int getSingleChoiceCount() {
        return getQuestionCountByType("单选题");
    }

    /**
     * 获取多选题数量
     */
    public int getMultipleChoiceCount() {
        return getQuestionCountByType("多选题");
    }

    /**
     * 获取判断题数量
     */
    public int getTrueFalseCount() {
        return getQuestionCountByType("判断题");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subject subject = (Subject) o;
        return id != null ? id.equals(subject.id) : subject.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Subject{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", totalQuestions=" + getTotalQuestions() +
                ", wrongQuestions=" + getWrongQuestionCount() +
                '}';
    }

    /**
     * 根据题目列表重新计算统计数据，例如正确数、已答数等。
     */
    public void recalculateStats() {
        if (questions == null) {
            this.attemptedCount = 0;
            this.correctCount = 0;
            return;
        }

        int attempted = 0;
        int correct = 0;
        for (Question q : questions) {
            if (q.getAnswerState() != Question.AnswerState.UNANSWERED) {
                attempted++;
                if (q.getAnswerState() == Question.AnswerState.CORRECT) {
                    correct++;
                }
            }
        }
        this.setAttemptedCount(attempted);
        this.setCorrectCount(correct);
    }
}