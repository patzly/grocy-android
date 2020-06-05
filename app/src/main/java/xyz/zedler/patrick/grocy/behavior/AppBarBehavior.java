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

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

public class AppBarBehavior {

	private final static String TAG = AppBarBehavior.class.getSimpleName();
	private final static boolean DEBUG = false;

	private Activity activity;
	private View viewVisible, viewInvisible;

	public AppBarBehavior(Activity activity, @IdRes int layoutDefault) {
		this.activity = activity;
		viewVisible = activity.findViewById(layoutDefault);
	}

	public void saveInstanceState(@NonNull Bundle outState) {
		if(viewVisible != null) {
			outState.putInt("appBarBehavior_visible_view_id", viewVisible.getId());
		}
		if(viewInvisible != null) {
			outState.putInt("appBarBehavior_invisible_view_id", viewInvisible.getId());
		}
	}

	public void restoreInstanceState(@NonNull Bundle savedInstanceState) {
		View viewVisible = activity.findViewById(
				savedInstanceState.getInt("appBarBehavior_visible_view_id")
		);
		View viewInvisible = activity.findViewById(
				savedInstanceState.getInt("appBarBehavior_invisible_view_id")
		);
		if(viewVisible != null) {
			viewVisible.setVisibility(View.VISIBLE);
			this.viewVisible = viewVisible;
		}
		if(viewInvisible != null) {
			viewInvisible.setVisibility(View.GONE);
			this.viewInvisible = viewInvisible;
		}
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
							viewInvisible = viewVisible;
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
