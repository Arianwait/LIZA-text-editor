package kz.aws.gametexteditor.model;

import java.io.Serializable;

/**
 * A named project resource (background image or sound file).
 * {@code displayName} is the user-visible label (editable),
 * {@code path} is the relative path inside the project directory.
 */
public class ResourceEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private String displayName;
    private String path;

    public ResourceEntry() {}

    /**
     * Creates a resource entry.
     *
     * @param displayName user-visible name (e.g. "Ночной город")
     * @param path        relative path from project root (e.g. "lib/Scene/backgrounds/city_night.png")
     */
    public ResourceEntry(String displayName, String path) {
        this.displayName = displayName;
        this.path = path;
    }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    /**
     * Extracts the filename from the path (without directory prefix).
     *
     * @return filename portion of {@link #path}
     */
    public String getFileName() {
        if (path == null) return "";
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    @Override
    public String toString() {
        return displayName != null ? displayName : getFileName();
    }
}
