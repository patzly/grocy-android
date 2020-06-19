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

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.IdRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.AppBarLayout;

import xyz.zedler.patrick.grocy.R;

public class AppBarScrollBehavior {

	private final static String TAG = AppBarScrollBehavior.class.getSimpleName();
	private final static boolean DEBUG = false;

	private static final int STATE_SCROLLED_DOWN = 1;
	private static final int STATE_SCROLLED_UP = 2;

	private int currentState = STATE_SCROLLED_UP;
	private int pufferSize = 0; // distance before top scroll when overScroll is turned off
	private int pufferDivider = 2; // distance gets divided to prevent cutoff of edge effect

	private boolean isTopScroll = false;
	private boolean liftOnScroll = true;
	private boolean showNavBarDivider = true;

	private Activity activity;
	private AppBarLayout appBarLayout;
	private LinearLayout linearAppBar;
	private NestedScrollView nestedScrollView;

	/**
	 * Initializes the scroll view behavior like liftOnScroll etc.
	 */
	public void setUpScroll(
			Activity targetActivity,
			@IdRes int appBarLayoutId,
			@IdRes int linearAppBarId,
			@IdRes int nestedScrollViewId,
			boolean liftOnScroll
	) {
		activity = targetActivity;
		
		this.liftOnScroll = liftOnScroll;
		appBarLayout = activity.findViewById(appBarLayoutId);
		linearAppBar = activity.findViewById(linearAppBarId);
		nestedScrollView = activity.findViewById(nestedScrollViewId);
		currentState = STATE_SCROLLED_UP;
		measureScrollView();
		setLiftOnScroll(liftOnScroll);
		if(nestedScrollView != null) {
			nestedScrollView.setOnScrollChangeListener((NestedScrollView v,
                                                        int scrollX,
                                                        int scrollY,
                                                        int oldScrollX,
                                                        int oldScrollY
			) -> {
				if(!isTopScroll && scrollY == 0) { // TOP
					onTopScroll();
				} else {
					if(scrollY < oldScrollY) { // UP
						if(currentState != STATE_SCROLLED_UP) {
							onScrollUp();
						}
						if (liftOnScroll) {
							if (scrollY < pufferSize) {
								new Handler().postDelayed(() -> {
									if (scrollY > 0) {
										nestedScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
									}
								}, 1);
							}
						}
					} else if(scrollY > oldScrollY) {
						if(currentState != STATE_SCROLLED_DOWN) { // DOWN
							onScrollDown();
						}
					}
				}
			});
		}
		if(DEBUG) Log.i(TAG, "setUpScroll: liftOnScroll = " + liftOnScroll);
	}

	/**
	 * Gets called once when scrollY is 0.
	 */
	private void onTopScroll() {
		isTopScroll = true;
		if(appBarLayout != null) {
			if(liftOnScroll) {
				tintTopBars(R.color.background);
				appBarLayout.setLifted(false);
			}
			if(DEBUG) Log.i(TAG, "onTopScroll: liftOnScroll = " + liftOnScroll);
		} else if(DEBUG) {
			Log.e(TAG, "onTopScroll: appBarLayout is null!");
		}
	}

	/**
	 * Gets called once when the user scrolls up.
	 */
	private void onScrollUp() {
		currentState = STATE_SCROLLED_UP;
		/*if(liftOnScroll) {
			nestedScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
		} else {
			nestedScrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
		}*/
		if(appBarLayout != null) {
			appBarLayout.setLifted(true);
			tintTopBars(R.color.primary);
		} else if(DEBUG) {
			Log.e(TAG, "onScrollUp: appBarLayout is null!");
		}
		if(DEBUG) Log.i(TAG, "onScrollUp: UP");
	}

	/**
	 * Gets called once when the user scrolls down.
	 */
	private void onScrollDown() {
		isTopScroll = false; // second top scroll is unrealistic before down scroll
		currentState = STATE_SCROLLED_DOWN;
		if(appBarLayout != null && nestedScrollView != null) {
			appBarLayout.setLifted(true);
			tintTopBars(R.color.primary);
			nestedScrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
		} else if(DEBUG) {
			Log.e(TAG, "onScrollDown: appBarLayout or scrollView is null!");
		}
		if(DEBUG) Log.i(TAG, "onScrollDown: DOWN");
	}

