package kz.arianwait.gametexteditor.util;

import kz.arianwait.gametexteditor.model.ResourceEntry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Scans the project directory for background images and sound files,
 * and maintains user-assigned display names persisted in
 * {@code lib/config/resource_names.json}.
 */
public class ProjectResources {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".gif", ".bmp");
    private static final Set<String> AUDIO_EXTENSIONS = Set.of(".mp3", ".wav", ".ogg");
    private static final String NAMES_FILE = "lib/config/resource_names.json";

    private final File projectDir;
    private final List<ResourceEntry> backgrounds = new ArrayList<>();
    private final List<ResourceEntry> sounds = new ArrayList<>();
    private final Map<String, String> nameMap = new LinkedHashMap<>();

    public ProjectResources(File projectDir) {
        this.projectDir = projectDir;
        loadNameMap();
        scan();
    }

    /** Re-scans the disk and reloads the name map. */
    public void refresh() {
        loadNameMap();
        scan();
    }

    /**
     * Returns all background entries (sorted by display name).
     *
     * @return unmodifiable list of background resources
     */
    public List<ResourceEntry> getBackgrounds() { return backgrounds; }

    /**
     * Returns all sound entries (sorted by display name).
     *
     * @return unmodifiable list of sound resources
     */
    public List<ResourceEntry> getSounds() { return sounds; }

    /** Returns the project root directory. */
    public File getProjectDir() { return projectDir; }

    /**
     * Finds the display name for a path (used by badges, labels).
     *
     * @param path relative path inside the project
     * @return display name if one is assigned, otherwise the filename
     */
    public String getDisplayName(String path) {
        if (path == null || path.isEmpty()) return "";
        String mapped = nameMap.get(path);
        if (mapped != null) return mapped;
        int slash = path.lastIndexOf('/');
        String filename = slash >= 0 ? path.substring(slash + 1) : path;
        return filename.replaceFirst("\\.[^.]+$", "");
    }

    /**
     * Renames a resource (changes its display name) and persists the mapping.
     *
     * @param path    relative path of the resource
     * @param newName new user-visible name
     */
    public void rename(String path, String newName) {
        nameMap.put(path, newName);
        saveNameMap();
        updateEntryName(path, newName);
    }

    // ========================== Scanning ==========================

    private void scan() {
        backgrounds.clear();
        sounds.clear();
        scanDir(new File(projectDir, "lib/Scene"), IMAGE_EXTENSIONS, backgrounds);
        scanDir(new File(projectDir, "lib/MainMenuImg"), IMAGE_EXTENSIONS, backgrounds);
        scanDir(new File(projectDir, "lib/sound"), AUDIO_EXTENSIONS, sounds);
        backgrounds.sort(Comparator.comparing(ResourceEntry::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        sounds.sort(Comparator.comparing(ResourceEntry::getDisplayName, String.CASE_INSENSITIVE_ORDER));
    }

    private void scanDir(File dir, Set<String> extensions, List<ResourceEntry> results) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                scanDir(file, extensions, results);
            } else if (matchesExtension(file.getName(), extensions)) {
                String rel = projectDir.toPath().relativize(file.toPath()).toString().replace('\\', '/');
                results.add(new ResourceEntry(getDisplayName(rel), rel));
            }
        }
    }

    private boolean matchesExtension(String name, Set<String> extensions) {
        String lower = name.toLowerCase();
        for (String ext : extensions) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    private void updateEntryName(String path, String newName) {
        for (ResourceEntry e : backgrounds) {
            if (path.equals(e.getPath())) { e.setDisplayName(newName); return; }
        }
        for (ResourceEntry e : sounds) {
            if (path.equals(e.getPath())) { e.setDisplayName(newName); return; }
        }
    }

    // ====================== Name persistence ======================

    private void loadNameMap() {
        nameMap.clear();
        File file = new File(projectDir, NAMES_FILE);
        if (!file.exists()) return;
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            parseJson(reader);
        } catch (IOException ignored) {}
    }

    private void saveNameMap() {
        File file = new File(projectDir, NAMES_FILE);
        file.getParentFile().mkdirs();
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writeJson(writer);
        } catch (IOException ignored) {}
    }

    /** Minimal JSON parser — reads a flat {"key":"value"} map. */
    private void parseJson(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        String content = sb.toString().trim();
        if (!content.startsWith("{") || !content.endsWith("}")) return;
        content = content.substring(1, content.length() - 1).trim();
        if (content.isEmpty()) return;
        for (String pair : splitJsonPairs(content)) {
            String[] kv = splitKeyValue(pair);
            if (kv != null) nameMap.put(kv[0], kv[1]);
        }
    }

    private List<String> splitJsonPairs(String content) {
        List<String> pairs = new ArrayList<>();
        boolean inString = false;
        int start = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '"') inString = !inString;
            if (c == ',' && !inString) {
                pairs.add(content.substring(start, i).trim());
                start = i + 1;
            }
        }
        if (start < content.length()) pairs.add(content.substring(start).trim());
        return pairs;
    }

    private String[] splitKeyValue(String pair) {
        int colon = findUnquotedColon(pair);
        if (colon < 0) return null;
        String key = unquote(pair.substring(0, colon).trim());
        String value = unquote(pair.substring(colon + 1).trim());
        if (key == null || value == null) return null;
        return new String[] { key, value };
    }

    private int findUnquotedColon(String s) {
        boolean inString = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') inString = !inString;
            if (c == ':' && !inString) return i;
        }
        return -1;
    }

    private String unquote(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        return null;
    }

    private void writeJson(BufferedWriter writer) throws IOException {
        writer.write("{\n");
        Iterator<Map.Entry<String, String>> it = nameMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            writer.write("  \"" + escape(entry.getKey()) + "\": \"" + escape(entry.getValue()) + "\"");
            if (it.hasNext()) writer.write(",");
            writer.write("\n");
        }
        writer.write("}\n");
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
