package kz.arianwait.gametexteditor.search;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import kz.arianwait.gametexteditor.model.EditorChoice;
import kz.arianwait.gametexteditor.model.EditorCommand;
import kz.arianwait.gametexteditor.model.EditorFrame;
import kz.arianwait.gametexteditor.model.EditorScene;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class SearchDialog {

    public record SearchResult(int sceneId, int frameIndex, String context) {}

    public static void show(List<EditorScene> scenes, Stage owner,
                            BiConsumer<Integer, Integer> onNavigate) {
        show(scenes, owner, onNavigate, null);
    }

    public static void show(List<EditorScene> scenes, Stage owner,
                            BiConsumer<Integer, Integer> onNavigate,
                            Runnable onReplaced) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("\u041f\u043e\u0438\u0441\u043a \u0438 \u0437\u0430\u043c\u0435\u043d\u0430");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setResizable(true);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("search-dialog");
        dialogPane.setPrefSize(650, 550);

        // Search field
        TextField searchField = new TextField();
        searchField.setPromptText("\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u0442\u0435\u043a\u0441\u0442 \u0434\u043b\u044f \u043f\u043e\u0438\u0441\u043a\u0430...");
        searchField.getStyleClass().add("search-field");

        // Replace field
        TextField replaceField = new TextField();
        replaceField.setPromptText("\u0417\u0430\u043c\u0435\u043d\u0438\u0442\u044c \u043d\u0430...");
        replaceField.getStyleClass().add("search-field");

        CheckBox caseSensitive = new CheckBox("\u0423\u0447\u0438\u0442\u044b\u0432\u0430\u0442\u044c \u0440\u0435\u0433\u0438\u0441\u0442\u0440");
        caseSensitive.getStyleClass().add("search-checkbox");

        Button searchBtn = new Button("\u0418\u0441\u043a\u0430\u0442\u044c");
        searchBtn.getStyleClass().add("search-btn");

        HBox searchRow = new HBox(8, new Label("\u041d\u0430\u0439\u0442\u0438:"), searchField, searchBtn);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        HBox replaceRow = new HBox(8, new Label("\u0417\u0430\u043c\u0435\u043d\u0430:"), replaceField);
        replaceRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(replaceField, Priority.ALWAYS);

        // Style the labels
        for (var node : searchRow.getChildren()) {
            if (node instanceof Label lbl) {
                lbl.getStyleClass().add("search-results-label");
                lbl.setMinWidth(60);
            }
        }
        for (var node : replaceRow.getChildren()) {
            if (node instanceof Label lbl) {
                lbl.getStyleClass().add("search-results-label");
                lbl.setMinWidth(60);
            }
        }

        // Results
        Label resultsLabel = new Label("\u0420\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u044b:");
        resultsLabel.getStyleClass().add("search-results-label");

        ObservableList<SearchResult> resultItems = FXCollections.observableArrayList();
        ListView<SearchResult> resultList = new ListView<>(resultItems);
        resultList.getStyleClass().add("search-results-list");
        VBox.setVgrow(resultList, Priority.ALWAYS);

        resultList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SearchResult item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String frameStr = item.frameIndex >= 0
                            ? ", \u043a\u0430\u0434\u0440 " + (item.frameIndex + 1)
                            : "";
                    Label loc = new Label("\u0421\u0446\u0435\u043d\u0430 " + item.sceneId + frameStr);
                    loc.getStyleClass().add("search-result-location");

                    Label ctx = new Label("\u2014 \u00ab" + truncate(item.context, 80) + "\u00bb");
                    ctx.getStyleClass().add("search-result-context");

                    HBox row = new HBox(6, loc, ctx);
                    row.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(row);
                    setText(null);
                }
            }
        });

        // Buttons
        Button navBtn = new Button("\u041f\u0435\u0440\u0435\u0439\u0442\u0438");
        navBtn.getStyleClass().add("search-btn");
        navBtn.setDisable(true);

        Button replaceOneBtn = new Button("\u0417\u0430\u043c\u0435\u043d\u0438\u0442\u044c");
        replaceOneBtn.getStyleClass().add("search-btn");
        replaceOneBtn.setDisable(true);

        Button replaceAllBtn = new Button("\u0417\u0430\u043c\u0435\u043d\u0438\u0442\u044c \u0432\u0441\u0451");
        replaceAllBtn.getStyleClass().add("search-btn");
        replaceAllBtn.setDisable(true);

        resultList.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            navBtn.setDisable(nv == null);
            replaceOneBtn.setDisable(nv == null);
        });

        Runnable doNavigate = () -> {
            SearchResult sel = resultList.getSelectionModel().getSelectedItem();
            if (sel != null && onNavigate != null) {
                onNavigate.accept(sel.sceneId, sel.frameIndex);
                dialog.close();
            }
        };

        navBtn.setOnAction(e -> doNavigate.run());
        resultList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) doNavigate.run();
        });

        // Search action
        Runnable doSearch = () -> {
            String query = searchField.getText();
            if (query == null || query.isBlank()) return;
            boolean matchCase = caseSensitive.isSelected();
            List<SearchResult> results = performSearch(scenes, query, matchCase);
            resultItems.setAll(results);
            resultsLabel.setText("\u0420\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442\u044b (" + results.size() + "):");
            replaceAllBtn.setDisable(results.isEmpty());
        };

        searchBtn.setOnAction(e -> doSearch.run());
        searchField.setOnAction(e -> doSearch.run());

        // Replace one
        replaceOneBtn.setOnAction(e -> {
            SearchResult sel = resultList.getSelectionModel().getSelectedItem();
            String query = searchField.getText();
            String replacement = replaceField.getText();
            if (sel == null || query == null || query.isEmpty() || replacement == null) return;
            boolean matchCase = caseSensitive.isSelected();

            replaceInFrame(scenes, sel.sceneId, sel.frameIndex, query, replacement, matchCase);
            if (onReplaced != null) onReplaced.run();

            // Re-search
            doSearch.run();
        });

        // Replace all
        replaceAllBtn.setOnAction(e -> {
            String query = searchField.getText();
            String replacement = replaceField.getText();
            if (query == null || query.isEmpty() || replacement == null) return;
            boolean matchCase = caseSensitive.isSelected();

            int count = replaceAll(scenes, query, replacement, matchCase);
            if (onReplaced != null && count > 0) onReplaced.run();

            // Re-search
            doSearch.run();
            resultsLabel.setText("\u0417\u0430\u043c\u0435\u043d\u0435\u043d\u043e: " + count + " | " + resultsLabel.getText());
        });

        // Layout
        HBox buttonRow = new HBox(8, navBtn, replaceOneBtn, replaceAllBtn);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(8, searchRow, replaceRow, caseSensitive, resultsLabel, resultList, buttonRow);
        content.setPadding(new Insets(12));
        VBox.setVgrow(resultList, Priority.ALWAYS);

        dialogPane.setContent(content);
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    private static void replaceInFrame(List<EditorScene> scenes, int sceneId, int frameIndex,
                                        String query, String replacement, boolean matchCase) {
        for (EditorScene scene : scenes) {
            if (scene.getId() != sceneId) continue;
            if (frameIndex < 0 || frameIndex >= scene.getFrames().size()) break;
            EditorFrame frame = scene.getFrames().get(frameIndex);
            String text = frame.getText();
            if (text != null) {
                frame.setText(replaceText(text, query, replacement, matchCase));
            }
            break;
        }
    }

    private static int replaceAll(List<EditorScene> scenes, String query,
                                   String replacement, boolean matchCase) {
        int count = 0;
        for (EditorScene scene : scenes) {
            for (EditorFrame frame : scene.getFrames()) {
                String text = frame.getText();
                if (text != null && contains(text, query, matchCase)) {
                    frame.setText(replaceText(text, query, replacement, matchCase));
                    count++;
                }
            }
        }
        return count;
    }

    private static String replaceText(String text, String query, String replacement, boolean matchCase) {
        if (matchCase) {
            return text.replace(query, replacement);
        }
        // Case-insensitive replace
        StringBuilder sb = new StringBuilder();
        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();
        int fromIndex = 0;
        int idx;
        while ((idx = lowerText.indexOf(lowerQuery, fromIndex)) >= 0) {
            sb.append(text, fromIndex, idx);
            sb.append(replacement);
            fromIndex = idx + query.length();
        }
        sb.append(text.substring(fromIndex));
        return sb.toString();
    }

    private static List<SearchResult> performSearch(List<EditorScene> scenes,
                                                     String query, boolean matchCase) {
        List<SearchResult> results = new ArrayList<>();
        String q = matchCase ? query : query.toLowerCase();

        for (EditorScene scene : scenes) {
            List<EditorFrame> frames = scene.getFrames();
            for (int i = 0; i < frames.size(); i++) {
                EditorFrame frame = frames.get(i);

                // Search in text
                if (contains(frame.getText(), q, matchCase)) {
                    results.add(new SearchResult(scene.getId(), i, frame.getText()));
                    continue;
                }

                // Search in speaker name
                if (contains(frame.getSpeakerName(), q, matchCase)) {
                    results.add(new SearchResult(scene.getId(), i,
                            "\u0413\u043e\u0432\u043e\u0440\u0438\u0442: " + frame.getSpeakerName()));
                    continue;
                }

                // Search in commands
                boolean foundInCmd = false;
                for (EditorCommand cmd : frame.getCommands()) {
                    if (containsAny(q, matchCase,
                            cmd.getAction(), cmd.getTarget(), cmd.getValue())) {
                        results.add(new SearchResult(scene.getId(), i,
                                "\u041a\u043e\u043c\u0430\u043d\u0434\u0430: " + cmd.getAction()
                                        + " " + nullSafe(cmd.getTarget())));
                        foundInCmd = true;
                        break;
                    }
                }
                if (foundInCmd) continue;

                // Search in choices
                if (frame.getChoices() != null) {
                    for (EditorChoice ch : frame.getChoices()) {
                        if (contains(ch.getText(), q, matchCase)) {
                            results.add(new SearchResult(scene.getId(), i,
                                    "\u0412\u044b\u0431\u043e\u0440: " + ch.getText()));
                            break;
                        }
                    }
                }
            }
        }
        return results;
    }

    private static boolean contains(String text, String query, boolean matchCase) {
        if (text == null || text.isEmpty()) return false;
        return matchCase ? text.contains(query) : text.toLowerCase().contains(query);
    }

    private static boolean containsAny(String query, boolean matchCase, String... values) {
        for (String v : values) {
            if (contains(v, query, matchCase)) return true;
        }
        return false;
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        String clean = s.replace("\n", " ").replace("\r", "");
        return clean.length() > max ? clean.substring(0, max) + "..." : clean;
    }
}
