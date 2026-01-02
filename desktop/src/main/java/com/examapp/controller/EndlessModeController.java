package com.examapp.controller;

import com.examapp.Main;
import com.examapp.data.QuestionManager;
import com.examapp.model.Question;
import com.examapp.model.Subject;
import com.examapp.util.IconHelper;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class EndlessModeController implements Initializable {

    // FXML fields
    @FXML private Button backButton;
    @FXML private Label subjectNameLabel;
    @FXML private Label streakLabel;
    @FXML private Label bestStreakLabel;
    @FXML private Label questionNumberLabel;
    @FXML private Label questionTypeLabel;
    @FXML private Label questionTextLabel;
    @FXML private VBox optionsContainer;
    @FXML private VBox feedbackPane;
    @FXML private Label feedbackLabel;
    @FXML private VBox explanationPane;
    @FXML private Label explanationLabel;
    @FXML private Button submitButton;
    @FXML private Button continueButton;
    @FXML private Button previousButton;
    @FXML private Button nextButton;
    @FXML private Button favoriteButton;
    @FXML private Button aiAssistButton;
    @FXML private SplitPane mainSplitPane;
    @FXML private VBox aiPanelContainer;
    @FXML private VBox aiPanel;
    @FXML private AIPanelController aiPanelController;

    // Class members
    private QuestionManager questionManager;
    private Subject subject;
    private List<Question> allQuestions;
    private Deque<Question> questionDeck;
    private List<Question> questionHistory;
    private int historyIndex = -1;
    private Question currentQuestion;
    private int currentStreak = 0;
    private int bestStreak = 0;
    private ToggleGroup optionsGroup;
    private GameState gameState;

    private enum GameState {
        PLAYING,
        SHOWING_ANSWER,
        REVIEWING // 新增状态，用于回顾历史题目
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
        if (subject != null) {
            this.questionManager = QuestionManager.getInstance();
            this.allQuestions = new ArrayList<>(subject.getQuestions());
            this.bestStreak = questionManager.getEndlessBestStreak(subject.getId());

            subjectNameLabel.setText(subject.getDisplayName() + " - 无尽模式");
            bestStreakLabel.setText("最高纪录: " + bestStreak);

            startGame();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        backButton.setGraphic(IconHelper.getBackIcon(24));
        
        if (aiPanelContainer != null) {
            aiPanelContainer.setManaged(false);
            aiPanelContainer.setVisible(false);
        }
        
        // 设置AI面板关闭回调
        if (aiPanelController != null) {
            aiPanelController.setOnCloseCallback(v -> {
                aiPanelContainer.setVisible(false);
                aiPanelContainer.setManaged(false);
                mainSplitPane.setDividerPositions(1.0);
            });
        }
    }

    private void startGame() {
        currentStreak = 0;
        questionHistory = new ArrayList<>();
        historyIndex = -1;
        streakLabel.setText("当前连对: 0");

        if (subject != null) {
            List<Question> questionsToShuffle = new ArrayList<>(allQuestions);
            Collections.shuffle(questionsToShuffle);
            this.questionDeck = new ArrayDeque<>(questionsToShuffle);
        }
        fetchAndDisplayNewQuestion();
    }

    private void fetchAndDisplayNewQuestion() {
        if (questionDeck == null || questionDeck.isEmpty()) {
            List<Question> questionsToShuffle = new ArrayList<>(allQuestions);
            Collections.shuffle(questionsToShuffle);
            this.questionDeck = new ArrayDeque<>(questionsToShuffle);
        }

        currentQuestion = questionDeck.poll();
        questionHistory.add(currentQuestion);
        historyIndex = questionHistory.size() - 1;
        
        displayQuestion();
    }

    private void displayQuestion() {
        if (historyIndex == questionHistory.size() - 1) {
            gameState = GameState.PLAYING;
        } else {
            gameState = GameState.REVIEWING;
        }

        questionNumberLabel.setText("第 " + (historyIndex + 1) + " 题");
        questionTypeLabel.setText(String.format("[%s]", currentQuestion.getType()));
        questionTextLabel.setText(currentQuestion.getQuestionText());
        streakLabel.setText("当前连对: " + currentStreak);

        updateFavoriteButton();
        optionsContainer.getChildren().clear();
        optionsContainer.setDisable(false);
        optionsGroup = new ToggleGroup();

        feedbackPane.setVisible(false);
        feedbackPane.setManaged(false);
        explanationPane.setVisible(false);
        explanationPane.setManaged(false);
        continueButton.setVisible(false);
        continueButton.setManaged(false);

        boolean isMultipleChoice = "多选题".equals(currentQuestion.getType());
        submitButton.setVisible(isMultipleChoice);
        submitButton.setManaged(isMultipleChoice);

        List<String> options = currentQuestion.getOptions();
        if (options == null || options.isEmpty()) {
            options = Arrays.asList("A. 正确", "B. 错误");
        }

        if (isMultipleChoice) {
            for (String option : options) {
                CheckBox checkBox = new CheckBox(option);
                checkBox.getStyleClass().add("option-checkbox");
                optionsContainer.getChildren().add(checkBox);
            }
        } else {
            for (String option : options) {
                RadioButton radioButton = new RadioButton(option);
                radioButton.setToggleGroup(optionsGroup);
                radioButton.getStyleClass().add("option-radio-button");
                radioButton.setOnAction(e -> evaluateAnswer());
                optionsContainer.getChildren().add(radioButton);
            }
        }
    }

    @FXML
    private void evaluateAnswer() {
        if (gameState != GameState.PLAYING) return;

        String userAnswer = getUserAnswer();
        if (userAnswer == null || userAnswer.isEmpty()) {
            return; // Or show a prompt for multi-choice
        }

        optionsContainer.setDisable(true);
        submitButton.setVisible(false);
        submitButton.setManaged(false);

        boolean isCorrect = userAnswer.equals(currentQuestion.getAnswer());

        if (isCorrect) {
            handleCorrectAnswer();
        } else {
            handleWrongAnswer();
        }
    }

    private void handleCorrectAnswer() {
        currentStreak++;
        streakLabel.setText("当前连对: " + currentStreak);
        showFeedback(true, null);

        PauseTransition pause = new PauseTransition(Duration.millis(800));
        pause.setOnFinished(event -> fetchAndDisplayNewQuestion());
        pause.play();
    }

    private void handleWrongAnswer() {
        gameState = GameState.SHOWING_ANSWER;
        updateBestStreak();
        questionManager.incrementWrongAnswerCount(currentQuestion.getId());
        
        String feedbackText = "答错了! 正确答案是: " + currentQuestion.getFormattedAnswer();
        showFeedback(false, feedbackText);
        
        continueButton.setVisible(true);
        continueButton.setManaged(true);
    }

    private String getUserAnswer() {
        boolean isMultipleChoice = "多选题".equals(currentQuestion.getType());
        boolean isTrueFalse = "判断题".equals(currentQuestion.getType());

        if (isMultipleChoice) {
            StringBuilder answer = new StringBuilder();
            for (Node node : optionsContainer.getChildren()) {
                if (node instanceof CheckBox && ((CheckBox) node).isSelected()) {
                    answer.append(((CheckBox) node).getText().charAt(0));
                }
            }
            char[] chars = answer.toString().toCharArray();
            Arrays.sort(chars);
            return new String(chars);
        } else {
            Toggle selected = optionsGroup.getSelectedToggle();
            if (selected != null) {
                RadioButton rb = (RadioButton) selected;
                String optionText = rb.getText();
                
                if (isTrueFalse) {
                    // 判断题特殊处理:提取"正确"或"错误"
                    if (optionText.contains("正确")) {
                        return "正确";
                    } else if (optionText.contains("错误")) {
                        return "错误";
                    }
                }
                
                // 其他题型返回选项字母
                return String.valueOf(optionText.charAt(0));
            }
        }
        return null;
    }

    private void showFeedback(boolean isCorrect, String text) {
        feedbackPane.setVisible(true);
        feedbackPane.setManaged(true);
        feedbackLabel.getStyleClass().removeAll("feedback-correct", "feedback-wrong");

        if (isCorrect) {
            feedbackLabel.setText("✓ 正确!");
            feedbackLabel.getStyleClass().add("feedback-correct");
        } else {
            feedbackLabel.setText(text);
            feedbackLabel.getStyleClass().add("feedback-wrong");
            if (currentQuestion.getExplanation() != null && !currentQuestion.getExplanation().isEmpty()) {
                explanationPane.setVisible(true);
                explanationPane.setManaged(true);
                explanationLabel.setText(currentQuestion.getExplanation());
            }
        }
    }

    @FXML
    private void handlePrevious() {
        if (historyIndex > 0) {
            historyIndex--;
            currentQuestion = questionHistory.get(historyIndex);
            displayQuestion();
        }
    }

    @FXML
    private void handleNext() {
        if (historyIndex < questionHistory.size() - 1) {
            historyIndex++;
            currentQuestion = questionHistory.get(historyIndex);
            displayQuestion();
        } else {
            fetchAndDisplayNewQuestion();
        }
    }
    
    @FXML
    private void handleContinue() {
        // This is called after a wrong answer. It resets the streak and starts the next question.
        currentStreak = 0;
        fetchAndDisplayNewQuestion();
    }
    
    private void updateNavigationButtons() {
        previousButton.setDisable(historyIndex <= 0);
        nextButton.setDisable(false); // Next is always available
    }
    
    @FXML
    private void toggleStar() {
        currentQuestion.setWrong(!currentQuestion.isWrong());
        questionManager.updateQuestionStarStatus(currentQuestion);
        updateFavoriteButton();
    }

    private void updateFavoriteButton() {
        if (currentQuestion.isWrong()) {
            favoriteButton.setText("★ 星标");
        } else {
            favoriteButton.setText("☆ 星标");
        }
    }

    private void updateBestStreak() {
        if (currentStreak > bestStreak) {
            bestStreak = currentStreak;
            questionManager.updateEndlessBestStreak(subject.getId(), bestStreak);
            bestStreakLabel.setText("最高纪录: " + bestStreak);
        }
    }

    @FXML
    private void handleAIAssist() {
        if (currentQuestion == null) return;
        
        boolean isVisible = aiPanelContainer.isVisible();
        aiPanelContainer.setVisible(!isVisible);
        aiPanelContainer.setManaged(!isVisible);
        
        if (!isVisible) {
            mainSplitPane.setDividerPositions(0.6);
            if (aiPanelController != null) {
                aiPanelController.analyzeQuestion(currentQuestion);
            }
        } else {
            mainSplitPane.setDividerPositions(1.0);
        }
    }

    @FXML
    private void handleBack() {
        updateBestStreak();
        try {
            Main.switchSceneWithData("/fxml/study-mode.fxml", subject.getDisplayName(), controller -> {
                if (controller instanceof StudyModeController) {
                    ((StudyModeController) controller).setSubject(subject);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}