package kz.aws.gametexteditor.model;

import java.io.Serializable;

public class EditorPose implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String spritePath;

    public EditorPose() {}

    public EditorPose(String name, String spritePath) {
        this.name = name;
        this.spritePath = spritePath;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpritePath() { return spritePath; }
    public void setSpritePath(String spritePath) { this.spritePath = spritePath; }
}
