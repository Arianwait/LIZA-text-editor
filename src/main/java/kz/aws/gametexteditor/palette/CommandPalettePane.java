package kz.aws.gametexteditor.palette;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import kz.aws.gametexteditor.model.EditorCharacter;
import kz.aws.gametexteditor.model.EditorCommand;
import kz.aws.gametexteditor.model.EditorProject;
import kz.aws.gametexteditor.util.ProjectResources;

public class CommandPalettePane extends VBox {

    private EditorProject project;
    private ProjectResources resources;
    private Consumer<EditorCommand> onCommandInsert;

    // Category panes
    private final List<VBox> categoryPanes;
    private final List<ToggleButton> categoryButtons;
    private final Label categoryTitleLabel;

    // Character section
    private final ComboBox<String> charActionBox;
    private final ComboBox<String> charTargetBox;
    private final ComboBox<String> charPoseBox;
    private final HBox charPoseRow;

    // Background section
    private final Label bgSelectedLabel;

    // Sound section
    private final Label soundSelectedLabel;

    // Text panel section
    private final ComboBox<String> textPanelActionBox;

    // Flag section
    private final TextField flagNameField;
    private final ComboBox<String> flagValueBox;

    // Reputation section
    private final ComboBox<String> repActionBox;
    private final ComboBox<String> repTargetBox;
    private final Spinner<Integer> repValueSpinner;

    // Choice section
    private final TextField choiceKeyField;
    private final TextField choiceValueField;

    // Special effects section
    private final ComboBox<String> fxActionBox;
    private final ComboBox<String> fxIdField;
    private final TextField fxFlagField;
    private final Spinner<Integer> fxSuccessSpinner;
    private final Spinner<Integer> fxFailureSpinner;
    private final VBox fxPuzzleFields;

    // Visual effects section
    private final ComboBox<String> vfxNameField;
    private final ComboBox<String> vfxActionBox;

    // Input section
    private final ComboBox<String> inputKeyBox;
    private final TextField inputPromptField;

    // Actions that need a character target
    private static final Set<String> ACTIONS_WITH_TARGET = Set.of(
            "showPerson", "removeFromScene",
            "move_Left", "move_Right", "move_Center",
            "setFromLeft", "setFromRight", "setFromCenter",
            "runToLeft", "runToRight", "AppearEffect");

    // Actions that need a pose
    private static final Set<String> ACTIONS_WITH_POSE = Set.of("showPerson");

    private static final String[] CATEGORY_LABELS = {
            "\u041f\u0435\u0440\u0441", "\u0421\u0446\u0435\u043d", "\u041f\u0430\u043d\u0435\u043b", "\u0414\u0430\u043d\u043d", "\u042d\u0444\u0444\u0435\u043a"
    };
    private static final String[] CATEGORY_NAMES = {
            "\u041f\u0415\u0420\u0421\u041e\u041d\u0410\u0416",
            "\u0421\u0426\u0415\u041d\u0410",
            "\u041f\u0410\u041d\u0415\u041b\u042c",
            "\u0414\u0410\u041d\u041d\u042b\u0415",
            "\u042d\u0424\u0424\u0415\u041a\u0422\u042b"
    };
    private static final String[] CATEGORY_TOOLTIPS = {
            "\u041f\u0435\u0440\u0441\u043e\u043d\u0430\u0436: \u043f\u043e\u043a\u0430\u0437\u0430\u0442\u044c, \u0443\u0431\u0440\u0430\u0442\u044c, \u0434\u0432\u0438\u0433\u0430\u0442\u044c",
            "\u0421\u0446\u0435\u043d\u0430: \u0444\u043e\u043d \u0438 \u0437\u0432\u0443\u043a",
            "\u0422\u0435\u043a\u0441\u0442\u043e\u0432\u0430\u044f \u043f\u0430\u043d\u0435\u043b\u044c",
            "\u0414\u0430\u043d\u043d\u044b\u0435: \u0444\u043b\u0430\u0433\u0438, \u0440\u0435\u043f\u0443\u0442\u0430\u0446\u0438\u044f, \u0432\u044b\u0431\u043e\u0440",
            "\u042d\u0444\u0444\u0435\u043a\u0442\u044b: \u0442\u0438\u0442\u0440\u044b, \u043f\u0430\u0437\u043b\u044b, \u0432\u0432\u043e\u0434"
    };

