package kz.aws.gametexteditor.model;

import java.io.Serializable;

public class SceneEdge implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum EdgeType {
        NEXT_SCENE, CHOICE, PUZZLE_SUCCESS, PUZZLE_FAILURE
    }

    private final int sourceId;
    private final int targetId;
    private final EdgeType type;
    private final String label;

    public SceneEdge(int sourceId, int targetId, EdgeType type, String label) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.type = type;
        this.label = label;
    }

    public int getSourceId() { return sourceId; }
    public int getTargetId() { return targetId; }
    public EdgeType getType() { return type; }
    public String getLabel() { return label; }
}
