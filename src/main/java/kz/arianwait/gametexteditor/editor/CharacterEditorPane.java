package kz.arianwait.gametexteditor.editor;

import javafx.beans.property.SimpleStringProperty;
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
import kz.arianwait.gametexteditor.model.*;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

public class CharacterEditorPane extends VBox {

    private EditorProject project;

    private final ListView<EditorCharacter> characterList;
    private final ObservableList<EditorCharacter> characterItems = FXCollections.observableArrayList();

    // Variable table
    private final TableView<EditorVariable> variableTable;
    private final ObservableList<EditorVariable> variableItems = FXCollections.observableArrayList();

    // Detail fields
    private final TextField nameField;
    private final TextField colorField;
    private final TableView<EditorPose> poseTable;
    private final ObservableList<EditorPose> poseItems = FXCollections.observableArrayList();
    private final ImageView spritePreview;
    private final VBox detailPane;
    private final Label noSelectionLabel;

    private Runnable onBackToGraph;
    private Runnable onCharactersChanged;

    private boolean suppressListeners = false;

    public CharacterEditorPane() {
        getStyleClass().add("character-editor");
        setSpacing(0);

        // Toolbar
        HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(8, 12, 8, 12));
        toolbar.getStyleClass().add("editor-toolbar");

        Button backBtn = new Button("\u2190 \u0413\u0440\u0430\u0444");
        backBtn.getStyleClass().add("editor-toolbar-btn");
        backBtn.setOnAction(e -> { if (onBackToGraph != null) onBackToGraph.run(); });

        Label titleLabel = new Label("\u041f\u0435\u0440\u0441\u043e\u043d\u0430\u0436\u0438");
        titleLabel.getStyleClass().add("editor-toolbar-title");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button addCharBtn = new Button("+ \u041f\u0435\u0440\u0441\u043e\u043d\u0430\u0436");
        addCharBtn.getStyleClass().add("toolbar-add-btn");
        addCharBtn.setOnAction(e -> addNewCharacter());

        Button deleteCharBtn = new Button("\u0423\u0434\u0430\u043b\u0438\u0442\u044c");
        deleteCharBtn.getStyleClass().add("toolbar-delete-btn");
        deleteCharBtn.setOnAction(e -> deleteSelectedCharacter());

        toolbar.getChildren().addAll(backBtn, titleLabel, addCharBtn, deleteCharBtn);

