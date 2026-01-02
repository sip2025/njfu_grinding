package com.examapp.controller;

import com.examapp.Main;
import com.examapp.data.QuestionManager;
import com.examapp.model.ExamHistoryEntry;
import com.examapp.model.Question;
import com.examapp.model.Subject;
import com.examapp.util.IconHelper;
import com.examapp.util.LogManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * 模拟考试控制器
 */
public class MockExamController implements Initializable {
    
    @FXML private Button backButton;
    @FXML private Label subjectNameLabel;
    @FXML private Label timerLabel;
    @FXML private Label questionNumberLabel;
    @FXML private Label questionTypeLabel;
    @FXML private Label questionTextLabel;
    @FXML private VBox optionsContainer;
    @FXML private Button submitButton;
    @FXML private Label progressLabel;
    @FXML private Button previousButton;
    @FXML private Button nextButton;
    @FXML private ToggleButton favoriteButton;
    @FXML private Node questionNavPanel; // 对应 fx:include 的 fx:id

    // 注入子控制器
    @FXML private QuestionNavPanelController questionNavPanelController;
    
    private QuestionManager questionManager;
    private Subject subject;
    private List<Question> examQuestions;
    private int currentPosition = 0;
    private Map<Integer, String> userAnswers = new HashMap<>();
    private ToggleGroup optionsGroup;
    private Timeline timer;
    private int remainingSeconds = 5400; // 90 minutes
    
