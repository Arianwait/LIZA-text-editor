package kz.arianwait.gametexteditor.editor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import kz.arianwait.gametexteditor.model.EditorCommand;

public class CommandBadge extends HBox {

    private final EditorCommand command;
    private Runnable onRemove;
    private Runnable onEdit;

    public CommandBadge(EditorCommand command) {
        this.command = command;
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(4);
        setPadding(new Insets(2, 6, 2, 6));
        getStyleClass().add("command-badge");

        String type = command.getType() != null ? command.getType() : "";
        getStyleClass().add("command-badge-" + type);

        Label label = new Label(formatCommand(command));
        label.getStyleClass().add("command-badge-text");

        Button removeBtn = new Button("\u00d7");
        removeBtn.getStyleClass().add("command-badge-remove");
        removeBtn.setOnAction(e -> {
            if (onRemove != null) onRemove.run();
        });

        getChildren().addAll(label, removeBtn);

        Tooltip.install(this, new Tooltip(formatTooltip(command)));

        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && onEdit != null) {
                onEdit.run();
                e.consume();
            }
        });
    }

    private String formatCommand(EditorCommand cmd) {
        String type = cmd.getType() != null ? cmd.getType() : "";
        return switch (type) {
            case "character" -> {
                String action = cmd.getAction() != null ? cmd.getAction() : "";
                if ("showPerson".equals(action)) {
                    yield cmd.getTarget() + "/" + cmd.getValue();
                }
                yield action + ": " + cmd.getTarget();
            }
            case "background" -> "\u0424\u043e\u043d: " + shortenToName(cmd.getValue());
            case "input" -> "\u0412\u0432\u043e\u0434: " + cmd.getKey();
            case "panel" -> "\u041f\u0430\u043d\u0435\u043b\u044c: " + cmd.getId();
            case "effect" -> formatEffectBadge(cmd);
            default -> type + ": " + cmd.getAction();
        };
    }

    private String formatTooltip(EditorCommand cmd) {
        StringBuilder sb = new StringBuilder();
        sb.append("\u0422\u0438\u043f: ").append(cmd.getType());
        if (cmd.getAction() != null && !cmd.getAction().isEmpty())
            sb.append("\n\u0414\u0435\u0439\u0441\u0442\u0432\u0438\u0435: ").append(cmd.getAction());
        if (cmd.getTarget() != null && !cmd.getTarget().isEmpty())
            sb.append("\n\u0426\u0435\u043b\u044c: ").append(cmd.getTarget());
        if (cmd.getValue() != null && !cmd.getValue().isEmpty())
            sb.append("\n\u0417\u043d\u0430\u0447\u0435\u043d\u0438\u0435: ").append(cmd.getValue());
        if (cmd.getKey() != null && !cmd.getKey().isEmpty())
            sb.append("\n\u041a\u043b\u044e\u0447: ").append(cmd.getKey());
        if (cmd.getPrompt() != null && !cmd.getPrompt().isEmpty())
            sb.append("\n\u041f\u043e\u0434\u0441\u043a\u0430\u0437\u043a\u0430: ").append(cmd.getPrompt());
        if (cmd.getId() != null && !cmd.getId().isEmpty())
            sb.append("\nID: ").append(cmd.getId());
        if (cmd.getEffect() != null && !cmd.getEffect().isEmpty())
            sb.append("\n\u042d\u0444\u0444\u0435\u043a\u0442: ").append(cmd.getEffect());
        if (cmd.getDuration() > 0)
            sb.append("\n\u0414\u043b\u0438\u0442.: ").append(cmd.getDuration()).append("\u043c\u0441");
        if (cmd.getFilter() != null && !cmd.getFilter().isEmpty())
            sb.append("\n\u0424\u0438\u043b\u044c\u0442\u0440: ").append(cmd.getFilter());
        if (cmd.getIntensity() >= 0)
            sb.append("\n\u0418\u043d\u0442\u0435\u043d\u0441.: ").append(cmd.getIntensity());
        if (cmd.getOnSuccess() >= 0)
            sb.append("\n\u0423\u0441\u043f\u0435\u0445 \u2192 ").append(cmd.getOnSuccess());
        if (cmd.getOnFailure() >= 0)
            sb.append("\n\u041d\u0435\u0443\u0434\u0430\u0447\u0430 \u2192 ").append(cmd.getOnFailure());
        return sb.toString();
    }

    /**
     * Formats a short label for a visual effect command badge.
     *
     * @param cmd the effect command
     * @return human-readable short label like "VFX: blink ▶" or "VFX: blink ■"
     */
    private String formatEffectBadge(EditorCommand cmd) {
        String effect = cmd.getEffect() != null ? cmd.getEffect() : "?";
        boolean isStop = "stop".equals(cmd.getAction());
        return "VFX: " + effect + (isStop ? " \u25a0" : " \u25b6");
    }

    /**
     * Extracts the filename without extension from a relative path,
     * used as a short display name in badges.
     *
     * @param path relative resource path (e.g. "lib/Scene/backgrounds/city.png")
     * @return filename without extension (e.g. "city")
     */
    private String shortenToName(String path) {
        if (path == null || path.isEmpty()) return "";
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    public EditorCommand getCommand() { return command; }
    public void setOnRemove(Runnable handler) { this.onRemove = handler; }
    public void setOnEdit(Runnable handler) { this.onEdit = handler; }
}
