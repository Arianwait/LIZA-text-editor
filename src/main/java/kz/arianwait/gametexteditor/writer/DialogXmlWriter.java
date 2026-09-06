package kz.arianwait.gametexteditor.writer;

import kz.arianwait.gametexteditor.model.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.List;

public class DialogXmlWriter {

    public static void write(List<EditorScene> scenes, File outputFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        doc.setXmlStandalone(true);

        Element root = doc.createElement("game");
        doc.appendChild(root);

        for (EditorScene scene : scenes) {
            root.appendChild(writeDialog(doc, scene));
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.transform(new DOMSource(doc), new StreamResult(outputFile));
    }

    private static Element writeDialog(Document doc, EditorScene scene) {
        Element dialog = doc.createElement("dialog");
        dialog.setAttribute("id", String.valueOf(scene.getId()));
        if (scene.getNextSceneId() > 0) {
            dialog.setAttribute("nextScene", String.valueOf(scene.getNextSceneId()));
        }
        if (!scene.getBackground().isEmpty()) {
            dialog.setAttribute("background", scene.getBackground());
        }
        if (!scene.getMusic().isEmpty()) {
            dialog.setAttribute("music", scene.getMusic());
        }
        if (!scene.getTag().isEmpty()) {
            dialog.setAttribute("tag", scene.getTag());
        }

        for (EditorFrame frame : scene.getFrames()) {
            if (frame.getType() == EditorFrame.FrameType.OVERLAY) {
                dialog.appendChild(writeOverlay(doc, frame));
            } else {
                dialog.appendChild(writeCharacter(doc, frame));
            }
        }
        return dialog;
    }

    private static Element writeOverlay(Document doc, EditorFrame frame) {
        Element overlay = doc.createElement("overlay");
        overlay.setAttribute("text", frame.getText());
        if (!frame.getTag().isEmpty()) {
            overlay.setAttribute("tag", frame.getTag());
        }
        return overlay;
    }

    private static Element writeCharacter(Document doc, EditorFrame frame) {
        Element character = doc.createElement("character");
        if (!frame.getSpeakerName().isEmpty()) {
            character.setAttribute("name", frame.getSpeakerName());
        }
        if (!frame.getSpeakerColor().isEmpty()) {
            character.setAttribute("color", frame.getSpeakerColor());
        }
        if (frame.getStyle() != null && !frame.getStyle().isEmpty()) {
            character.setAttribute("style", frame.getStyle());
        }
        if (!frame.getTag().isEmpty()) {
            character.setAttribute("tag", frame.getTag());
        }

        // Commands before text
        for (EditorCommand cmd : frame.getCommands()) {
            character.appendChild(writeCommand(doc, cmd));
        }

        // Text content
        if (!frame.getText().isEmpty()) {
            character.appendChild(doc.createTextNode("\n        " + frame.getText() + "\n    "));
        }

        // Choices
        if (frame.hasChoices()) {
            character.appendChild(writeChoices(doc, frame.getChoices()));
        }

        return character;
    }

    private static Element writeCommand(Document doc, EditorCommand cmd) {
        Element el = doc.createElement("command");
        setIfNotEmpty(el, "type", cmd.getType());
        setIfNotEmpty(el, "action", cmd.getAction());
        setIfNotEmpty(el, "target", cmd.getTarget());
        setIfNotEmpty(el, "value", cmd.getValue());
        setIfNotEmpty(el, "id", cmd.getId());
        setIfNotEmpty(el, "flag", cmd.getFlag());
        setIfNotEmpty(el, "key", cmd.getKey());
        setIfNotEmpty(el, "prompt", cmd.getPrompt());
        setIfNotEmpty(el, "effect", cmd.getEffect());
        setIfNotEmpty(el, "filter", cmd.getFilter());
        if (cmd.getOnSuccess() >= 0) {
            el.setAttribute("onSuccess", String.valueOf(cmd.getOnSuccess()));
        }
        if (cmd.getOnFailure() >= 0) {
            el.setAttribute("onFailure", String.valueOf(cmd.getOnFailure()));
        }
        if (cmd.getDuration() > 0) {
            el.setAttribute("duration", String.valueOf(cmd.getDuration()));
        }
        if (cmd.getStartScale() >= 0) {
            el.setAttribute("startScale", String.valueOf(cmd.getStartScale()));
        }
        if (cmd.getEndScale() >= 0) {
            el.setAttribute("endScale", String.valueOf(cmd.getEndScale()));
        }
        if (cmd.getIntensity() >= 0) {
            el.setAttribute("intensity", String.valueOf(cmd.getIntensity()));
        }
        return el;
    }

    private static Element writeChoices(Document doc, List<EditorChoice> choices) {
        Element choiceEl = doc.createElement("choice");
        for (EditorChoice ch : choices) {
            Element option = doc.createElement("option");
            option.setAttribute("text", ch.getText());
            option.setAttribute("requestsID", String.valueOf(ch.getRequestsId()));
            setIfNotEmpty(option, "id", ch.getId());
            setIfNotEmpty(option, "description", ch.getDescription());
            setIfNotEmpty(option, "addChoice", ch.getAddChoice());
            setIfNotEmpty(option, "removeChoice", ch.getRemoveChoice());
            setIfNotEmpty(option, "CharacterChoice", ch.getCharacterChoice());
            option.setAttribute("minRep", String.valueOf(ch.getMinRep()));
            option.setAttribute("maxRep", String.valueOf(ch.getMaxRep()));
            choiceEl.appendChild(option);
        }
        return choiceEl;
    }

    private static void setIfNotEmpty(Element el, String attr, String value) {
        if (value != null && !value.isEmpty()) {
            el.setAttribute(attr, value);
        }
    }
}
