package com.examapp.controller;

import com.examapp.Main;
import com.examapp.data.QuestionManager;
import com.examapp.model.Question;
import com.examapp.model.Subject;
import com.examapp.service.AIService;
import com.examapp.util.DraggableFABHelper;
import com.examapp.util.IconHelper;
import com.examapp.util.LogManager;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 练习模式控制器 (重构后)
 */
public class PracticeController implements Initializable {

    @FXML private AnchorPane rootPane;
    @FXML private Label subjectNameLabel;
    @FXML private Label progressLabel;
    @FXML private Label questionNumberLabel;
    @FXML private Label questionTypeLabel;
    @FXML private Label questionTextLabel;
    @FXML private VBox optionsContainer;
    @FXML private Button submitButton;
    @FXML private VBox feedbackPane;
    @FXML private Label feedbackLabel;
    @FXML private VBox explanationPane;
    @FXML private Label explanationLabel;
    @FXML private Button favoriteButton;
    @FXML private Button previousButton;
    @FXML private Button nextButton;
    @FXML private Button aiAssistButton;
    @FXML private VBox questionContentBox;
    @FXML private Node questionNavPanel; // 对应 fx:include 的 fx:id
    @FXML private SplitPane mainSplitPane;
    @FXML private VBox aiPanelContainer;
    @FXML private Node aiPanel;

    // 注入子控制器
    @FXML private QuestionNavPanelController questionNavPanelController;
    @FXML private AIPanelController aiPanelController;

    private QuestionManager questionManager;
    private AIService aiService;
    private Subject subject;
    private List<Question> questions; // For practice logic
    private List<Question> sidebarQuestions; // For display order in sidebar
    private List<Question> baseQuestions;
    private int currentPosition = 0;
    private String mode;
    private boolean isRandomMode = false;
    private boolean isStudyMode = false;
    private boolean isWrongQuestionMode = false;
    private ToggleGroup optionsGroup;
    private Map<Integer, String> userAnswers = new HashMap<>();

    // 滑动手势变量
    private double startX;
    private static final double SWIPE_THRESHOLD = 75;

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public void setMode(boolean random) {
        this.isRandomMode = random;
        this.mode = random ? "random" : "sequential";
    }

    public void setStudyMode(boolean studyMode) {
        this.isStudyMode = studyMode;
        this.mode = "review";
    }

    public void setWrongQuestionMode(boolean wrongQuestionMode) {
        this.isWrongQuestionMode = wrongQuestionMode;
        // 错题回顾模式的侧边栏逻辑和背题模式一致
        this.mode = "review";
        // 但题目本身需要随机
        this.isRandomMode = true;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        questionManager = QuestionManager.getInstance();
        aiService = AIService.getInstance();

        if (aiPanelContainer != null) {
            aiPanelContainer.setManaged(false);
            aiPanelContainer.setVisible(false);
        }
        
        if (aiPanelController != null) {
            aiPanelController.setOnCloseCallback(v -> {
                aiPanelContainer.setVisible(false);
                aiPanelContainer.setManaged(false);
                mainSplitPane.setDividerPositions(0.25, 1.0);
            });
        }

        setupSwipeGestures();
    }

    /**
     * 在所有模式和科目设置完成后，调用此方法开始练习
     */
    public void start() {
        if (subject != null) {
            subjectNameLabel.setText(subject.getDisplayName());
            loadQuestions();
            if (questions != null && !questions.isEmpty()) {
                displayCurrentQuestion();
            }
        } else {
            // 异常处理，比如返回或显示错误
            showAlert(Alert.AlertType.ERROR, "错误", "未设置题库，无法开始练习。");
            handleBack();
        }
    }

