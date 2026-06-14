package kz.aws.gametexteditor.window;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class CustomTitleBar extends HBox {

    private final Stage stage;
    private final Label titleLabel;
    private boolean maximized = false;
    private double restoreX, restoreY, restoreW, restoreH;

    public CustomTitleBar(Stage stage) {
        this.stage = stage;
        getStyleClass().add("title-bar");
        setAlignment(Pos.CENTER_LEFT);

        titleLabel = new Label("Liza Text Editor");
        titleLabel.getStyleClass().add("title-label");
        Label title = titleLabel;

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minimizeBtn = createWindowButton("\u2014", "minimize-button");
        minimizeBtn.setOnAction(e -> stage.setIconified(true));

        Button maximizeBtn = createWindowButton("\u25A1", "maximize-button");
        maximizeBtn.setOnAction(e -> toggleMaximize());

        Button closeBtn = createWindowButton("\u2715", "close-button");
        closeBtn.setOnAction(e -> stage.close());

        getChildren().addAll(title, spacer, minimizeBtn, maximizeBtn, closeBtn);

        WindowDragHandler.install(this, stage);

        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                toggleMaximize();
            }
        });
    }

    private Button createWindowButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.getStyleClass().addAll("window-button", styleClass);
        btn.setFocusTraversable(false);
        return btn;
    }

    public void setDirtyIndicator(boolean dirty) {
        titleLabel.setText(dirty ? "Liza Text Editor *" : "Liza Text Editor");
    }

    private void toggleMaximize() {
        if (maximized) {
            stage.setX(restoreX);
            stage.setY(restoreY);
            stage.setWidth(restoreW);
            stage.setHeight(restoreH);
            maximized = false;
        } else {
            restoreX = stage.getX();
            restoreY = stage.getY();
            restoreW = stage.getWidth();
            restoreH = stage.getHeight();

            var screen = javafx.stage.Screen.getPrimary().getVisualBounds();
            stage.setX(screen.getMinX());
            stage.setY(screen.getMinY());
            stage.setWidth(screen.getWidth());
            stage.setHeight(screen.getHeight());
            maximized = true;
        }
    }
}
