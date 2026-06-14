package kz.aws.gametexteditor.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EditorFrame implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum FrameType { CHARACTER, OVERLAY }

    private FrameType type;
    private String speakerName = "";
    private String speakerColor = "";
    private String text = "";
    private String style;
    private List<EditorCommand> commands = new ArrayList<>();
    private List<EditorChoice> choices;
    private String tag = "";

    public FrameType getType() { return type; }
    public void setType(FrameType type) { this.type = type; }

    public String getSpeakerName() { return speakerName; }
    public void setSpeakerName(String speakerName) { this.speakerName = speakerName; }

    public String getSpeakerColor() { return speakerColor; }
    public void setSpeakerColor(String speakerColor) { this.speakerColor = speakerColor; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public List<EditorCommand> getCommands() { return commands; }
    public void setCommands(List<EditorCommand> commands) { this.commands = commands; }

    public List<EditorChoice> getChoices() { return choices; }
    public void setChoices(List<EditorChoice> choices) { this.choices = choices; }

    public boolean hasChoices() {
        return choices != null && !choices.isEmpty();
    }

    public String getTag() { return tag != null ? tag : ""; }
    public void setTag(String tag) { this.tag = tag; }
}
