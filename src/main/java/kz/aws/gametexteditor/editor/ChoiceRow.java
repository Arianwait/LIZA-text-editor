package kz.aws.gametexteditor.editor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import kz.aws.gametexteditor.model.EditorChoice;

public class ChoiceRow extends VBox {

    private final EditorChoice choice;
    private boolean expanded = false;
    private final VBox detailsBox;
    private Runnable onChanged;
    private Runnable onRemove;

    public ChoiceRow(EditorChoice choice) {
        this.choice = choice;
        setSpacing(4);
        setPadding(new Insets(4, 8, 4, 8));
        getStyleClass().add("choice-row");

        // Compact header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label arrow = new Label("\u25b6");
        arrow.getStyleClass().add("choice-row-arrow");

        TextField textField = new TextField(choice.getText());
        textField.setPromptText("\u0422\u0435\u043a\u0441\u0442 \u0432\u044b\u0431\u043e\u0440\u0430...");
        textField.getStyleClass().add("choice-row-text");
        HBox.setHgrow(textField, Priority.ALWAYS);
        textField.textProperty().addListener((o, ov, nv) -> {
            choice.setText(nv);
            fireChanged();
        });

        Label targetLabel = new Label("\u2192");
        targetLabel.getStyleClass().add("choice-row-arrow-label");

        Spinner<Integer> targetSpinner = new Spinner<>(0, 9999, choice.getRequestsId());
        targetSpinner.setEditable(true);
        targetSpinner.setPrefWidth(80);
        targetSpinner.getStyleClass().add("choice-row-spinner");
        targetSpinner.valueProperty().addListener((o, ov, nv) -> {
            choice.setRequestsId(nv);
            fireChanged();
        });

        Button removeBtn = new Button("\u00d7");
        removeBtn.getStyleClass().add("choice-row-remove");
        removeBtn.setOnAction(e -> {
            if (onRemove != null) onRemove.run();
        });

        header.getChildren().addAll(arrow, textField, targetLabel, targetSpinner, removeBtn);

        // Expandable details
        detailsBox = new VBox(4);
        detailsBox.setPadding(new Insets(4, 0, 0, 20));
        detailsBox.setVisible(false);
        detailsBox.setManaged(false);
        detailsBox.getStyleClass().add("choice-row-details");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(4);

        addField(grid, 0, "ID:", choice.getId(), v -> choice.setId(v));
        addField(grid, 1, "\u041e\u043f\u0438\u0441\u0430\u043d\u0438\u0435:", choice.getDescription(), v -> choice.setDescription(v));
        addField(grid, 2, "\u041f\u0435\u0440\u0441\u043e\u043d\u0430\u0436:", choice.getCharacterChoice(), v -> choice.setCharacterChoice(v));
        addField(grid, 3, "addChoice:", choice.getAddChoice(), v -> choice.setAddChoice(v));
        addField(grid, 4, "removeChoice:", choice.getRemoveChoice(), v -> choice.setRemoveChoice(v));

        HBox repRow = new HBox(8);
        repRow.setAlignment(Pos.CENTER_LEFT);
        repRow.getChildren().addAll(
                new Label("minRep:"),
                createIntSpinner(choice.getMinRep(), v -> choice.setMinRep(v)),
                new Label("maxRep:"),
                createIntSpinner(choice.getMaxRep(), v -> choice.setMaxRep(v))
        );
        repRow.getChildren().forEach(n -> {
            if (n instanceof Label l) l.getStyleClass().add("choice-detail-label");
        });

        detailsBox.getChildren().addAll(grid, repRow);

        // Toggle expansion
        header.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof TextField || e.getTarget() instanceof Spinner) return;
            expanded = !expanded;
            detailsBox.setVisible(expanded);
            detailsBox.setManaged(expanded);
            arrow.setText(expanded ? "\u25bc" : "\u25b6");
        });

        getChildren().addAll(header, detailsBox);
    }

    private void addField(GridPane grid, int row, String labelText, String value,
                          java.util.function.Consumer<String> setter) {
        Label label = new Label(labelText);
        label.getStyleClass().add("choice-detail-label");
        TextField field = new TextField(value != null ? value : "");
        field.getStyleClass().add("choice-detail-field");
        field.textProperty().addListener((o, ov, nv) -> {
            setter.accept(nv);
            fireChanged();
        });
        grid.add(label, 0, row);
        grid.add(field, 1, row);
    }

    private Spinner<Integer> createIntSpinner(int value, java.util.function.IntConsumer setter) {
        Spinner<Integer> spinner = new Spinner<>(0, 100, value);
        spinner.setEditable(true);
        spinner.setPrefWidth(70);
        spinner.getStyleClass().add("choice-detail-spinner");
        spinner.valueProperty().addListener((o, ov, nv) -> {
            setter.accept(nv);
            fireChanged();
        });
        return spinner;
    }

    private void fireChanged() {
        if (onChanged != null) onChanged.run();
    }

    public EditorChoice getChoice() { return choice; }
    public void setOnChanged(Runnable handler) { this.onChanged = handler; }
    public void setOnRemove(Runnable handler) { this.onRemove = handler; }
}
