package com.examapp.controller;

import com.examapp.Main;
import com.examapp.data.QuestionManager;
import com.examapp.model.Question;
import com.examapp.model.Subject;
import com.examapp.util.IconHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class WrongQuestionsController {

    @FXML private Label titleLabel;
    @FXML private Button backButton;
    @FXML private Button clearAllButton;
    @FXML private Label countLabel;
    @FXML private ListView<Question> wrongQuestionsListView;

    private Subject subject;
    private QuestionManager questionManager;

    @FXML
    private void initialize() {
        questionManager = QuestionManager.getInstance();
        setupIcons();
        setupListView();
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
        titleLabel.setText(subject.getDisplayName() + " - 错题本");
        loadWrongQuestions();
    }

    private void setupIcons() {
        if (backButton != null) {
            backButton.setGraphic(IconHelper.getBackIcon(16));
        }
    }

    private void setupListView() {
        wrongQuestionsListView.setCellFactory(lv -> {
            ListCell<Question> cell = new ListCell<>() {
                @Override
                protected void updateItem(Question question, boolean empty) {
                    super.updateItem(question, empty);
                    if (empty || question == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        // The graphic is created and managed by createQuestionCell
                        setGraphic(createQuestionCell(question, wrongQuestionsListView.widthProperty().subtract(40)));
                        // Remove default padding
                        setPadding(Insets.EMPTY);
                    }
                }
            };
            // Add click listener to the entire cell
            cell.setOnMouseClicked(event -> {
                if (!cell.isEmpty()) {
                    showQuestionDetails(cell.getItem());
                }
            });
            return cell;
        });
    }

    private VBox createQuestionCell(Question question, javafx.beans.value.ObservableValue<? extends Number> cellWidth) {
        VBox cellContainer = new VBox(15);
        cellContainer.getStyleClass().add("wrong-question-card");

        // Header
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.getStyleClass().add("card-header");
        
        Label typeLabel = new Label(question.getType().toString());
        typeLabel.getStyleClass().add("type-tag");
        
        Label idLabel = new Label("第 " + question.getRelativeId() + " 题");
        idLabel.getStyleClass().add("id-tag");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(typeLabel, idLabel, spacer);

        // Question Text
        Label questionLabel = new Label(question.getQuestionText());
        questionLabel.setWrapText(true);
        questionLabel.getStyleClass().add("question-text");
        // Bind the wrapping width to the cell width to ensure proper wrapping
        questionLabel.maxWidthProperty().bind(cellWidth);
        
        // Answer Text
        Label answerLabel = new Label("正确答案: " + question.getAnswer());
        answerLabel.setWrapText(true);
        answerLabel.getStyleClass().add("answer-text");

        // Action Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        
        // The entire cell is now clickable, so we can make the button less prominent
        // or just rely on the cell click. For clarity, let's keep a visual cue.
        Button viewButton = new Button("查看详情");
        viewButton.getStyleClass().addAll("action-button", "primary-button");
        viewButton.setMouseTransparent(true); // Makes the button non-interactive, click goes to cell

        Button deleteButton = new Button("移出错题本");
        deleteButton.getStyleClass().addAll("action-button", "danger-button");
        deleteButton.setOnAction(e -> {
            deleteQuestion(question);
            e.consume(); // Prevent the cell's click event from firing
        });

        buttonBox.getChildren().addAll(viewButton, deleteButton);

        cellContainer.getChildren().addAll(headerBox, questionLabel, answerLabel, buttonBox);
        return cellContainer;
    }

    private void loadWrongQuestions() {
        List<Question> wrongQuestions = questionManager.getWrongQuestions(subject.getId());
        wrongQuestionsListView.getItems().setAll(wrongQuestions);
        countLabel.setText("共 " + wrongQuestions.size() + " 道错题");
        clearAllButton.setDisable(wrongQuestions.isEmpty());
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
        }
    }

    private void deleteQuestion(Question question) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("确定要从错题本中删除这道题吗?");
        alert.setContentText(question.getQuestionText().substring(0, Math.min(50, question.getQuestionText().length())) + "...");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            questionManager.updateQuestionStarStatus(question);
            question.setWrong(false);
            loadWrongQuestions();
        }
    }

    @FXML
    private void handleClearAll() {
        List<Question> wrongQuestions = questionManager.getWrongQuestions(subject.getId());
        if (wrongQuestions.isEmpty()) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认清空");
        alert.setHeaderText("确定要清空所有错题吗?");
        alert.setContentText("此操作将删除 " + wrongQuestions.size() + " 道错题，无法恢复。");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            questionManager.clearAllWrongQuestions(subject.getId());
            loadWrongQuestions();
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