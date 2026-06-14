package kz.aws.gametexteditor.io;

import kz.aws.gametexteditor.model.EditorScene;
import kz.aws.gametexteditor.parser.DialogXmlParser;
import kz.aws.gametexteditor.writer.DialogXmlWriter;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class SceneExporter {

    public static void exportScenes(List<EditorScene> scenes, File outputFile) throws Exception {
        DialogXmlWriter.write(scenes, outputFile);
    }

    public static List<EditorScene> importScenes(File inputFile) {
        try {
            return DialogXmlParser.parse(inputFile);
        } catch (Exception e) {
            System.err.println("Import failed: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
