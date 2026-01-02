package com.examapp.util;

import javafx.scene.Node;
import javafx.scene.Cursor;
import javafx.scene.layout.Pane;

/**
 * 一个帮助类，使任何Node（如按钮）都可以在其父Pane内拖动。
 */
public class DraggableFABHelper {

    private double xOffset = 0;
    private double yOffset = 0;

    /**
     * 使节点可拖动。
     * @param node 要使其可拖动的节点。
     */
    public void makeDraggable(Node node) {
        // 确保节点有一个父Pane
        if (!(node.getParent() instanceof Pane)) {
            System.err.println("Draggable node must be inside a Pane.");
            return;
        }
        
        final Pane parentPane = (Pane) node.getParent();

        node.setOnMousePressed(event -> {
            xOffset = node.getLayoutX() - event.getSceneX();
            yOffset = node.getLayoutY() - event.getSceneY();
            node.setCursor(Cursor.CLOSED_HAND);
            event.consume();
        });

        node.setOnMouseDragged(event -> {
            double newX = event.getSceneX() + xOffset;
            double newY = event.getSceneY() + yOffset;

            // 限制按钮在父Pane的边界内
            double clampedX = Math.max(0, Math.min(newX, parentPane.getWidth() - node.getBoundsInLocal().getWidth()));
            double clampedY = Math.max(0, Math.min(newY, parentPane.getHeight() - node.getBoundsInLocal().getHeight()));

            node.setLayoutX(clampedX);
            node.setLayoutY(clampedY);
            event.consume();
        });

        node.setOnMouseReleased(event -> {
            node.setCursor(Cursor.HAND);
            event.consume();
        });
        
        node.setCursor(Cursor.HAND);
    }
}