package kz.aws.gametexteditor.palette;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import kz.aws.gametexteditor.model.EditorCharacter;
import kz.aws.gametexteditor.model.EditorCommand;
import kz.aws.gametexteditor.model.EditorProject;
import kz.aws.gametexteditor.util.ProjectResources;

import java.util.Optional;

public class CommandEditDialog {

    private static final String BG_TYPE = "background";

    public static boolean showAndEdit(EditorCommand cmd, EditorProject project,
                                       ProjectResources resources, Window owner) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u043a\u043e\u043c\u0430\u043d\u0434\u044b");
        dialog.initOwner(owner);
        dialog.getDialogPane().getStyleClass().add("command-edit-dialog");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("character", BG_TYPE, "input", "panel", "puzzle", "effect");
        typeBox.setValue(cmd.getType() != null ? cmd.getType() : "character");
        addRow(grid, 0, "\u0422\u0438\u043f:", typeBox);

        TextField actionField = new TextField(cmd.getAction() != null ? cmd.getAction() : "");
        actionField.setPromptText("showPerson, changebackground...");
        addRow(grid, 1, "\u0414\u0435\u0439\u0441\u0442\u0432\u0438\u0435:", actionField);

        ComboBox<String> targetBox = new ComboBox<>();
        targetBox.setEditable(true);
        if (project != null) {
            for (EditorCharacter ch : project.getCharacters()) {
                targetBox.getItems().add(ch.getName());
            }
        }
        targetBox.setValue(cmd.getTarget() != null ? cmd.getTarget() : "");
        addRow(grid, 2, "\u0426\u0435\u043b\u044c:", targetBox);

        StackPane valueContainer = buildValueEditor(cmd, resources, typeBox);
        addRow(grid, 3, "\u0417\u043d\u0430\u0447\u0435\u043d\u0438\u0435:", valueContainer);

        TextField idField = new TextField(cmd.getId() != null ? cmd.getId() : "");
        addRow(grid, 4, "ID:", idField);

        TextField flagField = new TextField(cmd.getFlag() != null ? cmd.getFlag() : "");
        addRow(grid, 5, "\u0424\u043b\u0430\u0433:", flagField);

        ComboBox<String> keyBox = new ComboBox<>();
        keyBox.setEditable(true);
        keyBox.getItems().addAll("playerName", "playerSurname", "playerNickname");
        collectInputKeys(project, keyBox);
        keyBox.setValue(cmd.getKey() != null ? cmd.getKey() : "");
        addRow(grid, 6, "\u041a\u043b\u044e\u0447:", keyBox);

        TextField promptField = new TextField(cmd.getPrompt() != null ? cmd.getPrompt() : "");
        addRow(grid, 7, "\u041f\u043e\u0434\u0441\u043a\u0430\u0437\u043a\u0430:", promptField);

        Spinner<Integer> successSpinner = new Spinner<>(-1, 9999, cmd.getOnSuccess());
        successSpinner.setEditable(true);
        successSpinner.setPrefWidth(90);
        addRow(grid, 8, "\u0423\u0441\u043f\u0435\u0445 \u2192:", successSpinner);

        Spinner<Integer> failureSpinner = new Spinner<>(-1, 9999, cmd.getOnFailure());
        failureSpinner.setEditable(true);
        failureSpinner.setPrefWidth(90);
        addRow(grid, 9, "\u041d\u0435\u0443\u0434\u0430\u0447\u0430 \u2192:", failureSpinner);

        // Effect fields — simple: name + on/off
        ComboBox<String> effectBox = new ComboBox<>();
        effectBox.setEditable(true);
        effectBox.setPromptText("blink");
        collectEffectNames(project, effectBox);
        effectBox.setValue(cmd.getEffect() != null ? cmd.getEffect() : "");
        Label effectLabel = new Label("\u042d\u0444\u0444\u0435\u043a\u0442:");
        effectLabel.getStyleClass().add("command-edit-label");
        grid.add(effectLabel, 0, 10);
        grid.add(effectBox, 1, 10);
        GridPane.setHgrow(effectBox, Priority.ALWAYS);

        // Show/hide effect row based on type
        Runnable updateEffectVisibility = () -> {
            boolean isEffect = "effect".equals(typeBox.getValue());
            effectLabel.setVisible(isEffect);
            effectLabel.setManaged(isEffect);
            effectBox.setVisible(isEffect);
            effectBox.setManaged(isEffect);
        };
        typeBox.valueProperty().addListener((o, ov, nv) -> updateEffectVisibility.run());
        updateEffectVisibility.run();

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            cmd.setType(typeBox.getValue());
            cmd.setAction(actionField.getText());
            cmd.setTarget(targetBox.getValue());
            cmd.setValue(readValue(valueContainer, typeBox.getValue()));
            cmd.setId(idField.getText().isEmpty() ? null : idField.getText());
            cmd.setFlag(flagField.getText().isEmpty() ? null : flagField.getText());
            String keyVal = keyBox.getValue();
            cmd.setKey(keyVal == null || keyVal.isEmpty() ? null : keyVal);
            cmd.setPrompt(promptField.getText().isEmpty() ? null : promptField.getText());
            cmd.setOnSuccess(successSpinner.getValue());
            cmd.setOnFailure(failureSpinner.getValue());