    private void setupSwipeGestures() {
        questionContentBox.setOnMousePressed((MouseEvent event) -> {
            startX = event.getSceneX();
        });

        questionContentBox.setOnMouseReleased((MouseEvent event) -> {
            double endX = event.getSceneX();
            double deltaX = endX - startX;

            if (deltaX > SWIPE_THRESHOLD) {
                handlePrevious();
            } else if (deltaX < -SWIPE_THRESHOLD) {
                handleNext();
            }
        });
    }

    private void updateSidebar() {
        if (questionNavPanelController != null && sidebarQuestions != null && !sidebarQuestions.isEmpty()) {
            questionNavPanelController.loadQuestions(
                sidebarQuestions,
                questions.isEmpty() ? null : questions.get(currentPosition),
                this::jumpToQuestionByObject,
                this::filterAndReloadQuestions,
                mode
            );
        }
    }

    private void loadQuestions() {
        if (subject == null) return;

        if (isWrongQuestionMode) {
            baseQuestions = new ArrayList<>(questionManager.getWrongQuestions(subject.getId()));
            
            // For sidebar: sort by original order
            sidebarQuestions = questionManager.getClonedQuestions(baseQuestions);
            sidebarQuestions.sort(Comparator.comparingInt(Question::getRelativeId));

            // For practice: shuffle
            questions = questionManager.getClonedQuestions(baseQuestions);
            Collections.shuffle(questions);

        } else {
             baseQuestions = subject.getQuestions() != null ?
                new ArrayList<>(subject.getQuestions()) : new ArrayList<>();
            questions = questionManager.getClonedQuestions(baseQuestions);
            sidebarQuestions = questions; // In other modes, they are the same
        }
        
        // isRandomMode is true for both random practice and wrong question review
        if (isRandomMode) {
            for (Question q : questions) {
                q.setUserAnswer(null);
                q.setAnswerState(Question.AnswerState.UNANSWERED);
            }
            if (!isWrongQuestionMode) { // Shuffle for normal random mode
                 Collections.shuffle(questions);
            }
            currentPosition = 0;
        } else { // Sequential / Study mode
            currentPosition = Math.max(0, subject.getSequentialLastPosition());
        }

        if (questions.isEmpty()) {
            String message = isWrongQuestionMode ? "太棒了，当前没有错题！" : "没有可用的题目";
            showAlert(Alert.AlertType.INFORMATION, "提示", message);
            handleBack();
        }
    }

    private void displayCurrentQuestion() {
        if (questions == null || questions.isEmpty() || currentPosition >= questions.size()) {
             showAlert(Alert.AlertType.INFORMATION, "完成", "所有题目已练习完毕！");
             handleBack();
             return;
        }

        Question question = questions.get(currentPosition);

        questionNumberLabel.setText(String.format("第 %d 题", question.getRelativeId()));
        questionTypeLabel.setText(String.format("[%s]", question.getType()));
        questionTextLabel.setText(question.getQuestionText());
        progressLabel.setText(String.format("进度: %d/%d", currentPosition + 1, questions.size()));

        optionsContainer.getChildren().clear();
        optionsContainer.setDisable(false);
        optionsGroup = new ToggleGroup();

        feedbackPane.setVisible(false);
        feedbackPane.setManaged(false);
        explanationPane.setVisible(false);
        explanationPane.setManaged(false);

        updateFavoriteButton(question);

        // 统一由 displayPracticeMode 处理，内部根据 isStudyMode 决定展示方式
        displayPracticeMode(question);

        previousButton.setDisable(currentPosition == 0);
        nextButton.setDisable(currentPosition >= questions.size() - 1);

        updateSidebar();
    }

