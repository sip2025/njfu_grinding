package com.examapp.controller;

import com.examapp.Main;
import com.examapp.data.QuestionManager;
import com.examapp.model.Question;
import com.examapp.model.Subject;
import com.examapp.util.IconHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;

import java.io.IOException;
import java.util.List;

/**
 * 错题分析控制器 - 按错误次数排序显示错题
 */
public class WrongAnalysisController {
    @FXML private Label titleLabel;
    @FXML private Button backButton;
    @FXML private ListView<Question> wrongQuestionsListView;
    
    private Subject subject;
    private QuestionManager questionManager;
    
    @FXML
    private void initialize() {
        questionManager = QuestionManager.getInstance();
        setupListView();
        setupIcons();
    }
    
    public void setSubject(Subject subject) {
        this.subject = subject;
        titleLabel.setText(subject.getDisplayName() + " - 错题分析");
        loadWrongQuestions();
    }
    
    private void setupIcons() {
        if (backButton != null) {
            backButton.setGraphic(IconHelper.getBackIcon(16));
        }
    }

    @FXML
    private void handleBack() {
        try {
            if (subject != null) {
                Main.switchSceneWithData("/fxml/study-mode.fxml", subject.getDisplayName(), controller -> {
                    if (controller instanceof StudyModeController) {
                        ((StudyModeController) controller).setSubject(subject);
                    }
                });
            } else {
                Main.switchScene("/fxml/main.fxml", "");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void loadWrongQuestions() {
        List<Question> wrongQuestions = questionManager.getQuestionsSortedByWrongCount(subject.getId());
        wrongQuestionsListView.getItems().setAll(wrongQuestions);
        
        if (wrongQuestions.isEmpty()) {
            wrongQuestionsListView.setPlaceholder(new Label("恭喜！该题库暂无错题记录。"));
        }
    }
    
    private void setupListView() {
        wrongQuestionsListView.setCellFactory(new Callback<ListView<Question>, ListCell<Question>>() {
            @Override
            public ListCell<Question> call(ListView<Question> listView) {
                return new WrongQuestionCell();
            }
        });

        // Change from double-click to single-click for consistency
        wrongQuestionsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) { // Changed from 2 to 1
                Question selected = wrongQuestionsListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showQuestionDetails(selected);
                }
            }
        });
    }
    
    private void showQuestionDetails(Question question) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/question-details-dialog.fxml"));
            DialogPane dialogPane = loader.load();

            QuestionDetailsDialogController controller = loader.getController();
            controller.setQuestion(subject, question);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("题目详情");
            
            dialog.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showSimpleQuestionDetails(question);
        }
    }

    private void showSimpleQuestionDetails(Question question) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("题目详情");
        dialog.setHeaderText(String.format("[%s] 错误次数: %d", question.getType(), question.getWrongAnswerCount()));

        VBox dialogContent = new VBox(10);
        dialogContent.setPadding(new Insets(10));

        TextArea questionArea = new TextArea("题目: " + question.getQuestionText());
        questionArea.setWrapText(true);
        questionArea.setEditable(false);
        questionArea.setPrefHeight(100);

        StringBuilder optionsText = new StringBuilder("选项:\n");
        if (question.getOptions() != null && !question.getOptions().isEmpty()) {
            for (String option : question.getOptions()) {
                optionsText.append(option).append("\n");
            }
        } else {
            optionsText.append("无\n");
        }
        
        TextArea optionsArea = new TextArea(optionsText.toString());
        optionsArea.setWrapText(true);
        optionsArea.setEditable(false);
        optionsArea.setPrefHeight(120);

        TextFlow answerFlow = new TextFlow(
            new Text("正确答案: "),
            new Text(question.getFormattedAnswer()) {{ setStyle("-fx-font-weight: bold;"); }}
        );

        dialogContent.getChildren().addAll(questionArea, optionsArea, answerFlow);
        
        if(question.getExplanation() != null && !question.getExplanation().isEmpty()){
             TextArea explanationArea = new TextArea("解析:\n" + question.getExplanation());
             explanationArea.setWrapText(true);
             explanationArea.setEditable(false);
             explanationArea.setPrefHeight(150);
             dialogContent.getChildren().add(explanationArea);
        }

        dialog.getDialogPane().setContent(dialogContent);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    // Custom ListCell to use the new card style
    static class WrongQuestionCell extends ListCell<Question> {
        private VBox cell;
        private Label typeLabel;
        private Label idLabel;
        private Label wrongCountLabel;
        private Label questionLabel;
        private Label answerLabel;
        
        public WrongQuestionCell() {
            super();
            // Create the layout once
            cell = new VBox(15);
            cell.getStyleClass().add("wrong-question-card");

            // Header
            HBox headerBox = new HBox(10);
            headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            headerBox.getStyleClass().add("card-header");
            
            typeLabel = new Label();
            typeLabel.getStyleClass().add("type-tag");
            
            idLabel = new Label();
            idLabel.getStyleClass().add("id-tag");
            
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            wrongCountLabel = new Label();
            wrongCountLabel.setStyle("-fx-font-size: 1.1em; -fx-font-weight: bold; -fx-text-fill: #D32F2F;"); // Red for emphasis

            headerBox.getChildren().addAll(typeLabel, idLabel, spacer, wrongCountLabel);

            // Question Text
            questionLabel = new Label();
            questionLabel.setWrapText(true);
            questionLabel.getStyleClass().add("question-text");
            
            // Answer Text
            answerLabel = new Label();
            answerLabel.setWrapText(true);
            answerLabel.getStyleClass().add("answer-text");

            cell.getChildren().addAll(headerBox, questionLabel, answerLabel);
        }

        @Override
        protected void updateItem(Question item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                typeLabel.setText(item.getType());
                idLabel.setText("第 " + item.getRelativeId() + " 题");
                wrongCountLabel.setText("错误 " + item.getWrongAnswerCount() + " 次");
                questionLabel.setText(item.getQuestionText());
                // Bind wrapping width for this label as well
                questionLabel.maxWidthProperty().bind(getListView().widthProperty().subtract(40));
                answerLabel.setText("正确答案: " + item.getFormattedAnswer());
                setGraphic(cell);
                setPadding(Insets.EMPTY);
            }
        }
    }
}