package kz.aws.gametexteditor.parser;

import kz.aws.gametexteditor.model.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EdgeExtractor {

    public static List<SceneEdge> extract(List<EditorScene> scenes) {
        List<SceneEdge> edges = new ArrayList<>();
        Set<Integer> sceneIds = new HashSet<>();
        for (EditorScene s : scenes) {
            sceneIds.add(s.getId());
        }

        for (EditorScene scene : scenes) {
            // 1. nextScene edge (only if target exists and scene has no choices)
            if (scene.getNextSceneId() > 0 && sceneIds.contains(scene.getNextSceneId())) {
                if (!scene.hasChoices()) {
                    edges.add(new SceneEdge(
                            scene.getId(), scene.getNextSceneId(),
                            SceneEdge.EdgeType.NEXT_SCENE, ""));
                }
            }

            // 2. Choice edges
            for (EditorFrame frame : scene.getFrames()) {
                if (frame.hasChoices()) {
                    for (EditorChoice choice : frame.getChoices()) {
                        if (choice.getRequestsId() > 0 && sceneIds.contains(choice.getRequestsId())) {
                            edges.add(new SceneEdge(
                                    scene.getId(), choice.getRequestsId(),
                                    SceneEdge.EdgeType.CHOICE, choice.getText()));
                        }
                    }
                }

                // 3. Puzzle edges
                for (EditorCommand cmd : frame.getCommands()) {
                    String type = cmd.getType();
                    if ("puzzle".equals(type) || "panel".equals(type)) {
                        if (cmd.getOnSuccess() > 0 && sceneIds.contains(cmd.getOnSuccess())) {
                            edges.add(new SceneEdge(
                                    scene.getId(), cmd.getOnSuccess(),
                                    SceneEdge.EdgeType.PUZZLE_SUCCESS, "Success"));
                        }
                        if (cmd.getOnFailure() > 0 && sceneIds.contains(cmd.getOnFailure())) {
                            edges.add(new SceneEdge(
                                    scene.getId(), cmd.getOnFailure(),
                                    SceneEdge.EdgeType.PUZZLE_FAILURE, "Failure"));
                        }
                    }
                }
            }
        }
        return edges;
    }
}
