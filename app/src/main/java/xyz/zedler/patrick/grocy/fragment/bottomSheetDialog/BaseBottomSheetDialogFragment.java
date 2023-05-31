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

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.PaintDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat.Type;
import androidx.preference.PreferenceManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback;
import com.google.android.material.bottomsheet.CustomBottomSheetDialog;
import com.google.android.material.bottomsheet.CustomBottomSheetDialogFragment;
import com.google.android.material.elevation.SurfaceColors;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class BaseBottomSheetDialogFragment extends CustomBottomSheetDialogFragment {

  private static final String TAG = "BaseBottomSheetDialogFragment";

  private MainActivity activity;
  private Dialog dialog;
  private View decorView;
  private ViewUtil viewUtil;
  private boolean lightNavBar;
  private int backgroundColor;
  private boolean skipCollapsedStateInPortrait = false;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    activity = (MainActivity) requireActivity();
    viewUtil = new ViewUtil();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    dialog = new CustomBottomSheetDialog(requireContext());

    decorView = dialog.getWindow().getDecorView();
    if (decorView == null) {
      return dialog;
    }

    decorView.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            ViewGroup container = dialog.findViewById(R.id.container);
            View sheet = dialog.findViewById(R.id.design_bottom_sheet);
            if (container == null || sheet == null) {
              return;
            }

            backgroundColor = SurfaceColors.SURFACE_3.getColor(activity);
            PaintDrawable background = new PaintDrawable(backgroundColor);
            int radius = UiUtil.dpToPx(requireContext(), 28);
            setCornerRadius(background, radius);
            sheet.setBackground(background);

            int peakHeightHalf = UiUtil.getDisplayHeight(requireContext()) / 2;

            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);

            SharedPreferences sharedPrefs
                = PreferenceManager.getDefaultSharedPreferences(requireContext());
            boolean expandBottomSheets = sharedPrefs.getBoolean(
                Constants.SETTINGS.BEHAVIOR.EXPAND_BOTTOM_SHEETS,
                Constants.SETTINGS_DEFAULT.BEHAVIOR.EXPAND_BOTTOM_SHEETS
            );

            boolean isPortrait = UiUtil.isOrientationPortrait(requireContext());
            if (isPortrait && skipCollapsedStateInPortrait) {
              behavior.setSkipCollapsed(true);
              behavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
            } else if (isPortrait) {
              behavior.setSkipCollapsed(false);
              behavior.setPeekHeight(peakHeightHalf);
            } else { // landscape
              behavior.setSkipCollapsed(false);
              behavior.setPeekHeight(
                  expandBottomSheets
                      ? UiUtil.getDisplayHeight(requireActivity())
                      : UiUtil.getDisplayHeight(requireActivity()) / 2
              );
            }

            boolean isFullWidth =
                behavior.getMaxWidth() >= UiUtil.getDisplayWidth(requireContext());

            ViewCompat.setOnApplyWindowInsetsListener(decorView, (view, insets) -> {
              int insetTop = insets.getInsets(Type.systemBars()).top;
              int insetBottom = insets.getInsets(Type.systemBars()).bottom;

              layoutEdgeToEdge(dialog.getWindow(), insetBottom);

              if (lightNavBar) {
                // Below API 30 it does not work for non-gesture if we take the normal method
                UiUtil.setLightNavigationBar(sheet, true);
              }

              applyBottomInset(insetBottom);

              if (!isFullWidth) {
                // Don't let the sheet go below the status bar
                container.setPadding(0, insetTop, 0, 0);
              }

              behavior.addBottomSheetCallback(new BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                  updateCornerRadii(bottomSheet, insetTop, isFullWidth, radius, background);
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                  updateCornerRadii(bottomSheet, insetTop, isFullWidth, radius, background);
                }
              });

              return insets;
            });

            ViewCompat.setOnApplyWindowInsetsListener(sheet, (view, insets) -> {
              int insetImeBottom = insets.getInsets(Type.ime()).bottom;
              int insetNavBottom = insets.getInsets(Type.systemBars()).bottom;
              boolean isImeVisible = insets.isVisible(Type.ime());
              view.setPadding(
                  view.getPaddingLeft(),
                  view.getPaddingTop(),
                  view.getPaddingRight(),
                  isImeVisible ? insetImeBottom - insetNavBottom : insetImeBottom
              );
              if (isImeVisible) {
                behavior.setSkipCollapsed(true);
                behavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
              } else {
                if (isPortrait && skipCollapsedStateInPortrait) {
                  behavior.setSkipCollapsed(true);
                  behavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
                } else if (isPortrait) {
                  behavior.setSkipCollapsed(false);
                  behavior.setPeekHeight(peakHeightHalf);
                } else { // landscape
                  behavior.setSkipCollapsed(false);
                  behavior.setPeekHeight(
                      expandBottomSheets
                          ? UiUtil.getDisplayHeight(requireActivity())
                          : UiUtil.getDisplayHeight(requireActivity()) / 2
                  );
                }
              }
              return insets;
            });

            if (decorView.getViewTreeObserver().isAlive()) {
              ViewUtil.removeOnGlobalLayoutListener(decorView, this);
            }
          }
        });

    return dialog;
  }

  private void updateCornerRadii(
      View bottomSheet, int insetTop, boolean isFullWidth, int radius, PaintDrawable background
  ) {
    if (bottomSheet.getTop() < insetTop && isFullWidth) {
      float fraction = (float) bottomSheet.getTop() / (float) insetTop;
      setCornerRadius(background, radius * fraction);
    } else if (bottomSheet.getTop() != 0 && isFullWidth) {
      setCornerRadius(background, radius);
    }
  }

  private void setCornerRadius(PaintDrawable drawable, float radius) {
    drawable.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
  }

  private void layoutEdgeToEdge(Window window, int insetBottom) {
    boolean isOrientationPortraitOrNavAtBottom =
        UiUtil.isOrientationPortrait(requireContext()) || insetBottom > 0;
    boolean isDarkModeActive = UiUtil.isDarkModeActive(requireContext());

    int colorScrim = ColorUtils.setAlphaComponent(backgroundColor, (int) (0.7f * 255));

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // 29
      window.setStatusBarColor(Color.TRANSPARENT);
      if (UiUtil.isNavigationModeGesture(requireContext())) {
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setNavigationBarContrastEnforced(true);
      } else {
        lightNavBar = !isDarkModeActive && isOrientationPortraitOrNavAtBottom;
        if (isOrientationPortraitOrNavAtBottom) {
          window.setNavigationBarColor(colorScrim);
        } else {
          window.setNavigationBarColor(
              isDarkModeActive ? UiUtil.SCRIM_DARK_DIALOG : UiUtil.SCRIM_LIGHT_DIALOG
          );
          window.setNavigationBarDividerColor(ResUtil.getColorOutlineSecondary(activity));
        }
      }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // 28
      window.setStatusBarColor(Color.TRANSPARENT);
      lightNavBar = !isDarkModeActive && isOrientationPortraitOrNavAtBottom;
      if (isOrientationPortraitOrNavAtBottom) {
        window.setNavigationBarColor(colorScrim);
      } else {
        window.setNavigationBarColor(
            isDarkModeActive ? UiUtil.SCRIM_DARK_DIALOG : UiUtil.SCRIM_LIGHT_DIALOG
        );
        window.setNavigationBarDividerColor(ResUtil.getColorOutlineSecondary(activity));
      }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // 26
      window.setStatusBarColor(Color.TRANSPARENT);
      lightNavBar = !isDarkModeActive && isOrientationPortraitOrNavAtBottom;
      if (isOrientationPortraitOrNavAtBottom) {
        window.setNavigationBarColor(colorScrim);
      } else {
        window.setNavigationBarColor(
            isDarkModeActive ? UiUtil.SCRIM_DARK_DIALOG : UiUtil.SCRIM_LIGHT_DIALOG
        );
      }
    } else  { // down to 21
      if (isOrientationPortraitOrNavAtBottom) {
        window.setNavigationBarColor(isDarkModeActive ? colorScrim : UiUtil.SCRIM);
      } else {
        window.setNavigationBarColor(
            isDarkModeActive ? UiUtil.SCRIM_DARK_DIALOG : UiUtil.SCRIM_LIGHT_DIALOG
        );
      }
    }
  }

  public void setSkipCollapsedInPortrait() {
    skipCollapsedStateInPortrait = true;
  }

  public SharedPreferences getSharedPrefs() {
    return activity.getSharedPrefs();
  }

  public ViewUtil getViewUtil() {
    return viewUtil;
  }

  public void performHapticClick() {
    activity.hapticUtil.click();
  }

  public void performHapticHeavyClick() {
    activity.hapticUtil.heavyClick();
  }

  public void applyBottomInset(int bottom) {
  }
}
