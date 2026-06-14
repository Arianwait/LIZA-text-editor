package kz.aws.gametexteditor.util;

import java.util.ArrayDeque;
import java.util.Deque;

public class NavigationHistory {

    private static final int MAX_SIZE = 50;

    private final Deque<Integer> backStack = new ArrayDeque<>();
    private final Deque<Integer> forwardStack = new ArrayDeque<>();
    private int currentSceneId = -1;

    public void visit(int sceneId) {
        if (sceneId == currentSceneId) return;
        if (currentSceneId > 0) {
            backStack.push(currentSceneId);
            if (backStack.size() > MAX_SIZE) {
                backStack.removeLast();
            }
        }
        currentSceneId = sceneId;
        forwardStack.clear();
    }

    public int goBack() {
        if (backStack.isEmpty()) return -1;
        forwardStack.push(currentSceneId);
        currentSceneId = backStack.pop();
        return currentSceneId;
    }

    public int goForward() {
        if (forwardStack.isEmpty()) return -1;
        backStack.push(currentSceneId);
        currentSceneId = forwardStack.pop();
        return currentSceneId;
    }

    public boolean canGoBack() {
        return !backStack.isEmpty();
    }

    public boolean canGoForward() {
        return !forwardStack.isEmpty();
    }

    public void clear() {
        backStack.clear();
        forwardStack.clear();
        currentSceneId = -1;
    }
}
