package kz.aws.gametexteditor.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EditorScene implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int nextSceneId = -1;
    private String background = "";
    private String music = "";
    private List<EditorFrame> frames = new ArrayList<>();
    private String tag = "";

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getNextSceneId() { return nextSceneId; }
    public void setNextSceneId(int nextSceneId) { this.nextSceneId = nextSceneId; }

    public String getBackground() { return background; }
    public void setBackground(String background) { this.background = background; }

    public String getMusic() { return music; }
    public void setMusic(String music) { this.music = music; }

    public List<EditorFrame> getFrames() { return frames; }
    public void setFrames(List<EditorFrame> frames) { this.frames = frames; }

    public int getFrameCount() { return frames.size(); }

    public String getFirstSpeaker() {
        for (EditorFrame frame : frames) {
            if (frame.getType() == EditorFrame.FrameType.CHARACTER
                    && frame.getSpeakerName() != null
                    && !frame.getSpeakerName().isBlank()) {
                return frame.getSpeakerName();
            }
        }
        return "";
    }

    public boolean hasChoices() {
        return frames.stream().anyMatch(EditorFrame::hasChoices);
    }

    public List<EditorChoice> getAllChoices() {
        List<EditorChoice> all = new ArrayList<>();
        for (EditorFrame frame : frames) {
            if (frame.hasChoices()) {
                all.addAll(frame.getChoices());
            }
        }
        return all;
    }

    public boolean isEndScene() {
        return nextSceneId <= 0 && !hasChoices();
    }

    public String getTag() { return tag != null ? tag : ""; }
    public void setTag(String tag) { this.tag = tag; }
}
