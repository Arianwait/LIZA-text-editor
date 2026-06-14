package kz.aws.gametexteditor.parser;

import kz.aws.gametexteditor.model.EditorChoice;
import kz.aws.gametexteditor.model.EditorCommand;
import kz.aws.gametexteditor.model.EditorFrame;
import kz.aws.gametexteditor.model.EditorScene;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DialogXmlParser {

    public static List<EditorScene> parse(File xmlFile) {
        List<EditorScene> scenes = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList dialogs = doc.getElementsByTagName("dialog");
            for (int i = 0; i < dialogs.getLength(); i++) {
                Element dialogEl = (Element) dialogs.item(i);
                scenes.add(parseDialog(dialogEl));
            }
        } catch (Exception e) {
            System.err.println("Error parsing Dialog XML: " + e.getMessage());
        }
        return scenes;
    }

    private static EditorScene parseDialog(Element dialogEl) {
        EditorScene scene = new EditorScene();
        scene.setId(intAttr(dialogEl, "id", -1));
        scene.setNextSceneId(intAttr(dialogEl, "nextScene", -1));
        scene.setBackground(dialogEl.getAttribute("background"));
        scene.setMusic(dialogEl.getAttribute("music"));
        if (dialogEl.hasAttribute("tag")) {
            scene.setTag(dialogEl.getAttribute("tag"));
        }

        List<EditorFrame> frames = new ArrayList<>();
        NodeList children = dialogEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;

            Element el = (Element) child;
            switch (el.getTagName()) {
                case "overlay" -> frames.add(parseOverlay(el));
                case "character" -> frames.add(parseCharacter(el));
            }
        }
        scene.setFrames(frames);
        return scene;
    }

    private static EditorFrame parseOverlay(Element el) {
        EditorFrame frame = new EditorFrame();
        frame.setType(EditorFrame.FrameType.OVERLAY);
        frame.setText(el.getAttribute("text"));
        if (el.hasAttribute("tag")) frame.setTag(el.getAttribute("tag"));
        return frame;
    }

    private static EditorFrame parseCharacter(Element el) {
        EditorFrame frame = new EditorFrame();
        frame.setType(EditorFrame.FrameType.CHARACTER);
        frame.setSpeakerName(el.getAttribute("name"));
        frame.setSpeakerColor(el.getAttribute("color"));
        if (el.hasAttribute("style")) {
            frame.setStyle(el.getAttribute("style"));
        }
        if (el.hasAttribute("tag")) {
            frame.setTag(el.getAttribute("tag"));
        }

        List<EditorCommand> commands = new ArrayList<>();
        List<EditorChoice> choices = null;
        StringBuilder textBuilder = new StringBuilder();

        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                textBuilder.append(child.getTextContent());
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childEl = (Element) child;
                switch (childEl.getTagName()) {
                    case "command" -> commands.add(parseCommand(childEl));
                    case "choice" -> choices = parseChoices(childEl);
                    case "clue" -> textBuilder.append(childEl.getTextContent());
                }
            }
        }

        frame.setText(textBuilder.toString().trim().replaceAll("\\s+", " "));
        frame.setCommands(commands);
        frame.setChoices(choices);
        return frame;
    }

    private static EditorCommand parseCommand(Element el) {
        EditorCommand cmd = new EditorCommand();
        cmd.setType(el.getAttribute("type"));
        cmd.setAction(el.getAttribute("action"));
        cmd.setTarget(el.getAttribute("target"));
        cmd.setValue(el.getAttribute("value"));

        if (el.hasAttribute("id")) cmd.setId(el.getAttribute("id"));
        if (el.hasAttribute("flag")) cmd.setFlag(el.getAttribute("flag"));
        if (el.hasAttribute("key")) cmd.setKey(el.getAttribute("key"));
        if (el.hasAttribute("prompt")) cmd.setPrompt(el.getAttribute("prompt"));
        if (el.hasAttribute("onSuccess")) cmd.setOnSuccess(intAttr(el, "onSuccess", -1));
        if (el.hasAttribute("onFailure")) cmd.setOnFailure(intAttr(el, "onFailure", -1));
        if (el.hasAttribute("effect")) cmd.setEffect(el.getAttribute("effect"));

        return cmd;
    }

    private static List<EditorChoice> parseChoices(Element choiceEl) {
        List<EditorChoice> choices = new ArrayList<>();
        NodeList options = choiceEl.getElementsByTagName("option");
        for (int i = 0; i < options.getLength(); i++) {
            Element optEl = (Element) options.item(i);
            EditorChoice choice = new EditorChoice();
            choice.setText(optEl.getAttribute("text"));
            choice.setRequestsId(intAttr(optEl, "requestsID", -1));
            if (optEl.hasAttribute("id")) choice.setId(optEl.getAttribute("id"));
            if (optEl.hasAttribute("description")) choice.setDescription(optEl.getAttribute("description"));
            if (optEl.hasAttribute("addChoice")) choice.setAddChoice(optEl.getAttribute("addChoice"));
            if (optEl.hasAttribute("removeChoice")) choice.setRemoveChoice(optEl.getAttribute("removeChoice"));
            if (optEl.hasAttribute("CharacterChoice")) choice.setCharacterChoice(optEl.getAttribute("CharacterChoice"));
            choice.setMinRep(intAttr(optEl, "minRep", 0));
            choice.setMaxRep(intAttr(optEl, "maxRep", 100));
            choices.add(choice);
        }
        return choices;
    }

    private static int intAttr(Element el, String name, int defaultVal) {
        String val = el.getAttribute(name);
        if (val == null || val.isEmpty()) return defaultVal;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
