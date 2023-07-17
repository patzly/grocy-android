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

package xyz.zedler.patrick.grocy.behavior;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat.Type;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class BottomScrollBehavior {

  private static final String TAG = BottomScrollBehavior.class.getSimpleName();
  private static final boolean DEBUG = false;

  private static final int STATE_SCROLLED_DOWN = 1;
  private static final int STATE_SCROLLED_UP = 2;
  // distance gets divided to prevent cutoff of edge effect
  private static final int PUFFER_DIVIDER = 2;

  private final BottomAppBar bottomAppBar;
  private final FloatingActionButton fabMain, fabTopScroll;
  private AppBarLayout appBar;
  private ViewGroup scrollView;
  private final View snackbarAnchor;
  private boolean liftOnScroll;
  private boolean provideTopScroll;

  private int bottomBarHeight;
  private int insetBottomY;
  private float bottomBarTranslationY;
  // distance before top scroll when overScroll is turned off
  private int pufferSize = 0;
  private int currentState;
  private final int topScrollLimit;
  private boolean isTopScroll = false;
  private boolean canBottomAppBarBeVisible;
  private boolean useOverScrollFix;
  private boolean useTopScrollAsAnchor, useFabAsAnchor;

  public BottomScrollBehavior(
      @NonNull Context context, @NonNull BottomAppBar bottomAppBar,
      @NonNull FloatingActionButton fabMain, @NonNull FloatingActionButton fabTopScroll,
      @NonNull View snackbarAnchor
  ) {
    this.bottomAppBar = bottomAppBar;
    this.fabMain = fabMain;
    this.fabTopScroll = fabTopScroll;
    this.snackbarAnchor = snackbarAnchor;

    ViewCompat.setOnApplyWindowInsetsListener(fabTopScroll, (v, insets) -> {
      int insetBottom = insets.getInsets(Type.systemBars()).bottom;
      int screenHeight = UiUtil.getDisplayHeight(context);
      insetBottomY = screenHeight - insetBottom;
      return insets;
    });

    ViewTreeObserver observerBottomBar = bottomAppBar.getViewTreeObserver();
    observerBottomBar.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        bottomBarHeight = bottomAppBar.getMeasuredHeight();
        ((LayoutParams) fabTopScroll.getLayoutParams()).bottomMargin
            = UiUtil.dpToPx(context, 16) + bottomBarHeight;
        if (bottomAppBar.getViewTreeObserver().isAlive()) {
          bottomAppBar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
      }
    });

    int fabMainMarginTop = UiUtil.dpToPx(context, 12);
    int bottomEdgeDistance = UiUtil.dpToPx(context, 16);
    bottomBarTranslationY = bottomAppBar.getTranslationY();
    observerBottomBar.addOnDrawListener(() -> {
      float translationY = bottomAppBar.getTranslationY();
      if (bottomBarTranslationY == translationY) {
        return; // no translation change
      }
      bottomBarTranslationY = translationY;
      if (fabMain.isOrWillBeShown() && bottomAppBar.getY() > fabMain.getY()) {
        fabTopScroll.setTranslationY(
            fabMain.getY() - fabMainMarginTop - fabTopScroll.getBottom()
        );
      } else if (bottomAppBar.getY() > insetBottomY) {
        fabTopScroll.setTranslationY(
            insetBottomY - bottomEdgeDistance - fabTopScroll.getBottom()
        );
      } else {
        fabTopScroll.setTranslationY(translationY);
      }
      Log.i(TAG, "updateSnackbarAnchor() in OnDrawListener of bottomAppBar");
      updateSnackbarAnchor();
    });

    fabMain.addOnHideAnimationListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        useFabAsAnchor = false;
        Log.i(TAG, "updateSnackbarAnchor() after fab hide animation ended");
        updateSnackbarAnchor();
      }
    });
    fabMain.addOnShowAnimationListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationStart(Animator animation) {
        useFabAsAnchor = true;
        Log.i(TAG, "updateSnackbarAnchor() after fab show animation started");
        updateSnackbarAnchor();
      }
    });

    fabTopScroll.addOnHideAnimationListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        useTopScrollAsAnchor = false;
        Log.i(TAG, "updateSnackbarAnchor() after topScroll hide animation ended");
        updateSnackbarAnchor();
      }
    });
    fabTopScroll.addOnShowAnimationListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationStart(Animator animation) {
        useTopScrollAsAnchor = true;
        Log.i(TAG, "updateSnackbarAnchor() after topScroll show animation started");
        updateSnackbarAnchor();
      }
    });

    topScrollLimit = UiUtil.dpToPx(context, 150);
    canBottomAppBarBeVisible = true;
    useOverScrollFix = Build.VERSION.SDK_INT < 31;
    useFabAsAnchor = true;
    useTopScrollAsAnchor = false;
  }

  /**
   * Usage:
   * activity.getScrollBehavior().setUpScroll(appBar, liftOnScroll, scrollView, ?provideTopScroll, ?keepScrollPosition);
   * activity.getScrollBehavior().setBottomBarVisibility(visible, ?stay, ?animated);
   */
  public void setUpScroll(
      @NonNull AppBarLayout appBar, boolean liftOnScroll,
      @Nullable ViewGroup scrollView, boolean provideTopScroll, boolean keepScrollPosition
  ) {
    this.appBar = appBar;
    this.scrollView = scrollView;
    this.provideTopScroll = provideTopScroll;

    currentState = STATE_SCROLLED_UP;
    if (keepScrollPosition) {
      if (fabTopScroll != null) {
        fabTopScroll.hide();
      }
    } else { // Explicitly hides top scroll
      new Handler(Looper.getMainLooper()).postDelayed(() -> {
        if (fabTopScroll != null) {
          fabTopScroll.hide();
        }
      }, 1);
    }

    measureScrollView();
    setLiftOnScroll(liftOnScroll);

    if (scrollView instanceof NestedScrollView) {
      NestedScrollView nested = (NestedScrollView) scrollView;
      if (!keepScrollPosition) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> nested.setScrollY(0), 1);
      }
      nested.setOnScrollChangeListener(getOnScrollChangeListener());
    } else if (scrollView instanceof RecyclerView) {
      RecyclerView recycler = (RecyclerView) scrollView;
      if (!keepScrollPosition) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> recycler.setScrollY(0), 1);
      }
      recycler.addOnScrollListener(getOnScrollListener());
    }

    if (fabTopScroll != null) {
      fabTopScroll.setOnClickListener(v -> {
        if (scrollView instanceof NestedScrollView) {
          ((NestedScrollView) scrollView).smoothScrollTo(0, 0);
        } else if (scrollView instanceof RecyclerView) {
          ((RecyclerView) scrollView).smoothScrollToPosition(0);
        }
        fabTopScroll.hide();
        if (bottomAppBar.getHideOnScroll()) {
          bottomAppBar.performShow(true);
        }
      });
    }
  }

  public void setUpScroll(
      @NonNull AppBarLayout appBar, boolean liftOnScroll,
      @Nullable ViewGroup scrollView, boolean provideTopScroll
  ) {
    setUpScroll(appBar, liftOnScroll, scrollView, provideTopScroll, false);
  }

  public void setUpScroll(
      @NonNull AppBarLayout appBar, boolean liftOnScroll, @Nullable ViewGroup scrollView
  ) {
    setUpScroll(appBar, liftOnScroll, scrollView, true);
  }

  public void setProvideTopScroll(boolean provideTopScroll) {
    this.provideTopScroll = provideTopScroll;
    if (fabTopScroll != null && !provideTopScroll) {
      fabTopScroll.hide();
    }
  }

  public void setCanBottomAppBarBeVisible(boolean canBeVisible) {
    canBottomAppBarBeVisible = canBeVisible;
  }

  public void setBottomBarVisibility(boolean visible, boolean stay, boolean animated) {
    bottomAppBar.setHideOnScroll(canBottomAppBarBeVisible && !stay);
    ViewTreeObserver observer = bottomAppBar.getViewTreeObserver();
    observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        if (visible && canBottomAppBarBeVisible) {
          bottomAppBar.performShow(animated);
        } else {
          bottomAppBar.performHide(animated);
        }
        if (bottomAppBar.getViewTreeObserver().isAlive()) {
          bottomAppBar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
      }
    });
  }

  public void setBottomBarVisibility(boolean visible, boolean stay) {
    setBottomBarVisibility(visible, stay, true);
  }

  public void setBottomBarVisibility(boolean visible) {
    setBottomBarVisibility(visible, false, true);
  }

  public void setLiftOnScroll(boolean lift) {
    liftOnScroll = lift;
    // We'll make this manually
    appBar.setLiftOnScroll(false);
    appBar.setLiftable(true);
    if (scrollView != null) {
      if (lift) {
        if (scrollView.getScrollY() == 0) {
          appBar.setLifted(false);
          setOverScrollEnabled(false);
        } else {
          appBar.setLifted(true);
        }
      } else {
        if (useOverScrollFix) {
          if (scrollView.getScrollY() == 0) {
            setOverScrollEnabled(false);
          }
        } else {
          setOverScrollEnabled(true);
        }
        appBar.setLifted(true);
      }
    } else {
      appBar.setLifted(!lift);
    }
    if (DEBUG) {
      Log.i(TAG, "setLiftOnScroll(" + lift + ")");
    }
  }

  private void measureScrollView() {
    if (scrollView == null || scrollView instanceof RecyclerView) {
      return;
    }
    ViewTreeObserver observer = scrollView.getViewTreeObserver();
    observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        int scrollViewHeight = scrollView.getMeasuredHeight();
        if (scrollView.getChildAt(0) != null) {
          int scrollContentHeight = scrollView.getChildAt(0).getHeight();
          pufferSize = (scrollContentHeight - scrollViewHeight) / PUFFER_DIVIDER;
        } else if (DEBUG) {
          Log.e(TAG, "measureScrollView: no child");
        }
        if (scrollView.getViewTreeObserver().isAlive()) {
          scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
      }
    });
  }

  private void setOverScrollEnabled(boolean enabled) {
    if (scrollView == null) {
      return;
    }
    if (Build.VERSION.SDK_INT >= 31) {
      // Stretch effect is always nice
      scrollView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
    } else {
      scrollView.setOverScrollMode(
          enabled ? View.OVER_SCROLL_IF_CONTENT_SCROLLS : View.OVER_SCROLL_NEVER
      );
    }
  }

  public void setNestedOverScrollFixEnabled(boolean enabled) {
    useOverScrollFix = enabled && Build.VERSION.SDK_INT < 31;
    if (useOverScrollFix && scrollView != null && scrollView.getScrollY() == 0) {
      setOverScrollEnabled(false);
    }
  }

  private NestedScrollView.OnScrollChangeListener getOnScrollChangeListener() {
    return (NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
      if (!isTopScroll && scrollY == 0) { // TOP
        onTopScroll();
      } else {
        if (scrollY < oldScrollY) { // UP
          if (currentState != STATE_SCROLLED_UP) {
            onScrollUp();
          }
          if ((liftOnScroll || useOverScrollFix) && scrollY < pufferSize) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
              if (scrollY > 0) {
                setOverScrollEnabled(false);
              }
            }, 1);
          }
          if (scrollY < topScrollLimit && provideTopScroll) {
            if (fabTopScroll.isOrWillBeShown()) {
              fabTopScroll.hide();
            }
          }
        } else if (scrollY > oldScrollY) {
          if (currentState != STATE_SCROLLED_DOWN) { // DOWN
            onScrollDown();
          }
          if (scrollY > topScrollLimit && provideTopScroll) {
            if (fabTopScroll.isOrWillBeHidden()) {
              fabTopScroll.show();
            }
          }
        }
      }
    };
  }
  private RecyclerView.OnScrollListener getOnScrollListener() {
    return new OnScrollListener() {
      @Override
      public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        int scrollAbsoluteY = recyclerView.computeVerticalScrollOffset();
        if (!isTopScroll && scrollAbsoluteY == 0) { // TOP
          onTopScroll();
        } else {
          if (dy < 0) { // UP
            if (currentState != STATE_SCROLLED_UP) {
              onScrollUp();
            }
            if ((liftOnScroll || useOverScrollFix) && dy < pufferSize) {
              new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (scrollAbsoluteY > 0) {
                  setOverScrollEnabled(false);
                }
              }, 1);
            }
            if (scrollAbsoluteY < topScrollLimit && provideTopScroll) {
              if (fabTopScroll.isOrWillBeShown()) {
                fabTopScroll.hide();
              }
            }
          } else if (dy > 0) {
            if (currentState != STATE_SCROLLED_DOWN) { // DOWN
              onScrollDown();
            }
            if (scrollAbsoluteY > topScrollLimit && provideTopScroll) {
              if (fabTopScroll.isOrWillBeHidden()) {
                fabTopScroll.show();
              }
            }
          }
        }
      }
    };
  }

  private void onTopScroll() {
    isTopScroll = true;
    if (liftOnScroll) {
      appBar.setLifted(false);
    }
    if (DEBUG) {
      Log.i(TAG, "onTopScroll: liftOnScroll = " + liftOnScroll);
    }
  }

  private void onScrollUp() {
    currentState = STATE_SCROLLED_UP;
    appBar.setLifted(true);
    if (DEBUG) {
      Log.i(TAG, "onScrollUp: UP");
    }
  }

  private void onScrollDown() {
    // second top scroll is unrealistic before down scroll
    isTopScroll = false;
    currentState = STATE_SCROLLED_DOWN;
    if (scrollView != null) {
      appBar.setLifted(true);
      setOverScrollEnabled(true);
    }
    if (DEBUG) {
      Log.i(TAG, "onScrollDown: DOWN");
    }
  }

  public void updateSnackbarAnchor() {
    snackbarAnchor.setY(getSnackbarAnchorY());
  }

  public float getSnackbarAnchorY() {
    if (useTopScrollAsAnchor) {
      Log.i(TAG, "getSnackbarAnchorY: topScroll");
      return fabTopScroll.getY();
    } else if (useFabAsAnchor) {
      if (fabMain.getY() < bottomAppBar.getY()) {
        Log.i(TAG, "getSnackbarAnchorY: fab or bottomAppBar: fab");
      } else if (fabMain.getY() > bottomAppBar.getY()) {
        Log.i(TAG, "getSnackbarAnchorY: fab or bottomAppBar: bottomAppBar");
      } else {
        Log.i(TAG, "getSnackbarAnchorY: fab or bottomAppBar: equal");
      }
      return Math.min(fabMain.getY(), bottomAppBar.getY());
    } else {
      Log.i(TAG, "getSnackbarAnchorY: bottomAppBar");
      return bottomAppBar.getY();
    }
  }

  @Nullable
  public ViewGroup getScrollView() {
    if (scrollView == null || scrollView instanceof RecyclerView) {
      return null;
    } else {
      return scrollView;
    }
  }
}
