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
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.util;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavController.OnDestinationChangedListener;
import androidx.navigation.NavDirections;
import androidx.navigation.NavGraph;
import androidx.navigation.NavInflater;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigator;
import androidx.navigation.fragment.NavHostFragment;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.fragment.BaseFragment;

public class NavUtil {

  private final String TAG;
  private final MainActivity activity;
  private final NavController navController;
  private final NavHostFragment navHostFragment;
  private final SharedPreferences sharedPrefs;

  public NavUtil(
      MainActivity mainActivity,
      OnDestinationChangedListener destinationListener,
      SharedPreferences sharedPrefs,
      String tag
  ) {
    this.activity = mainActivity;
    FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
    navHostFragment = (NavHostFragment) fragmentManager.findFragmentById(
        R.id.fragment_main_nav_host
    );
    assert navHostFragment != null;
    navController = navHostFragment.getNavController();
    navController.addOnDestinationChangedListener(destinationListener);
    this.sharedPrefs = sharedPrefs;
    this.TAG = tag;
  }

  @NonNull
  public BaseFragment getCurrentFragment() {
    return (BaseFragment) navHostFragment.getChildFragmentManager().getFragments().get(0);
  }

  public void updateStartDestination() {
    NavInflater navInflater = navController.getNavInflater();
    NavGraph graph = navInflater.inflate(R.navigation.navigation_main);
    boolean introShown = sharedPrefs.getBoolean(Constants.PREF.INTRO_SHOWN, false);
    if (!introShown) {
      graph.setStartDestination(R.id.onboardingFragment);
    } else if (PrefsUtil.isServerUrlEmpty(sharedPrefs)) {
      graph.setStartDestination(R.id.navigation_login);
    } else {
      graph.setStartDestination(R.id.overviewStartFragment);
    }
    navController.setGraph(graph);
  }

  public void navigate(NavDirections directions) {
    if (navController == null || directions == null) {
      Log.e(TAG, "navigate: controller or direction is null");
      return;
    }
    try {
      navController.navigate(directions);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigate: " + directions, e);
    }
  }

  public void navigate(@IdRes int destination) {
    if (navController == null) {
      Log.e(TAG, "navigate: controller or direction is null");
      return;
    }
    try {
      navController.navigate(destination);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigate: ", e);
    }
  }

  public void navigate(NavDirections directions, @NonNull NavOptions navOptions) {
    if (navController == null || directions == null) {
      Log.e(TAG, "navigate: controller or direction is null");
      return;
    }
    try {
      navController.navigate(directions, navOptions);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigate: " + directions, e);
    }
  }

  public void navigate(NavDirections directions, @NonNull Navigator.Extras navigatorExtras) {
    if (navController == null || directions == null) {
      Log.e(TAG, "navigate: controller or direction is null");
      return;
    }
    try {
      navController.navigate(directions, navigatorExtras);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigate: " + directions, e);
    }
  }

  public void navigateUp() {
    navController.navigateUp();
    activity.binding.bottomAppBar.performShow();
    activity.hideKeyboard();
  }

  public void navigate(@IdRes int destination, @Nullable Bundle arguments) {
    if (navController == null ) {
      Log.e(TAG, "navigateFragment: controller is null");
      return;
    }
    try {
      navController.navigate(destination, arguments);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigateFragment: ", e);
    }
  }

  public void navigate(@IdRes int destination, @NonNull NavOptions navOptions) {
    if (navController == null ) {
      Log.e(TAG, "navigateFragment: controller is null");
      return;
    }
    try {
      navController.navigate(destination, null, navOptions);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigateFragment: ", e);
    }
  }

  public void navigateDeepLink(@NonNull Uri uri) {
    if (navController == null ) {
      Log.e(TAG, "navigateDeepLink: controller is null");
      return;
    }
    try {
      navController.navigate(uri);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigateDeepLink: ", e);
    }
  }

  public void navigateDeepLink(@NonNull Uri uri, @NonNull NavOptions navOptions) {
    if (navController == null ) {
      Log.e(TAG, "navigateDeepLink: controller is null");
      return;
    }
    try {
      navController.navigate(uri, navOptions);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigateDeepLink: ", e);
    }
  }

  public void navigateDeepLink(String uri) {
    navigateDeepLink(Uri.parse(uri));
  }

  public void navigateDeepLink(@StringRes int uri) {
    navigateDeepLink(Uri.parse(activity.getString(uri)));
  }

  public void navigateDeepLink(@StringRes int uri, @NonNull Bundle args) {
    navigateDeepLink(getUriWithArgs(activity.getString(uri), args));
  }

  public static Uri getUriWithArgs(@NonNull String uri, @NonNull Bundle argsBundle) {
    String[] parts = uri.split("\\?");
    if (parts.length == 1) {
      return Uri.parse(uri);
    }
    String linkPart = parts[0];
    String argsPart = parts[parts.length - 1];
    String[] pairs = argsPart.split("&");
    StringBuilder finalDeepLink = new StringBuilder(linkPart + "?");
    for (int i = 0; i <= pairs.length - 1; i++) {
      String pair = pairs[i];
      String key = pair.split("=")[0];
      Object valueBundle = argsBundle.get(key);
      if (valueBundle == null) {
        continue;
      }
      try {
        String encoded;
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
          encoded = URLEncoder.encode(valueBundle.toString(), StandardCharsets.UTF_8);
        } else {
          encoded = URLEncoder.encode(valueBundle.toString(), "UTF-8");
        }
        encoded = encoded.replace("+", "%20");
        finalDeepLink.append(key).append("=").append(encoded);
      } catch (Throwable ignore) {
      }
      if (i != pairs.length - 1) {
        finalDeepLink.append("&");
      }
    }
    return Uri.parse(finalDeepLink.toString());
  }
}
