package kz.arianwait.gametexteditor.util;

import kz.arianwait.gametexteditor.model.EditorFrame;

import java.io.*;
import java.util.*;

public class FrameTemplateManager {

    private static final File TEMPLATES_DIR = new File(
            System.getProperty("user.home"), ".liza-editor/templates");

    public static void saveTemplate(String name, EditorFrame frame) {
        TEMPLATES_DIR.mkdirs();
        File file = new File(TEMPLATES_DIR, sanitize(name) + ".dat");
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(file))) {
            oos.writeObject(DeepCopyUtil.copyFrame(frame));
        } catch (Exception e) {
            System.err.println("Template save failed: " + e.getMessage());
        }
    }

    public static EditorFrame loadTemplate(String name) {
        File file = new File(TEMPLATES_DIR, sanitize(name) + ".dat");
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(file))) {
            return (EditorFrame) ois.readObject();
        } catch (Exception e) {
            System.err.println("Template load failed: " + e.getMessage());
            return null;
        }
    }

    public static List<String> listTemplates() {
        if (!TEMPLATES_DIR.exists()) return Collections.emptyList();
        File[] files = TEMPLATES_DIR.listFiles((d, n) -> n.endsWith(".dat"));
        if (files == null) return Collections.emptyList();
        return Arrays.stream(files)
                .map(f -> f.getName().replace(".dat", ""))
                .sorted()
                .toList();
    }

    public static void deleteTemplate(String name) {
        new File(TEMPLATES_DIR, sanitize(name) + ".dat").delete();
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9\u0400-\u04FF_\\- ]", "_");
    }
}
