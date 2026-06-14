package kz.aws.gametexteditor.util;

import kz.aws.gametexteditor.model.EditorScene;

import java.util.ArrayDeque;
import java.util.Deque;

public class UndoManager {

    private static final int MAX_HISTORY = 50;

    private final Deque<EditorScene> undoStack = new ArrayDeque<>();
    private final Deque<EditorScene> redoStack = new ArrayDeque<>();

    public void recordState(EditorScene scene) {
        if (undoStack.size() >= MAX_HISTORY) {
            ((ArrayDeque<EditorScene>) undoStack).removeLast();
        }
        undoStack.push(DeepCopyUtil.copyScene(scene));
        redoStack.clear();
    }

    public EditorScene undo(EditorScene current) {
        if (undoStack.isEmpty()) return null;
        redoStack.push(DeepCopyUtil.copyScene(current));
        return undoStack.pop();
    }

    public EditorScene redo(EditorScene current) {
        if (redoStack.isEmpty()) return null;
        undoStack.push(DeepCopyUtil.copyScene(current));
        return redoStack.pop();
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