    private void displayPracticeMode(Question question) {
        boolean isMultipleChoice = "多选题".equals(question.getType());
        submitButton.setVisible(isMultipleChoice && !isStudyMode);
        submitButton.setManaged(isMultipleChoice && !isStudyMode);

        List<String> options = question.getOptions();
        if (options == null || options.isEmpty()) {
            options = Arrays.asList("A. 正确", "B. 错误");
        }
        
        // 清除当前题目在userAnswers中的记录,避免错题痕迹
        if (isRandomMode && !userAnswers.containsKey(currentPosition)) {
            // 在随机模式下,如果当前题目没有答案记录,确保不恢复任何选项状态
        }

        // 背题模式：直接显示答案和解析
        if (isStudyMode) {
            optionsContainer.setDisable(true);

            // 针对判断题的特殊处理
            if ("判断题".equals(question.getType())) {
                Label correctLabel = new Label("正确");
                correctLabel.setWrapText(true);
                correctLabel.getStyleClass().add("option-label");
                optionsContainer.getChildren().add(correctLabel);

                Label wrongLabel = new Label("错误");
                wrongLabel.setWrapText(true);
                wrongLabel.getStyleClass().add("option-label");
                optionsContainer.getChildren().add(wrongLabel);
            } else {
                for (String option : options) {
                    Label optionLabel = new Label(option);
                    optionLabel.setWrapText(true);
                    optionLabel.getStyleClass().add("option-label"); // 使用样式类
                    optionsContainer.getChildren().add(optionLabel);
                }
            }
            showFeedback(true, question); // 直接显示反馈和解析

        // 练习模式：显示可交互的选项
        } else {
            optionsContainer.setDisable(false);
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
            // 只有在非随机模式下,或者在随机模式下已经有保存的答案时,才恢复选项状态
            String savedAnswer = userAnswers.get(currentPosition);
            if (savedAnswer != null && (!isRandomMode || question.getAnswerState() != Question.AnswerState.UNANSWERED)) {
                restoreAnswer(savedAnswer, isMultipleChoice);
            }
        }
    }

    private void evaluateAnswer() {
        Question question = questions.get(currentPosition);
        String userAnswer = getUserAnswer();

        if (userAnswer == null || userAnswer.isEmpty()) {
            return;
        }

        userAnswers.put(currentPosition, userAnswer);
        question.setUserAnswer(userAnswer);

        boolean isCorrect = question.isAnsweredCorrectly();
        question.setAnswerState(isCorrect ? Question.AnswerState.CORRECT : Question.AnswerState.WRONG);

        if (subject != null) {
            int originalIndex = getOriginalQuestionIndex(currentPosition);
            questionManager.recordAnswer(subject.getId(), originalIndex, userAnswer, isCorrect);

            if (!isCorrect) {
                if (isWrongQuestionMode) {
                    // 错题回顾模式下答错，增加错误次数
                    questionManager.incrementWrongAnswerCount(question.getId());
                } else if (!question.isWrong()) {
                    // 普通模式下答错，加入错题本
                    questionManager.addWrongQuestion(subject.getId(), originalIndex);
                    question.setWrong(true);
                    if (originalIndex < baseQuestions.size()) {
                        baseQuestions.get(originalIndex).setWrong(true);
                    }
                }
            } else {
                // 答对题目
                if (question.isWrong()) {
                    // 如果是错题，则从错题本中移除
                    questionManager.removeWrongQuestion(subject.getId(), originalIndex);
                    question.setWrong(false);
                    if (originalIndex < baseQuestions.size()) {
                        baseQuestions.get(originalIndex).setWrong(false);
                    }
                }
            }
        }
        
        // 错题模式下答对，高亮并自动从列表中移除进入下一题
        if (isWrongQuestionMode && isCorrect) {
            optionsContainer.setDisable(true);
            showFeedback(true, question); // 在延迟前高亮
            PauseTransition pause = new PauseTransition(Duration.seconds(0.4));
            pause.setOnFinished(event -> {
                questions.remove(currentPosition);
                if (currentPosition >= questions.size() && !questions.isEmpty()) {
                    currentPosition = questions.size() - 1;
                }
                displayCurrentQuestion();
            });
            pause.play();
            return;
        }

        optionsContainer.setDisable(true);
        showFeedback(isCorrect, question);
        updateFavoriteButton(question);

        if (questionNavPanelController != null) {
            questionNavPanelController.updateQuestionState(question);
        }

        // 只要回答正确，就自动进入下一题 (多选、单选、判断都一样)
        if (isCorrect) {
            if (currentPosition < questions.size() - 1) {
                // 添加0.4秒延迟后跳转
                PauseTransition pause = new PauseTransition(Duration.seconds(0.4));
                pause.setOnFinished(event -> handleNext());
                pause.play();
            }
        }
    }

