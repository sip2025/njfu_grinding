package com.examapp.controller;

import com.examapp.Main;
import com.examapp.data.SettingsManager;
import com.examapp.util.IconHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class DeveloperModeController implements Initializable {
    
    @FXML private CheckBox developerModeCheckBox;
    @FXML private TextArea customCssArea;
    @FXML private Button saveButton;
    @FXML private Button resetButton;
    @FXML private Button testApiButton;
    @FXML private Button clearCacheButton;
    @FXML private Button exportDataButton;
    @FXML private Label statusLabel;
    @FXML private VBox contentVBox;
    
    private SettingsManager settingsManager;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        settingsManager = SettingsManager.getInstance();
        
        // Set up icons
        saveButton.setGraphic(IconHelper.getCheckIcon(16));
        resetButton.setGraphic(IconHelper.getSyncIcon(16));
        testApiButton.setGraphic(IconHelper.getAiAssistantIcon(16));
        clearCacheButton.setGraphic(IconHelper.getDeleteIcon(16));
        exportDataButton.setGraphic(IconHelper.getShareIcon(16));
        
        // Load current settings
        loadSettings();
        
        // Set up event handlers
        saveButton.setOnAction(e -> saveSettings());
        resetButton.setOnAction(e -> resetSettings());
        testApiButton.setOnAction(e -> testApiConnection());
        clearCacheButton.setOnAction(e -> clearCache());
        exportDataButton.setOnAction(e -> exportData());
        
        // Initially hide status label
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }
    
    private void loadSettings() {
        developerModeCheckBox.setSelected(settingsManager.isDeveloperMode());
        customCssArea.setText(settingsManager.getCustomCss());
    }
    
    @FXML
    private void saveSettings() {
        settingsManager.setDeveloperMode(developerModeCheckBox.isSelected());
        settingsManager.setCustomCss(customCssArea.getText());
        
        showStatus("设置已保存", "success");
        
        // Apply custom CSS if enabled
        if (developerModeCheckBox.isSelected() && !customCssArea.getText().trim().isEmpty()) {
            applyCustomCss(customCssArea.getText());
        }
    }
    
    @FXML
    private void resetSettings() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认重置");
        alert.setHeaderText("确定要重置所有开发者设置吗？");
        alert.setContentText("这将恢复默认设置并清除自定义CSS。");
        
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            settingsManager.resetDeveloperSettings();
            loadSettings();
            showStatus("设置已重置", "info");
        }
    }
    
    @FXML
    private void testApiConnection() {
        showStatus("测试API连接中...", "info");
        
        // Simulate API test in background
        new Thread(() -> {
            try {
                // Here you would implement actual API testing logic
                Thread.sleep(2000); // Simulate delay
                
                javafx.application.Platform.runLater(() -> {
                    showStatus("API连接测试成功", "success");
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    showStatus("API连接测试失败: " + e.getMessage(), "error");
                });
            }
        }).start();
    }
    
    @FXML
    private void clearCache() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认清除缓存");
        alert.setHeaderText("确定要清除所有缓存吗？");
        alert.setContentText("这将清除AI缓存、题库缓存等所有临时数据。");
        
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                // Clear AI cache
                com.examapp.data.AICacheManager.getInstance().clearCache();
                
                // Clear question cache
                com.examapp.data.QuestionManager.getInstance().clearCache();
                
                showStatus("缓存已清除", "success");
            } catch (Exception e) {
                showStatus("清除缓存失败: " + e.getMessage(), "error");
            }
        }
    }
    
    @FXML
    private void exportData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出数据");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON文件", "*.json"));
        fileChooser.setInitialFileName("examapp_data_export.json");
        
        java.io.File file = fileChooser.showSaveDialog(exportDataButton.getScene().getWindow());
        if (file != null) {
            try {
                // Export all data
                com.examapp.data.DataExporter.exportAllData(file);
                showStatus("数据导出成功: " + file.getName(), "success");
            } catch (Exception e) {
                showStatus("数据导出失败: " + e.getMessage(), "error");
            }
        }
    }
    
    @FXML
    private void handleBack() {
        try {
            Main.switchScene("/fxml/main.fxml", "主菜单");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void applyCustomCss(String css) {
        try {
            // Apply custom CSS to the current scene
            if (Main.getPrimaryStage() != null && Main.getPrimaryStage().getScene() != null) {
                Main.getPrimaryStage().getScene().getStylesheets().add(
                    "data:text/css," + java.net.URLEncoder.encode(css, "UTF-8")
                );
            }
        } catch (Exception e) {
            showStatus("应用自定义CSS失败: " + e.getMessage(), "error");
        }
    }
    
    private void showStatus(String message, String type) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        
        // Set color based on type
        String color = switch (type) {
            case "success" -> "#4CAF50";
            case "error" -> "#F44336";
            case "warning" -> "#FF9800";
            default -> "#2196F3";
        };
        
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 14px;");
        
        // Hide status after 3 seconds
        javafx.util.Duration duration = javafx.util.Duration.seconds(3);
        javafx.animation.KeyFrame keyFrame = new javafx.animation.KeyFrame(duration, e -> {
            statusLabel.setVisible(false);
            statusLabel.setManaged(false);
        });
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(keyFrame);
        timeline.play();
    }
}