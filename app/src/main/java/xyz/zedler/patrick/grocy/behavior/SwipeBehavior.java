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
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.behavior;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;

public abstract class SwipeBehavior extends ItemTouchHelper.SimpleCallback {

  private final Context context;
  private final GestureDetector gestureDetector;
  private final Map<Integer, List<UnderlayButton>> buttonsBuffer;
  private final Queue<Integer> recoverQueue;
  private final int buttonWidth;
  private final OnSwipeListener onSwipeListener;
  private RecyclerView recyclerView;
  private List<UnderlayButton> buttons;
  private boolean swiping = false;
  private int swipedPos = -1;
  private float swipeThreshold = 0.5f;
  private final Paint paintBg, paintDivider;
  private final int colorBg, colorBgSwipe, colorDivider;

  private final View.OnTouchListener onTouchListener = new View.OnTouchListener() {
    @Override
    public boolean onTouch(View view, MotionEvent e) {
      view.performClick();

      if (e.getAction() == MotionEvent.ACTION_MOVE && !swiping) {
        swiping = true;
        if (onSwipeListener != null) {
          onSwipeListener.onSwipeStartedOrEnded(true);
        }
      } else if (e.getAction() == MotionEvent.ACTION_UP && swiping) {
        swiping = false;
        if (onSwipeListener != null) {
          onSwipeListener.onSwipeStartedOrEnded(false);
        }
      }

      if (swipedPos < 0) {
        return false;
      }
      Point point = new Point((int) e.getRawX(), (int) e.getRawY());

      RecyclerView.ViewHolder swipedViewHolder
          = recyclerView.findViewHolderForAdapterPosition(swipedPos);
      if (swipedViewHolder == null) {
        return false;
      }
      View swipedItem = swipedViewHolder.itemView;
      Rect rect = new Rect();
      swipedItem.getGlobalVisibleRect(rect);

      if (e.getAction() == MotionEvent.ACTION_DOWN
          || e.getAction() == MotionEvent.ACTION_UP
          || e.getAction() == MotionEvent.ACTION_MOVE
      ) {
        if (rect.top < point.y && rect.bottom > point.y) {
          gestureDetector.onTouchEvent(e);
        } else {
          recoverQueue.add(swipedPos);
          swipedPos = -1;
          recoverSwipedItem();
        }
      }
      return false;
    }
  };

  protected SwipeBehavior(Context context, OnSwipeListener onSwipeListener) {
    super(0, ItemTouchHelper.RIGHT);
    this.context = context;
    buttons = new ArrayList<>();
    buttonWidth = UiUtil.dpToPx(context, UiUtil.isOrientationPortrait(context) ? 72 : 64);

    colorBg = ResUtil.getColorAttr(context, android.R.attr.colorBackground);
    colorBgSwipe = ResUtil.getColorAttr(context, R.attr.colorPrimary);
    colorDivider = ResUtil.getColorAttr(context, R.attr.colorOutlineVariant);

    paintBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintBg.setColor(colorBg);
    paintDivider = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintDivider.setColor(colorBg);

    GestureDetector.SimpleOnGestureListener gestureListener
        = new GestureDetector.SimpleOnGestureListener() {
      @Override
      public boolean onSingleTapConfirmed(MotionEvent e) {
        for (UnderlayButton button : buttons) {
          if (button.onClick(e.getX(), e.getY())) {
            break;
          }
        }
        return true;
      }
    };
    gestureDetector = new GestureDetector(context, gestureListener);
    buttonsBuffer = new HashMap<>();
    recoverQueue = new LinkedList<>() {
      @Override
      public boolean add(Integer o) {
        if (contains(o)) {
          return false;
        } else {
          return super.add(o);
        }
      }
    };
    this.onSwipeListener = onSwipeListener;
  }

  @Override
  public boolean onMove(
      @NonNull RecyclerView recyclerView,
      @NonNull RecyclerView.ViewHolder viewHolder,
      @NonNull RecyclerView.ViewHolder target
  ) {
    return false;
  }

  @Override
  public int getSwipeDirs(
      @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder
  ) {
    List<UnderlayButton> buffer = new ArrayList<>();
    instantiateUnderlayButton(viewHolder, buffer);
    if (buffer.isEmpty()) {
      return 0;
    }
    return super.getSwipeDirs(recyclerView, viewHolder);
  }

  @Override
  public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
    int pos = viewHolder.getAdapterPosition();

