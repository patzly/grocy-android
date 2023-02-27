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
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import androidx.browser.customtabs.CustomTabsIntent;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;

public class NetUtil {

  private ConnectivityManager cm;

  public NetUtil(Activity activity) {
    if (activity == null) {
      return;
    }
    cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
  }

  public NetUtil(Application application) {
    if (application == null) {
      return;
    }
    cm = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
  }

  public boolean isOnline() {
    if (cm == null) {
      return false;
    }
    NetworkInfo networkInfo = cm.getActiveNetworkInfo();
    return networkInfo != null && networkInfo.isConnectedOrConnecting();
  }

  public static boolean openURL(MainActivity activity, String url) {
    boolean useSliding = activity.getSharedPrefs().getBoolean(
        Constants.SETTINGS.APPEARANCE.USE_SLIDING,
        Constants.SETTINGS_DEFAULT.APPEARANCE.USE_SLIDING
    );
    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
    if (useSliding) {
      builder.setStartAnimations(activity, R.anim.slide_from_end, R.anim.slide_to_start);
      builder.setExitAnimations(activity, R.anim.slide_from_start, R.anim.slide_to_end);
    } else {
      builder.setStartAnimations(activity, R.anim.enter_end_fade, R.anim.exit_start_fade);
      builder.setExitAnimations(activity, R.anim.enter_start_fade, R.anim.exit_end_fade);
    }
    CustomTabsIntent customTabsIntent = builder.build();
    try {
      customTabsIntent.launchUrl(activity, Uri.parse(url));
      return true;
    } catch (ActivityNotFoundException ex) {
      return false;
    }
  }
}
