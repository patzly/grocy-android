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
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Op;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;
import com.google.android.material.color.ColorRoles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class BezierCurveChart extends View {

  private static final String TAG = BezierCurveChart.class.getSimpleName();

  private static final float LABEL_ROTATION = 45;

  public static class Point implements Comparable<Point> {
    final float x;
    final float y;

    public Point(float x, float y) {
      this.x = x;
      this.y = y;
    }

    @Override
    public int compareTo(Point o) {
      return Float.compare(this.x, o.x);
    }

    @NonNull
    @Override
    public String toString() {
      return "(" + x + ", " + y + ")";
    }
  }

  private final Paint paintChartBg = new Paint();
  private final Paint paintCurve;
  private final Paint paintDot = new Paint();
  private final Paint paintFill = new Paint();
  private final Paint paintGrid = new Paint();
  private final Paint paintLabel = new Paint();
  private final Paint paintBadge = new Paint();
  private final Paint paintBadgeText = new Paint();
  private final Path pathCurve = new Path();
  private final Path pathFill = new Path();
  private final Path pathFillMask = new Path();
  private final RectF rectChart = new RectF(); // rectangle without labels
  private final RectF rectBadge = new RectF();
  private final Rect rectMeasure = new Rect();
  private final Rect rectDrawing = new Rect();
  private final List<ColorRoles> curveColors = new ArrayList<>();
  private ArrayList<String> labels;
  private HashMap<String, ArrayList<Point>> curveLists;
  private final HashMap<String, ArrayList<Point>> adjustedCurveLists = new HashMap<>();
  private float lastXLeftBadge = 0;
  private float maxY = 0;
  private float scaleY;
  private final int cornerRadiusBadge, cornerRadiusBg;
  private final int badgeHeight, badgePadding, badgeMargin;
  private final int dotRadius;
  private final int labelMargin, paddingEnd;
  private final boolean isRtl;
  private final int decimalPlacesPriceDisplay;

  public BezierCurveChart(Context context, AttributeSet attrs) {
    super(context, attrs);

    isRtl = UiUtil.isLayoutRtl(context);
    decimalPlacesPriceDisplay = PreferenceManager.getDefaultSharedPreferences(context).getInt(
        STOCK.DECIMAL_PLACES_PRICES_DISPLAY,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_PRICES_DISPLAY
    );

    cornerRadiusBadge = UiUtil.dpToPx(context, 8);
    cornerRadiusBg = UiUtil.dpToPx(context, 12);
    dotRadius = UiUtil.dpToPx(context, 4);
    badgeHeight = UiUtil.dpToPx(context, 24);
    badgePadding = UiUtil.dpToPx(context, 7);
    badgeMargin = UiUtil.dpToPx(context, 8);
    labelMargin = UiUtil.dpToPx(context, 8);
    paddingEnd = UiUtil.dpToPx(context, 4);

    paintCurve = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintCurve.setStyle(Paint.Style.STROKE);
    paintCurve.setStrokeWidth(UiUtil.dpToPx(context, 2));

    curveColors.add(ResUtil.getHarmonizedRoles(context, R.color.green));
    curveColors.add(ResUtil.getHarmonizedRoles(context, R.color.yellow));
    curveColors.add(ResUtil.getHarmonizedRoles(context, R.color.orange));
    curveColors.add(ResUtil.getHarmonizedRoles(context, R.color.red));
    curveColors.add(ResUtil.getHarmonizedRoles(context, R.color.blue));

    paintDot.setStyle(Paint.Style.FILL);
    paintDot.setAntiAlias(true);

    paintFill.setStyle(Paint.Style.FILL);
    paintFill.setAntiAlias(true);

    paintChartBg.setStyle(Paint.Style.FILL);
    paintChartBg.setColor(ResUtil.getColorAttr(context, R.attr.colorSurfaceVariant));
    paintChartBg.setAntiAlias(true);

    paintGrid.setStyle(Paint.Style.STROKE);
    // TODO: use colorOutlineVariant when it's available
    paintGrid.setColor(ResUtil.getColorAttr(context, R.attr.colorOutline));
    paintGrid.setAlpha(100);
    paintGrid.setAntiAlias(true);
    paintGrid.setStrokeWidth(UiUtil.dpToPx(context, 1));

    paintBadge.setStyle(Paint.Style.FILL);
    paintBadge.setAntiAlias(true);

    paintBadgeText.setAntiAlias(true);
    paintBadgeText.setTextSize(UiUtil.spToPx(context, 12));
    paintBadgeText.setTypeface(ResourcesCompat.getFont(context, R.font.jost_medium));

    paintLabel.setColor(ResUtil.getColorAttr(context, R.attr.colorOnSurface));
    paintLabel.setTextSize(UiUtil.spToPx(context, 11));
    paintLabel.setTypeface(ResourcesCompat.getFont(context, R.font.jost_medium));
    paintLabel.setAntiAlias(true);
  }

  private void drawLabels(Canvas canvas) {
    // max date width
    int maxDateHeight = 0;
    int lastDateWidth = 0;
    for (int i = 0; i < labels.size(); i++) {
      int labelWidth = getTextWidth(paintLabel, labels.get(i)) + labelMargin;
      double alpha = Math.toRadians(90 - LABEL_ROTATION);
      int rotatedHeight = (int) (labelWidth * Math.cos(alpha));
      if (rotatedHeight > maxDateHeight) {
        maxDateHeight = rotatedHeight;
      }
      if (i == labels.size() - 1) { // calculate last width
        lastDateWidth = (int) (labelWidth * Math.sin(Math.toRadians(LABEL_ROTATION)));
      }
    }
    rectChart.bottom -= maxDateHeight + paintLabel.getTextSize() / 2;

    // max price width
    int maxPriceWidth = 0;
    for (float y = 1; y < maxY * (maxY <= 1 ? 10 : 1); y++) {
      if (maxY >= 10 && maxY < 50 && y % 5 != 0) {
        continue;
      }
      if (maxY >= 50 && maxY < 100 && y % 10 != 0) {
        continue;
      }
      if (maxY >= 100 && y % 20 != 0) {
        continue;
      }
      int labelWidth = getTextWidth(paintLabel, NumUtil.trimPrice(y / (maxY <= 1 ? 10 : 1), decimalPlacesPriceDisplay));
      if (labelWidth > maxPriceWidth) {
        maxPriceWidth = labelWidth;
      }
    }
    rectChart.right -= Math.max(maxPriceWidth + labelMargin, lastDateWidth);

    // X-AXIS

    float sectionWidth;
    if (labels.size() > 1) {
      sectionWidth = rectChart.width() / (labels.size() - 1);
    } else {
      sectionWidth = 1;
    }

    float centerX, centerY;
    for (int i = 0; i < labels.size(); i++) {
      String label = labels.get(i);
      paintLabel.getTextBounds(label, 0, label.length(), rectMeasure);
      centerX = rectChart.left + sectionWidth * i;
      centerY = rectChart.bottom + labelMargin;
      canvas.save();
      canvas.rotate(LABEL_ROTATION, centerX, centerY);
      canvas.drawText(label, centerX, centerY - rectMeasure.centerY(), paintLabel);
      canvas.restore();
    }

    // Y-AXIS

    float drawingHeight = rectChart.height() - badgeHeight - badgeMargin * 2;
    if (maxY > 1) {
      scaleY = drawingHeight / maxY; // 800 / 4 = 200
    } else {
      scaleY = drawingHeight / (maxY * 10); // 800 / 0.4 * 10
    }

    centerX = rectChart.right + labelMargin;
    for (float y = 1; y < maxY * (maxY <= 1 ? 10 : 1); y++) {
      if (maxY >= 10 && maxY < 50 && y % 5 != 0) {
        continue;
      }
      if (maxY >= 50 && maxY < 100 && y % 10 != 0) {
        continue;
      }
      if (maxY >= 100 && y % 20 != 0) {
        continue;
      }
      String label = NumUtil.trimPrice(y / (maxY <= 1 ? 10 : 1), decimalPlacesPriceDisplay);
      centerY = rectChart.bottom - y * scaleY;
      centerY = centerY + getTextHeight(paintLabel, label) / 2;
      canvas.drawText(label, centerX, centerY, paintLabel);
    }
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
            rectChart.height() - (p.y * scaleY * (maxY <= 1 ? 10 : 1))
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
    canvas.drawRoundRect(rectChart, cornerRadiusBg, cornerRadiusBg, paintChartBg);
    getRoundedRectAsPath(pathFillMask, rectChart, cornerRadiusBg, cornerRadiusBg);

    int gridCount = labels.size() - 1;
    float part = rectChart.width() / gridCount;

    for (int i = 1; i < gridCount; i++) {
      float x = rectChart.left + part * i;
      canvas.drawLine(x, rectChart.top, x, rectChart.bottom, paintGrid);
    }

    for (float y = 1; y < maxY * (maxY <= 1 ? 10 : 1); y++) {
      if (maxY >= 10 && maxY < 50 && y % 5 != 0) {
        continue;
      } else if (maxY >= 50 && maxY < 100 && y % 10 != 0) {
        continue;
      } else if (maxY >= 100 && y % 20 != 0) {
        continue;
      }
      canvas.drawLine(
          rectChart.left, rectChart.bottom - y * scaleY,
          rectChart.right, rectChart.bottom - y * scaleY,
          paintGrid
      );
    }
  }

  private static void getRoundedRectAsPath(Path target, RectF rect, float rx, float ry) {
    target.reset();
    if (rx < 0) {
      rx = 0;
    }
    if (ry < 0) {
      ry = 0;
    }
    if (rx > rect.width() / 2) {
      rx = rect.width() / 2;
    }
    if (ry > rect.height() / 2) {
      ry = rect.height() / 2;
    }
    float widthMinusCorners = (rect.width() - (2 * rx));
    float heightMinusCorners = (rect.height() - (2 * ry));

    target.moveTo(rect.right, rect.top + ry);
    target.arcTo( //top-right-corner
        rect.right - 2 * rx, rect.top, rect.right, rect.top + 2 * ry,
        0, -90,
        false
    );
    target.rLineTo(-widthMinusCorners, 0);
    target.arcTo( //top-left corner
        rect.left, rect.top, rect.left + 2*rx, rect.top + 2 * ry,
        270, -90,
        false
    );
    target.rLineTo(0, heightMinusCorners);
    target.arcTo( // bottom-left corner
        rect.left, rect.bottom - 2 * ry, rect.left + 2 * rx, rect.bottom,
        180, -90,
        false
    );
    target.rLineTo(widthMinusCorners, 0);
    target.arcTo( // bottom-right corner
        rect.right - 2 * rx, rect.bottom - 2 * ry, rect.right, rect.bottom,
        90, -90,
        false
    );
    target.rLineTo(0, -heightMinusCorners);
    target.close();
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
      pathFill.op(pathFillMask, Op.INTERSECT);
      int curveColor = curveColors.get(colorIndex).getAccentContainer();
      colorIndex++;
      if (colorIndex > curveColors.size() - 1) {
        colorIndex = 0;
      }
      paintFill.setColor(curveColor);
      paintFill.setAlpha(100);
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
      int curveColor = curveColors.get(colorIndex).getAccent();
      if (colorIndex > curveColors.size() - 1) {
        colorIndex = 0;
      }

      buildPath(pathCurve, curveList);
      paintCurve.setColor(curveColor);
      canvas.drawPath(pathCurve, paintCurve);

      drawDots(canvas, curveList, curveColor);
      drawBadge(canvas, curveLabel, curveColors.get(colorIndex));

      colorIndex++;
    }
  }

  private void drawDots(Canvas canvas, ArrayList<Point> curveList, int color) {
    paintDot.setColor(color);
    for (Point point : curveList) {
      canvas.drawCircle(point.x, point.y, dotRadius, paintDot);
    }
  }

  private void drawBadge(Canvas canvas, String text, ColorRoles colorRoles) {
    Rect textBounds = new Rect();
    paintBadgeText.getTextBounds(text, 0, text.length(), textBounds);
    textBounds.inset(-badgePadding, 0);

    rectBadge.top = rectChart.top + badgeMargin;
    rectBadge.bottom = rectBadge.top + badgeHeight;

    if (lastXLeftBadge == 0) {
      lastXLeftBadge = rectChart.right;
    }
    rectBadge.right = lastXLeftBadge - badgeMargin;
    rectBadge.left = rectBadge.right - textBounds.width();
    lastXLeftBadge = rectBadge.left;

    float textX = rectBadge.left + badgePadding;
    float textY = rectBadge.centerY() + textBounds.height() / 2f;

    paintBadge.setColor(colorRoles.getAccent());
    canvas.drawRoundRect(rectBadge, cornerRadiusBadge, cornerRadiusBadge, paintBadge);
    paintBadgeText.setColor(colorRoles.getOnAccent());
    canvas.drawText(text, textX, textY, paintBadgeText);
  }

  public float getTextHeight(Paint textPaint, String text) {
    textPaint.getTextBounds(text, 0, text.length(), rectMeasure);
    return rectMeasure.height();
  }

  public int getTextWidth(Paint textPaint, String text) {
    textPaint.getTextBounds(text, 0, text.length(), rectMeasure);
    return rectMeasure.width();
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
      Collections.sort(curveList);
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
    if (isRtl) {
      rectChart.right -= dotRadius;
      rectChart.left += paddingEnd;
    } else {
      rectChart.left += dotRadius;
      rectChart.right -= paddingEnd;
    }

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
}

