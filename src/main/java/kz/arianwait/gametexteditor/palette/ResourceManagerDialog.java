package kz.arianwait.gametexteditor.palette;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import kz.arianwait.gametexteditor.model.ResourceEntry;
import kz.arianwait.gametexteditor.util.ProjectResources;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Modal dialog for managing project resources (backgrounds or sounds).
 * Displays a table with editable display names, an import button,
 * a delete button, and (for images) a preview pane.
 */
public class ResourceManagerDialog {

    /** Which resource kind this dialog manages. */
    public enum Kind { BACKGROUNDS, SOUNDS }

    private final Stage owner;
    private final ProjectResources resources;
    private final Kind kind;

    public ResourceManagerDialog(Stage owner, ProjectResources resources, Kind kind) {
        this.owner = owner;
        this.resources = resources;
        this.kind = kind;
    }

    /**
     * Opens the manager dialog. All changes (renames, imports) are applied
     * directly to {@link ProjectResources}.
     */
    public void showAndWait() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(kind == Kind.BACKGROUNDS ? "\u0423\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0444\u043e\u043d\u0430\u043c\u0438" : "\u0423\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0437\u0432\u0443\u043a\u0430\u043c\u0438");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setResizable(true);

        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("resource-manager-dialog");
        pane.setPrefSize(650, 480);

        ObservableList<ResourceEntry> items = FXCollections.observableArrayList(getEntries());
        TableView<ResourceEntry> table = buildTable(items);

        HBox buttons = buildButtons(items, table);

        ImageView previewView = createPreview();
        StackPane previewPane = wrapPreview(previewView);
        wirePreview(table, previewView);

        VBox leftSide = new VBox(8, table, buttons);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(leftSide, Priority.ALWAYS);
        HBox.setHgrow(leftSide, Priority.ALWAYS);

        HBox content;
        if (kind == Kind.BACKGROUNDS) {
            VBox rightSide = new VBox(previewPane);
            rightSide.setAlignment(Pos.TOP_CENTER);
            rightSide.setPadding(new Insets(0, 0, 0, 10));
            content = new HBox(10, leftSide, rightSide);
        } else {
            content = new HBox(leftSide);
        }
        content.setPadding(new Insets(10));
        HBox.setHgrow(content, Priority.ALWAYS);

        pane.setContent(content);
        pane.getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    private List<ResourceEntry> getEntries() {
        return kind == Kind.BACKGROUNDS ? resources.getBackgrounds() : resources.getSounds();
    }