    private String getUserAnswer() {
        Question question = questions.get(currentPosition);
        boolean isMultipleChoice = "多选题".equals(question.getType());
        boolean isTrueFalse = "判断题".equals(question.getType());

        if (isMultipleChoice) {
            StringBuilder answer = new StringBuilder();
            for (Node node : optionsContainer.getChildren()) {
                if (node instanceof CheckBox) {
                    CheckBox cb = (CheckBox) node;
                    if (cb.isSelected()) {
                        answer.append(cb.getText().charAt(0));
                    }
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
                
                // 判断题特殊处理：如果数据库中存储的是"正确"/"错误"，但界面显示的是"A. 正确"/"B. 错误"
                if (isTrueFalse) {
                    if (optionText.contains("正确")) {
                        return "A";  // 或者根据实际存储格式返回"正确"
                    } else if (optionText.contains("错误")) {
                        return "B";  // 或者根据实际存储格式返回"错误"
                    }
                }
                
                return String.valueOf(optionText.charAt(0));
            }
        }
        return null;
    }

    private void restoreAnswer(String answer, boolean isMultipleChoice) {
        if (isMultipleChoice) {
            for (Node node : optionsContainer.getChildren()) {
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

    private void showFeedback(boolean isCorrect, Question question) {
        // 在练习模式下，显示对错反馈
        if (!isStudyMode) {
            feedbackPane.setVisible(true);
            feedbackPane.setManaged(true);
            feedbackLabel.getStyleClass().removeAll("feedback-correct", "feedback-wrong");
            if (isCorrect) {
                feedbackLabel.setText("✓ 正确!");
                feedbackLabel.getStyleClass().add("feedback-correct");
            } else {
                feedbackLabel.setText("✗ 错误! 正确答案是: " + question.getFormattedAnswer());
                feedbackLabel.getStyleClass().add("feedback-wrong");
            }
        }

        // 高亮选项
        highlightOptions(question, getUserAnswer());

        // 总是显示解析（如果存在）
        if (question.getExplanation() != null && !question.getExplanation().isEmpty()) {
            explanationPane.setVisible(true);
            explanationPane.setManaged(true);
            explanationLabel.setText(question.getExplanation());
        } else {
            explanationPane.setVisible(false);
            explanationPane.setManaged(false);
        }
    }

    private void filterAndReloadQuestions(String typeFilter) {
        if (subject == null) return;

        List<Question> filteredBase = baseQuestions.stream()
            .filter(q -> "全部".equals(typeFilter) || typeFilter.equals(q.getType()))
            .collect(Collectors.toList());
        
        questions = questionManager.getClonedQuestions(filteredBase);
        Collections.shuffle(questions);
        currentPosition = 0;

        if (questions.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "提示", "该类型下没有题目。");
            // 清空题目区域
            questionTextLabel.setText("该类型下没有题目。");
            optionsContainer.getChildren().clear();
            progressLabel.setText("进度: 0/0");
        } else {
            displayCurrentQuestion();
        }

        // 即使题目为空，也要更新侧边栏以反映正确的状态
        updateSidebar();
        if (questionNavPanelController != null) {
            questionNavPanelController.updateFilterSelection(typeFilter);
        }
    }

    private void jumpToQuestionByObject(Question question) {
        int targetPosition = -1;
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).getId().equals(question.getId())) {
                targetPosition = i;
                break;
            }
        }

        if (targetPosition != -1 && targetPosition != currentPosition) {
            currentPosition = targetPosition;
            saveProgress();
            displayCurrentQuestion();
            if (questionNavPanelController != null) {
                questionNavPanelController.updateSelection(questions.get(currentPosition));
            }
        }
    }

