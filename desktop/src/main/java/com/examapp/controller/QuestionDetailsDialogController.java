package com.examapp.controller;

import com.examapp.model.Question;
import com.examapp.model.Subject;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.List;

public class QuestionDetailsDialogController {

    @FXML private Label subjectNameLabel;
    @FXML private Label questionTypeLabel;
    @FXML private Label questionTextLabel;
    @FXML private VBox optionsContainer;
    @FXML private Label answerLabel;
    @FXML private VBox explanationPane;
    @FXML private Label explanationLabel;

    private Question question;
    private Subject subject;

    public void setQuestion(Subject subject, Question question) {
        this.subject = subject;
        this.question = question;
        displayQuestionDetails();
    }

    private void displayQuestionDetails() {
        if (question == null) return;

        subjectNameLabel.setText(subject != null ? subject.getDisplayName() : "未知题库");
        questionTypeLabel.setText(String.format("[%s]", question.getType()));
        questionTextLabel.setText(question.getQuestionText());
        answerLabel.setText(question.getFormattedAnswer());

        renderOptions();

        if (question.getExplanation() != null && !question.getExplanation().trim().isEmpty()) {
            explanationLabel.setText(question.getExplanation());
            explanationPane.setManaged(true);
            explanationPane.setVisible(true);
        }
    }

    private void renderOptions() {
        optionsContainer.getChildren().clear();
        String correctAnswer = question.getAnswer();

        List<String> options = question.getOptions();
        if (options == null || options.isEmpty()) {
            options = Arrays.asList("A. 正确", "B. 错误");
        }

        for (int i = 0; i < options.size(); i++) {
            String optionText = options.get(i);
            String optionLetter = String.valueOf((char) ('A' + i));

            Label optionLabel = new Label(optionText);
            optionLabel.setWrapText(true);
            optionLabel.getStyleClass().add("option-label");

            if (correctAnswer.contains(optionLetter)) {
                optionLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4CAF50;");
            }

            optionsContainer.getChildren().add(optionLabel);
        }
    }
}