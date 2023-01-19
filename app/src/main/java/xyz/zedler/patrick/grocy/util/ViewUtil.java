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

package xyz.zedler.patrick.grocy.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat.Type;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.transition.TransitionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.elevation.SurfaceColors;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import xyz.zedler.patrick.grocy.R;

public class ViewUtil {

  private final static String TAG = ViewUtil.class.getSimpleName();

  private final long idle;
  private final LinkedList<Timestamp> timestamps;
  private long lastClick;

  private static class Timestamp {

    private final int id;
    private long time;

    public Timestamp(int id, long time) {
      this.id = id;
      this.time = time;
    }
  }

  // Prevent multiple clicks

  public ViewUtil(long minClickIdle) {
    idle = minClickIdle;
    timestamps = new LinkedList<>();
    lastClick = 0;
  }

  public ViewUtil() {
    idle = 500;
    timestamps = new LinkedList<>();
    lastClick = 0;
  }

  public boolean isClickDisabled(int id) {
    for (int i = 0; i < timestamps.size(); i++) {
      if (timestamps.get(i).id == id) {
        if (SystemClock.elapsedRealtime() - timestamps.get(i).time < idle) {
          return true;
        } else {
          timestamps.get(i).time = SystemClock.elapsedRealtime();
          return false;
        }
      }
    }
    timestamps.add(new Timestamp(id, SystemClock.elapsedRealtime()));
    return false;
  }

  @Deprecated
  public boolean isClickDisabled() {
    if (SystemClock.elapsedRealtime() - lastClick < idle) {
      return true;
    }
    lastClick = SystemClock.elapsedRealtime();
    return false;
  }

  public boolean isClickEnabled(int id) {
    return !isClickDisabled(id);
  }

  @Deprecated
  public boolean isClickEnabled() {
    return !isClickDisabled();
  }

  public void cleanUp() {
    for (Iterator<Timestamp> iterator = timestamps.iterator(); iterator.hasNext(); ) {
      Timestamp timestamp = iterator.next();
      if (SystemClock.elapsedRealtime() - timestamp.time > idle) {
        iterator.remove();
      }
    }
  }

  // Show keyboard for EditText

  public static void requestFocusAndShowKeyboard(@NonNull Window window, @NonNull View view) {
    WindowCompat.getInsetsController(window, view).show(Type.ime());
    view.requestFocus();
  }

  @Deprecated
  public static void requestFocusAndShowKeyboard(@NonNull final View view) {
    view.requestFocus();
    view.post(() -> {
      InputMethodManager inputMethod = (InputMethodManager) view.getContext().getSystemService(
          Context.INPUT_METHOD_SERVICE
      );
      inputMethod.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    });
  }

  // ClickListeners & OnCheckedChangeListeners

  public static void setOnClickListeners(View.OnClickListener listener, View... views) {
    for (View view : views) {
      view.setOnClickListener(listener);
    }
  }

  public static void setOnCheckedChangeListeners(
      CompoundButton.OnCheckedChangeListener listener,
      CompoundButton... compoundButtons
  ) {
    for (CompoundButton view : compoundButtons) {
      view.setOnCheckedChangeListener(listener);
    }
  }

  public static void setChecked(boolean checked, MaterialCardView... cardViews) {
    for (MaterialCardView cardView : cardViews) {
      if (cardView != null) {
        cardView.setChecked(checked);
      }
    }
  }

  public static void uncheckAllChildren(ViewGroup... viewGroups) {
    for (ViewGroup viewGroup : viewGroups) {
      for (int i = 0; i < viewGroup.getChildCount(); i++) {
        View child = viewGroup.getChildAt(i);
        if (child instanceof MaterialCardView) {
          ((MaterialCardView) child).setChecked(false);
        }
      }
    }
  }

  // BottomSheets

  public static void showBottomSheet(AppCompatActivity activity, BottomSheetDialogFragment sheet) {
    sheet.show(activity.getSupportFragmentManager(), sheet.toString());
  }

  public static void showBottomSheet(
      AppCompatActivity activity, BottomSheetDialogFragment sheet, @Nullable Bundle bundle
  ) {
    if (bundle != null) {
      sheet.setArguments(bundle);
    }
    sheet.show(activity.getSupportFragmentManager(), sheet.toString());
  }

  // OnGlobalLayoutListeners

  public static void addOnGlobalLayoutListener(
      @Nullable View view, @NonNull OnGlobalLayoutListener listener) {
    if (view != null) {
      view.getViewTreeObserver().addOnGlobalLayoutListener(listener);
    }
  }

  public static void removeOnGlobalLayoutListener(
      @Nullable View view, @NonNull OnGlobalLayoutListener victim) {
    if (view != null) {
      view.getViewTreeObserver().removeOnGlobalLayoutListener(victim);
    }
  }

  // Animated icons

  public static void startIcon(View imageView) {
    if (!(imageView instanceof ImageView)) {
      return;
    }
    startIcon(((ImageView) imageView).getDrawable());
  }

