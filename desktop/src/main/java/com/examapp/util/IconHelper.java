package com.examapp.util;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * 图标助手类 - 提供统一的图标访问
 * 使用Unicode字符和图片资源来复刻Android端的Material Design图标
 */
public class IconHelper {
    
    // Unicode图标字符 (保留，用于快速占位或简单场景)
    public static final String ICON_CHECK_UNICODE = "✓";
    public static final String ICON_WRONG_UNICODE = "❌";
    public static final String ICON_CORRECT_UNICODE = "✅";

    // ==================== 标准图片图标 ====================
    // 为常用图标提供专用方法，统一使用图片资源

    public static Node getMenuIcon(double size) { return createImageIcon("ic_menu.png", size, size); }
    public static Node getSettingsIcon(double size) { return createImageIcon("ic_settings.png", size, size); }
    public static Node getAboutIcon(double size) { return createImageIcon("关于.png", size, size); }
    public static Node getSyncIcon(double size) { return createImageIcon("ic_sync.png", size, size); }
    public static Node getShareIcon(double size) { return createImageIcon("ic_share.png", size, size); }
    public static Node getDeleteIcon(double size) { return createImageIcon("ic_delete.png", size, size); }
    public static Node getCheckIcon(double size) { return createImageIcon("ic_check.png", size, size); }
    public static Node getDeveloperIcon(double size) { return createImageIcon("ic_developer.png", size, size); }
    public static Node getLinkIcon(double size) { return createImageIcon("ic_link.png", size, size); }
    public static Node getBackgroundIcon(double size) { return createImageIcon("ic_background.png", size, size); }
    public static Node getAiAssistantIcon(double size) { return createImageIcon("AI答疑.png", size, size); }
    public static Node getExpandMoreIcon(double size) { return createImageIcon("ic_expand_more.png", size, size); }
    public static Node getSearchIcon(double size) { return createImageIcon("ic_search.png", size, size); }
    public static Node getHistoryIcon(double size) { return createImageIcon("考试历史.png", size, size); }
    public static Node getBackIcon(double size) { return createImageIcon("ic_back.png", size, size); }
    public static Node getImportIcon(double size) { return createImageIcon("导入.png", size, size); }
    public static Node getApiSettingsIcon(double size) { return createImageIcon("API接口.png", size, size); }
    public static Node getStarOutlineIcon(double size) {
        Label star = new Label("☆");
        star.setStyle(String.format("-fx-font-size: %.0fpx; -fx-text-fill: #FFC107;", size));
        return star;
    }
    
    public static Node getStarFilledIcon(double size) {
        Label star = new Label("★");
        star.setStyle(String.format("-fx-font-size: %.0fpx; -fx-text-fill: #FFC107;", size));
        return star;
    }
    public static Node getStudyModeIcon(double size) { return createImageIcon("学习模式.png", size, size); }
    public static Node getEndlessModeIcon(double size) { return createImageIcon("无尽模式.png", size, size); }
    public static Node getWrongReviewIcon(double size) { return createImageIcon("错题回顾.png", size, size); }
    public static Node getMockExamIcon(double size) { return createImageIcon("模拟考试.png", size, size); }
    public static Node getWrongQuestionsIcon(double size) { return createImageIcon("错题本.png", size, size); }
    public static Node getWrongAnalysisIcon(double size) { return createImageIcon("错题分析.png", size, size); }
    
    // ==================== 创建图标Label ====================
    
    /**
     * 创建图标Label
     * @param iconChar 图标字符
     * @param size 字体大小
     * @param color 颜色（可选）
     * @return Label节点
     */
    // createIcon 方法已废弃，未来将被移除，统一使用图片图标
    @Deprecated
    public static Label createIcon(String iconChar, int size, String color) {
        Label icon = new Label(iconChar);
        icon.setStyle(String.format(
            "-fx-font-size: %dpx; -fx-text-fill: %s; -fx-padding: 0;",
            size, color != null ? color : "#333333"
        ));
        return icon;
    }
    
    /**
     * 创建图标Label（默认颜色）
     */
    public static Label createIcon(String iconChar, int size) {
        return createIcon(iconChar, size, null);
    }
    
    /**
     * 创建小图标（16px）
     */
    public static Label createSmallIcon(String iconChar) {
        return createIcon(iconChar, 16);
    }
    
    /**
     * 创建中等图标（20px）
     */
    public static Label createMediumIcon(String iconChar) {
        return createIcon(iconChar, 20);
    }
    
    /**
     * 创建大图标（24px）
     */
    public static Label createLargeIcon(String iconChar) {
        return createIcon(iconChar, 24);
    }
    
    // ==================== 创建图片图标 ====================
    
