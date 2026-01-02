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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class MaterialTimelineView extends ConstraintLayout {

  // SOURCE: https://github.com/hypeapps/MaterialTimelineView/tree/master

  public static final int TIMELINE_TYPE_HEADER = 0;
  public static final int TIMELINE_TYPE_ITEM = 1;
  public static final int POSITION_FIRST = 0;
  public static final int POSITION_MIDDLE = 1;
  public static final int POSITION_LAST = 2;
  public static final int POSITION_SINGLE = 3;
  private static final int DEFAULT_RADIO_RADIUS_DP = 5;
  private static final int DEFAULT_RADIO_MARGIN_START_DP = 44;
  private static final int DEFAULT_HEADER_CONTENT_PADDING_DP = 18;
  private static final int DEFAULT_ENTRY_CONTENT_PADDING_DP = 8;
  private static final int LINE_WIDTH_DP = 2;
  private static final int LINE_LENGTH_DP = 12;

  private int position = POSITION_FIRST;
  private int timelineType = TIMELINE_TYPE_ITEM;
  private float radioRadius;
  private float radioMarginStart;
  private final int headerContentPadding;
  private final int entryContentPadding;
  private final int lineWidth;
  private final int lineLength;
  private final int radioColor;
  private final int lineColor;
  private boolean showLineAndRadio = true;

  private final Paint paint = new Paint();

  public MaterialTimelineView(Context context) {
    this(context, null);
  }

  public MaterialTimelineView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public MaterialTimelineView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    radioRadius = UiUtil.dpToPxFloat(context, DEFAULT_RADIO_RADIUS_DP);
    radioMarginStart = UiUtil.dpToPxFloat(context, DEFAULT_RADIO_MARGIN_START_DP);
    headerContentPadding = UiUtil.dpToPx(context, DEFAULT_HEADER_CONTENT_PADDING_DP);
    entryContentPadding = UiUtil.dpToPx(context, DEFAULT_ENTRY_CONTENT_PADDING_DP);
    lineWidth = UiUtil.dpToPx(context, LINE_WIDTH_DP);
    lineLength = UiUtil.dpToPx(context, LINE_LENGTH_DP);
    radioColor = ResUtil.getColor(context, R.attr.colorOutline);
    lineColor = ResUtil.getColor(context, R.attr.colorOutline);

    setLayerType(View.LAYER_TYPE_HARDWARE, null);
    setWillNotDraw(false);
    paint.setAntiAlias(true);

    if (attrs != null) {
      TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MaterialTimelineView);
      initAttrs(typedArray);
      typedArray.recycle();
    }
  }

  @Override
  public void draw(@NonNull Canvas canvas) {
    super.draw(canvas);
    if (!showLineAndRadio) {
      return;
    }
    drawTimeline(canvas);
  }

  private void drawTimeline(Canvas canvas) {
    float x = radioMarginStart;
    float yRadio = getHeight() / 2f;
    float yLine;
    int headerContentPaddingTop = 0;
    int headerContentPaddingBottom = 0;
    int entryContentPaddingTop = 0;

    switch (position) {
      case POSITION_FIRST:
        yLine = getHeight();
        if (timelineType == TIMELINE_TYPE_HEADER) {
          yRadio = getHeight() / 2f - headerContentPadding / 2f;
        } else {
          yRadio = getHeight() / 2f - entryContentPadding / 2f;
        }
        drawLine(canvas, x, yRadio + radioRadius, x, yLine);
        headerContentPaddingBottom = headerContentPadding;
        break;
      case POSITION_MIDDLE:
        drawLine(canvas, x, 0, x, yRadio - radioRadius);
        drawLine(canvas, x, yRadio + radioRadius, x, getHeight());
        headerContentPaddingTop = headerContentPadding;
        headerContentPaddingBottom = headerContentPadding;
        entryContentPaddingTop = entryContentPadding;
        break;
      case POSITION_LAST:
        if (timelineType == TIMELINE_TYPE_HEADER) {
          yRadio = getHeight() / 2f + headerContentPadding / 2f;
        }
        drawLine(canvas, x, 0, x, yRadio - radioRadius);
        headerContentPaddingTop = headerContentPadding;
        entryContentPaddingTop = entryContentPadding;
        break;
    }

    if (timelineType == TIMELINE_TYPE_HEADER) {
      drawLine(canvas, x + radioRadius, yRadio, x + radioRadius + lineLength, yRadio);
      drawRadio(canvas, x, yRadio, radioRadius, true);
      setPadding(getPaddingLeft(), headerContentPaddingTop, getPaddingRight(), headerContentPaddingBottom);
    } else {
      drawRadio(canvas, x, yRadio, radioRadius, false);
      setPadding(getPaddingLeft(), entryContentPaddingTop, getPaddingRight(), entryContentPadding);
    }
  }

  private void drawRadio(Canvas canvas, float x, float y, float radius, boolean outline) {
    paint.setColor(radioColor);
    paint.setStyle(outline ? Style.STROKE : Style.FILL);
    canvas.drawCircle(x, y, outline ? radius - lineWidth / 2f : radius, paint);
  }

  private void drawLine(Canvas canvas, float startX, float startY, float stopX, float stopY) {
    paint.setColor(lineColor);
    paint.setStyle(Style.STROKE);
    paint.setStrokeWidth(lineWidth);
    paint.setStrokeCap(Cap.ROUND);
    canvas.drawLine(startX, startY, stopX, stopY, paint);
  }

  private void initAttrs(TypedArray typedArray) {
    this.position = typedArray.getInteger(R.styleable.MaterialTimelineView_timeline_position, POSITION_FIRST);
    this.timelineType = typedArray.getInteger(R.styleable.MaterialTimelineView_timeline_type, TIMELINE_TYPE_HEADER);
    this.radioRadius = UiUtil.dpToPxFloat(getContext(),
        typedArray.getDimension(R.styleable.MaterialTimelineView_timeline_radio_radius, DEFAULT_RADIO_RADIUS_DP));
    this.radioMarginStart = UiUtil.dpToPxFloat(getContext(), typedArray.getDimension(
        R.styleable.MaterialTimelineView_timeline_margin_start, DEFAULT_RADIO_MARGIN_START_DP));
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public void setShowLineAndRadio(boolean showLineAndRadio) {
    this.showLineAndRadio = showLineAndRadio;
  }
}
