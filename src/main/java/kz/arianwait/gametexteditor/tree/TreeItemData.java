package kz.arianwait.gametexteditor.tree;

import kz.arianwait.gametexteditor.model.*;

public class TreeItemData {

    public enum ItemType { ROOT, SECTION, SCENE, FRAME, CHARACTER, POSE, CHAPTER, THEME, BUTTON }

    private final ItemType itemType;
    private final String displayText;
    private final int sceneId;
    private final int frameIndex;
    private final EditorScene scene;
    private final EditorFrame frame;

    private TreeItemData(ItemType type, String text, int sceneId, int frameIndex,
                         EditorScene scene, EditorFrame frame) {
        this.itemType = type;
        this.displayText = text;
        this.sceneId = sceneId;
        this.frameIndex = frameIndex;
        this.scene = scene;
        this.frame = frame;
    }

    public static TreeItemData root(String text) {
        return new TreeItemData(ItemType.ROOT, text, -1, -1, null, null);
    }

    public static TreeItemData section(String text) {
        return new TreeItemData(ItemType.SECTION, text, -1, -1, null, null);
    }

    public static TreeItemData forScene(EditorScene scene) {
        String speaker = scene.getFirstSpeaker();
        String suffix = speaker.isEmpty() ? "" : " — " + speaker;
        String text = "Сцена " + scene.getId() + suffix
                + " (" + scene.getFrameCount() + " кадров)";
        return new TreeItemData(ItemType.SCENE, text, scene.getId(), -1, scene, null);
    }

    public static TreeItemData forFrame(int sceneId, int index, EditorFrame frame) {
        String prefix;
        if (frame.getType() == EditorFrame.FrameType.OVERLAY) {
            prefix = "[Overlay]";
        } else {
            prefix = frame.getSpeakerName().isEmpty() ? "[???]" : frame.getSpeakerName();
        }
        String text = frame.getText();
        if (text.length() > 40) text = text.substring(0, 40) + "...";
        return new TreeItemData(ItemType.FRAME, prefix + ": " + text, sceneId, index, null, frame);
    }

    public static TreeItemData forCharacter(EditorCharacter ch) {
        String text = ch.getName() + " (" + ch.getPoseCount() + " поз)";
        return new TreeItemData(ItemType.CHARACTER, text, -1, -1, null, null);
    }

    public static TreeItemData forPose(EditorPose pose) {
        return new TreeItemData(ItemType.POSE, pose.getName() + " → " + pose.getSpritePath(), -1, -1, null, null);
    }

    public static TreeItemData forChapter(EditorChapter chapter) {
        return new TreeItemData(ItemType.CHAPTER,
                chapter.getName() + " (сцена " + chapter.getSceneId() + ")",
                chapter.getSceneId(), -1, null, null);
    }

    public static TreeItemData forTheme(EditorTheme theme) {
        return new TreeItemData(ItemType.THEME, theme.getName() + " [" + theme.getId() + "]", -1, -1, null, null);
    }

    public static TreeItemData forButton(EditorButton btn) {
        return new TreeItemData(ItemType.BUTTON,
                btn.getText() + " (" + btn.getContext() + ") [" + btn.getId() + "]",
                -1, -1, null, null);
    }

    public ItemType getItemType() { return itemType; }
    public String getDisplayText() { return displayText; }
    public int getSceneId() { return sceneId; }
    public int getFrameIndex() { return frameIndex; }
    public EditorScene getScene() { return scene; }
    public EditorFrame getFrame() { return frame; }
    public boolean isScene() { return itemType == ItemType.SCENE; }
    public boolean isFrame() { return itemType == ItemType.FRAME; }
    public boolean isChapter() { return itemType == ItemType.CHAPTER; }

    public String getTag() {
        if (scene != null) return scene.getTag();
        if (frame != null) return frame.getTag();
        return "";
    }

    @Override
    public String toString() { return displayText; }
}
