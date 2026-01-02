package com.examapp.controller;

import com.examapp.Main;
import com.examapp.data.HitokotoManager;
import com.examapp.data.QuestionManager;
import com.examapp.model.Subject;
import com.examapp.util.IconHelper;
import com.examapp.util.LogManager;
import com.examapp.service.SyncServer;
import com.examapp.service.SyncServerCallback;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainController implements SyncServerCallback {

    @FXML private Label hitokotoText;
    @FXML private Hyperlink questionBankLink;
    @FXML private Button importButton;
    @FXML private ToggleButton syncToggleButton;
    @FXML private Label syncStatusLabel;
    @FXML private Button apiSettingsButton;
    @FXML private Button aboutButton;
    @FXML private Button studyModeButton;
    @FXML private VBox subjectContainer;
    @FXML private SplitPane contentSplitPane;
    @FXML private VBox aiPanelContainer;

    private Timer hitokotoTimer;
    private Subject selectedSubject = null;
    private HBox selectedButton = null;
    private SyncServer syncServer;
    private Alert authCodeAlert; // To hold the reference to the auth code dialog

    @FXML
    private void initialize() {
        loadHitokoto();
        startHitokotoRefresh();
        setupButtonIcons();
        loadSubjects();
        setupSyncServer();
    }

    @FXML
    private void openImport() {
        LogManager.info("User clicked 'Import' button.");
        try {
            Main.switchScene("/fxml/import.fxml", "导入题库");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法打开导入界面");
        }
    }

    @FXML
    private void openAISettings() {
        LogManager.info("User clicked 'AI Settings' button.");
        try {
            Main.switchScene("/fxml/ai-settings.fxml", "API接口设置");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法打开API设置界面");
        }
    }

    @FXML
    private void openAbout() {
        LogManager.info("User clicked 'About' button.");
        try {
            Main.switchScene("/fxml/about.fxml", "关于");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法打开关于界面");
        }
    }

    @FXML
    private void openStudyMode() {
        if (selectedSubject == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先选择一个题库");
            return;
        }
        
        LogManager.info("User clicked 'Study Mode' button for subject: " + selectedSubject.getName());
        try {
            Main.switchSceneWithData("/fxml/study-mode.fxml", selectedSubject.getDisplayName(), controller -> {
                if (controller instanceof StudyModeController) {
                    ((StudyModeController) controller).setSubject(selectedSubject);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法打开学习模式界面");
        }
    }
    

    @FXML
    private void openQuestionBankLink() {
        LogManager.info("User clicked 'Question Bank Link'.");
        try {
            String url = "https://github.com/keggin-CHN/njfu_grinding/tree/main/%E9%A2%98%E5%BA%93%E6%94%B6%E9%9B%86";
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法打开链接");
        }
    }

    @FXML
    private void refreshHitokoto() {
        LogManager.info("User manually refreshed Hitokoto.");
        loadHitokoto();
    }
    
    private void loadHitokoto() {
        hitokotoText.setText("加载中...");
        new Thread(() -> {
            String hitokoto = HitokotoManager.getHitokoto();
            Platform.runLater(() -> hitokotoText.setText(hitokoto));
        }).start();
    }

    private void startHitokotoRefresh() {
        stopHitokotoRefresh();
        hitokotoTimer = new Timer(true);
        hitokotoTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> loadHitokoto());
            }
        }, 30 * 60 * 1000L, 30 * 60 * 1000L);
    }

    private void stopHitokotoRefresh() {
        if (hitokotoTimer != null) {
            hitokotoTimer.cancel();
            hitokotoTimer = null;
        }
    }

    private void setupButtonIcons() {
        // Icons for sidebar
        if (importButton != null) importButton.setGraphic(IconHelper.getImportIcon(24));
        if (apiSettingsButton != null) apiSettingsButton.setGraphic(IconHelper.getApiSettingsIcon(24));
        if (aboutButton != null) aboutButton.setGraphic(IconHelper.getAboutIcon(24));
        
        // Icons for bottom bar
        if (studyModeButton != null) studyModeButton.setGraphic(IconHelper.getCheckIcon(24));
    }

    private void loadSubjects() {
        subjectContainer.getChildren().clear();
        List<Subject> subjects = QuestionManager.getInstance().getSubjects();

        if (subjects.isEmpty()) {
            Label placeholder = new Label("题库为空，请先导入题库");
            placeholder.setStyle("-fx-font-size: 1.2em; -fx-text-fill: #888;");
            subjectContainer.getChildren().add(placeholder);
            return;
        }

        for (Subject subject : subjects) {
            HBox subjectCard = createSubjectCard(subject);
            subjectContainer.getChildren().add(subjectCard);
        }
    }

    private HBox createSubjectCard(Subject subject) {
        // 主容器
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("subject-card-button");
        card.setStyle("-fx-padding: 15; -fx-cursor: hand;");
        card.setPrefHeight(80);
        card.setMaxWidth(Double.MAX_VALUE);
        
        // 左侧信息区域
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);
        
        Label nameLabel = new Label(subject.getName());
        nameLabel.setWrapText(true);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 1.3em;");
        
        Label countLabel = new Label(subject.getQuestions().size() + " 题");
        countLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 1.1em;");
        
        infoBox.getChildren().addAll(nameLabel, countLabel);
        
        // 右侧操作按钮区域
        HBox actionBox = new HBox(5);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        
        // 重命名按钮
        Button renameButton = new Button("重命名");
        renameButton.getStyleClass().add("secondary-button");
        renameButton.setStyle("-fx-font-size: 0.9em; -fx-padding: 5 10;");
        renameButton.setOnAction(e -> {
            e.consume(); // 阻止事件冒泡
            renameSubject(subject);
        });
        
        // 删除按钮
        Button deleteButton = new Button("删除");
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setStyle("-fx-font-size: 0.9em; -fx-padding: 5 10;");
        deleteButton.setOnAction(e -> {
            e.consume(); // 阻止事件冒泡
            deleteSubject(subject);
        });
        
        actionBox.getChildren().addAll(renameButton, deleteButton);
        
        card.getChildren().addAll(infoBox, actionBox);
        
        // 点击卡片选中
        card.setOnMouseClicked(e -> {
            if (e.getTarget() == card || e.getTarget() == infoBox ||
                e.getTarget() == nameLabel || e.getTarget() == countLabel) {
                selectSubject(subject, card);
            }
        });
        
        return card;
    }

    private void renameSubject(Subject subject) {
        TextInputDialog dialog = new TextInputDialog(subject.getName());
        dialog.setTitle("重命名题库");
        dialog.setHeaderText("请输入新的题库名称");
        dialog.setContentText("名称:");
        
        dialog.showAndWait().ifPresent(newName -> {
            if (newName != null && !newName.trim().isEmpty()) {
                subject.setName(newName.trim());
                QuestionManager qm = QuestionManager.getInstance();
                // 保存更改
                try {
                    java.lang.reflect.Method saveMethod = qm.getClass().getDeclaredMethod("saveSubjects");
                    saveMethod.setAccessible(true);
                    saveMethod.invoke(qm);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                loadSubjects();
                LogManager.info("User renamed subject to: " + newName);
            }
        });
    }

    private void deleteSubject(Subject subject) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("确定要删除题库 \"" + subject.getName() + "\" 吗？");
        alert.setContentText("此操作不可撤销！");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                QuestionManager.getInstance().deleteSubject(subject.getId());
                if (selectedSubject != null && selectedSubject.getId().equals(subject.getId())) {
                    selectedSubject = null;
                    selectedButton = null;
                }
                loadSubjects();
                LogManager.info("User deleted subject: " + subject.getName());
            }
        });
    }

    private void selectSubject(Subject subject, HBox card) {
        LogManager.info("User selected subject card: " + subject.getName());
        
        // 取消之前选中的卡片样式
        if (selectedButton != null) {
            selectedButton.setStyle("-fx-padding: 15; -fx-cursor: hand;");
        }
        
        // 设置新选中的卡片样式
        selectedSubject = subject;
        selectedButton = card;
        card.setStyle("-fx-padding: 15; -fx-cursor: hand; -fx-border-color: #1976D2; -fx-border-width: 3px; -fx-background-color: #E3F2FD;");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void setupSyncServer() {
        syncServer = new SyncServer(this);
        syncToggleButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                syncServer.start();
            } else {
                syncServer.stop();
            }
        });

        // Set icon for toggle button
        syncToggleButton.setGraphic(IconHelper.getSyncIcon(24));
    }

    @Override
    public void onServerStarted(String ipAddress, int port) {
        Platform.runLater(() -> {
            syncStatusLabel.setText("服务运行中，等待连接...\nIP: " + ipAddress + "\n端口: " + port);
            syncToggleButton.setText("停止同步服务");
        });
    }

    @Override
    public void onServerStopped() {
        Platform.runLater(() -> {
            if (authCodeAlert != null && authCodeAlert.isShowing()) {
                authCodeAlert.close();
            }
            syncStatusLabel.setText("服务已停止");
            syncToggleButton.setSelected(false);
            syncToggleButton.setText("开启同步服务");
        });
    }

    @Override
    public void onClientConnected() {
        Platform.runLater(() -> syncStatusLabel.setText("客户端已连接，正在同步..."));
    }

    @Override
    public void onClientDisconnected() {
         Platform.runLater(() -> {
            // If server is still running, revert to waiting message
            if (syncServer.isRunning()) {
                onServerStarted(syncServer.getIpAddress(), syncServer.getPort());
            }
        });
    }

    @Override
    public void onAuthCodeGenerated(String authCode) {
        Platform.runLater(() -> {
            authCodeAlert = new Alert(Alert.AlertType.INFORMATION);
            authCodeAlert.setTitle("同步授权");
            authCodeAlert.setHeaderText("请在手机端输入以下授权码以开始同步：");
            
            // Use a TextField in a VBox to make the code selectable
            VBox box = new VBox();
            TextField codeField = new TextField(authCode);
            codeField.setEditable(false);
            codeField.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold; -fx-alignment: center;");
            box.getChildren().add(codeField);
            box.setAlignment(Pos.CENTER);
            
            authCodeAlert.getDialogPane().setContent(box);
            authCodeAlert.show(); // Show and don't wait, as the process continues
            syncStatusLabel.setText("已生成授权码，等待客户端输入...");
        });
    }

    @Override
    public void onSyncProgress(String message) {
        Platform.runLater(() -> syncStatusLabel.setText(message));
    }

    @Override
    public void onSyncCompleted() {
        Platform.runLater(() -> {
            if (authCodeAlert != null && authCodeAlert.isShowing()) {
                authCodeAlert.close();
            }
            showAlert(Alert.AlertType.INFORMATION, "同步成功", "数据同步已成功完成！");
            loadSubjects(); // Refresh the subject list
            
            // Stop the server and immediately update the UI
            if (syncServer != null && syncServer.isRunning()) {
                syncServer.stop(); // This will run in the background
            }
            // Manually call onServerStopped to update UI immediately
            onServerStopped();
        });
    }

    @Override
    public void onError(String errorMessage) {
        Platform.runLater(() -> {
            showAlert(Alert.AlertType.ERROR, "同步错误", errorMessage);
            // Stop the server on critical errors
            if (syncServer.isRunning()) {
                syncServer.stop();
            }
        });
    }

    public void cleanup() {
        stopHitokotoRefresh();
        if (syncServer != null) {
            syncServer.stop();
        }
    }
}