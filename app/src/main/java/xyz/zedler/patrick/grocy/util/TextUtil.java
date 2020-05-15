package xyz.zedler.patrick.grocy.util;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

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
        return html != null && !html.isEmpty()
                ? trim(Html.fromHtml(html).toString().trim())
                : null;
    }
}
