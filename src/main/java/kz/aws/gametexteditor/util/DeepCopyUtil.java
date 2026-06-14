package kz.aws.gametexteditor.util;

import kz.aws.gametexteditor.model.*;

import java.util.ArrayList;
import java.util.List;

public class DeepCopyUtil {

    public static EditorScene copyScene(EditorScene src) {
        EditorScene copy = new EditorScene();
        copy.setId(src.getId());
        copy.setNextSceneId(src.getNextSceneId());
        copy.setBackground(src.getBackground());
        copy.setMusic(src.getMusic());
        copy.setTag(src.getTag());
        List<EditorFrame> frames = new ArrayList<>();
        for (EditorFrame f : src.getFrames()) {
            frames.add(copyFrame(f));
        }
        copy.setFrames(frames);
        return copy;
    }

    public static EditorFrame copyFrame(EditorFrame src) {
        EditorFrame copy = new EditorFrame();
        copy.setType(src.getType());
        copy.setSpeakerName(src.getSpeakerName());
        copy.setSpeakerColor(src.getSpeakerColor());
        copy.setText(src.getText());
        copy.setStyle(src.getStyle());
        copy.setTag(src.getTag());

        List<EditorCommand> cmds = new ArrayList<>();
        for (EditorCommand c : src.getCommands()) {
            cmds.add(copyCommand(c));
        }
        copy.setCommands(cmds);

        if (src.getChoices() != null) {
            List<EditorChoice> choices = new ArrayList<>();
            for (EditorChoice ch : src.getChoices()) {
                choices.add(copyChoice(ch));
            }
            copy.setChoices(choices);
        }
        return copy;
    }

    public static EditorCommand copyCommand(EditorCommand src) {
        EditorCommand copy = new EditorCommand();
        copy.setType(src.getType());
        copy.setAction(src.getAction());
        copy.setTarget(src.getTarget());
        copy.setValue(src.getValue());
        copy.setId(src.getId());
        copy.setFlag(src.getFlag());
        copy.setKey(src.getKey());
        copy.setPrompt(src.getPrompt());
        copy.setOnSuccess(src.getOnSuccess());
        copy.setOnFailure(src.getOnFailure());
        return copy;
    }

    public static EditorChoice copyChoice(EditorChoice src) {
        EditorChoice copy = new EditorChoice();
        copy.setText(src.getText());
        copy.setRequestsId(src.getRequestsId());
        copy.setId(src.getId());
        copy.setDescription(src.getDescription());
        copy.setAddChoice(src.getAddChoice());
        copy.setRemoveChoice(src.getRemoveChoice());
        copy.setCharacterChoice(src.getCharacterChoice());
        copy.setMinRep(src.getMinRep());
        copy.setMaxRep(src.getMaxRep());
        return copy;
    }
}
