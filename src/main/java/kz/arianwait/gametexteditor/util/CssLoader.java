package kz.arianwait.gametexteditor.util;

public final class CssLoader {

    private CssLoader() {}

    public static String getEditorCss() {
        return CssLoader.class.getResource("/kz/arianwait/gametexteditor/css/editor-dark.css")
                .toExternalForm();
    }
}
