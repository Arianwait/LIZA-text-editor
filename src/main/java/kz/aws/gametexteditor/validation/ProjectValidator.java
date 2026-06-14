package kz.aws.gametexteditor.validation;

import kz.aws.gametexteditor.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class ProjectValidator {

    public record Issue(Severity severity, String message, int sceneId, int frameIndex) {}

    public enum Severity { ERROR, WARNING }

    public static List<Issue> validate(EditorProject project) {
        List<Issue> issues = new ArrayList<>();
        List<EditorScene> scenes = project.getScenes();
        Set<Integer> sceneIds = scenes.stream()
                .map(EditorScene::getId)
                .collect(Collectors.toSet());

        // Collect start scenes from chapters
        Set<Integer> chapterStartIds = new HashSet<>();
        for (EditorChapter ch : project.getChapters()) {
            chapterStartIds.add(ch.getSceneId());
        }

        // Collect all referenced scene IDs (for orphan detection)
        Set<Integer> referencedIds = new HashSet<>(chapterStartIds);

        Set<String> characterNames = project.getCharacters().stream()
                .map(EditorCharacter::getName)
                .collect(Collectors.toSet());

        for (EditorScene scene : scenes) {
            int sid = scene.getId();

            // 1. Check nextSceneId
            if (scene.getNextSceneId() > 0) {
                referencedIds.add(scene.getNextSceneId());
                if (!sceneIds.contains(scene.getNextSceneId())) {
                    issues.add(new Issue(Severity.ERROR,
                            "\u0421\u0446\u0435\u043d\u0430 " + sid + " \u2192 nextScene=" + scene.getNextSceneId()
                                    + " \u043d\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u0435\u0442",
                            sid, -1));
                }
            }

            // 2. Check for dead-end scenes
            if (scene.getNextSceneId() <= 0 && !scene.hasChoices()) {
                issues.add(new Issue(Severity.WARNING,
                        "\u0421\u0446\u0435\u043d\u0430 " + sid + " \u2014 \u0442\u0443\u043f\u0438\u043a (nextScene \u043d\u0435 \u0437\u0430\u0434\u0430\u043d, \u043d\u0435\u0442 \u0432\u044b\u0431\u043e\u0440\u043e\u0432)",
                        sid, -1));
            }

            List<EditorFrame> frames = scene.getFrames();
            for (int i = 0; i < frames.size(); i++) {
                EditorFrame frame = frames.get(i);

                // 3. Empty text
                if (frame.getText() == null || frame.getText().isBlank()) {
                    issues.add(new Issue(Severity.WARNING,
                            "\u0421\u0446\u0435\u043d\u0430 " + sid + ", \u043a\u0430\u0434\u0440 " + (i + 1)
                                    + " \u2014 \u043f\u0443\u0441\u0442\u043e\u0439 \u0442\u0435\u043a\u0441\u0442",
                            sid, i));
                }

                // 4. Unknown character
                if (frame.getType() == EditorFrame.FrameType.CHARACTER
                        && frame.getSpeakerName() != null
                        && !frame.getSpeakerName().isBlank()
                        && !characterNames.contains(frame.getSpeakerName())) {
                    issues.add(new Issue(Severity.WARNING,
                            "\u0421\u0446\u0435\u043d\u0430 " + sid + ", \u043a\u0430\u0434\u0440 " + (i + 1)
                                    + " \u2014 \u043f\u0435\u0440\u0441\u043e\u043d\u0430\u0436 \u00ab" + frame.getSpeakerName()
                                    + "\u00bb \u043d\u0435 \u043e\u043f\u0440\u0435\u0434\u0435\u043b\u0451\u043d",
                            sid, i));
                }

                // 5. Check command references
                for (EditorCommand cmd : frame.getCommands()) {
                    if (cmd.getOnSuccess() > 0 && !sceneIds.contains(cmd.getOnSuccess())) {
                        issues.add(new Issue(Severity.ERROR,
                                "\u0421\u0446\u0435\u043d\u0430 " + sid + ", \u043a\u0430\u0434\u0440 " + (i + 1)
                                        + " \u2014 \u043a\u043e\u043c\u0430\u043d\u0434\u0430 onSuccess=" + cmd.getOnSuccess()
                                        + " \u043d\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u0435\u0442",
                                sid, i));
                    }
                    if (cmd.getOnFailure() > 0 && !sceneIds.contains(cmd.getOnFailure())) {
                        issues.add(new Issue(Severity.ERROR,
                                "\u0421\u0446\u0435\u043d\u0430 " + sid + ", \u043a\u0430\u0434\u0440 " + (i + 1)
                                        + " \u2014 \u043a\u043e\u043c\u0430\u043d\u0434\u0430 onFailure=" + cmd.getOnFailure()
                                        + " \u043d\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u0435\u0442",
                                sid, i));
                    }
                    if (cmd.getOnSuccess() > 0) referencedIds.add(cmd.getOnSuccess());
                    if (cmd.getOnFailure() > 0) referencedIds.add(cmd.getOnFailure());
                }

                // 6. Check choice references
                if (frame.getChoices() != null) {
                    for (EditorChoice ch : frame.getChoices()) {
                        if (ch.getRequestsId() > 0) {
                            referencedIds.add(ch.getRequestsId());
                            if (!sceneIds.contains(ch.getRequestsId())) {
                                issues.add(new Issue(Severity.ERROR,
                                        "\u0421\u0446\u0435\u043d\u0430 " + sid + ", \u043a\u0430\u0434\u0440 " + (i + 1)
                                                + " \u2014 \u0432\u044b\u0431\u043e\u0440 \u2192 " + ch.getRequestsId()
                                                + " \u043d\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u0435\u0442",
                                        sid, i));
                            }
                        }
                    }
                }
            }
        }

        // 7. Orphan scenes — no incoming references
        for (EditorScene scene : scenes) {
            if (!referencedIds.contains(scene.getId())) {
                issues.add(new Issue(Severity.WARNING,
                        "\u0421\u0446\u0435\u043d\u0430 " + scene.getId()
                                + " \u2014 \u043d\u0435\u0442 \u0432\u0445\u043e\u0434\u044f\u0449\u0438\u0445 \u0441\u0441\u044b\u043b\u043e\u043a (\u0441\u0438\u0440\u043e\u0442\u0430)",
                        scene.getId(), -1));
            }
        }

        // Sort: errors first, then warnings
        issues.sort(Comparator.comparing(Issue::severity)
                .thenComparing(Issue::sceneId)
                .thenComparing(Issue::frameIndex));

        return issues;
    }
}
