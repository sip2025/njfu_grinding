package com.examapp.controller;

import com.examapp.model.Question;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.application.Platform;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.scene.control.Toggle;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class QuestionNavPanelController {

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private VBox navContainer;

    private List<Question> questions;
    private Consumer<Question> onQuestionSelect;
    private Consumer<String> onFilterSelect;
    private Question currentQuestion;
    private String currentMode;
    private String currentFilter = "全部";
    private Map<String, QuestionNavItemController> navItemControllers = new HashMap<>();
    private ToggleGroup filterGroup = new ToggleGroup();

    public void initialize() {
        // Initialization if needed
    }

    /**
     * 加载并构建整个题目导航面板
     * @param questions 题目列表
     * @param currentQuestion 当前显示的题目
     * @param onQuestionSelect 点击题号时的回调函数
     * @param onFilterSelect 点击筛选按钮时的回调
     * @param mode 当前的练习模式 ("sequential", "random", "review")
     */
    public void loadQuestions(List<Question> questions, Question currentQuestion, Consumer<Question> onQuestionSelect, Consumer<String> onFilterSelect, String mode) {
        this.questions = questions;
        this.currentQuestion = currentQuestion;
        this.onQuestionSelect = onQuestionSelect;
        this.onFilterSelect = onFilterSelect;
        this.currentMode = mode;
        // 只有在第一次加载时，或者模式切换时才重置筛选器
        if (this.filterGroup.getToggles().isEmpty()) {
            this.currentFilter = "全部";
        }
        buildNavPanel();
        
        // 初始加载时自动对焦到当前题目
        if (!"random".equals(mode) && currentQuestion != null && navItemControllers.containsKey(currentQuestion.getId())) {
            Platform.runLater(() -> {
                QuestionNavItemController currentItemController = navItemControllers.get(currentQuestion.getId());
                scrollToCenter(currentItemController.getRootNode());
            });
        }
    }

    /**
     * 当用户切换题目时，更新导航面板中的高亮状态
     * @param newCurrentQuestion 新的当前题目
     */
    public void updateSelection(Question newCurrentQuestion) {
        // 随机模式下没有选择高亮
        if ("random".equals(currentMode)) return;

        boolean isReviewMode = "review".equals(currentMode);
        
        if (this.currentQuestion != null && navItemControllers.containsKey(this.currentQuestion.getId())) {
            navItemControllers.get(this.currentQuestion.getId()).updateStyle(false, isReviewMode); // 取消旧的高亮
        }
        this.currentQuestion = newCurrentQuestion;
        if (this.currentQuestion != null && navItemControllers.containsKey(this.currentQuestion.getId())) {
            QuestionNavItemController currentItemController = navItemControllers.get(this.currentQuestion.getId());
            currentItemController.updateStyle(true, isReviewMode); // 设置新的高亮
            
            // 滚动到视图中央
            scrollToCenter(currentItemController.getRootNode());
        }
    }

    // Enhanced scrolling logic
    public void scrollToCenter(Node node) {
        // Give the layout a moment to settle before calculating positions
        PauseTransition pause = new PauseTransition(Duration.millis(50));
        pause.setOnFinished(event -> {
            if (scrollPane == null || node == null || scrollPane.getViewportBounds() == null) return;

            double scrollPaneHeight = scrollPane.getViewportBounds().getHeight();
            // Use bounds in parent relative to the scroll pane's content
            double contentHeight = scrollPane.getContent().getBoundsInLocal().getHeight();
            
            if (contentHeight <= scrollPaneHeight) {
                scrollPane.setVvalue(0);
                return;
            }

            // Calculate the node's position relative to the VBox (the content of the ScrollPane)
            double nodeY = node.getBoundsInParent().getMinY();
            Node parent = node.getParent();
            while (parent != null && parent != scrollPane.getContent()) {
                nodeY += parent.getBoundsInParent().getMinY();
                parent = parent.getParent();
            }
            
            double nodeHeight = node.getBoundsInParent().getHeight();
            double nodeCenterY = nodeY + nodeHeight / 2.0;

            // Calculate the desired scroll position (vvalue) to center the node
            double targetVvalue = (nodeCenterY - scrollPaneHeight / 2.0) / (contentHeight - scrollPaneHeight);

            // Clamp the value between 0 and 1
            scrollPane.setVvalue(Math.max(0, Math.min(1, targetVvalue)));
        });
        pause.play();
    }

    /**
     * 当某个题目的状态（如对错）发生改变时，更新其显示
     * @param updatedQuestion 更新后的题目对象
     */
    public void updateFilterSelection(String newFilter) {
        this.currentFilter = newFilter;
        for (Toggle toggle : filterGroup.getToggles()) {
            ToggleButton button = (ToggleButton) toggle;
            if (button.getUserData().equals(newFilter)) {
                button.setSelected(true);
            } else {
                button.setSelected(false);
            }
        }
    }

    public void updateQuestionState(Question updatedQuestion) {
        // 更新questions列表中的question对象
        if (questions != null) {
            for (int i = 0; i < questions.size(); i++) {
                if (questions.get(i).getId().equals(updatedQuestion.getId())) {
                    questions.set(i, updatedQuestion);
                    break;
                }
            }
        }
        // 更新单个导航项的样式
        boolean isReviewMode = "review".equals(currentMode);
        if (navItemControllers.containsKey(updatedQuestion.getId())) {
            navItemControllers.get(updatedQuestion.getId()).updateStyle(
                updatedQuestion.getId().equals(currentQuestion.getId()), // 是否是当前题目
                isReviewMode
            );
        }
    }

    private void buildNavPanel() {
        navContainer.getChildren().clear();
        navItemControllers.clear(); // 清空旧的控制器引用

        if (questions == null || questions.isEmpty()) {
            navContainer.getChildren().add(new Label("没有题目可供导航。"));
            return;
        }

        switch (currentMode) {
            case "random":
                buildRandomModeFilterPanel();
                break;
            case "review":
                buildReviewModePanel();
                break;
            case "mock_exam":
                buildMockExamPanel();
                break;
            case "sequential":
            default:
                buildPracticeModePanel();
                break;
        }
    }

    private void buildReviewModePanel() {
        // 背题/错题模式: 简单的分组网格 (热力图)
        navContainer.getChildren().add(createLegend());
        buildGroupedPanel(false);
    }

    private Node createLegend() {
        VBox legendBox = new VBox(5);
        legendBox.setPadding(new Insets(0, 0, 15, 5));
        legendBox.getChildren().add(new Label("图例 (错题次数):"));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);

        grid.add(createLegendItem("#FFCDD2", "1-2次"), 0, 0);
        grid.add(createLegendItem("#FF8A80", "3-4次"), 1, 0);
        grid.add(createLegendItem("#D32F2F", "5次及以上"), 0, 1);
        grid.add(createLegendItem("#BDBDBD", "未答/答对"), 1, 1);
        
        legendBox.getChildren().add(grid);
        return legendBox;
    }

    private HBox createLegendItem(String color, String text) {
        Rectangle rect = new Rectangle(16, 16);
        rect.setFill(Color.web(color));
        rect.setStroke(Color.GRAY);
        rect.setStrokeWidth(0.5);

        Label label = new Label(text);
        HBox hbox = new HBox(5, rect, label);
        hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return hbox;
    }

    private void buildPracticeModePanel() {
        // 顺序练习模式: 可折叠的分组列表
        buildGroupedPanel(true);
    }

    private void buildMockExamPanel() {
        // 模拟考试模式: 按题型分组显示，不可折叠（与安卓端对齐）
        buildGroupedPanel(false);
    }

    private void buildGroupedPanel(boolean collapsible) {
        // 使用LinkedHashMap保持插入顺序
        Map<String, List<Question>> groupedQuestions = new LinkedHashMap<>();
        
        // 按题目类型分组，保持原有顺序
        for (Question q : questions) {
            String type = q.getType();
            groupedQuestions.computeIfAbsent(type, k -> new ArrayList<>()).add(q);
        }

        List<String> typeOrder = Arrays.asList("单选题", "多选题", "判断题");

        for (String questionType : typeOrder) {
            if (groupedQuestions.containsKey(questionType)) {
                List<Question> questionGroup = groupedQuestions.get(questionType);

                TilePane tilePane = createTilePaneForGroup(questionGroup);

                Label titleLabel = new Label(questionType + " (" + questionGroup.size() + ")");
                titleLabel.getStyleClass().add("section-title");
                VBox.setMargin(titleLabel, new javafx.geometry.Insets(10, 0, 5, 0));

                if (collapsible) {
                    titleLabel.setGraphic(new Label("▼ ")); // 初始为展开状态
                    titleLabel.setStyle("-fx-cursor: hand;");
                    titleLabel.setOnMouseClicked(event -> {
                        boolean isVisible = !tilePane.isVisible();
                        tilePane.setVisible(isVisible);
                        tilePane.setManaged(isVisible);
                        titleLabel.setGraphic(new Label(isVisible ? "▼ " : "▶ "));
                    });
                }

                navContainer.getChildren().addAll(titleLabel, tilePane);
            }
        }
    }

    private TilePane createTilePaneForGroup(List<Question> questionGroup) {
        TilePane tilePane = new TilePane();
        tilePane.setHgap(8);
        tilePane.setVgap(8);
        tilePane.setPrefColumns(5);

        for (Question question : questionGroup) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/question-nav-item.fxml"));
                Node navItemNode = loader.load();
                QuestionNavItemController controller = loader.getController();
                navItemControllers.put(question.getId(), controller); // 存储控制器引用

                boolean isCurrent = question.getId().equals(currentQuestion.getId());
                boolean isReviewMode = "review".equals(currentMode);
                controller.setData(question, isCurrent, isReviewMode, q -> {
                    if (onQuestionSelect != null) {
                        onQuestionSelect.accept(q);
                    }
                }, navItemNode);

                tilePane.getChildren().add(navItemNode);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return tilePane;
    }

    private void buildRandomModeFilterPanel() {
        navContainer.getChildren().clear();
        navContainer.setSpacing(10);

        Label title = new Label("题目类型筛选");
        title.getStyleClass().add("section-title");

        filterGroup.getToggles().clear();

        List<String> filters = Arrays.asList("混合题", "单选题", "多选题", "判断题");
        VBox filterBox = new VBox(8);

        for (String filterName : filters) {
            ToggleButton button = new ToggleButton(filterName);
            button.setToggleGroup(filterGroup);
            button.getStyleClass().add("filter-toggle-button");
            button.setMaxWidth(Double.MAX_VALUE);
            
            // "混合题" 对应 "全部" 的筛选逻辑
            String userData = "混合题".equals(filterName) ? "全部" : filterName;
            button.setUserData(userData);

            if (userData.equals(currentFilter)) {
                button.setSelected(true);
            }

            button.setOnAction(event -> {
                String selectedFilter = (String) button.getUserData();
                if (onFilterSelect != null && !selectedFilter.equals(currentFilter)) {
                    currentFilter = selectedFilter;
                    onFilterSelect.accept(currentFilter);
                }
            });
            filterBox.getChildren().add(button);
        }

        navContainer.getChildren().addAll(title, filterBox);
    }
}