package com.examapp.controller;

import com.examapp.Main;
import com.examapp.model.Subject;
import com.examapp.util.IconHelper;
import com.examapp.util.LogManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import java.io.IOException;

public class StudyModeController {

    @FXML private Label titleLabel;
    @FXML private VBox studyModeCard;
    @FXML private VBox sequentialPracticeCard;
    @FXML private VBox randomPracticeCard;
    @FXML private VBox endlessModeCard;
    @FXML private VBox wrongReviewCard;
    @FXML private VBox mockExamCard;
    @FXML private VBox wrongQuestionsCard;
    @FXML private VBox searchCard;
    @FXML private VBox examHistoryCard;
    @FXML private VBox wrongAnalysisCard;

    private Subject subject;

    public void setSubject(Subject subject) {
        this.subject = subject;
        if (subject != null) {
            titleLabel.setText(subject.getDisplayName());
        }
    }

    @FXML
    private void initialize() {
        // 设置卡片图标
        setupCardIcons();
    }

    private void setupCardIcons() {
        // 为每个卡片添加图标，大小为32px
        if (studyModeCard != null) {
            studyModeCard.getChildren().add(0, IconHelper.getStudyModeIcon(32));
        }
        if (sequentialPracticeCard != null) {
            sequentialPracticeCard.getChildren().add(0, IconHelper.getCheckIcon(32));
        }
        if (randomPracticeCard != null) {
            randomPracticeCard.getChildren().add(0, IconHelper.getSyncIcon(32));
        }
        if (endlessModeCard != null) {
            endlessModeCard.getChildren().add(0, IconHelper.getEndlessModeIcon(32));
        }
        if (wrongReviewCard != null) {
            wrongReviewCard.getChildren().add(0, IconHelper.getWrongReviewIcon(32));
        }
        if (mockExamCard != null) {
            mockExamCard.getChildren().add(0, IconHelper.getMockExamIcon(32));
        }
        if (wrongQuestionsCard != null) {
            wrongQuestionsCard.getChildren().add(0, IconHelper.getWrongQuestionsIcon(32));
        }
        if (searchCard != null) {
            searchCard.getChildren().add(0, IconHelper.getSearchIcon(32));
        }
        if (examHistoryCard != null) {
            examHistoryCard.getChildren().add(0, IconHelper.getHistoryIcon(32));
        }
        if (wrongAnalysisCard != null) {
            wrongAnalysisCard.getChildren().add(0, IconHelper.getWrongAnalysisIcon(32));
        }
    }

    private void ensureSubjectInitialized() {
        if (this.subject == null) {
            this.subject = new Subject("all", "全部题库");
            if (titleLabel != null) {
                titleLabel.setText(this.subject.getDisplayName());
            }
        }
    }
    
    @FXML
    private void handleBack() {
        LogManager.info("User clicked 'Back' from Study Mode screen for subject: " + (subject != null ? subject.getDisplayName() : "N/A"));
        try {
            Main.switchScene("/fxml/main.fxml", "主菜单");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void startStudyMode() {
        ensureSubjectInitialized();
        LogManager.info("User started 'Study Mode' for subject: " + subject.getDisplayName());
        try {
            Main.switchSceneWithData("/fxml/practice.fxml", "背题模式 - " + subject.getDisplayName(), controller -> {
                if (controller instanceof PracticeController) {
                    PracticeController pc = (PracticeController) controller;
                    pc.setSubject(subject);
                    pc.setStudyMode(true);
                    pc.start();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void startSequentialPractice() {
        startPractice(false);
    }

    @FXML
    private void startRandomPractice() {
        startPractice(true);
    }
    
    private void startPractice(boolean isRandom) {
        ensureSubjectInitialized();
        String mode = isRandom ? "随机练习" : "顺序练习";
        LogManager.info("User started '" + mode + "' for subject: " + subject.getDisplayName());
        try {
            Main.switchSceneWithData("/fxml/practice.fxml", mode + " - " + subject.getDisplayName(), controller -> {
                if (controller instanceof PracticeController) {
                    PracticeController pc = (PracticeController) controller;
                    pc.setSubject(subject);
                    pc.setMode(isRandom);
                    pc.start();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void startEndlessMode() {
        ensureSubjectInitialized();
        LogManager.info("User started 'Endless Mode' for subject: " + subject.getDisplayName());
        try {
            Main.switchSceneWithData("/fxml/endless-mode.fxml", "无尽模式 - " + subject.getDisplayName(), controller -> {
                if (controller instanceof EndlessModeController) {
                    ((EndlessModeController) controller).setSubject(subject);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void startWrongReview() {
        ensureSubjectInitialized();
        LogManager.info("User started 'Wrong Question Review' for subject: " + subject.getDisplayName());
        try {
            Main.switchSceneWithData("/fxml/practice.fxml", "错题回顾 - " + subject.getDisplayName(), controller -> {
                if (controller instanceof PracticeController) {
                    // 需要一个方法来加载错题
                    // ((PracticeController) controller).loadWrongQuestions(subject);
                    // 暂时先用一个标志位
                    PracticeController pc = (PracticeController) controller;
                    pc.setSubject(subject);
                    pc.setWrongQuestionMode(true);
                    pc.start();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void startMockExam() {
        ensureSubjectInitialized();
        LogManager.info("User started 'Mock Exam' for subject: " + subject.getDisplayName());
        try {
            Main.switchSceneWithData("/fxml/mock-exam.fxml", "模拟考试 - " + subject.getDisplayName(), controller -> {
                if (controller instanceof MockExamController) {
                    ((MockExamController) controller).setSubject(subject);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void startWrongQuestions() {
        ensureSubjectInitialized();
        LogManager.info("User started 'Wrong Questions' for subject: " + subject.getDisplayName());
        try {
            Main.switchSceneWithData("/fxml/wrong-questions.fxml", "错题本 - " + subject.getDisplayName(), controller -> {
                if (controller instanceof WrongQuestionsController) {
                    ((WrongQuestionsController) controller).setSubject(subject);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void startSearch() {
        ensureSubjectInitialized();
        LogManager.info("User started 'Search' for subject: " + subject.getDisplayName());
        try {
            Main.switchSceneWithData("/fxml/search.fxml", "题库内搜索 - " + subject.getDisplayName(), controller -> {
                if (controller instanceof SearchController) {
                    ((SearchController) controller).setSubject(subject);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void startHistory() {
        ensureSubjectInitialized();
        LogManager.info("User started 'History' for subject: " + subject.getDisplayName());
        try {
            Main.switchSceneWithData("/fxml/history.fxml", "考试历史 - " + subject.getDisplayName(), controller -> {
                if (controller instanceof HistoryController) {
                    ((HistoryController) controller).setSubject(subject);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void startWrongAnalysis() {
        // This feature is now integrated into the new WrongQuestions screen.
        // The card's on-click event should be removed from the FXML or commented out.
        // Keeping the method for now in case it's needed elsewhere.
        ensureSubjectInitialized();
        LogManager.info("User clicked 'Wrong Analysis', redirecting to new 'Wrong Questions' screen for subject: " + subject.getDisplayName());
        startWrongQuestions(); // Redirect to the new unified wrong questions screen
    }
}