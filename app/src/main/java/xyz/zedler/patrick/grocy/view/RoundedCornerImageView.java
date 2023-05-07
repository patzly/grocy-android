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
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;

public class RoundedCornerImageView extends AppCompatImageView {

  private float radius = 12f;
  private Path path;
  private RectF rect;

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
    radius = getResources().getDisplayMetrics().density * radius;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    rect.set(0, 0, getWidth(), getHeight());
    path.addRoundRect(rect, radius, radius, Path.Direction.CW);
    canvas.clipPath(path);
    super.onDraw(canvas);
  }
}
