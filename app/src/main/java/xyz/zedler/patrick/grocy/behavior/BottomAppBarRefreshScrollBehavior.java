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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.behavior;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewPropertyAnimator;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.animation.AnimationUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.bottomappbar.CustomBottomAppBar;
import xyz.zedler.patrick.grocy.util.UnitUtil;

public class BottomAppBarRefreshScrollBehavior {

  private final static String TAG = "BottomBarScrollBehavior";
  private final static boolean DEBUG = false;

  private static final int STATE_SCROLLED_DOWN = 1;
  private static final int STATE_SCROLLED_UP = 2;

  private int currentState = STATE_SCROLLED_UP;
  private final int pufferSize = 0; // distance before top scroll when overScroll is turned off
  private final int pufferDivider = 2; // distance gets divided to prevent cutoff of edge effect
  private int topScrollLimit;
  private int storedFirstBottomScrollY = 0;
  private int scrollLimitY;

  private boolean isTopScroll = false;
  private boolean hideOnScroll = true;
  private boolean showTopScroll = true;

  private final Activity activity;
  private CustomBottomAppBar bottomAppBar;
  private FloatingActionButton fabScroll;
  private View scrollView;
  private ViewPropertyAnimator topScrollAnimator;

  public BottomAppBarRefreshScrollBehavior(Activity activity) {
    this.activity = activity;
    if (activity == null) {
      Log.e(TAG, "constructor: activity is null!");
      return;
    }
    topScrollLimit = UnitUtil.getDp(activity, 100);
    scrollLimitY = UnitUtil.getDp(activity, 24);
  }

  /**
   * Call this before setUpScroll() if the activity has a bottomAppBar! Hides NavBarDivider because
   * you don't need it with a bottomAppBar.
   */
  public void setUpBottomAppBar(CustomBottomAppBar bottomAppBar) {
    this.bottomAppBar = bottomAppBar;
  }

  /**
   * Call this before setUpScroll() if the activity has a to scroll button! But call this AFTER
   * setUpBottomAppBar() if the activity has a bottomAppBar!
   */
  public void setUpTopScroll(@IdRes int fabId) {
    fabScroll = activity.findViewById(fabId);
    if (bottomAppBar != null) {
      bottomAppBar.setOnShowListener(() -> animateTopScrollTo(
          0,
          bottomAppBar.getEnterAnimationDuration(),
          AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR
      ));
      bottomAppBar.setOnHideListener(() -> animateTopScrollTo(
          bottomAppBar.getMeasuredHeight(),
          bottomAppBar.getExitAnimationDuration(),
          AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR
      ));
    }
  }

  /**
   * Initializes the scroll view behavior like liftOnScroll etc.
   */
  public void setUpScroll(@IdRes int nestedScrollViewId) {
    if (activity == null) {
      if (DEBUG) {
        Log.e(TAG, "setUpScroll: activity is null!");
      }
      return;
    }
    setUpScroll(activity.findViewById(nestedScrollViewId));
  }

  /**
   * Initializes the scroll view behavior like liftOnScroll etc.
   */
  public void setUpScroll(View scrollViewNew) {
    this.scrollView = scrollViewNew;
    currentState = STATE_SCROLLED_UP;
    if (fabScroll != null) {
      fabScroll.hide();
    }

    if (scrollView != null && scrollView instanceof RecyclerView) {
      ((RecyclerView) scrollView).addOnScrollListener(onScrollListenerRecycler());
    } else if (scrollView != null && scrollView instanceof NestedScrollView) {
      ((NestedScrollView) scrollView).setOnScrollChangeListener(onScrollChangeListener());
    }

    if (fabScroll != null && scrollView != null) {
      fabScroll.setOnClickListener(v -> {
        if (scrollView != null && scrollView instanceof RecyclerView) {
          ((RecyclerView) scrollView).smoothScrollToPosition(0);
        } else if (scrollView != null && scrollView instanceof NestedScrollView) {
          ((NestedScrollView) scrollView).smoothScrollTo(0, 0);
        }
        fabScroll.hide();
      });
    }
    if (bottomAppBar != null) {
      bottomAppBar.show();
      onChangeBottomAppBarVisibility(true, "setUpScroll");
      if (scrollView != null && scrollView.getScrollY() == 0 && activity instanceof MainActivity) {
        ((MainActivity) activity).isScrollRestored = false;
      }
    }
    if (DEBUG) {
      Log.i(TAG, "setUpScroll with ScrollView");
    }
  }

