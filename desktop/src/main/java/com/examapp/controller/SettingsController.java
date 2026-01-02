package com.examapp.controller;

import com.examapp.Main;
import com.examapp.util.LogManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }
    
    @FXML
    private void handleBack() {
        LogManager.info("User clicked 'Back' from Settings screen.");
        try {
            Main.switchScene("/fxml/main.fxml", "");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleAISettings() {
        LogManager.info("User clicked 'AI Settings' from Settings screen.");
        try {
            Main.switchScene("/fxml/ai-settings.fxml", "AI答疑设置");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleAbout() {
        LogManager.info("User clicked 'About' from Settings screen.");
        try {
            Main.switchScene("/fxml/about.fxml", "关于");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}