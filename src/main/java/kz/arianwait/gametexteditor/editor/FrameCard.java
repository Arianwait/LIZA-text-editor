package kz.arianwait.gametexteditor.editor;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import kz.arianwait.gametexteditor.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FrameCard extends VBox {

    private final EditorFrame frame;
    private final int frameIndex;
    private final List<EditorCharacter> characters;
    private final List<EditorVariable> variables;
    private boolean selected = false;

    private Runnable onChanged;
    private Consumer<FrameCard> onSelected;
    private Runnable onMoveUp;
    private Runnable onMoveDown;
    private Runnable onDuplicate;
    private Runnable onDelete;
    private Consumer<EditorCommand> onEditCommand;

    public static final DataFormat FRAME_INDEX_FORMAT = new DataFormat("application/x-frame-index");

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{(\\w+)}");

    private final FlowPane commandsPane;
    private final VBox choicesBox;
    private final FlowPane variableBadgesPane;

    public FrameCard(EditorFrame frame, int frameIndex, List<EditorCharacter> characters) {
        this(frame, frameIndex, characters, List.of());
    }

    public FrameCard(EditorFrame frame, int frameIndex, List<EditorCharacter> characters,
                     List<EditorVariable> variables) {
        this.frame = frame;
        this.frameIndex = frameIndex;
        this.characters = characters;
        this.variables = variables;

        setSpacing(6);
        setPadding(new Insets(10));
        getStyleClass().add("frame-card");

        setOnMouseClicked(e -> {
            if (onSelected != null) onSelected.accept(this);
        });

        // --- Header row ---
        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);

        Label indexLabel = new Label("#" + (frameIndex + 1));
        indexLabel.getStyleClass().add("frame-card-index");

        Button upBtn = new Button("\u25b2");
        upBtn.getStyleClass().add("frame-card-btn");
        upBtn.setOnAction(e -> { if (onMoveUp != null) onMoveUp.run(); });

        Button downBtn = new Button("\u25bc");
        downBtn.getStyleClass().add("frame-card-btn");
        downBtn.setOnAction(e -> { if (onMoveDown != null) onMoveDown.run(); });

        Button dupBtn = new Button("\u29c9");
        dupBtn.getStyleClass().add("frame-card-btn");
        dupBtn.setTooltip(new Tooltip("\u0414\u0443\u0431\u043b\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u043a\u0430\u0434\u0440"));
        dupBtn.setOnAction(e -> { if (onDuplicate != null) onDuplicate.run(); });

        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList(
                "CHARACTER", "OVERLAY"));
        typeBox.getStyleClass().add("frame-card-type");
        typeBox.setValue(frame.getType().name());
        typeBox.setOnAction(e -> {
            frame.setType(EditorFrame.FrameType.valueOf(typeBox.getValue()));
            fireChanged();
            rebuildContent();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button deleteBtn = new Button("\u2716");
        deleteBtn.getStyleClass().add("frame-card-delete");
        deleteBtn.setOnAction(e -> { if (onDelete != null) onDelete.run(); });

        header.getChildren().addAll(indexLabel, upBtn, downBtn, dupBtn, typeBox, spacer, deleteBtn);

        // Drag support — drag from header
        header.setOnDragDetected(ev -> {
            Dragboard db = startDragAndDrop(TransferMode.MOVE);
            ClipboardContent cc = new ClipboardContent();
            cc.put(FRAME_INDEX_FORMAT, frameIndex);
            db.setContent(cc);
            getStyleClass().add("frame-card-dragging");
            ev.consume();
        });
        setOnDragDone(ev -> {
            getStyleClass().remove("frame-card-dragging");
            ev.consume();
        });

        getChildren().add(header);

        // --- Content (type-dependent) ---
        commandsPane = new FlowPane(6, 4);
        commandsPane.getStyleClass().add("frame-commands-pane");

        choicesBox = new VBox(4);
        choicesBox.getStyleClass().add("frame-choices-box");

        variableBadgesPane = new FlowPane(4, 4);
        variableBadgesPane.getStyleClass().add("variable-badges-pane");

        rebuildContent();
    }

    private void rebuildContent() {
        // Remove everything except header
        if (getChildren().size() > 1) {
            getChildren().remove(1, getChildren().size());
        }

        if (frame.getType() == EditorFrame.FrameType.CHARACTER) {
            buildCharacterFields();
        } else {
            buildOverlayFields();
        }

        // Commands section
        Label cmdLabel = new Label("\u041a\u043e\u043c\u0430\u043d\u0434\u044b:");
        cmdLabel.getStyleClass().add("frame-section-label");
        rebuildCommandBadges();
        Button addCmdBtn = new Button("+ \u041a\u043e\u043c\u0430\u043d\u0434\u0430");
        addCmdBtn.getStyleClass().add("frame-add-btn");
        addCmdBtn.setOnAction(e -> {
            // Placeholder — will be wired to command palette
            EditorCommand cmd = new EditorCommand();
            cmd.setType("character");
            cmd.setAction("showPerson");
            frame.getCommands().add(cmd);
            rebuildCommandBadges();
            fireChanged();
        });
        HBox cmdHeader = new HBox(8, cmdLabel, addCmdBtn);
        cmdHeader.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(cmdHeader, commandsPane);

        // Choices section
        Label choicesLabel = new Label("\u0412\u044b\u0431\u043e\u0440\u044b:");
        choicesLabel.getStyleClass().add("frame-section-label");
        rebuildChoiceRows();
        Button addChoiceBtn = new Button("+ \u0412\u044b\u0431\u043e\u0440");
        addChoiceBtn.getStyleClass().add("frame-add-btn");
        addChoiceBtn.setOnAction(e -> {
            if (frame.getChoices() == null) frame.setChoices(new ArrayList<>());
            EditorChoice ch = new EditorChoice();
            ch.setText("\u041d\u043e\u0432\u044b\u0439 \u0432\u044b\u0431\u043e\u0440");
            ch.setRequestsId(-1);
            frame.getChoices().add(ch);
            rebuildChoiceRows();
            fireChanged();
        });
        HBox choicesHeader = new HBox(8, choicesLabel, addChoiceBtn);
        choicesHeader.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(choicesHeader, choicesBox);
    }

    private void buildCharacterFields() {
        // Speaker name
        HBox nameRow = new HBox(8);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label("\u0418\u043c\u044f:");
        nameLabel.getStyleClass().add("frame-field-label");
        nameLabel.setMinWidth(50);

        ComboBox<String> nameBox = new ComboBox<>();
        nameBox.setEditable(true);
        nameBox.getStyleClass().add("frame-field-combo");
        for (EditorCharacter ch : characters) {
            nameBox.getItems().add(ch.getName());
        }
        for (EditorVariable v : variables) {
            nameBox.getItems().add("{" + v.getKey() + "}");
        }
        nameBox.setValue(frame.getSpeakerName());
        nameBox.setPrefWidth(150);

        Label colorLabel = new Label("\u0426\u0432\u0435\u0442:");
        colorLabel.getStyleClass().add("frame-field-label");

        TextField colorField = new TextField(frame.getSpeakerColor());
        colorField.setPrefWidth(80);

        nameBox.valueProperty().addListener((o, ov, nv) -> {
            frame.setSpeakerName(nv != null ? nv : "");
            String autoColor = findColorForName(nv);
            if (autoColor != null) {
                frame.setSpeakerColor(autoColor);
                colorField.setText(autoColor);
            }
            fireChanged();
        });
        colorField.getStyleClass().add("frame-field-input");
        colorField.textProperty().addListener((o, ov, nv) -> {
            frame.setSpeakerColor(nv);
            fireChanged();
        });

        nameRow.getChildren().addAll(nameLabel, nameBox, colorLabel, colorField);
        getChildren().add(nameRow);

        // Style (optional)
        if (frame.getStyle() != null && !frame.getStyle().isEmpty()) {
            HBox styleRow = new HBox(8);
            styleRow.setAlignment(Pos.CENTER_LEFT);
            Label styleLabel = new Label("\u0421\u0442\u0438\u043b\u044c:");
            styleLabel.getStyleClass().add("frame-field-label");
            styleLabel.setMinWidth(50);
            TextField styleField = new TextField(frame.getStyle());
            styleField.getStyleClass().add("frame-field-input");
            styleField.textProperty().addListener((o, ov, nv) -> {
                frame.setStyle(nv);
                fireChanged();
            });
            styleRow.getChildren().addAll(styleLabel, styleField);
            getChildren().add(styleRow);
        }

        // Text area
        TextArea textArea = new TextArea(frame.getText());
        textArea.setPromptText("\u0422\u0435\u043a\u0441\u0442 \u0434\u0438\u0430\u043b\u043e\u0433\u0430...");
        textArea.setPrefRowCount(3);
        textArea.setWrapText(true);
        textArea.getStyleClass().add("frame-text-area");
        textArea.textProperty().addListener((o, ov, nv) -> {
            frame.setText(nv);
            rebuildVariableBadges();
            fireChanged();
        });

        Button insertVarBtn = createInsertVariableButton(textArea);
        HBox textRow = new HBox(4, textArea, insertVarBtn);
        textRow.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(textArea, Priority.ALWAYS);
        getChildren().add(textRow);
        rebuildVariableBadges();
        getChildren().add(variableBadgesPane);
    }

    private void buildOverlayFields() {
        TextArea textArea = new TextArea(frame.getText());
        textArea.setPromptText("\u0422\u0435\u043a\u0441\u0442 \u043e\u0432\u0435\u0440\u043b\u0435\u044f...");
        textArea.setPrefRowCount(3);
        textArea.setWrapText(true);
        textArea.getStyleClass().add("frame-text-area");
        textArea.textProperty().addListener((o, ov, nv) -> {
            frame.setText(nv);
            rebuildVariableBadges();
            fireChanged();
        });

        Button insertVarBtn = createInsertVariableButton(textArea);
        HBox textRow = new HBox(4, textArea, insertVarBtn);
        textRow.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(textArea, Priority.ALWAYS);
        getChildren().add(textRow);
        rebuildVariableBadges();
        getChildren().add(variableBadgesPane);
    }

    private void rebuildVariableBadges() {
        variableBadgesPane.getChildren().clear();
        String text = frame.getText();
        if (text == null || text.isEmpty()) return;
        Matcher matcher = VAR_PATTERN.matcher(text);
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        while (matcher.find()) {
            seen.add(matcher.group(1));
        }
        for (String varName : seen) {
            Label badge = new Label("{" + varName + "}");
            badge.getStyleClass().add("variable-badge");
            variableBadgesPane.getChildren().add(badge);
        }
        variableBadgesPane.setVisible(!seen.isEmpty());
        variableBadgesPane.setManaged(!seen.isEmpty());
    }

    /**
     * Creates a button that shows a popup menu of available variable keys.
     * Selecting a variable inserts {@code {key}} at the caret position in the text area.
     *
     * @param textArea the text area to insert into
     * @return the insert-variable button
     */
    private Button createInsertVariableButton(TextArea textArea) {
        Button btn = new Button("{x}");
        btn.getStyleClass().add("insert-var-btn");
        btn.setTooltip(new Tooltip("\u0412\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u043f\u0435\u0440\u0435\u043c\u0435\u043d\u043d\u0443\u044e"));
        btn.setOnAction(e -> showVariableMenu(btn, textArea));
        btn.setVisible(!variables.isEmpty());
        btn.setManaged(!variables.isEmpty());
        return btn;
    }

    /**
     * Shows a context menu listing all available variable keys for insertion.
     *
     * @param anchor the node to anchor the menu to
     * @param textArea the text area to insert the variable into
     */
    private void showVariableMenu(Button anchor, TextArea textArea) {
        ContextMenu menu = new ContextMenu();
        for (EditorVariable v : variables) {
            MenuItem item = new MenuItem("{" + v.getKey() + "}");
            item.setOnAction(ev -> {
                int caret = textArea.getCaretPosition();
                textArea.insertText(caret, "{" + v.getKey() + "}");
            });
            menu.getItems().add(item);
        }
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    /**
     * Looks up the color for a speaker name — first among characters, then among variables.
     *
     * @param name the speaker name (may be a character name or {@code {key}})
     * @return the color string, or null if not found
     */
    private String findColorForName(String name) {
        if (name == null) return null;
        for (EditorCharacter ch : characters) {
            if (ch.getName().equals(name)) return ch.getColor();
        }
        if (name.startsWith("{") && name.endsWith("}")) {
            String key = name.substring(1, name.length() - 1);
            for (EditorVariable v : variables) {
                if (key.equals(v.getKey()) && v.getColor() != null && !v.getColor().isEmpty()) {
                    return v.getColor();
                }
            }
        }
        return null;
    }

    private void rebuildCommandBadges() {
        commandsPane.getChildren().clear();
        for (int i = 0; i < frame.getCommands().size(); i++) {
            EditorCommand cmd = frame.getCommands().get(i);
            CommandBadge badge = new CommandBadge(cmd);
            final int idx = i;
            badge.setOnRemove(() -> {
                frame.getCommands().remove(idx);
                rebuildCommandBadges();
                fireChanged();
            });
            badge.setOnEdit(() -> {
                if (onEditCommand != null) onEditCommand.accept(cmd);
            });
            commandsPane.getChildren().add(badge);
        }
    }

    private void rebuildChoiceRows() {
        choicesBox.getChildren().clear();
        if (frame.getChoices() == null) return;
        for (int i = 0; i < frame.getChoices().size(); i++) {
            EditorChoice ch = frame.getChoices().get(i);
            ChoiceRow row = new ChoiceRow(ch);
            final int idx = i;
            row.setOnChanged(this::fireChanged);
            row.setOnRemove(() -> {
                frame.getChoices().remove(idx);
                rebuildChoiceRows();
                fireChanged();
            });
            choicesBox.getChildren().add(row);
        }
    }

    private void fireChanged() {
        if (onChanged != null) onChanged.run();
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            if (!getStyleClass().contains("frame-card-selected"))
                getStyleClass().add("frame-card-selected");
        } else {
            getStyleClass().remove("frame-card-selected");
        }
    }

    public boolean isSelected() { return selected; }
    public EditorFrame getFrame() { return frame; }
    public int getFrameIndex() { return frameIndex; }

    public void setOnChanged(Runnable handler) { this.onChanged = handler; }
    public void setOnSelected(Consumer<FrameCard> handler) { this.onSelected = handler; }
    public void setOnMoveUp(Runnable handler) { this.onMoveUp = handler; }
    public void setOnMoveDown(Runnable handler) { this.onMoveDown = handler; }
    public void setOnDuplicate(Runnable handler) { this.onDuplicate = handler; }
    public void setOnDelete(Runnable handler) { this.onDelete = handler; }
    public void setOnEditCommand(Consumer<EditorCommand> handler) { this.onEditCommand = handler; }

    public void addCommand(EditorCommand cmd) {
        frame.getCommands().add(cmd);
        rebuildCommandBadges();
        fireChanged();
    }
}
