package kz.aws.gametexteditor.editor;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import kz.aws.gametexteditor.model.EditorChapter;
import kz.aws.gametexteditor.model.EditorScene;

import java.util.List;

public class ChapterEditorDialog {

    /**
     * Shows chapter editor dialog. Returns true if chapters were modified.
     */
    public static boolean showAndEdit(List<EditorChapter> chapters, List<EditorScene> scenes, Stage owner) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("\u0420\u0435\u0434\u0430\u043a\u0442\u043e\u0440 \u0433\u043b\u0430\u0432");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setResizable(true);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("chapter-editor-dialog");
        dialogPane.setPrefSize(550, 400);

        ObservableList<EditorChapter> items = FXCollections.observableArrayList(chapters);

        // Table
        TableView<EditorChapter> table = new TableView<>(items);
        table.setEditable(true);
        table.getStyleClass().add("chapter-table");

        TableColumn<EditorChapter, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getId()));
        idCol.setPrefWidth(60);

        TableColumn<EditorChapter, String> nameCol = new TableColumn<>("\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getName()));
        nameCol.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(ev -> ev.getRowValue().setName(ev.getNewValue()));
        nameCol.setPrefWidth(280);

        TableColumn<EditorChapter, Number> sceneCol = new TableColumn<>("\u0421\u0446\u0435\u043d\u0430");
        sceneCol.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getSceneId()));
        sceneCol.setPrefWidth(80);

        table.getColumns().add(idCol);
        table.getColumns().add(nameCol);
        table.getColumns().add(sceneCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Buttons
        Button addBtn = new Button("+ \u0413\u043b\u0430\u0432\u0430");
        addBtn.getStyleClass().add("editor-toolbar-btn");
        addBtn.setOnAction(e -> {
            int maxId = items.stream().mapToInt(EditorChapter::getId).max().orElse(0);
            EditorChapter ch = new EditorChapter();
            ch.setId(maxId + 1);
            ch.setName("\u0413\u043b\u0430\u0432\u0430 " + ch.getId());
            ch.setSceneId(1);

            // Show mini-dialog for scene ID
            TextInputDialog sceneDialog = new TextInputDialog("1");
            sceneDialog.setTitle("\u041d\u043e\u0432\u0430\u044f \u0433\u043b\u0430\u0432\u0430");
            sceneDialog.setHeaderText("\u0413\u043b\u0430\u0432\u0430 " + ch.getId());
            sceneDialog.setContentText("\u041d\u0430\u0447\u0430\u043b\u044c\u043d\u0430\u044f \u0441\u0446\u0435\u043d\u0430 (ID):");
            sceneDialog.initOwner(dialog.getDialogPane().getScene().getWindow());

            sceneDialog.showAndWait().ifPresent(val -> {
                try {
                    ch.setSceneId(Integer.parseInt(val.trim()));
                } catch (NumberFormatException ignored) {}
                items.add(ch);
            });
        });

        Button deleteBtn = new Button("\u0423\u0434\u0430\u043b\u0438\u0442\u044c");
        deleteBtn.getStyleClass().add("editor-toolbar-btn");
        deleteBtn.setOnAction(e -> {
            EditorChapter sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) items.remove(sel);
        });

        HBox toolbar = new HBox(8, addBtn, deleteBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(8, 0, 0, 0));

        // Info label
        Label infoLabel = new Label("\u0414\u043e\u0441\u0442\u0443\u043f\u043d\u044b\u0435 \u0441\u0446\u0435\u043d\u044b: "
                + scenes.stream().map(s -> String.valueOf(s.getId())).reduce((a, b) -> a + ", " + b).orElse("\u043d\u0435\u0442"));
        infoLabel.getStyleClass().add("chapter-info-label");
        infoLabel.setWrapText(true);

        VBox content = new VBox(8, table, toolbar, infoLabel);
        content.setPadding(new Insets(10));
        dialogPane.setContent(content);

        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                // Apply changes to the original list
                chapters.clear();
                chapters.addAll(items);
                return true;
            }
            return false;
        });

        return dialog.showAndWait().orElse(false);
    }
}
