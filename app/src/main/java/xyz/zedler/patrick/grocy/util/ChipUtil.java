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
import android.view.View;
import android.widget.LinearLayout;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.ColorRoles;
import com.google.android.material.elevation.SurfaceColors;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.ChipData;

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

  public Chip createChipFromData(ChipData chipData) {
    Chip chip = createChip(context, chipData.getText(), -1);
    switch (chipData.getChipBackgroundColor()) {
      case ChipData.COLOR_ROLES_BLUE:
        chip.setChipBackgroundColor(ColorStateList.valueOf(colorBlue.getAccentContainer()));
        break;
      case ChipData.COLOR_ROLES_GREEN:
        chip.setChipBackgroundColor(ColorStateList.valueOf(colorGreen.getAccentContainer()));
        break;
      case ChipData.COLOR_ROLES_YELLOW:
        chip.setChipBackgroundColor(ColorStateList.valueOf(colorYellow.getAccentContainer()));
        break;
      case ChipData.COLOR_ROLES_RED:
        chip.setChipBackgroundColor(ColorStateList.valueOf(colorRed.getAccentContainer()));
        break;
    }
    switch (chipData.getTextColor()) {
      case ChipData.COLOR_ROLES_BLUE:
        chip.setChipBackgroundColor(ColorStateList.valueOf(colorBlue.getOnAccentContainer()));
        break;
      case ChipData.COLOR_ROLES_GREEN:
        chip.setChipBackgroundColor(ColorStateList.valueOf(colorGreen.getOnAccentContainer()));
        break;
      case ChipData.COLOR_ROLES_YELLOW:
        chip.setChipBackgroundColor(ColorStateList.valueOf(colorYellow.getOnAccentContainer()));
        break;
      case ChipData.COLOR_ROLES_RED:
        chip.setChipBackgroundColor(ColorStateList.valueOf(colorRed.getOnAccentContainer()));
        break;
    }
    chip.setOnClickListener(chipData.getOnClickListener());
    if (chipData.getChipIcon() != null) {
      chip.setChipIcon(chipData.getChipIcon());
    }
    switch (chipData.getChipIconTint()) {
      case ChipData.COLOR_ROLES_BLUE:
        chip.setChipIconTint(ColorStateList.valueOf(colorBlue.getOnAccentContainer()));
        break;
      case ChipData.COLOR_ROLES_GREEN:
        chip.setChipIconTint(ColorStateList.valueOf(colorGreen.getOnAccentContainer()));
        break;
      case ChipData.COLOR_ROLES_YELLOW:
        chip.setChipIconTint(ColorStateList.valueOf(colorYellow.getOnAccentContainer()));
        break;
      case ChipData.COLOR_ROLES_RED:
        chip.setChipIconTint(ColorStateList.valueOf(colorRed.getOnAccentContainer()));
        break;
    }
    if (chipData.getTextStartPadding() != -1) {
      chip.setTextStartPadding(chipData.getTextStartPadding());
    }
    if (chipData.getCloseIcon() != null) {
      chip.setCloseIcon(chipData.getCloseIcon());
    }
    switch (chipData.getCloseIconTint()) {
      case ChipData.COLOR_ROLES_BLUE:
        chip.setCloseIconTint(ColorStateList.valueOf(colorBlue.getOnAccentContainer()));
        break;
      case ChipData.COLOR_ROLES_GREEN:
        chip.setCloseIconTint(ColorStateList.valueOf(colorGreen.getOnAccentContainer()));
        break;
      case ChipData.COLOR_ROLES_YELLOW:
        chip.setCloseIconTint(ColorStateList.valueOf(colorYellow.getOnAccentContainer()));
        break;
      case ChipData.COLOR_ROLES_RED:
        chip.setCloseIconTint(ColorStateList.valueOf(colorRed.getOnAccentContainer()));
        break;
    }
    if (chipData.getCloseIconStartPadding() != -1 ) {
      chip.setCloseIconStartPadding(chipData.getCloseIconStartPadding());
    }
    chip.setCloseIconVisible(chipData.isCloseIconVisible());
    chip.setEnabled(chipData.isEnabled());
    chip.setFocusable(chipData.isEnabled());
    chip.setClickable(chipData.isEnabled());
    return chip;
  }

  public View createSeparator() {
    LinearLayout separator = new LinearLayout(context);
    int dp1 = UiUtil.dpToPx(context, 1);
    int dp18 = UiUtil.dpToPx(context, 18);
    int dp4 = UiUtil.dpToPx(context, 4);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp1, dp18);
    lp.setMargins(0, dp4, 0, dp4);
    separator.setLayoutParams(lp);
    separator.setBackgroundColor(ResUtil.getColorAttr(context, R.attr.colorOutlineVariant));
    return separator;
  }
}
