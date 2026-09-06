package kz.arianwait.gametexteditor.palette;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import kz.arianwait.gametexteditor.model.ResourceEntry;

import java.io.File;
import java.util.List;

/**
 * Modal dialog that shows a filterable list of project resources
 * (backgrounds or sounds). Each item displays its display name with
 * the full relative path as a tooltip. Returns the selected path.
 */
public class ResourcePickerDialog {

    private final Stage owner;
    private final File projectDir;
    private final boolean showImagePreview;

    public ResourcePickerDialog(Stage owner, File projectDir, boolean showImagePreview) {
        this.owner = owner;
        this.projectDir = projectDir;
        this.showImagePreview = showImagePreview;
    }

    /**
     * Shows the picker and returns the selected relative path, or null if cancelled.
     *
     * @param title     dialog title
     * @param resources available resources to choose from
     * @return relative path of the selected resource, or {@code null}
     */
    public String showAndWait(String title, List<ResourceEntry> resources) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setResizable(true);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("resource-picker-dialog");
        dialogPane.setPrefSize(550, 450);

        TextField searchField = createSearchField();

        ObservableList<ResourceEntry> items = FXCollections.observableArrayList(resources);
        FilteredList<ResourceEntry> filtered = new FilteredList<>(items, p -> true);
        wireSearch(searchField, filtered);

        ListView<ResourceEntry> listView = createListView(filtered);

        Label pathLabel = new Label("");
        pathLabel.getStyleClass().add("resource-path-label");
        pathLabel.setWrapText(true);

        ImageView previewView = createPreviewView();
        StackPane previewPane = wrapPreview(previewView);

        wireSelection(listView, pathLabel, previewView);

        HBox content = buildLayout(searchField, listView, pathLabel, previewPane);
        dialogPane.setContent(content);
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        wireDoubleClick(listView, dialog);

        dialog.setResultConverter(btn ->
                btn == ButtonType.OK && listView.getSelectionModel().getSelectedItem() != null
                        ? listView.getSelectionModel().getSelectedItem().getPath()
                        : null);

        if (!filtered.isEmpty()) {
            listView.getSelectionModel().selectFirst();
        }

        return dialog.showAndWait().orElse(null);
    }

    private TextField createSearchField() {
        TextField field = new TextField();
        field.setPromptText("\u041f\u043e\u0438\u0441\u043a...");
        field.getStyleClass().add("resource-search-field");
        return field;
    }

    private void wireSearch(TextField field, FilteredList<ResourceEntry> filtered) {
        field.textProperty().addListener((obs, old, text) -> {
            String lower = text.toLowerCase();
            filtered.setPredicate(entry -> text.isEmpty()
                    || entry.getDisplayName().toLowerCase().contains(lower)
                    || entry.getPath().toLowerCase().contains(lower));
        });
    }

    private ListView<ResourceEntry> createListView(FilteredList<ResourceEntry> filtered) {
        ListView<ResourceEntry> listView = new ListView<>(filtered);
        listView.getStyleClass().add("resource-list");
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ResourceEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item.getDisplayName());
                    setTooltip(new Tooltip(item.getPath()));
                }
            }
        });
        VBox.setVgrow(listView, Priority.ALWAYS);
        return listView;
    }

    private ImageView createPreviewView() {
        ImageView view = new ImageView();
        view.setPreserveRatio(true);
        view.setFitWidth(200);
        view.setFitHeight(130);
        return view;
    }

    private StackPane wrapPreview(ImageView view) {
        StackPane pane = new StackPane(view);
        pane.getStyleClass().add("resource-preview");
        pane.setPrefSize(200, 130);
        pane.setMinSize(200, 130);
        return pane;
    }

    private void wireSelection(ListView<ResourceEntry> list, Label pathLabel, ImageView preview) {
        list.getSelectionModel().selectedItemProperty().addListener((obs, old, entry) -> {
            if (entry != null) {
                pathLabel.setText(entry.getPath());
                if (showImagePreview) loadPreviewImage(preview, entry.getPath());
            } else {
                pathLabel.setText("");
                preview.setImage(null);
            }
        });
    }

    private HBox buildLayout(TextField search, ListView<ResourceEntry> list,
                             Label pathLabel, StackPane previewPane) {
        VBox leftSide = new VBox(6, search, list, pathLabel);
        VBox.setVgrow(leftSide, Priority.ALWAYS);
        HBox.setHgrow(leftSide, Priority.ALWAYS);

        HBox content;
        if (showImagePreview) {
            VBox rightSide = new VBox(previewPane);
            rightSide.setAlignment(Pos.TOP_CENTER);
            rightSide.setPadding(new Insets(0, 0, 0, 10));
            content = new HBox(10, leftSide, rightSide);
        } else {
            content = new HBox(leftSide);
        }
        content.setPadding(new Insets(10));
        HBox.setHgrow(content, Priority.ALWAYS);
        return content;
    }

    private void wireDoubleClick(ListView<ResourceEntry> list, Dialog<String> dialog) {
        list.setOnMouseClicked(e -> {
            ResourceEntry sel = list.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && sel != null) {
                dialog.setResult(sel.getPath());
                dialog.close();
            }
        });
    }

    private void loadPreviewImage(ImageView view, String relativePath) {
        try {
            File file = new File(projectDir, relativePath);
            if (file.exists()) {
                Image img = new Image(file.toURI().toString(), 200, 130, true, true, false);
                view.setImage(img);
            } else {
                view.setImage(null);
            }
        } catch (Exception e) {
            view.setImage(null);
        }
    }
}