    @SuppressWarnings("unchecked")
    private TableView<ResourceEntry> buildTable(ObservableList<ResourceEntry> items) {
        TableView<ResourceEntry> table = new TableView<>(items);
        table.setEditable(true);
        table.getStyleClass().add("resource-manager-table");
        table.setPrefHeight(300);

        TableColumn<ResourceEntry, String> nameCol = new TableColumn<>("\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435");
        nameCol.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getDisplayName()));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(evt -> {
            ResourceEntry entry = evt.getRowValue();
            String newName = evt.getNewValue().trim();
            if (!newName.isEmpty()) {
                entry.setDisplayName(newName);
                resources.rename(entry.getPath(), newName);
            }
        });
        nameCol.setPrefWidth(220);

        TableColumn<ResourceEntry, String> fileCol = new TableColumn<>("\u0424\u0430\u0439\u043b");
        fileCol.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getFileName()));
        fileCol.setEditable(false);
        fileCol.setPrefWidth(180);

        TableColumn<ResourceEntry, String> pathCol = new TableColumn<>("\u041f\u0443\u0442\u044c");
        pathCol.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getPath()));
        pathCol.setEditable(false);
        pathCol.setPrefWidth(200);

        table.getColumns().addAll(nameCol, fileCol, pathCol);
        return table;
    }

    private HBox buildButtons(ObservableList<ResourceEntry> items, TableView<ResourceEntry> table) {
        Button importBtn = new Button("\u0418\u043c\u043f\u043e\u0440\u0442...");
        importBtn.getStyleClass().add("toolbar-add-btn");
        importBtn.setOnAction(e -> doImport(items, table));

        Button deleteBtn = new Button("\u0423\u0434\u0430\u043b\u0438\u0442\u044c");
        deleteBtn.getStyleClass().add("toolbar-delete-btn");
        deleteBtn.setOnAction(e -> doDelete(items, table));

        HBox box = new HBox(8, importBtn, deleteBtn);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void doImport(ObservableList<ResourceEntry> items, TableView<ResourceEntry> table) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(kind == Kind.BACKGROUNDS ? "\u0418\u043c\u043f\u043e\u0440\u0442 \u0444\u043e\u043d\u043e\u0432" : "\u0418\u043c\u043f\u043e\u0440\u0442 \u0437\u0432\u0443\u043a\u043e\u0432");
        chooser.getExtensionFilters().add(buildFilter());
        List<File> selected = chooser.showOpenMultipleDialog(owner);
        if (selected == null || selected.isEmpty()) return;

        File targetDir = getTargetDir();
        if (targetDir == null) return;

        for (File src : selected) {
            ResourceEntry entry = copyAndCreateEntry(src, targetDir);
            if (entry != null) items.add(entry);
        }
        resources.refresh();
        items.setAll(getEntries());
        if (!items.isEmpty()) table.getSelectionModel().selectLast();
    }

    private void doDelete(ObservableList<ResourceEntry> items, TableView<ResourceEntry> table) {
        ResourceEntry sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("\u0423\u0434\u0430\u043b\u0435\u043d\u0438\u0435");
        confirm.setHeaderText(null);
        confirm.setContentText("\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u0444\u0430\u0439\u043b \"" + sel.getDisplayName() + "\"?\n" + sel.getPath());
        confirm.initOwner(owner);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        File file = new File(resources.getProjectDir(), sel.getPath());
        if (file.exists()) file.delete();
        resources.refresh();
        items.setAll(getEntries());
    }

    private FileChooser.ExtensionFilter buildFilter() {
        if (kind == Kind.BACKGROUNDS) {
            return new FileChooser.ExtensionFilter("\u0418\u0437\u043e\u0431\u0440\u0430\u0436\u0435\u043d\u0438\u044f", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp");
        } else {
            return new FileChooser.ExtensionFilter("\u0417\u0432\u0443\u043a\u0438", "*.mp3", "*.wav", "*.ogg");
        }
    }

    private File getTargetDir() {
        String subpath = kind == Kind.BACKGROUNDS ? "lib/Scene/backgrounds" : "lib/sound";
        File dir = new File(resources.getProjectDir(), subpath);
        if (!dir.exists() && !dir.mkdirs()) return null;
        return dir;
    }

    private ResourceEntry copyAndCreateEntry(File src, File targetDir) {
        File dest = new File(targetDir, src.getName());
        if (dest.exists()) return null;
        try {
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            String rel = resources.getProjectDir().toPath().relativize(dest.toPath())
                    .toString().replace('\\', '/');
            String name = src.getName().replaceFirst("\\.[^.]+$", "");
            return new ResourceEntry(name, rel);
        } catch (IOException ex) {
            return null;
        }
    }

    private ImageView createPreview() {
        ImageView view = new ImageView();
        view.setPreserveRatio(true);
        view.setFitWidth(200);
        view.setFitHeight(150);
        return view;
    }

    private StackPane wrapPreview(ImageView view) {
        StackPane pane = new StackPane(view);
        pane.getStyleClass().add("resource-preview");
        pane.setPrefSize(200, 150);
        pane.setMinSize(200, 150);
        return pane;
    }

    private void wirePreview(TableView<ResourceEntry> table, ImageView view) {
        if (kind != Kind.BACKGROUNDS) return;
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, entry) -> {
            if (entry == null) { view.setImage(null); return; }
            try {
                File file = new File(resources.getProjectDir(), entry.getPath());
                if (file.exists()) {
                    view.setImage(new Image(file.toURI().toString(), 200, 150, true, true, false));
                } else {
                    view.setImage(null);
                }
            } catch (Exception e) {
                view.setImage(null);
            }
        });
    }
}
