package xyz.zedler.patrick.grocy.behavior;

import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.IdRes;

public class AppBarBehavior {

	private final static String TAG = "AppBarBehavior";
	private final static boolean DEBUG = false;

	private LinearLayout container;
	private View viewVisible;

	public AppBarBehavior(LinearLayout container, @IdRes int layoutDefault) {
		this.container = container;
		viewVisible = container.findViewById(layoutDefault);
	}

	public void replaceLayout(@IdRes int layoutNew, boolean animated) {
		View viewNew = container.findViewById(layoutNew);
		if(animated) {
			int duration = 300;
			viewVisible.animate()
					.alpha(0)
					.setDuration(duration)
					.withEndAction(() -> {
						viewVisible.setVisibility(View.GONE);
						viewNew.setVisibility(View.VISIBLE);
						viewNew.setAlpha(0);
						viewNew.animate().alpha(1).setDuration(duration).start();
						viewVisible = viewNew;
					}).start();
		} else {
			viewVisible.setVisibility(View.GONE);
			viewNew.setVisibility(View.VISIBLE);
			viewNew.setAlpha(1);
		}
		if(DEBUG) Log.i(TAG, "replaceLayout: animated = " + animated);
	}
}