  private RecyclerView.OnScrollListener onScrollListenerRecycler() {
    return new RecyclerView.OnScrollListener() {
      @Override
      public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        int scrollAbsoluteY = recyclerView.computeVerticalScrollOffset();
        if (!isTopScroll && scrollAbsoluteY == 0) { // TOP
          onTopScroll();
        } else if (dy < 0) {
          storedFirstBottomScrollY = 0;
          if (currentState != STATE_SCROLLED_UP) {
            onScrollUp();
          }
          if (scrollAbsoluteY < topScrollLimit && fabScroll != null && showTopScroll) {
            if (fabScroll.isOrWillBeShown()) {
              fabScroll.hide();
            }
          }
        } else if (dy > 0) {
          if (storedFirstBottomScrollY == 0) {
            storedFirstBottomScrollY = scrollAbsoluteY;
          }
          int scrollYHide = storedFirstBottomScrollY + scrollLimitY;
          if (currentState != STATE_SCROLLED_DOWN && scrollAbsoluteY > scrollYHide) { // DOWN
            onScrollDown();
          }
          if (scrollAbsoluteY > topScrollLimit && fabScroll != null && showTopScroll) {
            if (fabScroll.isOrWillBeHidden()) {
              fabScroll.show();
            }
          }
        }
      }
    };
  }

  private NestedScrollView.OnScrollChangeListener onScrollChangeListener() {
    return (NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
      if (DEBUG) {
        Log.i(
            TAG,
            "onScrollChangeListener: newY = " + scrollY + ", oldY = " + oldScrollY
        );
      }
      if (oldScrollY == 0 && scrollY > 0) {
        return; // prevent hiding bottom app bar after pressing back button
      }
      if (!isTopScroll && scrollY == 0) { // TOP
        onTopScroll();
      } else {
        if (scrollY < oldScrollY) { // UP
          storedFirstBottomScrollY = 0;
          if (currentState != STATE_SCROLLED_UP) {
            onScrollUp();
          }
          if (scrollY < topScrollLimit && fabScroll != null && showTopScroll) {
            if (fabScroll.isOrWillBeShown()) {
              fabScroll.hide();
            }
          }
        } else if (scrollY > oldScrollY) {
          if (storedFirstBottomScrollY == 0) {
            storedFirstBottomScrollY = oldScrollY;
          }
          int scrollYHide = storedFirstBottomScrollY + scrollLimitY;
          if (currentState != STATE_SCROLLED_DOWN && scrollY > scrollYHide) { // DOWN
            onScrollDown();
          }
          if (scrollY > topScrollLimit && fabScroll != null && showTopScroll) {
            if (fabScroll.isOrWillBeHidden()) {
              fabScroll.show();
            }
          }
        }
      }
    };
  }

  /**
   * Gets called once when scrollY is 0.
   */
  private void onTopScroll() {
    isTopScroll = true;
    scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
    if (bottomAppBar != null) {
      if (bottomAppBar.isOrWillBeShown()) {
        if (DEBUG) {
          Log.i(TAG, "onTopScroll: bottomAppBar already shown");
        }
        return;
      }
      bottomAppBar.show();
      onChangeBottomAppBarVisibility(true, "onTopScroll");
    } else if (DEBUG) {
      Log.e(TAG, "onTopScroll: bottomAppBar is null!");
    }
  }

  /**
   * Gets called once when the user scrolls up.
   */
  private void onScrollUp() {
    currentState = STATE_SCROLLED_UP;
    scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
    if (bottomAppBar != null) {
      bottomAppBar.show();
      onChangeBottomAppBarVisibility(true, "onScrollUp");
    } else if (DEBUG) {
      Log.e(TAG, "onScrollUp: bottomAppBar is null!");
    }
    if (DEBUG) {
      Log.i(TAG, "onScrollUp: UP");
    }
  }

  /**
   * Gets called once when the user scrolls down.
   */
  private void onScrollDown() {
    isTopScroll = false; // second top scroll is unrealistic before down scroll
    currentState = STATE_SCROLLED_DOWN;
    scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
    if (bottomAppBar != null) {
      if (hideOnScroll) {
        if (activity instanceof MainActivity) {
          boolean isScrollRestored = ((MainActivity) activity).isScrollRestored;
          if (!isScrollRestored) {
            bottomAppBar.hide();
            onChangeBottomAppBarVisibility(false, "onScrollDown");
          } else {
            ((MainActivity) activity).isScrollRestored = false;
          }
        } else {
          bottomAppBar.hide();
          onChangeBottomAppBarVisibility(false, "onScrollDown");
        }
      }
    } else if (DEBUG) {
      Log.e(TAG, "onScrollDown: bottomAppBar is null!");
    }
    if (DEBUG) {
      Log.i(TAG, "onScrollDown: DOWN");
    }
  }

  /**
   * Sets the global boolean and moves the bottomAppBar manually if necessary.
   */
  public void setHideOnScroll(boolean hide) {
    hideOnScroll = hide;
    if (bottomAppBar != null) {
      if (!hide && !bottomAppBar.isOrWillBeShown()) {
        bottomAppBar.show();
        onChangeBottomAppBarVisibility(true, "setHideOnScroll");
      }
    } else if (DEBUG) {
      Log.e(TAG, "setHideOnScroll: bottomAppBar is null!");
    }
    if (DEBUG) {
      Log.i(TAG, "setHideOnScroll(" + hide + ")");
    }
  }

  /**
   * Call this after setUpScroll()!
   */
  public void setTopScrollVisibility(boolean visible) {
    showTopScroll = visible;
    if (fabScroll != null) {
      if (!visible && fabScroll.isOrWillBeShown()) {
        fabScroll.hide();
      }
    }
  }

  private void onChangeBottomAppBarVisibility(boolean visible, String origin) {
    if (activity != null) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        if (DEBUG) {
          Log.i(TAG, "onChangeBottomAppBarVisibility: SDK below 29");
        }
        return;
      }
      int dividerCurrentColor = activity.getWindow().getNavigationBarDividerColor();
      int dividerTargetColor = ContextCompat.getColor(
          activity, visible ? R.color.primary : R.color.stroke_secondary
      );
      if (dividerCurrentColor != dividerTargetColor) {
        ValueAnimator valueAnimator = ValueAnimator.ofArgb(
            dividerCurrentColor, dividerTargetColor
        );
        valueAnimator.addUpdateListener(
            animation -> activity.getWindow().setNavigationBarDividerColor(
                (int) valueAnimator.getAnimatedValue()
            )
        );
        valueAnimator.setDuration(200).start();
      } else if (DEBUG) {
        Log.i(TAG, "onHideBottomAppBar: current and target identical");
      }
      int navBarCurrentColor = activity.getWindow().getNavigationBarColor();
      int navBarTargetColor = ContextCompat.getColor(
          activity, visible ? R.color.primary : R.color.background
      );
      if (navBarCurrentColor != navBarTargetColor) {
        ValueAnimator valueAnimator = ValueAnimator.ofArgb(
            navBarCurrentColor, navBarTargetColor
        );
        valueAnimator.addUpdateListener(
            animation -> activity.getWindow().setNavigationBarColor(
                (int) valueAnimator.getAnimatedValue()
            )
        );
        valueAnimator.setStartDelay(visible ? 0 : 100);
        valueAnimator.setDuration(visible ? 70 : 100).start();
      } else if (DEBUG) {
        Log.i(
            TAG, "onChangeBottomBarVisibility("
                + origin
                + "): current and target identical"
        );
      }
    } else if (DEBUG) {
      Log.e(TAG, "onChangeBottomBarVisibility("
          + origin
          + "): activity is null!"
      );
    }
  }

  private void animateTopScrollTo(int targetY, long duration, TimeInterpolator interpolator) {
    if (fabScroll != null && showTopScroll) {
      if (topScrollAnimator != null) {
        topScrollAnimator.cancel();
        fabScroll.clearAnimation();
      }
      topScrollAnimator = fabScroll.animate()
          .translationY(targetY)
          .setInterpolator(interpolator)
          .setDuration(duration)
          .setListener(
              new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                  topScrollAnimator = null;
                }
              });
    }
  }
}
