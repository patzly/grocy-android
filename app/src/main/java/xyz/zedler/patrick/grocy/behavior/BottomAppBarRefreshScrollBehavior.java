package xyz.zedler.patrick.grocy.behavior;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;

import androidx.annotation.IdRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.animation.AnimationUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.UnitUtil;
import xyz.zedler.patrick.grocy.view.CustomBottomAppBar;

public class BottomAppBarRefreshScrollBehavior {

	private final static String TAG = "ScrollBehavior";
	private final static boolean DEBUG = false;

	private static final int STATE_SCROLLED_DOWN = 1;
	private static final int STATE_SCROLLED_UP = 2;

	private int currentState = STATE_SCROLLED_UP;
	private int pufferSize = 0; // distance before top scroll when overScroll is turned off
	private int pufferDivider = 2; // distance gets divided to prevent cutoff of edge effect
	private int topScrollLimit = 100;
	private int storedFirstBottomScrollY = 0;

	private boolean isTopScroll = false;
	private boolean hideOnScroll = true;
	private boolean showTopScroll = true;

	private Activity activity;
	private CustomBottomAppBar bottomAppBar;
	private FloatingActionButton fabScroll;
	private NestedScrollView nestedScrollView;
	private ViewPropertyAnimator topScrollAnimator;

	public BottomAppBarRefreshScrollBehavior(Activity activity) {
		this.activity = activity;
	}

	/**
	 * Call this before setUpScroll() if the activity has a bottomAppBar!
	 * Hides NavBarDivider because you don't need it with a bottomAppBar.
	 */
	public void setUpBottomAppBar(CustomBottomAppBar bottomAppBar) {
		this.bottomAppBar = bottomAppBar;
	}