  public static void startIcon(Drawable drawable) {
    if (drawable == null) {
      return;
    }
    try {
      ((Animatable) drawable).start();
    } catch (ClassCastException e) {
      Log.e(TAG, "icon animation requires AnimVectorDrawable");
    }
  }

  public static void startIcon(MenuItem item) {
    if (item == null) {
      return;
    }
    startIcon(item.getIcon());
  }

  public static void resetAnimatedIcon(ImageView imageView) {
    if (imageView == null) {
      return;
    }
    try {
      Animatable animatable = (Animatable) imageView.getDrawable();
      if (animatable != null) {
        animatable.stop();
      }
      imageView.setImageDrawable(null);
      imageView.setImageDrawable((Drawable) animatable);
    } catch (ClassCastException e) {
      Log.e(TAG, "resetting animated icon requires AnimVectorDrawable");
    }
  }

  // Toolbar

  public static void centerToolbarTitleOnLargeScreens(MaterialToolbar toolbar) {
    toolbar.setTitleCentered(!UiUtil.isFullWidth(toolbar.getContext()));
  }

  public static void centerTextOnLargeScreens(TextView textView) {
    if (UiUtil.isFullWidth(textView.getContext())) {
      textView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
      textView.setGravity(Gravity.START);
    } else {
      textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
      textView.setGravity(Gravity.CENTER_HORIZONTAL);
    }
  }

  // Ripple background for surface list items

  public static Drawable getRippleBgListItemSurface(Context context) {
    return getRippleBgListItemSurface(context, 8, 8);
  }

  public static Drawable getRippleBgListItemSurface(
      Context context, float paddingStart, float paddingEnd
  ) {
    boolean isRtl = UiUtil.isLayoutRtl(context);
    float[] radii = new float[8];
    Arrays.fill(radii, UiUtil.dpToPx(context, 16));
    RoundRectShape rect = new RoundRectShape(radii, null, null);
    ShapeDrawable shape = new ShapeDrawable(rect);
    shape.getPaint().setColor(SurfaceColors.SURFACE_3.getColor(context));
    LayerDrawable layers = new LayerDrawable(new ShapeDrawable[]{shape});
    layers.setLayerInset(
        0,
        UiUtil.dpToPx(context, isRtl ? paddingEnd : paddingStart),
        UiUtil.dpToPx(context, 2),
        UiUtil.dpToPx(context, isRtl ? paddingStart : paddingEnd),
        UiUtil.dpToPx(context, 2)
    );
    return new RippleDrawable(
        ColorStateList.valueOf(ResUtil.getColorHighlight(context)), null, layers
    );
  }

  public static Drawable getBgListItemSelected(Context context) {
    return getBgListItemSelected(context, 8, 8);
  }

  public static Drawable getBgListItemSelected(
      Context context, float paddingStart, float paddingEnd
  ) {
    boolean isRtl = UiUtil.isLayoutRtl(context);
    float[] radii = new float[8];
    Arrays.fill(radii, UiUtil.dpToPx(context, 16));
    RoundRectShape rect = new RoundRectShape(radii, null, null);
    ShapeDrawable shape = new ShapeDrawable(rect);
    shape.getPaint().setColor(ResUtil.getColorAttr(context, R.attr.colorSecondaryContainer));
    LayerDrawable layers = new LayerDrawable(new ShapeDrawable[]{shape});
    layers.setLayerInset(
        0,
        UiUtil.dpToPx(context, isRtl ? paddingEnd : paddingStart),
        UiUtil.dpToPx(context, 2),
        UiUtil.dpToPx(context, isRtl ? paddingStart : paddingEnd),
        UiUtil.dpToPx(context, 2)
    );
    return layers;
  }

  public static Drawable getRippleBgListItemSurfaceRecyclerItem(Context context) {
    return getRippleBgListItemSurfaceRecyclerItem(context, 8, 8);
  }

  public static Drawable getRippleBgListItemSurfaceRecyclerItem(
      Context context, float paddingStart, float paddingEnd
  ) {
    boolean isRtl = UiUtil.isLayoutRtl(context);
    float[] radii = new float[8];
    Arrays.fill(radii, UiUtil.dpToPx(context, 16));
    RoundRectShape rect = new RoundRectShape(radii, null, null);
    ShapeDrawable shape = new ShapeDrawable(rect);
    shape.getPaint().setColor(SurfaceColors.SURFACE_3.getColor(context));
    LayerDrawable layers = new LayerDrawable(new ShapeDrawable[]{shape});
    layers.setLayerInset(
        0,
        UiUtil.dpToPx(context, isRtl ? paddingEnd : paddingStart),
        UiUtil.dpToPx(context, 2),
        UiUtil.dpToPx(context, isRtl ? paddingStart : paddingEnd),
        UiUtil.dpToPx(context, 2)
    );
    return new RippleDrawable(
        ColorStateList.valueOf(ResUtil.getColorHighlight(context)), null, layers
    );
  }

