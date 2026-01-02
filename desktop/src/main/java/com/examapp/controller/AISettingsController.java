package com.examapp.controller;

import com.examapp.Main;
import com.examapp.data.AISettingsManager;
import com.examapp.service.AIService;
import com.examapp.util.IconHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * AI设置界面控制器
 */
public class AISettingsController implements Initializable {

    @FXML private Button backButton;
    @FXML private TextField baseUrlField;
    @FXML private Label fullPathLabel;
    @FXML private PasswordField apiKeyField;
    @FXML private ComboBox<String> modelComboBox;
    @FXML private Button refreshModelsButton;
    @FXML private Slider temperatureSlider;
    @FXML private Label temperatureValueLabel;
    @FXML private Slider topPSlider;
    @FXML private Label topPValueLabel;
    @FXML private Slider maxRetrySlider;
    @FXML private Label maxRetryValueLabel;
    @FXML private Button resetButton;
    @FXML private Button saveButton;

    private AISettingsManager aiSettingsManager;
    private AIService aiService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        aiSettingsManager = AISettingsManager.getInstance();
        aiService = AIService.getInstance();
        
        setupIcons();

        setupListeners();
        loadSettings();
    }
    
    private void setupIcons() {
        if (backButton != null) {
            backButton.setGraphic(IconHelper.getBackIcon(16));
        }
        if (refreshModelsButton != null) {
            refreshModelsButton.setGraphic(IconHelper.getSyncIcon(16));
        }
        if (resetButton != null) {
            resetButton.setGraphic(IconHelper.getBackgroundIcon(16));
        }
        if (saveButton != null) {
            saveButton.setGraphic(IconHelper.getCheckIcon(16));
        }
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        // Base URL变化时更新完整路径显示
        baseUrlField.textProperty().addListener((obs, oldVal, newVal) -> updateFullPath());

        // Temperature滑块
        temperatureSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            temperatureValueLabel.setText(String.format("%.2f", newVal.doubleValue()));
        });

        // Top-P滑块
        topPSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            topPValueLabel.setText(String.format("%.2f", newVal.doubleValue()));
        });

        // Max Retry滑块
        maxRetrySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            maxRetryValueLabel.setText(String.valueOf(newVal.intValue()));
        });
    }

    /**
     * 加载设置
     */
    private void loadSettings() {
        baseUrlField.setText(aiSettingsManager.getBaseUrl());
        apiKeyField.setText(aiSettingsManager.getApiKey());

        double temperature = aiSettingsManager.getTemperature();
        temperatureSlider.setValue(temperature);
        temperatureValueLabel.setText(String.format("%.2f", temperature));

        float topP = aiSettingsManager.getTopP();
        topPSlider.setValue(topP);
        topPValueLabel.setText(String.format("%.2f", topP));

        int maxRetry = aiSettingsManager.getMaxRetry();
        maxRetrySlider.setValue(maxRetry);
        maxRetryValueLabel.setText(String.valueOf(maxRetry));

        updateFullPath();

        // 加载已保存的模型
        String savedModel = aiSettingsManager.getModel();
        if (!savedModel.isEmpty()) {
            modelComboBox.getItems().add(savedModel);
            modelComboBox.setValue(savedModel);
        }
    }

    /**
     * 更新完整API路径显示
     */
    private void updateFullPath() {
        String baseUrl = baseUrlField.getText().trim();
        if (baseUrl.isEmpty()) {
            fullPathLabel.setText("完整API路径: ");
            return;
        }

        if (baseUrl.endsWith("#")) {
            String customUrl = baseUrl.substring(0, baseUrl.length() - 1);
            fullPathLabel.setText("完整API路径: " + customUrl + " (自定义完整路径)");
        } else {
            String displayUrl = baseUrl;
            if (displayUrl.endsWith("/")) {
                displayUrl = displayUrl.substring(0, displayUrl.length() - 1);
            }
            fullPathLabel.setText("完整API路径: " + displayUrl + "/v1/chat/completions");
        }
    }

    /**
     * 刷新模型列表
     */
    @FXML
    private void handleRefreshModels() {
        String baseUrl = baseUrlField.getText().trim();
        String apiKey = apiKeyField.getText().trim();

        if (baseUrl.isEmpty() || apiKey.isEmpty()) {
            showAlert(AlertType.WARNING, "提示", "请先填写Base URL和API Key");
            return;
        }

        // 临时保存以便AIService可以使用
        aiSettingsManager.setBaseUrl(baseUrl);
        aiSettingsManager.setApiKey(apiKey);

        // 显示加载提示
        refreshModelsButton.setDisable(true);
        refreshModelsButton.setText("加载中...");

        // 在后台线程获取模型列表
        new Thread(() -> {
            aiService.fetchModels(new AIService.ModelsCallback() {
                @Override
                public void onSuccess(List<String> models) {
                    Platform.runLater(() -> {
                        refreshModelsButton.setDisable(false);
                        refreshModelsButton.setText("刷新模型列表");

                        modelComboBox.getItems().clear();
                        modelComboBox.getItems().addAll(models);

                        // 如果之前有选择的模型，尝试恢复
                        String savedModel = aiSettingsManager.getModel();
                        if (!savedModel.isEmpty() && models.contains(savedModel)) {
                            modelComboBox.setValue(savedModel);
                        } else if (!models.isEmpty()) {
                            modelComboBox.setValue(models.get(0));
                        }

                        showAlert(AlertType.INFORMATION, "成功", "获取到 " + models.size() + " 个模型");
                    });
                }

                @Override
                public void onError(String error) {
                    Platform.runLater(() -> {
                        refreshModelsButton.setDisable(false);
                        refreshModelsButton.setText("刷新模型列表");
                        showAlert(AlertType.ERROR, "错误", error);
                    });
                }
            });
        }).start();
    }

    /**
     * 恢复默认值
     */
    @FXML
    private void handleReset() {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("确认");
        confirm.setHeaderText("恢复默认参数");
        confirm.setContentText("确定要将Temperature和Top-P恢复为默认值吗？");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                aiSettingsManager.resetParametersToDefault();

                double temperature = aiSettingsManager.getTemperature();
                temperatureSlider.setValue(temperature);
                temperatureValueLabel.setText(String.format("%.2f", temperature));

                float topP = aiSettingsManager.getTopP();
                topPSlider.setValue(topP);
                topPValueLabel.setText(String.format("%.2f", topP));

                showAlert(AlertType.INFORMATION, "成功", "已恢复默认值: Temperature=0.7, Top-P=0.9");
            }
        });
    }

    /**
     * 保存设置
     */
    @FXML
    private void handleSave() {
        aiSettingsManager.setBaseUrl(baseUrlField.getText().trim());
        aiSettingsManager.setApiKey(apiKeyField.getText().trim());

        if (modelComboBox.getValue() != null) {
            aiSettingsManager.setModel(modelComboBox.getValue());
        }

        aiSettingsManager.setTemperature(temperatureSlider.getValue());
        aiSettingsManager.setTopP((float) topPSlider.getValue());
        aiSettingsManager.setMaxRetry((int) maxRetrySlider.getValue());

        showAlert(AlertType.INFORMATION, "成功", "AI设置已保存");
        handleBack();
    }

    /**
     * 返回主界面
     */
    @FXML
    private void handleBack() {
        try {
            Main.switchScene("/fxml/main.fxml", "NJFU刷题助手");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "错误", "无法返回主界面: " + e.getMessage());
        }
    }

    /**
     * 显示提示对话框
     */
    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}