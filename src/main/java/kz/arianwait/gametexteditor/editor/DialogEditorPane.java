package kz.arianwait.gametexteditor.editor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import kz.arianwait.gametexteditor.model.*;
import kz.arianwait.gametexteditor.palette.ResourcePickerField;
import kz.arianwait.gametexteditor.util.DeepCopyUtil;
import kz.arianwait.gametexteditor.util.ProjectResources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DialogEditorPane extends VBox {

    private EditorScene currentScene;
    private EditorProject project;
    private ProjectResources resources;
    private final VBox frameContainer;
    private final ScrollPane scrollPane;
    private final ResourcePickerField bgPicker;
    private final ResourcePickerField musicPicker;

    private FrameCard selectedCard;
    private BiConsumer<EditorFrame, Integer> onFrameSelected;
    private Consumer<EditorScene> onSceneChanged;
    private Runnable onBackToGraph;
    private Consumer<EditorCommand> onEditCommand;

    public DialogEditorPane() {
        getStyleClass().add("dialog-editor");
        setSpacing(0);

        // Toolbar
        HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(8, 12, 8, 12));
        toolbar.getStyleClass().add("editor-toolbar");

        Button backBtn = new Button("\u2190 \u0413\u0440\u0430\u0444");
        backBtn.getStyleClass().add("editor-toolbar-btn");
        backBtn.setOnAction(e -> { if (onBackToGraph != null) onBackToGraph.run(); });

        Label titleLabel = new Label();
        titleLabel.getStyleClass().add("editor-toolbar-title");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button addFrameBtn = new Button("+ \u041a\u0430\u0434\u0440");
        addFrameBtn.getStyleClass().add("toolbar-add-btn");
        addFrameBtn.setOnAction(e -> addNewFrame());

        toolbar.getChildren().addAll(backBtn, titleLabel, addFrameBtn);

        // Scene header fields
        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(8);
        headerGrid.setVgap(4);
        headerGrid.setPadding(new Insets(8, 12, 8, 12));
        headerGrid.getStyleClass().add("scene-header");

        Label idLbl = new Label("ID:");
        idLbl.getStyleClass().add("scene-header-label");
        Label idValue = new Label();
        idValue.getStyleClass().add("scene-header-value");

        Label nextLbl = new Label("\u2192 \u0421\u0446\u0435\u043d\u0430:");
        nextLbl.getStyleClass().add("scene-header-label");
        Spinner<Integer> nextSpinner = new Spinner<>(-1, 9999, -1);
        nextSpinner.setEditable(true);
        nextSpinner.setPrefWidth(80);
        nextSpinner.getStyleClass().add("scene-header-spinner");

        Label bgLbl = new Label("\u0424\u043e\u043d:");
        bgLbl.getStyleClass().add("scene-header-label");
        bgPicker = new ResourcePickerField(ResourcePickerField.Kind.BACKGROUND);
        bgPicker.getStyleClass().add("scene-header-field");
        HBox.setHgrow(bgPicker, Priority.ALWAYS);

        Label musicLbl = new Label("\u041c\u0443\u0437\u044b\u043a\u0430:");
        musicLbl.getStyleClass().add("scene-header-label");
        musicPicker = new ResourcePickerField(ResourcePickerField.Kind.SOUND);
        musicPicker.getStyleClass().add("scene-header-field");

        headerGrid.add(idLbl, 0, 0);
        headerGrid.add(idValue, 1, 0);
        headerGrid.add(nextLbl, 2, 0);
        headerGrid.add(nextSpinner, 3, 0);
        headerGrid.add(bgLbl, 0, 1);
        headerGrid.add(bgPicker, 1, 1, 3, 1);
        headerGrid.add(musicLbl, 0, 2);
        headerGrid.add(musicPicker, 1, 2, 3, 1);

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setMinWidth(60);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setMinWidth(70);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setMinWidth(80);
        headerGrid.getColumnConstraints().addAll(col0, col1, col2, col3);

        // Wire header fields on change
        nextSpinner.valueProperty().addListener((o, ov, nv) -> {
            if (currentScene != null) {
                currentScene.setNextSceneId(nv);
                fireSceneChanged();
            }
        });
        bgPicker.valueProperty().addListener((o, ov, nv) -> {
            if (currentScene != null) {
                currentScene.setBackground(nv);
                fireSceneChanged();
            }
        });
        musicPicker.valueProperty().addListener((o, ov, nv) -> {
            if (currentScene != null) {
                currentScene.setMusic(nv);
                fireSceneChanged();
            }
        });

        // Frame container
        frameContainer = new VBox(8);
        frameContainer.setPadding(new Insets(8, 12, 8, 12));
        frameContainer.getStyleClass().add("frame-container");

        scrollPane = new ScrollPane(frameContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("frame-scroll-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(toolbar, headerGrid, scrollPane);

        // Store grid refs
        this.setUserData(new Object[] { titleLabel, idValue, nextSpinner });
    }

    public void loadScene(EditorScene scene, EditorProject project) {
        this.currentScene = scene;
        this.project = project;
        this.selectedCard = null;

        // Update header fields
        Object[] refs = (Object[]) getUserData();
        Label titleLabel = (Label) refs[0];
        Label idValue = (Label) refs[1];
        @SuppressWarnings("unchecked")
        Spinner<Integer> nextSpinner = (Spinner<Integer>) refs[2];

        titleLabel.setText("\u0421\u0446\u0435\u043d\u0430 " + scene.getId());
        idValue.setText(String.valueOf(scene.getId()));
        nextSpinner.getValueFactory().setValue(scene.getNextSceneId());
        bgPicker.setValue(scene.getBackground());
        musicPicker.setValue(scene.getMusic());

        rebuildFrameCards();

        // Select first frame
        if (!frameContainer.getChildren().isEmpty()) {
            selectCard((FrameCard) frameContainer.getChildren().getFirst());
        }
    }

    public void rebuildFrameCards() {
        frameContainer.getChildren().clear();
        List<EditorCharacter> chars = project != null ? project.getCharacters() : new ArrayList<>();
        List<EditorVariable> vars = project != null ? project.getVariables() : new ArrayList<>();

        for (int i = 0; i < currentScene.getFrames().size(); i++) {
            EditorFrame frame = currentScene.getFrames().get(i);
            FrameCard card = createFrameCard(frame, i, chars, vars);
            frameContainer.getChildren().add(card);
        }
    }

    private FrameCard createFrameCard(EditorFrame frame, int index,
                                       List<EditorCharacter> chars, List<EditorVariable> vars) {
        FrameCard card = new FrameCard(frame, index, chars, vars);

        card.setOnSelected(this::selectCard);
        card.setOnChanged(this::fireSceneChanged);

        card.setOnMoveUp(() -> {
            int idx = currentScene.getFrames().indexOf(frame);
            if (idx > 0) {
                Collections.swap(currentScene.getFrames(), idx, idx - 1);
                rebuildFrameCards();
                selectFrame(idx - 1);
                fireSceneChanged();
            }
        });
        card.setOnMoveDown(() -> {
            int idx = currentScene.getFrames().indexOf(frame);
            if (idx < currentScene.getFrames().size() - 1) {
                Collections.swap(currentScene.getFrames(), idx, idx + 1);
                rebuildFrameCards();
                selectFrame(idx + 1);
                fireSceneChanged();
            }
        });
        card.setOnDuplicate(() -> {
            EditorFrame copy = DeepCopyUtil.copyFrame(frame);
            int insertAt = currentScene.getFrames().indexOf(frame) + 1;
            currentScene.getFrames().add(insertAt, copy);
            rebuildFrameCards();
            selectFrame(insertAt);
            fireSceneChanged();
        });
        card.setOnDelete(() -> {
            int idx = currentScene.getFrames().indexOf(frame);
            currentScene.getFrames().remove(frame);
            rebuildFrameCards();
            int scrollTo = Math.min(idx, currentScene.getFrames().size() - 1);
            if (scrollTo >= 0) selectFrame(scrollTo);
            fireSceneChanged();
        });
        card.setOnEditCommand(cmd -> {
            if (onEditCommand != null) onEditCommand.accept(cmd);
        });

        // Drag & drop — accept drops
        card.setOnDragOver(ev -> {
            if (ev.getGestureSource() != card && ev.getDragboard().hasContent(FrameCard.FRAME_INDEX_FORMAT)) {
                ev.acceptTransferModes(TransferMode.MOVE);
                // Show drop indicator
                double y = ev.getY();
                boolean topHalf = y < card.getHeight() / 2;
                card.getStyleClass().removeAll("frame-card-drag-over-top", "frame-card-drag-over-bottom");
                card.getStyleClass().add(topHalf ? "frame-card-drag-over-top" : "frame-card-drag-over-bottom");
            }
            ev.consume();
        });
        card.setOnDragExited(ev -> {
            card.getStyleClass().removeAll("frame-card-drag-over-top", "frame-card-drag-over-bottom");
            ev.consume();
        });
        card.setOnDragDropped(ev -> {
            Dragboard db = ev.getDragboard();
            if (db.hasContent(FrameCard.FRAME_INDEX_FORMAT)) {
                int sourceIdx = (int) db.getContent(FrameCard.FRAME_INDEX_FORMAT);
                int targetIdx = currentScene.getFrames().indexOf(frame);
                double y = ev.getY();
                boolean topHalf = y < card.getHeight() / 2;
                if (!topHalf) targetIdx++;

                if (sourceIdx != targetIdx && sourceIdx >= 0 && sourceIdx < currentScene.getFrames().size()) {
                    EditorFrame moved = currentScene.getFrames().remove(sourceIdx);
                    if (targetIdx > sourceIdx) targetIdx--;
                    if (targetIdx < 0) targetIdx = 0;
                    if (targetIdx > currentScene.getFrames().size()) targetIdx = currentScene.getFrames().size();
                    currentScene.getFrames().add(targetIdx, moved);
                    rebuildFrameCards();
                    selectFrame(targetIdx);
                    fireSceneChanged();
                }
                ev.setDropCompleted(true);
            } else {
                ev.setDropCompleted(false);
            }
            ev.consume();
        });

        return card;
    }

    /**
     * Adds a new empty CHARACTER frame after the currently selected card
     * and scrolls to it. If no card is selected, appends to the end.
     */
    public void addNewFrame() {
        if (currentScene == null) return;
        EditorFrame frame = new EditorFrame();
        frame.setType(EditorFrame.FrameType.CHARACTER);
        frame.setSpeakerName("");
        frame.setSpeakerColor("");
        frame.setText("");
        int insertAt = currentScene.getFrames().size();
        if (selectedCard != null) {
            int selIdx = currentScene.getFrames().indexOf(selectedCard.getFrame());
            if (selIdx >= 0) insertAt = selIdx + 1;
        }
        currentScene.getFrames().add(insertAt, frame);
        rebuildFrameCards();
        selectFrame(insertAt);
        fireSceneChanged();
    }

    private void selectCard(FrameCard card) {
        if (selectedCard != null) selectedCard.setSelected(false);
        selectedCard = card;
        card.setSelected(true);
        if (onFrameSelected != null) {
            int frameIndex = currentScene != null ? currentScene.getFrames().indexOf(card.getFrame()) : -1;
            onFrameSelected.accept(card.getFrame(), frameIndex);
        }
    }

    public void selectFrame(int index) {
        if (index >= 0 && index < frameContainer.getChildren().size()) {
            selectCard((FrameCard) frameContainer.getChildren().get(index));
            scrollToFrame(index);
        }
    }

    /** Прокрутка к указанному кадру */
    public void scrollToFrame(int index) {
        if (index >= 0 && index < frameContainer.getChildren().size()) {
            javafx.application.Platform.runLater(() -> {
                var node = frameContainer.getChildren().get(index);
                double totalHeight = frameContainer.getBoundsInLocal().getHeight();
                if (totalHeight > 0) {
                    double y = node.getBoundsInParent().getMinY();
                    double viewH = scrollPane.getViewportBounds().getHeight();
                    double vvalue = Math.max(0, Math.min(1, (y - viewH / 3) / (totalHeight - viewH)));
                    scrollPane.setVvalue(vvalue);
                }
            });
        }
    }

    private void fireSceneChanged() {
        if (onSceneChanged != null && currentScene != null) {
            onSceneChanged.accept(currentScene);
        }
    }

    public EditorScene getCurrentScene() { return currentScene; }
    public FrameCard getSelectedCard() { return selectedCard; }

    public void setOnFrameSelected(BiConsumer<EditorFrame, Integer> handler) { this.onFrameSelected = handler; }
    public void setOnSceneChanged(Consumer<EditorScene> handler) { this.onSceneChanged = handler; }
    public void setOnBackToGraph(Runnable handler) { this.onBackToGraph = handler; }
    public void setOnEditCommand(Consumer<EditorCommand> handler) { this.onEditCommand = handler; }

    /**
     * Provides the project resource cache used by inline pickers (background, music).
     *
     * @param resources scanned resources for the currently loaded project
     */
    public void setResources(ProjectResources resources) {
        this.resources = resources;
        bgPicker.setResources(resources);
        musicPicker.setResources(resources);
    }

}
