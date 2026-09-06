package kz.arianwait.gametexteditor.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kz.arianwait.gametexteditor.cache.DirtyTracker;
import kz.arianwait.gametexteditor.cache.EditorCache;
import kz.arianwait.gametexteditor.editor.ChapterEditorDialog;
import kz.arianwait.gametexteditor.editor.CharacterEditorPane;
import kz.arianwait.gametexteditor.editor.DialogEditorPane;
import kz.arianwait.gametexteditor.graph.GraphPane;
import kz.arianwait.gametexteditor.io.SceneExporter;
import kz.arianwait.gametexteditor.model.EditorChoice;
import kz.arianwait.gametexteditor.model.EditorCommand;
import kz.arianwait.gametexteditor.model.EditorFrame;
import kz.arianwait.gametexteditor.model.EditorProject;
import kz.arianwait.gametexteditor.model.EditorScene;
import kz.arianwait.gametexteditor.palette.CommandEditDialog;
import kz.arianwait.gametexteditor.palette.CommandPalettePane;
import kz.arianwait.gametexteditor.parser.EdgeExtractor;
import kz.arianwait.gametexteditor.preview.MiniPreviewPane;
import kz.arianwait.gametexteditor.project.NewProjectWizard;
import kz.arianwait.gametexteditor.project.ProjectLoader;
import kz.arianwait.gametexteditor.search.SearchDialog;
import kz.arianwait.gametexteditor.tree.SceneTreeBuilder;
import kz.arianwait.gametexteditor.tree.TreeItemData;
import kz.arianwait.gametexteditor.util.DeepCopyUtil;
import kz.arianwait.gametexteditor.util.FrameTemplateManager;
import kz.arianwait.gametexteditor.util.NavigationHistory;
import kz.arianwait.gametexteditor.util.ProjectResources;
import kz.arianwait.gametexteditor.palette.ResourceManagerDialog;
import kz.arianwait.gametexteditor.util.ResourceImporter;
import kz.arianwait.gametexteditor.util.RecentProjects;
import kz.arianwait.gametexteditor.util.TextStatistics;
import kz.arianwait.gametexteditor.util.UndoManager;
import kz.arianwait.gametexteditor.validation.ProjectValidator;
import kz.arianwait.gametexteditor.validation.ValidationDialog;
import kz.arianwait.gametexteditor.writer.ChaptersXmlWriter;
import kz.arianwait.gametexteditor.writer.DialogXmlWriter;
import kz.arianwait.gametexteditor.writer.PersonXmlWriter;

public class MainController {

    private final Stage stage;
    private final BorderPane view;
    private final SplitPane splitPane;
    private final VBox leftPanel;
    private final GraphPane graphPane;
    private final Label statusLabel;

    // Editor components
    private final DialogEditorPane dialogEditor;
    private final CharacterEditorPane characterEditor;
    private final MiniPreviewPane previewPane;
    private final CommandPalettePane commandPalette;
    private final VBox rightPanel;

    // State
    private EditorProject currentProject;
    private ProjectResources projectResources;
    private TreeView<TreeItemData> sceneTree;
    private boolean inEditorMode = false;
    private EditorFrame clipboardFrame = null;
    private TextField treeFilterField;
    private boolean suppressTreeSync = false;

    // Systems
    private final DirtyTracker dirtyTracker = new DirtyTracker();
    private final EditorCache editorCache = new EditorCache();
    private final UndoManager undoManager = new UndoManager();
    private final NavigationHistory navigationHistory = new NavigationHistory();

    public MainController(Stage stage) {
        this.stage = stage;
        view = new BorderPane();
        view.getStyleClass().add("main-content");

        // Menu bar
        MenuBar menuBar = buildMenuBar();
        view.setTop(menuBar);

        // Left panel (tree)
        leftPanel = new VBox();
        leftPanel.getStyleClass().add("left-panel");
        leftPanel.setMinWidth(250);

        Label treeHeader = new Label("\u0421\u0442\u0440\u0443\u043a\u0442\u0443\u0440\u0430 \u043f\u0440\u043e\u0435\u043a\u0442\u0430");
        treeHeader.getStyleClass().add("panel-header");
        treeHeader.setMaxWidth(Double.MAX_VALUE);
        treeHeader.setPadding(new Insets(8, 12, 8, 12));

        treeFilterField = new TextField();
        treeFilterField.setPromptText("\u0424\u0438\u043b\u044c\u0442\u0440 \u0441\u0446\u0435\u043d...");
        treeFilterField.getStyleClass().add("tree-filter-field");
        treeFilterField.setVisible(false);
        treeFilterField.setManaged(false);
        treeFilterField.textProperty().addListener((o, ov, nv) -> {
            if (currentProject != null) applyTreeFilter(nv);
        });

        leftPanel.getChildren().addAll(treeHeader, treeFilterField);

        // Graph pane
        graphPane = new GraphPane();

        // Preview pane (created before dialogEditor callbacks that reference it)
        previewPane = new MiniPreviewPane();

        // Dialog editor
        dialogEditor = new DialogEditorPane();
        dialogEditor.setOnBackToGraph(this::closeEditor);
        dialogEditor.setOnFrameSelected((frame, frameIndex) -> {
            previewPane.updatePreview(frame, dialogEditor.getCurrentScene(), frameIndex);
            // F1: синхронизация редактор → дерево
            if (!suppressTreeSync && inEditorMode && sceneTree != null && dialogEditor.getCurrentScene() != null) {
                selectFrameInTree(dialogEditor.getCurrentScene().getId(), frameIndex);
            }
        });
        dialogEditor.setOnSceneChanged(scene -> {
            dirtyTracker.markDirty();
            EditorFrame selectedFrame = dialogEditor.getSelectedCard() != null ? dialogEditor.getSelectedCard().getFrame() : null;
            int idx = selectedFrame != null ? scene.getFrames().indexOf(selectedFrame) : -1;
            previewPane.updatePreview(selectedFrame, scene, idx);
        });
        dialogEditor.setOnEditCommand(cmd -> {
            if (currentProject != null) {
                boolean changed = CommandEditDialog.showAndEdit(cmd, currentProject, projectResources, stage);
                if (changed) {
                    dirtyTracker.markDirty();
                    // Reload editor to reflect changes
                    dialogEditor.loadScene(dialogEditor.getCurrentScene(), currentProject);
                }
            }
        });

        // Character editor
        characterEditor = new CharacterEditorPane();
        characterEditor.setOnBackToGraph(this::closeEditor);
        characterEditor.setOnCharactersChanged(() -> {
            dirtyTracker.markDirty();
            refreshProjectUI();
        });

        // Command palette
        commandPalette = new CommandPalettePane();
        commandPalette.setOnCommandInsert(cmd -> {
            if (dialogEditor.getSelectedCard() != null) {
                recordUndoState();
                dialogEditor.getSelectedCard().addCommand(cmd);
                dirtyTracker.markDirty();
            }
        });

        // Right panel (preview + palette)
        rightPanel = new VBox();
        rightPanel.getStyleClass().add("right-panel");
        SplitPane rightSplit = new SplitPane(previewPane, commandPalette);
        rightSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        rightSplit.setDividerPositions(0.55);
        rightSplit.getStyleClass().add("right-split");
        VBox.setVgrow(rightSplit, Priority.ALWAYS);
        rightPanel.getChildren().add(rightSplit);

        // Split pane
        splitPane = new SplitPane(leftPanel, graphPane);
        splitPane.setDividerPositions(0.25);
        SplitPane.setResizableWithParent(leftPanel, false);
        splitPane.getStyleClass().add("main-split");
        view.setCenter(splitPane);

        // Status bar
        HBox statusBar = new HBox(12);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(4, 12, 4, 12));
        statusLabel = new Label("\u041e\u0442\u043a\u0440\u043e\u0439\u0442\u0435 \u043f\u0440\u043e\u0435\u043a\u0442: \u0424\u0430\u0439\u043b \u2192 \u041e\u0442\u043a\u0440\u044b\u0442\u044c \u043f\u0440\u043e\u0435\u043a\u0442");
        statusLabel.getStyleClass().add("status-label");
        statusBar.getChildren().add(statusLabel);
        view.setBottom(statusBar);

