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

package xyz.zedler.patrick.grocy.view.swiperefreshlayout;

import android.content.Context;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.view.animation.Animation;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.view.ViewCompat;

/**
 * Private class created to work around issues with AnimationListeners being called before the
 * animation is actually complete and support shadows on older platforms.
 */
public class CircleImageView extends AppCompatImageView {

  private static final int DEFAULT_BACKGROUND_COLOR = 0xFFFAFAFA;

  // PX
  private static final int SHADOW_ELEVATION = 1; // originally 4

  private Animation.AnimationListener mListener;
  private int mBackgroundColor;

  CircleImageView(Context context) {
    super(context);

    final float density = getContext().getResources().getDisplayMetrics().density;

    ShapeDrawable circle = new ShapeDrawable(new OvalShape());
    ViewCompat.setElevation(this, SHADOW_ELEVATION * density);
    circle.getPaint().setColor(mBackgroundColor);
    ViewCompat.setBackground(this, circle);
  }

  public void setAnimationListener(Animation.AnimationListener listener) {
    mListener = listener;
  }

  @Override
  public void onAnimationStart() {
    super.onAnimationStart();
    if (mListener != null) {
      mListener.onAnimationStart(getAnimation());
    }
  }

  @Override
  public void onAnimationEnd() {
    super.onAnimationEnd();
    if (mListener != null) {
      mListener.onAnimationEnd(getAnimation());
    }
  }

  @Override
  public void setBackgroundColor(int color) {
    if (getBackground() instanceof ShapeDrawable) {
      ((ShapeDrawable) getBackground()).getPaint().setColor(color);
      mBackgroundColor = color;
    }
  }

  public int getBackgroundColor() {
    return mBackgroundColor;
  }
}
