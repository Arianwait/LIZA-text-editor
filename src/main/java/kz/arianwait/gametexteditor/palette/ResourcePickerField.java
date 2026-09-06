package kz.arianwait.gametexteditor.palette;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.stage.Window;
import kz.arianwait.gametexteditor.model.ResourceEntry;
import kz.arianwait.gametexteditor.util.ProjectResources;

import java.util.List;

/**
 * Compact inline control for picking a project resource (image or sound).
 * Shows the display name as a label, a "..." button to open the picker,
 * and a clear button. Emits a {@link StringProperty} with the relative path.
 */
public class ResourcePickerField extends HBox {

    /** Resource flavor — decides which list and whether to show an image preview. */
    public enum Kind {
        BACKGROUND(true),
        SOUND(false);

        private final boolean imagePreview;

        Kind(boolean imagePreview) {
            this.imagePreview = imagePreview;
        }

        public boolean hasImagePreview() {
            return imagePreview;
        }
    }

    private static final String PLACEHOLDER = "\u2014";

    private final Label valueLabel = new Label(PLACEHOLDER);
    private final Button pickButton = new Button("...");
    private final Button clearButton = new Button("\u00d7");
    private final StringProperty value = new SimpleStringProperty("");

    private ProjectResources resources;
    private final Kind kind;

    public ResourcePickerField(Kind kind) {
        super(6);
        this.kind = kind;
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("resource-picker-field");

        valueLabel.getStyleClass().add("resource-picker-value");
        valueLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(valueLabel, Priority.ALWAYS);

        pickButton.getStyleClass().add("resource-picker-btn");
        clearButton.getStyleClass().add("resource-picker-clear");

        pickButton.setOnAction(e -> openPicker());
        clearButton.setOnAction(e -> setValue(""));

        value.addListener((o, ov, nv) -> updateLabel(nv));

        getChildren().addAll(valueLabel, pickButton, clearButton);
        updateLabel("");
    }

    /**
     * Provides the project resource cache used by the picker.
     *
     * @param resources scanned resources for the currently loaded project
     */
    public void setResources(ProjectResources resources) {
        this.resources = resources;
    }

    public String getValue() {
        return value.get();
    }

    public void setValue(String path) {
        value.set(path == null ? "" : path);
    }

    public StringProperty valueProperty() {
        return value;
    }

    private void openPicker() {
        if (resources == null) return;
        Window owner = getScene() != null ? getScene().getWindow() : null;
        Stage ownerStage = (owner instanceof Stage s) ? s : null;
        List<ResourceEntry> list = resolveList();
        String title = kind == Kind.BACKGROUND
                ? "\u0412\u044b\u0431\u043e\u0440 \u0444\u043e\u043d\u0430"
                : "\u0412\u044b\u0431\u043e\u0440 \u0437\u0432\u0443\u043a\u0430";
        ResourcePickerDialog dialog = new ResourcePickerDialog(
                ownerStage, resources.getProjectDir(), kind.hasImagePreview());
        String picked = dialog.showAndWait(title, list);
        if (picked != null) {
            setValue(picked);
        }
    }

    private List<ResourceEntry> resolveList() {
        return switch (kind) {
            case BACKGROUND -> resources.getBackgrounds();
            case SOUND -> resources.getSounds();
        };
    }

    private void updateLabel(String path) {
        if (path == null || path.isEmpty()) {
            valueLabel.setText(PLACEHOLDER);
            valueLabel.setTooltip(null);
            clearButton.setDisable(true);
        } else {
            String name = resources != null ? resources.getDisplayName(path) : fileNameOf(path);
            valueLabel.setText(name);
            valueLabel.setTooltip(new Tooltip(path));
            clearButton.setDisable(false);
        }
    }

    private String fileNameOf(String path) {
        int slash = path.lastIndexOf('/');
        String fn = slash >= 0 ? path.substring(slash + 1) : path;
        return fn.replaceFirst("\\.[^.]+$", "");
    }
}
