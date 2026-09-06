package kz.arianwait.gametexteditor.writer;

import kz.arianwait.gametexteditor.model.EditorChapter;
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

public class ChaptersXmlWriter {

    public static void write(List<EditorChapter> chapters, File outputFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        doc.setXmlStandalone(true);

        Element root = doc.createElement("chapters");
        doc.appendChild(root);

        for (EditorChapter ch : chapters) {
            Element chapterEl = doc.createElement("chapter");
            chapterEl.setAttribute("id", String.valueOf(ch.getId()));
            chapterEl.setAttribute("name", ch.getName());
            chapterEl.setAttribute("sceneId", String.valueOf(ch.getSceneId()));
            root.appendChild(chapterEl);
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.transform(new DOMSource(doc), new StreamResult(outputFile));
    }
}
