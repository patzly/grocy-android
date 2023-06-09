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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;
import xyz.zedler.patrick.grocy.R;

public class RoundedCornerImageView extends AppCompatImageView {

  private float radius = 16f;
  private Path path;
  private RectF rect;
  private boolean bottomCornersRound = true;
  private boolean topCornersRound = true;
  private boolean showBottomShadow = false;
  private Paint shadowPaint;

  public RoundedCornerImageView(Context context) {
    super(context);
    init(null, 0);
  }

  public RoundedCornerImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(attrs, 0);
  }

  public RoundedCornerImageView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(attrs, defStyle);
  }

  private void init(AttributeSet attrs, int defStyle) {
    path = new Path();
    rect = new RectF();

    // Initialize the shadow paint
    shadowPaint = new Paint();
    shadowPaint.setStyle(Paint.Style.FILL);
    shadowPaint.setColor(Color.BLACK);
    shadowPaint.setAlpha(100);
    shadowPaint.setMaskFilter(new BlurMaskFilter(26, BlurMaskFilter.Blur.NORMAL));

    // Extract custom attributes into a TypedArray
    TypedArray a = getContext().getTheme().obtainStyledAttributes(
        attrs,
        R.styleable.RoundedCornerImageView,
        defStyle, 0);

    try {
      // Extract the radius, onlyBottomCornersRound, and showBottomShadow if they exist
      radius = a.getDimensionPixelSize(R.styleable.RoundedCornerImageView_radius, (int) radius);
      bottomCornersRound = a.getBoolean(R.styleable.RoundedCornerImageView_bottomCornersRound, true);
      topCornersRound = a.getBoolean(R.styleable.RoundedCornerImageView_topCornersRound, true);
      showBottomShadow = a.getBoolean(R.styleable.RoundedCornerImageView_showBottomShadow, false);
    } finally {
      // Recycle the TypedArray
      a.recycle();
    }

    // Convert radius to pixels
    radius = getResources().getDisplayMetrics().density * radius;
  }

  public void setRadius(float radius) {
    this.radius = getResources().getDisplayMetrics().density * radius;
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    rect.set(0, 0, getWidth(), getHeight());
    path.rewind();  // Clear the path before adding to it

    if (bottomCornersRound && !topCornersRound) {
      float[] radii = {0, 0, 0, 0, radius, radius, radius, radius};
      path.addRoundRect(rect, radii, Path.Direction.CW);
    } else if (!bottomCornersRound && topCornersRound) {
      float[] radii = {radius, radius, radius, radius, 0, 0, 0, 0};
      path.addRoundRect(rect, radii, Path.Direction.CW);
    } if (!bottomCornersRound && !topCornersRound) {
      path.addRoundRect(rect, 0, 0, Path.Direction.CW);
    } else {
      path.addRoundRect(rect, radius, radius, Path.Direction.CW);
    }

    canvas.clipPath(path);

    // Draw the image
    super.onDraw(canvas);

    // Draw the shadow
    if (showBottomShadow) {
      canvas.drawRect(0, getHeight() - 14, getWidth(), getHeight(), shadowPaint);
    }
  }
}



