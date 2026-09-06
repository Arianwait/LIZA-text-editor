package kz.arianwait.gametexteditor.preview;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import kz.arianwait.gametexteditor.model.EditorCharacter;
import kz.arianwait.gametexteditor.model.EditorChoice;
import kz.arianwait.gametexteditor.model.EditorCommand;
import kz.arianwait.gametexteditor.model.EditorFrame;
import kz.arianwait.gametexteditor.model.EditorPose;
import kz.arianwait.gametexteditor.model.EditorProject;
import kz.arianwait.gametexteditor.model.EditorScene;

public class MiniPreviewPane extends StackPane {

    private EditorProject project;
    private final Map<String, Image> imageCache = new HashMap<>();

    private final ImageView backgroundView;
    private final StackPane characterLayer;
    private final VBox dialogPanel;
    private final Label speakerLabel;
    private final Label textLabel;
    private final VBox choiceContainer;
    private final StackPane overlayPane;
    private final Label overlayText;

    /** Состояние персонажей на сцене: имя → {pose, position} */
    private static class CharacterState {
        String pose;
        String position; // LEFT, CENTER, RIGHT
        CharacterState(String pose, String position) {
            this.pose = pose;
            this.position = position;
        }
    }

    public MiniPreviewPane() {
        getStyleClass().add("preview-pane");
        setMinSize(200, 120);

        // Clip to maintain aspect ratio
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        clip.setArcWidth(8);
        clip.setArcHeight(8);
        setClip(clip);

        // Background
        backgroundView = new ImageView();
        backgroundView.setPreserveRatio(false);
        backgroundView.fitWidthProperty().bind(widthProperty());
        backgroundView.fitHeightProperty().bind(heightProperty());

        // Layer for multiple character sprites
        characterLayer = new StackPane();
        characterLayer.setPickOnBounds(false);

        // Dialog gradient panel
        dialogPanel = new VBox(2);
        dialogPanel.setAlignment(Pos.TOP_LEFT);
        dialogPanel.setPadding(new Insets(12, 16, 8, 16));
        dialogPanel.setMaxHeight(USE_PREF_SIZE);
        dialogPanel.prefHeightProperty().bind(heightProperty().multiply(0.30));
        dialogPanel.getStyleClass().add("preview-dialog-panel");
        StackPane.setAlignment(dialogPanel, Pos.BOTTOM_CENTER);

        speakerLabel = new Label();
        speakerLabel.getStyleClass().add("preview-speaker");

        textLabel = new Label();
        textLabel.getStyleClass().add("preview-text");
        textLabel.setWrapText(true);

        choiceContainer = new VBox(3);
        choiceContainer.setAlignment(Pos.CENTER);
        choiceContainer.getStyleClass().add("preview-choices");

        dialogPanel.getChildren().addAll(speakerLabel, textLabel, choiceContainer);

        // Full-screen overlay
        overlayPane = new StackPane();
        overlayPane.getStyleClass().add("preview-overlay");
        overlayPane.setVisible(false);

        overlayText = new Label();
        overlayText.getStyleClass().add("preview-overlay-text");
        overlayText.setWrapText(true);
        overlayPane.getChildren().add(overlayText);

        // Dark placeholder background
        setBackground(new Background(new BackgroundFill(
                Color.web("#0a1219"), CornerRadii.EMPTY, Insets.EMPTY)));

        getChildren().addAll(backgroundView, characterLayer, dialogPanel, overlayPane);
    }

    public void setProject(EditorProject project) {
        this.project = project;
        imageCache.clear();
    }

    /**
     * Legacy method (without frame index).
     */
    public void updatePreview(EditorFrame frame, EditorScene scene) {
        updatePreview(frame, scene, -1);
    }

    /**
     * Update preview with background/character inheritance up to frameIndex.
     * Characters persist across frames until removeFromScene.
     */
    public void updatePreview(EditorFrame frame, EditorScene scene, int frameIndex) {
        if (frame == null || scene == null) {
            clearPreview();
            return;
        }

        if (frame.getType() == EditorFrame.FrameType.OVERLAY) {
            showOverlay(frame, scene, frameIndex);
        } else {
            showCharacterFrame(frame, scene, frameIndex);
        }
    }

    private void showOverlay(EditorFrame frame, EditorScene scene, int frameIndex) {
        String bgPath = resolveCurrentBackground(scene, frameIndex);
        if (bgPath != null && !bgPath.isEmpty()) {
            backgroundView.setImage(loadImage(bgPath));
        } else {
            backgroundView.setImage(null);
        }
        characterLayer.getChildren().clear();
        dialogPanel.setVisible(false);
        overlayPane.setVisible(true);
        overlayText.setText(frame.getText());
    }

