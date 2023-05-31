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

public class NavUtil {

  private final String TAG;
  private final MainActivity activity;
  private NavController navController;
  private final FragmentManager fragmentManager;
  private final SharedPreferences sharedPrefs;

  public NavUtil(
      MainActivity mainActivity,
      OnDestinationChangedListener destinationListener,
      SharedPreferences sharedPrefs,
      String tag
  ) {
    this.activity = mainActivity;
    this.fragmentManager = mainActivity.getSupportFragmentManager();
    NavHostFragment navHostFragment = (NavHostFragment) fragmentManager
        .findFragmentById(R.id.fragment_main_nav_host);
    assert navHostFragment != null;
    navController = navHostFragment.getNavController();
    navController.addOnDestinationChangedListener(destinationListener);
    this.sharedPrefs = sharedPrefs;
    this.TAG = tag;
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

  public NavOptions.Builder getNavOptionsBuilderFragmentFadeOrSlide(boolean slideVertically) {
    if (UiUtil.areAnimationsEnabled(activity)) {
      boolean useSliding = sharedPrefs.getBoolean(
          Constants.SETTINGS.APPEARANCE.USE_SLIDING,
          Constants.SETTINGS_DEFAULT.APPEARANCE.USE_SLIDING
      );
      if (useSliding) {
        if (slideVertically) {
          return new NavOptions.Builder()
              .setEnterAnim(R.anim.slide_in_up)
              .setPopExitAnim(R.anim.slide_out_down)
              .setExitAnim(R.anim.slide_no);
        } else {
          return new NavOptions.Builder()
              .setEnterAnim(R.anim.slide_from_end)
              .setPopExitAnim(R.anim.slide_to_end)
              .setPopEnterAnim(R.anim.slide_from_start)
              .setExitAnim(R.anim.slide_to_start);
        }
      } else {
        return new NavOptions.Builder()
            .setEnterAnim(R.anim.enter_end_fade)
            .setExitAnim(R.anim.exit_start_fade)
            .setPopEnterAnim(R.anim.enter_start_fade)
            .setPopExitAnim(R.anim.exit_end_fade);
      }
    } else {
      return new NavOptions.Builder()
          .setEnterAnim(R.anim.fade_in_a11y)
          .setExitAnim(R.anim.fade_out_a11y)
          .setPopEnterAnim(R.anim.fade_in_a11y)
          .setPopExitAnim(R.anim.fade_out_a11y);
    }
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
    if (navController == null) {
      NavHostFragment navHostFragment = (NavHostFragment) fragmentManager.findFragmentById(
          R.id.fragment_main_nav_host
      );
      assert navHostFragment != null;
      navController = navHostFragment.getNavController();
    }
    navController.navigateUp();
    activity.binding.bottomAppBar.performShow();
    activity.hideKeyboard();
  }

  public void navigateFragment(@IdRes int destination) {
    navigateFragment(destination, (Bundle) null);
  }

  public void navigateFragment(@IdRes int destination, @Nullable Bundle arguments) {
    if (navController == null ) {
      Log.e(TAG, "navigateFragment: controller is null");
      return;
    }
    try {
      navController.navigate(
          destination, arguments, getNavOptionsBuilderFragmentFadeOrSlide(true).build()
      );
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigateFragment: ", e);
    }
  }

  public void navigateFragment(NavDirections directions) {
    if (navController == null || directions == null) {
      Log.e(TAG, "navigateFragment: controller or direction is null");
      return;
    }
    try {
      navController.navigate(
          directions, getNavOptionsBuilderFragmentFadeOrSlide(true).build()
      );
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigateFragment: " + directions, e);
    }
  }

  public void navigateFragment(@IdRes int destination, @NonNull NavOptions navOptions) {
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

  public void navigateDeepLink(@NonNull Uri uri, boolean slideVertically) {
    if (navController == null ) {
      Log.e(TAG, "navigateDeepLink: controller is null");
      return;
    }
    try {
      navController.navigate(uri, getNavOptionsBuilderFragmentFadeOrSlide(slideVertically).build());
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
    navigateDeepLink(Uri.parse(uri), true);
  }

  public void navigateDeepLink(@StringRes int uri) {
    navigateDeepLink(Uri.parse(activity.getString(uri)), true);
  }

  public void navigateDeepLink(@StringRes int uri, @NonNull Bundle args) {
    navigateDeepLink(getUriWithArgs(activity.getString(uri), args), true);
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