  // Enable/disable views

  public static void setEnabled(boolean enabled, View... views) {
    for (View view : views) {
      view.setEnabled(enabled);
    }
  }

  public static void setEnabledAlpha(boolean enabled, boolean animated, View... views) {
    for (View view : views) {
      view.setEnabled(enabled);
      if (animated) {
        view.animate().alpha(enabled ? 1 : 0.5f).setDuration(200).start();
      } else {
        view.setAlpha(enabled ? 1 : 0.5f);
      }
    }
  }

  public static void setOnlyOverScrollStretchEnabled(ViewGroup group) {
    group.setOverScrollMode(
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ? View.OVER_SCROLL_IF_CONTENT_SCROLLS
            : View.OVER_SCROLL_NEVER
    );
  }

  // TouchProgressBar

  public static class TouchProgressBarUtil {
    private final ProgressBar progressConfirm;
    private ValueAnimator confirmProgressAnimator;
    private final OnConfirmedListener onConfirmedListener;
    private final int delayMilliseconds;

    private final static int CONFIRMATION_DURATION = 2000;

    @SuppressLint("ClickableViewAccessibility")
    public TouchProgressBarUtil(
        ProgressBar progressConfirm,
        @Nullable Button button,
        int delayMilliseconds,
        OnConfirmedListener onConfirmedListener
    ) {
      this.progressConfirm = progressConfirm;
      this.onConfirmedListener = onConfirmedListener;
      this.delayMilliseconds = delayMilliseconds;
      if (button != null) {
        button.setOnTouchListener((v, event) -> {
          onTouchDelete(v, event);
          return true;
        });
      }
    }

    public TouchProgressBarUtil(
        ProgressBar progressConfirm,
        Button button,
        OnConfirmedListener onConfirmedListener
    ) {
      this(progressConfirm, button, CONFIRMATION_DURATION, onConfirmedListener);
    }

    public void onTouchDelete(View view, MotionEvent event) {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
        showAndStartProgress((MaterialButton) view, null);
      } else if (event.getAction() == MotionEvent.ACTION_UP
          || event.getAction() == MotionEvent.ACTION_CANCEL) {
        hideAndStopProgress();
      }
    }

    public void showAndStartProgress(MaterialButton button, @Nullable Object objectOptional) {
      TransitionManager.beginDelayedTransition((ViewGroup) progressConfirm.getParent());
      progressConfirm.setVisibility(View.VISIBLE);
      int startValue = 0;
      if (confirmProgressAnimator != null) {
        startValue = progressConfirm.getProgress();
        if (startValue == 100) {
          startValue = 0;
        }
        confirmProgressAnimator.removeAllListeners();
        confirmProgressAnimator.cancel();
        confirmProgressAnimator = null;
      }
      confirmProgressAnimator = ValueAnimator.ofInt(startValue, progressConfirm.getMax());
      confirmProgressAnimator.setDuration((long) delayMilliseconds
          * (progressConfirm.getMax() - startValue) / progressConfirm.getMax());
      confirmProgressAnimator.addUpdateListener(
          animation -> progressConfirm.setProgress((Integer) animation.getAnimatedValue())
      );
      confirmProgressAnimator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          int currentProgress = progressConfirm.getProgress();
          if (currentProgress == progressConfirm.getMax()) {
            TransitionManager.beginDelayedTransition((ViewGroup) progressConfirm.getParent());
            progressConfirm.setVisibility(View.GONE);
            ViewUtil.startIcon(button.getIcon());
            onConfirmedListener.onConfirmed(objectOptional);
            return;
          }
          confirmProgressAnimator = ValueAnimator.ofInt(currentProgress, 0);
          confirmProgressAnimator.setDuration((long) (delayMilliseconds / 2)
              * currentProgress / progressConfirm.getMax());
          confirmProgressAnimator.setInterpolator(new FastOutSlowInInterpolator());
          confirmProgressAnimator.addUpdateListener(
              anim -> progressConfirm.setProgress((Integer) anim.getAnimatedValue())
          );
          confirmProgressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
              TransitionManager.beginDelayedTransition((ViewGroup) progressConfirm.getParent());
              progressConfirm.setVisibility(View.GONE);
            }
          });
          confirmProgressAnimator.start();
        }
      });
      confirmProgressAnimator.start();
    }

    public void hideAndStopProgress() {
      if (confirmProgressAnimator != null) {
        confirmProgressAnimator.cancel();
      }

      if (progressConfirm.getProgress() != 100) {
        Toast.makeText(
            progressConfirm.getContext(),
            R.string.msg_press_hold_confirm,
            Toast.LENGTH_SHORT
        ).show();
      }
    }

    public void onDestroy() {
      if (confirmProgressAnimator != null) {
        confirmProgressAnimator.cancel();
        confirmProgressAnimator = null;
      }
    }

    public interface OnConfirmedListener {
      void onConfirmed(@Nullable Object objectOptional);
    }
  }
}