	/**
	 * Call this before setUpScroll() if the activity has a to scroll button!
	 * But call this AFTER setUpBottomAppBar() if the activity has a bottomAppBar!
	 */
	public void setUpTopScroll(@IdRes int fabId) {
		fabScroll = activity.findViewById(fabId);
		if(bottomAppBar != null) {
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
		if(activity == null) {
			if(DEBUG) Log.e(TAG, "setUpScroll: activity is missing!");
			return;
		}
		nestedScrollView = activity.findViewById(nestedScrollViewId);
		currentState = STATE_SCROLLED_UP;
		if(fabScroll != null) fabScroll.hide();
		measureScrollView();
		if(nestedScrollView != null) {
			nestedScrollView.setOnScrollChangeListener((NestedScrollView v,
                                                        int scrollX,
                                                        int scrollY,
                                                        int oldScrollX,
                                                        int oldScrollY
			) -> {
				if (!isTopScroll && scrollY == 0) { // TOP
					onTopScroll();
				} else {
					if (scrollY < oldScrollY) { // UP
						storedFirstBottomScrollY = 0;
						if (currentState != STATE_SCROLLED_UP) {
							onScrollUp();
						}
						if (scrollY < pufferSize) {
							new Handler().postDelayed(() -> {
								if (scrollY > 0) {
									nestedScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
								}
							}, 1);
						}
						if (scrollY < dp(topScrollLimit) && fabScroll != null && showTopScroll) {
							if (fabScroll.isOrWillBeShown()) fabScroll.hide();
						}
					} else if (scrollY > oldScrollY) {
						if(storedFirstBottomScrollY == 0) {
							storedFirstBottomScrollY = oldScrollY;
						}
						int scrollYHide = storedFirstBottomScrollY + UnitUtil.getDp(
								activity,
								24
						);
						if (currentState != STATE_SCROLLED_DOWN && scrollY > scrollYHide) { // DOWN
							onScrollDown();
						}
						if (scrollY > dp(topScrollLimit) && fabScroll != null && showTopScroll) {
							if (fabScroll.isOrWillBeHidden()) fabScroll.show();
						}
					}
				}
			});
		}
		if(fabScroll != null) {
			fabScroll.setOnClickListener(v -> {
				nestedScrollView.smoothScrollTo(0, 0);
				fabScroll.hide();
			});
		}
		if(DEBUG) Log.i(TAG, "setUpScroll");
	}

	/**
	 * Gets called once when scrollY is 0.
	 */
	private void onTopScroll() {
		isTopScroll = true;
		if(bottomAppBar != null) {
			bottomAppBar.show();
			onChangeBottomAppBarVisibility(true);
		} else if(DEBUG) Log.e(TAG, "onTopScroll: bottomAppBar is null!");
	}

	/**
	 * Gets called once when the user scrolls up.
	 */
	private void onScrollUp() {
		currentState = STATE_SCROLLED_UP;
		if(bottomAppBar != null) {
			bottomAppBar.show();
			onChangeBottomAppBarVisibility(true);
		} else if(DEBUG) Log.e(TAG, "onScrollUp: bottomAppBar is null!");
		if(DEBUG) Log.i(TAG, "onScrollUp: UP");
	}

	/**
	 * Gets called once when the user scrolls down.
	 */
	private void onScrollDown() {
		isTopScroll = false; // second top scroll is unrealistic before down scroll
		currentState = STATE_SCROLLED_DOWN;
		nestedScrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
		if(bottomAppBar != null) {
			if(hideOnScroll) {
				bottomAppBar.hide();
				onChangeBottomAppBarVisibility(false);
			}
		} else if(DEBUG) Log.e(TAG, "onScrollDown: bottomAppBar is null!");
		if(DEBUG) Log.i(TAG, "onScrollDown: DOWN");
	}

	/**
	 * Sets the global boolean and moves the bottomAppBar manually if necessary.
	 */
	public void setHideOnScroll(boolean hide) {
		hideOnScroll = hide;
		if(bottomAppBar != null) {
			if(!hide) {
				bottomAppBar.show();
				onChangeBottomAppBarVisibility(true);
			}
		} else if(DEBUG) Log.e(TAG, "setHideOnScroll: bottomAppBar is null!");
		if(DEBUG) Log.i(TAG, "setHideOnScroll(" + hide + ")");
	}

	/**
	 * Adds a globalLayoutListener to the scrollView to get its own and the content's height.
	 */
	private void measureScrollView() {
		if(nestedScrollView != null) {
			nestedScrollView.getViewTreeObserver().addOnGlobalLayoutListener(
					new ViewTreeObserver.OnGlobalLayoutListener() {
						@Override
						public void onGlobalLayout() {
							int scrollViewHeight = nestedScrollView.getMeasuredHeight();
							int scrollContentHeight = nestedScrollView.getChildAt(
									0
							).getHeight();
							pufferSize = (scrollContentHeight - scrollViewHeight) / pufferDivider;
							if(DEBUG) {
								Log.i(TAG, "measureScrollView: viewHeight = " +
										scrollViewHeight +
										", contentHeight = " + scrollContentHeight
								);
							}
							// Kill ViewTreeObserver
							/*if (nestedScrollView.getViewTreeObserver().isAlive()) {
								nestedScrollView.getViewTreeObserver()
										.removeOnGlobalLayoutListener(
												this
										);
							}*/
						}
					});
		}
	}

	/**
	 * Call this after setUpScroll()!
	 */
	public void setTopScrollVisibility(boolean visible) {
		showTopScroll = visible;
		if(fabScroll != null) {
			if(!visible && fabScroll.isOrWillBeShown()) fabScroll.hide();
		}
	}

	private void onChangeBottomAppBarVisibility(boolean visible) {
		if(activity != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
			int dividerCurrentColor = activity.getWindow().getNavigationBarDividerColor();
			int dividerTargetColor = ContextCompat.getColor(
					activity, visible ? R.color.primary : R.color.stroke_secondary
			);
			if(dividerCurrentColor != dividerTargetColor) {
				ValueAnimator valueAnimator = ValueAnimator.ofArgb(
						dividerCurrentColor, dividerTargetColor
				);
				valueAnimator.addUpdateListener(
						animation -> activity.getWindow().setNavigationBarDividerColor(
								(int) valueAnimator.getAnimatedValue()
						)
				);
				valueAnimator.setDuration(200).start();
			} else if(DEBUG) Log.i(TAG, "onHideBottomAppBar: current and target identical");
			int navBarCurrentColor = activity.getWindow().getNavigationBarColor();
			int navBarTargetColor = ContextCompat.getColor(
					activity, visible ? R.color.primary : R.color.background
			);
			if(navBarCurrentColor != navBarTargetColor) {
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
			} else if(DEBUG) Log.i(TAG, "onHideBottomAppBar: current and target identical");
		} else if(DEBUG) Log.e(TAG, "onHideBottomAppBar: activity is null!");
	}

	private void animateTopScrollTo(int targetY, long duration, TimeInterpolator interpolator) {
		if(fabScroll != null && showTopScroll) {
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

	private int dp(float dp){
		return (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP,
				dp,
				activity.getResources().getDisplayMetrics()
		);
	}
}
