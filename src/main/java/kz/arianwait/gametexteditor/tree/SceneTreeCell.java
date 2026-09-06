package kz.arianwait.gametexteditor.tree;

import javafx.scene.control.TreeCell;

import java.util.List;

public class SceneTreeCell extends TreeCell<TreeItemData> {

    private static final List<String> ALL_STYLES = List.of(
            "tree-cell-root", "tree-cell-section", "tree-cell-scene", "tree-cell-frame",
            "tree-cell-character", "tree-cell-pose", "tree-cell-chapter",
            "tree-cell-theme", "tree-cell-button"
    );

    private static final List<String> TAG_STYLES = List.of(
            "tree-cell-tag-red", "tree-cell-tag-green", "tree-cell-tag-blue",
            "tree-cell-tag-yellow", "tree-cell-tag-purple"
    );

    @Override
    protected void updateItem(TreeItemData item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            getStyleClass().removeAll(ALL_STYLES);
            getStyleClass().removeAll(TAG_STYLES);
        } else {
            setText(item.getDisplayText());
            getStyleClass().removeAll(ALL_STYLES);
            getStyleClass().removeAll(TAG_STYLES);
            switch (item.getItemType()) {
                case ROOT -> getStyleClass().add("tree-cell-root");
                case SECTION -> getStyleClass().add("tree-cell-section");
                case SCENE -> getStyleClass().add("tree-cell-scene");
                case FRAME -> getStyleClass().add("tree-cell-frame");
                case CHARACTER -> getStyleClass().add("tree-cell-character");
                case POSE -> getStyleClass().add("tree-cell-pose");
                case CHAPTER -> getStyleClass().add("tree-cell-chapter");
                case THEME -> getStyleClass().add("tree-cell-theme");
                case BUTTON -> getStyleClass().add("tree-cell-button");
            }

            String tag = item.getTag();
            if (tag != null && !tag.isEmpty()) {
                getStyleClass().add("tree-cell-tag-" + tag);
            }
        }
    }
}
