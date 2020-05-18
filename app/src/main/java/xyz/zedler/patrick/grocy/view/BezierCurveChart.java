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
import android.util.Log;
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

    private static final String TAG = BezierCurveChart.class.getSimpleName();

    private static final float CURVE_LINE_WIDTH = 4f;
    private static final float BADGE_HEIGHT = 10; // percentage of chartRect height
    private static final float BADGE_MARGIN = 5;
    private static final float BADGE_FONT_SIZE = 70; // 100 means font fills badge with height
    private static final float X_LABEL_OFFSET_Y = 16;
    private static final float Y_LABEL_OFFSET_X = 12;
    private static final float DOT_RADIUS = 8;

    public static class Point {
        float x;
        float y;

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

    private Context context;
    private Paint paintChartBg = new Paint();
    private Paint paintCurve = new Paint();
    private Paint paintDot = new Paint();
    private Paint paintFill = new Paint();
    private Paint paintGrid = new Paint();
    private Paint paintLabel = new Paint();
    private Paint paintBadge = new Paint();
    private Paint paintBadgeText = new Paint();
    private Path pathCurve = new Path();
    private Path pathFill = new Path();
    private RectF rectChart = new RectF(); // The rect of chart, x labels on bottom not included
    private Rect rectDrawing = new Rect();
    private ArrayList<String> labels;
    private HashMap<String, ArrayList<Point>> curveLists;
    private HashMap<String, ArrayList<Point>> adjustedCurveLists = new HashMap<>();
    private float lastXLeftBadge = 0;
    private float maxY = 0;
    private float scaleY;
    private int cornerRadius;

    public BezierCurveChart(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        cornerRadius = UnitUtil.getDp(context, 12);

        paintCurve.setStyle(Paint.Style.STROKE);
        paintCurve.setStrokeCap(Paint.Cap.ROUND);
        paintCurve.setStrokeWidth(CURVE_LINE_WIDTH);
        paintCurve.setAntiAlias(true);

        paintDot.setStyle(Paint.Style.FILL);
        paintDot.setAntiAlias(true);

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setAntiAlias(true);
        paintFill.setPathEffect(new CornerPathEffect(cornerRadius));

        paintChartBg.setStyle(Paint.Style.FILL);
        paintChartBg.setColor(getColor(R.color.on_surface_secondary));
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
        paintBadgeText.setTextSize(UnitUtil.getSp(context, 14));
        paintBadgeText.setTypeface(ResourcesCompat.getFont(context, R.font.roboto_mono_medium));

        paintLabel.setColor(getColor(R.color.on_background_secondary));
        paintLabel.setTextSize(UnitUtil.getSp(context, 13));
        paintLabel.setTypeface(ResourcesCompat.getFont(context, R.font.roboto_mono_regular));
        paintLabel.setAntiAlias(true);
    }

    private void adjustPoints(int chartWidth, int chartHeight) {
        float minX = 0;
        float maxX = 0;

        for(ArrayList<Point> curveList : curveLists.values()) {
            for(Point p : curveList) {
                if(p.x < minX) {
                    minX = p.x;
                }
                if(p.x > maxX) {
                    maxX = p.x;
                }
            }
        }

        float axesSpan = maxX - minX;
        if(axesSpan == 0) axesSpan = 1;
        float startX = minX;

        for(String key : curveLists.keySet()) {
            ArrayList<Point> curveList = curveLists.get(key);
            assert curveList != null;
            for (int i = 0; i<curveList.size(); i++) {
                Point p = curveList.get(i);

                Point newPoint = new Point(
                        (p.x - startX) * chartWidth / axesSpan + rectChart.left,
                        chartHeight - (p.y * scaleY)
                );

                ArrayList<Point> adjustedCurveList = adjustedCurveLists.get(key);
                assert adjustedCurveList != null;
                if(i + 1 > adjustedCurveList.size()) {
                    adjustedCurveList.add(newPoint);
                } else {
                    adjustedCurveList.set(i, newPoint);
                }
            }
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

    private void drawDots(Canvas canvas, ArrayList<Point> curveList, int color) {
        paintDot.setColor(color);
        for(Point point : curveList) {
            canvas.drawCircle(point.x, point.y, DOT_RADIUS, paintDot);
        }
    }

    private void drawCurve(Canvas canvas) {
        ArrayList<Integer> curveColors = new ArrayList<>();
        curveColors.add(getColor(R.color.retro_blue_bg));
        curveColors.add(getColor(R.color.retro_yellow_bg));
        curveColors.add(getColor(R.color.retro_red_bg));
        curveColors.add(getColor(R.color.retro_dirt_bg)); // TODO: light variants
        curveColors.add(getColor(R.color.retro_green_bg_black));
        int colorIndex = 0;

        for(String curveLabel : adjustedCurveLists.keySet()) {
            ArrayList<Point> curveList = adjustedCurveLists.get(curveLabel);
            assert curveList != null;
            buildPath(pathCurve, curveList);
            buildPath(pathFill, curveList);
            pathFill.lineTo(curveList.get(curveList.size()-1).x, rectChart.bottom);
            pathFill.lineTo(curveList.get(0).x, rectChart.bottom);
            pathFill.lineTo(curveList.get(0).x, curveList.get(0).y);
            pathFill.close();
            int curveColor = curveColors.get(colorIndex);
            colorIndex++;
            if(colorIndex > curveColors.size()-1) {
                colorIndex = 0;
            }
            paintFill.setColor(curveColor);
            paintFill.setAlpha(40);
            paintCurve.setColor(curveColor);
            paintCurve.setAlpha(230);
            canvas.drawPath(pathFill, paintFill);
            canvas.drawPath(pathCurve, paintCurve);
            drawBadge(canvas, curveLabel, curveColor);
            drawDots(canvas, curveList, curveColor);
        }
    }

    private void drawGrid(Canvas canvas, int width) {
        canvas.drawRoundRect(rectChart, cornerRadius, cornerRadius, paintChartBg);

        int gridCount = labels.size() - 1;
        float part = (float) width / gridCount;

        for(int i = 1; i < gridCount; i++) {
            float x = rectChart.left + part * i;
            canvas.drawLine(x, rectChart.top, x, rectChart.bottom, paintGrid);
        }

        for(float y = 1; y < maxY; y++) {
            if(maxY >= 10 && maxY < 50 && y % 5 != 0) continue;
            if(maxY >= 50 && maxY < 100 && y % 10 != 0) continue;
            if(maxY >= 100 && y % 20 != 0) continue;
            canvas.drawLine(rectChart.left,
                    rectChart.bottom - y * scaleY,
                    rectChart.right,
                    rectChart.bottom - y * scaleY, paintGrid
            );
        }
    }

    private void drawLabels(Canvas canvas) {
        // Y-AXIS

        // Y coordinate scale
        float maxWidth = 0;
        for(String label : labels) {
            float labelWidth = getTextWidth(paintLabel, label);
            if(labelWidth > maxWidth) maxWidth = labelWidth;
        }
        int newBottom = (int) (rectChart.bottom - maxWidth - X_LABEL_OFFSET_Y);
        int newChartHeight = (int) (newBottom - rectChart.top);
        scaleY = newChartHeight / maxY * (1 - BADGE_HEIGHT / 100 - BADGE_MARGIN * 2 / 100);

        maxWidth = 0;
        for(float y = 1; y < maxY; y++) {
            if(maxY >= 10 && maxY < 50 && y % 5 != 0) continue;
            if(maxY >= 50 && maxY < 100 && y % 10 != 0) continue;
            if(maxY >= 100 && y % 20 != 0) continue;
            String s = NumUtil.trim(y);
            float labelWidth = getTextWidth(paintLabel, s);
            if(maxWidth < labelWidth) maxWidth = labelWidth;
        }

        // Move left border
        rectChart.left = (int) (rectChart.left + DOT_RADIUS);
        if(getTextWidth(paintLabel, labels.get(labels.size() - 1)) > maxWidth) {
            rectChart.right -= getTextWidth(paintLabel, labels.get(labels.size() - 1)) - 40;
        } else {
            rectChart.right -= maxWidth + 20;
        }

        float labelX = rectChart.right;
        float labelY;
        for(float y = 1; y < maxY; y++) {
            if(maxY >= 10 && maxY < 50 && y % 5 != 0) continue;
            if(maxY >= 50 && maxY < 100 && y % 10 != 0) continue;
            if(maxY >= 100 && y % 20 != 0) continue;
            String s = NumUtil.trim(y);
            float centerY = newBottom - y * scaleY;
            labelY = centerY + (getTextHeight(paintLabel) - 16) / 2;
            canvas.drawText(s, labelX + Y_LABEL_OFFSET_X, labelY, paintLabel);
        }

        // X-AXIS

        float part;
        if(labels.size() > 1) {
            part = (float) ((int) (rectChart.right - rectChart.left)) / (labels.size() - 1);
        } else {
            part = 1;
        }

        maxWidth = 0;
        for(int i = 0; i < labels.size(); i++) {
            String s = labels.get(i);
            float centerX = rectChart.left + part * i;
            float labelWidth = getTextWidth(paintLabel, s);
            if(labelWidth > maxWidth) maxWidth = labelWidth;

            if(i == 0) {
                centerX += 15;
                labelX = centerX;
            } else if(i == labels.size() - 1) {
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

    private void drawBadge(Canvas canvas, String text, int color) {
        float badgeHeight = rectChart.height() * BADGE_HEIGHT / 100;
        float badgeTop = rectChart.top + rectChart.height() * BADGE_MARGIN / 100;
        float badgeBottom = badgeTop + badgeHeight;
        float badgeMargin = rectChart.height() * BADGE_MARGIN / 100;
        float badgeY = badgeTop + badgeHeight / 2;

        Rect textBounds = new Rect();
        paintBadgeText.getTextBounds(text, 0, 1, textBounds);
        float textWidth = getTextWidth(paintBadgeText, text);
        float textHeight = textBounds.bottom - textBounds.top;
        float textY = badgeY + textHeight / 2;

        if(lastXLeftBadge == 0) lastXLeftBadge = rectChart.right;
        float badgeRight = lastXLeftBadge - badgeMargin;
        float badgeLeft = badgeRight - textWidth - badgeHeight * BADGE_FONT_SIZE / 200;
        lastXLeftBadge = badgeLeft;
        float badgeCenterX = badgeLeft + (badgeRight - badgeLeft) / 2;

        float textX = badgeCenterX - textWidth / 2;

        RectF badgeRect = new RectF();
        badgeRect.left = badgeLeft;
        badgeRect.top = badgeTop;
        badgeRect.right = badgeRight;
        badgeRect.bottom = badgeBottom;

        float radius = UnitUtil.getDp(context, 4);

        paintBadge.setColor(color);
        canvas.drawRoundRect(badgeRect, radius, radius, paintBadge);
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
        for(ArrayList<Point> curveList : curveLists.values()) {
            Collections.sort(curveList, Point.X_COMPARATOR);
        }
        // set maxY to highest y coordinate
        maxY = 0;
        for(ArrayList<Point> curveList : curveLists.values()) {
            for(Point p : curveList) {
                if(p.y > maxY) maxY = p.y;
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

        Log.d(TAG, rectChart.toString());

        if (curveLists != null) {
            drawLabels(canvas);

            int chartHeight = (int) (rectChart.bottom - rectChart.top);
            int chartWidth = (int) (rectChart.right - rectChart.left);

            adjustPoints(chartWidth, chartHeight);

            drawGrid(canvas, chartWidth);

            lastXLeftBadge = 0;
            drawCurve(canvas);

            //canvas.drawRoundRect(chartRect, CORNERS, CORNERS, borderPaint);
        }
    }

    private int getColor(@ColorRes int color) {
        return ContextCompat.getColor(context, color);
    }
}

