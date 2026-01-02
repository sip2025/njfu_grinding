package com.examapp.controller;

import com.examapp.Main;
import com.examapp.data.QuestionManager;
import com.examapp.model.Question;
import com.examapp.model.Subject;
import com.examapp.util.IconHelper;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import javafx.fxml.FXMLLoader;


import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import com.examapp.util.IconHelper;

public class SearchController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ScrollPane resultsScrollPane;
    @FXML private VBox resultsContainer;
    @FXML private VBox emptyPane;
    @FXML private Button backButton;
    @FXML private Button searchActionButton;

    private Subject subject;
    private List<Question> allQuestions;
    private PauseTransition debounceTimer;

    public void setSubject(Subject subject) {
        this.subject = subject;
        loadAllQuestions();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        debounceTimer = new PauseTransition(Duration.millis(350));
        debounceTimer.setOnFinished(event -> performSearch());

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            debounceTimer.playFromStart();
        });

        // 设置图标
        backButton.setGraphic(IconHelper.getBackIcon(16));
        backButton.setText("");
        searchActionButton.setGraphic(IconHelper.getSearchIcon(16));
        searchActionButton.setText("搜索");
    }

    private void loadAllQuestions() {
        allQuestions = (subject != null && subject.getQuestions() != null) ?
                new ArrayList<>(subject.getQuestions()) : new ArrayList<>();
    }

    @FXML
    private void handleSearch() {
        // 这个方法现在由fxml中的搜索按钮调用，或者可以手动调用
        debounceTimer.stop(); // 立即停止计时器并执行搜索
        performSearch();
    }

    private void performSearch() {
        String keyword = searchField.getText().trim();

        if (keyword.isEmpty()) {
            resultsScrollPane.setVisible(false);
            emptyPane.setVisible(true);
            return;
        }

        String lowerKeyword = keyword.toLowerCase();

        List<Question> results = allQuestions.stream()
                .filter(q -> questionContainsKeyword(q, lowerKeyword))
                .collect(Collectors.toList());

        displayResults(results, keyword);
    }

    private boolean questionContainsKeyword(Question q, String lowerKeyword) {
        if (q.getQuestionText().toLowerCase().contains(lowerKeyword)) return true;
        if (q.getExplanation() != null && q.getExplanation().toLowerCase().contains(lowerKeyword)) return true;
        if (q.getOptions() != null) {
            for (String option : q.getOptions()) {
                if (option.toLowerCase().contains(lowerKeyword)) return true;
            }
        }
        return false;
    }

    private void displayResults(List<Question> results, String keyword) {
        resultsContainer.getChildren().clear();

        if (results.isEmpty()) {
            resultsScrollPane.setVisible(false);
            emptyPane.setVisible(true);
            emptyPane.getChildren().clear();

            Label icon = new Label("❌");
            icon.setStyle("-fx-font-size: 48px;");

            Label message = new Label("未找到包含\"" + keyword + "\"的题目");
            message.setStyle("-fx-font-size: 16px; -fx-text-fill: gray;");

            emptyPane.getChildren().addAll(icon, message);
            return;
        }

        resultsScrollPane.setVisible(true);
        emptyPane.setVisible(false);

        Label countLabel = new Label("找到 " + results.size() + " 道相关题目");
        countLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2196F3;");
        resultsContainer.getChildren().add(countLabel);

        for (int i = 0; i < results.size(); i++) {
            Question q = results.get(i);
            VBox card = createResultCard(q, i + 1, keyword);
            resultsContainer.getChildren().add(card);
        }
    }

    private VBox createResultCard(Question question, int index, String keyword) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8px; " +
                     "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 5, 0, 1, 2);");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label numberLabel = new Label("#" + index);
        numberLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label typeLabel = new Label("[" + question.getType() + "]");
        typeLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 12px;");

        header.getChildren().addAll(numberLabel, typeLabel);

        TextFlow questionTextFlow = createHighlightedTextFlow(question.getQuestionText(), keyword);

        Label answerLabel = new Label("答案: " + question.getFormattedAnswer());
        answerLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4CAF50;");

        card.getChildren().addAll(header, new Separator(), questionTextFlow, answerLabel);

        if (question.getExplanation() != null && !question.getExplanation().isEmpty()) {
            TextFlow explanationFlow = createHighlightedTextFlow("解析: " + question.getExplanation(), keyword);
            explanationFlow.setStyle("-fx-font-size: 13px;");
            card.getChildren().add(explanationFlow);
        }
        
        card.setOnMouseClicked(event -> showDetailsDialog(question));

        return card;
    }

    private TextFlow createHighlightedTextFlow(String text, String keyword) {
        TextFlow textFlow = new TextFlow();
        if (text == null || text.isEmpty()) {
            return textFlow;
        }

        String lowerText = text.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();

        int lastIndex = 0;
        int index = lowerText.indexOf(lowerKeyword, lastIndex);

        while (index != -1) {
            // 添加关键词前的部分
            if (index > lastIndex) {
                textFlow.getChildren().add(new Text(text.substring(lastIndex, index)));
            }
            // 添加高亮的关键词
            Text highlightedText = new Text(text.substring(index, index + keyword.length()));
            highlightedText.setStyle("-fx-font-weight: bold; -fx-fill: #E91E63;"); // 使用醒目的粉红色
            textFlow.getChildren().add(highlightedText);

            lastIndex = index + keyword.length();
            index = lowerText.indexOf(lowerKeyword, lastIndex);
        }

        // 添加剩余的部分
        if (lastIndex < text.length()) {
            textFlow.getChildren().add(new Text(text.substring(lastIndex)));
        }

        return textFlow;
    }

    private void showDetailsDialog(Question question) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/question-details-dialog.fxml"));
            DialogPane dialogPane = loader.load();

            QuestionDetailsDialogController controller = loader.getController();
            controller.setQuestion(this.subject, question);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("题目详情");

            dialog.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            // Show error alert
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
}