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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.view;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.ViewFilterChipNewBinding;
import xyz.zedler.patrick.grocy.model.FilterChipLiveData;
import xyz.zedler.patrick.grocy.model.FilterChipLiveData.MenuItemData;

public class FilterChipNew extends LinearLayout {

  private final static String TAG = FilterChipNew.class.getSimpleName();

  private final ViewFilterChipNewBinding binding;

  public FilterChipNew(@NonNull Context context, AttributeSet attributeSet) {
    super(context, attributeSet);
    binding = ViewFilterChipNewBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    );
  }

  public void setData(FilterChipLiveData data) {
    int colorFrom = binding.card.getCardBackgroundColor().getColorForState(
        EMPTY_STATE_SET,
        ContextCompat.getColor(getContext(), R.color.on_background_secondary)
    );
    int colorTo = ContextCompat.getColor(getContext(), data.getColorBackground());
    ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
    colorAnimation.setDuration(250);
    colorAnimation.addUpdateListener(
        animation -> binding.card.setCardBackgroundColor((int) animation.getAnimatedValue())
    );
    colorAnimation.start();
    binding.text.setText(data.getText());
    binding.card.setStrokeColor(ContextCompat.getColor(getContext(), data.getColorStroke()));
    if (data.getDrawable() == -1) {
      binding.frameIcon.setVisibility(GONE);
    } else {
      binding.imageIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), data.getDrawable()));
      binding.frameIcon.setVisibility(View.VISIBLE);
    }

    if (data.hasPopupMenu()) {
      PopupMenu popupMenu = new PopupMenu(getContext(), this);
      Menu menu = popupMenu.getMenu();
      for (MenuItemData menuItemData : data.getMenuItemDataList()) {
        MenuItem menuItem = menu
            .add(0, menuItemData.getItemId(), Menu.NONE, menuItemData.getText());
        menuItem.setCheckable(true);
        menuItem.setChecked(menuItemData.getItemId() == data.getItemIdChecked());
        menuItem.setOnMenuItemClickListener(data.getMenuItemClickListener());
      }
      menu.setGroupCheckable(0, true, true);
      binding.card.setOnClickListener(v -> popupMenu.show());
    }
  }
}
