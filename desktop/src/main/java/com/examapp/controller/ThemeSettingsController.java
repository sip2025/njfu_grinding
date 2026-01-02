package com.examapp.controller;

import com.examapp.Main;
import com.examapp.data.SettingsManager;
import com.examapp.util.IconHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ThemeSettingsController implements Initializable {
    
    @FXML private ComboBox<String> themeComboBox;
    @FXML private Slider fontSizeSlider;
    @FXML private Label fontSizeLabel;
    @FXML private CheckBox autoSaveCheckBox;
    @FXML private CheckBox showExplanationCheckBox;
    @FXML private Button saveButton;
    @FXML private Button resetButton;
    @FXML private Button previewButton;
    @FXML private Label statusLabel;
    @FXML private VBox contentVBox;
    
    private SettingsManager settingsManager;
    private String originalTheme;
    private int originalFontSize;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        settingsManager = SettingsManager.getInstance();
        
        // Set up icons
        saveButton.setGraphic(IconHelper.getCheckIcon(16));
        resetButton.setGraphic(IconHelper.getSyncIcon(16));
        previewButton.setGraphic(IconHelper.getBackgroundIcon(16));
        
        // Initialize theme options
        themeComboBox.getItems().addAll("浅色主题", "深色主题", "自动跟随系统");
        
        // Set up font size slider
        fontSizeSlider.setMin(10);
        fontSizeSlider.setMax(24);
        fontSizeSlider.setBlockIncrement(1);
        fontSizeSlider.setShowTickLabels(true);
        fontSizeSlider.setShowTickMarks(true);
        fontSizeSlider.setMajorTickUnit(2);
        
        // Load current settings
        loadSettings();
        
        // Store original values for reset
        originalTheme = settingsManager.getTheme();
        originalFontSize = settingsManager.getFontSize();
        
        // Set up event handlers
        saveButton.setOnAction(e -> saveSettings());
        resetButton.setOnAction(e -> resetSettings());
        previewButton.setOnAction(e -> previewTheme());
        
        // Theme change listener
        themeComboBox.setOnAction(e -> updateFontSizeLabel());
        
        // Font size change listener
        fontSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            fontSizeLabel.setText("字体大小: " + newVal.intValue() + "px");
            updateFontSizeLabel();
        });
        
        // Initially hide status label
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }
    
    private void loadSettings() {
        // Load theme
        String theme = settingsManager.getTheme();
        switch (theme) {
            case "dark":
                themeComboBox.setValue("深色主题");
                break;
            case "auto":
                themeComboBox.setValue("自动跟随系统");
                break;
            default:
                themeComboBox.setValue("浅色主题");
                break;
        }
        
        // Load font size
        int fontSize = settingsManager.getFontSize();
        fontSizeSlider.setValue(fontSize);
        fontSizeLabel.setText("字体大小: " + fontSize + "px");
        
        // Load other settings
        autoSaveCheckBox.setSelected(settingsManager.isAutoSave());
        showExplanationCheckBox.setSelected(settingsManager.isShowExplanation());
    }
    
    private void updateFontSizeLabel() {
        int fontSize = (int) fontSizeSlider.getValue();
        fontSizeLabel.setStyle(String.format("-fx-font-size: %dpx;", fontSize));
        
        // Update preview
        if (previewButton != null) {
            previewButton.setStyle(String.format("-fx-font-size: %dpx;", fontSize));
        }
        if (saveButton != null) {
            saveButton.setStyle(String.format("-fx-font-size: %dpx;", fontSize));
        }
    }
    
    @FXML
    private void saveSettings() {
        // Save theme
        String selectedTheme = themeComboBox.getValue();
        String themeValue = switch (selectedTheme) {
            case "深色主题" -> "dark";
            case "自动跟随系统" -> "auto";
            default -> "light";
        };
        settingsManager.setTheme(themeValue);
        
        // Save font size
        int fontSize = (int) fontSizeSlider.getValue();
        settingsManager.setFontSize(fontSize);
        
        // Save other settings
        settingsManager.setAutoSave(autoSaveCheckBox.isSelected());
        settingsManager.setShowExplanation(showExplanationCheckBox.isSelected());
        
        // Apply theme immediately
        applyTheme(themeValue, fontSize);
        
        showStatus("设置已保存", "success");
        
        // Store new original values
        originalTheme = themeValue;
        originalFontSize = fontSize;
    }
    
    @FXML
    private void resetSettings() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认重置");
        alert.setHeaderText("确定要重置主题和显示设置吗？");
        alert.setContentText("这将恢复默认的显示设置。");
        
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            // Reset to defaults
            themeComboBox.setValue("浅色主题");
            fontSizeSlider.setValue(14);
            autoSaveCheckBox.setSelected(true);
            showExplanationCheckBox.setSelected(true);
            
            updateFontSizeLabel();
            showStatus("设置已重置", "info");
        }
    }
    
    @FXML
    private void previewTheme() {
        String selectedTheme = themeComboBox.getValue();
        String themeValue = switch (selectedTheme) {
            case "深色主题" -> "dark";
            case "自动跟随系统" -> "auto";
            default -> "light";
        };
        
        int fontSize = (int) fontSizeSlider.getValue();
        
        // Apply theme temporarily
        applyTheme(themeValue, fontSize);
        
        showStatus("预览模式 - 点击保存以永久应用", "info");
    }
    
    @FXML
    private void handleBack() {
        try {
            // Restore original theme if not saved
            if (!originalTheme.equals(settingsManager.getTheme()) || 
                originalFontSize != settingsManager.getFontSize()) {
                applyTheme(originalTheme, originalFontSize);
            }
            
            Main.switchScene("/fxml/main.fxml", "主菜单");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void applyTheme(String theme, int fontSize) {
        try {
            if (Main.getPrimaryStage() != null && Main.getPrimaryStage().getScene() != null) {
                // Remove existing theme stylesheets
                Main.getPrimaryStage().getScene().getStylesheets().removeIf(url -> 
                    url.contains("theme-") || url.contains("font-size.css")
                );
                
                // Add new theme stylesheet
                String themeFile = switch (theme) {
                    case "dark" -> "/css/theme-dark.css";
                    case "auto" -> "/css/theme-auto.css";
                    default -> "/css/theme-light.css";
                };
                
                // Try to load theme file
                try {
                    Main.getPrimaryStage().getScene().getStylesheets().add(
                        ThemeSettingsController.class.getResource(themeFile).toExternalForm()
                    );
                } catch (NullPointerException e) {
                    // If theme file doesn't exist, create inline CSS
                    String inlineCSS = switch (theme) {
                        case "dark" -> createDarkThemeCSS();
                        case "auto" -> createAutoThemeCSS();
                        default -> createLightThemeCSS();
                    };
                    
                    Main.getPrimaryStage().getScene().getStylesheets().add(
                        "data:text/css," + java.net.URLEncoder.encode(inlineCSS, "UTF-8")
                    );
                }
                
                // Add font size CSS
                String fontSizeCSS = String.format(
                    ".root { -fx-font-size: %dpx; } " +
                    ".label { -fx-font-size: %dpx; } " +
                    ".button { -fx-font-size: %dpx; } " +
                    ".text-field { -fx-font-size: %dpx; } " +
                    ".text-area { -fx-font-size: %dpx; } " +
                    ".combo-box { -fx-font-size: %dpx; }",
                    fontSize, fontSize, fontSize, fontSize, fontSize, fontSize
                );
                
                Main.getPrimaryStage().getScene().getStylesheets().add(
                    "data:text/css," + java.net.URLEncoder.encode(fontSizeCSS, "UTF-8")
                );
                
                System.out.println("主题已应用: " + theme + ", 字体大小: " + fontSize + "px");
            }
        } catch (Exception e) {
            System.err.println("应用主题失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String createLightThemeCSS() {
        return """
            .root { 
                -fx-background-color: #f0f2f5; 
                -fx-text-fill: #333333; 
            }
            .button { 
                -fx-background-color: #ffffff; 
                -fx-border-color: #dddddd; 
                -fx-text-fill: #333333; 
            }
            .button:hover { 
                -fx-background-color: #f8f9fa; 
            }
            """;
    }
    
    private String createDarkThemeCSS() {
        return """
            .root { 
                -fx-background-color: #1a1a1a; 
                -fx-text-fill: #ffffff; 
            }
            .button { 
                -fx-background-color: #2d2d2d; 
                -fx-border-color: #444444; 
                -fx-text-fill: #ffffff; 
            }
            .button:hover { 
                -fx-background-color: #3d3d3d; 
            }
            .text-field, .text-area { 
                -fx-background-color: #2d2d2d; 
                -fx-text-fill: #ffffff; 
                -fx-border-color: #444444; 
            }
            """;
    }
    
    private String createAutoThemeCSS() {
        return createLightThemeCSS(); // Default to light for auto
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