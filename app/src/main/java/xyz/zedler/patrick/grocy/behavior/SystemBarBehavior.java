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

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat.Type;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.appbar.AppBarLayout;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class SystemBarBehavior {

  private static final String TAG = SystemBarBehavior.class.getSimpleName();

  private final Activity activity;
  private final Window window;
  int containerPaddingTop;
  int containerPaddingBottom;
  int scrollContentPaddingBottom;
  private AppBarLayout appBarLayout;
  private ViewGroup container;
  private NestedScrollView scrollView;
  private ViewGroup scrollContent;
  private boolean applyAppBarInsetOnContainer;
  private boolean applyStatusBarInsetOnContainer;
  private boolean hasScrollView;
  private boolean isScrollable;
  private int addBottomInset;

  public SystemBarBehavior(@NonNull Activity activity) {
    this.activity = activity;
    window = activity.getWindow();

    // GOING EDGE TO EDGE
    UiUtil.layoutEdgeToEdge(window);

    applyAppBarInsetOnContainer = true;
    applyStatusBarInsetOnContainer = true;
    hasScrollView = false;
    isScrollable = false;
  }

  public void setAppBar(AppBarLayout appBarLayout) {
    this.appBarLayout = appBarLayout;
  }

  public void setContainer(ViewGroup container) {
    this.container = container;
    containerPaddingTop = container.getPaddingTop();
    containerPaddingBottom = container.getPaddingBottom();
  }

  public void setScroll(@NonNull NestedScrollView scrollView, @NonNull ViewGroup scrollContent) {
    this.scrollView = scrollView;
    this.scrollContent = scrollContent;
    scrollContentPaddingBottom = scrollContent.getPaddingBottom();
    hasScrollView = true;

    if (container == null) {
      setContainer(scrollView);
    }
  }

  public void setAdditionalBottomInset(int additional) {
    addBottomInset = additional;
  }

  public void setUp() {
    // TOP INSET
    if (appBarLayout != null) {
      ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (v, insets) -> {
        int statusBarInset = insets.getInsets(Type.systemBars()).top;

        // STATUS BAR INSET
        appBarLayout.setPadding(0, statusBarInset, 0, appBarLayout.getPaddingBottom());
        appBarLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        // APP BAR INSET
        if (container != null && applyAppBarInsetOnContainer) {
          ViewGroup.MarginLayoutParams params
              = (ViewGroup.MarginLayoutParams) container.getLayoutParams();
          params.topMargin = appBarLayout.getMeasuredHeight();
          container.setLayoutParams(params);
        } else if (container != null) {
          //
          container.setPadding(
              container.getPaddingLeft(),
              containerPaddingTop + (applyStatusBarInsetOnContainer ? statusBarInset : 0),
              container.getPaddingRight(),
              container.getPaddingBottom()
          );
        }
        return insets;
      });
    } else if (container != null) {
      // if no app bar exists, status bar inset is applied to container
      ViewCompat.setOnApplyWindowInsetsListener(container, (v, insets) -> {
        int statusBarInset = applyStatusBarInsetOnContainer
            ? insets.getInsets(Type.systemBars()).top
            : 0;

        // STATUS BAR INSET
        container.setPadding(
            container.getPaddingLeft(),
            containerPaddingTop + statusBarInset,
            container.getPaddingRight(),
            container.getPaddingBottom()
        );
        return insets;
      });
    }

    // NAV BAR INSET
    if (UiUtil.isOrientationPortrait(activity) && hasContainer()) {
      View container = hasScrollView ? scrollContent : this.container;
      ViewCompat.setOnApplyWindowInsetsListener(container, (v, insets) -> {
        int paddingBottom = hasScrollView
            ? scrollContentPaddingBottom
            : containerPaddingBottom;
        container.setPadding(
            container.getPaddingLeft(),
            container.getPaddingTop(),
            container.getPaddingRight(),
            paddingBottom + addBottomInset + insets.getInsets(Type.systemBars()).bottom
        );
        return insets;
      });
    } else {
      if (UiUtil.isNavigationModeGesture(activity) && hasContainer()) {
        View container = hasScrollView ? scrollContent : this.container;
        ViewCompat.setOnApplyWindowInsetsListener(container, (v, insets) -> {
          int paddingBottom = hasScrollView
              ? scrollContentPaddingBottom
              : containerPaddingBottom;
          container.setPadding(
              container.getPaddingLeft(),
              container.getPaddingTop(),
              container.getPaddingRight(),
              paddingBottom + addBottomInset + insets.getInsets(Type.systemBars()).bottom
          );
          return insets;
        });
      } else {
        View root = window.getDecorView().findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
          root.setPadding(
              root.getPaddingLeft(),
              root.getPaddingTop(),
              insets.getInsets(Type.systemBars()).right,
              root.getPaddingBottom()
          );
          return insets;
        });
        View container = hasScrollView ? scrollContent : this.container;
        if (container != null) {
          container.setPadding(
              container.getPaddingLeft(),
              container.getPaddingTop(),
              container.getPaddingRight(),
              container.getPaddingBottom() + addBottomInset
          );
        }
      }
    }

    if (hasScrollView) {
      // call viewThreeObserver, this updates the system bar appearance
      measureScrollView();
    } else {
      // call directly because there won't be any changes caused by scroll content
      updateSystemBars();
    }
  }

  private void measureScrollView() {
    scrollView.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            int scrollViewHeight = scrollView.getMeasuredHeight();
            int scrollContentHeight = scrollContent.getHeight();
            isScrollable = scrollViewHeight - scrollContentHeight < 0;
            updateSystemBars();
            // Kill ViewTreeObserver
            if (scrollView.getViewTreeObserver().isAlive()) {
              scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(
                  this
              );
            }
          }
        });
  }

  public void applyAppBarInsetOnContainer(boolean apply) {
    applyAppBarInsetOnContainer = apply;
  }

  public void applyStatusBarInsetOnContainer(boolean apply) {
    applyStatusBarInsetOnContainer = apply;
  }

  public static void applyBottomInset(@NonNull View view) {
    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
    int marginBottom = params.bottomMargin;
    ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
      params.bottomMargin = marginBottom + insets.getInsets(Type.systemBars()).bottom;
      view.setLayoutParams(params);
      return insets;
    });
  }

  private void updateSystemBars() {
    boolean isOrientationPortrait = UiUtil.isOrientationPortrait(activity);
    boolean isDarkModeActive = UiUtil.isDarkModeActive(activity);

    int colorScrim = ResUtil.getColorAttr(activity, android.R.attr.colorBackground, 0.7f);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // 29
      window.setStatusBarColor(Color.TRANSPARENT);
      if (!isDarkModeActive) {
        UiUtil.setLightStatusBar(window.getDecorView(), true);
      }
      if (UiUtil.isNavigationModeGesture(activity)) {
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setNavigationBarContrastEnforced(true);
      } else {
        if (!isDarkModeActive) {
          UiUtil.setLightNavigationBar(window.getDecorView(), true);
        }
        if (isOrientationPortrait) {
          window.setNavigationBarColor(
              isScrollable ? colorScrim : Color.parseColor("#01000000")
          );
        } else {
          window.setNavigationBarDividerColor(ResUtil.getColorOutlineSecondary(activity));
          window.setNavigationBarColor(ResUtil.getColorBg(activity));
        }
      }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // 28
      window.setStatusBarColor(Color.TRANSPARENT);
      if (!isDarkModeActive) {
        UiUtil.setLightStatusBar(window.getDecorView(), true);
        UiUtil.setLightNavigationBar(window.getDecorView(), true);
      }
      if (isOrientationPortrait) {
        window.setNavigationBarColor(isScrollable ? colorScrim : Color.TRANSPARENT);
      } else {
        window.setNavigationBarDividerColor(ResUtil.getColorOutlineSecondary(activity));
        window.setNavigationBarColor(ResUtil.getColorBg(activity));
      }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // 26
      window.setStatusBarColor(Color.TRANSPARENT);
      if (!isDarkModeActive) {
        UiUtil.setLightStatusBar(window.getDecorView(), true);
      }
      if (isOrientationPortrait) {
        window.setNavigationBarColor(isScrollable ? colorScrim : Color.TRANSPARENT);
        if (!isDarkModeActive) {
          UiUtil.setLightNavigationBar(window.getDecorView(), true);
        }
      } else {
        window.setNavigationBarColor(isDarkModeActive ? Color.BLACK : UiUtil.SCRIM);
      }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 23
      window.setStatusBarColor(Color.TRANSPARENT);
      if (!isDarkModeActive) {
        UiUtil.setLightStatusBar(window.getDecorView(), true);
      }
      if (isOrientationPortrait) {
        window.setNavigationBarColor(
            isDarkModeActive ? (isScrollable ? colorScrim : Color.TRANSPARENT) : UiUtil.SCRIM
        );
      } else {
        window.setNavigationBarColor(isDarkModeActive ? colorScrim : UiUtil.SCRIM);
      }
    } else { // 21
      window.setStatusBarColor(isDarkModeActive ? Color.TRANSPARENT : UiUtil.SCRIM);
      if (isOrientationPortrait) {
        window.setNavigationBarColor(
            isDarkModeActive ? (isScrollable ? colorScrim : Color.TRANSPARENT) : UiUtil.SCRIM
        );
      } else {
        window.setNavigationBarColor(Color.BLACK);
      }
    }
  }

  private boolean hasContainer() {
    return container != null;
  }
}