    /**
     * 创建图片图标（用于AI服务商logo等）
     * @param imageName 图片文件名（不含路径）
     * @param width 宽度
     * @param height 高度
     * @return ImageView节点
     */
    public static Node createImageIcon(String imageName, double width, double height) {
        // 修复：图片资源位于 'images/' 目录下，而不是 'icons/'
        final String path = "images/" + imageName;
        try {
            // 使用ClassLoader来加载资源，这是在JAR包中最可靠的方式
            java.io.InputStream imageStream = IconHelper.class.getClassLoader().getResourceAsStream(path);
            if (imageStream == null) {
                 throw new java.io.FileNotFoundException("Cannot find resource: " + path);
            }
            Image image = new Image(imageStream);
            if (image.isError()) {
                throw new Exception("Image loading error for: " + path, image.getException());
            }
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(width);
            imageView.setFitHeight(height);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            return imageView;
        } catch (Exception e) {
            System.err.println("[IconHelper] 加载图片失败: " + imageName);
            // 返回占位符Label，避免返回null导致崩溃
            Label placeholder = new Label("?");
            placeholder.setStyle(String.format(
                "-fx-font-size: %.0fpx; -fx-text-fill: #999999; -fx-background-color: #EEEEEE; " +
                "-fx-pref-width: %.0fpx; -fx-pref-height: %.0fpx; -fx-alignment: center;",
                Math.min(width, height) * 0.6, width, height
            ));
            return placeholder;
        }
    }
    
    /**
     * 获取AI服务商图标
     */
    public static Node getAIProviderIcon(String provider, double size) {
        String imageName = switch (provider.toLowerCase()) {
            case "openai" -> "openai.png";
            case "claude" -> "claude_color.png";
            case "gemini" -> "gemini_color.png";
            case "qwen" -> "qwen_color.png";
            case "deepseek" -> "deepseek_color.png";
            case "chatglm" -> "chatglm_color.png";
            case "grok" -> "grok.png";
            case "ollama" -> "ollama.png";
            default -> "ic_launcher.png";
        };
        return createImageIcon(imageName, size, size);
    }
    
    // ==================== 状态颜色 ====================
    
    public static final String COLOR_PRIMARY = "#1976D2";    // 主色
    public static final String COLOR_SUCCESS = "#4CAF50";    // 成功绿色
    public static final String COLOR_ERROR = "#F44336";      // 错误红色
    public static final String COLOR_WARNING = "#FF9800";    // 警告橙色
    public static final String COLOR_INFO = "#2196F3";       // 信息蓝色
    public static final String COLOR_GRAY = "#757575";       // 灰色
    public static final String COLOR_LIGHT_GRAY = "#BDBDBD"; // 浅灰色
    
    /**
     * 创建带颜色的图标
     */
    public static Label createColoredIcon(String iconChar, int size, IconColor colorType) {
        String color = switch (colorType) {
            case PRIMARY -> COLOR_PRIMARY;
            case SUCCESS -> COLOR_SUCCESS;
            case ERROR -> COLOR_ERROR;
            case WARNING -> COLOR_WARNING;
            case INFO -> COLOR_INFO;
            case GRAY -> COLOR_GRAY;
            case LIGHT_GRAY -> COLOR_LIGHT_GRAY;
        };
        return createIcon(iconChar, size, color);
    }
    
    /**
     * 图标颜色枚举
     */
    public enum IconColor {
        PRIMARY, SUCCESS, ERROR, WARNING, INFO, GRAY, LIGHT_GRAY
    }
    
    // ==================== 题目状态圆圈 ====================
    
    /**
     * 创建题目状态圆圈（用于侧边栏）
     * @param number 题号
     * @param status 状态（UNANSWERED/CORRECT/WRONG）
     * @return Label节点
     */
    public static Label createQuestionCircle(int number, String status) {
        Label circle = new Label(String.valueOf(number));
        String backgroundColor = switch (status.toUpperCase()) {
            case "CORRECT" -> COLOR_SUCCESS;   // 绿色 - 正确
            case "WRONG" -> COLOR_ERROR;       // 红色 - 错误
            default -> "#FFFFFF";              // 白色 - 未作答
        };
        String textColor = "UNANSWERED".equals(status.toUpperCase()) ? "#333333" : "#FFFFFF";
        String borderColor = "UNANSWERED".equals(status.toUpperCase()) ? "#BDBDBD" : backgroundColor;
        
        circle.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: %s; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 50%%; " +
            "-fx-background-radius: 50%%; " +
            "-fx-min-width: 36px; " +
            "-fx-min-height: 36px; " +
            "-fx-max-width: 36px; " +
            "-fx-max-height: 36px; " +
            "-fx-alignment: center; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-cursor: hand;",
            backgroundColor, textColor, borderColor
        ));
        
        return circle;
    }
}