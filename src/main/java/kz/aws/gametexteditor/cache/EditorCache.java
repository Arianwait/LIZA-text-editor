package kz.aws.gametexteditor.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import kz.aws.gametexteditor.model.EditorProject;

public class EditorCache {

    private static final String CACHE_DIR = ".liza-editor-cache";
    private static final String DATA_FILE = "autosave.dat";
    private static final String META_FILE = "autosave.meta";
    private static final long AUTOSAVE_INTERVAL_MS = 60_000;

    private ScheduledExecutorService autosaveExecutor;
    private EditorProject projectRef;

    public void startAutosave(EditorProject project, DirtyTracker dirtyTracker) {
        this.projectRef = project;
        stopAutosave();
        autosaveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "editor-autosave");
            t.setDaemon(true);
            return t;
        });
        autosaveExecutor.scheduleWithFixedDelay(() -> {
            if (dirtyTracker.isDirty() && projectRef != null) {
                saveCache(projectRef);
            }
        }, AUTOSAVE_INTERVAL_MS, AUTOSAVE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public void stopAutosave() {
        if (autosaveExecutor != null && !autosaveExecutor.isShutdown()) {
            autosaveExecutor.shutdownNow();
            autosaveExecutor = null;
        }
    }

    public void saveCache(EditorProject project) {
        File cacheDir = getCacheDir(project.getProjectDir());
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            System.err.println("Cannot create cache directory: " + cacheDir);
            return;
        }

        // Save data
        File dataFile = new File(cacheDir, DATA_FILE);
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(dataFile)))) {
            oos.writeObject(project);
        } catch (Exception e) {
            System.err.println("Autosave failed: " + e.getMessage());
        }

        // Save metadata
        File metaFile = new File(cacheDir, META_FILE);
        try (FileOutputStream fos = new FileOutputStream(metaFile)) {
            Properties props = new Properties();
            props.setProperty("timestamp", String.valueOf(System.currentTimeMillis()));
            props.setProperty("sceneCount", String.valueOf(project.getSceneCount()));
            props.store(fos, "Liza Editor Cache");
        } catch (Exception e) {
            System.err.println("Meta save failed: " + e.getMessage());
        }
    }

    public EditorProject loadCache(File projectDir) {
        File dataFile = new File(getCacheDir(projectDir), DATA_FILE);
        if (!dataFile.exists()) return null;

        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(dataFile)))) {
            return (EditorProject) ois.readObject();
        } catch (Exception e) {
            System.err.println("Cache load failed: " + e.getMessage());
            return null;
        }
    }

    public boolean hasCrashRecovery(File projectDir) {
        File dataFile = new File(getCacheDir(projectDir), DATA_FILE);
        return dataFile.exists();
    }

    public void clearCache(File projectDir) {
        File cacheDir = getCacheDir(projectDir);
        if (cacheDir.exists()) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
            cacheDir.delete();
        }
    }

    public String getCacheInfo(File projectDir) {
        File metaFile = new File(getCacheDir(projectDir), META_FILE);
        if (!metaFile.exists()) return null;

        try (FileInputStream fis = new FileInputStream(metaFile)) {
            Properties props = new Properties();
            props.load(fis);
            long ts = Long.parseLong(props.getProperty("timestamp", "0"));
            String scenes = props.getProperty("sceneCount", "?");
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            return sdf.format(new java.util.Date(ts)) + " (" + scenes + " сцен)";
        } catch (Exception e) {
            return null;
        }
    }

    private File getCacheDir(File projectDir) {
        return new File(projectDir, CACHE_DIR);
    }
}
