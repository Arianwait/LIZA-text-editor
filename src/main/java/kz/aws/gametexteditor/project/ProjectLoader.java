package kz.aws.gametexteditor.project;

import javafx.scene.control.Alert;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import kz.aws.gametexteditor.model.*;
import kz.aws.gametexteditor.parser.*;
import kz.aws.gametexteditor.model.EditorVariable;

import java.io.File;
import java.util.List;

public class ProjectLoader {

    public static EditorProject openProject(Stage stage) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Открыть проект Liza");
        File dir = dc.showDialog(stage);
        if (dir == null) return null;

        return loadFromDirectory(dir, stage);
    }

    public static EditorProject loadFromDirectory(File dir, Stage stage) {
        // Главный XML сценария
        File dialogXml = resolveFile(dir, "lib/Scene/Dialog_Structured.xml");
        if (dialogXml == null) {
            showError(stage, "Файл Dialog_Structured.xml не найден в " + dir.getAbsolutePath());
            return null;
        }

        // Персонажи
        File personXml = resolveFile(dir, "lib/Scene/Person.xml");

        // Конфиги
        File chaptersXml = resolveFile(dir, "lib/config/Chapters.xml");
        File themesJson = resolveFile(dir, "lib/config/ThemesConfig.json");
        File buttonsXml = resolveFile(dir, "lib/config/UI/Buttons.xml");

        // Парсинг
        List<EditorScene> scenes = DialogXmlParser.parse(dialogXml);
        List<EditorCharacter> characters = PersonXmlParser.parse(personXml);
        List<SceneEdge> edges = EdgeExtractor.extract(scenes);
        List<EditorChapter> chapters = ChaptersParser.parse(chaptersXml);
        List<EditorTheme> themes = ThemesParser.parse(themesJson);
        List<EditorButton> buttons = ButtonsParser.parse(buttonsXml);
        List<EditorVariable> variables = PersonXmlParser.parseVariables(personXml);

        return new EditorProject(dir, scenes, characters, edges, chapters, themes, buttons, variables);
    }

    private static File resolveFile(File dir, String relativePath) {
        File f = new File(dir, relativePath);
        if (f.exists()) return f;
        // Попробовать с обратными слэшами (Windows)
        f = new File(dir, relativePath.replace('/', '\\'));
        return f.exists() ? f : null;
    }

    private static void showError(Stage stage, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText("Не удалось открыть проект");
        alert.setContentText(message);
        alert.initOwner(stage);
        alert.showAndWait();
    }
}