    public CommandPalettePane() {
        setSpacing(0);
        getStyleClass().add("command-palette");

        // Header
        Label header = new Label("\u041f\u0410\u041b\u0418\u0422\u0420\u0410 \u041a\u041e\u041c\u0410\u041d\u0414");
        header.getStyleClass().add("palette-header");
        header.setMaxWidth(Double.MAX_VALUE);
        header.setPadding(new Insets(8, 12, 8, 12));

        // Category bar with toggle buttons
        ToggleGroup categoryGroup = new ToggleGroup();
        HBox categoryBar = new HBox(2);
        categoryBar.setAlignment(Pos.CENTER);
        categoryBar.setPadding(new Insets(6, 4, 6, 4));
        categoryBar.getStyleClass().add("palette-category-bar");

        categoryButtons = new java.util.ArrayList<>();
        for (int i = 0; i < CATEGORY_LABELS.length; i++) {
            ToggleButton btn = new ToggleButton(CATEGORY_LABELS[i]);
            btn.setTooltip(new Tooltip(CATEGORY_TOOLTIPS[i]));
            btn.getStyleClass().add("palette-category-btn");
            btn.setToggleGroup(categoryGroup);
            HBox.setHgrow(btn, Priority.ALWAYS);
            btn.setMaxWidth(Double.MAX_VALUE);
            final int idx = i;
            btn.setOnAction(e -> {
                if (!btn.isSelected()) {
                    btn.setSelected(true);
                }
                selectCategory(idx);
            });
            categoryButtons.add(btn);
            categoryBar.getChildren().add(btn);
        }

        // Category title (shows active category name)
        categoryTitleLabel = new Label(CATEGORY_NAMES[0]);
        categoryTitleLabel.getStyleClass().add("palette-category-title");
        categoryTitleLabel.setMaxWidth(Double.MAX_VALUE);
        categoryTitleLabel.setPadding(new Insets(4, 12, 4, 12));

        // ===== Build all fields =====

        // Character
        charActionBox = new ComboBox<>(FXCollections.observableArrayList(
                "showPerson", "removeFromScene",
                "move_Left", "move_Right", "move_Center",
                "setFromLeft", "setFromRight", "setFromCenter",
                "runToLeft", "runToRight", "AppearEffect"));
        charActionBox.setValue("showPerson");
        charActionBox.getStyleClass().add("palette-combo");
        charActionBox.setMaxWidth(Double.MAX_VALUE);

        charTargetBox = new ComboBox<>();
        charTargetBox.setEditable(true);
        charTargetBox.getStyleClass().add("palette-combo");
        charTargetBox.setMaxWidth(Double.MAX_VALUE);
        charTargetBox.setOnAction(e -> updatePoseList());

        charPoseBox = new ComboBox<>();
        charPoseBox.setEditable(true);
        charPoseBox.getStyleClass().add("palette-combo");
        charPoseBox.setMaxWidth(Double.MAX_VALUE);

        charPoseRow = labeled("\u041f\u043e\u0437\u0430:", charPoseBox);

        charActionBox.setOnAction(e -> {
            boolean needPose = ACTIONS_WITH_POSE.contains(charActionBox.getValue());
            charPoseRow.setVisible(needPose);
            charPoseRow.setManaged(needPose);
        });

        // Background
        bgSelectedLabel = new Label("\u043d\u0435 \u0432\u044b\u0431\u0440\u0430\u043d");
        bgSelectedLabel.getStyleClass().add("palette-path-label");
        bgSelectedLabel.setWrapText(true);

        Button bgPickBtn = new Button("\u0412\u044b\u0431\u0440\u0430\u0442\u044c \u0444\u043e\u043d...");
        bgPickBtn.getStyleClass().add("palette-pick-btn");
        bgPickBtn.setMaxWidth(Double.MAX_VALUE);
        bgPickBtn.setOnAction(e -> pickBackground());

        // Sound
        soundSelectedLabel = new Label("\u043d\u0435 \u0432\u044b\u0431\u0440\u0430\u043d");
        soundSelectedLabel.getStyleClass().add("palette-path-label");
        soundSelectedLabel.setWrapText(true);

        Button soundPickBtn = new Button("\u0412\u044b\u0431\u0440\u0430\u0442\u044c \u0437\u0432\u0443\u043a...");
        soundPickBtn.getStyleClass().add("palette-pick-btn");
        soundPickBtn.setMaxWidth(Double.MAX_VALUE);
        soundPickBtn.setOnAction(e -> pickSound());

        // Text panel
        textPanelActionBox = new ComboBox<>(FXCollections.observableArrayList(
                "openTextTable", "closeTextTable", "OpenDarkText"));
        textPanelActionBox.setValue("openTextTable");
        textPanelActionBox.getStyleClass().add("palette-combo");
        textPanelActionBox.setMaxWidth(Double.MAX_VALUE);

        // Flags
        flagNameField = new TextField();
        flagNameField.setPromptText("flag_name");
        flagNameField.getStyleClass().add("palette-field");

        flagValueBox = new ComboBox<>(FXCollections.observableArrayList("true", "false"));
        flagValueBox.setValue("true");
        flagValueBox.getStyleClass().add("palette-combo");
        flagValueBox.setMaxWidth(Double.MAX_VALUE);

        // Reputation
        repActionBox = new ComboBox<>(FXCollections.observableArrayList(
                "SetReputation", "AppendReputathion", "ReduceReputathion"));
        repActionBox.setValue("AppendReputathion");
        repActionBox.getStyleClass().add("palette-combo");
        repActionBox.setMaxWidth(Double.MAX_VALUE);

        repTargetBox = new ComboBox<>();
        repTargetBox.setEditable(true);
        repTargetBox.getStyleClass().add("palette-combo");
        repTargetBox.setMaxWidth(Double.MAX_VALUE);

        repValueSpinner = new Spinner<>(-999, 999, 1);
        repValueSpinner.setEditable(true);
        repValueSpinner.setPrefWidth(80);
        repValueSpinner.getStyleClass().add("palette-spinner");

        // Choice
        choiceKeyField = new TextField();
        choiceKeyField.setPromptText("choice_key");
        choiceKeyField.getStyleClass().add("palette-field");

        choiceValueField = new TextField();
        choiceValueField.setPromptText("\u0437\u043d\u0430\u0447\u0435\u043d\u0438\u0435");
        choiceValueField.getStyleClass().add("palette-field");

        // Special effects
        fxActionBox = new ComboBox<>(FXCollections.observableArrayList(
                "StartTitries", "openPuzzle"));
        fxActionBox.setValue("StartTitries");
        fxActionBox.getStyleClass().add("palette-combo");
        fxActionBox.setMaxWidth(Double.MAX_VALUE);

        fxIdField = new ComboBox<>(FXCollections.observableArrayList(
                "memory_match", "tic_tac_toe", "code_input", "evidence_board"));
        fxIdField.setEditable(true);
        fxIdField.setPromptText("puzzle_id");
        fxIdField.getStyleClass().add("palette-combo");
        fxIdField.setMaxWidth(Double.MAX_VALUE);

        fxFlagField = new TextField();
        fxFlagField.setPromptText("flag_name");
        fxFlagField.getStyleClass().add("palette-field");

        fxSuccessSpinner = new Spinner<>(-1, 9999, -1);
        fxSuccessSpinner.setEditable(true);
        fxSuccessSpinner.setPrefWidth(80);
        fxSuccessSpinner.getStyleClass().add("palette-spinner");

        fxFailureSpinner = new Spinner<>(-1, 9999, -1);
        fxFailureSpinner.setEditable(true);
        fxFailureSpinner.setPrefWidth(80);
        fxFailureSpinner.getStyleClass().add("palette-spinner");

        fxPuzzleFields = new VBox(6,
                labeled("ID:", fxIdField),
                labeled("\u0424\u043b\u0430\u0433:", fxFlagField),
                labeled("\u0423\u0441\u043f\u0435\u0445 \u2192:", fxSuccessSpinner),
                labeled("\u041d\u0435\u0443\u0434\u0430\u0447\u0430 \u2192:", fxFailureSpinner));
        fxPuzzleFields.setVisible(false);
        fxPuzzleFields.setManaged(false);

        fxActionBox.setOnAction(e -> {
            boolean isPuzzle = "openPuzzle".equals(fxActionBox.getValue());
            fxPuzzleFields.setVisible(isPuzzle);
            fxPuzzleFields.setManaged(isPuzzle);
        });

        // Input
        inputKeyBox = new ComboBox<>();
        inputKeyBox.setEditable(true);
        inputKeyBox.setPromptText("playerName");
        inputKeyBox.getStyleClass().add("palette-combo");
        inputKeyBox.setMaxWidth(Double.MAX_VALUE);
        inputKeyBox.getItems().addAll("playerName", "playerSurname", "playerNickname");

        inputPromptField = new TextField();
        inputPromptField.setPromptText("\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u0438\u043c\u044f:");
        inputPromptField.getStyleClass().add("palette-field");

        // Visual effects — simple: effect name + on/off
        vfxNameField = new ComboBox<>();
        vfxNameField.setEditable(true);
        vfxNameField.setPromptText("blink");
        vfxNameField.getStyleClass().add("palette-combo");
        vfxNameField.setMaxWidth(Double.MAX_VALUE);

        vfxActionBox = new ComboBox<>(FXCollections.observableArrayList(
                "\u0412\u043a\u043b\u044e\u0447\u0438\u0442\u044c", "\u0412\u044b\u043a\u043b\u044e\u0447\u0438\u0442\u044c"));
        vfxActionBox.setValue("\u0412\u043a\u043b\u044e\u0447\u0438\u0442\u044c");
        vfxActionBox.getStyleClass().add("palette-combo");
        vfxActionBox.setMaxWidth(Double.MAX_VALUE);

        // ===== Build category panes =====
        VBox charCategoryPane = buildCharCategory();
        VBox sceneCategoryPane = buildSceneCategory(bgPickBtn, soundPickBtn);
        VBox panelCategoryPane = buildPanelCategory();
        VBox dataCategoryPane = buildDataCategory();
        VBox fxCategoryPane = buildFxCategory();

        categoryPanes = List.of(charCategoryPane, sceneCategoryPane,
                panelCategoryPane, dataCategoryPane, fxCategoryPane);

        // Content area
        StackPane contentArea = new StackPane();
        contentArea.getStyleClass().add("palette-content-area");
        contentArea.getChildren().addAll(categoryPanes);

        ScrollPane scroll = new ScrollPane(contentArea);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("palette-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(header, categoryBar, categoryTitleLabel, scroll);

        // Select first category by default
        categoryButtons.getFirst().setSelected(true);
        selectCategory(0);
    }

    // ===== Category builders =====

    private VBox buildCharCategory() {
        Button insertBtn = createInsertButton();
        insertBtn.setOnAction(e -> insertCharacterCommand());

        VBox pane = new VBox(8);
        pane.setPadding(new Insets(10));
        pane.getStyleClass().add("palette-category-content");
        pane.getChildren().addAll(
                labeled("\u0414\u0435\u0439\u0441\u0442\u0432\u0438\u0435:", charActionBox),
                labeled("\u041f\u0435\u0440\u0441\u043e\u043d\u0430\u0436:", charTargetBox),
                charPoseRow,
                insertBtn);
        return pane;
    }

    private VBox buildSceneCategory(Button bgPickBtn, Button soundPickBtn) {
        Button bgInsertBtn = createInsertButton();
        bgInsertBtn.setOnAction(e -> insertBackgroundCommand());

        Button soundInsertBtn = createInsertButton();
        soundInsertBtn.setOnAction(e -> insertSoundCommand());

        VBox bgBlock = createSubSection("\u0424\u043e\u043d",
                bgPickBtn, bgSelectedLabel, bgInsertBtn);
        VBox soundBlock = createSubSection("\u0417\u0432\u0443\u043a",
                soundPickBtn, soundSelectedLabel, soundInsertBtn);

        VBox pane = new VBox(14);
        pane.setPadding(new Insets(10));
        pane.getStyleClass().add("palette-category-content");
        pane.getChildren().addAll(bgBlock, soundBlock);
        return pane;
    }

    private VBox buildPanelCategory() {
        Button insertBtn = createInsertButton();
        insertBtn.setOnAction(e -> insertTextPanelCommand());

        VBox pane = new VBox(8);
        pane.setPadding(new Insets(10));
        pane.getStyleClass().add("palette-category-content");
        pane.getChildren().addAll(
                labeled("\u0414\u0435\u0439\u0441\u0442\u0432\u0438\u0435:", textPanelActionBox),
                insertBtn);
        return pane;
    }

    private VBox buildDataCategory() {
        Button flagInsertBtn = createInsertButton();
        flagInsertBtn.setOnAction(e -> insertFlagCommand());
        VBox flagBlock = createSubSection("\u0424\u043b\u0430\u0433\u0438",
                labeled("\u0418\u043c\u044f:", flagNameField),
                labeled("\u0417\u043d\u0430\u0447\u0435\u043d\u0438\u0435:", flagValueBox),
                flagInsertBtn);

        Button repInsertBtn = createInsertButton();
        repInsertBtn.setOnAction(e -> insertReputationCommand());
        VBox repBlock = createSubSection("\u0420\u0435\u043f\u0443\u0442\u0430\u0446\u0438\u044f",
                labeled("\u0414\u0435\u0439\u0441\u0442\u0432\u0438\u0435:", repActionBox),
                labeled("\u041f\u0435\u0440\u0441\u043e\u043d\u0430\u0436:", repTargetBox),
                labeled("\u0417\u043d\u0430\u0447\u0435\u043d\u0438\u0435:", repValueSpinner),
                repInsertBtn);

        Button choiceInsertBtn = createInsertButton();
        choiceInsertBtn.setOnAction(e -> insertChoiceCommand());
        VBox choiceBlock = createSubSection("\u0412\u044b\u0431\u043e\u0440 \u0438\u0433\u0440\u043e\u043a\u0430",
                labeled("\u041a\u043b\u044e\u0447:", choiceKeyField),
                labeled("\u0417\u043d\u0430\u0447\u0435\u043d\u0438\u0435:", choiceValueField),
                choiceInsertBtn);

        VBox pane = new VBox(14);
        pane.setPadding(new Insets(10));
        pane.getStyleClass().add("palette-category-content");
        pane.getChildren().addAll(flagBlock, repBlock, choiceBlock);
        return pane;
    }

    private VBox buildFxCategory() {
        Button fxInsertBtn = createInsertButton();
        fxInsertBtn.setOnAction(e -> insertFxCommand());
        VBox fxBlock = createSubSection("\u0421\u043f\u0435\u0446\u044d\u0444\u0444\u0435\u043a\u0442\u044b",
                labeled("\u0414\u0435\u0439\u0441\u0442\u0432\u0438\u0435:", fxActionBox),
                fxPuzzleFields,
                fxInsertBtn);

        Button vfxInsertBtn = createInsertButton();
        vfxInsertBtn.setOnAction(e -> insertVfxCommand());
        VBox vfxBlock = createSubSection("\u0412\u0438\u0437\u0443\u0430\u043b\u044c\u043d\u044b\u0435 \u044d\u0444\u0444\u0435\u043a\u0442\u044b",
                labeled("\u042d\u0444\u0444\u0435\u043a\u0442:", vfxNameField),
                labeled("\u0414\u0435\u0439\u0441\u0442\u0432\u0438\u0435:", vfxActionBox),
                vfxInsertBtn);

        Button inputInsertBtn = createInsertButton();
        inputInsertBtn.setOnAction(e -> insertInputCommand());
        VBox inputBlock = createSubSection("\u0412\u0432\u043e\u0434 \u0438\u0433\u0440\u043e\u043a\u0430",
                labeled("\u041a\u043b\u044e\u0447:", inputKeyBox),
                labeled("\u041f\u043e\u0434\u0441\u043a\u0430\u0437\u043a\u0430:", inputPromptField),
                inputInsertBtn);

        VBox pane = new VBox(14);
        pane.setPadding(new Insets(10));
        pane.getStyleClass().add("palette-category-content");
        pane.getChildren().addAll(fxBlock, vfxBlock, inputBlock);
        return pane;
    }

    // ===== Category switching =====

    private void selectCategory(int index) {
        for (int i = 0; i < categoryPanes.size(); i++) {
            boolean active = (i == index);
            categoryPanes.get(i).setVisible(active);
            categoryPanes.get(i).setManaged(active);
        }
        categoryTitleLabel.setText(CATEGORY_NAMES[index]);
        if (!categoryButtons.get(index).isSelected()) {
            categoryButtons.get(index).setSelected(true);
        }
    }

    // ===== Project data =====

    public void setProject(EditorProject project) {
        this.project = project;
        charTargetBox.getItems().clear();
        repTargetBox.getItems().clear();
        if (project != null) {
            for (EditorCharacter ch : project.getCharacters()) {
                charTargetBox.getItems().add(ch.getName());
                repTargetBox.getItems().add(ch.getName());
            }
            if (!charTargetBox.getItems().isEmpty()) {
                charTargetBox.setValue(charTargetBox.getItems().getFirst());
                repTargetBox.setValue(repTargetBox.getItems().getFirst());
                updatePoseList();
            }
            refreshInputKeys(project);
            refreshEffectNames(project);
        }
    }

    /**
     * Scans all scenes/frames for existing input command keys and
     * adds them to the key combobox so they can be reused.
     *
     * @param proj current project
     */
    private void refreshInputKeys(EditorProject proj) {
        Set<String> defaults = Set.of("playerName", "playerSurname", "playerNickname");
        Set<String> keys = new java.util.LinkedHashSet<>(defaults);
        for (var scene : proj.getScenes()) {
            for (var frame : scene.getFrames()) {
                for (var cmd : frame.getCommands()) {
                    if ("input".equals(cmd.getType()) && cmd.getKey() != null && !cmd.getKey().isEmpty()) {
                        keys.add(cmd.getKey());
                    }
                }
            }
        }
        inputKeyBox.getItems().setAll(keys);
    }

    /**
     * Scans all scenes/frames for effect names used in effect commands
     * and populates the VFX name combo box.
     *
     * @param proj current project
     */
    private void refreshEffectNames(EditorProject proj) {
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
        for (var scene : proj.getScenes()) {
            for (var frame : scene.getFrames()) {
                for (var cmd : frame.getCommands()) {
                    if ("effect".equals(cmd.getType())
                            && cmd.getEffect() != null && !cmd.getEffect().isEmpty()) {
                        names.add(cmd.getEffect());
                    }
                }
            }
        }
        vfxNameField.getItems().setAll(names);
    }

    public void setResources(ProjectResources resources) {
        this.resources = resources;
    }

    private void updatePoseList() {
        charPoseBox.getItems().clear();
        if (project == null) return;
        String target = charTargetBox.getValue();
        for (EditorCharacter ch : project.getCharacters()) {
            if (ch.getName().equals(target)) {
                charPoseBox.getItems().addAll(ch.getPoseNames());
                if (!charPoseBox.getItems().isEmpty()) {
                    charPoseBox.setValue(charPoseBox.getItems().getFirst());
                }
                break;
            }
        }
    }

    // ===== Insert methods =====

    private void insertCharacterCommand() {
        EditorCommand cmd = new EditorCommand();
        cmd.setType("character");
        cmd.setAction(charActionBox.getValue());
        cmd.setTarget(charTargetBox.getValue());
        if (ACTIONS_WITH_POSE.contains(cmd.getAction())) {
            cmd.setValue(charPoseBox.getValue());
        }
        fireInsert(cmd);
    }

    private void insertBackgroundCommand() {
        String path = (String) bgSelectedLabel.getUserData();
        if (path == null || path.isEmpty()) return;
        EditorCommand cmd = new EditorCommand();
        cmd.setType("background");
        cmd.setAction("changebackground");
        cmd.setValue(path);
        fireInsert(cmd);
    }

    private void insertSoundCommand() {
        String path = (String) soundSelectedLabel.getUserData();
        if (path == null || path.isEmpty()) return;
        EditorCommand cmd = new EditorCommand();
        cmd.setType("character");
        cmd.setAction("SoundEffect");
        cmd.setValue(path);
        fireInsert(cmd);
    }

    private void insertTextPanelCommand() {
        EditorCommand cmd = new EditorCommand();
        cmd.setType("character");
        cmd.setAction(textPanelActionBox.getValue());
        fireInsert(cmd);
    }

    private void insertFlagCommand() {
        String name = flagNameField.getText();
        if (name == null || name.isEmpty()) return;
        EditorCommand cmd = new EditorCommand();
        cmd.setType("character");
        cmd.setAction("SetFlag");
        cmd.setTarget(name);
        cmd.setValue(flagValueBox.getValue());
        fireInsert(cmd);
    }

    private void insertReputationCommand() {
        EditorCommand cmd = new EditorCommand();
        cmd.setType("character");
        cmd.setAction(repActionBox.getValue());
        cmd.setTarget(repTargetBox.getValue());
        cmd.setValue(String.valueOf(repValueSpinner.getValue()));
        fireInsert(cmd);
    }

    private void insertChoiceCommand() {
        String key = choiceKeyField.getText();
        if (key == null || key.isEmpty()) return;
        EditorCommand cmd = new EditorCommand();
        cmd.setType("character");
        cmd.setAction("SetChoice");
        cmd.setTarget(key);
        cmd.setValue(choiceValueField.getText());
        fireInsert(cmd);
    }

    private void insertFxCommand() {
        EditorCommand cmd = new EditorCommand();
        String action = fxActionBox.getValue();
        if ("openPuzzle".equals(action)) {
            cmd.setType("puzzle");
            cmd.setAction("openPuzzle");
            cmd.setId(fxIdField.getValue());
            cmd.setFlag(fxFlagField.getText());
            cmd.setOnSuccess(fxSuccessSpinner.getValue());
            cmd.setOnFailure(fxFailureSpinner.getValue());
        } else {
            cmd.setType("character");
            cmd.setAction(action);
        }
        fireInsert(cmd);
    }

    /**
     * Inserts a visual effect command — either start or stop the named effect.
     */
    private void insertVfxCommand() {
        String name = vfxNameField.getValue();
        if (name == null || name.isEmpty()) return;

        EditorCommand cmd = new EditorCommand();
        cmd.setType("effect");
        cmd.setEffect(name);

        boolean turnOn = "\u0412\u043a\u043b\u044e\u0447\u0438\u0442\u044c".equals(vfxActionBox.getValue());
        cmd.setAction(turnOn ? "start" : "stop");
        fireInsert(cmd);
    }

    private void insertInputCommand() {
        EditorCommand cmd = new EditorCommand();
        cmd.setType("input");
        cmd.setKey(inputKeyBox.getValue());
        cmd.setPrompt(inputPromptField.getText());
        fireInsert(cmd);
    }

    // ===== Resource pickers =====

    private void pickBackground() {
        if (resources == null || project == null) return;
        Stage ownerStage = (Stage) getScene().getWindow();
        ResourcePickerDialog picker = new ResourcePickerDialog(ownerStage, resources.getProjectDir(), true);
        String path = picker.showAndWait("\u0412\u044b\u0431\u043e\u0440 \u0444\u043e\u043d\u0430", resources.getBackgrounds());
        if (path != null) {
            bgSelectedLabel.setText(resources.getDisplayName(path));
            bgSelectedLabel.setUserData(path);
        }
    }

    private void pickSound() {
        if (resources == null || project == null) return;
        Stage ownerStage = (Stage) getScene().getWindow();
        ResourcePickerDialog picker = new ResourcePickerDialog(ownerStage, resources.getProjectDir(), false);
        String path = picker.showAndWait("\u0412\u044b\u0431\u043e\u0440 \u0437\u0432\u0443\u043a\u0430", resources.getSounds());
        if (path != null) {
            soundSelectedLabel.setText(resources.getDisplayName(path));
            soundSelectedLabel.setUserData(path);
        }
    }

    private void fireInsert(EditorCommand cmd) {
        if (onCommandInsert != null) onCommandInsert.accept(cmd);
    }

    public void setOnCommandInsert(Consumer<EditorCommand> handler) {
        this.onCommandInsert = handler;
    }

    // ===== UI Helpers =====

    private VBox createSubSection(String title, Node... children) {
        Label titleLabel = new Label("\u2500\u2500 " + title + " \u2500\u2500");
        titleLabel.getStyleClass().add("palette-subsection-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        VBox box = new VBox(6);
        box.getStyleClass().add("palette-subsection");
        box.getChildren().add(titleLabel);
        box.getChildren().addAll(children);
        return box;
    }

    private HBox labeled(String text, Node field) {
        Label label = new Label(text);
        label.getStyleClass().add("palette-label");
        label.setMinWidth(75);
        HBox row = new HBox(8, label, field);
        row.setAlignment(Pos.CENTER_LEFT);
        if (field instanceof Region r) HBox.setHgrow(r, Priority.ALWAYS);
        return row;
    }

    private Button createInsertButton() {
        Button btn = new Button("\u0412\u0441\u0442\u0430\u0432\u0438\u0442\u044c");
        btn.getStyleClass().add("palette-insert-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }
}
