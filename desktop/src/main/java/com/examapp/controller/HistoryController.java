package com.examapp.controller;

import com.examapp.Main;
import com.examapp.data.QuestionManager;
import com.examapp.model.ExamHistoryEntry;
import com.examapp.util.IconHelper;
import com.examapp.util.LogManager;
import com.examapp.util.XGBoostPredictor;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class HistoryController implements Initializable {

    @FXML private ListView<ExamHistoryEntry> historyListView;
    @FXML private LineChart<String, Number> scoreChart;
    @FXML private Label predictionLabel;
    @FXML private Button backButton;
    @FXML private Button clearHistoryButton;

    private QuestionManager questionManager;
    private com.examapp.model.Subject subject;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        questionManager = QuestionManager.getInstance();
        setupListView();

        backButton.setGraphic(IconHelper.getBackIcon(16));
        backButton.setText("");
        clearHistoryButton.setGraphic(IconHelper.getDeleteIcon(16));
        clearHistoryButton.setText("清空历史");
    }

    public void setSubject(com.examapp.model.Subject subject) {
        this.subject = subject;
        loadHistory();
    }

    private void loadHistory() {
        List<ExamHistoryEntry> entries;
        if (subject != null) {
            entries = questionManager.getExamHistoryEntries(subject.getId());
        } else {
            entries = questionManager.getExamHistoryEntries();
        }
        
        // 按时间倒序排列（最新的在最上面）
        entries.sort((e1, e2) -> Long.compare(e2.getTimestamp(), e1.getTimestamp()));

        if (entries.isEmpty()) {
            historyListView.setPlaceholder(new Label("暂无考试记录"));
            predictionLabel.setText("N/A");
            scoreChart.getData().clear();
        } else {
            ObservableList<ExamHistoryEntry> observableEntries = FXCollections.observableArrayList(entries);
            historyListView.setItems(observableEntries);
            populateScoreChart(entries);
            predictNextScore(entries);
        }
    }

    private void setupListView() {
        historyListView.setCellFactory(param -> new HistoryListCell());
        historyListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                navigateToResult(newSelection);
            }
        });
    }

    private void populateScoreChart(List<ExamHistoryEntry> entries) {
        scoreChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("考试分数");

        // 反转列表，使最新的考试显示在最右边
        List<ExamHistoryEntry> reversedEntries = new ArrayList<>(entries);
        Collections.reverse(reversedEntries);
        
        for (int i = 0; i < reversedEntries.size(); i++) {
            ExamHistoryEntry entry = reversedEntries.get(i);
            String attemptNumber = String.valueOf(i + 1);
            series.getData().add(new XYChart.Data<>(attemptNumber, entry.getScore()));
        }

        scoreChart.getData().add(series);
    }

    private void predictNextScore(List<ExamHistoryEntry> entries) {
        if (entries.size() < 2) {
            predictionLabel.setText("数据不足");
            return;
        }
        // 反转列表以确保按时间顺序（从旧到新）传递给预测器
        List<ExamHistoryEntry> chronologicalEntries = new ArrayList<>(entries);
        Collections.reverse(chronologicalEntries);
        
        List<Double> scores = chronologicalEntries.stream().map(entry -> (double) entry.getScore()).collect(java.util.stream.Collectors.toList());
        double predictedScore = XGBoostPredictor.predict(scores);
        predictionLabel.setText("预期最终成绩为 " + String.format("%.1f", predictedScore) + " 分");
    }

    private void navigateToResult(ExamHistoryEntry entry) {
        LogManager.info("User clicked on history entry to view results. Subject: " + entry.getSubjectName() + ", Timestamp: " + entry.getTimestamp());
        try {
            Main.switchSceneWithData("/fxml/result.fxml", "考试结果", controller -> {
                if (controller instanceof ResultController) {
                    ((ResultController) controller).setHistoryEntry(entry);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法加载结果页面。");
        }
    }

    @FXML
    private void handleBack() {
        LogManager.info("User clicked 'Back' from history screen.");
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

    @FXML
    private void handleClearHistory() {
        LogManager.info("User clicked 'Clear History' button for subject: " + (subject != null ? subject.getDisplayName() : "All"));
        List<ExamHistoryEntry> entries;
        if (subject != null) {
            entries = questionManager.getExamHistoryEntries(subject.getId());
        } else {
            entries = questionManager.getExamHistoryEntries();
        }

        if (entries.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "提示", "没有历史记录可清空");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认清空");
        alert.setHeaderText("确定要清空所有历史记录吗？");
        alert.setContentText("此操作不可恢复！");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (subject != null) {
                questionManager.clearExamHistory(subject.getId());
            } else {
                questionManager.clearExamHistory();
            }
            loadHistory();
            showAlert(Alert.AlertType.INFORMATION, "提示", "历史记录已清空");
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
     * 自定义 ListView 单元格
     */
    private static class HistoryListCell extends ListCell<ExamHistoryEntry> {
        private final HBox card = new HBox();
        private final VBox scoreBox = new VBox(5);
        private final Label scoreLabel = new Label();
        private final VBox infoBox = new VBox(8);
        private final Label subjectNameLabel = new Label();
        private final Label dateLabel = new Label();
        private final Label deviceSourceLabel = new Label(); // 新增Label

        public HistoryListCell() {
            super();
            
            // 构建卡片UI
            card.getStyleClass().add("history-card");
            card.setAlignment(Pos.CENTER_LEFT);

            scoreBox.setAlignment(Pos.CENTER);
            scoreBox.getStyleClass().add("history-score-box");
            scoreLabel.getStyleClass().add("history-score-label");
            scoreBox.getChildren().add(scoreLabel);

            infoBox.getStyleClass().add("history-info-box");
            HBox.setHgrow(infoBox, Priority.ALWAYS);
            subjectNameLabel.getStyleClass().add("history-subject-label");
            dateLabel.getStyleClass().add("history-date-label");
            deviceSourceLabel.getStyleClass().add("history-device-label"); // 新增样式类
            infoBox.getChildren().addAll(subjectNameLabel, dateLabel, deviceSourceLabel); // 添加新Label
            
            card.getChildren().addAll(infoBox, scoreBox);
        }

        @Override
        protected void updateItem(ExamHistoryEntry entry, boolean empty) {
            super.updateItem(entry, empty);
            if (empty || entry == null) {
                setGraphic(null);
            } else {
                scoreLabel.setText(String.valueOf(entry.getScore()));
                // 将 "科目名称" 改为 "题库名称"
                subjectNameLabel.setText("题库名称: " + entry.getSubjectName());
                dateLabel.setText(entry.getFormattedDateTime());
                
                // 设置设备来源
                if (entry.getDeviceSource() != null && !entry.getDeviceSource().isEmpty()) {
                    deviceSourceLabel.setText("来自: " + entry.getDeviceSource());
                    deviceSourceLabel.setVisible(true);
                    deviceSourceLabel.setManaged(true);
                } else {
                    deviceSourceLabel.setVisible(false);
                    deviceSourceLabel.setManaged(false);
                }

                // 移除旧样式再添加新样式，防止样式叠加
                scoreBox.getStyleClass().removeAll("history-score-high", "history-score-medium", "history-score-low");
                double score = entry.getScore();
                if (score >= 90) {
                    scoreBox.getStyleClass().add("history-score-high");
                } else if (score >= 60) {
                    scoreBox.getStyleClass().add("history-score-medium");
                } else {
                    scoreBox.getStyleClass().add("history-score-low");
                }

                setGraphic(card);
            }
        }
    }
}