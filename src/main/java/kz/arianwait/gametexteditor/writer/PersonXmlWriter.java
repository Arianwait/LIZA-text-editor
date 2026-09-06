package kz.arianwait.gametexteditor.writer;

import kz.arianwait.gametexteditor.model.EditorCharacter;
import kz.arianwait.gametexteditor.model.EditorPose;
import kz.arianwait.gametexteditor.model.EditorVariable;
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

public class PersonXmlWriter {

    /**
     * Writes characters and variables to the Person XML file.
     *
     * @param characters list of characters to write
     * @param variables list of variables with color bindings
     * @param outputFile the target XML file
     * @throws Exception if XML writing fails
     */
    public static void write(List<EditorCharacter> characters, List<EditorVariable> variables,
                             File outputFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        doc.setXmlStandalone(true);

        Element root = doc.createElement("Persons");
        doc.appendChild(root);

        for (EditorCharacter ch : characters) {
            root.appendChild(writePerson(doc, ch));
        }
        for (EditorVariable v : variables) {
            root.appendChild(writeVariable(doc, v));
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.transform(new DOMSource(doc), new StreamResult(outputFile));
    }

    /**
     * Backward-compatible overload without variables.
     *
     * @param characters list of characters to write
     * @param outputFile the target XML file
     * @throws Exception if XML writing fails
     */
    public static void write(List<EditorCharacter> characters, File outputFile) throws Exception {
        write(characters, List.of(), outputFile);
    }

    private static Element writePerson(Document doc, EditorCharacter ch) {
        Element person = doc.createElement("Person");
        person.setAttribute("name", ch.getName());
        if (ch.getColor() != null && !ch.getColor().isEmpty()) {
            person.setAttribute("Color", ch.getColor());
        }

        for (EditorPose pose : ch.getPoses()) {
            Element poseEl = doc.createElement("Pose");
            poseEl.setAttribute("name", pose.getName());
            poseEl.setAttribute("src", pose.getSpritePath());
            person.appendChild(poseEl);
        }

        return person;
    }

    /**
     * Creates a {@code <Variable>} element with key and color attributes.
     *
     * @param doc the XML document
     * @param v the variable to serialize
     * @return the Variable element
     */
    private static Element writeVariable(Document doc, EditorVariable v) {
        Element el = doc.createElement("Variable");
        el.setAttribute("key", v.getKey());
        if (v.getColor() != null && !v.getColor().isEmpty()) {
            el.setAttribute("color", v.getColor());
        }
        return el;
    }
}
