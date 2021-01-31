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

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;

import androidx.core.content.ContextCompat;

import java.util.List;

import xyz.zedler.patrick.grocy.R;

public class BulletUtil {

    public static CharSequence makeBulletList(
            Context context,
            float leadingMargin,
            float bulletSize,
            String prefixToReplace,
            String text,
            List<String> highlights
    ) {
        int color = ContextCompat.getColor(context, R.color.on_background);
        int margin = UnitUtil.getSp(context, leadingMargin);
        int size = UnitUtil.getSp(context, bulletSize);

        String[] lines = text.split("\n");
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i] + (i < lines.length - 1 ? "\n" : "");
            if (!line.startsWith(prefixToReplace)) {
                builder.append(line);
                continue;
            }
            line = line.substring(prefixToReplace.length());

            BulletSpan bulletSpan;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                bulletSpan = new BulletSpan(margin, color, size);
            } else {
                bulletSpan = new BulletSpan(margin, color);
            }

            for(String highlight : highlights) {
                line = line.replaceAll(highlight, "<b>" + highlight + "</b>");
                line = line.replaceAll("\n", "<br/>");
            }

            Spannable spannable = new SpannableString(Html.fromHtml(line));
            spannable.setSpan(
                    bulletSpan,
                    0,
                    spannable.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            );
            builder.append(spannable);
        }
        return builder;
    }

}
