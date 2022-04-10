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
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.util;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import xyz.zedler.patrick.grocy.util.Constants.PREF;

public class VersionUtil {

  public final static String SERVER_3_2_0 = "3.2.0";
  public final static String SERVER_3_3_0 = "3.3.0";

  public static boolean isGrocyServerMin320(SharedPreferences prefs) {
    return isGrocyThisVersionOrHigher(prefs, SERVER_3_2_0);
  }

  public static boolean isGrocyServerMin330(SharedPreferences prefs) {
    return isGrocyThisVersionOrHigher(prefs, SERVER_3_3_0);
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
