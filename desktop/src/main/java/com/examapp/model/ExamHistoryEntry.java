package com.examapp.model;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 考试历史记录数据模型
 */
public class ExamHistoryEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private long timestamp;
    private String subjectId;
    private String subjectName;
    private int totalQuestions;
    private int answeredQuestions;
    private int score;
    private int maxScore;
    private long durationInSeconds; // 单位：秒
    private List<QuestionRecord> questionRecords;
    private long lastModified;
    private String deviceSource; // 新增字段

    public ExamHistoryEntry() {
        this.questionRecords = new ArrayList<>();
        this.lastModified = System.currentTimeMillis();
        try {
            this.deviceSource = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            this.deviceSource = "Desktop"; // Fallback
        }
    }

    /**
     * 题目记录内部类
     */
    public static class QuestionRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String questionId;
        private String questionText;
        private String correctAnswer;
        private String userAnswer;
        private boolean isCorrect;

        public QuestionRecord() {
        }

        // Getters and Setters
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

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        public void setCorrectAnswer(String correctAnswer) {
            this.correctAnswer = correctAnswer;
        }

        public String getUserAnswer() {
            return userAnswer;
        }

        public void setUserAnswer(String userAnswer) {
            this.userAnswer = userAnswer;
        }

        public boolean isCorrect() {
            return isCorrect;
        }

        public void setCorrect(boolean correct) {
            isCorrect = correct;
        }

        @Override
        public String toString() {
            return "QuestionRecord{" +
                    "questionId='" + questionId + '\'' +
                    ", isCorrect=" + isCorrect +
                    '}';
        }
    }

    // Getters and Setters
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getAnsweredQuestions() {
        return answeredQuestions;
    }

    public void setAnsweredQuestions(int answeredQuestions) {
        this.answeredQuestions = answeredQuestions;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public List<QuestionRecord> getQuestionRecords() {
        return questionRecords;
    }

    public void setQuestionRecords(List<QuestionRecord> questionRecords) {
        this.questionRecords = questionRecords;
    }

    public long getDurationInSeconds() {
        return durationInSeconds;
    }

    public void setDurationInSeconds(long durationInSeconds) {
        this.durationInSeconds = durationInSeconds;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public String getDeviceSource() {
        return deviceSource;
    }

    public void setDeviceSource(String deviceSource) {
        this.deviceSource = deviceSource;
    }

    /**
     * 获取格式化的日期时间
     */
    public String getFormattedDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        return sdf.format(new Date(timestamp));
    }

    /**
     * 获取格式化的日期
     */
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        return sdf.format(new Date(timestamp));
    }

    /**
     * 获取格式化的时间
     */
    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
        return sdf.format(new Date(timestamp));
    }

    /**
     * 获取正确题目数量
     */
    public int getCorrectCount() {
        if (questionRecords == null) {
            return 0;
        }
        int count = 0;
        for (QuestionRecord record : questionRecords) {
            if (record.isCorrect()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取错误题目数量
     */
    public int getWrongCount() {
        return answeredQuestions - getCorrectCount();
    }

    /**
     * 获取正确率
     */
    public double getAccuracy() {
        if (answeredQuestions == 0) {
            return 0.0;
        }
        return (double) getCorrectCount() / answeredQuestions * 100;
    }

    /**
     * 获取得分率
     */
    public double getScoreRate() {
        if (maxScore == 0) {
            return 0.0;
        }
        return (double) score / maxScore * 100;
    }

    /**
     * 获取格式化的分数
     */
    public String getFormattedScore() {
        return String.format("%d / %d", score, maxScore);
    }

    /**
     * 获取格式化的答题情况
     */
    public String getFormattedAnswerStatus() {
        return String.format("已答 %d / %d 题", answeredQuestions, totalQuestions);
    }

    /**
     * 获取格式化的用时
     */
    public String getFormattedDuration() {
        if (durationInSeconds <= 0) {
            return "未知";
        }
        long minutes = durationInSeconds / 60;
        long seconds = durationInSeconds % 60;
        if (minutes > 0) {
            return String.format("%d分%d秒", minutes, seconds);
        } else {
            return String.format("%d秒", seconds);
        }
    }

    @Override
    public String toString() {
        return "ExamHistoryEntry{" +
                "timestamp=" + getFormattedDateTime() +
                ", subjectName='" + subjectName + '\'' +
                ", score=" + getFormattedScore() +
                ", accuracy=" + String.format("%.1f%%", getAccuracy()) +
                '}';
    }
}