    private void showCharacterFrame(EditorFrame frame, EditorScene scene, int frameIndex) {
        overlayPane.setVisible(false);
        dialogPanel.setVisible(true);

        // Background
        String bgPath = resolveCurrentBackground(scene, frameIndex);
        if (bgPath != null && !bgPath.isEmpty()) {
            backgroundView.setImage(loadImage(bgPath));
        } else {
            backgroundView.setImage(null);
        }

        // Собираем состояние ВСЕХ персонажей на сцене
        // Каждый персонаж — отдельный объект, живёт пока не придёт removeFromScene для него
        Map<String, CharacterState> visibleCharacters = new LinkedHashMap<>();

        // Наследование из предыдущей сцены
        if (project != null) {
            Map<String, CharacterState> inherited = resolveInheritedCharacters(scene);
            visibleCharacters.putAll(inherited);
        }

        // Проходим все кадры от 0 до frameIndex
        java.util.List<EditorFrame> frames = scene.getFrames();
        int endIdx = (frameIndex >= 0 && frameIndex < frames.size()) ? frameIndex : frames.indexOf(frame);
        if (endIdx < 0) endIdx = 0;

        for (int i = 0; i <= endIdx && i < frames.size(); i++) {
            for (EditorCommand cmd : frames.get(i).getCommands()) {
                if (!"character".equals(cmd.getType())) continue;
                String action = cmd.getAction();
                String target = cmd.getTarget();
                if (action == null || target == null || target.isEmpty()) continue;

                if ("showPerson".equals(action)) {
                    // Появляется персонаж (или меняется поза)
                    CharacterState existing = visibleCharacters.get(target);
                    String pos = existing != null ? existing.position : "CENTER";
                    visibleCharacters.put(target, new CharacterState(cmd.getValue(), pos));
                } else if ("removeFromScene".equals(action)) {
                    // Убираем конкретного персонажа
                    visibleCharacters.remove(target);
                } else if (action.startsWith("move_") || action.startsWith("setFrom") || action.startsWith("runTo")) {
                    // Перемещение персонажа
                    CharacterState state = visibleCharacters.get(target);
                    if (state != null) {
                        String pos = action.replace("move_", "").replace("setFrom", "").replace("runTo", "");
                        if (!pos.isEmpty()) state.position = pos;
                    }
                }
            }
        }

        // Отрисовка всех видимых персонажей
        characterLayer.getChildren().clear();
        for (Map.Entry<String, CharacterState> entry : visibleCharacters.entrySet()) {
            String charName = entry.getKey();
            CharacterState state = entry.getValue();
            String spritePath = resolveSpritePath(charName, state.pose);
            if (spritePath == null) continue;

            Image img = loadImage(spritePath);
            if (img == null) continue;

            ImageView view = new ImageView(img);
            view.setPreserveRatio(true);
            view.fitHeightProperty().bind(heightProperty().multiply(0.7));
            StackPane.setAlignment(view, Pos.BOTTOM_CENTER);

            double offset = switch (state.position.toUpperCase()) {
                case "LEFT" -> -0.25;
                case "RIGHT" -> 0.25;
                default -> 0;
            };
            // Bind translate to width for responsive positioning
            view.translateXProperty().bind(widthProperty().multiply(offset));

            characterLayer.getChildren().add(view);
        }

        // Dialog text
        speakerLabel.setText(frame.getSpeakerName());
        if (!frame.getSpeakerColor().isEmpty()) {
            try {
                Color c = parseColor(frame.getSpeakerColor());
                speakerLabel.setTextFill(c);
            } catch (Exception e) {
                speakerLabel.setTextFill(Color.web("#ffcc00"));
            }
        } else {
            speakerLabel.setTextFill(Color.web("#ffcc00"));
        }

        String text = frame.getText();
        if (text.length() > 120) text = text.substring(0, 120) + "...";
        textLabel.setText(text);

        // Choices
        choiceContainer.getChildren().clear();
        if (frame.hasChoices()) {
            for (EditorChoice ch : frame.getChoices()) {
                Label btn = new Label(ch.getText());
                btn.getStyleClass().add("preview-choice-button");
                choiceContainer.getChildren().add(btn);
            }
        }
    }

    /**
     * Resolve current background by walking scene background + changebackground commands.
     */
    private String resolveCurrentBackground(EditorScene scene, int frameIndex) {
        String bg = scene.getBackground();

        if ((bg == null || bg.isEmpty()) && project != null) {
            bg = resolveInheritedBackground(scene);
        }

        java.util.List<EditorFrame> frames = scene.getFrames();
        int endIdx = Math.min(frameIndex, frames.size() - 1);

        for (int i = 0; i <= endIdx; i++) {
            for (EditorCommand cmd : frames.get(i).getCommands()) {
                if ("background".equals(cmd.getType()) && "changebackground".equals(cmd.getAction())) {
                    if (cmd.getValue() != null && !cmd.getValue().isEmpty()) {
                        bg = cmd.getValue();
                    }
                }
            }
        }
        return bg;
    }

    private String resolveInheritedBackground(EditorScene currentScene) {
        if (project == null) return null;
        java.util.Set<Integer> visited = new java.util.HashSet<>();
        visited.add(currentScene.getId());

        EditorScene prev = findPreviousScene(currentScene.getId(), visited);
        while (prev != null) {
            String bg = prev.getBackground();

            for (EditorFrame frame : prev.getFrames()) {
                for (EditorCommand cmd : frame.getCommands()) {
                    if ("background".equals(cmd.getType()) && "changebackground".equals(cmd.getAction())) {
                        if (cmd.getValue() != null && !cmd.getValue().isEmpty()) {
                            bg = cmd.getValue();
                        }
                    }
                }
            }

            if (bg != null && !bg.isEmpty()) {
                return bg;
            }

            visited.add(prev.getId());
            prev = findPreviousScene(prev.getId(), visited);
        }
        return null;
    }