        // Welcome content
        showWelcome();

        // Wire graph selection back to tree
        graphPane.setOnSceneSelected(sceneId -> {
            if (sceneTree != null) {
                selectSceneInTree(sceneId);
            }
        });

        // Wire graph double-click to open editor
        graphPane.setOnSceneDoubleClicked(this::openSceneEditor);
    }

    private MenuBar buildMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.getStyleClass().add("editor-menu-bar");

        // File menu
        Menu fileMenu = new Menu("\u0424\u0430\u0439\u043b");
        MenuItem newProjectItem = new MenuItem("\u041d\u043e\u0432\u044b\u0439 \u043f\u0440\u043e\u0435\u043a\u0442...");
        newProjectItem.setOnAction(e -> newProject());
        MenuItem openItem = new MenuItem("\u041e\u0442\u043a\u0440\u044b\u0442\u044c \u043f\u0440\u043e\u0435\u043a\u0442...");
        openItem.setOnAction(e -> openProject());
        Menu recentMenu = new Menu("\u041d\u0435\u0434\u0430\u0432\u043d\u0438\u0435 \u043f\u0440\u043e\u0435\u043a\u0442\u044b");
        recentMenu.setOnShowing(e -> rebuildRecentMenu(recentMenu));
        rebuildRecentMenu(recentMenu);

        MenuItem saveItem = new MenuItem("\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c (Ctrl+S)");
        saveItem.setOnAction(e -> save());

        // F5: Экспорт/Импорт
        MenuItem exportItem = new MenuItem("\u042d\u043a\u0441\u043f\u043e\u0440\u0442 \u0441\u0446\u0435\u043d...");
        exportItem.setOnAction(e -> exportScenes());
        MenuItem importItem = new MenuItem("\u0418\u043c\u043f\u043e\u0440\u0442 \u0441\u0446\u0435\u043d...");
        importItem.setOnAction(e -> importScenes());

        MenuItem exitItem = new MenuItem("\u0412\u044b\u0445\u043e\u0434");
        exitItem.setOnAction(e -> requestClose());
        fileMenu.getItems().addAll(newProjectItem, openItem, recentMenu, new SeparatorMenuItem(),
                saveItem, new SeparatorMenuItem(),
                exportItem, importItem, new SeparatorMenuItem(),
                exitItem);

        // Edit menu
        Menu editMenu = new Menu("\u041f\u0440\u0430\u0432\u043a\u0430");
        MenuItem undoItem = new MenuItem("\u041e\u0442\u043c\u0435\u043d\u0438\u0442\u044c (Ctrl+Z)");
        undoItem.setOnAction(e -> undo());
        MenuItem redoItem = new MenuItem("\u041f\u043e\u0432\u0442\u043e\u0440\u0438\u0442\u044c (Ctrl+Shift+Z)");
        redoItem.setOnAction(e -> redo());
        MenuItem copyFrameItem = new MenuItem("\u041a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u043a\u0430\u0434\u0440 (Ctrl+C)");
        copyFrameItem.setOnAction(e -> copyFrame());
        MenuItem pasteFrameItem = new MenuItem("\u0412\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u043a\u0430\u0434\u0440 (Ctrl+V)");
        pasteFrameItem.setOnAction(e -> pasteFrame());
        MenuItem searchItem = new MenuItem("\u041f\u043e\u0438\u0441\u043a \u0438 \u0437\u0430\u043c\u0435\u043d\u0430... (Ctrl+F)");
        searchItem.setOnAction(e -> openSearch());

        // F10: Шаблоны кадров
        Menu templatesMenu = new Menu("\u0428\u0430\u0431\u043b\u043e\u043d\u044b \u043a\u0430\u0434\u0440\u043e\u0432");
        MenuItem saveTemplateItem = new MenuItem("\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u043a\u0430\u043a \u0448\u0430\u0431\u043b\u043e\u043d...");
        saveTemplateItem.setOnAction(e -> saveFrameTemplate());
        templatesMenu.getItems().add(saveTemplateItem);
        templatesMenu.getItems().add(new SeparatorMenuItem());
        templatesMenu.setOnShowing(e -> rebuildTemplatesMenu(templatesMenu));

        // F11: Переместить кадр в другую сцену
        MenuItem moveFrameItem = new MenuItem("\u041f\u0435\u0440\u0435\u043c\u0435\u0441\u0442\u0438\u0442\u044c \u043a\u0430\u0434\u0440 \u0432 \u0441\u0446\u0435\u043d\u0443...");
        moveFrameItem.setOnAction(e -> moveFrameToScene());

        editMenu.getItems().addAll(undoItem, redoItem, new SeparatorMenuItem(),
                copyFrameItem, pasteFrameItem, new SeparatorMenuItem(),
                searchItem, new SeparatorMenuItem(),
                templatesMenu, moveFrameItem);

        // Project menu
        Menu projectMenu = new Menu("\u041f\u0440\u043e\u0435\u043a\u0442");
        MenuItem newSceneItem = new MenuItem("\u041d\u043e\u0432\u0430\u044f \u0441\u0446\u0435\u043d\u0430");
        newSceneItem.setOnAction(e -> newScene());
        MenuItem charsItem = new MenuItem("\u041f\u0435\u0440\u0441\u043e\u043d\u0430\u0436\u0438...");
        charsItem.setOnAction(e -> openCharacterEditor());
        MenuItem dupSceneItem = new MenuItem("\u0414\u0443\u0431\u043b\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u0441\u0446\u0435\u043d\u0443");
        dupSceneItem.setOnAction(e -> duplicateScene());

        // F4: Удалить сцену
        MenuItem deleteSceneItem = new MenuItem("\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u0441\u0446\u0435\u043d\u0443...");
        deleteSceneItem.setOnAction(e -> deleteScene());

        // F8: Переименовать ID сцены
        MenuItem renameIdItem = new MenuItem("\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c ID \u0441\u0446\u0435\u043d\u044b...");
        renameIdItem.setOnAction(e -> renameSceneId());

        MenuItem chaptersItem = new MenuItem("\u0413\u043b\u0430\u0432\u044b...");
        chaptersItem.setOnAction(e -> editChapters());
        MenuItem importBgItem = new MenuItem("\u0418\u043c\u043f\u043e\u0440\u0442 \u0444\u043e\u043d\u043e\u0432...");
        importBgItem.setOnAction(e -> importBackgrounds());
        MenuItem importSoundItem = new MenuItem("\u0418\u043c\u043f\u043e\u0440\u0442 \u0437\u0432\u0443\u043a\u043e\u0432...");
        importSoundItem.setOnAction(e -> importSounds());
        MenuItem manageBgItem = new MenuItem("\u0423\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0444\u043e\u043d\u0430\u043c\u0438...");
        manageBgItem.setOnAction(e -> manageResources(ResourceManagerDialog.Kind.BACKGROUNDS));
        MenuItem manageSoundItem = new MenuItem("\u0423\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0438\u0435 \u0437\u0432\u0443\u043a\u0430\u043c\u0438...");
        manageSoundItem.setOnAction(e -> manageResources(ResourceManagerDialog.Kind.SOUNDS));
        MenuItem validateItem = new MenuItem("\u0412\u0430\u043b\u0438\u0434\u0430\u0446\u0438\u044f... (Ctrl+Shift+V)");
        validateItem.setOnAction(e -> validateProject());
        projectMenu.getItems().addAll(newSceneItem, dupSceneItem, deleteSceneItem, renameIdItem,
                new SeparatorMenuItem(),
                charsItem, chaptersItem, new SeparatorMenuItem(),
                importBgItem, importSoundItem, manageBgItem, manageSoundItem,
                new SeparatorMenuItem(), validateItem);

        // View menu
        Menu viewMenu = new Menu("\u0412\u0438\u0434");
        MenuItem zoomIn = new MenuItem("\u041f\u0440\u0438\u0431\u043b\u0438\u0437\u0438\u0442\u044c");
        zoomIn.setOnAction(e -> graphPane.zoom(1.2));
        MenuItem zoomOut = new MenuItem("\u041e\u0442\u0434\u0430\u043b\u0438\u0442\u044c");
        zoomOut.setOnAction(e -> graphPane.zoom(0.8));
        MenuItem graphViewItem = new MenuItem("\u0413\u0440\u0430\u0444 \u0441\u0446\u0435\u043d (Esc)");
        graphViewItem.setOnAction(e -> closeEditor());

        // F7: Фильтр дерева
        MenuItem filterItem = new MenuItem("\u0424\u0438\u043b\u044c\u0442\u0440 \u0441\u0446\u0435\u043d");
        filterItem.setOnAction(e -> toggleTreeFilter());

        // F6: Статистика
        MenuItem statsItem = new MenuItem("\u0421\u0442\u0430\u0442\u0438\u0441\u0442\u0438\u043a\u0430 \u0442\u0435\u043a\u0441\u0442\u0430...");
        statsItem.setOnAction(e -> showStatistics());

        // F16: Навигация
        MenuItem navBackItem = new MenuItem("\u041d\u0430\u0437\u0430\u0434 (Alt+\u2190)");
        navBackItem.setOnAction(e -> navigateBack());
        MenuItem navForwardItem = new MenuItem("\u0412\u043f\u0435\u0440\u0451\u0434 (Alt+\u2192)");
        navForwardItem.setOnAction(e -> navigateForward());

        viewMenu.getItems().addAll(zoomIn, zoomOut, new SeparatorMenuItem(),
                graphViewItem, new SeparatorMenuItem(),
                filterItem, statsItem, new SeparatorMenuItem(),
                navBackItem, navForwardItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, projectMenu, viewMenu);
        return menuBar;
    }

    private void openProject() {
        if (dirtyTracker.isDirty() && !confirmDiscardChanges()) return;

        EditorProject project = ProjectLoader.openProject(stage);
        if (project == null) return;

        // Check for crash recovery
        if (editorCache.hasCrashRecovery(project.getProjectDir())) {
            String info = editorCache.getCacheInfo(project.getProjectDir());
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("\u0412\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u0435");
            alert.setHeaderText("\u041e\u0431\u043d\u0430\u0440\u0443\u0436\u0435\u043d\u0430 \u043d\u0435\u0437\u0430\u0432\u0435\u0440\u0448\u0451\u043d\u043d\u0430\u044f \u0441\u0435\u0441\u0441\u0438\u044f");
            alert.setContentText(info != null
                    ? "\u041a\u0435\u0448 \u043e\u0442: " + info + "\n\u0412\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c \u0438\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u044f?"
                    : "\u0412\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c \u0438\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u044f?");
            alert.initOwner(stage);
            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                EditorProject cached = editorCache.loadCache(project.getProjectDir());
                if (cached != null) {
                    project = cached;
                }
            } else {
                editorCache.clearCache(project.getProjectDir());
            }
        }

        loadProject(project);
    }

    private void loadProject(EditorProject project) {
        currentProject = project;
        dirtyTracker.markClean();
        undoManager.clear();
        navigationHistory.clear();

        // Remember in recent projects
        RecentProjects.addProject(project.getProjectDir());

        // Build tree
        sceneTree = SceneTreeBuilder.build(project);
        VBox.setVgrow(sceneTree, Priority.ALWAYS);

        leftPanel.getChildren().clear();
        Label treeHeader = new Label("\u0421\u0442\u0440\u0443\u043a\u0442\u0443\u0440\u0430: " + project.getProjectDir().getName());
        treeHeader.getStyleClass().add("panel-header");
        treeHeader.setMaxWidth(Double.MAX_VALUE);
        treeHeader.setPadding(new Insets(8, 12, 8, 12));

        // F7: показать фильтр
        treeFilterField.setVisible(true);
        treeFilterField.setManaged(true);
        treeFilterField.setText("");

        leftPanel.getChildren().addAll(treeHeader, treeFilterField, sceneTree);

        wireTreeEvents();

        // Switch to graph view
        closeEditor();

        // Replace welcome with graph
        if (!splitPane.getItems().contains(graphPane)) {
            if (splitPane.getItems().size() > 1) {
                splitPane.getItems().set(1, graphPane);
            }
            splitPane.setDividerPositions(0.25);
        }

        // Load graph
        graphPane.loadProject(project);

        // Setup preview & palette
        previewPane.setProject(project);
        commandPalette.setProject(project);
        projectResources = new ProjectResources(project.getProjectDir());
        commandPalette.setResources(projectResources);
        dialogEditor.setResources(projectResources);

        // Start autosave
        editorCache.startAutosave(project, dirtyTracker);

        // Update status
        updateStatusBar();
    }

    public void openSceneEditor(int sceneId) {
        openSceneEditor(sceneId, -1);
    }

    public void openSceneEditor(int sceneId, int frameIndex) {
        if (currentProject == null) return;

        EditorScene scene = null;
        for (EditorScene s : currentProject.getScenes()) {
            if (s.getId() == sceneId) {
                scene = s;
                break;
            }
        }
        if (scene == null) return;

        // F16: навигационная история
        navigationHistory.visit(sceneId);

        inEditorMode = true;
        undoManager.clear();
        recordUndoState();

        // Load scene into editor
        dialogEditor.loadScene(scene, currentProject);

        // F1: если указан конкретный кадр — перейти к нему
        if (frameIndex >= 0) {
            dialogEditor.selectFrame(frameIndex);
        }

        // Switch layout: tree | editor | right panel
        splitPane.getItems().clear();
        splitPane.getItems().addAll(leftPanel, dialogEditor, rightPanel);
        splitPane.setDividerPositions(0.20, 0.65);
        SplitPane.setResizableWithParent(leftPanel, false);
        SplitPane.setResizableWithParent(rightPanel, false);

        statusLabel.setText(String.format("\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435: \u0441\u0446\u0435\u043d\u0430 %d | %d \u043a\u0430\u0434\u0440\u043e\u0432 | Esc \u2014 \u0432\u0435\u0440\u043d\u0443\u0442\u044c\u0441\u044f \u043a \u0433\u0440\u0430\u0444\u0443",
                scene.getId(), scene.getFrameCount()));
    }

    public void closeEditor() {
        inEditorMode = false;

        // Switch layout back: tree | graph
        splitPane.getItems().clear();
        splitPane.getItems().addAll(leftPanel, graphPane);
        splitPane.setDividerPositions(0.25);
        SplitPane.setResizableWithParent(leftPanel, false);

        if (currentProject != null) {
            updateStatusBar();
        }
    }

    // --- Save / Undo / Redo ---

    public void save() {
        if (currentProject == null) return;
        try {
            File projectDir = currentProject.getProjectDir();

            // 1. Dialog_Structured.xml (сцены)
            File dialogXml = new File(projectDir, "lib/Scene/Dialog_Structured.xml");
            DialogXmlWriter.write(currentProject.getScenes(), dialogXml);

            // 2. Person.xml (персонажи)
            File personXml = new File(projectDir, "lib/Scene/Person.xml");
            PersonXmlWriter.write(currentProject.getCharacters(), currentProject.getVariables(), personXml);

            // 3. Chapters.xml (главы)
            File chaptersXml = new File(projectDir, "lib/config/Chapters.xml");
            if (!currentProject.getChapters().isEmpty()) {
                ChaptersXmlWriter.write(currentProject.getChapters(), chaptersXml);
            }

            dirtyTracker.markClean();
            editorCache.clearCache(projectDir);
            statusLabel.setText("\u0421\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u043e: " + projectDir.getAbsolutePath());
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("\u041e\u0448\u0438\u0431\u043a\u0430 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u044f");
            alert.setContentText(e.getMessage());
            alert.initOwner(stage);
            alert.showAndWait();
        }
    }

    public void undo() {
        if (!inEditorMode || dialogEditor.getCurrentScene() == null) return;
        EditorScene restored = undoManager.undo(dialogEditor.getCurrentScene());
        if (restored != null) {
            replaceSceneInProject(restored);
            dialogEditor.loadScene(restored, currentProject);
            dirtyTracker.markDirty();
        }
    }

    public void redo() {
        if (!inEditorMode || dialogEditor.getCurrentScene() == null) return;
        EditorScene restored = undoManager.redo(dialogEditor.getCurrentScene());
        if (restored != null) {
            replaceSceneInProject(restored);
            dialogEditor.loadScene(restored, currentProject);
            dirtyTracker.markDirty();
        }
    }

    private void recordUndoState() {
        if (dialogEditor.getCurrentScene() != null) {
            undoManager.recordState(dialogEditor.getCurrentScene());
        }
    }

    private void replaceSceneInProject(EditorScene newScene) {
        for (int i = 0; i < currentProject.getScenes().size(); i++) {
            if (currentProject.getScenes().get(i).getId() == newScene.getId()) {
                currentProject.getScenes().set(i, newScene);
                break;
            }
        }
    }

    public void newFrame() {
        if (inEditorMode && dialogEditor.getCurrentScene() != null) {
            recordUndoState();
            dialogEditor.addNewFrame();
            dirtyTracker.markDirty();
        }
    }

    public void deleteSelected() {
        if (inEditorMode && dialogEditor.getSelectedCard() != null) {
            recordUndoState();
            var frame = dialogEditor.getSelectedCard().getFrame();
            dialogEditor.getCurrentScene().getFrames().remove(frame);
            dialogEditor.loadScene(dialogEditor.getCurrentScene(), currentProject);
            dirtyTracker.markDirty();
        }
    }

    // --- Project / Scene / Character creation ---

    private void newProject() {
        if (dirtyTracker.isDirty() && !confirmDiscardChanges()) return;

        File dir = NewProjectWizard.show(stage);
        if (dir == null) return;

        EditorProject project = ProjectLoader.loadFromDirectory(dir, stage);
        if (project != null) loadProject(project);
    }

    public void newScene() {
        if (currentProject == null) return;
        int maxId = currentProject.getScenes().stream()
                .mapToInt(EditorScene::getId).max().orElse(0);
        EditorScene scene = new EditorScene();
        scene.setId(maxId + 1);
        EditorFrame frame = new EditorFrame();
        frame.setType(EditorFrame.FrameType.CHARACTER);
        frame.setSpeakerName("");
        frame.setSpeakerColor("");
        frame.setText("");
        scene.getFrames().add(frame);
        currentProject.getScenes().add(scene);
        dirtyTracker.markDirty();
        refreshProjectUI();
        statusLabel.setText("\u0421\u043e\u0437\u0434\u0430\u043d\u0430 \u0441\u0446\u0435\u043d\u0430 " + scene.getId());
    }

    private void openCharacterEditor() {
        if (currentProject == null) return;
        inEditorMode = false;
        characterEditor.loadProject(currentProject);
        splitPane.getItems().clear();
        splitPane.getItems().addAll(leftPanel, characterEditor);
        splitPane.setDividerPositions(0.25);
        SplitPane.setResizableWithParent(leftPanel, false);
        statusLabel.setText("\u0420\u0435\u0434\u0430\u043a\u0442\u043e\u0440 \u043f\u0435\u0440\u0441\u043e\u043d\u0430\u0436\u0435\u0439");
    }

    private void editChapters() {
        if (currentProject == null) return;
        boolean changed = ChapterEditorDialog.showAndEdit(
                currentProject.getChapters(), currentProject.getScenes(), stage);
        if (changed) {
            dirtyTracker.markDirty();
            refreshProjectUI();
        }
    }

    /**
     * Lets the user pick images from disk and copies them into the project's
     * backgrounds folder, then refreshes the resource cache so pickers see them.
     */
    private void importBackgrounds() {
        if (currentProject == null || projectResources == null) return;
        List<String> imported = ResourceImporter.importBackgrounds(stage, currentProject.getProjectDir());
        if (!imported.isEmpty()) {
            projectResources.refresh();
        }
    }

    private void importSounds() {
        if (currentProject == null || projectResources == null) return;
        List<String> imported = ResourceImporter.importSounds(stage, currentProject.getProjectDir());
        if (!imported.isEmpty()) {
            projectResources.refresh();
        }
    }

    /**
     * Opens the resource manager dialog for backgrounds or sounds,
     * allowing the user to import, rename and delete resources.
     *
     * @param kind which resource type to manage
     */
    private void manageResources(ResourceManagerDialog.Kind kind) {
        if (currentProject == null || projectResources == null) return;
        new ResourceManagerDialog(stage, projectResources, kind).showAndWait();
    }

    private void refreshProjectUI() {
        if (currentProject == null) return;
        currentProject.rebuildEdges(EdgeExtractor.extract(currentProject.getScenes()));

        // Rebuild tree (with current filter)
        String filter = treeFilterField.getText();
        sceneTree = SceneTreeBuilder.build(currentProject, (filter != null && !filter.isBlank()) ? filter : null);
        VBox.setVgrow(sceneTree, Priority.ALWAYS);
        leftPanel.getChildren().clear();
        Label treeHeader = new Label("\u0421\u0442\u0440\u0443\u043a\u0442\u0443\u0440\u0430: " + currentProject.getProjectDir().getName());
        treeHeader.getStyleClass().add("panel-header");
        treeHeader.setMaxWidth(Double.MAX_VALUE);
        treeHeader.setPadding(new Insets(8, 12, 8, 12));
        leftPanel.getChildren().addAll(treeHeader, treeFilterField, sceneTree);

        wireTreeEvents();

        // Reload graph
        graphPane.loadProject(currentProject);
        updateStatusBar();
    }

    // --- Copy / Paste / Duplicate / Search / Validate ---

    public void copyFrame() {
        if (inEditorMode && dialogEditor.getSelectedCard() != null) {
            clipboardFrame = DeepCopyUtil.copyFrame(dialogEditor.getSelectedCard().getFrame());
            statusLabel.setText("\u041a\u0430\u0434\u0440 \u0441\u043a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u043d");
        }
    }

    public void pasteFrame() {
        if (inEditorMode && clipboardFrame != null && dialogEditor.getCurrentScene() != null) {
            recordUndoState();
            EditorFrame copy = DeepCopyUtil.copyFrame(clipboardFrame);
            int insertIndex;
            if (dialogEditor.getSelectedCard() != null) {
                insertIndex = dialogEditor.getCurrentScene().getFrames().indexOf(
                        dialogEditor.getSelectedCard().getFrame()) + 1;
            } else {
                insertIndex = dialogEditor.getCurrentScene().getFrames().size();
            }
            dialogEditor.getCurrentScene().getFrames().add(insertIndex, copy);
            dialogEditor.rebuildFrameCards();
            dialogEditor.selectFrame(insertIndex);
            dirtyTracker.markDirty();
            statusLabel.setText("\u041a\u0430\u0434\u0440 \u0432\u0441\u0442\u0430\u0432\u043b\u0435\u043d");
        }
    }

    private void duplicateScene() {
        if (currentProject == null) return;
        EditorScene source = getCurrentSceneForAction();
        if (source == null) {
            statusLabel.setText("\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0441\u0446\u0435\u043d\u0443 \u0434\u043b\u044f \u0434\u0443\u0431\u043b\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u044f");
            return;
        }
        int maxId = currentProject.getScenes().stream()
                .mapToInt(EditorScene::getId).max().orElse(0);
        EditorScene copy = DeepCopyUtil.copyScene(source);
        copy.setId(maxId + 1);
        currentProject.getScenes().add(copy);
        dirtyTracker.markDirty();
        refreshProjectUI();
        statusLabel.setText("\u0421\u0446\u0435\u043d\u0430 " + source.getId() + " \u2192 \u0434\u0443\u0431\u043b\u0438\u043a\u0430\u0442 " + copy.getId());
    }

    private EditorScene getCurrentSceneForAction() {
        // If editing a scene, use that
        if (inEditorMode && dialogEditor.getCurrentScene() != null) {
            return dialogEditor.getCurrentScene();
        }
        // Otherwise, try tree selection
        if (sceneTree != null) {
            TreeItem<TreeItemData> sel = sceneTree.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getValue() != null && sel.getValue().getSceneId() > 0) {
                int sceneId = sel.getValue().getSceneId();
                for (EditorScene s : currentProject.getScenes()) {
                    if (s.getId() == sceneId) return s;
                }
            }
        }
        return null;
    }

    public void openSearch() {
        if (currentProject == null) return;
        SearchDialog.show(currentProject.getScenes(), stage, (sceneId, frameIndex) -> {
            openSceneEditor(sceneId, frameIndex);
        }, () -> {
            // F3: callback после замены — обновить UI
            dirtyTracker.markDirty();
            if (inEditorMode && dialogEditor.getCurrentScene() != null) {
                dialogEditor.loadScene(dialogEditor.getCurrentScene(), currentProject);
            }
        });
    }

    public void validateProject() {
        if (currentProject == null) return;
        var issues = ProjectValidator.validate(currentProject);
        ValidationDialog.show(issues, stage, (sceneId, frameIndex) -> {
            openSceneEditor(sceneId);
            if (frameIndex >= 0) {
                dialogEditor.selectFrame(frameIndex);
            }
        });
    }

    // --- F1: Tree ↔ Editor sync ---

    private void wireTreeEvents() {
        sceneTree.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null && sel.getValue() != null) {
                int sceneId = sel.getValue().getSceneId();
                if (sceneId > 0) {
                    int frameIdx = sel.getValue().isFrame() ? sel.getValue().getFrameIndex() : -1;
                    if (inEditorMode && dialogEditor.getCurrentScene() != null
                            && dialogEditor.getCurrentScene().getId() == sceneId) {
                        // Та же сцена — скролл к кадру
                        if (sel.getValue().isFrame()) {
                            suppressTreeSync = true;
                            dialogEditor.selectFrame(frameIdx);
                            suppressTreeSync = false;
                        }
                    } else {
                        // Другая сцена или не в редакторе — открыть сцену
                        openSceneEditor(sceneId, frameIdx);
                    }
                }
            }
        });

        // F15: контекстное меню для тегов
        ContextMenu treeContextMenu = new ContextMenu();
        Menu tagMenu = new Menu("\u041c\u0435\u0442\u043a\u0430");
        for (String color : new String[]{"red", "green", "blue", "yellow", "purple"}) {
            MenuItem tagItem = new MenuItem(color);
            tagItem.setOnAction(e -> setTagOnSelected(color));
            tagMenu.getItems().add(tagItem);
        }
        MenuItem clearTagItem = new MenuItem("\u0423\u0431\u0440\u0430\u0442\u044c \u043c\u0435\u0442\u043a\u0443");
        clearTagItem.setOnAction(e -> setTagOnSelected(""));
        tagMenu.getItems().addAll(new SeparatorMenuItem(), clearTagItem);
        treeContextMenu.getItems().add(tagMenu);
        sceneTree.setContextMenu(treeContextMenu);
    }

    private void selectFrameInTree(int sceneId, int frameIndex) {
        if (sceneTree == null) return;
        TreeItem<TreeItemData> root = sceneTree.getRoot();
        if (root == null) return;
        TreeItem<TreeItemData> found = findFrameItem(root, sceneId, frameIndex);
        if (found != null) {
            sceneTree.getSelectionModel().select(found);
            sceneTree.scrollTo(sceneTree.getRow(found));
        }
    }

    private TreeItem<TreeItemData> findFrameItem(TreeItem<TreeItemData> parent, int sceneId, int frameIndex) {
        for (TreeItem<TreeItemData> child : parent.getChildren()) {
            TreeItemData data = child.getValue();
            if (data.isFrame() && data.getSceneId() == sceneId && data.getFrameIndex() == frameIndex) {
                // Expand parent scene node
                if (child.getParent() != null) child.getParent().setExpanded(true);
                return child;
            }
            TreeItem<TreeItemData> found = findFrameItem(child, sceneId, frameIndex);
            if (found != null) return found;
        }
        return null;
    }

    // --- F15: Tags/bookmarks ---

    private void setTagOnSelected(String tag) {
        if (sceneTree == null) return;
        TreeItem<TreeItemData> sel = sceneTree.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getValue() == null) return;
        TreeItemData data = sel.getValue();
        if (data.getScene() != null) {
            data.getScene().setTag(tag);
            dirtyTracker.markDirty();
            refreshProjectUI();
        } else if (data.getFrame() != null) {
            data.getFrame().setTag(tag);
            dirtyTracker.markDirty();
            refreshProjectUI();
        }
    }

    // --- F4: Delete scene ---

    private void deleteScene() {
        if (currentProject == null) return;
        EditorScene scene = getCurrentSceneForAction();
        if (scene == null) {
            statusLabel.setText("\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0441\u0446\u0435\u043d\u0443 \u0434\u043b\u044f \u0443\u0434\u0430\u043b\u0435\u043d\u0438\u044f");
            return;
        }

        // Проверяем ссылки на эту сцену
        List<String> refs = findReferencesToScene(scene.getId());
        String warning = refs.isEmpty()
                ? "\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u0441\u0446\u0435\u043d\u0443 " + scene.getId() + "?"
                : "\u0421\u0446\u0435\u043d\u0430 " + scene.getId() + " \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0435\u0442\u0441\u044f:\n" + String.join("\n", refs) + "\n\n\u0412\u0441\u0451 \u0440\u0430\u0432\u043d\u043e \u0443\u0434\u0430\u043b\u0438\u0442\u044c?";

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("\u0423\u0434\u0430\u043b\u0435\u043d\u0438\u0435 \u0441\u0446\u0435\u043d\u044b");
        alert.setHeaderText(null);
        alert.setContentText(warning);
        alert.initOwner(stage);
        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        currentProject.getScenes().remove(scene);
        if (inEditorMode && dialogEditor.getCurrentScene() == scene) {
            closeEditor();
        }
        dirtyTracker.markDirty();
        refreshProjectUI();
        statusLabel.setText("\u0421\u0446\u0435\u043d\u0430 " + scene.getId() + " \u0443\u0434\u0430\u043b\u0435\u043d\u0430");
    }

    private List<String> findReferencesToScene(int sceneId) {
        List<String> refs = new ArrayList<>();
        for (EditorScene s : currentProject.getScenes()) {
            if (s.getId() == sceneId) continue;
            if (s.getNextSceneId() == sceneId) {
                refs.add("\u0421\u0446\u0435\u043d\u0430 " + s.getId() + " \u2192 nextScene");
            }
            for (EditorFrame f : s.getFrames()) {
                if (f.getChoices() != null) {
                    for (EditorChoice ch : f.getChoices()) {
                        if (ch.getRequestsId() == sceneId) {
                            refs.add("\u0421\u0446\u0435\u043d\u0430 " + s.getId() + " \u2192 \u0432\u044b\u0431\u043e\u0440 \"" + ch.getText() + "\"");
                        }
                    }
                }
                for (EditorCommand cmd : f.getCommands()) {
                    if (cmd.getOnSuccess() == sceneId || cmd.getOnFailure() == sceneId) {
                        refs.add("\u0421\u0446\u0435\u043d\u0430 " + s.getId() + " \u2192 \u043a\u043e\u043c\u0430\u043d\u0434\u0430 " + cmd.getAction());
                    }
                }
            }
        }
        return refs;
    }

    // --- F8: Rename scene ID ---

    private void renameSceneId() {
        if (currentProject == null) return;
        EditorScene scene = getCurrentSceneForAction();
        if (scene == null) {
            statusLabel.setText("\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0441\u0446\u0435\u043d\u0443");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(String.valueOf(scene.getId()));
        dialog.setTitle("\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c ID \u0441\u0446\u0435\u043d\u044b");
        dialog.setHeaderText("\u0421\u0446\u0435\u043d\u0430 " + scene.getId());
        dialog.setContentText("\u041d\u043e\u0432\u044b\u0439 ID:");
        dialog.initOwner(stage);

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        int newId;
        try {
            newId = Integer.parseInt(result.get().trim());
        } catch (NumberFormatException e) {
            statusLabel.setText("\u041d\u0435\u043a\u043e\u0440\u0440\u0435\u043a\u0442\u043d\u044b\u0439 ID");
            return;
        }

        if (newId == scene.getId()) return;

        // Проверка уникальности
        for (EditorScene s : currentProject.getScenes()) {
            if (s.getId() == newId) {
                statusLabel.setText("ID " + newId + " \u0443\u0436\u0435 \u0437\u0430\u043d\u044f\u0442");
                return;
            }
        }

        int oldId = scene.getId();

        // Обновить все ссылки
        for (EditorScene s : currentProject.getScenes()) {
            if (s.getNextSceneId() == oldId) s.setNextSceneId(newId);
            for (EditorFrame f : s.getFrames()) {
                if (f.getChoices() != null) {
                    for (EditorChoice ch : f.getChoices()) {
                        if (ch.getRequestsId() == oldId) ch.setRequestsId(newId);
                    }
                }
                for (EditorCommand cmd : f.getCommands()) {
                    if (cmd.getOnSuccess() == oldId) cmd.setOnSuccess(newId);
                    if (cmd.getOnFailure() == oldId) cmd.setOnFailure(newId);
                }
            }
        }
        // Обновить главы
        currentProject.getChapters().forEach(ch -> {
            if (ch.getSceneId() == oldId) ch.setSceneId(newId);
        });

        scene.setId(newId);
        dirtyTracker.markDirty();
        refreshProjectUI();
        if (inEditorMode) {
            dialogEditor.loadScene(scene, currentProject);
        }
        statusLabel.setText("ID \u0441\u0446\u0435\u043d\u044b: " + oldId + " \u2192 " + newId);
    }

    // --- F7: Tree filter ---

    private void toggleTreeFilter() {
        boolean visible = !treeFilterField.isVisible();
        treeFilterField.setVisible(visible);
        treeFilterField.setManaged(visible);
        if (visible) {
            treeFilterField.requestFocus();
        } else {
            treeFilterField.setText("");
        }
    }

    private void applyTreeFilter(String filter) {
        if (currentProject == null) return;
        String f = (filter != null && !filter.isBlank()) ? filter : null;
        sceneTree = SceneTreeBuilder.build(currentProject, f);
        VBox.setVgrow(sceneTree, Priority.ALWAYS);

        // Сохраняем header и filterField, заменяем только дерево
        if (leftPanel.getChildren().size() > 2) {
            leftPanel.getChildren().remove(2, leftPanel.getChildren().size());
        }
        leftPanel.getChildren().add(sceneTree);
        wireTreeEvents();
    }

    // --- F6: Statistics ---

    private void showStatistics() {
        if (currentProject == null) return;
        TextStatistics.show(currentProject, stage);
    }

    // --- F16: Navigation history ---

    public void navigateBack() {
        int sceneId = navigationHistory.goBack();
        if (sceneId > 0) openSceneEditor(sceneId);
    }

    public void navigateForward() {
        int sceneId = navigationHistory.goForward();
        if (sceneId > 0) openSceneEditor(sceneId);
    }

    // --- F14: Toggle frame type ---

    public void toggleFrameType() {
        if (!inEditorMode || dialogEditor.getSelectedCard() == null) return;
        recordUndoState();
        EditorFrame frame = dialogEditor.getSelectedCard().getFrame();
        if (frame.getType() == EditorFrame.FrameType.CHARACTER) {
            frame.setType(EditorFrame.FrameType.OVERLAY);
        } else {
            frame.setType(EditorFrame.FrameType.CHARACTER);
        }
        dialogEditor.loadScene(dialogEditor.getCurrentScene(), currentProject);
        dirtyTracker.markDirty();
        statusLabel.setText("\u0422\u0438\u043f \u043a\u0430\u0434\u0440\u0430: " + frame.getType().name());
    }

    // --- F10: Frame templates ---

    private void saveFrameTemplate() {
        if (!inEditorMode || dialogEditor.getSelectedCard() == null) {
            statusLabel.setText("\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u043a\u0430\u0434\u0440 \u0434\u043b\u044f \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u044f \u043a\u0430\u043a \u0448\u0430\u0431\u043b\u043e\u043d");
            return;
        }
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0448\u0430\u0431\u043b\u043e\u043d");
        dlg.setHeaderText(null);
        dlg.setContentText("\u0418\u043c\u044f \u0448\u0430\u0431\u043b\u043e\u043d\u0430:");
        dlg.initOwner(stage);
        Optional<String> result = dlg.showAndWait();
        if (result.isEmpty() || result.get().isBlank()) return;

        FrameTemplateManager.saveTemplate(result.get().trim(), dialogEditor.getSelectedCard().getFrame());
        statusLabel.setText("\u0428\u0430\u0431\u043b\u043e\u043d \u0441\u043e\u0445\u0440\u0430\u043d\u0451\u043d: " + result.get().trim());
    }

    private void rebuildTemplatesMenu(Menu menu) {
        // Сохраняем первые 2 элемента ("Сохранить как шаблон..." + сепаратор)
        while (menu.getItems().size() > 2) {
            menu.getItems().remove(menu.getItems().size() - 1);
        }
        List<String> templates = FrameTemplateManager.listTemplates();
        if (templates.isEmpty()) {
            MenuItem empty = new MenuItem("(\u043d\u0435\u0442 \u0448\u0430\u0431\u043b\u043e\u043d\u043e\u0432)");
            empty.setDisable(true);
            menu.getItems().add(empty);
        } else {
            for (String name : templates) {
                MenuItem item = new MenuItem(name);
                item.setOnAction(e -> applyTemplate(name));
                menu.getItems().add(item);
            }
        }
    }

    private void applyTemplate(String name) {
        if (!inEditorMode || dialogEditor.getCurrentScene() == null) return;
        EditorFrame template = FrameTemplateManager.loadTemplate(name);
        if (template == null) {
            statusLabel.setText("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c \u0448\u0430\u0431\u043b\u043e\u043d");
            return;
        }
        recordUndoState();
        EditorFrame copy = DeepCopyUtil.copyFrame(template);
        int insertIndex;
        if (dialogEditor.getSelectedCard() != null) {
            insertIndex = dialogEditor.getCurrentScene().getFrames().indexOf(
                    dialogEditor.getSelectedCard().getFrame()) + 1;
        } else {
            insertIndex = dialogEditor.getCurrentScene().getFrames().size();
        }
        dialogEditor.getCurrentScene().getFrames().add(insertIndex, copy);
        dialogEditor.rebuildFrameCards();
        dialogEditor.selectFrame(insertIndex);
        dirtyTracker.markDirty();
        statusLabel.setText("\u0428\u0430\u0431\u043b\u043e\u043d \u043f\u0440\u0438\u043c\u0435\u043d\u0451\u043d: " + name);
    }

    // --- F11: Move frame to another scene ---

    private void moveFrameToScene() {
        if (!inEditorMode || dialogEditor.getSelectedCard() == null || currentProject == null) {
            statusLabel.setText("\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u043a\u0430\u0434\u0440 \u0434\u043b\u044f \u043f\u0435\u0440\u0435\u043c\u0435\u0449\u0435\u043d\u0438\u044f");
            return;
        }

        EditorScene sourceScene = dialogEditor.getCurrentScene();
        List<Integer> sceneIds = new ArrayList<>();
        for (EditorScene s : currentProject.getScenes()) {
            if (s.getId() != sourceScene.getId()) sceneIds.add(s.getId());
        }
        if (sceneIds.isEmpty()) {
            statusLabel.setText("\u041d\u0435\u0442 \u0434\u0440\u0443\u0433\u0438\u0445 \u0441\u0446\u0435\u043d");
            return;
        }

        ChoiceDialog<Integer> dlg = new ChoiceDialog<>(sceneIds.getFirst(), sceneIds);
        dlg.setTitle("\u041f\u0435\u0440\u0435\u043c\u0435\u0441\u0442\u0438\u0442\u044c \u043a\u0430\u0434\u0440");
        dlg.setHeaderText(null);
        dlg.setContentText("\u0412 \u0441\u0446\u0435\u043d\u0443:");
        dlg.initOwner(stage);

        Optional<Integer> result = dlg.showAndWait();
        if (result.isEmpty()) return;

        int targetId = result.get();
        EditorScene targetScene = null;
        for (EditorScene s : currentProject.getScenes()) {
            if (s.getId() == targetId) { targetScene = s; break; }
        }
        if (targetScene == null) return;

        recordUndoState();
        EditorFrame frame = dialogEditor.getSelectedCard().getFrame();
        sourceScene.getFrames().remove(frame);
        targetScene.getFrames().add(frame);

        dialogEditor.loadScene(sourceScene, currentProject);
        dirtyTracker.markDirty();
        statusLabel.setText("\u041a\u0430\u0434\u0440 \u043f\u0435\u0440\u0435\u043c\u0435\u0449\u0451\u043d \u0432 \u0441\u0446\u0435\u043d\u0443 " + targetId);
    }

    // --- F5: Export / Import scenes ---

    private void exportScenes() {
        if (currentProject == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("\u042d\u043a\u0441\u043f\u043e\u0440\u0442 \u0441\u0446\u0435\u043d");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML", "*.xml"));
        fc.setInitialFileName("scenes_export.xml");
        File file = fc.showSaveDialog(stage);
        if (file == null) return;

        try {
            SceneExporter.exportScenes(currentProject.getScenes(), file);
            statusLabel.setText("\u042d\u043a\u0441\u043f\u043e\u0440\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u043e: " + file.getName());
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "\u041e\u0448\u0438\u0431\u043a\u0430 \u044d\u043a\u0441\u043f\u043e\u0440\u0442\u0430: " + e.getMessage());
            alert.initOwner(stage);
            alert.showAndWait();
        }
    }

    private void importScenes() {
        if (currentProject == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("\u0418\u043c\u043f\u043e\u0440\u0442 \u0441\u0446\u0435\u043d");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML", "*.xml"));
        File file = fc.showOpenDialog(stage);
        if (file == null) return;

        try {
            List<EditorScene> imported = SceneExporter.importScenes(file);
            if (imported.isEmpty()) {
                statusLabel.setText("\u041d\u0435\u0442 \u0441\u0446\u0435\u043d \u0434\u043b\u044f \u0438\u043c\u043f\u043e\u0440\u0442\u0430");
                return;
            }

            // Автоназначение ID при коллизиях
            int maxId = currentProject.getScenes().stream()
                    .mapToInt(EditorScene::getId).max().orElse(0);
            int reassigned = 0;
            for (EditorScene s : imported) {
                boolean collision = currentProject.getScenes().stream()
                        .anyMatch(existing -> existing.getId() == s.getId());
                if (collision) {
                    maxId++;
                    s.setId(maxId);
                    reassigned++;
                }
            }

            currentProject.getScenes().addAll(imported);
            dirtyTracker.markDirty();
            refreshProjectUI();
            String msg = "\u0418\u043c\u043f\u043e\u0440\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u043e: " + imported.size() + " \u0441\u0446\u0435\u043d";
            if (reassigned > 0) msg += " (" + reassigned + " ID \u043f\u0435\u0440\u0435\u043d\u0430\u0437\u043d\u0430\u0447\u0435\u043d\u043e)";
            statusLabel.setText(msg);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "\u041e\u0448\u0438\u0431\u043a\u0430 \u0438\u043c\u043f\u043e\u0440\u0442\u0430: " + e.getMessage());
            alert.initOwner(stage);
            alert.showAndWait();
        }
    }

    // --- Close handling ---

    public void requestClose() {
        if (dirtyTracker.isDirty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("\u041d\u0435\u0441\u043e\u0445\u0440\u0430\u043d\u0451\u043d\u043d\u044b\u0435 \u0438\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u044f");
            alert.setHeaderText("\u0415\u0441\u0442\u044c \u043d\u0435\u0441\u043e\u0445\u0440\u0430\u043d\u0451\u043d\u043d\u044b\u0435 \u0438\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u044f.");
            alert.initOwner(stage);

            ButtonType saveBtn = new ButtonType("\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c");
            ButtonType discardBtn = new ButtonType("\u041d\u0435 \u0441\u043e\u0445\u0440\u0430\u043d\u044f\u0442\u044c");
            ButtonType cancelBtn = new ButtonType("\u041e\u0442\u043c\u0435\u043d\u0430", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(saveBtn, discardBtn, cancelBtn);
            var result = alert.showAndWait().orElse(cancelBtn);

            if (result == saveBtn) {
                save();
                shutdown();
                stage.close();
            } else if (result == discardBtn) {
                shutdown();
                stage.close();
            }
            // Cancel = do nothing
        } else {
            shutdown();
            stage.close();
        }
    }

    public boolean confirmDiscardChanges() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("\u041d\u0435\u0441\u043e\u0445\u0440\u0430\u043d\u0451\u043d\u043d\u044b\u0435 \u0438\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u044f");
        alert.setContentText("\u0415\u0441\u0442\u044c \u043d\u0435\u0441\u043e\u0445\u0440\u0430\u043d\u0451\u043d\u043d\u044b\u0435 \u0438\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u044f. \u041f\u0440\u043e\u0434\u043e\u043b\u0436\u0438\u0442\u044c?");
        alert.initOwner(stage);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void shutdown() {
        editorCache.stopAutosave();
    }

    // --- Helpers ---

    private void updateStatusBar() {
        if (currentProject != null) {
            statusLabel.setText(String.format(
                    "\u041f\u0440\u043e\u0435\u043a\u0442: %s | %d \u0441\u0446\u0435\u043d, %d \u043a\u0430\u0434\u0440\u043e\u0432 | %d \u043f\u0435\u0440\u0441\u043e\u043d\u0430\u0436\u0435\u0439 | %d \u0433\u043b\u0430\u0432 | %d \u0432\u0435\u0442\u0432\u043b\u0435\u043d\u0438\u0439 | %d \u043a\u043e\u043d\u0446\u043e\u0432\u043e\u043a",
                    currentProject.getProjectDir().getName(),
                    currentProject.getSceneCount(),
                    currentProject.getTotalFrameCount(),
                    currentProject.getCharacterCount(),
                    currentProject.getChapterCount(),
                    currentProject.getBranchCount(),
                    currentProject.getEndCount()));
        }
    }

    private void rebuildRecentMenu(Menu recentMenu) {
        recentMenu.getItems().clear();
        List<RecentProjects.RecentEntry> recents = RecentProjects.load();
        if (recents.isEmpty()) {
            MenuItem empty = new MenuItem("(пусто)");
            empty.setDisable(true);
            recentMenu.getItems().add(empty);
        } else {
            for (RecentProjects.RecentEntry entry : recents) {
                MenuItem item = new MenuItem(entry.name() + "  —  " + entry.path());
                item.setOnAction(e -> openRecentProject(entry.path()));
                recentMenu.getItems().add(item);
            }
            recentMenu.getItems().add(new SeparatorMenuItem());
            MenuItem clearItem = new MenuItem("Очистить список");
            clearItem.setOnAction(e -> {
                RecentProjects.clearAll();
                rebuildRecentMenu(recentMenu);
            });
            recentMenu.getItems().add(clearItem);
        }
    }

    private void openRecentProject(String path) {
        if (dirtyTracker.isDirty() && !confirmDiscardChanges()) return;

        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            RecentProjects.removeProject(path);
            statusLabel.setText("Проект не найден: " + path);
            return;
        }

        EditorProject project = ProjectLoader.loadFromDirectory(dir, stage);
        if (project == null) return;

        // Check for crash recovery
        if (editorCache.hasCrashRecovery(project.getProjectDir())) {
            String info = editorCache.getCacheInfo(project.getProjectDir());
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Восстановление");
            alert.setHeaderText("Обнаружена незавершённая сессия");
            alert.setContentText(info != null
                    ? "Кеш от: " + info + "\nВосстановить изменения?"
                    : "Восстановить изменения?");
            alert.initOwner(stage);
            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                EditorProject cached = editorCache.loadCache(project.getProjectDir());
                if (cached != null) {
                    project = cached;
                }
            } else {
                editorCache.clearCache(project.getProjectDir());
            }
        }

        loadProject(project);
    }

    private void showWelcome() {
        VBox welcome = new VBox(16);
        welcome.setAlignment(Pos.CENTER);
        welcome.getStyleClass().add("welcome-pane");

        Label title = new Label("Liza Text Editor");
        title.getStyleClass().add("welcome-title");

        Label subtitle = new Label("\u0420\u0435\u0434\u0430\u043a\u0442\u043e\u0440 \u0441\u0446\u0435\u043d\u0430\u0440\u0438\u0435\u0432 \u0434\u043b\u044f \u0434\u0432\u0438\u0436\u043a\u0430 \u0432\u0438\u0437\u0443\u0430\u043b\u044c\u043d\u043e\u0439 \u043d\u043e\u0432\u0435\u043b\u043b\u044b");
        subtitle.getStyleClass().add("welcome-subtitle");

        Button openBtn = new Button("\u041e\u0442\u043a\u0440\u044b\u0442\u044c \u043f\u0440\u043e\u0435\u043a\u0442...");
        openBtn.getStyleClass().add("welcome-button");
        openBtn.setOnAction(e -> openProject());

        welcome.getChildren().addAll(title, subtitle, openBtn);

        // Recent projects list
        List<RecentProjects.RecentEntry> recents = RecentProjects.load();
        if (!recents.isEmpty()) {
            Label recentHeader = new Label("\u041d\u0435\u0434\u0430\u0432\u043d\u0438\u0435 \u043f\u0440\u043e\u0435\u043a\u0442\u044b");
            recentHeader.getStyleClass().add("recent-header");

            VBox recentList = new VBox(4);
            recentList.setAlignment(Pos.CENTER);
            recentList.setMaxWidth(500);

            for (RecentProjects.RecentEntry entry : recents) {
                File dir = new File(entry.path());
                if (!dir.exists()) continue;

                VBox item = new VBox(1);
                item.getStyleClass().add("recent-item");
                item.setPadding(new Insets(6, 14, 6, 14));

                Label nameLabel = new Label(entry.name());
                nameLabel.getStyleClass().add("recent-item-name");

                Label pathLabel = new Label(entry.path());
                pathLabel.getStyleClass().add("recent-item-path");

                item.getChildren().addAll(nameLabel, pathLabel);
                item.setOnMouseClicked(e -> openRecentProject(entry.path()));

                recentList.getChildren().add(item);
            }

            // Remove entries for deleted dirs from storage
            List<String> validPaths = recentList.getChildren().stream()
                    .map(n -> ((Label) ((VBox) n).getChildren().get(1)).getText())
                    .toList();
            for (RecentProjects.RecentEntry entry : recents) {
                if (!validPaths.contains(entry.path())) {
                    RecentProjects.removeProject(entry.path());
                }
            }

            if (!recentList.getChildren().isEmpty()) {
                welcome.getChildren().addAll(recentHeader, recentList);
            }
        }

        if (splitPane.getItems().size() > 1) {
            splitPane.getItems().set(1, welcome);
        }
    }

    private void selectSceneInTree(int sceneId) {
        if (sceneTree == null) return;
        TreeItem<TreeItemData> root = sceneTree.getRoot();
        if (root == null) return;
        TreeItem<TreeItemData> found = findSceneItem(root, sceneId);
        if (found != null) {
            sceneTree.getSelectionModel().select(found);
            sceneTree.scrollTo(sceneTree.getRow(found));
        }
    }

    private TreeItem<TreeItemData> findSceneItem(TreeItem<TreeItemData> parent, int sceneId) {
        for (TreeItem<TreeItemData> child : parent.getChildren()) {
            if (child.getValue().isScene() && child.getValue().getSceneId() == sceneId) {
                return child;
            }
            TreeItem<TreeItemData> found = findSceneItem(child, sceneId);
            if (found != null) return found;
        }
        return null;
    }

    public boolean isInEditorMode() { return inEditorMode; }
    public DirtyTracker getDirtyTracker() { return dirtyTracker; }
    public Node getView() { return view; }
}
