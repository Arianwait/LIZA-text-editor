package kz.arianwait.gametexteditor.graph;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import kz.arianwait.gametexteditor.model.EditorScene;

public class SceneNode extends VBox {

    public static final double NODE_WIDTH = 180;

    private final EditorScene scene;
    private final DoubleProperty centerXProp = new SimpleDoubleProperty();
    private final DoubleProperty centerYProp = new SimpleDoubleProperty();
    private boolean selected = false;

    private double dragStartX, dragStartY;
    private double dragOffsetX, dragOffsetY;

    public SceneNode(EditorScene scene) {
        this.scene = scene;
        setPrefWidth(NODE_WIDTH);
        setMinWidth(NODE_WIDTH);
        setMaxWidth(NODE_WIDTH);
        setPadding(new Insets(8, 12, 8, 12));
        setSpacing(3);
        setAlignment(Pos.TOP_LEFT);
        getStyleClass().add("scene-node");

        if (scene.getId() == 1) {
            getStyleClass().add("start-node");
        }
        if (scene.isEndScene()) {
            getStyleClass().add("end-node");
        }
        if (scene.hasChoices()) {
            getStyleClass().add("branch-node");
        }

        Label idLabel = new Label("Сцена " + scene.getId());
        idLabel.getStyleClass().add("scene-node-id");

        String speaker = scene.getFirstSpeaker();
        Label speakerLabel = new Label(speaker.isEmpty() ? "—" : truncate(speaker, 22));
        speakerLabel.getStyleClass().add("scene-node-speaker");

        HBox infoRow = new HBox(6);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        Label frameCount = new Label(scene.getFrameCount() + " кадров");
        frameCount.getStyleClass().add("scene-node-info");
        infoRow.getChildren().add(frameCount);

        if (scene.hasChoices()) {
            Label branchBadge = new Label("\u2726 ветвление");
            branchBadge.getStyleClass().add("scene-node-badge-branch");
            infoRow.getChildren().add(branchBadge);
        }
        if (scene.isEndScene()) {
            Label endBadge = new Label("\u25CF конец");
            endBadge.getStyleClass().add("scene-node-badge-end");
            infoRow.getChildren().add(endBadge);
        }

        getChildren().addAll(idLabel, speakerLabel, infoRow);

        // Tooltip
        StringBuilder tip = new StringBuilder();
        tip.append("Сцена ").append(scene.getId());
        if (scene.getNextSceneId() > 0) tip.append(" \u2192 ").append(scene.getNextSceneId());
        tip.append("\n").append(scene.getFrameCount()).append(" кадров");
        if (!scene.getBackground().isEmpty()) tip.append("\nФон: ").append(scene.getBackground());
        Tooltip.install(this, new Tooltip(tip.toString()));

        // Drag
        setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown()) {
                dragStartX = getLayoutX();
                dragStartY = getLayoutY();
                dragOffsetX = e.getSceneX();
                dragOffsetY = e.getSceneY();
                toFront();
                e.consume();
            }
        });
        setOnMouseDragged(e -> {
            if (e.isPrimaryButtonDown()) {
                double newX = dragStartX + (e.getSceneX() - dragOffsetX);
                double newY = dragStartY + (e.getSceneY() - dragOffsetY);
                setLayoutX(newX);
                setLayoutY(newY);
                e.consume();
            }
        });

        // Bind center properties
        layoutXProperty().addListener((o, ov, nv) ->
                centerXProp.set(nv.doubleValue() + NODE_WIDTH / 2));
        layoutYProperty().addListener((o, ov, nv) ->
                centerYProp.set(nv.doubleValue() + getHeight() / 2));
        heightProperty().addListener((o, ov, nv) ->
                centerYProp.set(getLayoutY() + nv.doubleValue() / 2));
    }

    public EditorScene getEditorScene() { return scene; }
    public int getSceneId() { return scene.getId(); }

    public DoubleProperty centerXProp() { return centerXProp; }
    public DoubleProperty centerYProp() { return centerYProp; }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            if (!getStyleClass().contains("selected")) getStyleClass().add("selected");
        } else {
            getStyleClass().remove("selected");
        }
    }

    public boolean isSelected() { return selected; }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
