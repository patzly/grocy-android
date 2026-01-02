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
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import androidx.core.content.ContextCompat;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.Userfield;

public class ChipUtil {

  private final Context context;
  private final int colorGreenContainer, colorOnGreenContainer;
  private final int colorYellowContainer, colorOnYellowContainer;

  public ChipUtil(Context context) {
    this.context = context;
    colorGreenContainer = ResUtil.getColor(context, R.attr.colorCustomGreenContainer);
    colorOnGreenContainer = ResUtil.getColor(context, R.attr.colorOnCustomGreenContainer);
    colorYellowContainer = ResUtil.getColor(context, R.attr.colorCustomYellowContainer);
    colorOnYellowContainer = ResUtil.getColor(context, R.attr.colorOnCustomYellowContainer);
  }

  private static Chip createChip(Context ctx, String text) {
    @SuppressLint("InflateParams")
    Chip chip = (Chip) LayoutInflater.from(ctx).inflate(
        R.layout.view_info_chip, null, false
    );
    chip.setText(text);
    return chip;
  }

  public Chip createTextChip(String text) {
    return createChip(context, text);
  }

  public Chip createTextChip(String text, String textOnClick) {
    Chip chip = createChip(context, text);
    chip.setOnClickListener(v -> {
      new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Grocy_AlertDialog)
          .setMessage(textOnClick)
          .setPositiveButton(R.string.action_close, (dialog, which) -> dialog.dismiss())
          .create().show();
    });
    return chip;
  }

  public Chip createRecipeFulfillmentChip(RecipeFulfillment recipeFulfillment, boolean showYellow) {
    Chip chipFulfillment;
    String textFulfillment;
    if (recipeFulfillment.isNeedFulfilled()) {
      textFulfillment = context.getString(R.string.msg_recipes_enough_in_stock);
      chipFulfillment = createChip(context, context.getString(R.string.property_status_insert));
      chipFulfillment.setTextColor(colorOnGreenContainer);
      chipFulfillment.setCloseIcon(
          ContextCompat.getDrawable(context, R.drawable.ic_round_check_circle_outline)
      );
      chipFulfillment.setCloseIconTint(ColorStateList.valueOf(colorOnGreenContainer));
      chipFulfillment.setChipBackgroundColor(ColorStateList.valueOf(colorGreenContainer));
    } else if (recipeFulfillment.isNeedFulfilledWithShoppingList() && showYellow) {
      textFulfillment = context.getString(R.string.msg_recipes_not_enough) + "\n"
          + context.getResources()
          .getQuantityString(R.plurals.msg_recipes_ingredients_missing_but_on_shopping_list,
              recipeFulfillment.getMissingProductsCount(),
              recipeFulfillment.getMissingProductsCount());
      chipFulfillment = createChip(context, context.getString(R.string.property_status_insert));
      chipFulfillment.setTextColor(colorOnYellowContainer);
      chipFulfillment.setCloseIcon(
          ContextCompat.getDrawable(context, R.drawable.ic_round_error_outline)
      );
      chipFulfillment.setCloseIconTint(ColorStateList.valueOf(colorOnYellowContainer));
      chipFulfillment.setChipBackgroundColor(ColorStateList.valueOf(colorYellowContainer));
    } else {
      textFulfillment = context.getString(R.string.msg_recipes_not_enough) + "\n"
          + context.getResources()
          .getQuantityString(R.plurals.msg_recipes_ingredients_missing,
              recipeFulfillment.getMissingProductsCount(),
              recipeFulfillment.getMissingProductsCount());
      int colorOnErrorContainer = ResUtil.getColor(context, R.attr.colorOnErrorContainer);
      chipFulfillment = createChip(context, context.getString(R.string.property_status_insert));
      chipFulfillment.setTextColor(colorOnErrorContainer);
      chipFulfillment.setCloseIcon(
          ContextCompat.getDrawable(context, R.drawable.ic_round_highlight_off)
      );
      chipFulfillment.setCloseIconTint(ColorStateList.valueOf(colorOnErrorContainer));
      chipFulfillment.setChipBackgroundColor(ColorStateList.valueOf(
          ResUtil.getColor(context, R.attr.colorErrorContainer)
      ));
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

  public Chip createRecipeFulfillmentChip(RecipeFulfillment recipeFulfillment) {
    return createRecipeFulfillmentChip(recipeFulfillment, true);
  }

  public Chip createProductFulfillmentChip(boolean needFulfilled) {
    Chip chipFulfillment;
    String textFulfillment;
    if (needFulfilled) {
      textFulfillment = context.getString(R.string.msg_recipes_enough_in_stock);
      chipFulfillment = createChip(context, context.getString(R.string.property_status_insert));
      chipFulfillment.setTextColor(colorOnGreenContainer);
      chipFulfillment.setCloseIcon(
          ContextCompat.getDrawable(context, R.drawable.ic_round_check_circle_outline)
      );
      chipFulfillment.setCloseIconTint(ColorStateList.valueOf(colorOnGreenContainer));
      chipFulfillment.setChipBackgroundColor(ColorStateList.valueOf(colorGreenContainer));
    } else {
      textFulfillment = context.getString(R.string.msg_recipes_not_enough);
      int colorOnErrorContainer = ResUtil.getColor(context, R.attr.colorOnErrorContainer);
      chipFulfillment = createChip(context, context.getString(R.string.property_status_insert));
      chipFulfillment.setTextColor(colorOnErrorContainer);
      chipFulfillment.setCloseIcon(
          ContextCompat.getDrawable(context, R.drawable.ic_round_highlight_off)
      );
      chipFulfillment.setCloseIconTint(ColorStateList.valueOf(colorOnErrorContainer));
      chipFulfillment.setChipBackgroundColor(ColorStateList.valueOf(
          ResUtil.getColor(context, R.attr.colorErrorContainer)
      ));
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

  public Chip createRecipeDueScoreChip(int dueScore) {
    Chip dueScoreChip;
    if (dueScore == 0) {
      dueScoreChip = createChip(context, context.getString(
          R.string.subtitle_recipe_due_score,
          String.valueOf(dueScore)
      ));
    } else if (dueScore <= 10) {
      dueScoreChip = createChip(context, context.getString(
          R.string.subtitle_recipe_due_score,
          String.valueOf(dueScore)
      ));
      dueScoreChip.setTextColor(colorOnYellowContainer);
      dueScoreChip.setChipBackgroundColor(ColorStateList.valueOf(colorYellowContainer));
    } else {
      dueScoreChip = createChip(
          context, context.getString(R.string.subtitle_recipe_due_score, String.valueOf(dueScore))
      );
      dueScoreChip.setTextColor(ResUtil.getColor(context, R.attr.colorOnErrorContainer));
      dueScoreChip.setChipBackgroundColor(ColorStateList.valueOf(
          ResUtil.getColor(context, R.attr.colorErrorContainer)
      ));
    }
    dueScoreChip.setEnabled(false);
    dueScoreChip.setClickable(false);
    dueScoreChip.setFocusable(false);
    return dueScoreChip;
  }

  public Chip createUserfieldChip(Userfield userfield, String value) {
    Chip chipUserfield = createTextChip(null);
    return Userfield.fillChipWithUserfield(chipUserfield, userfield, value);
  }
}