    private void updateFavoriteButton(Question question) {
        if (isWrongQuestionMode) {
            favoriteButton.setText("✓ 取消星标");
        } else {
            if (question.isWrong()) {
                favoriteButton.setText("★ 已星标");
            } else {
                favoriteButton.setText("☆ 星标");
            }
        }
    }

    private int getOriginalQuestionIndex(int position) {
        if (subject == null || subject.getQuestions() == null ||
            questions == null || position < 0 || position >= questions.size()) {
            return -1;
        }
        Question question = questions.get(position);
        
        List<Question> originalQuestions = questionManager.getSubject(subject.getId()).getQuestions();
        if (originalQuestions == null) return -1;
        
        for (int i = 0; i < originalQuestions.size(); i++) {
            if (originalQuestions.get(i).getId() == question.getId()) {
                return i;
            }
        }
        return -1;
    }

    private void saveProgress() {
        if (isRandomMode || isWrongQuestionMode) {
            return; // 随机模式和错题回顾不保存进度
        }
        if (subject == null) {
            return;
        }

        int originalIndex = getOriginalQuestionIndex(currentPosition);
        if (originalIndex == -1) {
            return;
        }

        if (isStudyMode) {
            questionManager.updateReviewProgress(subject.getId(), originalIndex);
        } else if ("sequential".equals(mode)) {
            questionManager.updateSequentialProgress(subject.getId(), originalIndex);
        }
    }

    // ==================== 事件处理 ====================

    @FXML
    private void handleBack() {
        saveProgress();
        LogManager.info("User clicked 'Back' from practice screen. Mode: " + mode + ", Subject: " + subject.getDisplayName());
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
    }

    @FXML
    private void handlePrevious() {
        if (currentPosition > 0) {
            LogManager.info("User clicked 'Previous' button. From question index: " + currentPosition);
            saveProgress();
            currentPosition--;
            displayCurrentQuestion();
            if (questionNavPanelController != null) {
                questionNavPanelController.updateSelection(questions.get(currentPosition));
            }
        }
    }

    @FXML
    private void handleNext() {
        if (currentPosition < questions.size() - 1) {
            LogManager.info("User clicked 'Next' button. From question index: " + currentPosition);
            saveProgress();
            currentPosition++;
            displayCurrentQuestion();
            if (questionNavPanelController != null) {
                questionNavPanelController.updateSelection(questions.get(currentPosition));
            }
        } else {
            showAlert(Alert.AlertType.INFORMATION, "完成", "所有题目已练习完毕！");
            handleBack();
        }
    }

    @FXML
    private void handleSubmit() {
        LogManager.info("User clicked 'Submit' for multiple choice question. Question ID: " + questions.get(currentPosition).getId());
        evaluateAnswer();
    }

    @FXML
    private void handleAIAssist() {
        if (questions == null || questions.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "AI助手", "请先加载题目。");
            return;
        }
    
        Question question = questions.get(currentPosition);
        LogManager.info("User clicked 'AI Assist' button for question ID: " + question.getId());
        
        boolean isVisible = aiPanelContainer.isVisible();
        aiPanelContainer.setVisible(!isVisible);
        aiPanelContainer.setManaged(!isVisible);
        
