package kz.aws.gametexteditor.graph;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;
import kz.aws.gametexteditor.model.EditorProject;
import kz.aws.gametexteditor.model.EditorScene;
import kz.aws.gametexteditor.model.SceneEdge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class GraphPane extends ScrollPane {

    private final Pane canvas;
    private final Group canvasGroup;
    private final Scale scaleTransform;
    private double zoomLevel = 1.0;

    private Map<Integer, SceneNode> nodeMap = new HashMap<>();
    private List<EdgeLine> edgeLines = new ArrayList<>();
    private Consumer<Integer> onSceneSelected;
    private Consumer<Integer> onSceneDoubleClicked;

    public GraphPane() {
        canvas = new Pane();
        canvas.getStyleClass().add("graph-canvas");
        canvas.setMinSize(2000, 2000);

        scaleTransform = new Scale(1, 1, 0, 0);
        canvas.getTransforms().add(scaleTransform);

        canvasGroup = new Group(canvas);

        setContent(canvasGroup);
        setPannable(true);
        setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        getStyleClass().add("graph-scroll-pane");
        setFitToWidth(false);
        setFitToHeight(false);

        // Zoom with scroll wheel
        canvasGroup.setOnScroll(e -> {
            if (e.isControlDown()) {
                double factor = e.getDeltaY() > 0 ? 1.1 : 0.9;
                zoom(factor);
                e.consume();
            }
        });
    }

    public void loadProject(EditorProject project) {
        canvas.getChildren().clear();
        nodeMap.clear();
        edgeLines.clear();

        // Create nodes
        for (EditorScene scene : project.getScenes()) {
            SceneNode node = new SceneNode(scene);
            nodeMap.put(scene.getId(), node);

            node.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    if (onSceneDoubleClicked != null) {
                        onSceneDoubleClicked.accept(scene.getId());
                    }
                    e.consume();
                } else if (e.getClickCount() == 1) {
                    selectScene(scene.getId());
                    e.consume();
                }
            });
        }

        // Layout
        GraphLayoutEngine.layout(project.getScenes(), project.getEdges(), nodeMap);

        // Create edges (add before nodes so nodes render on top)
        for (SceneEdge edge : project.getEdges()) {
            SceneNode source = nodeMap.get(edge.getSourceId());
            SceneNode target = nodeMap.get(edge.getTargetId());
            if (source != null && target != null) {
                EdgeLine edgeLine = new EdgeLine(source, target, edge);
                edgeLines.add(edgeLine);
                canvas.getChildren().add(edgeLine);
            }
        }

        // Add nodes on top
        canvas.getChildren().addAll(nodeMap.values());

        // Adjust canvas size
        adjustCanvasSize();
    }

    private void adjustCanvasSize() {
        double maxX = 0, maxY = 0;
        for (SceneNode node : nodeMap.values()) {
            maxX = Math.max(maxX, node.getLayoutX() + SceneNode.NODE_WIDTH + 100);
            maxY = Math.max(maxY, node.getLayoutY() + 150);
        }
        canvas.setMinSize(Math.max(2000, maxX), Math.max(2000, maxY));
    }

    public void zoom(double factor) {
        zoomLevel *= factor;
        zoomLevel = Math.max(0.2, Math.min(3.0, zoomLevel));
        scaleTransform.setX(zoomLevel);
        scaleTransform.setY(zoomLevel);
    }

    public void selectScene(int sceneId) {
        for (SceneNode node : nodeMap.values()) {
            node.setSelected(node.getSceneId() == sceneId);
        }
        if (onSceneSelected != null) {
            onSceneSelected.accept(sceneId);
        }
    }

    public void scrollToScene(int sceneId) {
        SceneNode node = nodeMap.get(sceneId);
        if (node == null) return;

        selectScene(sceneId);

        Bounds viewportBounds = getViewportBounds();
        double contentWidth = canvas.getMinWidth() * zoomLevel;
        double contentHeight = canvas.getMinHeight() * zoomLevel;

        double nodeX = node.getLayoutX() * zoomLevel;
        double nodeY = node.getLayoutY() * zoomLevel;

        double hValue = (nodeX - viewportBounds.getWidth() / 2) / (contentWidth - viewportBounds.getWidth());
        double vValue = (nodeY - viewportBounds.getHeight() / 2) / (contentHeight - viewportBounds.getHeight());

        setHvalue(Math.max(0, Math.min(1, hValue)));
        setVvalue(Math.max(0, Math.min(1, vValue)));
    }

    public void setOnSceneSelected(Consumer<Integer> handler) {
        this.onSceneSelected = handler;
    }

    public void setOnSceneDoubleClicked(Consumer<Integer> handler) {
        this.onSceneDoubleClicked = handler;
    }
}
