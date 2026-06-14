package kz.aws.gametexteditor.model;

import java.io.Serializable;

public class EditorTheme implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String background;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBackground() { return background; }
    public void setBackground(String background) { this.background = background; }
}
