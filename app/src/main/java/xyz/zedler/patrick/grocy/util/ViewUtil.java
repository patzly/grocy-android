package xyz.zedler.patrick.grocy.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.transition.TransitionManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.R;

public class ViewUtil {

  private final static String TAG = ViewUtil.class.getSimpleName();

  private long lastClick;
  private long idle = 500;

  // Prevent multiple clicks

  public ViewUtil() {
    lastClick = 0;
  }

  public ViewUtil(long minClickIdle) {
    lastClick = 0;
    idle = minClickIdle;
  }

  public boolean isClickDisabled() {
    if (SystemClock.elapsedRealtime() - lastClick < idle) {
      return true;
    }
    lastClick = SystemClock.elapsedRealtime();
    return false;
  }

  public boolean isClickEnabled() {
    return !isClickDisabled();
  }

  // Layout direction

  public static boolean isLayoutRtl(View view) {
    return ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_RTL;
  }

  // Show keyboard for EditText

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
        showAndStartProgress(view, null);
      } else if (event.getAction() == MotionEvent.ACTION_UP
          || event.getAction() == MotionEvent.ACTION_CANCEL) {
        hideAndStopProgress();
      }
    }

    public void showAndStartProgress(View buttonView, @Nullable Object objectOptional) {
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
            ImageView buttonImage = buttonView.findViewById(R.id.image_action_button);
            if (buttonImage != null) {
              ((Animatable) buttonImage.getDrawable()).start();
            }
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
            Toast.LENGTH_LONG
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
