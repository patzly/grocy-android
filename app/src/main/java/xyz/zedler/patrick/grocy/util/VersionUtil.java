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
import android.os.Bundle;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Arrays;
import xyz.zedler.patrick.grocy.BuildConfig;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.NavigationMainDirections;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.CompatibilityBottomSheet;

public class VersionUtil {

  public final static String SERVER_3_2_0 = "3.2.0";
  public final static String SERVER_3_3_0 = "3.3.0";
  public final static String SERVER_3_3_1 = "3.3.1";
  public final static String SERVER_4_0_0 = "4.0.0";

  public static boolean isGrocyServerMin320(SharedPreferences prefs) {
    return isGrocyThisVersionOrHigher(prefs, SERVER_3_2_0);
  }

  public static boolean isGrocyServerMin330(SharedPreferences prefs) {
    return isGrocyThisVersionOrHigher(prefs, SERVER_3_3_0);
  }

  public static boolean isGrocyServerMin331(SharedPreferences prefs) {
    return isGrocyThisVersionOrHigher(prefs, SERVER_3_3_1);
  }

  public static boolean isGrocyServerMin400(SharedPreferences prefs) {
    return isGrocyThisVersionOrHigher(prefs, SERVER_4_0_0);
  }

  public static boolean isGrocyThisVersionOrHigher(SharedPreferences prefs, @NonNull String version) {
    String current = prefs.getString(PREF.GROCY_VERSION, null);
    if (current == null) return true;
    try {
      Version a = new Version(current);
      Version b = new Version(version);
      return a.compareTo(b) >= 0;
    } catch (IllegalArgumentException e) {
      return true;
    }
  }

  public static void showCompatibilityBottomSheetIfNecessary(
      MainActivity mainActivity,
      SharedPreferences sharedPrefs
  ) {
    String version = sharedPrefs.getString(Constants.PREF.GROCY_VERSION, null);
    if (version == null || version.isEmpty()) {
      return;
    }
    ArrayList<String> supportedVersions = new ArrayList<>(
        Arrays.asList(mainActivity.getResources().getStringArray(R.array.compatible_grocy_versions))
    );
    if (supportedVersions.contains(version)) {
      return;
    }

    // If user already ignored warning, do not display again
    String ignoredVersion = sharedPrefs.getString(
        Constants.PREF.VERSION_COMPATIBILITY_IGNORED, null
    );
    if (ignoredVersion != null && ignoredVersion.equals(version)) {
      return;
    }

    Bundle bundle = new Bundle();
    bundle.putString(Constants.ARGUMENT.VERSION, version);
    bundle.putStringArrayList(Constants.ARGUMENT.SUPPORTED_VERSIONS, supportedVersions);
    mainActivity.showBottomSheet(new CompatibilityBottomSheet(), bundle);
  }

  public static boolean isAppUpdated(SharedPreferences sharedPrefs) {
    int versionNew = BuildConfig.VERSION_CODE;
    int versionOld = sharedPrefs.getInt(PREF.LAST_VERSION, 0);
    if (versionOld == 0) {
      sharedPrefs.edit().putInt(PREF.LAST_VERSION, versionNew).apply();
      return false;
    } else if (versionOld != versionNew) {
      sharedPrefs.edit().putInt(PREF.LAST_VERSION, versionNew).apply();
      return true;
    }
    return false;
  }

  public static boolean isDatabaseUpdated(SharedPreferences sharedPrefs, int currentVersion) {
    int versionOld = sharedPrefs.getInt(PREF.LAST_VERSION_DATABASE, 0);
    if (versionOld == 0) {
      sharedPrefs.edit().putInt(PREF.LAST_VERSION_DATABASE, currentVersion).apply();
      return false;
    } else if (versionOld != currentVersion) {
      sharedPrefs.edit().putInt(PREF.LAST_VERSION_DATABASE, currentVersion).apply();
      return true;
    }
    return false;
  }

  public static void showChangelogBottomSheet(MainActivity mainActivity) {
    xyz.zedler.patrick.grocy.NavigationMainDirections.ActionGlobalTextDialog action
        = NavigationMainDirections.actionGlobalTextDialog();
    action.setTitle(R.string.info_changelog);
    action.setFile(R.raw.changelog);
    action.setHighlights(new String[]{"New:", "Improved:", "Fixed:"});
    mainActivity.navUtil.navigate(action);
  }

  public static void clearCachingInfoIfAppOrDatabaseUpdated(SharedPreferences sharedPrefs) {

  }

  private static class Version implements Comparable<Version> {

    private final String version;

    public final String get() {
      return this.version;
    }

    public Version(String version) {
      if(version == null)
        throw new IllegalArgumentException("Version can not be null");
      if(!version.matches("[0-9]+(\\.[0-9]+)*"))
        throw new IllegalArgumentException("Invalid version format");
      this.version = version;
    }

    @Override public int compareTo(Version that) {
      if(that == null)
        return 1;
      String[] thisParts = this.get().split("\\.");
      String[] thatParts = that.get().split("\\.");
      int length = Math.max(thisParts.length, thatParts.length);
      for(int i = 0; i < length; i++) {
        int thisPart = i < thisParts.length ?
            Integer.parseInt(thisParts[i]) : 0;
        int thatPart = i < thatParts.length ?
            Integer.parseInt(thatParts[i]) : 0;
        if(thisPart < thatPart)
          return -1;
        if(thisPart > thatPart)
          return 1;
      }
      return 0;
    }

    @Override public boolean equals(Object that) {
      if(this == that)
        return true;
      if(that == null)
        return false;
      if(this.getClass() != that.getClass())
        return false;
      return this.compareTo((Version) that) == 0;
    }
  }
}
