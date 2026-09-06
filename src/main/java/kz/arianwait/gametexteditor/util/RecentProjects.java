package kz.arianwait.gametexteditor.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class RecentProjects {

    private static final int MAX_ENTRIES = 10;
    private static final String DIR_NAME = ".liza-editor";
    private static final String FILE_NAME = "recent-projects.properties";

    public record RecentEntry(String path, String name, long timestamp) {}

    public static List<RecentEntry> load() {
        List<RecentEntry> list = new ArrayList<>();
        File file = getRecentFile();
        if (!file.exists()) return list;

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println("Failed to load recent projects: " + e.getMessage());
            return list;
        }

        for (int i = 0; i < MAX_ENTRIES; i++) {
            String path = props.getProperty("project." + i + ".path");
            if (path == null) break;
            String name = props.getProperty("project." + i + ".name", new File(path).getName());
            long time;
            try {
                time = Long.parseLong(props.getProperty("project." + i + ".time", "0"));
            } catch (NumberFormatException e) {
                time = 0;
            }
            list.add(new RecentEntry(path, name, time));
        }
        return list;
    }

    public static void addProject(File projectDir) {
        if (projectDir == null) return;
        String absPath = projectDir.getAbsolutePath();
        List<RecentEntry> list = load();

        // Remove duplicate
        list.removeIf(e -> e.path().equals(absPath));

        // Add at the top
        list.add(0, new RecentEntry(absPath, projectDir.getName(), System.currentTimeMillis()));

        // Trim to max
        while (list.size() > MAX_ENTRIES) {
            list.remove(list.size() - 1);
        }

        save(list);
    }

    public static void removeProject(String path) {
        List<RecentEntry> list = load();
        list.removeIf(e -> e.path().equals(path));
        save(list);
    }

    public static void clearAll() {
        save(List.of());
    }

    private static void save(List<RecentEntry> list) {
        File file = getRecentFile();
        file.getParentFile().mkdirs();

        Properties props = new Properties();
        for (int i = 0; i < list.size(); i++) {
            RecentEntry entry = list.get(i);
            props.setProperty("project." + i + ".path", entry.path());
            props.setProperty("project." + i + ".name", entry.name());
            props.setProperty("project." + i + ".time", String.valueOf(entry.timestamp()));
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, "Liza Editor Recent Projects");
        } catch (IOException e) {
            System.err.println("Failed to save recent projects: " + e.getMessage());
        }
    }

    private static File getRecentFile() {
        String home = System.getProperty("user.home");
        return new File(home, DIR_NAME + File.separator + FILE_NAME);
    }
}
