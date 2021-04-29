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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.behavior;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import xyz.zedler.patrick.grocy.util.Constants;

public class AppBarBehavior {

  private final static String TAG = AppBarBehavior.class.getSimpleName();

  private static final int ANIM_DURATION = 300;

  private final Activity activity;
  private View viewPrimary, viewSecondary;
  private boolean isPrimary;
  private final boolean debug;

  public AppBarBehavior(Activity activity, @IdRes int primary, @IdRes int secondary) {
    this.activity = activity;

    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
    debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

    viewPrimary = activity.findViewById(primary);
    viewPrimary.setVisibility(View.VISIBLE);
    viewPrimary.setAlpha(1);

    viewSecondary = activity.findViewById(secondary);
    viewSecondary.setVisibility(View.GONE);

    isPrimary = true;
  }

  public void saveInstanceState(@NonNull Bundle outState) {
    if (viewPrimary != null) {
      outState.putInt("appBarBehavior_primary_view_id", viewPrimary.getId());
    }
    if (viewSecondary != null) {
      outState.putInt("appBarBehavior_secondary_view_id", viewSecondary.getId());
    }
    outState.putBoolean("appBarBehavior_is_primary", isPrimary);

    if (debug) {
      Log.i(TAG, "saved state: isPrimary = " + isPrimary);
    }
  }

  public void restoreInstanceState(@NonNull Bundle savedInstanceState) {
    View viewPrimary = activity.findViewById(
        savedInstanceState.getInt("appBarBehavior_primary_view_id")
    );
    View viewSecondary = activity.findViewById(
        savedInstanceState.getInt("appBarBehavior_secondary_view_id")
    );

    this.viewPrimary = viewPrimary;
    this.viewSecondary = viewSecondary;

    isPrimary = savedInstanceState.getBoolean(
        "appBarBehavior_is_primary",
        true
    );

    if (viewPrimary != null) {
      viewPrimary.setVisibility(isPrimary ? View.VISIBLE : View.GONE);
    }
    if (viewSecondary != null) {
      viewSecondary.setVisibility(isPrimary ? View.GONE : View.VISIBLE);
    }

    if (debug) {
      Log.i(TAG, "restored state: isPrimary = " + isPrimary);
    }
  }

  public void switchToPrimary() {
    if (isPrimary) {
      return;
    }
    isPrimary = true;
    viewSecondary.animate()
        .alpha(0)
        .setDuration(ANIM_DURATION / 2)
        .withEndAction(() -> {
          viewSecondary.setVisibility(View.GONE);
          viewPrimary.setVisibility(View.VISIBLE);
          viewPrimary.setAlpha(0);
          viewPrimary.animate().alpha(1).setDuration(ANIM_DURATION / 2).start();
        }).start();
    if (debug) {
      Log.i(TAG, "switch to primary layout");
    }
  }

  public void switchToSecondary() {
    if (!isPrimary) {
      return;
    }
    isPrimary = false;
    viewPrimary.animate()
        .alpha(0)
        .setDuration(ANIM_DURATION / 2)
        .withEndAction(() -> {
          viewPrimary.setVisibility(View.GONE);
          viewSecondary.setVisibility(View.VISIBLE);
          viewSecondary.setAlpha(0);
          viewSecondary.animate().alpha(1).setDuration(ANIM_DURATION / 2).start();
        }).start();
    if (debug) {
      Log.i(TAG, "switch to secondary layout");
    }
  }

  public boolean isPrimaryLayout() {
    return isPrimary;
  }
}
