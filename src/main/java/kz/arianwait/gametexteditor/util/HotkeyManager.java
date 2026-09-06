package kz.arianwait.gametexteditor.util;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import kz.arianwait.gametexteditor.controller.MainController;

/**
 * Installs global keyboard shortcuts on the application scene.
 * Shortcuts that conflict with standard text editing (copy, paste, delete)
 * are skipped when a text input control has focus.
 */
public class HotkeyManager {

    /**
     * Installs all global hotkeys as an event filter on the given scene.
     *
     * @param scene the application scene
     * @param controller the main controller handling actions
     */
    public static void install(Scene scene, MainController controller) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (isTextInputFocused(scene) && isTextEditKey(e)) return;
            if (e.isAltDown()) {
                switch (e.getCode()) {
                    case LEFT -> {
                        controller.navigateBack();
                        e.consume();
                    }
                    case RIGHT -> {
                        controller.navigateForward();
                        e.consume();
                    }
                    default -> {}
                }
            }
            if (e.isControlDown()) {
                switch (e.getCode()) {
                    case S -> {
                        controller.save();
                        e.consume();
                    }
                    case Z -> {
                        if (e.isShiftDown()) {
                            controller.redo();
                        } else {
                            controller.undo();
                        }
                        e.consume();
                    }
                    case N -> {
                        controller.newFrame();
                        e.consume();
                    }
                    case C -> {
                        if (controller.isInEditorMode()) {
                            controller.copyFrame();
                            e.consume();
                        }
                    }
                    case V -> {
                        if (e.isShiftDown()) {
                            controller.validateProject();
                        } else if (controller.isInEditorMode()) {
                            controller.pasteFrame();
                        }
                        e.consume();
                    }
                    case F -> {
                        controller.openSearch();
                        e.consume();
                    }
                    case T -> {
                        if (controller.isInEditorMode()) {
                            controller.toggleFrameType();
                            e.consume();
                        }
                    }
                    default -> {}
                }
            }
            if (e.getCode() == KeyCode.ESCAPE) {
                if (controller.isInEditorMode()) {
                    controller.closeEditor();
                    e.consume();
                }
            }
            if (e.getCode() == KeyCode.DELETE) {
                controller.deleteSelected();
                e.consume();
            }
        });
    }

    /**
     * Checks whether the currently focused node is a text input control.
     *
     * @param scene the application scene
     * @return true if a TextField or TextArea has focus
     */
    private static boolean isTextInputFocused(Scene scene) {
        Node focused = scene.getFocusOwner();
        return focused instanceof TextInputControl;
    }

    /**
     * Checks whether the key event is a standard text editing shortcut
     * that should not be intercepted when a text input has focus.
     *
     * @param e the key event
     * @return true if this is a copy/paste/cut/undo/redo/delete key combination
     */
    private static boolean isTextEditKey(KeyEvent e) {
        if (e.getCode() == KeyCode.DELETE) return true;
        if (e.isControlDown()) {
            return switch (e.getCode()) {
                case C, V, X, Z, A -> true;
                default -> false;
            };
        }
        return false;
    }
}
