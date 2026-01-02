package com.examapp.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 题目数据模型
 */
public class Question implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String questionText;
    private List<String> options;
    private String answer;
    private String explanation;
    private String category;
    private boolean isWrong;
    private String userAnswer;
    private int index;
    private String type;
    private int relativeId;
    private int wrongAnswerCount;
    private AnswerState answerState = AnswerState.UNANSWERED;
    private transient String examDisplayId; // For mock exam display, not persisted

    /**
     * 答题状态枚举
     */
    public enum AnswerState {
        UNANSWERED,  // 未答题
        ANSWERED,    // 已答题（模拟考试）
        CORRECT,     // 答对
        WRONG        // 答错
    }

    public Question() {
    }

    /**
     * 拷贝构造函数
     */
    public Question(Question other) {
        this.id = other.id;
        this.questionText = other.questionText;
        this.options = other.options != null ? new ArrayList<>(other.options) : null;
        this.answer = other.answer;
        this.explanation = other.explanation;
        this.category = other.category;
        this.isWrong = other.isWrong;
        this.userAnswer = other.userAnswer;
        this.index = other.index;
        this.type = other.type;
        this.relativeId = other.relativeId;
        this.wrongAnswerCount = other.wrongAnswerCount;
        this.answerState = other.answerState;
        this.examDisplayId = other.examDisplayId; // Copy transient state too
    }

    public Question(String questionText, List<String> options, String answer, String category) {
        this.questionText = questionText;
        this.options = options;
        this.answer = answer;
        this.category = category;
        this.isWrong = false;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getAnswer() {
        return answer;
    }

    /**
     * 获取格式化的答案（多选题用逗号分隔）
     */
    public String getFormattedAnswer() {
        if (answer == null || answer.isEmpty()) {
            return "";
        }
        if ("多选题".equals(type) && answer.length() > 1) {
            StringBuilder formatted = new StringBuilder();
            for (char c : answer.toCharArray()) {
                formatted.append(c).append(", ");
            }
            return formatted.substring(0, formatted.length() - 2);
        }
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isWrong() {
        return isWrong;
    }

    public void setWrong(boolean wrong) {
        isWrong = wrong;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getRelativeId() {
        return relativeId;
    }

    public void setRelativeId(int relativeId) {
        this.relativeId = relativeId;
    }

    public AnswerState getAnswerState() {
        return answerState;
    }

    public void setAnswerState(AnswerState answerState) {
        this.answerState = answerState;
    }

    public int getWrongAnswerCount() {
        return wrongAnswerCount;
    }

    public void setWrongAnswerCount(int wrongAnswerCount) {
        this.wrongAnswerCount = wrongAnswerCount;
    }

    public void incrementWrongAnswerCount() {
        this.wrongAnswerCount++;
    }

    public String getExamDisplayId() {
        return examDisplayId;
    }

    public void setExamDisplayId(String examDisplayId) {
        this.examDisplayId = examDisplayId;
    }

    /**
     * 字符串排序（用于多选题答案比较）
     */
    private String sortString(String str) {
        if (str == null) return null;
        char[] chars = str.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }

    /**
     * 判断用户答案是否正确
     */
    public boolean isAnsweredCorrectly() {
        if (answer == null || userAnswer == null) {
            return false;
        }

        // 多选题：排序后比较
        if ("多选题".equals(type)) {
            return sortString(answer).equals(sortString(userAnswer));
        }

        // 判断题：转换A/B为正确/错误
        if ("判断题".equals(type)) {
            String convertedAnswer = userAnswer;
            if ("A".equals(userAnswer)) {
                convertedAnswer = "正确";
            } else if ("B".equals(userAnswer)) {
                convertedAnswer = "错误";
            }
            return answer.equals(convertedAnswer);
        }

        // 单选题：直接比较
        return answer.equals(userAnswer);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Question question = (Question) o;
        return id != null ? id.equals(question.id) : question.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Question{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", questionText='" + questionText + '\'' +
                ", answer='" + answer + '\'' +
                '}';
    }
}