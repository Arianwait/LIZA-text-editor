package kz.aws.gametexteditor.project;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class NewProjectWizard {

    /**
     * Shows the new project wizard. Returns the project directory or null if cancelled.
     */
    public static File show(Stage owner) {
        Dialog<File> dialog = new Dialog<>();
        dialog.setTitle("\u041d\u043e\u0432\u044b\u0439 \u043f\u0440\u043e\u0435\u043a\u0442");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setResizable(false);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("new-project-dialog");
        dialogPane.setPrefWidth(500);

        // Fields
        TextField nameField = new TextField("MyNovel");
        nameField.setPromptText("\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 \u043f\u0440\u043e\u0435\u043a\u0442\u0430");

        TextField dirField = new TextField();
        dirField.setPromptText("\u041f\u0443\u0442\u044c \u043a \u043f\u0430\u043f\u043a\u0435");
        dirField.setEditable(false);
        HBox.setHgrow(dirField, Priority.ALWAYS);

        Button browseBtn = new Button("\u041e\u0431\u0437\u043e\u0440...");
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("\u0412\u044b\u0431\u0440\u0430\u0442\u044c \u043f\u0430\u043f\u043a\u0443 \u0434\u043b\u044f \u043f\u0440\u043e\u0435\u043a\u0442\u0430");
            File dir = dc.showDialog(owner);
            if (dir != null) {
                dirField.setText(dir.getAbsolutePath());
            }
        });

        HBox dirBox = new HBox(8, dirField, browseBtn);
        HBox.setHgrow(dirField, Priority.ALWAYS);

        Label infoLabel = new Label(
                "\u0412 \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u043e\u0439 \u043f\u0430\u043f\u043a\u0435 \u0431\u0443\u0434\u0435\u0442 \u0441\u043e\u0437\u0434\u0430\u043d\u0430 \u043f\u043e\u0434\u043f\u0430\u043f\u043a\u0430 \u0441 \u0438\u043c\u0435\u043d\u0435\u043c \u043f\u0440\u043e\u0435\u043a\u0442\u0430\n" +
                "\u0438 \u0432\u0441\u044f \u043d\u0435\u043e\u0431\u0445\u043e\u0434\u0438\u043c\u0430\u044f \u0441\u0442\u0440\u0443\u043a\u0442\u0443\u0440\u0430 \u0444\u0430\u0439\u043b\u043e\u0432.");
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-text-fill: #8899aa; -fx-font-size: 11px;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.add(new Label("\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("\u041f\u0430\u043f\u043a\u0430:"), 0, 1);
        grid.add(dirBox, 1, 1);
        grid.add(infoLabel, 0, 2, 2, 1);

        javafx.scene.layout.ColumnConstraints col0 = new javafx.scene.layout.ColumnConstraints();
        col0.setMinWidth(80);
        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);

        dialogPane.setContent(grid);
        dialogPane.getButtonTypes().addAll(
                new ButtonType("\u0421\u043e\u0437\u0434\u0430\u0442\u044c", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                String name = nameField.getText().trim();
                String parentPath = dirField.getText().trim();
                if (name.isEmpty() || parentPath.isEmpty()) return null;

                File projectDir = new File(parentPath, name);
                try {
                    createProjectStructure(projectDir, name);
                    return projectDir;
                } catch (IOException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("\u041e\u0448\u0438\u0431\u043a\u0430");
                    alert.setContentText("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0437\u0434\u0430\u0442\u044c \u043f\u0440\u043e\u0435\u043a\u0442: " + ex.getMessage());
                    alert.initOwner(owner);
                    alert.showAndWait();
                }
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    private static void createProjectStructure(File projectDir, String title) throws IOException {
        // Create directories
        mkdirs(projectDir, "lib/config/UI");
        mkdirs(projectDir, "lib/MainMenuImg");
        mkdirs(projectDir, "lib/person");
        mkdirs(projectDir, "lib/sound");
        mkdirs(projectDir, "lib/Scene");
        mkdirs(projectDir, "lib/Scene/backgrounds");
        mkdirs(projectDir, "lib/Logo");
        mkdirs(projectDir, "lib/Json");

        // Dialog_Structured.xml
        writeFile(projectDir, "lib/Scene/Dialog_Structured.xml", DIALOG_XML);

        // Person.xml
        writeFile(projectDir, "lib/Scene/Person.xml", PERSON_XML);

        // Chapters.xml
        writeFile(projectDir, "lib/config/Chapters.xml", CHAPTERS_XML);

        // SettingsConfig.json
        writeFile(projectDir, "lib/config/SettingsConfig.json", SETTINGS_JSON);

        // ThemesConfig.json
        writeFile(projectDir, "lib/config/ThemesConfig.json", THEMES_JSON);

        // style.css
        writeFile(projectDir, "lib/config/style.css", STYLE_CSS);

        // dialog_styles.css
        writeFile(projectDir, "lib/config/dialog_styles.css", "/* Dialog styles */\n");

        // Buttons.xml
        writeFile(projectDir, "lib/config/UI/Buttons.xml", BUTTONS_XML);

        // MainMenu.xml
        writeFile(projectDir, "lib/config/UI/MainMenu.xml",
                MAIN_MENU_XML.replace("{{TITLE}}", title));

        // SceneSettings.xml
        writeFile(projectDir, "lib/config/UI/SceneSettings.xml", SCENE_SETTINGS_XML);
    }

    private static void mkdirs(File root, String subpath) {
        new File(root, subpath).mkdirs();
    }

    private static void writeFile(File root, String relativePath, String content) throws IOException {
        Path path = new File(root, relativePath).toPath();
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    // --- Templates ---

    private static final String DIALOG_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <game>
                <dialog id="1" nextScene="-1" background="" music="">
                    <character name="" color="">
                        \u041d\u0430\u0447\u0430\u043b\u043e \u0438\u0441\u0442\u043e\u0440\u0438\u0438
                    </character>
                </dialog>
            </game>
            """;

    private static final String PERSON_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Persons>
            </Persons>
            """;

    private static final String CHAPTERS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <chapters>
                <chapter id="1" name="\u0413\u043b\u0430\u0432\u0430 1" sceneId="1"/>
            </chapters>
            """;

    private static final String SETTINGS_JSON = """
            {
              "windowWidth": 1024,
              "windowHeight": 768,
              "fullscreen": false,
              "volumeValue": 0.5,
              "uiTheme": "walk"
            }
            """;

    private static final String THEMES_JSON = """
            {
              "themes": [
                { "id": "walk", "name": "\u041f\u0440\u043e\u0433\u0443\u043b\u043a\u0430", "background": "" }
              ]
            }
            """;

    private static final String BUTTONS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <ui-config>
                <defaults>
                    <sound-hover>hover</sound-hover>
                    <sound-click>click</sound-click>
                    <style-class>game-button</style-class>
                </defaults>
                <button id="menu-btn-continue"  context="main-menu" text="\u041f\u0440\u043e\u0434\u043e\u043b\u0436\u0438\u0442\u044c"/>
                <button id="menu-btn-start"     context="main-menu" text="\u041d\u0430\u0447\u0430\u0442\u044c \u043d\u043e\u0432\u0435\u043b\u043b\u0443"/>
                <button id="menu-btn-new-game"  context="main-menu" text="\u041d\u0430\u0447\u0430\u0442\u044c \u043d\u043e\u0432\u0443\u044e \u0438\u0433\u0440\u0443"/>
                <button id="menu-btn-settings"  context="main-menu" text="\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438"/>
                <button id="menu-btn-exit"      context="main-menu" text="\u0412\u044b\u0445\u043e\u0434"/>
                <button id="menu-btn-back"      context="submenu"   text="\u041d\u0430\u0437\u0430\u0434"/>
                <button id="game-btn-menu"      context="game-panel" text="\u0413\u043b\u0430\u0432\u043d\u043e\u0435 \u043c\u0435\u043d\u044e"/>
                <button id="game-btn-save"      context="game-panel" text="\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0438\u0433\u0440\u0443"/>
                <button id="game-btn-load"      context="game-panel" text="\u0417\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c \u0438\u0433\u0440\u0443"/>
                <button id="game-btn-back"      context="game-panel" text="\u041d\u0430\u0437\u0430\u0434"/>
                <button id="game-btn-next"      context="game-panel" text="\u0414\u0430\u043b\u0435\u0435"/>
                <button id="game-btn-history"   context="game-panel" text="\u0418\u0441\u0442\u043e\u0440\u0438\u044f"/>
            </ui-config>
            """;

    private static final String MAIN_MENU_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <main-menu-config>
                <resources>
                    <background-image></background-image>
                    <table-image></table-image>
                    <music></music>
                </resources>
                <positioning>
                    <margin-right>0.7</margin-right>
                    <margin-top>0.2</margin-top>
                    <margin-left>0.05</margin-left>
                </positioning>
                <font>
                    <title-size-multiplier>0.03</title-size-multiplier>
                </font>
                <spacing>
                    <menu-padding>50.0</menu-padding>
                    <menu-spacing>10.0</menu-spacing>
                </spacing>
                <preload>
                    <timeout-ms>5000</timeout-ms>
                    <check-interval-ms>50</check-interval-ms>
                </preload>
                <title>
                    <text>{{TITLE}}</text>
                    <color>brown</color>
                    <weight>bold</weight>
                </title>
            </main-menu-config>
            """;

    private static final String SCENE_SETTINGS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <scene-settings>
                <table-detail>
                    <table-image></table-image>
                    <table-size>
                        <width-multiplier>1.45</width-multiplier>
                        <height-multiplier>0.115</height-multiplier>
                    </table-size>
                    <table-position>
                        <bottom-margin>0.01</bottom-margin>
                    </table-position>
                    <text>
                        <wrapping-width-multiplier>0.65</wrapping-width-multiplier>
                        <dialog-font-size-multiplier>0.025</dialog-font-size-multiplier>
                        <name-font-size-multiplier>0.030</name-font-size-multiplier>
                        <text-padding-top>0.03</text-padding-top>
                        <text-padding-left>0.09</text-padding-left>
                        <text-content-width>0.60</text-content-width>
                    </text>
                    <gradient-panel>
                        <use-gradient>true</use-gradient>
                        <height-multiplier>0.30</height-multiplier>
                        <color-start>rgba(0, 50, 100, 0.0)</color-start>
                        <color-end>rgba(0, 20, 60, 0.95)</color-end>
                    </gradient-panel>
                    <buttons>
                        <spacing-multiplier>0.01</spacing-multiplier>
                        <padding-multiplier>0.015</padding-multiplier>
                        <width-multiplier>0.10</width-multiplier>
                        <height-multiplier>0.05</height-multiplier>
                        <font-size-multiplier>0.015</font-size-multiplier>
                        <menu-bottom-margin>0.027</menu-bottom-margin>
                    </buttons>
                </table-detail>
                <scene-selection>
                    <scroll-pane>
                        <max-height-multiplier>0.4</max-height-multiplier>
                    </scroll-pane>
                </scene-selection>
            </scene-settings>
            """;

    private static final String STYLE_CSS = """
            .game-button {
                -fx-font-family: "Verdana", "Arial", sans-serif;
                -fx-text-fill: #e8f4fc;
                -fx-padding: 10 16;
                -fx-alignment: center;
                -fx-pref-width: 200px;
                -fx-pref-height: 50px;
                -fx-background-color: linear-gradient(to bottom, #1e4d6e 0%, #2a6b8a 50%, #1e4d6e 100%);
                -fx-border-width: 1px;
                -fx-border-color: rgba(100, 180, 255, 0.5);
                -fx-border-radius: 8px;
                -fx-background-radius: 8px;
                -fx-cursor: hand;
            }
            .game-button:hover {
                -fx-background-color: linear-gradient(to bottom, #2a6b8a 0%, #3a8ab0 50%, #2a6b8a 100%);
            }
            .dialog-text {
                -fx-font-family: "Verdana", "Arial", sans-serif;
                -fx-font-weight: bold;
                -fx-fill: white;
                -fx-effect: dropshadow(two-pass-box, black, 2, 1.0, 1, 1);
            }
            .speaker-name {
                -fx-font-family: "Verdana", "Arial", sans-serif;
                -fx-font-weight: bold;
                -fx-fill: #ffcc00;
                -fx-effect: dropshadow(gaussian, black, 2, 1.0, 1, 1);
            }
            """;
}
