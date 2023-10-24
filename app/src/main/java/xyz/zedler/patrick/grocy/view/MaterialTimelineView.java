package xyz.zedler.patrick.grocy.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class MaterialTimelineView extends ConstraintLayout {

  // SOURCE: https://github.com/hypeapps/MaterialTimelineView/tree/master

  public static final int TIMELINE_TYPE_LINE = 0;
  public static final int TIMELINE_TYPE_ITEM = 1;
  public static final int POSITION_FIRST = 0;
  public static final int POSITION_MIDDLE = 1;
  public static final int POSITION_LAST = 2;
  private static final int DEFAULT_RADIO_RADIUS_DP = 12;
  private static final int DEFAULT_RADIO_OUTLINE_RADIUS_DP = 16;
  private static final int DEFAULT_RADIO_MARGIN_START_DP = 32;

  private int position = POSITION_FIRST;
  private int timelineType = TIMELINE_TYPE_LINE;
  private float radioRadius;
  private float radioOutlineRadius;
  private float radioMarginStart;
  private int topRadioColor = Color.WHITE;
  private int bottomRadioColor = Color.WHITE;
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
    radioOutlineRadius = UiUtil.dpToPxFloat(context, DEFAULT_RADIO_OUTLINE_RADIUS_DP);
    radioMarginStart = UiUtil.dpToPxFloat(context, DEFAULT_RADIO_MARGIN_START_DP);

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
    switch (timelineType) {
      case TIMELINE_TYPE_ITEM:
        drawTimelineTypeItem(canvas);
        break;
      case TIMELINE_TYPE_LINE:
        drawTimelineTypeLine(canvas);
        break;
      default:
        throw new UnsupportedOperationException("Wrong timeline item type.");
    }
  }

  private void drawTimelineTypeItem(Canvas canvas) {
    float x = radioMarginStart;
    float y = 0f;

    switch (position) {
      case POSITION_FIRST:
        y = getHeight();
        drawRadioOutline(canvas, x, y);
        drawRadio(canvas, bottomRadioColor, x, y);
        break;
      case POSITION_MIDDLE:
        drawRadioOutline(canvas, x, y);
        drawRadio(canvas, topRadioColor, x, y);
        y = getHeight();
        drawRadioOutline(canvas, x, y);
        drawRadio(canvas, bottomRadioColor, x, y);
        break;
      case POSITION_LAST:
        drawRadioOutline(canvas, x, y);
        drawRadio(canvas, topRadioColor, x, y);
        break;
    }
  }

  private void drawTimelineTypeLine(Canvas canvas) {
    float x = radioMarginStart;
    float y = getHeight();

    switch (position) {
      case POSITION_FIRST:
        drawLine(canvas, x - radioRadius / 2, radioRadius, x + radioRadius / 2, y);
        drawRadio(canvas, topRadioColor, x, radioRadius);
        drawRadio(canvas, bottomRadioColor, x, y);
        break;
      case POSITION_MIDDLE:
        drawLine(canvas, x - radioRadius / 2, 0f, x + radioRadius / 2, y);
        drawRadio(canvas, topRadioColor, x, 0f);
        drawRadio(canvas, bottomRadioColor, x, y);
        break;
      case POSITION_LAST:
        drawLine(canvas, x - radioRadius / 2, 0f, x + radioRadius / 2, y - radioRadius);
        drawRadio(canvas, topRadioColor, x, 0f);
        drawRadio(canvas, bottomRadioColor, x, y - radioRadius);
        break;
    }
  }

  private void drawRadioOutline(Canvas canvas, float x, float y) {
    paint.setXfermode(xfermodeClear);
    canvas.drawCircle(x, y, radioOutlineRadius, paint);
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
    this.timelineType = typedArray.getInteger(R.styleable.MaterialTimelineView_timeline_type, TIMELINE_TYPE_LINE);
    this.radioRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX,
        typedArray.getDimension(R.styleable.MaterialTimelineView_timeline_radio_radius, DEFAULT_RADIO_RADIUS_DP),
        getResources().getDisplayMetrics());
    this.radioOutlineRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX,
        typedArray.getDimension(R.styleable.MaterialTimelineView_timeline_radio_outline_radius, DEFAULT_RADIO_OUTLINE_RADIUS_DP),
        getResources().getDisplayMetrics());
    this.radioMarginStart = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX,
        typedArray.getDimension(R.styleable.MaterialTimelineView_timeline_margin_start, DEFAULT_RADIO_MARGIN_START_DP),
        getResources().getDisplayMetrics());
    this.topRadioColor = typedArray.getColor(R.styleable.MaterialTimelineView_timeline_top_radio_color, Color.WHITE);
    this.bottomRadioColor = typedArray.getColor(R.styleable.MaterialTimelineView_timeline_bottom_radio_color, Color.WHITE);
    this.lineColor = typedArray.getColor(R.styleable.MaterialTimelineView_timeline_line_color, Color.WHITE);
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public void setShowLineAndRadio(boolean showLineAndRadio) {
    this.showLineAndRadio = showLineAndRadio;
  }
}
