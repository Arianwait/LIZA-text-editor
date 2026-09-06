package kz.arianwait.gametexteditor.model;

import java.io.Serializable;

public class EditorButton implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String context;
    private String text;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
