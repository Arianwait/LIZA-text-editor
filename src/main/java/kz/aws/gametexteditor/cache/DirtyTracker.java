package kz.aws.gametexteditor.cache;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class DirtyTracker {

    private final BooleanProperty dirty = new SimpleBooleanProperty(false);

    public void markDirty() {
        dirty.set(true);
    }

    public void markClean() {
        dirty.set(false);
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public BooleanProperty dirtyProperty() {
        return dirty;
    }
}
