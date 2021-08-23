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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.UnitUtil;

public class BezierCurveChart extends View {

  private static final float CURVE_LINE_WIDTH = 2f;
  private static final float BADGE_HEIGHT = 10; // percentage of chartRect height
  private static final float BADGE_MARGIN = 5;
  private static final float BADGE_FONT_SIZE = 70; // 100 means font fills badge with height
  private static final float X_LABEL_OFFSET_Y = 16;
  private static final float Y_LABEL_OFFSET_X = 12;

  public static class Point {

    final float x;
    final float y;

    public Point(float x, float y) {
      this.x = x;
      this.y = y;
    }

    static final Comparator<Point> X_COMPARATOR = (lhs, rhs) -> (int) (lhs.x - rhs.x);

    @NonNull
    @Override
    public String toString() {
      return "(" + x + ", " + y + ")";
    }
  }

  private final Context context;
  private final Paint paintChartBg = new Paint();
  private final Paint paintCurve = new Paint();
  private final Paint paintDot = new Paint();
  private final Paint paintFill = new Paint();
  private final Paint paintGrid = new Paint();
  private final Paint paintLabel = new Paint();
  private final Paint paintBadge = new Paint();
  private final Paint paintBadgeText = new Paint();
  private final Path pathCurve = new Path();
  private final Path pathFill = new Path();
  private final RectF rectChart = new RectF(); // rectangle without labels
  private final RectF rectBadge = new RectF();
  private final Rect rectDrawing = new Rect();
  private final ArrayList<Integer> curveColors = new ArrayList<>();
  private ArrayList<String> labels;
  private HashMap<String, ArrayList<Point>> curveLists;
  private final HashMap<String, ArrayList<Point>> adjustedCurveLists = new HashMap<>();
  private float lastXLeftBadge = 0;
  private float maxY = 0;
  private float scaleY;
  private final int cornerRadius;
  private final int dotRadius;

  public BezierCurveChart(Context context, AttributeSet attrs) {
    super(context, attrs);

    this.context = context;

    cornerRadius = UnitUtil.dpToPx(context, 6);
    dotRadius = UnitUtil.dpToPx(context, 3);

    paintCurve.setStyle(Paint.Style.STROKE);
    paintCurve.setStrokeCap(Paint.Cap.ROUND);
    paintCurve.setStrokeWidth(UnitUtil.dpToPx(context, CURVE_LINE_WIDTH));
    paintCurve.setAntiAlias(true);

    curveColors.add(getColor(R.color.retro_blue_bg));
    curveColors.add(getColor(R.color.retro_yellow_bg));
    curveColors.add(getColor(R.color.retro_red_bg_black));
    curveColors.add(getColor(R.color.retro_dirt)); // TODO: light variants
    curveColors.add(getColor(R.color.retro_green_bg_black));

    paintDot.setStyle(Paint.Style.FILL);
    paintDot.setAntiAlias(true);

    paintFill.setStyle(Paint.Style.FILL);
    paintFill.setAntiAlias(true);
    paintFill.setPathEffect(new CornerPathEffect(cornerRadius));

    paintChartBg.setStyle(Paint.Style.FILL);
    paintChartBg.setColor(getColor(R.color.on_background_tertiary));
    paintChartBg.setAntiAlias(true);

    paintGrid.setStyle(Paint.Style.STROKE);
    paintGrid.setStrokeCap(Paint.Cap.SQUARE);
    paintGrid.setColor(Color.argb(0x30, 0xD0, 0xD0, 0xD0));
    paintGrid.setAntiAlias(true);
    paintGrid.setStrokeWidth(3.0f);

    paintBadge.setStyle(Paint.Style.FILL);
    paintBadge.setAntiAlias(true);

    paintBadgeText.setColor(Color.BLACK);
    paintBadgeText.setAntiAlias(true);
    paintBadgeText.setTextSize(UnitUtil.spToPx(context, 14));
    paintBadgeText.setTypeface(ResourcesCompat.getFont(context, R.font.jost_medium));

    paintLabel.setColor(getColor(R.color.on_background_secondary));
    paintLabel.setTextSize(UnitUtil.spToPx(context, 13));
    paintLabel.setTypeface(ResourcesCompat.getFont(context, R.font.jost_book));
    paintLabel.setAntiAlias(true);
  }

