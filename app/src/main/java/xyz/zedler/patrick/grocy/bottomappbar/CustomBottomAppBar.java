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

package xyz.zedler.patrick.grocy.bottomappbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewPropertyAnimator;
import androidx.annotation.DrawableRes;
import androidx.annotation.MenuRes;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutLinearInInterpolator;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class CustomBottomAppBar extends BottomAppBar {

  private final static String TAG = "CustomBottomAppBar";

  public static final int MENU_START = 0, MENU_END = 1;

  private static final int ENTER_ANIMATION_DURATION = 225;
  private static final int EXIT_ANIMATION_DURATION = 175;
  private static final int INVISIBLE = 0, VISIBLE = 1;
  private static final int ICON_ANIM_DURATION = 200;
  private static final double ICON_ANIM_DELAY_FACTOR = 0.7;

  private boolean isOrWillBeShown = true;

  private ViewPropertyAnimator currentAnimator;
  private ValueAnimator valueAnimatorNavigationIcon;

  private Runnable runnableOnShow;
  private Runnable runnableOnHide;

  public CustomBottomAppBar(Context context) {
    super(context);
  }

  public CustomBottomAppBar(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  /**
   * Animatedly shows the bottomAppBar.
   */
  public void show() {
    isOrWillBeShown = true;
    animateTo(
        0,
        ENTER_ANIMATION_DURATION,
        new LinearOutSlowInInterpolator()
    );
    if (runnableOnShow != null) {
      runnableOnShow.run();
    }
  }

  /**
   * Animatedly hides the bottomAppBar.
   */
  public void hide() {
    MarginLayoutParams params = (MarginLayoutParams) getLayoutParams();
    int addOffset = /*Build.VERSION.SDK_INT == 27 ? 0 :*/ UiUtil.dpToPx(getContext(), 10);
    isOrWillBeShown = false;
    animateTo(
        getMeasuredHeight()
            + params.bottomMargin
            + addOffset,
        EXIT_ANIMATION_DURATION,
        new FastOutLinearInInterpolator()
    );
    if (runnableOnHide != null) {
      runnableOnHide.run();
    }
  }

  public boolean isOrWillBeShown() {
    return isOrWillBeShown;
  }

  public int getEnterAnimationDuration() {
    return ENTER_ANIMATION_DURATION;
  }

  public int getExitAnimationDuration() {
    return EXIT_ANIMATION_DURATION;
  }

  /**
   * Runs the runnable if bottomAppBar is manually shown.
   */
  public void setOnShowListener(Runnable runnable) {
    runnableOnShow = runnable;
  }

  /**
   * Runs the runnable if bottomAppBar is manually hidden.
   */
  public void setOnHideListener(Runnable runnable) {
    runnableOnHide = runnable;
  }

  /**
   * Sets the visibility of the menu
   */
  public void setMenuVisibility(boolean visible) {
    Menu menu = getMenu();
    if (menu != null && menu.size() > 0) {
      MenuItem item;
      for (int i = 0; i < menu.size(); i++) {
        item = menu.getItem(i);
        if (item.getIcon() != null) {
          item.getIcon().setAlpha((visible) ? 255 : 0);
        } else if (item.getActionView() != null) {
          item.getActionView().setAlpha((visible) ? 1 : 0);
        }
      }
    }
  }

  /**
   * Animatedly changes the menu
   */
  public void changeMenu(@MenuRes int menuNew, int position, boolean animated) {
    if (animated) {
      for (int i = 0; i < this.getMenu().size(); i++) {
        animateMenuItem(getMenu().getItem(i), INVISIBLE, null);
      }
      new Handler().postDelayed(() -> {
        replaceMenu(menuNew);
        setMenuVisibility(false);
        switch (position) {
          case MENU_START:
            animateMenu(0, null);
            break;
          case MENU_END:
            animateMenu(getMenu().size() - 1, null);
            break;
          default:
            Log.e(TAG, "changeMenu: wrong argument: " + position);
        }
      }, ICON_ANIM_DURATION);
    } else {
      replaceMenu(menuNew);
      setMenuVisibility(true);
    }
  }

  /**
   * Animatedly changes the menu and runs an action after it has changed
   */
  public void changeMenu(
      @MenuRes int menuNew,
      int position,
      boolean animated,
      Runnable onChanged
  ) {
    if (animated) {
      for (int i = 0; i < this.getMenu().size(); i++) {
        animateMenuItem(getMenu().getItem(i), INVISIBLE, null);
      }
      new Handler().postDelayed(() -> {
        replaceMenu(menuNew);
        setMenuVisibility(false);
        switch (position) {
          case MENU_START:
            animateMenu(0, onChanged);
            break;
          case MENU_END:
            animateMenu(getMenu().size() - 1, onChanged);
            break;
          default:
            Log.e(TAG, "changeMenu: wrong argument: " + position);
        }
      }, ICON_ANIM_DURATION);
    } else {
      replaceMenu(menuNew);
      setMenuVisibility(true);
      onChanged.run();
    }
  }

  /**
   * Animatedly shows the navigation icon
   *
   * @param navigationResId necessary because nav icon has to be null for being gone
   */
  public void showNavigationIcon(@DrawableRes int navigationResId, boolean animated) {
    if (getNavigationIcon() == null) {
      Drawable navigationIcon = ContextCompat.getDrawable(getContext(), navigationResId);
      assert navigationIcon != null;
      if (animated) {
        new Handler().postDelayed(() -> {
          setNavigationIcon(navigationIcon);
          valueAnimatorNavigationIcon = ValueAnimator
              .ofInt(navigationIcon.getAlpha(), 255)
              .setDuration(ICON_ANIM_DURATION);
          valueAnimatorNavigationIcon.removeAllUpdateListeners();
          valueAnimatorNavigationIcon.addUpdateListener(
              animation -> navigationIcon.setAlpha(
                  (int) (animation.getAnimatedValue())
              )
          );
          valueAnimatorNavigationIcon.start();
        }, ICON_ANIM_DURATION);
      } else {
        navigationIcon.setAlpha(255);
        setNavigationIcon(navigationIcon);
      }
    }
  }

  public void showNavigationIcon(@DrawableRes int navigationResId) {
    showNavigationIcon(navigationResId, true);
  }

  /**
   * Animatedly hides the navigation icon
   */
  public void hideNavigationIcon(boolean animated) {
    if (getNavigationIcon() != null) {
      Drawable navigationIcon = getNavigationIcon();
      if (animated) {
        valueAnimatorNavigationIcon = ValueAnimator
            .ofInt(navigationIcon.getAlpha(), 0)
            .setDuration(ICON_ANIM_DURATION);
        valueAnimatorNavigationIcon.removeAllUpdateListeners();
        valueAnimatorNavigationIcon.addUpdateListener(
            animation -> navigationIcon.setAlpha((int) (animation.getAnimatedValue()))
        );
        valueAnimatorNavigationIcon.start();
        new Handler().postDelayed(
            () -> setNavigationIcon(null), ICON_ANIM_DURATION + 5
        );
      } else {
        setNavigationIcon(null);
      }
    }
  }

  public void hideNavigationIcon() {
    hideNavigationIcon(true);
  }

  /**
   * Returns if the navigation icon is visible
   */
  public boolean isNavigationIconVisible() {
    return getNavigationIcon() != null;
  }

  private void animateTo(int targetY, long duration, TimeInterpolator interpolator) {
    if (currentAnimator != null) {
      currentAnimator.cancel();
      clearAnimation();
    }
    currentAnimator = animate()
        .translationY(targetY)
        .setInterpolator(interpolator)
        .setDuration(duration)
        .setListener(
            new AnimatorListenerAdapter() {
              @Override
              public void onAnimationEnd(Animator animation) {
                currentAnimator = null;
              }
            });
  }

  private void animateMenu(int indexStart, Runnable onAnimEnd) {
    int delayIndex = 0;
    for (
        int i = indexStart;
        indexStart == 0 ? i < getMenu().size() : i >= 0;
        i = (indexStart == 0) ? i + 1 : i - 1
    ) {
      int finalI = i;
      new Handler().postDelayed(
          () -> {
            if (finalI >= getMenu().size() || finalI < 0) {
              return;
            }
            animateMenuItem(
                getMenu().getItem(finalI),
                VISIBLE,
                (indexStart == 0 ? finalI == getMenu().size() - 1 : finalI == 0)
                    ? onAnimEnd
                    : null
            );
          }, (long) (delayIndex * ICON_ANIM_DELAY_FACTOR * ICON_ANIM_DURATION)
      );
      delayIndex++;
    }
  }

  private void animateMenuItem(MenuItem item, int visibility, Runnable onAnimEnd) {
    if (item.getIcon() != null) {
      Drawable icon = item.getIcon();
      int targetAlpha;
      switch (visibility) {
        case VISIBLE:
          targetAlpha = 255;
          break;
        case INVISIBLE:
          targetAlpha = 0;
          break;
        default:
          return;
      }
      ValueAnimator alphaAnimator = ValueAnimator.ofInt(icon.getAlpha(), targetAlpha);
      alphaAnimator.addUpdateListener(
          animation -> icon.setAlpha((int) (animation.getAnimatedValue()))
      );
      alphaAnimator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          if (onAnimEnd != null) {
            onAnimEnd.run();
          }
        }
      });
      alphaAnimator.setDuration(ICON_ANIM_DURATION).start();
    }
  }

  @Override
  public void setHideOnScroll(boolean hide) {
  }

  @Override
  public boolean getHideOnScroll() {
    return false;
  }

  @Nullable
  private View findDependentView() {
    if (!(getParent() instanceof CoordinatorLayout)) {
      return null;
    }
    List<View> dependents = ((CoordinatorLayout) getParent()).getDependents(this);
    for (View v : dependents) {
      if (v instanceof FloatingActionButton || v instanceof ExtendedFloatingActionButton) {
        return v;
      }
    }
    return null;
  }

  @Nullable
  private FloatingActionButton findDependentFab() {
    View view = findDependentView();
    return view instanceof FloatingActionButton ? (FloatingActionButton) view : null;
  }

  private boolean isFabVisibleOrWillBeShown() {
    FloatingActionButton fab = findDependentFab();
    return fab != null && fab.isOrWillBeShown();
  }
}