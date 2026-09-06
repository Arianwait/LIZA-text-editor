package kz.arianwait.gametexteditor.graph;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import kz.arianwait.gametexteditor.model.SceneEdge;

public class EdgeLine extends Group {

    private static final double ARROW_SIZE = 8;

    private final SceneEdge edge;

    public EdgeLine(SceneNode source, SceneNode target, SceneEdge edge) {
        this.edge = edge;
        getStyleClass().add("edge-group");

        Line line = new Line();
        line.getStyleClass().add("edge-line");

        // Bind start to source bottom-center
        DoubleBinding srcX = Bindings.createDoubleBinding(
                () -> source.getLayoutX() + SceneNode.NODE_WIDTH / 2,
                source.layoutXProperty());
        DoubleBinding srcY = Bindings.createDoubleBinding(
                () -> source.getLayoutY() + source.getHeight(),
                source.layoutYProperty(), source.heightProperty());

        // Bind end to target top-center
        DoubleBinding tgtX = Bindings.createDoubleBinding(
                () -> target.getLayoutX() + SceneNode.NODE_WIDTH / 2,
                target.layoutXProperty());
        DoubleBinding tgtY = Bindings.createDoubleBinding(
                () -> target.getLayoutY(),
                target.layoutYProperty());

        line.startXProperty().bind(srcX);
        line.startYProperty().bind(srcY);
        line.endXProperty().bind(tgtX);
        line.endYProperty().bind(tgtY);

        // Style by edge type
        switch (edge.getType()) {
            case NEXT_SCENE -> line.getStyleClass().add("edge-next");
            case CHOICE -> line.getStyleClass().add("edge-choice");
            case PUZZLE_SUCCESS -> line.getStyleClass().add("edge-success");
            case PUZZLE_FAILURE -> line.getStyleClass().add("edge-failure");
        }

        // Arrowhead
        Polygon arrow = new Polygon();
        arrow.getStyleClass().add("edge-arrow");
        switch (edge.getType()) {
            case NEXT_SCENE -> arrow.getStyleClass().add("arrow-next");
            case CHOICE -> arrow.getStyleClass().add("arrow-choice");
            case PUZZLE_SUCCESS -> arrow.getStyleClass().add("arrow-success");
            case PUZZLE_FAILURE -> arrow.getStyleClass().add("arrow-failure");
        }

        // Update arrowhead position when line changes
        Runnable updateArrow = () -> {
            double ex = line.getEndX();
            double ey = line.getEndY();
            double sx = line.getStartX();
            double sy = line.getStartY();
            double angle = Math.atan2(ey - sy, ex - sx);

            double x1 = ex - ARROW_SIZE * Math.cos(angle - Math.PI / 6);
            double y1 = ey - ARROW_SIZE * Math.sin(angle - Math.PI / 6);
            double x2 = ex - ARROW_SIZE * Math.cos(angle + Math.PI / 6);
            double y2 = ey - ARROW_SIZE * Math.sin(angle + Math.PI / 6);

            arrow.getPoints().setAll(ex, ey, x1, y1, x2, y2);
        };

        line.startXProperty().addListener((o, ov, nv) -> updateArrow.run());
        line.startYProperty().addListener((o, ov, nv) -> updateArrow.run());
        line.endXProperty().addListener((o, ov, nv) -> updateArrow.run());
        line.endYProperty().addListener((o, ov, nv) -> updateArrow.run());

        getChildren().addAll(line, arrow);

        // Label for choice edges
        if (edge.getType() == SceneEdge.EdgeType.CHOICE && edge.getLabel() != null
                && !edge.getLabel().isEmpty()) {
            Label lbl = new Label(truncate(edge.getLabel(), 18));
            lbl.getStyleClass().add("edge-label");
            lbl.layoutXProperty().bind(srcX.add(tgtX).divide(2).subtract(30));
            lbl.layoutYProperty().bind(srcY.add(tgtY).divide(2).subtract(8));
            getChildren().add(lbl);
        }
    }

    public SceneEdge getEdge() { return edge; }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
