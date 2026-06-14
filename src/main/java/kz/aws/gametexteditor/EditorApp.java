package kz.aws.gametexteditor;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import kz.aws.gametexteditor.controller.MainController;
import kz.aws.gametexteditor.util.CssLoader;
import kz.aws.gametexteditor.util.HotkeyManager;
import kz.aws.gametexteditor.window.CustomTitleBar;

public class EditorApp extends Application {

    @Override
    public void start(Stage stage) {
        stage.initStyle(StageStyle.UNDECORATED);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("editor-root");

        MainController mainController = new MainController(stage);

        CustomTitleBar titleBar = new CustomTitleBar(stage);
        // Bind dirty indicator to title
        mainController.getDirtyTracker().dirtyProperty().addListener((o, ov, nv) -> {
            titleBar.setDirtyIndicator(nv);
        });

        root.setTop(titleBar);
        root.setCenter(mainController.getView());

        Scene scene = new Scene(root, 1280, 720);
        scene.getStylesheets().add(CssLoader.getEditorCss());
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        // Install hotkeys
        HotkeyManager.install(scene, mainController);

        // Close request
        stage.setOnCloseRequest(e -> {
            e.consume();
            mainController.requestClose();
        });

        stage.setScene(scene);
        stage.setMinWidth(960);
        stage.setMinHeight(600);
        stage.setTitle("Liza Text Editor");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