	/**
	 * Sets the global boolean and moves the appBar manually if necessary.
	 * If scrollY of the scrollView is 0, OverScroll is turned off.
	 * Otherwise it's on if the the view is scrollable.
	 */
	public void setLiftOnScroll(boolean lift) {
		liftOnScroll = lift;
		if(appBarLayout != null) {
			appBarLayout.setLiftOnScroll(false); // We'll make this manually
			appBarLayout.setLiftable(true);
			if(nestedScrollView != null) {
				if(lift) {
					if(nestedScrollView.getScrollY() == 0) {
						appBarLayout.setLifted(false);
						tintTopBars(R.color.background);
						nestedScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
					} else {
						appBarLayout.setLifted(true);
						tintTopBars(R.color.primary);
					}
				} else {
					appBarLayout.setLifted(true);
					tintTopBars(R.color.primary);
					nestedScrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
				}
			} else {
				if(lift) {
					appBarLayout.setLiftable(true);
					appBarLayout.setLifted(false);
					tintTopBars(R.color.background);
				} else {
					appBarLayout.setLiftable(false);
					//appBarLayout.setLifted(true);
					tintTopBars(R.color.primary);
				}
			}
		} else if(DEBUG) Log.e(TAG, "setLiftOnScroll: appBarLayout is null!");
		if(DEBUG) Log.i(TAG, "setLiftOnScroll(" + lift + ")");
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
							if(nestedScrollView.getChildAt(0) != null) {
								int scrollContentHeight = nestedScrollView.getChildAt(
										0
								).getHeight();
								showNavBarDivider = scrollViewHeight - scrollContentHeight < 0;
								setNavBarDividerVisibility();
								pufferSize = (scrollContentHeight-scrollViewHeight) / pufferDivider;
								if(DEBUG) {
									Log.i(TAG, "measureScrollView: viewHeight = "
											+ scrollViewHeight + ", contentHeight = "
											+ scrollContentHeight
									);
								}
							} else {
								if(DEBUG) Log.e(TAG, "measureScrollView: no child!");
							}
							// Kill ViewTreeObserver
							if(nestedScrollView.getViewTreeObserver().isAlive()) {
								nestedScrollView.getViewTreeObserver().removeOnGlobalLayoutListener(
										this
								);
							}
						}
					});
		}
	}

	/**
	 * Tints the navBarDivider with divider color if setUpBottomAppBar wasn't called before
	 * and the scrollView is scrollable, else transparent.
	 */
	private void setNavBarDividerVisibility() {
		if(activity != null) {
			int orientation = activity.getResources().getConfiguration().orientation;
			if(orientation == Configuration.ORIENTATION_PORTRAIT) {
				if(showNavBarDivider) {
					setNavBarDividerColor(R.color.stroke_secondary);
				} else {
					setNavBarDividerColor(R.color.transparent);
				}
				if(DEBUG) Log.i(TAG, "setNavBarDividerVisibility(" + showNavBarDivider + ")");
			} else {
				setNavBarDividerColor(R.color.stroke_secondary);
			}
		} else if(DEBUG) Log.wtf(TAG, "setNavBarDividerVisibility: activity is null!?");
	}

	/**
	 * Shows navBarDivider in portrait and/or landscape mode independent of the scrollView
	 */
	public void setNavBarDividerVisibility(boolean portrait, boolean landscape) {
		if(activity != null) {
			int orientation = activity.getResources().getConfiguration().orientation;
			if(orientation == Configuration.ORIENTATION_PORTRAIT) {
				if(portrait) {
					setNavBarDividerColor(R.color.stroke_secondary);
				} else {
					setNavBarDividerColor(R.color.transparent);
				}
				if(DEBUG) Log.i(TAG, "setNavBarDividerVisibility(" + showNavBarDivider + ")");
			} else {
				if(landscape) {
					setNavBarDividerColor(R.color.stroke_secondary);
				} else {
					setNavBarDividerColor(R.color.transparent);
				}
			}
		} else if(DEBUG) Log.wtf(TAG, "setNavBarDividerVisibility(): activity is null!?");
	}

	/**
	 * If SDK version is 28 or higher this tints the navBarDivider.
	 */
	private void setNavBarDividerColor(@ColorRes int color) {
		if(activity != null && Build.VERSION.SDK_INT >= 28) {
			activity.getWindow().setNavigationBarDividerColor(
					ContextCompat.getColor(activity, color)
			);
		} else if(DEBUG) Log.i(TAG, "setNavBarDividerColor: activity is null or SDK < 28");
	}

	@SuppressLint("PrivateResource")
	private void tintTopBars(@ColorRes int target) {
		if(activity != null && linearAppBar != null) {
			int appBarColor = ((ColorDrawable) linearAppBar.getBackground()).getColor();
			int statusBarColor = 0;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				statusBarColor = activity.getWindow().getStatusBarColor();
			}
			int targetColor = ContextCompat.getColor(activity, target);
			if(appBarColor != targetColor || statusBarColor != targetColor) {
				ValueAnimator valueAnimator = ValueAnimator.ofArgb(appBarColor, targetColor);
				valueAnimator.addUpdateListener(animation -> {
					if(linearAppBar != null) {
						linearAppBar.setBackgroundColor((int) valueAnimator.getAnimatedValue());
					}
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
						if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M
								&& appBarColor == ContextCompat.getColor(activity, R.color.white)
						) {
							activity.getWindow().setStatusBarColor(
									ContextCompat.getColor(activity, R.color.status_bar_lollipop)
							);
						} else {
							activity.getWindow().setStatusBarColor(
									(int) valueAnimator.getAnimatedValue()
							);
						}
					}
				});
				valueAnimator.setDuration(activity.getResources().getInteger(
						R.integer.app_bar_elevation_anim_duration
				)).start();
				if(DEBUG) Log.i(TAG, "tintTopBars: appBarLinearLayout and status bar tinted");
			} else {
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
						&& appBarColor == ContextCompat.getColor(activity, R.color.white)
				) {
					activity.getWindow().setStatusBarColor(
							ContextCompat.getColor(activity, R.color.status_bar_lollipop)
					);
				}
				if(DEBUG) Log.i(TAG, "tintTopBars: current and target identical");
			}
		} else if(DEBUG) Log.e(TAG, "tintTopBars: activity or appBarLinearLayout is null!");
	}
}
