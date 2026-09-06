package kz.arianwait.gametexteditor.window;

import javafx.scene.Node;
import javafx.stage.Stage;

public final class WindowDragHandler {

    private double offsetX;
    private double offsetY;

    private WindowDragHandler() {}

    public static void install(Node node, Stage stage) {
        WindowDragHandler handler = new WindowDragHandler();

        node.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown()) {
                handler.offsetX = e.getScreenX() - stage.getX();
                handler.offsetY = e.getScreenY() - stage.getY();
            }
        });

        node.setOnMouseDragged(e -> {
            if (e.isPrimaryButtonDown()) {
                stage.setX(e.getScreenX() - handler.offsetX);
                stage.setY(e.getScreenY() - handler.offsetY);
            }
        });
    }
}
