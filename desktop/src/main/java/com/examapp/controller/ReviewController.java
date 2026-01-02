package com.examapp.controller;

import com.examapp.Main;
import com.examapp.data.QuestionManager;
import com.examapp.model.Question;
import com.examapp.model.Subject;
import com.examapp.util.LogManager;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ReviewController {

    @FXML private Label subjectNameLabel;
    @FXML private Label questionNumberLabel;
    @FXML private Label questionTypeLabel;
    @FXML private Label questionTextLabel;
    @FXML private VBox optionsContainer;
    @FXML private VBox explanationPane;
    @FXML private Label explanationLabel;
    @FXML private Button nextButton;
    @FXML private Button previousButton;
    @FXML private Label resultLabel; // 显示用户答案
    @FXML private Button aiAssistButton;
    @FXML private SplitPane mainSplitPane;
    @FXML private VBox aiPanelContainer;
    @FXML private VBox aiPanel;
    @FXML private AIPanelController aiPanelController;

    private List<Question> questions;
    private int currentPosition;
    private Question currentQuestion;
    private Subject subject;
    private QuestionManager questionManager;
    private ToggleGroup optionsGroup;
    private boolean isExamReviewMode = false; // 是否为试卷回顾模式
    private String backDestination = "wrong-questions"; // 返回目标: "wrong-questions" 或 "result"

    @FXML
    public void initialize() {
        this.questionManager = QuestionManager.getInstance();
        
        if (aiPanelContainer != null) {
            aiPanelContainer.setManaged(false);
            aiPanelContainer.setVisible(false);
        }
        
        if (aiPanelController != null) {
            aiPanelController.setOnCloseCallback(v -> {
                aiPanelContainer.setVisible(false);
                aiPanelContainer.setManaged(false);
                mainSplitPane.setDividerPositions(1.0);
            });
        }
    }

    public void setReviewData(Subject subject, List<Question> questions) {
        setReviewData(subject, questions, false, "wrong-questions");
    }

    public void setReviewData(Subject subject, List<Question> questions, boolean isExamReview, String backDest) {
        this.subject = subject;
        this.questions = new ArrayList<>(questions); // Create a mutable copy
        this.isExamReviewMode = isExamReview;
        this.backDestination = backDest;
        
        if (!isExamReviewMode) {
            Collections.shuffle(this.questions); // Shuffle only for wrong question mode
        }
        this.currentPosition = 0;

        if (this.questions.isEmpty()) {
            handleBack();
        } else {
            displayCurrentQuestion();
        }
    }

    private void displayCurrentQuestion() {
        if (questions == null || questions.isEmpty()) {
            // All questions answered correctly (only in wrong question mode)
            questionTextLabel.setText("恭喜你，所有错题都已掌握！");
            optionsContainer.getChildren().clear();
            explanationPane.setVisible(false);
            nextButton.setText("返回");
            nextButton.setOnAction(e -> handleBack());
            nextButton.setVisible(true);
            previousButton.setVisible(false);
            if (resultLabel != null) resultLabel.setVisible(false);
            return;
        }

        currentQuestion = questions.get(currentPosition);

        if (isExamReviewMode) {
            subjectNameLabel.setText(subject.getDisplayName() + " - 试卷回顾");
            questionNumberLabel.setText(String.format("第 %d / %d 题", currentPosition + 1, questions.size()));
            
            // 题型单独显示
            questionTypeLabel.setText(String.format("[%s]", currentQuestion.getType()));
            questionTextLabel.setText(currentQuestion.getQuestionText());
            
            renderOptionsForExamReview();
            
            // 显示用户答案对比
            if (resultLabel != null) {
                String userAns = currentQuestion.getUserAnswer();
                String correctAns = currentQuestion.getAnswer();
                if (userAns == null || userAns.isEmpty()) {
                    resultLabel.setText("你的答案: 未作答 | 正确答案: " + correctAns);
                    resultLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #757575;");
                } else if (userAns.equals(correctAns)) {
                    resultLabel.setText("你的答案: " + userAns + " ✓");
                    resultLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
                } else {
                    resultLabel.setText("你的答案: " + userAns + " | 正确答案: " + correctAns);
                    resultLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #F44336;");
                }
                resultLabel.setVisible(true);
                resultLabel.setManaged(true);
            }
            
            // 显示解析
            if (currentQuestion.getExplanation() != null && !currentQuestion.getExplanation().trim().isEmpty()) {
                explanationLabel.setText(currentQuestion.getExplanation());
                explanationPane.setVisible(true);
                explanationPane.setManaged(true);
            } else {
                explanationPane.setVisible(false);
                explanationPane.setManaged(false);
            }
            
            nextButton.setVisible(true);
            previousButton.setVisible(true);
            previousButton.setDisable(currentPosition == 0);
            nextButton.setDisable(currentPosition >= questions.size() - 1);
        } else {
            subjectNameLabel.setText(subject.getDisplayName() + " - 错题重做");
            questionNumberLabel.setText(String.format("剩余 %d 题", questions.size()));
            questionTextLabel.setText(currentQuestion.getQuestionText());
            
            renderOptions();
            explanationPane.setVisible(false);
            explanationPane.setManaged(false);
            nextButton.setVisible(false);
            previousButton.setVisible(false);
            if (resultLabel != null) {
                resultLabel.setVisible(false);
                resultLabel.setManaged(false);
            }
        }
    }

    private void renderOptions() {
        optionsContainer.getChildren().clear();
        optionsContainer.setDisable(false);
        optionsGroup = new ToggleGroup();

        List<String> options = currentQuestion.getOptions();
        if ("判断题".equals(currentQuestion.getType())) {
            options = Arrays.asList("A. 正确", "B. 错误");
        }

        if (options == null) return;

        if ("多选题".equals(currentQuestion.getType())) {
            for (String option : options) {
                CheckBox checkBox = new CheckBox(option);
                checkBox.getStyleClass().add("option-checkbox");
                optionsContainer.getChildren().add(checkBox);
            }
        } else { // 单选题和判断题
            for (String option : options) {
                RadioButton radioButton = new RadioButton(option);
                radioButton.setToggleGroup(optionsGroup);
                radioButton.getStyleClass().add("option-radio-button");
                radioButton.setOnAction(e -> evaluateAnswer());
                optionsContainer.getChildren().add(radioButton);
            }
        }
    }
    
    private void renderOptionsForExamReview() {
        optionsContainer.getChildren().clear();
        optionsContainer.setDisable(true);
        
        List<String> options = currentQuestion.getOptions();
        if ("判断题".equals(currentQuestion.getType())) {
            options = Arrays.asList("A. 正确", "B. 错误");
        }
        
        if (options == null) return;
        
        String correctAnswer = currentQuestion.getAnswer();
        String userAnswer = currentQuestion.getUserAnswer();
        
        // 创建不可交互的Label来显示选项
        for (String option : options) {
            Label optionLabel = new Label(option);
            optionLabel.setWrapText(true);
            optionLabel.getStyleClass().add("option-label");
            // 明确设置默认文字颜色
            optionLabel.setStyle("-fx-text-fill: #333333;");
            
            String optionLetter = String.valueOf(option.charAt(0));
            
            // 绿色标记正确答案
            if (correctAnswer != null && correctAnswer.contains(optionLetter)) {
                optionLabel.getStyleClass().add("option-correct");
            }
            // 红色填充用户选择的错误答案
            else if (userAnswer != null && userAnswer.contains(optionLetter)) {
                optionLabel.getStyleClass().add("option-wrong");
            }
            
            optionsContainer.getChildren().add(optionLabel);
        }
    }

    @FXML
    private void evaluateAnswer() {
        String userAnswer = getUserAnswer();
        if (userAnswer == null || userAnswer.isEmpty()) return;

        LogManager.info("User answered wrong question review. Question ID: " + currentQuestion.getId() + ", Answer: " + userAnswer + ", Correct: " + userAnswer.equals(currentQuestion.getAnswer()));

        optionsContainer.setDisable(true);

        boolean isCorrect = userAnswer.equals(currentQuestion.getAnswer());

        if (isCorrect) {
            handleCorrectAnswer();
        } else {
            handleWrongAnswer();
        }
    }

    private void handleCorrectAnswer() {
        // Visual feedback: turn correct option(s) green
        for (Node node : optionsContainer.getChildren()) {
            if (node instanceof RadioButton) {
                RadioButton rb = (RadioButton) node;
                String optionText = rb.getText();
                if (currentQuestion.getAnswer().contains(String.valueOf(optionText.charAt(0)))) {
                    rb.getStyleClass().add("option-correct");
                }
            } else if (node instanceof CheckBox) {
                CheckBox cb = (CheckBox) node;
                String optionText = cb.getText();
                if (currentQuestion.getAnswer().contains(String.valueOf(optionText.charAt(0)))) {
                    cb.getStyleClass().add("option-correct");
                }
            }
        }

        // In exam review mode, just move to next question; in wrong question mode, remove from list
        if (isExamReviewMode) {
            // Show next button in exam review mode
            nextButton.setText("下一题");
            nextButton.setOnAction(e -> handleNext());
            nextButton.setVisible(true);
        } else {
            // Remove the correctly answered question from the list in wrong question mode
            questions.remove(currentPosition);

            // Pause and then proceed with green feedback
            PauseTransition pause = new PauseTransition(Duration.millis(400));
            pause.setOnFinished(event -> {
                if (currentPosition >= questions.size()) {
                    currentPosition = 0; // Reset if we've reached the end
                }
                displayCurrentQuestion();
            });
            pause.play();
        }
    }

    private void handleWrongAnswer() {
        // Increment wrong answer count (错题再做错，错题次数加一)
        questionManager.incrementWrongAnswerCount(currentQuestion.getId());

        // Show explanation
        if (currentQuestion.getExplanation() != null && !currentQuestion.getExplanation().trim().isEmpty()) {
            explanationLabel.setText(currentQuestion.getExplanation());
            explanationPane.setVisible(true);
            explanationPane.setManaged(true);
        }

        // Highlight correct and wrong answers
        highlightAnswers(currentQuestion, getUserAnswer());

        // Show next button to allow user to proceed
        nextButton.setText("继续");
        nextButton.setOnAction(e -> handleNext());
        nextButton.setVisible(true);
    }
    
    private void highlightAnswers(Question question, String userAnswer) {
        String correctAnswer = question.getAnswer();
        
        // 判断题特殊处理：需要转换A/B和正确/错误
        boolean isTrueFalse = "判断题".equals(question.getType());
        
        for (Node node : optionsContainer.getChildren()) {
            if (node instanceof RadioButton) {
                RadioButton rb = (RadioButton) node;
                String optionText = rb.getText();
                String optionLetter = String.valueOf(optionText.charAt(0));
                
                boolean isCorrectOption = false;
                boolean isUserOption = false;
                
                if (isTrueFalse) {
                    // 判断题：比较"正确"/"错误"或"A"/"B"
                    if (optionText.contains("正确") && (correctAnswer.equals("正确") || correctAnswer.equals("A"))) {
                        isCorrectOption = true;
                    } else if (optionText.contains("错误") && (correctAnswer.equals("错误") || correctAnswer.equals("B"))) {
                        isCorrectOption = true;
                    }
                    
                    if (optionText.contains("正确") && (userAnswer.equals("正确") || userAnswer.equals("A"))) {
                        isUserOption = true;
                    } else if (optionText.contains("错误") && (userAnswer.equals("错误") || userAnswer.equals("B"))) {
                        isUserOption = true;
                    }
                } else {
                    // 单选题：直接比较字母
                    isCorrectOption = correctAnswer.contains(optionLetter);
                    isUserOption = userAnswer != null && userAnswer.contains(optionLetter);
                }
                
                if (isCorrectOption) {
                    rb.getStyleClass().add("option-correct");
                } else if (isUserOption) {
                    rb.getStyleClass().add("option-wrong");
                }
            } else if (node instanceof CheckBox) {
                CheckBox cb = (CheckBox) node;
                String optionText = cb.getText();
                String optionLetter = String.valueOf(optionText.charAt(0));
                
                if (correctAnswer.contains(optionLetter)) {
                    cb.getStyleClass().add("option-correct");
                } else if (userAnswer != null && userAnswer.contains(optionLetter)) {
                    cb.getStyleClass().add("option-wrong");
                }
            }
        }
    }


    private String getUserAnswer() {
        if ("多选题".equals(currentQuestion.getType())) {
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
                return String.valueOf(((RadioButton) selected).getText().charAt(0));
            }
        }
        return null;
    }


    @FXML
    private void handleBack() {
        LogManager.info("User clicked 'Back' from review for subject: " + subject.getDisplayName());
        try {
            if ("result".equals(backDestination)) {
                Main.switchSceneWithData("/fxml/history.fxml", "考试历史", controller -> {
                    if (controller instanceof HistoryController) {
                        ((HistoryController) controller).setSubject(subject);
                    }
                });
            } else {
                Main.switchSceneWithData("/fxml/wrong-questions.fxml", "错题本", controller -> {
                    if (controller instanceof WrongQuestionsController) {
                        ((WrongQuestionsController) controller).setSubject(subject);
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handlePrevious() {
        if (currentPosition > 0) {
            currentPosition--;
            displayCurrentQuestion();
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
    private void handleNext() {
        LogManager.info("User clicked 'Next' in review. Question ID: " + currentQuestion.getId());
        // Move to the next question in the list
        currentPosition++;
        if (currentPosition >= questions.size()) {
            if (isExamReviewMode) {
                // In exam review mode, go back when reaching the end
                handleBack();
                return;
            } else {
                // In wrong question mode, loop back to the start
                currentPosition = 0;
            }
        }
        displayCurrentQuestion();
    }
}