    if (swiping) {
      swiping = false;
      if (onSwipeListener != null) {
        onSwipeListener.onSwipeStartedOrEnded(false);
      }
    }

    if (swipedPos != pos) {
      recoverQueue.add(swipedPos);
    }

    swipedPos = pos;

    if (buttonsBuffer.containsKey(swipedPos)) {
      buttons = buttonsBuffer.get(swipedPos);
    } else {
      buttons.clear();
    }

    buttonsBuffer.clear();
    swipeThreshold = 0.5f * buttons.size() * buttonWidth;
    recoverSwipedItem();
  }

  @Override
  public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
    return swipeThreshold;
  }

  @Override
  public float getSwipeEscapeVelocity(float defaultValue) {
    return 0.1f * defaultValue;
  }

  @Override
  public float getSwipeVelocityThreshold(float defaultValue) {
    return 5.0f * defaultValue;
  }

  @Override
  public void onChildDraw(
      @NonNull Canvas canvas,
      @NonNull RecyclerView recyclerView,
      RecyclerView.ViewHolder viewHolder,
      float dX, float dY,
      int actionState,
      boolean isCurrentlyActive
  ) {
    int pos = viewHolder.getAdapterPosition();
    float translationX = dX;
    View itemView = viewHolder.itemView;

    if (pos < 0) {
      swipedPos = pos;
      return;
    }

    if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
      if (dX > 0) {
        List<UnderlayButton> buffer = new ArrayList<>();

        if (!buttonsBuffer.containsKey(pos)) {
          instantiateUnderlayButton(viewHolder, buffer);
          buttonsBuffer.put(pos, buffer);
        } else {
          buffer = buttonsBuffer.get(pos);
        }
        assert buffer != null;

        translationX = dX * buffer.size() * buttonWidth / itemView.getWidth();

        /*int limit = UnitUtil.getDp(context, 24);
        if(translationX < limit && itemView.getElevation() > 0) {
            itemView.setElevation(UnitUtil.getDp(context, 2) * (translationX / limit));
        } else if(isCurrentlyActive) {
            itemView.setElevation(UnitUtil.getDp(context, 2));
        } else {
            itemView.setElevation(0);
        }*/
        itemView.setElevation(0);

        drawButtons(canvas, itemView, buffer, pos, translationX);
      } else if (swipedPos == pos && dX == 0) {
        swipedPos = -1;
        itemView.setElevation(0);
      }
    }

    super.onChildDraw(
        canvas, recyclerView, viewHolder, translationX, dY, actionState, isCurrentlyActive
    );
  }

  private synchronized void recoverSwipedItem() {
    while (!recoverQueue.isEmpty()) {
      int pos = recoverQueue.remove();
      if (pos > -1) {
        if (recyclerView.getAdapter() != null) {
          recyclerView.getAdapter().notifyItemChanged(pos);
        }
      }
    }
  }

  public void recoverLatestSwipedItem() {
    if (swipedPos != -1 && recoverQueue.isEmpty() && recyclerView.getAdapter() != null) {
      recyclerView.getAdapter().notifyItemChanged(swipedPos);
      swipedPos = -1;
    }
  }

  private void drawButtons(
      Canvas canvas, View itemView, List<UnderlayButton> buttons, int pos, float dX
  ) {
    if (dX < UiUtil.dpToPx(context, 24)) {
      if (dX > 0) {
        float friction = dX / UiUtil.dpToPx(context, 24);
        paintBg.setColor(ColorUtils.blendARGB(colorBg, colorBgSwipe, friction));
        paintDivider.setColor(ColorUtils.blendARGB(colorBg, colorDivider, friction));
      } else {
        paintBg.setColor(colorBg);
        paintDivider.setColor(colorBg);
      }
    } else {
      paintBg.setColor(colorBgSwipe);
      paintDivider.setColor(colorDivider);
    }

    // draw background

    canvas.drawRect(
        itemView.getLeft(), itemView.getTop(), itemView.getRight(), itemView.getBottom(),
        paintBg
    );

    // draw dividers at top and bottom starting at the current horizontal offset

    float dividerLeft = itemView.getLeft() + dX + UiUtil.dpToPx(context, 8);
    int gradientWidth = UiUtil.dpToPx(context, 16);
    int strokeWidth = UiUtil.dpToPx(context, 1);

    paintDivider.setShader(
        new LinearGradient(
            dividerLeft, 0, dividerLeft + gradientWidth, 0,
            Color.TRANSPARENT, paintDivider.getColor(),
            Shader.TileMode.CLAMP
        )
    );
    canvas.drawRect(
        dividerLeft, itemView.getTop(), itemView.getRight(), itemView.getTop() + strokeWidth,
        paintDivider
    );
    canvas.drawRect(
        dividerLeft, itemView.getBottom() - strokeWidth,
        itemView.getRight(), itemView.getBottom(),
        paintDivider
    );

    // draw actions

    float buttonLeft = itemView.getLeft();
    for (int i = 0; i < buttons.size(); i++) {
      float right = buttonLeft + buttonWidth;
      buttons.get(i).draw(
          canvas,
          new RectF(buttonLeft, itemView.getTop(), right, itemView.getBottom()),
          i,
          buttons.size(),
          pos
      );
      buttonLeft = right;
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  public void attachToRecyclerView(RecyclerView recyclerView) {
    this.recyclerView = recyclerView;
    this.recyclerView.setOnTouchListener(onTouchListener);
    ItemTouchHelper itemTouchHelper = new ItemTouchHelper(this);
    itemTouchHelper.attachToRecyclerView(this.recyclerView);
  }

  public abstract void instantiateUnderlayButton(
      RecyclerView.ViewHolder viewHolder,
      List<UnderlayButton> underlayButtons
  );

  public static class UnderlayButton {

    private final Drawable drawable;
    private final int iconSize, bgSize, bgCornerRadius, buttonDistance;
    private int pos;
    private RectF clickRegion;
    private final UnderlayButtonClickListener clickListener;
    private Paint paintButton;

    public UnderlayButton(
        Context context, @DrawableRes int resId, UnderlayButtonClickListener clickListener
    ) {
      this.clickListener = clickListener;

      iconSize = UiUtil.dpToPx(context, 24);
      bgSize = UiUtil.dpToPx(
          context, context.getResources().getInteger(R.integer.swipe_action_bg_size)
      );
      bgCornerRadius = UiUtil.dpToPx(context, 16);
      buttonDistance = UiUtil.dpToPx(context, 4);

      drawable = ResourcesCompat.getDrawable(context.getResources(), resId, null);
      if (drawable == null) {
        return;
      }
      drawable.setColorFilter(ResUtil.getColorAttr(context, R.attr.colorOnPrimary), Mode.SRC_ATOP);

      paintButton = new Paint(Paint.ANTI_ALIAS_FLAG);
      paintButton.setColor(ResUtil.getColorAttr(context, R.attr.colorOnPrimary, 0.08f));
    }

    private boolean onClick(float x, float y) {
      if (clickRegion != null && clickRegion.contains(x, y)) {
        clickListener.onClick(pos);
        return true;
      }
      return false;
    }

    private void draw(Canvas canvas, RectF rect, int nr, int count, int pos) {
      // push actions towards each other if there are two
      float offsetX = count == 2
          ? (nr == 0 ? buttonDistance : -buttonDistance)
          : 0;

      // DRAW ROUND BACKGROUND

      //canvas.drawCircle(rect.centerX() + offsetX, rect.centerY(), bgSize / 2f, paintBg);
      float centerX = rect.centerX() + offsetX;
      float centerY = rect.centerY();
      int sizeHalf = bgSize / 2;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        canvas.drawRoundRect(
                centerX - sizeHalf,
                centerY - sizeHalf,
                centerX + sizeHalf,
                centerY + sizeHalf,
                bgCornerRadius, bgCornerRadius,
                paintButton
        );
      } else {
        canvas.drawRect(
                centerX - sizeHalf,
                centerY - sizeHalf,
                centerX + sizeHalf,
                centerY + sizeHalf,
                paintButton
        );
      }

      // DRAW ICON

      if (drawable != null) {
        drawable.setBounds(
            (int) (rect.centerX() - iconSize / 2f + offsetX),
            (int) (rect.top + (rect.bottom - rect.top - iconSize) / 2),
            (int) ((rect.centerX() - iconSize / 2f + offsetX) + iconSize),
            (int) ((rect.top + (rect.bottom - rect.top - iconSize) / 2) + iconSize)
        );
        drawable.draw(canvas);
      }

      clickRegion = rect;
      this.pos = pos;
    }
  }

  public interface UnderlayButtonClickListener {

    void onClick(int position);
  }

  public interface OnSwipeListener {

    void onSwipeStartedOrEnded(boolean started);
  }
}
