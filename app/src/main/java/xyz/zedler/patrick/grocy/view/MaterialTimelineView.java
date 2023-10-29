package xyz.zedler.patrick.grocy.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class MaterialTimelineView extends ConstraintLayout {

  // SOURCE: https://github.com/hypeapps/MaterialTimelineView/tree/master

  public static final int TIMELINE_TYPE_HEADER = 0;
  public static final int TIMELINE_TYPE_ITEM = 1;
  public static final int POSITION_FIRST = 0;
  public static final int POSITION_MIDDLE = 1;
  public static final int POSITION_LAST = 2;
  public static final int POSITION_SINGLE = 3;
  private static final int DEFAULT_RADIO_RADIUS_DP = 8;
  private static final int DEFAULT_RADIO_INLINE_RADIUS_DP = 4;
  private static final int DEFAULT_RADIO_MARGIN_START_DP = 44;
  private static final int DEFAULT_HEADER_CONTENT_PADDING_DP = 32;
  private static final int DEFAULT_ENTRY_CONTENT_PADDING_DP = 16;
  private static final float LINE_WIDTH_MODIFIER = 0.3f;

  private int position = POSITION_FIRST;
  private int timelineType = TIMELINE_TYPE_ITEM;
  private float radioRadius;
  private float radioInlineRadius;
  private float radioMarginStart;
  private int headerContentPadding;
  private int entryContentPadding;
  private int radioColor = Color.WHITE;
  private int lineColor = Color.WHITE;
  private boolean showLineAndRadio = true;

  private final Paint paint = new Paint();
  private final PorterDuffXfermode xfermodeClear = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);

  public MaterialTimelineView(Context context) {
    this(context, null);
  }

  public MaterialTimelineView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public MaterialTimelineView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    radioRadius = UiUtil.dpToPxFloat(context, DEFAULT_RADIO_RADIUS_DP);
    radioInlineRadius = UiUtil.dpToPxFloat(context, DEFAULT_RADIO_INLINE_RADIUS_DP);
    radioMarginStart = UiUtil.dpToPxFloat(context, DEFAULT_RADIO_MARGIN_START_DP);
    headerContentPadding = UiUtil.dpToPx(context, DEFAULT_HEADER_CONTENT_PADDING_DP);
    entryContentPadding = UiUtil.dpToPx(context, DEFAULT_ENTRY_CONTENT_PADDING_DP);

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
        drawLine(canvas, x - radioRadius * LINE_WIDTH_MODIFIER, yRadio, x + radioRadius * LINE_WIDTH_MODIFIER, yLine);
        headerContentPaddingBottom = headerContentPadding;
        break;
      case POSITION_MIDDLE:
        yLine = getHeight();
        drawLine(canvas, x - radioRadius * LINE_WIDTH_MODIFIER, 0, x + radioRadius * LINE_WIDTH_MODIFIER, yLine);
        headerContentPaddingTop = headerContentPadding;
        headerContentPaddingBottom = headerContentPadding;
        entryContentPaddingTop = entryContentPadding;
        break;
      case POSITION_LAST:
        if (timelineType == TIMELINE_TYPE_HEADER) {
          yRadio = getHeight() / 2f + headerContentPadding / 2f;
        }
        drawLine(canvas, x - radioRadius * LINE_WIDTH_MODIFIER, 0, x + radioRadius * LINE_WIDTH_MODIFIER, yRadio);
        headerContentPaddingTop = headerContentPadding;
        entryContentPaddingTop = entryContentPadding;
        break;
    }
    drawRadio(canvas, radioColor, x, yRadio);

    if (timelineType == TIMELINE_TYPE_HEADER) {
      drawLine(canvas, x, yRadio - radioRadius * LINE_WIDTH_MODIFIER, x + radioRadius * 2, yRadio + radioRadius * LINE_WIDTH_MODIFIER);
      drawRadioOutline(canvas, x, yRadio);
      setPadding(getPaddingLeft(), headerContentPaddingTop, getPaddingRight(), headerContentPaddingBottom);
    } else {
      setPadding(getPaddingLeft(), entryContentPaddingTop, getPaddingRight(), entryContentPadding);
    }
  }

  private void drawRadioOutline(Canvas canvas, float x, float y) {
    paint.setXfermode(xfermodeClear);
    canvas.drawCircle(x, y, radioInlineRadius, paint);
    paint.setXfermode(null);
  }

  private void drawRadio(Canvas canvas, int color, float x, float y) {
    paint.setColor(color);
    canvas.drawCircle(x, y, radioRadius, paint);
  }

  private void drawLine(Canvas canvas, float left, float top, float right, float bottom) {
    paint.setColor(lineColor);
    canvas.drawRect(left, top, right, bottom, paint);
  }

  private void initAttrs(TypedArray typedArray) {
    this.position = typedArray.getInteger(R.styleable.MaterialTimelineView_timeline_position, POSITION_FIRST);
    this.timelineType = typedArray.getInteger(R.styleable.MaterialTimelineView_timeline_type, TIMELINE_TYPE_HEADER);
    this.radioRadius = UiUtil.dpToPxFloat(getContext(),
        typedArray.getDimension(R.styleable.MaterialTimelineView_timeline_radio_radius, DEFAULT_RADIO_RADIUS_DP));
    this.radioInlineRadius = UiUtil.dpToPxFloat(getContext(),
        typedArray.getDimension(R.styleable.MaterialTimelineView_timeline_radio_inline_radius, DEFAULT_RADIO_INLINE_RADIUS_DP));
    this.radioMarginStart = UiUtil.dpToPxFloat(getContext(), typedArray.getDimension(
        R.styleable.MaterialTimelineView_timeline_margin_start, DEFAULT_RADIO_MARGIN_START_DP));
    this.radioColor = typedArray.getColor(R.styleable.MaterialTimelineView_timeline_radio_color, Color.WHITE);
    this.lineColor = typedArray.getColor(R.styleable.MaterialTimelineView_timeline_line_color, Color.WHITE);
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public void setShowLineAndRadio(boolean showLineAndRadio) {
    this.showLineAndRadio = showLineAndRadio;
  }
}
