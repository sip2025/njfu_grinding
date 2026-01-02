package com.examapp.controller;

import com.examapp.data.AISettingsManager;
import com.examapp.model.Question;
import com.examapp.service.AIService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import java.util.function.Consumer;

public class AIPanelController {
    @FXML private ImageView modelIconView;
    @FXML private Label modelNameLabel;
    @FXML private VBox loadingContainer;
    @FXML private WebView responseWebView;
    @FXML private Label errorLabel;
    
    private AIService aiService;
    private AISettingsManager settingsManager;
    private Question currentQuestion;
    private Consumer<Void> onCloseCallback;
    
    @FXML
    public void initialize() {
        aiService = AIService.getInstance();
        settingsManager = AISettingsManager.getInstance();
        updateModelInfo();
    }
    
    public void setOnCloseCallback(Consumer<Void> callback) {
        this.onCloseCallback = callback;
    }
    
    private void updateModelInfo() {
        String model = settingsManager.getModel();
        modelNameLabel.setText(model != null && !model.isEmpty() ? model : "AI助手");
        
        String iconPath = getModelIconPath(model);
        if (iconPath != null) {
            try {
                modelIconView.setImage(new Image(getClass().getResourceAsStream(iconPath)));
            } catch (Exception e) {
                System.err.println("加载模型图标失败: " + e.getMessage());
            }
        }
    }
    
    private String getModelIconPath(String model) {
        if (model == null || model.isEmpty()) return "/images/ic_ai_assistant.png";
        
        String lower = model.toLowerCase();
        if (lower.contains("gpt") || lower.contains("openai")) return "/images/openai.png";
        if (lower.contains("gemini")) return "/images/gemini_color.png";
        if (lower.contains("claude")) return "/images/claude_color.png";
        if (lower.contains("deepseek")) return "/images/deepseek_color.png";
        if (lower.contains("glm") || lower.contains("chatglm")) return "/images/chatglm_color.png";
        if (lower.contains("qwen")) return "/images/qwen_color.png";
        if (lower.contains("grok")) return "/images/grok.png";
        if (lower.contains("ollama")) return "/images/ollama.png";
        return "/images/ic_ai_assistant.png";
    }
    
    public void analyzeQuestion(Question question) {
        this.currentQuestion = question;
        showLoading();
        
        aiService.askQuestion(question, new AIService.ChatCallback() {
            @Override
            public void onSuccess(String response) {
                Platform.runLater(() -> showResponse(response));
            }
            
            @Override
            public void onError(String error) {
                Platform.runLater(() -> showError(error));
            }
        });
    }
    
    @FXML
    private void handleRefresh() {
        if (currentQuestion != null) {
            showLoading();
            aiService.askQuestion(currentQuestion, new AIService.ChatCallback() {
                @Override
                public void onSuccess(String response) {
                    Platform.runLater(() -> showResponse(response));
                }
                
                @Override
                public void onError(String error) {
                    Platform.runLater(() -> showError(error));
                }
            }, true);
        }
    }
    
    @FXML
    private void handleClose() {
        // 隐藏并管理父容器
        if (responseWebView != null && responseWebView.getParent() != null) {
            javafx.scene.Node node = responseWebView;
            while (node.getParent() != null) {
                node = node.getParent();
                if (node instanceof VBox && node.getId() != null &&
                    (node.getId().equals("aiPanelContainer") || "aiPanelContainer".equals(((VBox)node).getId()))) {
                    node.setVisible(false);
                    node.setManaged(false);
                    break;
                }
            }
        }
        
        // 也调用回调（如果存在）
        if (onCloseCallback != null) {
            onCloseCallback.accept(null);
        }
    }
    
    private void showLoading() {
        loadingContainer.setVisible(true);
        loadingContainer.setManaged(true);
        responseWebView.setVisible(false);
        responseWebView.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
    
    private void showResponse(String markdown) {
        loadingContainer.setVisible(false);
        loadingContainer.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        
        String html = convertMarkdownToHtml(markdown);
        responseWebView.getEngine().loadContent(html);
        responseWebView.setVisible(true);
        responseWebView.setManaged(true);
    }
    
    private void showError(String error) {
        loadingContainer.setVisible(false);
        loadingContainer.setManaged(false);
        responseWebView.setVisible(false);
        responseWebView.setManaged(false);
        
        errorLabel.setText("错误: " + error);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
    
    private String convertMarkdownToHtml(String markdown) {
        String html = markdown
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\n\n", "</p><p>")
            .replace("\n", "<br>");
        
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
               "<style>body{font-family:sans-serif;padding:16px;line-height:1.6;}" +
               "p{margin:12px 0;}</style></head><body><p>" + html + "</p></body></html>";
    }
}