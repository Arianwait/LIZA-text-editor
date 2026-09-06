package kz.arianwait.gametexteditor.model;

import java.io.Serializable;

/**
 * Represents a named variable (from input commands) with an associated display color.
 * This allows speaker names like {@code {playerName}} to be styled automatically.
 */
public class EditorVariable implements Serializable {

    private static final long serialVersionUID = 1L;

    private String key;
    private String color;

    public EditorVariable() {}

    public EditorVariable(String key, String color) {
        this.key = key;
        this.color = color;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
