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
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.model;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class ChipData {
  public final static int COLOR_ROLES_BLUE = 0;
  public final static int COLOR_ROLES_GREEN = 1;
  public final static int COLOR_ROLES_YELLOW = 2;
  public final static int COLOR_ROLES_RED = 3;

  private String text;
  private int textColor;
  private int chipBackgroundColor;
  private View.OnClickListener onClickListener;
  private Drawable chipIcon;
  private int chipIconTint = -1;
  private int textStartPadding = -1;
  private Drawable closeIcon;
  private int closeIconTint = -1;
  private int closeIconStartPadding = -1;
  private boolean closeIconVisible = false;
  private boolean enabled = true;

  public ChipData(String text, int textColor, int chipBackgroundColor, View.OnClickListener onClickListener) {
    this.text = text;
    this.textColor = textColor;
    this.chipBackgroundColor = chipBackgroundColor;
    this.onClickListener = onClickListener;
  }

  public ChipData(String text, int textColor) {
    this.text = text;
    this.textColor = textColor;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public int getTextColor() {
    return textColor;
  }

  public void setTextColor(int textColor) {
    this.textColor = textColor;
  }

  public int getChipBackgroundColor() {
    return chipBackgroundColor;
  }

  public void setChipBackgroundColor(int chipBackgroundColor) {
    this.chipBackgroundColor = chipBackgroundColor;
  }

  public View.OnClickListener getOnClickListener() {
    return onClickListener;
  }

  public void setOnClickListener(View.OnClickListener onClickListener) {
    this.onClickListener = onClickListener;
  }

  public Drawable getChipIcon() {
    return chipIcon;
  }

  public void setChipIcon(Drawable chipIcon) {
    this.chipIcon = chipIcon;
  }

  public int getChipIconTint() {
    return chipIconTint;
  }

  public void setChipIconTint(int chipIconTint) {
    this.chipIconTint = chipIconTint;
  }

  public int getTextStartPadding() {
    return textStartPadding;
  }

  public void setTextStartPadding(int textStartPadding) {
    this.textStartPadding = textStartPadding;
  }

  public Drawable getCloseIcon() {
    return closeIcon;
  }

  public void setCloseIcon(Drawable closeIcon) {
    this.closeIcon = closeIcon;
  }

  public int getCloseIconTint() {
    return closeIconTint;
  }

  public void setCloseIconTint(int closeIconTint) {
    this.closeIconTint = closeIconTint;
  }

  public int getCloseIconStartPadding() {
    return closeIconStartPadding;
  }

  public void setCloseIconStartPadding(int closeIconStartPadding) {
    this.closeIconStartPadding = closeIconStartPadding;
  }

  public boolean isCloseIconVisible() {
    return closeIconVisible;
  }

  public void setCloseIconVisible(boolean closeIconVisible) {
    this.closeIconVisible = closeIconVisible;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public static ChipData getTextChipData(String text, @Nullable String textOnClick) {
    return new ChipData(text, -1, -1, textOnClick != null ?
        v -> new MaterialAlertDialogBuilder(v.getContext(), R.style.ThemeOverlay_Grocy_AlertDialog)
        .setMessage(textOnClick)
        .setPositiveButton(R.string.action_close, (dialog, which) -> dialog.dismiss())
        .create().show() : null);
  }

  public static ChipData getTextChipData(String text) {
    return getTextChipData(text, null);
  }

  public static ChipData getRecipeFulfillmentChipData(
      Context context,
      RecipeFulfillment recipeFulfillment,
      boolean showYellow
  ) {
    ChipData chipFulfillment;
    String textFulfillment;
    if (recipeFulfillment.isNeedFulfilled()) {
      textFulfillment = context.getString(R.string.msg_recipes_enough_in_stock);
      chipFulfillment = new ChipData(context.getString(R.string.property_status_insert), COLOR_ROLES_GREEN);
      chipFulfillment.setCloseIcon(
          ContextCompat.getDrawable(context, R.drawable.ic_round_check_circle_outline)
      );
      chipFulfillment.setCloseIconTint(COLOR_ROLES_GREEN);
      chipFulfillment.setChipBackgroundColor(COLOR_ROLES_GREEN);
    } else if (recipeFulfillment.isNeedFulfilledWithShoppingList() && showYellow) {
      textFulfillment = context.getString(R.string.msg_recipes_not_enough) + "\n"
          + context.getResources()
          .getQuantityString(R.plurals.msg_recipes_ingredients_missing_but_on_shopping_list,
              recipeFulfillment.getMissingProductsCount(),
              recipeFulfillment.getMissingProductsCount());
      chipFulfillment = new ChipData(context.getString(R.string.property_status_insert), COLOR_ROLES_YELLOW);
      chipFulfillment.setCloseIcon(
          ContextCompat.getDrawable(context, R.drawable.ic_round_error_outline)
      );
      chipFulfillment.setCloseIconTint(COLOR_ROLES_YELLOW);
      chipFulfillment.setChipBackgroundColor(COLOR_ROLES_YELLOW);
    } else {
      textFulfillment = context.getString(R.string.msg_recipes_not_enough) + "\n"
          + context.getResources()
          .getQuantityString(R.plurals.msg_recipes_ingredients_missing,
              recipeFulfillment.getMissingProductsCount(),
              recipeFulfillment.getMissingProductsCount());
      chipFulfillment = new ChipData(context.getString(R.string.property_status_insert), COLOR_ROLES_RED);
      chipFulfillment.setCloseIcon(
          ContextCompat.getDrawable(context, R.drawable.ic_round_highlight_off)
      );
      chipFulfillment.setCloseIconTint(COLOR_ROLES_RED);
      chipFulfillment.setChipBackgroundColor(COLOR_ROLES_RED);
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

  public static ChipData getRecipeFulfillmentChipData(Context context, RecipeFulfillment recipeFulfillment) {
    return getRecipeFulfillmentChipData(context, recipeFulfillment, true);
  }

  public static ChipData createProductFulfillmentChip(Context context, boolean needFulfilled) {
    ChipData chipFulfillment;
    String textFulfillment;
    if (needFulfilled) {
      textFulfillment = context.getString(R.string.msg_recipes_enough_in_stock);
      chipFulfillment = new ChipData(context.getString(R.string.property_status_insert), COLOR_ROLES_GREEN);
      chipFulfillment.setCloseIcon(
          ContextCompat.getDrawable(context, R.drawable.ic_round_check_circle_outline)
      );
      chipFulfillment.setCloseIconTint(COLOR_ROLES_GREEN);
      chipFulfillment.setChipBackgroundColor(COLOR_ROLES_GREEN);
    } else {
      textFulfillment = context.getString(R.string.msg_recipes_not_enough);
      chipFulfillment = new ChipData(context.getString(R.string.property_status_insert), COLOR_ROLES_RED);
      chipFulfillment.setCloseIcon(
          ContextCompat.getDrawable(context, R.drawable.ic_round_highlight_off)
      );
      chipFulfillment.setCloseIconTint(COLOR_ROLES_RED);
      chipFulfillment.setChipBackgroundColor(COLOR_ROLES_RED);
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

  public static ChipData getRecipeDueScoreChipData(Context context, int dueScore) {
    ChipData dueScoreChip;
    if (dueScore == 0) {
      dueScoreChip = new ChipData(context.getString(
          R.string.subtitle_recipe_due_score,
          String.valueOf(dueScore)
      ), -1);
    } else if (dueScore <= 10) {
      dueScoreChip = new ChipData(context.getString(
          R.string.subtitle_recipe_due_score,
          String.valueOf(dueScore)
      ), COLOR_ROLES_YELLOW);
      dueScoreChip.setChipBackgroundColor(COLOR_ROLES_YELLOW);
    } else {
      dueScoreChip = new ChipData(context.getString(
          R.string.subtitle_recipe_due_score,
          String.valueOf(dueScore)
      ), COLOR_ROLES_RED);
      dueScoreChip.setChipBackgroundColor(COLOR_ROLES_RED);
    }
    dueScoreChip.setEnabled(false);
    return dueScoreChip;
  }

  public static ChipData getUserfieldChipData(Context context, Userfield userfield, String value) {
    ChipData chipDataUserfield = getTextChipData(null);
    return Userfield.fillChipDataWithUserfield(context, chipDataUserfield, userfield, value);
  }
}

