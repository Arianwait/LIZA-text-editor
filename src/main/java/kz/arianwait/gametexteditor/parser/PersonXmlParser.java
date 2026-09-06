package kz.arianwait.gametexteditor.parser;

import kz.arianwait.gametexteditor.model.EditorCharacter;
import kz.arianwait.gametexteditor.model.EditorPose;
import kz.arianwait.gametexteditor.model.EditorVariable;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PersonXmlParser {

    public static List<EditorCharacter> parse(File xmlFile) {
        List<EditorCharacter> characters = new ArrayList<>();
        if (xmlFile == null || !xmlFile.exists()) return characters;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList persons = doc.getElementsByTagName("Person");
            for (int i = 0; i < persons.getLength(); i++) {
                Element personEl = (Element) persons.item(i);
                EditorCharacter ch = new EditorCharacter();
                ch.setName(personEl.getAttribute("name"));
                ch.setColor(personEl.getAttribute("Color"));

                List<EditorPose> poses = new ArrayList<>();
                NodeList poseNodes = personEl.getElementsByTagName("Pose");
                for (int j = 0; j < poseNodes.getLength(); j++) {
                    Element poseEl = (Element) poseNodes.item(j);
                    poses.add(new EditorPose(
                            poseEl.getAttribute("name"),
                            poseEl.getAttribute("src")
                    ));
                }
                ch.setPoses(poses);
                characters.add(ch);
            }
        } catch (Exception e) {
            System.err.println("Error parsing Person XML: " + e.getMessage());
        }
        return characters;
    }

    /**
     * Parses {@code <Variable>} elements from the Person XML file.
     * Each variable has a key and an associated display color.
     *
     * @param xmlFile the Person.xml file
     * @return list of parsed variables, empty if file is missing or has none
     */
    public static List<EditorVariable> parseVariables(File xmlFile) {
        List<EditorVariable> variables = new ArrayList<>();
        if (xmlFile == null || !xmlFile.exists()) return variables;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList nodes = doc.getElementsByTagName("Variable");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                String key = el.getAttribute("key");
                String color = el.getAttribute("color");
                if (key != null && !key.isEmpty()) {
                    variables.add(new EditorVariable(key, color));
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing Variables from Person XML: " + e.getMessage());
        }
        return variables;
    }
}
