package kz.arianwait.gametexteditor.util;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import kz.arianwait.gametexteditor.model.EditorFrame;
import kz.arianwait.gametexteditor.model.EditorProject;
import kz.arianwait.gametexteditor.model.EditorScene;

public class TextStatistics {

    public static void show(EditorProject project, Stage owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("\u0421\u0442\u0430\u0442\u0438\u0441\u0442\u0438\u043a\u0430 \u0442\u0435\u043a\u0441\u0442\u0430");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setResizable(true);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("search-dialog");
        dialogPane.setPrefSize(550, 450);

        int totalWords = 0, totalChars = 0, totalFrames = 0;
        StringBuilder details = new StringBuilder();

        for (EditorScene scene : project.getScenes()) {
            int sceneWords = 0, sceneChars = 0;
            for (EditorFrame frame : scene.getFrames()) {
                String text = frame.getText();
                if (text != null && !text.isBlank()) {
                    sceneChars += text.length();
                    sceneWords += text.trim().split("\\s+").length;
                }
            }
            totalWords += sceneWords;
            totalChars += sceneChars;
            totalFrames += scene.getFrameCount();

            String speaker = scene.getFirstSpeaker();
            details.append(String.format(
                    "\u0421\u0446\u0435\u043d\u0430 %d%s: %d \u0441\u043b\u043e\u0432, %d \u0441\u0438\u043c\u0432\u043e\u043b\u043e\u0432, %d \u043a\u0430\u0434\u0440\u043e\u0432\n",
                    scene.getId(),
                    speaker != null ? " (" + speaker + ")" : "",
                    sceneWords, sceneChars, scene.getFrameCount()));
        }

        Label summary = new Label(String.format(
                "\u0412\u0441\u0435\u0433\u043e: %d \u0441\u0446\u0435\u043d, %d \u043a\u0430\u0434\u0440\u043e\u0432, %d \u0441\u043b\u043e\u0432, %d \u0441\u0438\u043c\u0432\u043e\u043b\u043e\u0432",
                project.getSceneCount(), totalFrames, totalWords, totalChars));
        summary.getStyleClass().add("validation-summary");

        TextArea detailsArea = new TextArea(details.toString());
        detailsArea.setEditable(false);
        detailsArea.getStyleClass().add("frame-text-area");
        VBox.setVgrow(detailsArea, Priority.ALWAYS);

        VBox content = new VBox(8, summary, detailsArea);
        content.setPadding(new Insets(12));

        dialogPane.setContent(content);
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }
}
