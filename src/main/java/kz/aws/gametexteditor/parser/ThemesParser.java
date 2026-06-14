package kz.aws.gametexteditor.parser;

import kz.aws.gametexteditor.model.EditorTheme;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThemesParser {

    public static List<EditorTheme> parse(File jsonFile) {
        List<EditorTheme> themes = new ArrayList<>();
        if (jsonFile == null || !jsonFile.exists()) return themes;

        try {
            String content = Files.readString(jsonFile.toPath());
            // Simple regex parse — no external JSON lib needed
            Pattern themePattern = Pattern.compile(
                    "\\{\\s*\"id\"\\s*:\\s*\"([^\"]+)\"\\s*," +
                    "\\s*\"name\"\\s*:\\s*\"([^\"]+)\"\\s*," +
                    "\\s*\"background\"\\s*:\\s*\"([^\"]+)\"\\s*\\}");
            Matcher m = themePattern.matcher(content);
            while (m.find()) {
                EditorTheme theme = new EditorTheme();
                theme.setId(m.group(1));
                theme.setName(m.group(2));
                theme.setBackground(m.group(3));
                themes.add(theme);
            }
        } catch (IOException e) {
            System.err.println("Error parsing Themes JSON: " + e.getMessage());
        }
        return themes;
    }
}
