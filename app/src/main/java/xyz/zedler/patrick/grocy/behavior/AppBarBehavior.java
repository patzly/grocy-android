package xyz.zedler.patrick.grocy.behavior;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import androidx.annotation.IdRes;

public class AppBarBehavior {

	private final static String TAG = "AppBarBehavior";
	private final static boolean DEBUG = false;

	private Activity activity;
	private View viewVisible;

	public AppBarBehavior(Activity activity, @IdRes int layoutDefault) {
		this.activity = activity;
		viewVisible = activity.findViewById(layoutDefault);
	}

	public void replaceLayout(@IdRes int layoutNew, boolean animated) {
		View viewNew = activity.findViewById(layoutNew);
		if(viewNew.getVisibility() == View.GONE) {
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
		} else {
			if(DEBUG) Log.i(TAG, "replaceLayout: layout already visible");
		}
	}
}
