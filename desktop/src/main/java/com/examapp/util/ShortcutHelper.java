package com.examapp.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * 快捷键帮助工具类
 */
public class ShortcutHelper {
    
    /**
     * 显示快捷键帮助对话框
     */
    public static void showShortcutHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("快捷键帮助");
        alert.setHeaderText("NJFU刷题助手快捷键列表");
        
        // 创建快捷键内容
        String shortcutsText = """
            全局快捷键：
            
            文件操作：
            Ctrl+I         打开导入题库
            Ctrl+Q         退出应用
            
            界面操作：
            Ctrl+T         打开主题和显示设置
            Ctrl+A         打开API接口设置
            Ctrl+Shift+D   打开开发者模式
            Ctrl+H         打开历史记录
            F1             打开关于页面
            Escape         返回主菜单
            
            学习模式快捷键：
            Ctrl+N         下一题
            Ctrl+P         上一题
            Ctrl+S         显示/隐藏答案
            Ctrl+E         显示/隐藏解析
            Space          提交答案
            
            考试模式快捷键：
            Ctrl+N         下一题
            Ctrl+P         上一题
            Ctrl+S         保存当前进度
            Ctrl+F         完成考试
            
            搜索模式快捷键：
            Ctrl+F         聚焦搜索框
            Enter          执行搜索
            Escape         清空搜索
            
            其他快捷键：
            Ctrl+Plus      增大字体
            Ctrl+Minus     减小字体
            Ctrl+0         重置字体大小
            """;
        
        TextArea textArea = new TextArea(shortcutsText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(500);
        textArea.setPrefHeight(400);
        textArea.setStyle("-fx-font-family: monospace; -fx-font-size: 13px;");
        
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);
        
        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 0);
        
        alert.getDialogPane().setExpandableContent(expContent);
        alert.getDialogPane().setExpanded(true);
        
        alert.showAndWait();
    }
    
    /**
     * 显示快速提示信息
     */
    public static void showQuickTip(String tip) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(tip);
        alert.showAndWait();
    }
}