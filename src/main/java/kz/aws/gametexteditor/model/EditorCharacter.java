package kz.aws.gametexteditor.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EditorCharacter implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String color;
    private List<EditorPose> poses = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public List<EditorPose> getPoses() { return poses; }
    public void setPoses(List<EditorPose> poses) { this.poses = poses; }

    public int getPoseCount() { return poses.size(); }

    public List<String> getPoseNames() {
        return poses.stream().map(EditorPose::getName).toList();
    }
}