    public void setSubject(Subject subject) {
        this.subject = subject;
        if (subject != null) {
            subjectNameLabel.setText(subject.getDisplayName() + " - 模拟考试");
            loadExamQuestions();
            if (examQuestions != null && !examQuestions.isEmpty()) {
                displayCurrentQuestion();
                startTimer();
            } else {
                // 如果没有题目，确保显示提示信息
                showAlert(Alert.AlertType.WARNING, "提示", "题目不足，无法生成模拟考试");
                handleBack();
            }
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        questionManager = QuestionManager.getInstance();
        setupIcons();
    }

    private void setupIcons() {
        if (backButton != null) {
            backButton.setGraphic(IconHelper.getBackIcon(16));
        }
        // 删除previousButton和nextButton的图标，避免显示不全
    }

    private void updateSidebar() {
        if (questionNavPanelController != null && examQuestions != null && !examQuestions.isEmpty()) {
            questionNavPanelController.loadQuestions(
                examQuestions,
                examQuestions.get(currentPosition),
                this::jumpToQuestionByObject,
                null, // 模拟考试模式不需要筛选
                "mock_exam" // 使用独立的模拟考试模式标识
            );
        }
    }

    private void loadExamQuestions() {
        examQuestions = questionManager.getMockExamQuestions(subject.getId());
        if (examQuestions.isEmpty()) {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.WARNING, "提示", "题目不足，无法生成模拟考试");
                handleBack();
            });
        } else {
            calculateExamDisplayIds();
        }
    }

    private void calculateExamDisplayIds() {
        int singleChoiceCount = 1;
        int multipleChoiceCount = 1;
        int trueFalseCount = 1;

        for (Question q : examQuestions) {
            // 重置答题状态为未作答，确保模拟考试开始时所有题目都是未答状态
            q.setAnswerState(Question.AnswerState.UNANSWERED);
            
            switch (q.getType()) {
                case "单选题":
                    q.setExamDisplayId("单选题" + singleChoiceCount++);
                    break;
                case "多选题":
                    q.setExamDisplayId("多选题" + multipleChoiceCount++);
                    break;
                case "判断题":
                    q.setExamDisplayId("判断题" + trueFalseCount++);
                    break;
                default:
                    q.setExamDisplayId(String.valueOf(q.getRelativeId())); // Fallback
                    break;
            }
        }
    }

    private void startTimer() {
        if (timer != null) {
            timer.stop();
        }
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            updateTimerDisplay();
            
            if (remainingSeconds <= 0) {
                timer.stop();
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.WARNING, "时间到", "考试时间已到，系统将自动交卷");
                    submitExam();
                });
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void updateTimerDisplay() {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        timerLabel.setText(String.format("剩余时间: %02d:%02d", minutes, seconds));
        
        if (remainingSeconds < 300) { // 5 minutes remaining
            timerLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #F44336; -fx-font-weight: bold;");
        }
    }

    private void displayCurrentQuestion() {
        if (examQuestions == null || examQuestions.isEmpty()) return;
        
        Question question = examQuestions.get(currentPosition);
        
        questionNumberLabel.setText(question.getExamDisplayId() != null ? question.getExamDisplayId() : String.format("第 %d 题", currentPosition + 1));
        questionTypeLabel.setText(String.format("[%s]", question.getType()));
        questionTextLabel.setText(question.getQuestionText());
        
        optionsContainer.getChildren().clear();
        optionsGroup = new ToggleGroup();
        
        boolean isMultipleChoice = "多选题".equals(question.getType());
        
        List<String> options = question.getOptions();
        if (options == null || options.isEmpty()) {
            options = Arrays.asList("A. 正确", "B. 错误");
        }
        
        if (isMultipleChoice) {
            for (String option : options) {
                CheckBox checkBox = new CheckBox(option);
                checkBox.getStyleClass().add("option-checkbox");
                checkBox.setOnAction(e -> captureAnswer());
                optionsContainer.getChildren().add(checkBox);
            }
        } else {
            for (String option : options) {
                RadioButton radioButton = new RadioButton(option);
                radioButton.setToggleGroup(optionsGroup);
                radioButton.getStyleClass().add("option-radio-button");
                radioButton.setOnAction(e -> captureAnswer());
                optionsContainer.getChildren().add(radioButton);
            }
        }
        
        String savedAnswer = userAnswers.get(currentPosition);
        if (savedAnswer != null) {
            restoreAnswer(savedAnswer, isMultipleChoice);
        }
        
        previousButton.setDisable(currentPosition == 0);
        nextButton.setDisable(currentPosition >= examQuestions.size() - 1);
        submitButton.setVisible(true); // Ensure buttons are visible
        previousButton.setVisible(true);
        nextButton.setVisible(true);
        
        updateFavoriteButton(question);
        updateProgress();
        updateSidebar();
    }

    private void captureAnswer() {
        Question question = examQuestions.get(currentPosition);
        boolean isMultipleChoice = "多选题".equals(question.getType());
        
        String answer = "";
        if (isMultipleChoice) {
            StringBuilder sb = new StringBuilder();
            for (javafx.scene.Node node : optionsContainer.getChildren()) {
                if (node instanceof CheckBox && ((CheckBox) node).isSelected()) {
                    sb.append(((CheckBox) node).getText().charAt(0));
                }
            }
            answer = sb.toString();
        } else {
            Toggle selected = optionsGroup.getSelectedToggle();
            if (selected != null) {
                answer = String.valueOf(((RadioButton) selected).getText().charAt(0));
            }
        }
        
        Question currentQuestion = examQuestions.get(currentPosition);
        if (!answer.isEmpty()) {
            if (!answer.equals(userAnswers.get(currentPosition))) {
                LogManager.info("User answered question in mock exam. Question ID: " + currentQuestion.getId() + ", Answer: " + answer);
            }
            userAnswers.put(currentPosition, answer);
            currentQuestion.setAnswerState(Question.AnswerState.ANSWERED);
        } else {
            userAnswers.remove(currentPosition);
            currentQuestion.setAnswerState(Question.AnswerState.UNANSWERED);
        }
        
        updateProgress();
        if (questionNavPanelController != null) {
            questionNavPanelController.updateQuestionState(currentQuestion);
        }
    }

    private void restoreAnswer(String answer, boolean isMultipleChoice) {
        if (isMultipleChoice) {
            for (javafx.scene.Node node : optionsContainer.getChildren()) {
                if (node instanceof CheckBox) {
                    CheckBox cb = (CheckBox) node;
                    if (answer.contains(String.valueOf(cb.getText().charAt(0)))) {
                        cb.setSelected(true);
                    }
                }
            }
        } else {
            for (Toggle toggle : optionsGroup.getToggles()) {
                RadioButton rb = (RadioButton) toggle;
                if (rb.getText().startsWith(answer)) {
                    rb.setSelected(true);
                    break;
                }
            }
        }
    }

    private void updateProgress() {
        progressLabel.setText(String.format("已答: %d/%d", userAnswers.size(), examQuestions.size()));
    }

    private void jumpToQuestion(int index) {
        if (index >= 0 && index < examQuestions.size()) {
            currentPosition = index;
            displayCurrentQuestion();
        }
    }

    private void jumpToQuestionByObject(Question question) {
        int targetPosition = -1;
        for (int i = 0; i < examQuestions.size(); i++) {
            if (examQuestions.get(i).getId().equals(question.getId())) {
                targetPosition = i;
                break;
            }
        }

        if (targetPosition != -1 && targetPosition != currentPosition) {
            currentPosition = targetPosition;
            displayCurrentQuestion();
        }
    }

    @FXML
    private void handleBack() {
        LogManager.info("User clicked 'Back' during mock exam for subject: " + subject.getDisplayName());
        if (timer != null) timer.stop();
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认退出");
        alert.setHeaderText("确定要退出考试吗？");
        alert.setContentText("退出后考试数据将不会保存");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // 返回到学习模式选择页
                Main.switchSceneWithData("/fxml/study-mode.fxml", subject.getDisplayName(), controller -> {
                    if (controller instanceof StudyModeController) {
                        ((StudyModeController) controller).setSubject(subject);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // If user cancels, resume timer if it was running
            if (timer != null) {
                timer.play();
            }
        }
    }

    @FXML
    private void handlePrevious() {
        if (currentPosition > 0) {
            LogManager.info("User clicked 'Previous' button in mock exam. From question index: " + currentPosition);
            currentPosition--;
            displayCurrentQuestion();
        }
    }

    @FXML
    private void handleNext() {
        if (currentPosition < examQuestions.size() - 1) {
            LogManager.info("User clicked 'Next' button in mock exam. From question index: " + currentPosition);
            currentPosition++;
            displayCurrentQuestion();
        }
    }

    @FXML
    private void handleSubmitAnswer() {
        captureAnswer();
    }

    @FXML
    private void handleFavorite() {
        if (examQuestions == null || examQuestions.isEmpty() || subject == null) return;

        Question currentQuestion = examQuestions.get(currentPosition);
        LogManager.info("User clicked 'Favorite' button in mock exam for question ID: " + currentQuestion.getId() + ". Current state: " + (currentQuestion.isWrong() ? "Favorited" : "Not favorited"));
        
        currentQuestion.setWrong(!currentQuestion.isWrong());
        questionManager.updateQuestionStarStatus(currentQuestion);
        
        updateFavoriteButton(currentQuestion);
        
        if (questionNavPanelController != null) {
            questionNavPanelController.updateQuestionState(currentQuestion);
        }
    }

    private void updateFavoriteButton(Question question) {
        if (question.isWrong()) {
            favoriteButton.setSelected(true);
            favoriteButton.setText("★ 已标记");
        } else {
            favoriteButton.setSelected(false);
            favoriteButton.setText("☆ 星标");
        }
    }

    @FXML
    private void handleSubmitExam() {
        LogManager.info("User clicked 'Submit Exam' button for subject: " + subject.getDisplayName());
        List<Integer> unanswered = new ArrayList<>();
        for (int i = 0; i < examQuestions.size(); i++) {
            if (!userAnswers.containsKey(i)) {
                unanswered.add(i + 1);
            }
        }
        
        String message;
        if (!unanswered.isEmpty()) {
            message = String.format("还有 %d 道题未作答，确定要交卷吗？", unanswered.size());
        } else {
            message = "已完成所有题目，确定要交卷吗？";
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认交卷");
        alert.setHeaderText(message);
        alert.setContentText("交卷后将无法修改答案");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            submitExam();
        }
    }

    private void submitExam() {
        if (timer != null) timer.stop();
        
        int totalScore = 0;
        int maxScore = 0;
        int correctCount = 0;
        List<ExamHistoryEntry.QuestionRecord> records = new ArrayList<>();
        
        for (int i = 0; i < examQuestions.size(); i++) {
            Question question = examQuestions.get(i);
            String userAnswer = userAnswers.get(i);
            question.setUserAnswer(userAnswer);
            
            int questionScore = questionManager.scoreQuestion(subject.getId(), question);
            maxScore += questionScore;
            
            boolean isCorrect = question.isAnsweredCorrectly();
            if (isCorrect) {
                totalScore += questionScore;
                correctCount++;
            }
            
            ExamHistoryEntry.QuestionRecord record = new ExamHistoryEntry.QuestionRecord();
            record.setQuestionId(question.getId());
            record.setQuestionText(question.getQuestionText());
            record.setCorrectAnswer(question.getAnswer());
            record.setUserAnswer(userAnswer);
            record.setCorrect(isCorrect);
            records.add(record);
        }
        
        ExamHistoryEntry entry = new ExamHistoryEntry();
        entry.setTimestamp(System.currentTimeMillis());
        entry.setSubjectId(subject.getId());
        entry.setSubjectName(subject.getDisplayName());
        entry.setTotalQuestions(examQuestions.size());
        entry.setAnsweredQuestions(userAnswers.size());
        entry.setScore(totalScore);
        entry.setMaxScore(maxScore);
        entry.setQuestionRecords(records);
        
        questionManager.addExamHistoryEntry(entry);
        
        showResultScene(entry);
    }

    private void showResultScene(ExamHistoryEntry entry) {
        try {
            Main.switchSceneWithData("/fxml/result.fxml", "考试结果", controller -> {
                if (controller instanceof ResultController) {
                    ((ResultController) controller).setHistoryEntry(entry);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法加载结果页面");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}