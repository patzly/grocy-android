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

package xyz.zedler.patrick.grocy.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import xyz.zedler.patrick.grocy.databinding.ViewListItemBinding;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class ListItem extends LinearLayout {

  private ViewListItemBinding binding;
  private int height;

  public ListItem(Context context) {
    super(context);

  }

  /**
   * In layout XML set visibility to GONE if the container should expand when setText() is called.
   */
  public ListItem(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    binding = ViewListItemBinding.inflate(
        LayoutInflater.from(context), this, true
    );
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    binding = null;
  }

  public void setText(String property, String value) {
    setText(property, value, null);
  }

  public void setText(String property, String value, String extra) {
    if (property != null && extra != null) {
      int dp4 = UiUtil.dpToPx(getContext(), 4);
      boolean isRtl = UiUtil.isLayoutRtl(getContext());
      binding.linearContainer.setMinimumHeight(UiUtil.dpToPx(getContext(), 88));
      binding.linearContainer.setPadding(
          isRtl ? dp4 * 6 : dp4 * 4, dp4 * 3 , isRtl ? dp4 * 4 : dp4 * 6, dp4 * 3
      );
    }
    // property
    if (property != null) {
      binding.textProperty.setText(property);
    } else {
      binding.textProperty.setVisibility(GONE);
    }
    // value
    binding.textValue.setText(value);
    // extra
    if (extra != null) {
      binding.textExtra.setText(extra);
      binding.textExtra.setVisibility(VISIBLE);
    } else {
      binding.textExtra.setVisibility(GONE);
    }
    if (getVisibility() == GONE) {
      // expand
      measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
      height = getMeasuredHeight();
      getLayoutParams().height = 0;
      requestLayout();

      setAlpha(0);
      setVisibility(VISIBLE);
      animate().alpha(1).setDuration(300).setStartDelay(100).start();
      getViewTreeObserver().addOnGlobalLayoutListener(
          new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
              ValueAnimator heightAnimator = ValueAnimator.ofInt(0, height);
              heightAnimator.addUpdateListener(animation -> {
                getLayoutParams().height = (int) animation.getAnimatedValue();
                requestLayout();
              });
              heightAnimator.setDuration(400).setInterpolator(
                  new DecelerateInterpolator()
              );
              heightAnimator.start();
              if (getViewTreeObserver().isAlive()) {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
              }
            }
          });
    }
  }

  public void setSingleLine(boolean singleLine) {
    binding.textValue.setSingleLine(singleLine);
  }

  @Override
  public void setOnClickListener(@Nullable OnClickListener l) {
    binding.linearContainer.setOnClickListener(l);
  }
}
