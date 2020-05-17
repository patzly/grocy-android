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
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class BezierCurveChart extends View {
    public static class Point {
        static final Comparator<Point> X_COMPARATOR =
                (lhs, rhs) -> (int) (lhs.x - rhs.x);

        float x;
        float y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }

        private Point() {}

        @NonNull
        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    private static final float CURVE_LINE_WIDTH = 4f;
    private static final float CORNERS = 30;
    private static final float BADGE_HEIGHT = 10; // percentage of chartRect height
    private static final float BADGE_MARGIN = 5;
    private static final float BADGE_FONT_SIZE = 70; // 100 means font fills badge with height
    private static final float X_LABEL_OFFSET_Y = 16;
    private static final float Y_LABEL_OFFSET_X = 12;
    private static final float DOT_RADIUS = 8;

    private static final String TAG = BezierCurveChart.class.getSimpleName();

    private Paint chartBgPaint = new Paint();
    // The rect of chart, x labels on the bottom are not included
    private RectF chartRect = new RectF();
    private Paint curvePaint = new Paint();
    private Path curvePath = new Path();
    private Paint dotPaint = new Paint();
    private Paint fillPaint = new Paint();
    private Path fillPath = new Path();

    private Paint gridPaint = new Paint();
    private Paint labelPaint = new Paint();
    private ArrayList<String> labels;

    private HashMap<String, ArrayList<Point>> curveLists;
    private HashMap<String, ArrayList<Point>> adjustedCurveLists = new HashMap<>();
    private Rect drawingRect = new Rect();
    private Paint badgePaint = new Paint();
    private Paint badgeTextPaint = new Paint();
    private float lastXLeftBadge = 0;
    float maxY = 0;
    float scaleY;

    {
        Typeface customTypeface = ResourcesCompat.getFont(getContext(), R.font.roboto_mono_regular);

        curvePaint.setStyle(Paint.Style.STROKE);
        curvePaint.setStrokeCap(Paint.Cap.ROUND);
        curvePaint.setStrokeWidth(CURVE_LINE_WIDTH);
        curvePaint.setAntiAlias(true);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setAntiAlias(true);

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);
        fillPaint.setPathEffect(new CornerPathEffect(CORNERS));

        chartBgPaint.setStyle(Paint.Style.FILL);
        chartBgPaint.setColor(ResourcesCompat.getColor(getResources(), R.color.on_surface_secondary, null));
        chartBgPaint.setAntiAlias(true);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeCap(Paint.Cap.SQUARE);
        gridPaint.setColor(Color.argb(0x30, 0xD0, 0xD0, 0xD0));
        gridPaint.setAntiAlias(true);
        gridPaint.setStrokeWidth(3.0f);

        badgePaint.setStyle(Paint.Style.FILL);
        badgePaint.setAntiAlias(true);

        badgeTextPaint.setColor(Color.BLACK);
        badgeTextPaint.setAntiAlias(true);
        badgeTextPaint.setTypeface(customTypeface);

        labelPaint.setColor(ResourcesCompat.getColor(getResources(), R.color.on_background_secondary, null));
        labelPaint.setTextSize(36f);
        labelPaint.setTypeface(customTypeface);
        labelPaint.setAntiAlias(true);
    }

    public BezierCurveChart(Context context) {
        super(context);
    }

    public BezierCurveChart(Context context, AttributeSet attrs) {
        super(context, attrs);
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

                Point newPoint = new Point();
                newPoint.x = (p.x - startX) * chartWidth / axesSpan + chartRect.left;

                newPoint.y = p.y * scaleY;
                newPoint.y = chartHeight - newPoint.y;

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
        //Important!
        path.reset();

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
        dotPaint.setColor(color);
        for(Point point : curveList) {
            canvas.drawCircle(point.x, point.y, DOT_RADIUS, dotPaint);
        }
    }

    private void drawCurve(Canvas canvas) {

        ArrayList<Integer> curveColors = new ArrayList<>();
        curveColors.add(ResourcesCompat.getColor(getResources(), R.color.retro_blue_light, null));
        curveColors.add(ResourcesCompat.getColor(getResources(), R.color.retro_yellow_light, null));
        curveColors.add(ResourcesCompat.getColor(getResources(), R.color.retro_red_light, null));
        curveColors.add(ResourcesCompat.getColor(getResources(), R.color.retro_dirt_dark, null)); // TODO: light variants
        curveColors.add(ResourcesCompat.getColor(getResources(), R.color.retro_green_dark, null));
        int colorIndex = 0;

        for(String curveLabel : adjustedCurveLists.keySet()) {
            ArrayList<Point> curveList = adjustedCurveLists.get(curveLabel);
            assert curveList != null;
            buildPath(curvePath, curveList);
            buildPath(fillPath, curveList);
            fillPath.lineTo(curveList.get(curveList.size()-1).x, chartRect.bottom);
            fillPath.lineTo(curveList.get(0).x, chartRect.bottom);
            fillPath.lineTo(curveList.get(0).x, curveList.get(0).y);
            fillPath.close();
            int curveColor = curveColors.get(colorIndex);
            colorIndex++;
            if(colorIndex > curveColors.size()-1) {
                colorIndex = 0;
            }
            fillPaint.setColor(curveColor);
            fillPaint.setAlpha(40);
            curvePaint.setColor(curveColor);
            curvePaint.setAlpha(230);
            canvas.drawPath(fillPath, fillPaint);
            canvas.drawPath(curvePath, curvePaint);
            drawBadge(canvas, curveLabel, curveColor);
            drawDots(canvas, curveList, curveColor);
        }
    }

    private void drawGrid(Canvas canvas, int width) {

        canvas.drawRoundRect(chartRect, CORNERS, CORNERS, chartBgPaint);

        int gridCount = labels.size() - 1;
        float part = (float) width / gridCount;

        for(int i = 1; i < gridCount; i++) {
            float x = chartRect.left + part * i;
            canvas.drawLine(x, chartRect.top, x, chartRect.bottom, gridPaint);
        }

        for(float y = 1; y < maxY; y++) {
            if(maxY >= 10 && maxY < 50 && y % 5 != 0) continue;
            if(maxY >= 50 && maxY < 100 && y % 10 != 0) continue;
            if(maxY >= 100 && y % 20 != 0) continue;
            canvas.drawLine(chartRect.left,
                    chartRect.bottom - y * scaleY,
                    chartRect.right,
                    chartRect.bottom - y * scaleY, gridPaint
            );
        }
    }

    private void drawLabels(Canvas canvas) {

        // Y-AXIS

        // Y coordinate scale
        float maxWidth = 0;
        for(String label : labels) {
            float labelWidth = getTextWidth(labelPaint, label);
            if(labelWidth > maxWidth) maxWidth = labelWidth;
        }
        int newBottom = (int) (chartRect.bottom - maxWidth - X_LABEL_OFFSET_Y);
        int newChartHeight = (int) (newBottom - chartRect.top);
        scaleY = newChartHeight / maxY * (1 - BADGE_HEIGHT / 100 - BADGE_MARGIN * 2 / 100);

        maxWidth = 0;
        for(float y = 1; y < maxY; y++) {
            if(maxY >= 10 && maxY < 50 && y % 5 != 0) continue;
            if(maxY >= 50 && maxY < 100 && y % 10 != 0) continue;
            if(maxY >= 100 && y % 20 != 0) continue;
            String s = NumUtil.trim(y);
            float labelWidth = getTextWidth(labelPaint, s);
            if(maxWidth < labelWidth) maxWidth = labelWidth;
        }

        // Move left border
        chartRect.left = (int) (chartRect.left + DOT_RADIUS);
        if(getTextWidth(labelPaint, labels.get(labels.size() - 1)) > maxWidth) {
            chartRect.right -= getTextWidth(labelPaint, labels.get(labels.size() - 1)) - 40;
        } else {
            chartRect.right -= maxWidth + 20;
        }

        float labelX = chartRect.right;
        float labelY;
        for(float y = 1; y < maxY; y++) {
            if(maxY >= 10 && maxY < 50 && y % 5 != 0) continue;
            if(maxY >= 50 && maxY < 100 && y % 10 != 0) continue;
            if(maxY >= 100 && y % 20 != 0) continue;
            String s = NumUtil.trim(y);
            float centerY = newBottom - y * scaleY;
            labelY = centerY + (getTextHeight(labelPaint) - 16) / 2;
            canvas.drawText(s, labelX + Y_LABEL_OFFSET_X, labelY, labelPaint);
        }

        // X-AXIS

        float part;
        if(labels.size() > 1) {
            part = (float) ((int) (chartRect.right - chartRect.left)) / (labels.size() - 1);
        } else {
            part = 1;
        }

        maxWidth = 0;
        for(int i = 0; i < labels.size(); i++) {
            String s = labels.get(i);
            float centerX = chartRect.left + part * i;
            float labelWidth = getTextWidth(labelPaint, s);
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
            labelY = chartRect.bottom + X_LABEL_OFFSET_Y / 2;
            labelX += X_LABEL_OFFSET_Y / 2;
            canvas.save();
            canvas.rotate(45f, centerX, newBottom);
            canvas.drawText(s, labelX, labelY - labelWidth, labelPaint);
            canvas.restore();
        }
        chartRect.bottom = (int) (chartRect.bottom - maxWidth - X_LABEL_OFFSET_Y);
    }

    private void drawBadge(Canvas canvas, String text, int color) {

        float badgeHeight = chartRect.height() * BADGE_HEIGHT / 100;
        float badgeTop = chartRect.top + chartRect.height() * BADGE_MARGIN / 100;
        float badgeBottom = badgeTop + badgeHeight;
        float badgeMargin = chartRect.height() * BADGE_MARGIN / 100;
        float badgeY = badgeTop + badgeHeight / 2;

        Rect textBounds = new Rect();
        badgeTextPaint.setTextSize(badgeHeight * BADGE_FONT_SIZE / 100);
        badgeTextPaint.getTextBounds(text, 0, 1, textBounds);
        float textWidth = getTextWidth(badgeTextPaint, text);
        float textHeight = textBounds.bottom - textBounds.top;
        float textY = badgeY + textHeight / 2;

        if(lastXLeftBadge == 0) lastXLeftBadge = chartRect.right;
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

        float radius = BADGE_HEIGHT / 2;

        badgePaint.setColor(color);
        canvas.drawRoundRect(badgeRect, radius, radius, badgePaint);
        canvas.drawText(text, textX, textY, badgeTextPaint);
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
        getDrawingRect(drawingRect);

        chartRect.bottom = drawingRect.bottom;
        chartRect.top = drawingRect.top;
        chartRect.left = drawingRect.left;
        chartRect.right = drawingRect.right;

        Log.d(TAG, chartRect.toString());

        if (curveLists != null) {

            drawLabels(canvas);

            int chartHeight = (int) (chartRect.bottom - chartRect.top);
            int chartWidth = (int) (chartRect.right - chartRect.left);

            adjustPoints(chartWidth, chartHeight);

            drawGrid(canvas, chartWidth);

            lastXLeftBadge = 0;
            drawCurve(canvas);

            //canvas.drawRoundRect(chartRect, CORNERS, CORNERS, borderPaint);
        }
    }
}

