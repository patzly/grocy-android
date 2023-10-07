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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import androidx.core.content.ContextCompat;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.ColorRoles;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.elevation.SurfaceColors;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;

public class ChipUtil {

  private final Context context;
  private final ColorRoles colorBlue;
  private final ColorRoles colorGreen;
  private final ColorRoles colorYellow;
  private final ColorRoles colorRed;

  public ChipUtil(Context context) {
    this.context = context;
    colorBlue = ResUtil.getHarmonizedRoles(context, R.color.blue);
    colorGreen = ResUtil.getHarmonizedRoles(context, R.color.green);
    colorYellow = ResUtil.getHarmonizedRoles(context, R.color.yellow);
    colorRed = ResUtil.getHarmonizedRoles(context, R.color.red);
  }

  private static Chip createChip(Context ctx, String text, int textColor) {
    @SuppressLint("InflateParams")
    Chip chip = (Chip) LayoutInflater.from(ctx)
        .inflate(R.layout.view_info_chip, null, false);
    chip.setChipBackgroundColor(ColorStateList.valueOf(SurfaceColors.SURFACE_4.getColor(ctx)));
    chip.setText(text);
    if (textColor != -1) {
      chip.setTextColor(textColor);
    }
    return chip;
  }

  public Chip createTextChip(String text) {
    return createChip(context, text, -1);
  }

  public Chip createRecipeFulfillmentChip(RecipeFulfillment recipeFulfillment) {
    Chip chipFulfillment;
    String textFulfillment;
    if (recipeFulfillment.isNeedFulfilled()) {
      textFulfillment = context.getString(R.string.msg_recipes_enough_in_stock);
      chipFulfillment = createChip(context, context.getString(R.string.property_status_insert), colorGreen.getOnAccentContainer());
      chipFulfillment.setCloseIcon(
          ContextCompat.getDrawable(context, R.drawable.ic_round_check_circle_outline)
      );
      chipFulfillment.setCloseIconTint(ColorStateList.valueOf(colorGreen.getOnAccentContainer()));
      chipFulfillment.setChipBackgroundColor(ColorStateList.valueOf(colorGreen.getAccentContainer()));
    } else if (recipeFulfillment.isNeedFulfilledWithShoppingList()) {
      textFulfillment = context.getString(R.string.msg_recipes_not_enough) + "\n"
          + context.getResources()
          .getQuantityString(R.plurals.msg_recipes_ingredients_missing_but_on_shopping_list,
              recipeFulfillment.getMissingProductsCount(),
              recipeFulfillment.getMissingProductsCount());
      chipFulfillment = createChip(context, context.getString(R.string.property_status_insert), colorYellow.getOnAccentContainer());
      chipFulfillment.setCloseIcon(
          ContextCompat.getDrawable(context, R.drawable.ic_round_error_outline)
      );
      chipFulfillment.setCloseIconTint(ColorStateList.valueOf(colorYellow.getOnAccentContainer()));
      chipFulfillment.setChipBackgroundColor(ColorStateList.valueOf(colorYellow.getAccentContainer()));
    } else {
      textFulfillment = context.getString(R.string.msg_recipes_not_enough) + "\n"
          + context.getResources()
          .getQuantityString(R.plurals.msg_recipes_ingredients_missing,
              recipeFulfillment.getMissingProductsCount(),
              recipeFulfillment.getMissingProductsCount());
      chipFulfillment = createChip(context, context.getString(R.string.property_status_insert), colorRed.getOnAccentContainer());
      chipFulfillment.setCloseIcon(
          ContextCompat.getDrawable(context, R.drawable.ic_round_highlight_off)
      );
      chipFulfillment.setCloseIconTint(ColorStateList.valueOf(colorRed.getOnAccentContainer()));
      chipFulfillment.setChipBackgroundColor(ColorStateList.valueOf(colorRed.getAccentContainer()));
    }
    chipFulfillment.setCloseIconStartPadding(UiUtil.dpToPx(context, 4));
    chipFulfillment.setCloseIconVisible(true);
    String finalTextFulfillment = textFulfillment;
    chipFulfillment.setOnClickListener(v -> {
      new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Grocy_AlertDialog)
          .setTitle(R.string.property_requirements_fulfilled)
          .setMessage(finalTextFulfillment)
          .setPositiveButton(R.string.action_close, (dialog, which) -> dialog.dismiss())
          .create().show();
    });
    return chipFulfillment;
  }
}
