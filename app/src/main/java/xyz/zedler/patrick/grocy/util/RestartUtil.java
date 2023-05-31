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

package xyz.zedler.patrick.grocy.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;

public class RestartUtil {

  public static void restartApp(Context context) {
    Intent intent = new Intent(context, MainActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    context.startActivity(intent);
    if (context instanceof Activity) {
      ((Activity) context).finish();
    }
    Runtime.getRuntime().exit(0);
  }

  public static void restartToApply(MainActivity activity, long delay, @NonNull Bundle bundle) {
    new Handler(Looper.getMainLooper()).postDelayed(() -> {
      activity.saveInstanceState(bundle);
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        activity.finish();
      }
      Intent intent = new Intent(activity, MainActivity.class);
      intent.putExtra(ARGUMENT.INSTANCE_STATE, bundle);
      activity.startActivity(intent);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        activity.finish();
      }
      activity.overridePendingTransition(R.anim.fade_in_restart, R.anim.fade_out_restart);
    }, delay);
  }
}
