package kz.arianwait.gametexteditor.model;

import java.io.Serializable;

public class EditorChapter implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private int sceneId;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getSceneId() { return sceneId; }
    public void setSceneId(int sceneId) { this.sceneId = sceneId; }
}
