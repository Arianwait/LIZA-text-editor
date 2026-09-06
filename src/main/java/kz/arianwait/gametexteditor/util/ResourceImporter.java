package kz.arianwait.gametexteditor.util;

import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper for copying external image/audio files into the project's
 * resource folders ({@code lib/Scene/backgrounds/} and {@code lib/sound/}).
 */
public class ResourceImporter {

    private static final String BACKGROUNDS_SUBPATH = "lib/Scene/backgrounds";
    private static final String SOUNDS_SUBPATH = "lib/sound";

    private static final FileChooser.ExtensionFilter IMAGE_FILTER =
            new FileChooser.ExtensionFilter(
                    "\u0418\u0437\u043e\u0431\u0440\u0430\u0436\u0435\u043d\u0438\u044f",
                    "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp");

    private static final FileChooser.ExtensionFilter AUDIO_FILTER =
            new FileChooser.ExtensionFilter(
                    "\u0417\u0432\u0443\u043a\u0438",
                    "*.mp3", "*.wav", "*.ogg");

    private ResourceImporter() {}

    /**
     * Opens a file chooser and copies selected images into
     * {@code lib/Scene/backgrounds/}.
     *
     * @param owner      window used as the chooser owner
     * @param projectDir current project root
     * @return list of relative paths that were imported; empty if cancelled
     */
    public static List<String> importBackgrounds(Window owner, File projectDir) {
        return doImport(owner, projectDir, BACKGROUNDS_SUBPATH,
                IMAGE_FILTER, "\u0412\u044b\u0431\u043e\u0440 \u0444\u043e\u043d\u043e\u0432 \u0434\u043b\u044f \u0438\u043c\u043f\u043e\u0440\u0442\u0430",
                "\u0418\u043c\u043f\u043e\u0440\u0442 \u0444\u043e\u043d\u043e\u0432");
    }

    /**
     * Opens a file chooser and copies selected audio files into
     * {@code lib/sound/}.
     *
     * @param owner      window used as the chooser owner
     * @param projectDir current project root
     * @return list of relative paths that were imported; empty if cancelled
     */
    public static List<String> importSounds(Window owner, File projectDir) {
        return doImport(owner, projectDir, SOUNDS_SUBPATH,
                AUDIO_FILTER, "\u0412\u044b\u0431\u043e\u0440 \u0437\u0432\u0443\u043a\u043e\u0432 \u0434\u043b\u044f \u0438\u043c\u043f\u043e\u0440\u0442\u0430",
                "\u0418\u043c\u043f\u043e\u0440\u0442 \u0437\u0432\u0443\u043a\u043e\u0432");
    }

    private static List<String> doImport(Window owner, File projectDir,
                                          String subpath,
                                          FileChooser.ExtensionFilter filter,
                                          String chooserTitle, String resultTitle) {
        File targetDir = ensureDir(projectDir, subpath);
        if (targetDir == null) return List.of();

        FileChooser chooser = new FileChooser();
        chooser.setTitle(chooserTitle);
        chooser.getExtensionFilters().add(filter);
        List<File> selected = chooser.showOpenMultipleDialog(owner);
        if (selected == null || selected.isEmpty()) return List.of();

        List<String> imported = new ArrayList<>();
        for (File src : selected) {
            String rel = copyOne(src, targetDir, projectDir);
            if (rel != null) imported.add(rel);
        }
        if (!imported.isEmpty()) {
            showInfo(owner, imported.size(), resultTitle);
        }
        return imported;
    }

    private static File ensureDir(File projectDir, String subpath) {
        File target = new File(projectDir, subpath);
        if (!target.exists() && !target.mkdirs()) return null;
        return target;
    }

    private static String copyOne(File src, File targetDir, File projectDir) {
        File dest = new File(targetDir, src.getName());
        if (dest.exists()) return null;
        try {
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            Path rel = projectDir.toPath().relativize(dest.toPath());
            return rel.toString().replace('\\', '/');
        } catch (IOException ex) {
            return null;
        }
    }

    private static void showInfo(Window owner, int count, String title) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText("\u0418\u043c\u043f\u043e\u0440\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u043e \u0444\u0430\u0439\u043b\u043e\u0432: " + count);
        alert.initOwner(owner);
        alert.showAndWait();
    }
}