  private void drawLabels(Canvas canvas) {
    // Y-AXIS

    // Y coordinate scale
    float maxWidth = 0;
    for (String label : labels) {
      float labelWidth = getTextWidth(paintLabel, label);
      if (labelWidth > maxWidth) {
        maxWidth = labelWidth;
      }
    }
    int newBottom = (int) (rectChart.bottom - maxWidth - X_LABEL_OFFSET_Y);
    int newChartHeight = (int) (newBottom - rectChart.top);
    scaleY = newChartHeight / maxY * (1 - BADGE_HEIGHT / 100 - BADGE_MARGIN * 2 / 100);

    maxWidth = 0;
    for (float y = 1; y < maxY; y++) {
      if (maxY >= 10 && maxY < 50 && y % 5 != 0) {
        continue;
      }
      if (maxY >= 50 && maxY < 100 && y % 10 != 0) {
        continue;
      }
      if (maxY >= 100 && y % 20 != 0) {
        continue;
      }
      String s = NumUtil.trim(y);
      float labelWidth = getTextWidth(paintLabel, s);
      if (maxWidth < labelWidth) {
        maxWidth = labelWidth;
      }
    }

    // Move left border
    rectChart.left = (int) (rectChart.left + dotRadius);
    if (getTextWidth(paintLabel, labels.get(labels.size() - 1)) > maxWidth) {
      rectChart.right -= getTextWidth(paintLabel, labels.get(labels.size() - 1)) - 40;
    } else {
      rectChart.right -= maxWidth + 20;
    }

    float labelX = rectChart.right;
    float labelY;
    for (float y = 1; y < maxY; y++) {
      if (maxY >= 10 && maxY < 50 && y % 5 != 0) {
        continue;
      }
      if (maxY >= 50 && maxY < 100 && y % 10 != 0) {
        continue;
      }
      if (maxY >= 100 && y % 20 != 0) {
        continue;
      }
      String s = NumUtil.trim(y);
      float centerY = newBottom - y * scaleY;
      labelY = centerY + (getTextHeight(paintLabel) - 16) / 2;
      canvas.drawText(s, labelX + Y_LABEL_OFFSET_X, labelY, paintLabel);
    }

    // X-AXIS

    float part;
    if (labels.size() > 1) {
      part = rectChart.width() / (labels.size() - 1);
    } else {
      part = 1;
    }

    maxWidth = 0;
    for (int i = 0; i < labels.size(); i++) {
      String s = labels.get(i);
      float centerX = rectChart.left + part * i;
      float labelWidth = getTextWidth(paintLabel, s);
      if (labelWidth > maxWidth) {
        maxWidth = labelWidth;
      }

      if (i == 0) {
        centerX += 15;
        labelX = centerX;
      } else if (i == labels.size() - 1) {
        centerX -= 5;
        labelX = centerX;
      } else {
        labelX = centerX;
      }
      labelY = rectChart.bottom + X_LABEL_OFFSET_Y / 2;
      labelX += X_LABEL_OFFSET_Y / 2;
      canvas.save();
      canvas.rotate(45f, centerX, newBottom);
      canvas.drawText(s, labelX, labelY - labelWidth, paintLabel);
      canvas.restore();
    }
    rectChart.bottom = (int) (rectChart.bottom - maxWidth - X_LABEL_OFFSET_Y);
  }

  private void adjustPoints() {
    float minX = 0;
    float maxX = 0;

    for (ArrayList<Point> curveList : curveLists.values()) {
      for (Point p : curveList) {
        if (p.x < minX) {
          minX = p.x;
        }
        if (p.x > maxX) {
          maxX = p.x;
        }
      }
    }

    float axesSpan = maxX - minX;
    if (axesSpan == 0) {
      axesSpan = 1;
    }
    float startX = minX;

    for (String key : curveLists.keySet()) {
      ArrayList<Point> curveList = curveLists.get(key);
      assert curveList != null;
      for (int i = 0; i < curveList.size(); i++) {
        Point p = curveList.get(i);

        Point newPoint = new Point(
            (p.x - startX) * rectChart.width() / axesSpan + rectChart.left,
            rectChart.height() - (p.y * scaleY)
        );

        ArrayList<Point> adjustedCurveList = adjustedCurveLists.get(key);
        assert adjustedCurveList != null;
        if (i + 1 > adjustedCurveList.size()) {
          adjustedCurveList.add(newPoint);
        } else {
          adjustedCurveList.set(i, newPoint);
        }
      }
    }
  }

  private void drawGrid(Canvas canvas) {
    canvas.drawRoundRect(rectChart, cornerRadius, cornerRadius, paintChartBg);

    int gridCount = labels.size() - 1;
    float part = rectChart.width() / gridCount;

    for (int i = 1; i < gridCount; i++) {
      float x = rectChart.left + part * i;
      canvas.drawLine(x, rectChart.top, x, rectChart.bottom, paintGrid);
    }

    for (float y = 1; y < maxY; y++) {
      if (maxY >= 10 && maxY < 50 && y % 5 != 0) {
        continue;
      }
      if (maxY >= 50 && maxY < 100 && y % 10 != 0) {
        continue;
      }
      if (maxY >= 100 && y % 20 != 0) {
        continue;
      }
      canvas.drawLine(rectChart.left,
          rectChart.bottom - y * scaleY,
          rectChart.right,
          rectChart.bottom - y * scaleY, paintGrid
      );
    }
  }