        // Left: character list
        characterList = new ListView<>(characterItems);
        characterList.getStyleClass().add("character-list");
        characterList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(EditorCharacter item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (" + item.getPoseCount() + " \u043f\u043e\u0437)");
                }
            }
        });
        characterList.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> showCharacterDetail(selected));
        characterList.setPrefWidth(220);
        characterList.setMinWidth(180);

        // Right: detail pane
        detailPane = new VBox(10);
        detailPane.setPadding(new Insets(12));
        detailPane.getStyleClass().add("character-detail");

        // Name
        Label nameLbl = new Label("\u0418\u043c\u044f:");
        nameLbl.getStyleClass().add("char-detail-label");
        nameField = new TextField();
        nameField.getStyleClass().add("char-detail-field");
        nameField.textProperty().addListener((o, ov, nv) -> {
            if (suppressListeners) return;
            EditorCharacter sel = characterList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                sel.setName(nv);
                characterList.refresh();
                fireChanged();
            }
        });

        // Color
        Label colorLbl = new Label("\u0426\u0432\u0435\u0442 (\u0438\u043c\u044f \u0438\u043b\u0438 #hex):");
        colorLbl.getStyleClass().add("char-detail-label");
        colorField = new TextField();
        colorField.getStyleClass().add("char-detail-field");
        colorField.setPromptText("GREEN, #ff0000, ...");
        colorField.textProperty().addListener((o, ov, nv) -> {
            if (suppressListeners) return;
            EditorCharacter sel = characterList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                sel.setColor(nv);
                fireChanged();
            }
        });

        // Poses table
        Label poseLbl = new Label("\u041f\u043e\u0437\u044b:");
        poseLbl.getStyleClass().add("char-detail-label");

        poseTable = new TableView<>(poseItems);
        poseTable.setEditable(true);
        poseTable.getStyleClass().add("pose-table");
        poseTable.setPrefHeight(200);

        TableColumn<EditorPose, String> nameCol = new TableColumn<>("\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getName()));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(ev -> {
            ev.getRowValue().setName(ev.getNewValue());
            fireChanged();
        });
        nameCol.setPrefWidth(120);

        TableColumn<EditorPose, String> pathCol = new TableColumn<>("\u041f\u0443\u0442\u044c");
        pathCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getSpritePath()));
        pathCol.setCellFactory(TextFieldTableCell.forTableColumn());
        pathCol.setOnEditCommit(ev -> {
            ev.getRowValue().setSpritePath(ev.getNewValue());
            fireChanged();
        });
        pathCol.setPrefWidth(300);

        poseTable.getColumns().add(nameCol);
        poseTable.getColumns().add(pathCol);
        VBox.setVgrow(poseTable, Priority.ALWAYS);

        poseTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> showPosePreview(sel));

        // Pose buttons
        HBox poseButtons = new HBox(8);
        poseButtons.setAlignment(Pos.CENTER_LEFT);

        Button addPoseBtn = new Button("+ \u041f\u043e\u0437\u0430");
        addPoseBtn.getStyleClass().add("toolbar-add-btn");
        addPoseBtn.setOnAction(e -> addNewPose());

        Button importBtn = new Button("\u0418\u043c\u043f\u043e\u0440\u0442 \u0438\u0437\u043e\u0431\u0440\u0430\u0436\u0435\u043d\u0438\u044f...");
        importBtn.getStyleClass().add("editor-toolbar-btn");
        importBtn.setOnAction(e -> importSprite());

        Button deletePoseBtn = new Button("\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u043f\u043e\u0437\u0443");
        deletePoseBtn.getStyleClass().add("toolbar-delete-btn");
        deletePoseBtn.setOnAction(e -> deleteSelectedPose());

        poseButtons.getChildren().addAll(addPoseBtn, importBtn, deletePoseBtn);

        // Sprite preview
        spritePreview = new ImageView();
        spritePreview.setPreserveRatio(true);
        spritePreview.setFitHeight(180);
        spritePreview.setFitWidth(180);

        StackPane previewContainer = new StackPane(spritePreview);
        previewContainer.getStyleClass().add("pose-preview");
        previewContainer.setMinHeight(190);
        previewContainer.setPrefHeight(190);

        detailPane.getChildren().addAll(
                nameLbl, nameField,
                colorLbl, colorField,
                poseLbl, poseTable, poseButtons,
                previewContainer
        );

        // No-selection placeholder
        noSelectionLabel = new Label("\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u043f\u0435\u0440\u0441\u043e\u043d\u0430\u0436\u0430 \u0438\u043b\u0438 \u0441\u043e\u0437\u0434\u0430\u0439\u0442\u0435 \u043d\u043e\u0432\u043e\u0433\u043e");
        noSelectionLabel.getStyleClass().add("char-no-selection");
        noSelectionLabel.setWrapText(true);

        // Variable table
        variableTable = buildVariableTable();

        Label varLabel = new Label("\u041f\u0435\u0440\u0435\u043c\u0435\u043d\u043d\u044b\u0435:");
        varLabel.getStyleClass().add("char-detail-label");
        varLabel.setPadding(new Insets(6, 0, 2, 4));

        Button syncVarsBtn = new Button("\u2b07 \u0421\u043e\u0431\u0440\u0430\u0442\u044c \u0438\u0437 \u0441\u0446\u0435\u043d");
        syncVarsBtn.getStyleClass().add("editor-toolbar-btn");
        syncVarsBtn.setOnAction(e -> syncVariablesFromScenes());

        HBox varHeader = new HBox(8, varLabel, syncVarsBtn);
        varHeader.setAlignment(Pos.CENTER_LEFT);

        VBox leftPane = new VBox(0, characterList, varHeader, variableTable);
        VBox.setVgrow(characterList, Priority.ALWAYS);
        variableTable.setPrefHeight(150);
        leftPane.setPrefWidth(220);
        leftPane.setMinWidth(180);

        // Main split
        SplitPane split = new SplitPane();
        split.getItems().addAll(leftPane, detailPane);
        split.setDividerPositions(0.3);
        SplitPane.setResizableWithParent(leftPane, false);
        VBox.setVgrow(split, Priority.ALWAYS);

        getChildren().addAll(toolbar, split);

        showCharacterDetail(null);
    }

    public void loadProject(EditorProject project) {
        this.project = project;
        suppressListeners = true;
        characterItems.setAll(project.getCharacters());
        variableItems.setAll(project.getVariables());
        suppressListeners = false;

        if (!characterItems.isEmpty()) {
            characterList.getSelectionModel().selectFirst();
        } else {
            showCharacterDetail(null);
        }
    }

    private void showCharacterDetail(EditorCharacter ch) {
        suppressListeners = true;
        if (ch == null) {
            nameField.setText("");
            colorField.setText("");
            poseItems.clear();
            spritePreview.setImage(null);
            detailPane.setDisable(true);
        } else {
            detailPane.setDisable(false);
            nameField.setText(ch.getName());
            colorField.setText(ch.getColor());
            poseItems.setAll(ch.getPoses());
            spritePreview.setImage(null);
            if (!ch.getPoses().isEmpty()) {
                poseTable.getSelectionModel().selectFirst();
            }
        }
        suppressListeners = false;
    }

    private void showPosePreview(EditorPose pose) {
        if (pose == null || project == null) {
            spritePreview.setImage(null);
            return;
        }
        try {
            String path = pose.getSpritePath();
            if (path != null && !path.isEmpty()) {
                File file = new File(project.getProjectDir(), path.replace('\\', '/'));
                if (file.exists()) {
                    spritePreview.setImage(
                            new Image(file.toURI().toString(), 180, 180, true, true, false));
                    return;
                }
            }
        } catch (Exception ignored) {}
        spritePreview.setImage(null);
    }

    private void addNewCharacter() {
        if (project == null) return;

        TextInputDialog dialog = new TextInputDialog("\u041d\u043e\u0432\u044b\u0439\u041f\u0435\u0440\u0441\u043e\u043d\u0430\u0436");
        dialog.setTitle("\u041d\u043e\u0432\u044b\u0439 \u043f\u0435\u0440\u0441\u043e\u043d\u0430\u0436");
        dialog.setHeaderText(null);
        dialog.setContentText("\u0418\u043c\u044f:");
        dialog.initOwner(getScene().getWindow());

        dialog.showAndWait().ifPresent(name -> {
            if (name.isBlank()) return;
            EditorCharacter ch = new EditorCharacter();
            ch.setName(name.trim());
            ch.setColor("WHITE");
            ch.setPoses(new ArrayList<>());
            project.getCharacters().add(ch);
            characterItems.setAll(project.getCharacters());
            characterList.getSelectionModel().select(ch);
            fireChanged();
        });
    }

    private void deleteSelectedCharacter() {
        EditorCharacter sel = characterList.getSelectionModel().getSelectedItem();
        if (sel == null || project == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("\u0423\u0434\u0430\u043b\u0435\u043d\u0438\u0435");
        confirm.setHeaderText("\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u043f\u0435\u0440\u0441\u043e\u043d\u0430\u0436\u0430 \"" + sel.getName() + "\"?");
        confirm.initOwner(getScene().getWindow());

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            project.getCharacters().remove(sel);
            characterItems.setAll(project.getCharacters());
            fireChanged();
        }
    }

    private void addNewPose() {
        EditorCharacter sel = characterList.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        EditorPose pose = new EditorPose("\u043d\u043e\u0432\u0430\u044f", "");
        sel.getPoses().add(pose);
        poseItems.setAll(sel.getPoses());
        poseTable.getSelectionModel().select(pose);
        fireChanged();
    }

    private void importSprite() {
        EditorCharacter sel = characterList.getSelectionModel().getSelectedItem();
        if (sel == null || project == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("\u0412\u044b\u0431\u0440\u0430\u0442\u044c \u0438\u0437\u043e\u0431\u0440\u0430\u0436\u0435\u043d\u0438\u0435");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("\u0418\u0437\u043e\u0431\u0440\u0430\u0436\u0435\u043d\u0438\u044f", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File src = fc.showOpenDialog(getScene().getWindow());
        if (src == null) return;

        try {
            // Create character directory
            String charDirName = sel.getName().replaceAll("[^a-zA-Z0-9\u0400-\u04FF_\\-]", "_");
            File charDir = new File(project.getProjectDir(), "lib/person/" + charDirName);
            charDir.mkdirs();

            // Copy file
            File dest = new File(charDir, src.getName());
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Create pose
            String poseName = src.getName().replaceFirst("\\.[^.]+$", "");
            String relativePath = "lib/person/" + charDirName + "/" + src.getName();

            EditorPose pose = new EditorPose(poseName, relativePath);
            sel.getPoses().add(pose);
            poseItems.setAll(sel.getPoses());
            poseTable.getSelectionModel().select(pose);
            characterList.refresh();
            fireChanged();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("\u041e\u0448\u0438\u0431\u043a\u0430");
            alert.setContentText("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0438\u043c\u043f\u043e\u0440\u0442\u0438\u0440\u043e\u0432\u0430\u0442\u044c: " + e.getMessage());
            alert.initOwner(getScene().getWindow());
            alert.showAndWait();
        }
    }

    private void deleteSelectedPose() {
        EditorCharacter ch = characterList.getSelectionModel().getSelectedItem();
        EditorPose pose = poseTable.getSelectionModel().getSelectedItem();
        if (ch == null || pose == null) return;

        ch.getPoses().remove(pose);
        poseItems.setAll(ch.getPoses());
        characterList.refresh();
        fireChanged();
    }

    /**
     * Builds the editable table for variable-to-color bindings.
     *
     * @return configured TableView
     */
    private TableView<EditorVariable> buildVariableTable() {
        TableView<EditorVariable> table = new TableView<>(variableItems);
        table.setEditable(true);
        table.getStyleClass().add("pose-table");
        table.setPlaceholder(new Label("\u041d\u0435\u0442 \u043f\u0435\u0440\u0435\u043c\u0435\u043d\u043d\u044b\u0445"));

        TableColumn<EditorVariable, String> keyCol = new TableColumn<>("\u041a\u043b\u044e\u0447");
        keyCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getKey()));
        keyCol.setPrefWidth(100);

        TableColumn<EditorVariable, String> colorCol = new TableColumn<>("\u0426\u0432\u0435\u0442");
        colorCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getColor() != null ? cd.getValue().getColor() : ""));
        colorCol.setCellFactory(TextFieldTableCell.forTableColumn());
        colorCol.setOnEditCommit(ev -> {
            ev.getRowValue().setColor(ev.getNewValue());
            fireChanged();
        });
        colorCol.setPrefWidth(100);

        table.getColumns().add(keyCol);
        table.getColumns().add(colorCol);
        return table;
    }

    /**
     * Scans all project scenes for input commands and adds missing keys
     * to the variable list, preserving existing color assignments.
     */
    private void syncVariablesFromScenes() {
        if (project == null) return;
        java.util.Set<String> existing = new java.util.LinkedHashSet<>();
        for (EditorVariable v : project.getVariables()) {
            existing.add(v.getKey());
        }
        for (EditorScene scene : project.getScenes()) {
            for (EditorFrame f : scene.getFrames()) {
                for (EditorCommand cmd : f.getCommands()) {
                    if ("input".equals(cmd.getType())
                            && cmd.getKey() != null && !cmd.getKey().isEmpty()
                            && !existing.contains(cmd.getKey())) {
                        project.getVariables().add(new EditorVariable(cmd.getKey(), "WHITE"));
                        existing.add(cmd.getKey());
                    }
                }
            }
        }
        variableItems.setAll(project.getVariables());
        fireChanged();
    }

    private void fireChanged() {
        if (onCharactersChanged != null) onCharactersChanged.run();
    }

    public void setOnBackToGraph(Runnable handler) { this.onBackToGraph = handler; }
    public void setOnCharactersChanged(Runnable handler) { this.onCharactersChanged = handler; }
}
