package kz.arianwait.gametexteditor.parser;

import kz.arianwait.gametexteditor.model.EditorButton;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ButtonsParser {

    public static List<EditorButton> parse(File xmlFile) {
        List<EditorButton> buttons = new ArrayList<>();
        if (xmlFile == null || !xmlFile.exists()) return buttons;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList nodes = doc.getElementsByTagName("button");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                EditorButton btn = new EditorButton();
                btn.setId(el.getAttribute("id"));
                btn.setContext(el.getAttribute("context"));
                btn.setText(el.getAttribute("text"));
                buttons.add(btn);
            }
        } catch (Exception e) {
            System.err.println("Error parsing Buttons XML: " + e.getMessage());
        }
        return buttons;
    }
}
