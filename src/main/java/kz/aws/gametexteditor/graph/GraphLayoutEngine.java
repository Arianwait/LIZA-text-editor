package kz.aws.gametexteditor.graph;

import kz.aws.gametexteditor.model.EditorScene;
import kz.aws.gametexteditor.model.SceneEdge;

import java.util.*;

public class GraphLayoutEngine {

    private static final double H_SPACING = 220;
    private static final double V_SPACING = 120;
    private static final double MARGIN = 60;

    public static void layout(List<EditorScene> scenes, List<SceneEdge> edges,
                              Map<Integer, SceneNode> nodeMap) {
        if (scenes.isEmpty()) return;

        // Build adjacency
        Map<Integer, List<Integer>> adj = new HashMap<>();
        Set<Integer> allIds = new HashSet<>();
        for (EditorScene s : scenes) {
            allIds.add(s.getId());
            adj.put(s.getId(), new ArrayList<>());
        }
        for (SceneEdge edge : edges) {
            adj.computeIfAbsent(edge.getSourceId(), k -> new ArrayList<>())
                    .add(edge.getTargetId());
        }

        // BFS layer assignment from scene with smallest ID
        int startId = scenes.stream().mapToInt(EditorScene::getId).min().orElse(1);
        Map<Integer, Integer> layerOf = new HashMap<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.add(startId);
        layerOf.put(startId, 0);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            int currentLayer = layerOf.get(current);
            List<Integer> neighbors = adj.getOrDefault(current, List.of());
            for (int next : neighbors) {
                if (!layerOf.containsKey(next)) {
                    layerOf.put(next, currentLayer + 1);
                    queue.add(next);
                }
            }
        }

        // Place orphans (unreachable from start) on their own layers
        int maxLayer = layerOf.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        for (int id : allIds) {
            if (!layerOf.containsKey(id)) {
                maxLayer++;
                layerOf.put(id, maxLayer);
            }
        }

        // Group by layer
        Map<Integer, List<Integer>> layerToNodes = new TreeMap<>();
        for (Map.Entry<Integer, Integer> entry : layerOf.entrySet()) {
            layerToNodes.computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
                    .add(entry.getKey());
        }

        // Sort nodes within each layer by ID
        for (List<Integer> nodesInLayer : layerToNodes.values()) {
            nodesInLayer.sort(Integer::compareTo);
        }

        // Position nodes
        for (Map.Entry<Integer, List<Integer>> entry : layerToNodes.entrySet()) {
            int layer = entry.getKey();
            List<Integer> nodesInLayer = entry.getValue();
            double y = MARGIN + layer * V_SPACING;
            double totalWidth = nodesInLayer.size() * H_SPACING;
            double startX = MARGIN + (800 - totalWidth) / 2;
            if (startX < MARGIN) startX = MARGIN;

            for (int i = 0; i < nodesInLayer.size(); i++) {
                SceneNode node = nodeMap.get(nodesInLayer.get(i));
                if (node != null) {
                    node.setLayoutX(startX + i * H_SPACING);
                    node.setLayoutY(y);
                }
            }
        }
    }
}
