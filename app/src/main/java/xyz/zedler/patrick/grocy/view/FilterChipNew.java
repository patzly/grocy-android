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
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.MenuCompat;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.Observer;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.ViewFilterChipNewBinding;
import xyz.zedler.patrick.grocy.model.FilterChipLiveData;
import xyz.zedler.patrick.grocy.model.FilterChipLiveData.MenuItemData;
import xyz.zedler.patrick.grocy.model.FilterChipLiveData.MenuItemGroup;

public class FilterChipNew extends LinearLayout {

  private final static String TAG = FilterChipNew.class.getSimpleName();

  private final ViewFilterChipNewBinding binding;
  private FilterChipLiveData liveData;
  private final Observer<FilterChipLiveData> liveDataObserver;
  private boolean firstTimePassed = false;  // necessary to prevent weird animation on fragment init

  public FilterChipNew(@NonNull Context context, AttributeSet attributeSet) {
    super(context, attributeSet);
    binding = ViewFilterChipNewBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    );
    liveDataObserver = data -> {
      if (firstTimePassed) {
        TransitionManager.beginDelayedTransition((ViewGroup) getRootView(), new AutoTransition());
      }
      setData(data);
      firstTimePassed = true;
    };
  }

  public void setData(FilterChipLiveData data) {
    liveData = data;
    if (isAttachedToWindow() && !liveData.hasObservers()) {
      liveData.observeForever(liveDataObserver);
    }

    // background color
    int bgColorFrom = binding.card.getCardBackgroundColor().getColorForState(
        EMPTY_STATE_SET,
        ContextCompat.getColor(getContext(), R.color.filter_chip_background_inactive)
    );
    int bgColorTo = ContextCompat.getColor(getContext(), data.isActive()
        ? R.color.filter_chip_background_active
        : R.color.filter_chip_background_inactive
    );
    ValueAnimator colorAnimation = ValueAnimator
        .ofObject(new ArgbEvaluator(), bgColorFrom, bgColorTo);
    colorAnimation.setDuration(250);
    colorAnimation.addUpdateListener(
        animation -> binding.card.setCardBackgroundColor((int) animation.getAnimatedValue())
    );
    colorAnimation.start();

    // text
    binding.text.setText(data.getText());

    // text color
    binding.text.setTextColor(ContextCompat.getColor(getContext(), data.isActive()
        ? R.color.filter_chip_text_active
        : R.color.filter_chip_text_inactive
    ));

    // expand icon color
    Drawable expandDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_round_expand_more);
    assert expandDrawable != null;
    DrawableCompat.setTint(expandDrawable, ContextCompat.getColor(getContext(), data.isActive()
        ? R.color.filter_chip_expand_active
        : R.color.filter_chip_expand_inactive
    ));
    binding.imageIconExpand.setImageDrawable(expandDrawable);

    // stroke color
    binding.card.setStrokeColor(ContextCompat.getColor(getContext(), data.isActive()
        ? R.color.filter_chip_stroke_active
        : R.color.filter_chip_stroke_inactive
    ));

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
        MenuItem menuItem = menu.add(
            menuItemData.getGroupId(),
            menuItemData.getItemId(),
            Menu.NONE,
            menuItemData.getText()
        );
        menuItem.setCheckable(true);
        menuItem.setChecked(menuItemData.getItemId() == data.getItemIdChecked()
            || menuItemData.isChecked());
        menuItem.setOnMenuItemClickListener(data.getMenuItemClickListener());
      }
      if (data.getMenuItemGroupArray() != null) {
        for (MenuItemGroup menuItemGroup : data.getMenuItemGroupArray()) {
          menu.setGroupCheckable(
              menuItemGroup.getGroupId(),
              menuItemGroup.isCheckable(),
              menuItemGroup.isExclusive()
          );
        }
        if (data.getMenuItemGroupArray().length > 1) {
          MenuCompat.setGroupDividerEnabled(menu, true);
        }
      }
      binding.card.setOnClickListener(v -> popupMenu.show());
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (liveData != null && !liveData.hasObservers()) {
      liveData.observeForever(liveDataObserver);
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (liveData != null) {
      liveData.removeObserver(liveDataObserver);
    }
  }

  @BindingAdapter("data")
  public static void observeData(FilterChipNew view, FilterChipLiveData.Listener dataListener) {
    // This is a workaround. The static binding adapter method overrides auto method selection
    // for app:data and takes a FilterChipLiveData.Listener object where it can extract the
    // LiveData. It is necessary because Data Binding throws error when method takes LiveData as
    // parameter AND actual parameter is also LiveData.
    view.setData(dataListener.getData());
  }
}
