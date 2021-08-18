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

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

public class AppBarBehaviorNew {

  private final static String TAG = AppBarBehaviorNew.class.getSimpleName();

  private static final int ANIM_DURATION = 300;
  private static final String SAVED_STATE_KEY = "app_bar_layout_is_primary";

  private final View viewPrimary;
  private final View viewSecondary;
  private boolean isPrimary = true;
  private final boolean debug;

  public AppBarBehaviorNew(Activity activity, View primary, View secondary, Bundle savedState) {
    debug = PrefsUtil.isDebuggingEnabled(activity);

    if (savedState == null) {
      isPrimary = true;
    } else if (savedState.containsKey(SAVED_STATE_KEY)) {
      isPrimary = savedState.getBoolean(SAVED_STATE_KEY);
    }

    viewPrimary = primary;
    viewPrimary.setAlpha(1);
    viewPrimary.setVisibility(isPrimary ? View.VISIBLE : View.GONE);
    viewSecondary = secondary;
    viewSecondary.setAlpha(1);
    viewSecondary.setVisibility(isPrimary ? View.GONE : View.VISIBLE);
  }

  public void saveInstanceState(@NonNull Bundle outState) {
    outState.putBoolean(SAVED_STATE_KEY, isPrimary);
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
