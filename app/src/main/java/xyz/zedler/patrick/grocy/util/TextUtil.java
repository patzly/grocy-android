package xyz.zedler.patrick.grocy.util;

import android.text.Html;

public class TextUtil {

    private static final String TAG = TextUtil.class.getSimpleName();

    private static String trim(String text) {
        if(text != null) {
            String trimmed = text.trim();
            while (trimmed.startsWith("\u00A0")) {
                trimmed = trimmed.substring(1);
            }
            while (trimmed.endsWith("\u00A0")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            text = trimmed.trim();
        }
        return text;
    }

    public static String getFromHtml(String html) {
        return html != null && !html.equals("")
                ? trim(Html.fromHtml(html).toString().trim())
                : null;
    }
}
