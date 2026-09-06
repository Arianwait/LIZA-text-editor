package kz.arianwait.gametexteditor.tree;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import kz.arianwait.gametexteditor.model.*;

public class SceneTreeBuilder {

    public static TreeView<TreeItemData> build(EditorProject project) {
        return build(project, null);
    }

    public static TreeView<TreeItemData> build(EditorProject project, String filter) {
        TreeItem<TreeItemData> root = new TreeItem<>(TreeItemData.root("\u041f\u0440\u043e\u0435\u043a\u0442"));
        root.setExpanded(true);

        boolean hasFilter = filter != null && !filter.isBlank();
        String filterLower = hasFilter ? filter.toLowerCase() : "";

        // --- \u0413\u043b\u0430\u0432\u044b ---
        if (!project.getChapters().isEmpty() && !hasFilter) {
            TreeItem<TreeItemData> chaptersSection = new TreeItem<>(
                    TreeItemData.section("\u0413\u043b\u0430\u0432\u044b (" + project.getChapterCount() + ")"));
            chaptersSection.setExpanded(true);
            for (EditorChapter ch : project.getChapters()) {
                chaptersSection.getChildren().add(new TreeItem<>(TreeItemData.forChapter(ch)));
            }
            root.getChildren().add(chaptersSection);
        }

        // --- \u0421\u0446\u0435\u043d\u044b ---
        TreeItem<TreeItemData> scenesSection = new TreeItem<>(
                TreeItemData.section("\u0421\u0446\u0435\u043d\u044b (" + project.getSceneCount() + ")"));
        scenesSection.setExpanded(true);
        for (EditorScene scene : project.getScenes()) {
            if (hasFilter && !matchesFilter(scene, filterLower)) {
                continue;
            }
            TreeItem<TreeItemData> sceneItem = new TreeItem<>(TreeItemData.forScene(scene));
            for (int i = 0; i < scene.getFrames().size(); i++) {
                EditorFrame frame = scene.getFrames().get(i);
                sceneItem.getChildren().add(
                        new TreeItem<>(TreeItemData.forFrame(scene.getId(), i, frame))
                );
            }
            if (hasFilter) {
                sceneItem.setExpanded(true);
            }
            scenesSection.getChildren().add(sceneItem);
        }
        root.getChildren().add(scenesSection);

        // --- \u041f\u0435\u0440\u0441\u043e\u043d\u0430\u0436\u0438 ---
        if (!project.getCharacters().isEmpty() && !hasFilter) {
            TreeItem<TreeItemData> charsSection = new TreeItem<>(
                    TreeItemData.section("\u041f\u0435\u0440\u0441\u043e\u043d\u0430\u0436\u0438 (" + project.getCharacterCount() + ")"));
            for (EditorCharacter ch : project.getCharacters()) {
                TreeItem<TreeItemData> charItem = new TreeItem<>(TreeItemData.forCharacter(ch));
                for (EditorPose pose : ch.getPoses()) {
                    charItem.getChildren().add(new TreeItem<>(TreeItemData.forPose(pose)));
                }
                charsSection.getChildren().add(charItem);
            }
            root.getChildren().add(charsSection);
        }

        // --- \u0422\u0435\u043c\u044b ---
        if (!project.getThemes().isEmpty() && !hasFilter) {
            TreeItem<TreeItemData> themesSection = new TreeItem<>(
                    TreeItemData.section("\u0422\u0435\u043c\u044b (" + project.getThemes().size() + ")"));
            for (EditorTheme theme : project.getThemes()) {
                themesSection.getChildren().add(new TreeItem<>(TreeItemData.forTheme(theme)));
            }
            root.getChildren().add(themesSection);
        }

        // --- \u041a\u043d\u043e\u043f\u043a\u0438 ---
        if (!project.getButtons().isEmpty() && !hasFilter) {
            TreeItem<TreeItemData> buttonsSection = new TreeItem<>(
                    TreeItemData.section("\u041a\u043d\u043e\u043f\u043a\u0438 (" + project.getButtons().size() + ")"));
            for (EditorButton btn : project.getButtons()) {
                buttonsSection.getChildren().add(new TreeItem<>(TreeItemData.forButton(btn)));
            }
            root.getChildren().add(buttonsSection);
        }

        TreeView<TreeItemData> tree = new TreeView<>(root);
        tree.setCellFactory(tv -> new SceneTreeCell());
        tree.getStyleClass().add("scene-tree");
        return tree;
    }

    private static boolean matchesFilter(EditorScene scene, String filterLower) {
        // Match by scene ID
        if (String.valueOf(scene.getId()).contains(filterLower)) return true;
        // Match by first speaker
        String speaker = scene.getFirstSpeaker();
        if (!speaker.isEmpty() && speaker.toLowerCase().contains(filterLower)) return true;
        // Match by frame text
        for (EditorFrame frame : scene.getFrames()) {
            if (frame.getText() != null && frame.getText().toLowerCase().contains(filterLower)) return true;
            if (frame.getSpeakerName() != null && frame.getSpeakerName().toLowerCase().contains(filterLower)) return true;
        }
        return false;
    }
}
