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

package xyz.zedler.patrick.grocy.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
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
import androidx.core.view.MenuCompat;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.Observer;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.ViewFilterChipBinding;
import xyz.zedler.patrick.grocy.model.FilterChipLiveData;
import xyz.zedler.patrick.grocy.model.FilterChipLiveData.MenuItemData;
import xyz.zedler.patrick.grocy.model.FilterChipLiveData.MenuItemGroup;
import xyz.zedler.patrick.grocy.util.ResUtil;

public class FilterChip extends LinearLayout {

  private final static String TAG = FilterChip.class.getSimpleName();

  private final ViewFilterChipBinding binding;
  private FilterChipLiveData liveData;
  private final Observer<FilterChipLiveData> liveDataObserver;
  private boolean firstTimePassed = false;  // necessary to prevent weird animation on fragment init

  public FilterChip(@NonNull Context context, AttributeSet attributeSet) {
    super(context, attributeSet);
    binding = ViewFilterChipBinding.inflate(
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
        EMPTY_STATE_SET, Color.TRANSPARENT
    );
    int bgColorTo = data.isActive()
        ? ResUtil.getColorAttr(getContext(), R.attr.colorSecondaryContainer)
        : Color.TRANSPARENT;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      ValueAnimator colorAnimation = ValueAnimator.ofArgb(bgColorFrom, bgColorTo);
      colorAnimation.setDuration(250);
      colorAnimation.addUpdateListener(
              animation -> binding.card.setCardBackgroundColor((int) animation.getAnimatedValue())
      );
      colorAnimation.start();
    } else {
      binding.text.setBackgroundColor(bgColorTo);
    }

    // text
    binding.text.setText(data.getText());

    // text color
    binding.text.setTextColor(ResUtil.getColorAttr(getContext(), data.isActive()
        ? R.attr.colorOnSecondaryContainer
        : R.attr.colorOnSurface
    ));

    // expand icon color
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      binding.imageIconExpand.setImageTintList(ColorStateList.valueOf(
              ResUtil.getColorAttr(getContext(), data.isActive()
                      ? R.attr.colorOnSecondaryContainer
                      : R.attr.colorOnSurfaceVariant
              )
      ));
    }

    if (data.getDrawable() == -1) {
      binding.frameIcon.setVisibility(GONE);
    } else {
      binding.imageIcon.setImageDrawable(
          ContextCompat.getDrawable(getContext(), data.getDrawable())
      );
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
      binding.container.setOnClickListener(v -> popupMenu.show());
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
  public static void observeData(FilterChip view, FilterChipLiveData.Listener dataListener) {
    // This is a workaround. The static binding adapter method overrides auto method selection
    // for app:data and takes a FilterChipLiveData.Listener object where it can extract the
    // LiveData. It is necessary because Data Binding throws error when method takes LiveData as
    // parameter AND actual parameter is also LiveData.
    view.setData(dataListener.getData());
  }
}
