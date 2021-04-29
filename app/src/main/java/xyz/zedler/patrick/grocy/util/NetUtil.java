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

package xyz.zedler.patrick.grocy.util;

import android.app.Activity;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import androidx.browser.customtabs.CustomTabsIntent;
import xyz.zedler.patrick.grocy.R;

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

  public static boolean openURL(Context context, String url) {
    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
    builder.setStartAnimations(context, R.anim.slide_in_up, R.anim.slide_no);
    builder.setExitAnimations(context, R.anim.slide_no, R.anim.fade_out);
    CustomTabsIntent customTabsIntent = builder.build();
    try {
      customTabsIntent.launchUrl(context, Uri.parse(url));
      return true;
    } catch (ActivityNotFoundException ex) {
      return false;
    }
  }
}
