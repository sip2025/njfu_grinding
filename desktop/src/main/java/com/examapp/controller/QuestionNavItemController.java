package com.examapp.controller;

import com.examapp.model.Question;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import java.util.function.Consumer;

public class QuestionNavItemController {

    @FXML
    private StackPane itemPane;

    @FXML
    private Label questionNumberLabel;

    private Question question;
    private Consumer<Question> onClick;
    private Node rootNode;

    public void setData(Question question, boolean isCurrent, boolean isReviewMode, Consumer<Question> onClick, Node rootNode) {
        this.question = question;
        this.onClick = onClick;
        this.rootNode = rootNode;

        // 模拟考试模式：显示子题库题号（如"1"、"2"等，不带前缀）
        // 其他模式：显示完整ID或相对ID
        if (question.getExamDisplayId() != null && !question.getExamDisplayId().isEmpty()) {
            // examDisplayId格式为"单选题1"、"多选题2"等，提取数字部分
            String displayId = question.getExamDisplayId();
            String numberOnly = displayId.replaceAll("[^0-9]", "");
            questionNumberLabel.setText(numberOnly.isEmpty() ? displayId : numberOnly);
        } else {
            questionNumberLabel.setText(String.valueOf(question.getRelativeId()));
        }

        updateStyle(isCurrent, isReviewMode);

        itemPane.setOnMouseClicked(event -> {
            if (this.onClick != null) {
                this.onClick.accept(this.question);
            }
        });
    }

    public Node getRootNode() {
        return rootNode;
    }

    public void updateStyle(boolean isCurrent, boolean isReviewMode) {
        // 移除所有可能存在的状态样式，重新计算
        itemPane.getStyleClass().removeAll(
                "nav-item-current", "correct", "wrong", "unanswered", "answered",
                "wrong-count-low", "wrong-count-medium", "wrong-count-high"
        );

        // 如果是当前题目，应用蓝色高亮，这个样式优先级最高
        if (isCurrent) {
            itemPane.getStyleClass().add("nav-item-current");
            return; // 直接返回，不再应用其他颜色
        }

        if (isReviewMode) {
            // 背题/错题回顾模式: 根据错题次数显示热力图 (逻辑与安卓端对齐)
            int wrongCount = question.getWrongAnswerCount();
            if (wrongCount >= 5) {
                itemPane.getStyleClass().add("wrong-count-high");
            } else if (wrongCount >= 3) {
                itemPane.getStyleClass().add("wrong-count-medium");
            } else if (wrongCount > 0) {
                itemPane.getStyleClass().add("wrong-count-low");
            } else {
                itemPane.getStyleClass().add("unanswered");
            }
        } else {
            // 普通练习/模拟考试模式: 根据本次答题状态显示
            // 模拟考试中不显示对错，只显示已答/未答
            switch (question.getAnswerState()) {
                case CORRECT:
                    itemPane.getStyleClass().add("correct");
                    break;
                case WRONG:
                    itemPane.getStyleClass().add("wrong");
                    break;
                case ANSWERED: // 模拟考试已作答状态，显示浅蓝色
                    itemPane.getStyleClass().add("answered");
                    break;
                case UNANSWERED:
                default:
                    itemPane.getStyleClass().add("unanswered");
                    break;
            }
        }
    }
}