        if (!isVisible) {
            mainSplitPane.setDividerPositions(0.25, 0.6);
            if (aiPanelController != null) {
                aiPanelController.analyzeQuestion(question);
            }
        } else {
            mainSplitPane.setDividerPositions(0.25, 1.0);
        }
    }

    @FXML
    private void handleFavorite() {
        if (questions == null || questions.isEmpty() || subject == null) return;

        Question question = questions.get(currentPosition);
        LogManager.info("User clicked 'Favorite' button for question ID: " + question.getId() + ". Current state: " + (question.isWrong() ? "Favorited" : "Not favorited"));

        int originalIndex = getOriginalQuestionIndex(currentPosition);
        if (originalIndex < 0) return;

        Question questionInCurrentList = questions.get(currentPosition);
        boolean isCurrentlyWrong = questionInCurrentList.isWrong();

        if (isWrongQuestionMode) {
            // 在错题模式下，此按钮的功能是“移除”
            questionManager.removeWrongQuestion(subject.getId(), originalIndex);
            questionInCurrentList.setWrong(false);
            showAlert(Alert.AlertType.INFORMATION, "提示", "已取消星标");
            // 从列表中移除并转到下一题
            questions.remove(currentPosition);
            if (currentPosition >= questions.size() && !questions.isEmpty()) {
                currentPosition = questions.size() - 1;
            }
            displayCurrentQuestion();
        } else {
            if (isCurrentlyWrong) {
                questionManager.removeWrongQuestion(subject.getId(), originalIndex);
                questionInCurrentList.setWrong(false);
                showAlert(Alert.AlertType.INFORMATION, "提示", "已取消星标");
            } else {
                questionManager.addWrongQuestion(subject.getId(), originalIndex);
                questionInCurrentList.setWrong(true);
                showAlert(Alert.AlertType.INFORMATION, "提示", "已星标");
            }
            updateFavoriteButton(questionInCurrentList);
        }
        
        // 同步原始列表中的状态
        List<Question> originalQuestions = questionManager.getSubject(subject.getId()).getQuestions();
        if (originalQuestions != null && originalIndex < originalQuestions.size()) {
            originalQuestions.get(originalIndex).setWrong(questionInCurrentList.isWrong());
        }

        if (questionNavPanelController != null) {
            questionNavPanelController.updateQuestionState(questionInCurrentList);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * 根据答案高亮选项
     * @param question 当前问题
     * @param userAnswer 用户的答案
     */
    private void highlightOptions(Question question, String userAnswer) {
        String correctAnswer = question.getAnswer();
        boolean isTrueFalse = "判断题".equals(question.getType());

        for (Node node : optionsContainer.getChildren()) {
            if (!(node instanceof Labeled)) continue;

            Labeled optionControl = (Labeled) node;
            String optionText = optionControl.getText();
            if (optionText == null || optionText.isEmpty()) continue;
            
            String optionLetter = String.valueOf(optionText.charAt(0));

            // 清理旧样式
            optionControl.getStyleClass().removeAll("option-correct", "option-wrong");

            boolean isCorrectOption = false;
            boolean isUserOption = false;

            if (isTrueFalse) {
                // 判断题：需要处理"正确"/"错误"和"A"/"B"的对应关系
                if (optionText.contains("正确") && (correctAnswer.equals("A") || correctAnswer.equals("正确"))) {
                    isCorrectOption = true;
                } else if (optionText.contains("错误") && (correctAnswer.equals("B") || correctAnswer.equals("错误"))) {
                    isCorrectOption = true;
                }
                
                if (userAnswer != null) {
                    if (optionText.contains("正确") && (userAnswer.equals("A") || userAnswer.equals("正确"))) {
                        isUserOption = true;
                    } else if (optionText.contains("错误") && (userAnswer.equals("B") || userAnswer.equals("错误"))) {
                        isUserOption = true;
                    }
                }
            } else {
                // 单选题和多选题：直接比较字母
                isCorrectOption = correctAnswer.contains(optionLetter);
                isUserOption = userAnswer != null && userAnswer.contains(optionLetter);
            }

            // 如果是正确答案，应用正确样式
            if (isCorrectOption) {
                optionControl.getStyleClass().add("option-correct");
            }
            // 如果是用户选择的错误答案，应用错误样式
            else if (isUserOption) {
                optionControl.getStyleClass().add("option-wrong");
            }
        }
    }
}