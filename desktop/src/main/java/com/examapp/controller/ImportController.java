package com.examapp.controller;

import com.examapp.Main;
import com.examapp.model.Question;
import com.examapp.model.Subject;
import com.examapp.service.AIService;
import com.examapp.service.ImportService;
import com.examapp.util.IconHelper;
import com.examapp.util.LogManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ImportController implements Initializable {
    
    @FXML private Label fileNameLabel;
    @FXML private Button selectFileButton;
    @FXML private TextField subjectNameField;
    @FXML private TextArea previewArea;
    @FXML private ProgressBar progressBar;
    @FXML private Button importButton;
    @FXML private Button cancelButton;
    @FXML private Label statusLabel;
    @FXML private VBox contentVBox;
    @FXML private RadioButton noProcessingRadio;
    @FXML private RadioButton generateAIAnalysisRadio;
    @FXML private TextField concurrencyField;
    @FXML private ToggleGroup aiProcessingGroup;
    
    private ImportService importService;
    private AIService aiService;
    private File selectedFile;
    private ImportService.ImportPreview currentPreview;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        importService = new ImportService();
        aiService = AIService.getInstance();
        
        // Set up icons
        selectFileButton.setGraphic(IconHelper.getImportIcon(20));
        importButton.setGraphic(IconHelper.getCheckIcon(20));
        cancelButton.setGraphic(IconHelper.getBackIcon(20));
        
        // Initially hide progress components
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        
        // Set up event handlers
        selectFileButton.setOnAction(e -> selectFile());
        importButton.setOnAction(e -> importQuestions());
        cancelButton.setOnAction(e -> handleCancel());
        
        // Disable import button initially
        importButton.setDisable(true);
        
        // Set up subject name field listener
        subjectNameField.textProperty().addListener((obs, oldVal, newVal) -> updateImportButton());
        
        // Disable concurrency field initially
        concurrencyField.setDisable(true);
        concurrencyField.setText("10");
        
        // Enable/disable concurrency field based on radio selection
        aiProcessingGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            concurrencyField.setDisable(newVal != generateAIAnalysisRadio);
            updateImportButton();
        });
        
        // Validate concurrency field
        concurrencyField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                concurrencyField.setText(newVal.replaceAll("[^\\d]", ""));
            }
            updateImportButton();
        });
    }
    
    private void selectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择题库文件");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON文件", "*.json"));
        
        File file = fileChooser.showOpenDialog(selectFileButton.getScene().getWindow());
        if (file != null) {
            LogManager.info("User selected file to import: " + file.getAbsolutePath());
            selectedFile = file;
            fileNameLabel.setText("已选择文件: " + file.getName());
            
            // Auto-fill subject name from file name
            String fileName = file.getName();
            if (fileName.toLowerCase().endsWith(".json")) {
                fileName = fileName.substring(0, fileName.length() - 5);
            }
            subjectNameField.setText(fileName);
            
            // Load preview
            loadPreview();
        }
    }
    
    private void loadPreview() {
        if (selectedFile == null) return;
        
        previewArea.setText("正在加载预览...");
        
        Task<ImportService.ImportPreview> previewTask = new Task<>() {
            @Override
            protected ImportService.ImportPreview call() throws Exception {
                return importService.getPreview(selectedFile);
            }
        };
        
        previewTask.setOnSucceeded(e -> {
            currentPreview = previewTask.getValue();
            Platform.runLater(() -> {
                previewArea.setText(currentPreview.toString());
                updateImportButton();
            });
        });
        
        previewTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                previewArea.setText("预览加载失败: " + previewTask.getException().getMessage());
                currentPreview = null;
                updateImportButton();
            });
        });
        
        new Thread(previewTask).start();
    }
    
    private void updateImportButton() {
        boolean canImport = selectedFile != null &&
                           subjectNameField.getText() != null &&
                           !subjectNameField.getText().trim().isEmpty() &&
                           currentPreview != null &&
                           currentPreview.getTotalQuestions() > 0;
        
        // If AI processing is selected, require valid concurrency
        if (canImport && generateAIAnalysisRadio.isSelected()) {
            String concurrency = concurrencyField.getText();
            canImport = concurrency != null && !concurrency.trim().isEmpty() &&
                       Integer.parseInt(concurrency) > 0;
        }
        
        importButton.setDisable(!canImport);
    }
    
    private void importQuestions() {
        if (selectedFile == null || subjectNameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "错误", "请选择题库文件并输入科目名称");
            return;
        }
        LogManager.info("User clicked 'Import' button for file: " + selectedFile.getAbsolutePath() + " with subject name: " + subjectNameField.getText());
        
        // Show progress
        progressBar.setVisible(true);
        progressBar.setManaged(true);
        progressBar.setProgress(-1); // Indeterminate progress
        importButton.setDisable(true);
        selectFileButton.setDisable(true);
        subjectNameField.setDisable(true);
        statusLabel.setText("正在导入题库...");
        
        Task<Subject> importTask = new Task<>() {
            @Override
            protected Subject call() throws Exception {
                return importService.importFromJson(selectedFile);
            }
        };
        
        importTask.setOnSucceeded(e -> {
            Subject subject = importTask.getValue();
            Platform.runLater(() -> {
                // Check if AI processing is needed
                if (generateAIAnalysisRadio.isSelected()) {
                    int concurrency = Integer.parseInt(concurrencyField.getText());
                    statusLabel.setText("导入完成！正在后台生成AI解析...");
                    progressBar.setProgress(0);
                    
                    // Start AI processing in background
                    processAIAnalysis(subject, concurrency);
                    
                    showAlert(Alert.AlertType.INFORMATION, "导入成功",
                             String.format("成功导入题库 \"%s\"\n总题数: %d\n\nAI解析生成已在后台启动\n并发数: %d\n请在主界面查看进度",
                             subject.getName(),
                             subject.getTotalQuestions(),
                             concurrency));
                } else {
                    statusLabel.setText("导入完成！");
                    progressBar.setProgress(1.0);
                    
                    showAlert(Alert.AlertType.INFORMATION, "导入成功",
                             String.format("成功导入题库 \"%s\"\n总题数: %d\n单选题: %d\n多选题: %d\n判断题: %d",
                             subject.getName(),
                             subject.getTotalQuestions(),
                             subject.getSingleChoiceCount(),
                             subject.getMultipleChoiceCount(),
                             subject.getTrueFalseCount()));
                }
                
                // Return to main menu after a short delay
                javafx.util.Duration duration = javafx.util.Duration.seconds(2);
                javafx.animation.KeyFrame keyFrame = new javafx.animation.KeyFrame(duration, ev -> {
                    try {
                        Main.switchScene("/fxml/main.fxml", "主菜单");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                javafx.animation.Timeline timeline = new javafx.animation.Timeline(keyFrame);
                timeline.play();
            });
        });
        
        importTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                statusLabel.setText("导入失败");
                progressBar.setVisible(false);
                progressBar.setManaged(false);
                importButton.setDisable(false);
                selectFileButton.setDisable(false);
                subjectNameField.setDisable(false);
                
                showAlert(Alert.AlertType.ERROR, "导入失败", 
                         "导入题库时发生错误:\n" + importTask.getException().getMessage());
            });
        });
        
        new Thread(importTask).start();
    }
    
    private void processAIAnalysis(Subject subject, int concurrency) {
        List<Question> questions = subject.getQuestions();
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger total = new AtomicInteger(questions.size());
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        
        LogManager.info("Starting AI analysis generation for " + total.get() + " questions with concurrency: " + concurrency);
        
        for (Question question : questions) {
            executor.submit(() -> {
                try {
                    aiService.askQuestion(question, new AIService.ChatCallback() {
                        @Override
                        public void onSuccess(String response) {
                            int count = completed.incrementAndGet();
                            Platform.runLater(() -> {
                                double progress = (double) count / total.get();
                                progressBar.setProgress(progress);
                                statusLabel.setText(String.format("AI解析生成中... (%d/%d)", count, total.get()));
                            });
                            
                            if (count == total.get()) {
                                Platform.runLater(() -> {
                                    statusLabel.setText("AI解析生成完成！");
                                    LogManager.info("AI analysis generation completed for all questions");
                                });
                                executor.shutdown();
                            }
                        }
                        
                        @Override
                        public void onError(String error) {
                            LogManager.warning("AI analysis failed for question: " + error);
                            int count = completed.incrementAndGet();
                            Platform.runLater(() -> {
                                double progress = (double) count / total.get();
                                progressBar.setProgress(progress);
                            });
                            
                            if (count == total.get()) {
                                executor.shutdown();
                            }
                        }
                    });
                } catch (Exception e) {
                    LogManager.severe("Error processing AI analysis: " + e.getMessage());
                    int count = completed.incrementAndGet();
                    if (count == total.get()) {
                        executor.shutdown();
                    }
                }
            });
        }
    }
    
    @FXML
    private void handleCancel() {
        LogManager.info("User clicked 'Cancel' on import screen.");
        try {
            Main.switchScene("/fxml/main.fxml", "主菜单");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.initOwner(importButton.getScene().getWindow());
        alert.showAndWait();
    }
}