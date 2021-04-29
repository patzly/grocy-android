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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.behavior;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.BitmapUtil;
import xyz.zedler.patrick.grocy.util.UnitUtil;

public abstract class SwipeBehavior extends ItemTouchHelper.SimpleCallback {

  private final Context context;
  private RecyclerView recyclerView;
  private List<UnderlayButton> buttons;
  private final GestureDetector gestureDetector;
  private int swipedPos = -1;
  private float swipeThreshold = 0.5f;
  private final Map<Integer, List<UnderlayButton>> buttonsBuffer;
  private final Queue<Integer> recoverQueue;
  private final int buttonWidth;
  private final OnSwipeListener onSwipeListener;
  private boolean swiping = false;

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
    this.buttons = new ArrayList<>();
    buttonWidth = UnitUtil.getDp(context, 66);
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
    this.gestureDetector = new GestureDetector(context, gestureListener);
    buttonsBuffer = new HashMap<>();
    recoverQueue = new LinkedList<Integer>() {
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
      @NonNull RecyclerView recyclerView,
      @NonNull RecyclerView.ViewHolder viewHolder
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
    assert buttons != null;
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
      @NonNull Canvas c,
      @NonNull RecyclerView recyclerView,
      RecyclerView.ViewHolder viewHolder,
      float dX,
      float dY,
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

        drawButtons(c, itemView, buffer, pos, translationX);
      } else if (swipedPos == pos && dX == 0) {
        swipedPos = -1;
        itemView.setElevation(0);
      }
    }

    super.onChildDraw(
        c,
        recyclerView,
        viewHolder,
        translationX,
        dY,
        actionState,
        isCurrentlyActive
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
      Canvas canvas,
      View itemView,
      List<UnderlayButton> buttons,
      int pos,
      float dX
  ) {
    float left = itemView.getLeft();

    Paint paint = new Paint();
    paint.setColor(ContextCompat.getColor(context, R.color.secondary));
    if (dX < UnitUtil.getDp(context, 24)) {
      if (dX > 0) {
        float friction = dX / UnitUtil.getDp(context, 24);
        paint.setAlpha((int) (255 * friction));
      } else {
        paint.setAlpha(0);
      }
    } else {
      paint.setAlpha(255);
    }

    // draw background

    canvas.drawRect(
        left,
        itemView.getTop(),
        itemView.getRight(),
        itemView.getBottom(),
        paint
    );

    // draw actions

    float buttonLeft = left;
    for (int i = 0; i < buttons.size(); i++) {
      float right = buttonLeft + buttonWidth;
      buttons.get(i).onDraw(
          recyclerView.getContext(),
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

  public interface UnderlayButtonClickListener {

    void onClick(int position);
  }

  public static class UnderlayButton {

    private final int resId;
    private int pos;
    private RectF clickRegion;
    private final UnderlayButtonClickListener clickListener;

    public UnderlayButton(
        @DrawableRes int resId,
        UnderlayButtonClickListener clickListener
    ) {
      this.resId = resId;
      this.clickListener = clickListener;
    }

    boolean onClick(float x, float y) {
      if (clickRegion != null && clickRegion.contains(x, y)) {
        clickListener.onClick(pos);
        return true;
      }
      return false;
    }

    void onDraw(
        Context context,
        Canvas canvas,
        RectF rect,
        int nr,
        int count,
        int pos
    ) {
      // push actions towards each other if there are two
      float offsetX = count == 2
          ? nr == 0 ? UnitUtil.getDp(context, 4) : -UnitUtil.getDp(context, 4)
          : 0;

      // DRAW ROUND BACKGROUND

      Paint paintBg = new Paint(Paint.ANTI_ALIAS_FLAG);
      paintBg.setColor(Color.BLACK);
      paintBg.setAlpha(20);
      canvas.drawCircle(
          rect.centerX() + offsetX,
          rect.centerY(),
          UnitUtil.getDp(
              context,
              context.getResources().getInteger(R.integer.swipe_action_bg_radius)
          ),
          paintBg
      );

      // DRAW ICON

      Paint paintIcon = new Paint();
      paintIcon.setColorFilter(
          new PorterDuffColorFilter(
              ContextCompat.getColor(context, R.color.on_secondary),
              PorterDuff.Mode.SRC_IN
          )
      );
      Bitmap icon = BitmapUtil.getFromDrawable(context, resId);
      if (icon != null) {
        canvas.drawBitmap(
            icon,
            rect.centerX() - icon.getWidth() / 2f + offsetX,
            rect.top + (rect.bottom - rect.top - icon.getHeight()) / 2,
            paintIcon
        );
      }

      clickRegion = rect;
      this.pos = pos;
    }
  }

  public interface OnSwipeListener {

    void onSwipeStartedOrEnded(boolean started);
  }
}
