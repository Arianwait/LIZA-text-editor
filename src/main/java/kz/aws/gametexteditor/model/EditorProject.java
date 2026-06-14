package kz.aws.gametexteditor.model;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EditorProject implements Serializable {

    private static final long serialVersionUID = 1L;

    private final File projectDir;
    private final List<EditorScene> scenes;
    private final List<EditorCharacter> characters;
    private final List<SceneEdge> edges;
    private final List<EditorChapter> chapters;
    private final List<EditorTheme> themes;
    private final List<EditorButton> buttons;
    private final List<EditorVariable> variables;

    public EditorProject(File projectDir, List<EditorScene> scenes,
                         List<EditorCharacter> characters, List<SceneEdge> edges,
                         List<EditorChapter> chapters, List<EditorTheme> themes,
                         List<EditorButton> buttons) {
        this(projectDir, scenes, characters, edges, chapters, themes, buttons, null);
    }

    public EditorProject(File projectDir, List<EditorScene> scenes,
                         List<EditorCharacter> characters, List<SceneEdge> edges,
                         List<EditorChapter> chapters, List<EditorTheme> themes,
                         List<EditorButton> buttons, List<EditorVariable> variables) {
        this.projectDir = projectDir;
        this.scenes = scenes;
        this.characters = characters;
        this.edges = edges;
        this.chapters = chapters != null ? chapters : new ArrayList<>();
        this.themes = themes != null ? themes : new ArrayList<>();
        this.buttons = buttons != null ? buttons : new ArrayList<>();
        this.variables = variables != null ? variables : new ArrayList<>();
    }

    public File getProjectDir() { return projectDir; }
    public List<EditorScene> getScenes() { return scenes; }
    public List<EditorCharacter> getCharacters() { return characters; }
    public List<SceneEdge> getEdges() { return edges; }

    public void rebuildEdges(List<SceneEdge> newEdges) {
        this.edges.clear();
        this.edges.addAll(newEdges);
    }
    public List<EditorChapter> getChapters() { return chapters; }
    public List<EditorTheme> getThemes() { return themes; }
    public List<EditorButton> getButtons() { return buttons; }
    public List<EditorVariable> getVariables() { return variables; }

    public int getSceneCount() { return scenes.size(); }
    public int getCharacterCount() { return characters.size(); }
    public int getChapterCount() { return chapters.size(); }

    public long getBranchCount() {
        return scenes.stream().filter(EditorScene::hasChoices).count();
    }

    public long getEndCount() {
        return scenes.stream().filter(EditorScene::isEndScene).count();
    }

    public int getTotalFrameCount() {
        return scenes.stream().mapToInt(EditorScene::getFrameCount).sum();
    }
}
