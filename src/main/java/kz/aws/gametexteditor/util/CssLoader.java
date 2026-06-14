package kz.aws.gametexteditor.util;

public final class CssLoader {

    private CssLoader() {}

    public static String getEditorCss() {
        return CssLoader.class.getResource("/kz/aws/gametexteditor/css/editor-dark.css")
                .toExternalForm();
    }
}
