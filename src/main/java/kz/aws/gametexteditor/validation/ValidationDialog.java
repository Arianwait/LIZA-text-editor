package kz.aws.gametexteditor.validation;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.BiConsumer;

public class ValidationDialog {

    public static void show(List<ProjectValidator.Issue> issues, Stage owner,
                            BiConsumer<Integer, Integer> onNavigate) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("\u0412\u0430\u043b\u0438\u0434\u0430\u0446\u0438\u044f \u043f\u0440\u043e\u0435\u043a\u0442\u0430");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setResizable(true);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("validation-dialog");
        dialogPane.setPrefSize(650, 500);

        // Summary
        long errors = issues.stream()
                .filter(i -> i.severity() == ProjectValidator.Severity.ERROR).count();
        long warnings = issues.stream()
                .filter(i -> i.severity() == ProjectValidator.Severity.WARNING).count();

        String summaryText;
        if (issues.isEmpty()) {
            summaryText = "\u2714 \u041f\u0440\u043e\u0435\u043a\u0442 \u0432 \u043f\u043e\u0440\u044f\u0434\u043a\u0435 \u2014 \u043f\u0440\u043e\u0431\u043b\u0435\u043c \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e";
        } else {
            summaryText = "\u041f\u0440\u043e\u0432\u0435\u0440\u0435\u043d\u043e | "
                    + errors + " \u043e\u0448\u0438\u0431\u043e\u043a, "
                    + warnings + " \u043f\u0440\u0435\u0434\u0443\u043f\u0440\u0435\u0436\u0434\u0435\u043d\u0438\u0439";
        }

        Label summaryLabel = new Label(summaryText);
        summaryLabel.getStyleClass().add("validation-summary");

        // Issues list
        ObservableList<ProjectValidator.Issue> issueItems =
                FXCollections.observableArrayList(issues);
        ListView<ProjectValidator.Issue> issueList = new ListView<>(issueItems);
        issueList.getStyleClass().add("validation-list");
        VBox.setVgrow(issueList, Priority.ALWAYS);

        issueList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ProjectValidator.Issue item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String icon = item.severity() == ProjectValidator.Severity.ERROR
                            ? "\u274c" : "\u26a0";
                    Label iconLabel = new Label(icon);
                    iconLabel.getStyleClass().add("validation-icon");
                    iconLabel.setMinWidth(20);

                    Label msgLabel = new Label(item.message());
                    msgLabel.getStyleClass().add(
                            item.severity() == ProjectValidator.Severity.ERROR
                                    ? "validation-error-text"
                                    : "validation-warning-text");
                    msgLabel.setWrapText(true);

                    HBox row = new HBox(6, iconLabel, msgLabel);
                    row.setAlignment(Pos.CENTER_LEFT);
                    HBox.setHgrow(msgLabel, Priority.ALWAYS);
                    setGraphic(row);
                    setText(null);
                }
            }
        });

        // Navigate button
        Button navBtn = new Button("\u041f\u0435\u0440\u0435\u0439\u0442\u0438");
        navBtn.getStyleClass().add("search-btn");
        navBtn.setDisable(true);

        issueList.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) ->
                navBtn.setDisable(nv == null));

        Runnable doNavigate = () -> {
            ProjectValidator.Issue sel = issueList.getSelectionModel().getSelectedItem();
            if (sel != null && onNavigate != null) {
                onNavigate.accept(sel.sceneId(), sel.frameIndex());
                dialog.close();
            }
        };

        navBtn.setOnAction(e -> doNavigate.run());
        issueList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) doNavigate.run();
        });

        // Layout
        VBox content = new VBox(8, summaryLabel, issueList, navBtn);
        content.setPadding(new Insets(12));

        dialogPane.setContent(content);
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }
}
