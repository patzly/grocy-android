/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.util;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import androidx.databinding.BindingAdapter;
import com.google.android.material.color.ColorRoles;
import com.google.android.material.color.HarmonizedColors;
import com.google.android.material.color.HarmonizedColorsOptions;
import com.google.android.material.color.MaterialColors;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.StoredPurchase;

public class ResUtil {

  private final static String TAG = ResUtil.class.getSimpleName();

  @NonNull
  public static String getRawText(Context context, @RawRes int resId) {
    InputStream inputStream = context.getResources().openRawResource(resId);
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    StringBuilder text = new StringBuilder();
    try {
      for (String line; (line = bufferedReader.readLine()) != null; ) {
        text.append(line).append('\n');
      }
      text.deleteCharAt(text.length() - 1);
      inputStream.close();
    } catch (Exception e) {
      Log.e(TAG, "getRawText", e);
    }
    return text.toString();
  }

  public static void share(Context context, @StringRes int resId) {
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.putExtra(Intent.EXTRA_TEXT, context.getString(resId));
    intent.setType("text/plain");
    context.startActivity(Intent.createChooser(intent, null));
  }

  public static void applyColorHarmonization(Context context) {
    int[] resIds = new int[]{
        R.color.logo_yellow,
        R.color.logo_green,
        R.color.logo_red,

        R.color.custom_yellow_80,
        R.color.custom_yellow_50,
        R.color.custom_yellow_30,

        R.color.custom_green_80,
        R.color.custom_green_50,

        R.color.custom_red_60,
        R.color.custom_red_50,
        R.color.custom_red_35,
        R.color.custom_red_30,

        R.color.custom_brown_90,
        R.color.custom_brown_70,
        R.color.custom_brown_50,
        R.color.custom_brown_30,

        R.color.custom_dirt_95,
        R.color.custom_dirt_90,
        R.color.custom_dirt_80,
        R.color.custom_dirt_60,
        R.color.custom_dirt_40,
        R.color.custom_dirt_30,

        R.color.custom_blue_90,
        R.color.custom_blue_70,
        R.color.custom_blue_60,
        R.color.custom_blue_40,
        R.color.custom_blue_10,

        R.color.custom_grey_95,
        R.color.custom_grey_90,
        R.color.custom_grey_80,
        R.color.custom_grey_60,
        R.color.custom_grey_10,
    };
    HarmonizedColorsOptions options = new HarmonizedColorsOptions.Builder()
        .setColorResourceIds(resIds)
        .build();
    HarmonizedColors.applyToContextIfAvailable(context, options);
  }

  public static ColorRoles getHarmonizedRoles(Context context, @ColorRes int resId) {
    return MaterialColors.getColorRoles(
        context,
        MaterialColors.harmonizeWithPrimary(context, ContextCompat.getColor(context, resId))
    );
  }

  public static int getColorAttr(Context context, @AttrRes int resId) {
    TypedValue typedValue = new TypedValue();
    context.getTheme().resolveAttribute(resId, typedValue, true);
    return typedValue.data;
  }

  public static int getColorAttr(Context context, @AttrRes int resId, float alpha) {
    return ColorUtils.setAlphaComponent(getColorAttr(context, resId), (int) (alpha * 255));
  }

  public static int getColorBg(Context context) {
    return getColorAttr(context, android.R.attr.colorBackground);
  }

  public static int getColorOutline(Context context) {
    return getColorAttr(context, R.attr.colorOutline);
  }

  public static int getColorOutlineSecondary(Context context) {
    return getColorAttr(context, R.attr.colorOutline, 0.4f);
  }

  public static int getColorHighlight(Context context) {
    return getColorAttr(context, R.attr.colorSecondary, 0.09f);
  }

  public static void tintMenuItemIcon(Context context, MenuItem item) {
    if (item == null || item.getIcon() == null) {
      return;
    }
    item.getIcon().setTintList(
        ColorStateList.valueOf(ResUtil.getColorAttr(context, R.attr.colorOnSurfaceVariant))
    );
  }

  public static void tintMenuItemIcons(Context context, Menu menu) {
    for (int i = 0; i < menu.size(); i++) {
      MenuItem item = menu.getItem(i);
      if (item == null || item.getIcon() == null) {
        return;
      }
      item.getIcon().setTintList(
          ColorStateList.valueOf(ResUtil.getColorAttr(context, R.attr.colorOnSurfaceVariant))
      );
    }
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

  @BindingAdapter("shoppingCardDrawable")
  public static void setShoppingCardDrawable(
      ImageView view,
      List<StoredPurchase> pendingPurchases
  ) {
    int count = pendingPurchases != null ? pendingPurchases.size() : 0;
    view.setImageDrawable(new BitmapDrawable(
        view.getResources(),
        getFromDrawableWithNumber(
            view.getContext(),
            R.drawable.ic_round_shopping_cart,
            count,
            7.3f,
            -1.5f,
            8
        )
    ));
  }

  public static Bitmap getFromDrawableWithNumber(
      Context context,
      @DrawableRes int resId,
      int number,
      float textSize,
      float textOffsetX,
      float textOffsetY
  ) {
    Bitmap bitmap = getBitmapFromDrawable(context, resId);
    if (bitmap == null) {
      return null;
    }
    // make mutable
    bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

    Canvas canvas = new Canvas(bitmap);

    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setColor(ResUtil.getColorAttr(context, R.attr.colorOnSurfaceVariant));
    paint.setTextSize(UiUtil.dpToPx(context, textSize));
    paint.setTypeface(ResourcesCompat.getFont(context, R.font.material_digits_round));
    paint.setLetterSpacing(0.1f);

    Rect bounds = new Rect();
    paint.getTextBounds(
        String.valueOf(number),
        0,
        String.valueOf(number).length(),
        bounds
    );
    int x = (bitmap.getWidth() - bounds.width()) / 2;
    int y = (bitmap.getHeight() + bounds.height()) / 2;

    canvas.drawText(
        String.valueOf(number),
        x + UiUtil.dpToPx(context, textOffsetX),
        y - UiUtil.dpToPx(context, textOffsetY),
        paint
    );
    return bitmap;
  }
}
