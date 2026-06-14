package kz.aws.gametexteditor.parser;

import kz.aws.gametexteditor.model.EditorChapter;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ChaptersParser {

    public static List<EditorChapter> parse(File xmlFile) {
        List<EditorChapter> chapters = new ArrayList<>();
        if (xmlFile == null || !xmlFile.exists()) return chapters;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList nodes = doc.getElementsByTagName("chapter");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                EditorChapter ch = new EditorChapter();
                ch.setId(intAttr(el, "id", i + 1));
                ch.setName(el.getAttribute("name"));
                ch.setSceneId(intAttr(el, "sceneId", -1));
                chapters.add(ch);
            }
        } catch (Exception e) {
            System.err.println("Error parsing Chapters XML: " + e.getMessage());
        }
        return chapters;
    }

    private static int intAttr(Element el, String name, int def) {
        String val = el.getAttribute(name);
        if (val == null || val.isEmpty()) return def;
        try { return Integer.parseInt(val); }
        catch (NumberFormatException e) { return def; }
    }
}
