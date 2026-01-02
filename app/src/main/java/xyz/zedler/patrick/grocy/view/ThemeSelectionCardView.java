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

package xyz.zedler.patrick.grocy.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.LayerDrawable;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.google.android.material.card.MaterialCardView;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class ThemeSelectionCardView extends MaterialCardView {

  private final int innerSize;

  public ThemeSelectionCardView(Context context) {
    super(context);

    final int outerRadius = UiUtil.dpToPx(context, 16);
    final int outerPadding = UiUtil.dpToPx(context, 16);
    innerSize = UiUtil.dpToPx(context, 48);

    // OUTER CARD (this)

    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
    );
    if (UiUtil.isLayoutRtl(context)) {
      params.leftMargin = UiUtil.dpToPx(context, 4);
    } else {
      params.rightMargin = UiUtil.dpToPx(context, 4);
    }
    setLayoutParams(params);
    setContentPadding(outerPadding, outerPadding, outerPadding, outerPadding);
    setRadius(outerRadius);
    setCardElevation(0);
    setCardForegroundColor(null);
    super.setCardBackgroundColor(ResUtil.getColor(context, R.attr.colorSurfaceContainer));
    setRippleColor(ColorStateList.valueOf(ResUtil.getColorHighlight(context)));
    setStrokeWidth(0);
    setCheckable(true);
    setCheckedIconResource(R.drawable.shape_selection_check);
    setCheckedIconSize(innerSize);
    setCheckedIconMargin(outerPadding);
  }

  @Override
  @Deprecated
  public void setCardBackgroundColor(int color) {}

  public void setNestedContext(Context context) {
    removeAllViews();
    ViewGroup.LayoutParams innerParams = new ViewGroup.LayoutParams(innerSize, innerSize);
    MaterialCardView innerCard = new MaterialCardView(context);
    innerCard.setLayoutParams(innerParams);
    innerCard.setRadius(innerSize / 2f);
    innerCard.setStrokeWidth(UiUtil.dpToPx(context, 1));
    innerCard.setStrokeColor(ResUtil.getColor(context, R.attr.colorOutline));
    innerCard.setCardBackgroundColor(ResUtil.getColor(context, R.attr.colorPrimaryContainer));
    innerCard.setCheckable(false);
    addView(innerCard);
    setCheckedIconTint(
        ColorStateList.valueOf(ResUtil.getColor(context, R.attr.colorOnPrimaryContainer))
    );
  }

  public void startCheckedIcon() {
    try {
      LayerDrawable layers = (LayerDrawable) getCheckedIcon();
      if (layers != null) {
        ViewUtil.startIcon(layers.findDrawableByLayerId(R.id.icon_selection_check));
      }
    } catch (ClassCastException ignored) {
      // For API 21 it will be a androidx.core.graphics.drawable.WrappedDrawableApi21
    }
  }
}
