/*
 * This file is part of Doodle Android.
 *
 * Doodle Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Doodle Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Doodle Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2022 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.util.Log;
import android.view.View;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import xyz.zedler.patrick.grocy.R;

public class ResUtil {

  private final static String TAG = ResUtil.class.getSimpleName();

  @NonNull
  public static String getRawText(Context context, @RawRes int resId) {
    InputStream inputStream = context.getResources().openRawResource(resId);
    BufferedReader bufferedReader= new BufferedReader(new InputStreamReader(inputStream));
    StringBuilder text = new StringBuilder();
    try {
      for (String line; (line = bufferedReader.readLine()) != null; ) {
        text.append(line).append('\n');
      }
      text.deleteCharAt(text.length() - 1);
      inputStream.close();
    } catch (Exception e) {
      Log.e(TAG, "getRawText: ", e);
    }
    return text.toString();
  }

  public static void share(Context context, @StringRes int resId) {
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.putExtra(Intent.EXTRA_TEXT, context.getString(resId));
    intent.setType("text/plain");
    context.startActivity(Intent.createChooser(intent, null));
  }

  public static CharSequence getBulletList(
      Context context, String prefixToReplace, @Nullable String text, String... highlights
  ) {
    if (context == null || text == null) {
      return null;
    }

    // BulletSpan doesn't support RTL, use original text instead
    int direction = context.getResources().getConfiguration().getLayoutDirection();
    if (direction == View.LAYOUT_DIRECTION_RTL) {
      String formatted = text;
      for (String highlight : highlights) {
        formatted = formatted.replaceAll(highlight, "<b>" + highlight + "</b>");
        formatted = formatted.replaceAll("\n", "<br/>");
      }
      return Html.fromHtml(formatted);
    }

    int color = ContextCompat.getColor(context, R.color.on_background);
    int margin = UnitUtil.spToPx(context, 6);

    String[] lines = text.split("\n");
    SpannableStringBuilder builder = new SpannableStringBuilder();
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i] + (i < lines.length - 1 ? "\n" : "");
      if (!line.startsWith(prefixToReplace)) {
        builder.append(Html.fromHtml(line.replaceAll("\n", "<br/>")));
        continue;
      }
      line = line.substring(prefixToReplace.length());

      BulletSpan bulletSpan;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        bulletSpan = new BulletSpan(margin, color, UnitUtil.spToPx(context, 2));
      } else {
        bulletSpan = new BulletSpan(margin, color);
      }

      for (String highlight : highlights) {
        line = line.replaceAll(highlight, "<b>" + highlight + "</b>");
        line = line.replaceAll("\n", "<br/>");
      }

      Spannable spannable = new SpannableString(Html.fromHtml(line));
      spannable.setSpan(bulletSpan, 0, spannable.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
      builder.append(spannable);
    }
    return builder;
  }

  public static Bitmap getBitmapFromDrawable(Context context, @DrawableRes int resId) {
    Drawable drawable = ResourcesCompat.getDrawable(context.getResources(), resId, null);
    if (drawable != null) {
      if (drawable instanceof BitmapDrawable) {
        return ((BitmapDrawable) drawable).getBitmap();
      }
      Bitmap bitmap = Bitmap.createBitmap(
          drawable.getIntrinsicWidth(),
          drawable.getIntrinsicHeight(),
          Bitmap.Config.ARGB_8888
      );
      Canvas canvas = new Canvas(bitmap);
      drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
      drawable.draw(canvas);
      return bitmap;
    }
    return null;
  }
}