    /**
     * Наследование состояния ВСЕХ персонажей из предыдущих сцен.
     * Собирает цепочку сцен от самой ранней до текущей,
     * затем применяет команды кумулятивно (showPerson, removeFromScene, move).
     */
    private Map<String, CharacterState> resolveInheritedCharacters(EditorScene currentScene) {
        Map<String, CharacterState> result = new LinkedHashMap<>();
        if (project == null) return result;

        // Собираем цепочку предыдущих сцен (от текущей назад)
        java.util.List<EditorScene> chain = new java.util.ArrayList<>();
        java.util.Set<Integer> visited = new java.util.HashSet<>();
        visited.add(currentScene.getId());

        EditorScene prev = findPreviousScene(currentScene.getId(), visited);
        while (prev != null) {
            chain.add(prev);
            visited.add(prev.getId());
            prev = findPreviousScene(prev.getId(), visited);
        }

        // Разворачиваем: от самой ранней сцены к ближайшей
        java.util.Collections.reverse(chain);

        // Применяем команды кумулятивно по всей цепочке
        for (EditorScene scene : chain) {
            for (EditorFrame f : scene.getFrames()) {
                for (EditorCommand cmd : f.getCommands()) {
                    if (!"character".equals(cmd.getType())) continue;
                    String action = cmd.getAction();
                    String target = cmd.getTarget();
                    if (action == null || target == null || target.isEmpty()) continue;

                    if ("showPerson".equals(action)) {
                        CharacterState existing = result.get(target);
                        String pos = existing != null ? existing.position : "CENTER";
                        result.put(target, new CharacterState(cmd.getValue(), pos));
                    } else if ("removeFromScene".equals(action)) {
                        result.remove(target);
                    } else if (action.startsWith("move_") || action.startsWith("setFrom") || action.startsWith("runTo")) {
                        CharacterState state = result.get(target);
                        if (state != null) {
                            String pos = action.replace("move_", "").replace("setFrom", "").replace("runTo", "");
                            if (!pos.isEmpty()) state.position = pos;
                        }
                    }
                }
            }
        }

        return result;
    }

    private EditorScene findPreviousScene(int targetId, java.util.Set<Integer> visited) {
        if (project == null) return null;
        for (EditorScene s : project.getScenes()) {
            if (!visited.contains(s.getId()) && s.getNextSceneId() == targetId) {
                return s;
            }
        }
        for (EditorScene s : project.getScenes()) {
            if (visited.contains(s.getId())) continue;
            for (EditorFrame f : s.getFrames()) {
                if (f.getChoices() != null) {
                    for (EditorChoice ch : f.getChoices()) {
                        if (ch.getRequestsId() == targetId) {
                            return s;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void clearPreview() {
        backgroundView.setImage(null);
        characterLayer.getChildren().clear();
        dialogPanel.setVisible(false);
        overlayPane.setVisible(false);
        speakerLabel.setText("");
        textLabel.setText("");
        choiceContainer.getChildren().clear();
    }

    private String resolveSpritePath(String characterName, String poseName) {
        if (project == null || poseName == null) return null;
        for (EditorCharacter ch : project.getCharacters()) {
            if (ch.getName().equals(characterName)) {
                for (EditorPose pose : ch.getPoses()) {
                    if (pose.getName().equals(poseName)) {
                        return pose.getSpritePath();
                    }
                }
                break;
            }
        }
        return null;
    }

    private Image loadImage(String path) {
        if (path == null || path.isEmpty()) return null;
        return imageCache.computeIfAbsent(path, p -> {
            try {
                String normalizedPath = p.replace('\\', '/');
                File file;
                if (project != null) {
                    file = new File(project.getProjectDir(), normalizedPath);
                } else {
                    file = new File(normalizedPath);
                }
                if (file.exists()) {
                    return new Image(file.toURI().toString(), 400, 300, true, true, false);
                }
            } catch (Exception e) {
                System.err.println("Preview image load failed: " + path + " - " + e.getMessage());
            }
            return null;
        });
    }

    private Color parseColor(String color) {
        if (color == null || color.isEmpty()) return Color.web("#ffcc00");
        return switch (color.toUpperCase()) {
            case "BLACK" -> Color.web("#333333");
            case "WHITE" -> Color.WHITE;
            case "GREY", "GRAY" -> Color.LIGHTGRAY;
            case "GREEN" -> Color.LIGHTGREEN;
            case "RED" -> Color.INDIANRED;
            case "BLUE" -> Color.LIGHTBLUE;
            default -> {
                try { yield Color.web(color); }
                catch (Exception e) { yield Color.web("#ffcc00"); }
            }
        };
    }
}