            if ("effect".equals(typeBox.getValue())) {
                String ev = effectBox.getValue();
                cmd.setEffect(ev == null || ev.isEmpty() ? null : ev);
            }
            return true;
        }
        return false;
    }

    private static void addRow(GridPane grid, int row, String labelText, javafx.scene.Node field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("command-edit-label");
        grid.add(label, 0, row);
        grid.add(field, 1, row);
        if (field instanceof javafx.scene.layout.Region r) {
            GridPane.setHgrow(r, Priority.ALWAYS);
        }
    }

    /**
     * Builds a stack with a text field (for generic values) and a background picker
     * (shown only when type=background). Swaps visibility based on {@code typeBox}.
     *
     * @param cmd       command being edited
     * @param resources project resources for the picker (may be null)
     * @param typeBox   type selector driving which editor is visible
     * @return container holding both editors; retrievable via {@link #readValue}
     */
    private static StackPane buildValueEditor(EditorCommand cmd, ProjectResources resources,
                                              ComboBox<String> typeBox) {
        String initial = cmd.getValue() != null ? cmd.getValue() : "";

        TextField valueField = new TextField(initial);
        valueField.setPromptText("\u041f\u043e\u0437\u0430 / \u043f\u0443\u0442\u044c");

        ResourcePickerField bgPicker = new ResourcePickerField(ResourcePickerField.Kind.BACKGROUND);
        bgPicker.setResources(resources);
        bgPicker.setValue(initial);

        StackPane container = new StackPane(valueField, bgPicker);
        container.setUserData(new Object[] { valueField, bgPicker });
        applyValueEditorVisibility(container, typeBox.getValue());

        typeBox.valueProperty().addListener((o, ov, nv) -> syncValueAcrossEditors(container, ov, nv));
        return container;
    }

    private static void applyValueEditorVisibility(StackPane container, String type) {
        Object[] editors = (Object[]) container.getUserData();
        TextField valueField = (TextField) editors[0];
        ResourcePickerField bgPicker = (ResourcePickerField) editors[1];
        boolean isBg = BG_TYPE.equals(type);
        valueField.setVisible(!isBg);
        valueField.setManaged(!isBg);
        bgPicker.setVisible(isBg);
        bgPicker.setManaged(isBg);
    }

    private static void syncValueAcrossEditors(StackPane container, String oldType, String newType) {
        Object[] editors = (Object[]) container.getUserData();
        TextField valueField = (TextField) editors[0];
        ResourcePickerField bgPicker = (ResourcePickerField) editors[1];
        // Carry the current value across when switching modes
        if (BG_TYPE.equals(oldType) && !BG_TYPE.equals(newType)) {
            valueField.setText(bgPicker.getValue());
        } else if (!BG_TYPE.equals(oldType) && BG_TYPE.equals(newType)) {
            bgPicker.setValue(valueField.getText());
        }
        applyValueEditorVisibility(container, newType);
    }

    private static String readValue(StackPane container, String type) {
        Object[] editors = (Object[]) container.getUserData();
        TextField valueField = (TextField) editors[0];
        ResourcePickerField bgPicker = (ResourcePickerField) editors[1];
        return BG_TYPE.equals(type) ? bgPicker.getValue() : valueField.getText();
    }

    /**
     * Scans the project for existing input command keys and adds
     * any that are not already present to the combo box.
     *
     * @param project current editor project
     * @param keyBox  combo box to populate
     */
    private static void collectInputKeys(EditorProject project, ComboBox<String> keyBox) {
        if (project == null) return;
        for (var scene : project.getScenes()) {
            for (var frame : scene.getFrames()) {
                for (var cmd : frame.getCommands()) {
                    if ("input".equals(cmd.getType()) && cmd.getKey() != null
                            && !cmd.getKey().isEmpty() && !keyBox.getItems().contains(cmd.getKey())) {
                        keyBox.getItems().add(cmd.getKey());
                    }
                }
            }
        }
    }

    /**
     * Scans the project for effect names already used in effect commands
     * and populates the combo box so they can be reused.
     *
     * @param project current editor project
     * @param box     combo box to populate
     */
    private static void collectEffectNames(EditorProject project, ComboBox<String> box) {
        if (project == null) return;
        for (var scene : project.getScenes()) {
            for (var frame : scene.getFrames()) {
                for (var cmd : frame.getCommands()) {
                    if ("effect".equals(cmd.getType()) && cmd.getEffect() != null
                            && !cmd.getEffect().isEmpty() && !box.getItems().contains(cmd.getEffect())) {
                        box.getItems().add(cmd.getEffect());
                    }
                }
            }
        }
    }
}
