package com.examapp.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.application.HostServices;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

/**
 * 关于界面控制器
 * 对应Android版的AboutActivity
 */
public class AboutController {
    
    @FXML
    private ImageView donateImageView;
    
    private HostServices hostServices;
    
    /**
     * 设置HostServices用于打开外部链接
     */
    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }
    
    /**
     * 返回主界面
     */
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) donateImageView.getScene().getWindow();
            Scene scene = new Scene(root, 1000, 700);
            stage.setScene(scene);
            stage.setTitle("NJFU刷题助手");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 打开邮箱链接
     */
    @FXML
    private void handleEmailLink() {
        openLink("mailto:admin@mail.keggin.me");
    }
    
    /**
     * 打开GitHub链接
     */
    @FXML
    private void handleGithubLink() {
        openLink("https://github.com/keggin-CHN/njfu_grinding");
    }
    
    /**
     * 打开外部链接
     */
    private void openLink(String url) {
        try {
            // 优先使用HostServices
            if (hostServices != null) {
                hostServices.showDocument(url);
            } else {
                // 降级使用Desktop API
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.BROWSE)) {
                        desktop.browse(new URI(url));
                    } else if (desktop.isSupported(Desktop.Action.MAIL) && url.startsWith("mailto:")) {
                        desktop.mail(new URI(url));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("无法打开链接: " + url);
        }
    }
}