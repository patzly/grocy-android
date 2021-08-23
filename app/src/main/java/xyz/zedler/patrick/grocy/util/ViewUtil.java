package xyz.zedler.patrick.grocy.util;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

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

  public static void startIcon(ImageView imageView) {
    if (imageView == null) {
      return;
    }
    startIcon(imageView.getDrawable());
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

  public static void start(MenuItem item) {
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
}