  private void drawCurvesFill(Canvas canvas) {
    int colorIndex = 0;
    for (String curveLabel : adjustedCurveLists.keySet()) {
      ArrayList<Point> curveList = adjustedCurveLists.get(curveLabel);
      assert curveList != null;
      buildPath(pathFill, curveList);
      pathFill.lineTo(curveList.get(curveList.size() - 1).x, rectChart.bottom);
      pathFill.lineTo(curveList.get(0).x, rectChart.bottom);
      pathFill.lineTo(curveList.get(0).x, curveList.get(0).y);
      pathFill.close();
      int curveColor = curveColors.get(colorIndex);
      colorIndex++;
      if (colorIndex > curveColors.size() - 1) {
        colorIndex = 0;
      }
      paintFill.setColor(curveColor);
      paintFill.setAlpha(40);
      canvas.drawPath(pathFill, paintFill);
    }
  }

  private void buildPath(Path path, ArrayList<Point> curveList) {
    path.reset(); // important!

    path.moveTo(curveList.get(0).x, curveList.get(0).y);

    for (int i = 1; i < curveList.size(); i++) {
      float pointX = curveList.get(i).x;
      float pointY = curveList.get(i).y;

      float control1X = (curveList.get(i).x + curveList.get(i - 1).x) / 2;
      float control1Y = curveList.get(i - 1).y;

      float control2X = (curveList.get(i).x + curveList.get(i - 1).x) / 2;
      float control2Y = curveList.get(i).y;

      path.cubicTo(control1X, control1Y, control2X, control2Y, pointX, pointY);
    }
  }

  private void drawCurvesLine(Canvas canvas) {
    int colorIndex = 0;
    for (String curveLabel : adjustedCurveLists.keySet()) {
      ArrayList<Point> curveList = adjustedCurveLists.get(curveLabel);
      assert curveList != null;
      int curveColor = curveColors.get(colorIndex);
      colorIndex++;
      if (colorIndex > curveColors.size() - 1) {
        colorIndex = 0;
      }
      drawDots(canvas, curveList, curveColor);
      drawBadge(canvas, curveLabel, curveColor);

      buildPath(pathCurve, curveList);
      paintCurve.setColor(curveColor);
      canvas.drawPath(pathCurve, paintCurve);
    }
  }

  private void drawDots(Canvas canvas, ArrayList<Point> curveList, int color) {
    paintDot.setColor(color);
    for (Point point : curveList) {
      canvas.drawCircle(point.x, point.y, dotRadius, paintDot);
    }
  }

  private void drawBadge(Canvas canvas, String text, int color) {
    rectBadge.top = rectChart.top + rectChart.height() * BADGE_MARGIN / 100;
    rectBadge.bottom = rectBadge.top + rectChart.height() * BADGE_HEIGHT / 100;
    float badgeMargin = rectChart.height() * BADGE_MARGIN / 100;

    Rect textBounds = new Rect();
    paintBadgeText.getTextBounds(text, 0, 1, textBounds);
    float textWidth = getTextWidth(paintBadgeText, text);
    float textHeight = textBounds.bottom - textBounds.top;
    float textY = rectBadge.top + rectBadge.height() / 2 + textHeight / 2;

    if (lastXLeftBadge == 0) {
      lastXLeftBadge = rectChart.right;
    }
    rectBadge.right = lastXLeftBadge - badgeMargin;
    rectBadge.left = rectBadge.right - textWidth - rectBadge.height() * BADGE_FONT_SIZE / 200;
    lastXLeftBadge = rectBadge.left;
    float badgeCenterX = rectBadge.left + rectBadge.width() / 2;

    float textX = badgeCenterX - textWidth / 2;

    paintBadge.setColor(color);
    canvas.drawRoundRect(rectBadge, cornerRadius, cornerRadius, paintBadge);
    canvas.drawText(text, textX, textY, paintBadgeText);
  }

  public float getTextHeight(Paint textPaint) {
    FontMetrics fm = textPaint.getFontMetrics();
    return (float) Math.ceil(fm.descent - fm.ascent) - 2;
  }

  public float getTextWidth(Paint textPaint, String text) {
    return textPaint.measureText(text);
  }

  public void init(
      HashMap<String, ArrayList<Point>> curveLists,
      ArrayList<String> labels
  ) {
    this.curveLists = curveLists;
    this.labels = labels;
    for (String key : curveLists.keySet()) {
      adjustedCurveLists.put(key, new ArrayList<>());
    }
    // order by x coordinate ascending
    for (ArrayList<Point> curveList : curveLists.values()) {
      Collections.sort(curveList, Point.X_COMPARATOR);
    }
    // set maxY to highest y coordinate
    maxY = 0;
    for (ArrayList<Point> curveList : curveLists.values()) {
      for (Point p : curveList) {
        if (p.y > maxY) {
          maxY = p.y;
        }
      }
    }
    super.invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    getDrawingRect(rectDrawing);

    rectChart.bottom = rectDrawing.bottom;
    rectChart.top = rectDrawing.top;
    rectChart.left = rectDrawing.left;
    rectChart.right = rectDrawing.right;

    if (curveLists == null) {
      return;
    }

    lastXLeftBadge = 0;

    drawLabels(canvas);

    adjustPoints();

    drawGrid(canvas);

    drawCurvesFill(canvas);

    drawCurvesLine(canvas);
  }

  private int getColor(@ColorRes int color) {
    return ContextCompat.getColor(context, color);
  }
}

