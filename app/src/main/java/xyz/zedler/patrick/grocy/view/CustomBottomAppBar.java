package xyz.zedler.patrick.grocy.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
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

import com.google.android.material.animation.AnimationUtils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.util.UnitUtil;

public class CustomBottomAppBar extends com.google.android.material.bottomappbar.BottomAppBar {

	private final static boolean DEBUG = false;
	private final static String TAG = "CustomBottomAppBar";

	public static final int MENU_START = 0, MENU_END = 1;

	private static final int ENTER_ANIMATION_DURATION = 225;
	private static final int EXIT_ANIMATION_DURATION = 175;
	private static final int INVISIBLE = 0, VISIBLE = 1;
	private static final boolean USE_ACCURATE_CRADLE_ANIMATION = true;
	private static final int ICON_ANIM_DURATION = 200;
	private static final double ICON_ANIM_DELAY_FACTOR = 0.7;

	private ViewPropertyAnimator currentAnimator;
	private ValueAnimator valueAnimatorNavigationIcon;
	private AnimatorSet animatorSetNavigationIcon;

	private Runnable runnableOnShow;
	private Runnable runnableOnHide;

	private ArrayList<Handler> handlers = new ArrayList<>();

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
		animateTo(
				0,
				ENTER_ANIMATION_DURATION,
				AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR
		);
		if(isFabVisibleOrWillBeShown()) setCradleVisibility(true);
		if(runnableOnShow != null) runnableOnShow.run();
	}

	/**
	 * Animatedly hides the bottomAppBar.
	 */
	public void hide() {
		MarginLayoutParams params = (MarginLayoutParams) getLayoutParams();
		animateTo(
				getMeasuredHeight() + params.bottomMargin + UnitUtil.getDp(
						getContext(), 10
				), EXIT_ANIMATION_DURATION,
				AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR
		);
		if(isFabVisibleOrWillBeShown()) setCradleVisibility(false);
		if(runnableOnHide != null) runnableOnHide.run();
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
		if(menu != null && menu.size() > 0) {
			MenuItem item;
			for(int i = 0; i < menu.size(); i++) {
				item = menu.getItem(i);
				if(item.getIcon() != null) {
					item.getIcon().setAlpha((visible) ? 255 : 0);
				} else if(item.getActionView() != null) {
					item.getActionView().setAlpha((visible) ? 1 : 0);
				}
			}
		}
	}

	/**
	 * Animatedly changes the menu
	 */
	public void changeMenu(@MenuRes int menuNew, int position, boolean animated) {
		if(animated) {
			for(int i = 0; i < this.getMenu().size(); i++) {
				animateMenuItem(getMenu().getItem(i), INVISIBLE);
			}
			new Handler().postDelayed(() -> {
				replaceMenu(menuNew);
				setMenuVisibility(false);
				switch (position) {
					case MENU_START:
						animateMenu(0);
						break;
					case MENU_END:
						animateMenu(getMenu().size() - 1);
						break;
					default:
						if(DEBUG) Log.e(TAG, "changeMenu: wrong argument: " + position);
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
		changeMenu(menuNew, position, animated);
		new Handler().postDelayed(() -> {
			if(onChanged != null) onChanged.run();
		}, ICON_ANIM_DURATION + 50 + // wait for menu being fully changed
				(long) (getMenu().size() * ICON_ANIM_DELAY_FACTOR * ICON_ANIM_DURATION));
	}

	/**
	 * Animatedly shows the navigation icon
	 * @param navigationResId necessary because nav icon has to be null for being gone
	 */
	public void showNavigationIcon(@DrawableRes int navigationResId) {
		if(getNavigationIcon() == null) {
			new Handler().postDelayed(() -> {
				Drawable navigationIcon = ContextCompat.getDrawable(getContext(), navigationResId);
				assert navigationIcon != null;
				setNavigationIcon(navigationIcon);
				valueAnimatorNavigationIcon = ValueAnimator
						.ofInt(navigationIcon.getAlpha(), 255)
						.setDuration(ICON_ANIM_DURATION);
				valueAnimatorNavigationIcon.removeAllUpdateListeners();
				valueAnimatorNavigationIcon.addUpdateListener(
						animation -> navigationIcon.setAlpha((int) (animation.getAnimatedValue()))
				);
				animatorSetNavigationIcon = new AnimatorSet();
				animatorSetNavigationIcon.play(valueAnimatorNavigationIcon);
				animatorSetNavigationIcon.start();
			}, ICON_ANIM_DURATION);
		}
	}

	/**
	 * Animatedly hides the navigation icon
	 */
	public void hideNavigationIcon() {
		if(getNavigationIcon() != null) {
			Drawable navigationIcon = getNavigationIcon();
			valueAnimatorNavigationIcon = ValueAnimator
					.ofInt(navigationIcon.getAlpha(), 0)
					.setDuration(ICON_ANIM_DURATION);
			valueAnimatorNavigationIcon.removeAllUpdateListeners();
			valueAnimatorNavigationIcon.addUpdateListener(
					animation -> navigationIcon.setAlpha((int) (animation.getAnimatedValue()))
			);
			animatorSetNavigationIcon = new AnimatorSet();
			animatorSetNavigationIcon.play(valueAnimatorNavigationIcon);
			animatorSetNavigationIcon.start();
			new Handler().postDelayed(() -> setNavigationIcon(null), ICON_ANIM_DURATION + 5);
		}
	}

	/**
	 * Returns if the navigation icon is visible
	 */
	public boolean isNavigationIconVisible() {
		return getNavigationIcon() != null;
	}

	private void setCradleVisibility(boolean visible) {
		if(USE_ACCURATE_CRADLE_ANIMATION) {
			ValueAnimator valueAnimator = ValueAnimator.ofFloat(
					getCradleVerticalOffset(),
					(visible) ? 0 : 120
			);
			valueAnimator.addUpdateListener(
					animation -> setCradleVerticalOffset((Float) animation.getAnimatedValue())
			);
			valueAnimator.setInterpolator(
					(visible) ? new LinearOutSlowInInterpolator() : new FastOutLinearInInterpolator()
			);
			valueAnimator.setStartDelay((visible) ? 25 : 40); // ENTER | EXIT
			valueAnimator.setDuration((visible) ? 100 : 70).start(); // ENTER | EXIT
		} else {
			final int duration = (visible) ? 100 : 100; // ENTER | EXIT
			final int delay = (visible) ? 0 : 50; // ENTER | EXIT
			final int fps = 50;
			final double frameDuration = 1000f / fps;
			final int frames = (int) Math.round(duration / frameDuration);

			new Handler().postDelayed(() -> {
				final float currentOffset = getCradleVerticalOffset();
				final float targetOffset = (visible) ? 0 : 120; // ENTER | EXIT
				final float distance = currentOffset - targetOffset;
				final float step = (distance * -1) / frames;

				for(int i = 0; i < handlers.size(); i++) {
					handlers.get(i).removeCallbacksAndMessages(null);
				}
				handlers.clear();
				for(int i = 0; i <= frames; i++) {
					int finalI = i;
					handlers.add(i, new Handler());
					handlers.get(i).postDelayed(
							() -> setCradleVerticalOffset(currentOffset + finalI * step),
							i * Math.round(frameDuration)
					);
				}
			}, delay);
		}
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

	private void animateMenu(int indexStart) {
		int delayIndex = 0;
		for(
				int i = indexStart;
				(indexStart == 0) ? i < getMenu().size() : i >= 0;
				i = (indexStart == 0) ? i + 1 : i - 1
		) {
			int finalI = i;
			new Handler().postDelayed(
					() -> animateMenuItem(getMenu().getItem(finalI), VISIBLE),
					(long) (delayIndex * ICON_ANIM_DELAY_FACTOR * ICON_ANIM_DURATION)
			);
			delayIndex++;
		}
	}

	private void animateMenuItem(MenuItem item, int visibility) {
		if(item.getIcon() != null) {
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
					if(DEBUG) Log.e(
							TAG,
							"animateMenuItem(MenuItem): wrong argument: " + visibility
					);
					return;
			}
			ValueAnimator alphaAnimator = ValueAnimator.ofInt(icon.getAlpha(), targetAlpha);
			alphaAnimator.addUpdateListener(
					animation -> icon.setAlpha((int) (animation.getAnimatedValue()))
			);
			alphaAnimator.setDuration(ICON_ANIM_DURATION).start();
		}
	}

	@Override
	public void setHideOnScroll(boolean hide) {}

	@Override
	public boolean getHideOnScroll() {
		return false;
	}

	@Nullable
	private View findDependentView() {
		if (!(getParent() instanceof CoordinatorLayout)) return null;
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