package com.examapp;

import com.examapp.util.LogManager;
import com.examapp.controller.MainController;
import com.examapp.util.ShortcutHelper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * NJFU刷题助手桌面版主入口
 *
 * @author NJFU
 * @version 1.0.0
 */
public class Main extends Application {

    private static Stage primaryStage;
    private static final String APP_TITLE = "NJFU刷题助手";
    private static final int DEFAULT_WIDTH = 1200;
    private static final int DEFAULT_HEIGHT = 800;
    private static Scene currentScene;
    private static MainController mainController;

    @Override
    public void start(Stage stage) throws IOException {
        // 启动日志管理器并设置全局异常处理器
        LogManager.initialize();
        setupGlobalExceptionHandler();
        LogManager.info("日志文件位置: " + LogManager.getLogFilePath());

        primaryStage = stage;

        // 加载主界面
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();
        mainController = loader.getController();

        // 创建场景
        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);

        // 加载样式表
        scene.getStylesheets().add(
            Objects.requireNonNull(getClass().getResource("/css/style.css")).toExternalForm()
        );

        // 设置全局快捷键
        setupGlobalShortcuts(scene);

        // 设置窗口属性
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);
        primaryStage.setResizable(true);

        // 加载应用图标
        try {
            Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icon.png")));
            primaryStage.getIcons().add(icon);
            System.out.println("应用图标加载成功");
        } catch (Exception e) {
            System.err.println("无法加载应用图标: " + e.getMessage());
        }

        // 显示窗口
        primaryStage.show();

        // 确保窗口关闭时执行清理操作
        primaryStage.setOnCloseRequest(event -> {
            LogManager.info("窗口关闭请求，执行清理...");
            if (mainController != null) {
                mainController.cleanup();
            }
            LogManager.shutdown();
            Platform.exit();
            System.exit(0);
        });
    }

    private void setupGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("捕获到未处理的异常: " + throwable.getMessage());
            throwable.printStackTrace(); // 这会打印到日志文件

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("发生意外错误");
                alert.setHeaderText("应用程序遇到一个无法恢复的错误，即将关闭。");
                alert.setContentText("错误详情已记录到日志文件中，请将日志文件提供给开发者以帮助解决问题。");

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                String exceptionText = sw.toString();

                Label label = new Label("错误堆栈信息:");

                TextArea textArea = new TextArea(exceptionText);
                textArea.setEditable(false);
                textArea.setWrapText(true);

                textArea.setMaxWidth(Double.MAX_VALUE);
                textArea.setMaxHeight(Double.MAX_VALUE);
                GridPane.setVgrow(textArea, Priority.ALWAYS);
                GridPane.setHgrow(textArea, Priority.ALWAYS);

                GridPane expContent = new GridPane();
                expContent.setMaxWidth(Double.MAX_VALUE);
                expContent.add(label, 0, 0);
                expContent.add(textArea, 0, 1);

                alert.getDialogPane().setExpandableContent(expContent);
                alert.showAndWait();
                
                // 确保在关闭前日志被写入
                LogManager.shutdown();
                Platform.exit();
                System.exit(1);
            });
        });
    }

    /**
     * 设置全局快捷键
     */
    private void setupGlobalShortcuts(Scene scene) {
        setupGlobalShortcutsForScene(scene);
    }

    /**
     * 为指定场景设置全局快捷键
     */
    private static void setupGlobalShortcutsForScene(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            try {
                // Ctrl+I: 导入题库
                if (KeyCombination.keyCombination("Ctrl+I").match(event)) {
                    event.consume();
                    openImportShortcut();
                }
                // Ctrl+T: 主题设置
                else if (KeyCombination.keyCombination("Ctrl+T").match(event)) {
                    event.consume();
                    openThemeSettingsShortcut();
                }
                // Ctrl+Shift+D: 开发者模式
                else if (KeyCombination.keyCombination("Ctrl+Shift+D").match(event)) {
                    event.consume();
                    openDeveloperModeShortcut();
                }
                // Ctrl+A: AI设置
                else if (KeyCombination.keyCombination("Ctrl+A").match(event)) {
                    event.consume();
                    openAISettingsShortcut();
                }
                // Ctrl+H: 历史记录
                else if (KeyCombination.keyCombination("Ctrl+H").match(event)) {
                    event.consume();
                    openHistoryShortcut();
                }
                // Escape: 返回主界面
                else if (KeyCombination.keyCombination("Escape").match(event)) {
                    event.consume();
                    returnToMainShortcut();
                }
                // Ctrl+Q: 退出应用
                else if (KeyCombination.keyCombination("Ctrl+Q").match(event)) {
                    event.consume();
                    Platform.exit();
                }
                // F1: 帮助/关于
                else if (KeyCombination.keyCombination("F1").match(event)) {
                    event.consume();
                    showShortcutHelp();
                }
            } catch (Exception e) {
                System.err.println("快捷键处理失败: " + e.getMessage());
            }
        });
    }

    private static void openImportShortcut() {
        try {
            switchScene("/fxml/import.fxml", "导入题库");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void openThemeSettingsShortcut() {
        try {
            switchScene("/fxml/theme-settings.fxml", "主题和显示设置");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void openDeveloperModeShortcut() {
        try {
            switchScene("/fxml/developer-mode.fxml", "开发者模式");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void openAISettingsShortcut() {
        try {
            switchScene("/fxml/ai-settings.fxml", "API接口设置");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void openHistoryShortcut() {
        try {
            switchScene("/fxml/history.fxml", "历史记录");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void returnToMainShortcut() {
        try {
            switchScene("/fxml/main.fxml", "主菜单");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void showShortcutHelp() {
        ShortcutHelper.showShortcutHelp();
    }

    private static void openAboutShortcut() {
        try {
            switchScene("/fxml/about.fxml", "关于");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取主舞台
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * 获取当前场景
     */
    public static Scene getCurrentScene() {
        return currentScene;
    }

    /**
     * 切换场景
     */
    public static void switchScene(String fxmlPath, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
        Parent root = loader.load();
        // 创建场景时使用当前窗口的尺寸，以保持大小不变
        Scene scene = new Scene(root, primaryStage.getScene().getWidth(), primaryStage.getScene().getHeight());
        scene.getStylesheets().add(
            Objects.requireNonNull(Main.class.getResource("/css/style.css")).toExternalForm()
        );
        
        // 更新当前场景并重新设置快捷键
        currentScene = scene;
        setupGlobalShortcutsForScene(scene);
        
        primaryStage.setScene(scene);
        if (title != null && !title.isEmpty()) {
            primaryStage.setTitle(APP_TITLE + " - " + title);
        }
    }

    /**
     * 切换场景并传递数据给控制器
     */
    public static void switchSceneWithData(String fxmlPath, String title, Consumer<Object> controllerConsumer) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
        Parent root = loader.load();
        Object controller = loader.getController();
        if (controllerConsumer != null) {
            controllerConsumer.accept(controller);
        }
        // 创建场景时使用当前窗口的尺寸，以保持大小不变
        Scene scene = new Scene(root, primaryStage.getScene().getWidth(), primaryStage.getScene().getHeight());
        scene.getStylesheets().add(
            Objects.requireNonNull(Main.class.getResource("/css/style.css")).toExternalForm()
        );
        
        // 更新当前场景并重新设置快捷键
        currentScene = scene;
        setupGlobalShortcutsForScene(scene);
        
        primaryStage.setScene(scene);
        if (title != null && !title.isEmpty()) {
            primaryStage.setTitle(APP_TITLE + " - " + title);
        }
    }

    /**
     * 在新窗口中打开场景
     */
    public static Stage openNewWindow(String fxmlPath, String title, int width, int height) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
        Parent root = loader.load();
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(
            Objects.requireNonNull(Main.class.getResource("/css/style.css")).toExternalForm()
        );

        Stage newStage = new Stage();
        newStage.setTitle(title);
        newStage.setScene(scene);
        newStage.show();

        return newStage;
    }

    @Override
    public void stop() {
        // 这个方法在setOnCloseRequest之后可能不会被完全执行，
        // 但我们仍然保留它以备其他退出方式调用。
        LogManager.info("应用正在停止...");
        if (mainController != null) {
            mainController.cleanup();
        }
        LogManager.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}