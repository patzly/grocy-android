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

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;

public class PrefsUtil {

  public static void migratePrefs(SharedPreferences sharedPrefs) {
    // Due soon notification is now stock notification
    migratePref(
        sharedPrefs,
        "notification_due_soon_enable",
        "notification_stock_enable"
    );
    migratePref(
        sharedPrefs,
        "notification_due_soon_time",
        "notification_stock_time"
    );
  }

  private static void migratePref(
      SharedPreferences sharedPrefs,
      String oldKey,
      String newKey
  ) {
    if (sharedPrefs.contains(oldKey)) {
      Object value = sharedPrefs.getAll().get(oldKey);
      SharedPreferences.Editor editor = sharedPrefs.edit();
      if (value instanceof Boolean) {
        editor.putBoolean(newKey, (Boolean) value);
      } else if (value instanceof Float) {
        editor.putFloat(newKey, (Float) value);
      } else if (value instanceof Integer) {
        editor.putInt(newKey, (Integer) value);
      } else if (value instanceof Long) {
        editor.putLong(newKey, (Long) value);
      } else if (value instanceof String) {
        editor.putString(newKey, (String) value);
      }
      editor.remove(oldKey);
      editor.apply();
    }
  }

  public static void clearCachingRelatedSharedPreferences(SharedPreferences sharedPrefs) {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.remove(PREF.DB_LAST_TIME_STOCK_ITEMS);
    editPrefs.remove(PREF.DB_LAST_TIME_STOCK_ENTRIES);
    editPrefs.remove(PREF.DB_LAST_TIME_STORES);
    editPrefs.remove(PREF.DB_LAST_TIME_LOCATIONS);
    editPrefs.remove(PREF.DB_LAST_TIME_STOCK_LOCATIONS);
    editPrefs.remove(PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS);
    editPrefs.remove(PREF.DB_LAST_TIME_SHOPPING_LISTS);
    editPrefs.remove(PREF.DB_LAST_TIME_PRODUCT_GROUPS);
    editPrefs.remove(PREF.DB_LAST_TIME_QUANTITY_UNITS);
    editPrefs.remove(PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS);
    editPrefs.remove(PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS_RESOLVED);
    editPrefs.remove(PREF.DB_LAST_TIME_PRODUCTS);
    editPrefs.remove(PREF.DB_LAST_TIME_PRODUCTS_LAST_PURCHASED);
    editPrefs.remove(PREF.DB_LAST_TIME_PRODUCTS_AVERAGE_PRICE);
    editPrefs.remove(PREF.DB_LAST_TIME_PRODUCT_BARCODES);
    editPrefs.remove(PREF.DB_LAST_TIME_VOLATILE);
    editPrefs.remove(PREF.DB_LAST_TIME_VOLATILE_MISSING);
    editPrefs.remove(PREF.DB_LAST_TIME_TASKS);
    editPrefs.remove(PREF.DB_LAST_TIME_TASK_CATEGORIES);
    editPrefs.remove(PREF.DB_LAST_TIME_CHORES);
    editPrefs.remove(PREF.DB_LAST_TIME_CHORE_ENTRIES);
    editPrefs.remove(PREF.DB_LAST_TIME_USERS);
    editPrefs.remove(PREF.DB_LAST_TIME_RECIPES);
    editPrefs.remove(PREF.DB_LAST_TIME_RECIPE_FULFILLMENTS);
    editPrefs.remove(PREF.DB_LAST_TIME_RECIPE_POSITIONS);
    editPrefs.remove(PREF.DB_LAST_TIME_RECIPE_POSITIONS_RESOLVED);
    editPrefs.remove(PREF.DB_LAST_TIME_RECIPE_NESTINGS);
    editPrefs.apply();
  }

  public static void clearServerRelatedSharedPreferences(SharedPreferences sharedPrefs) {
    clearCachingRelatedSharedPreferences(sharedPrefs);

    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.remove(PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY);
    editPrefs.remove(PREF.HOME_ASSISTANT_INGRESS_SESSION_KEY_TIME);
    editPrefs.remove(PREF.SERVER_URL);
    editPrefs.remove(PREF.HOME_ASSISTANT_SERVER_URL);
    editPrefs.remove(PREF.HOME_ASSISTANT_LONG_LIVED_TOKEN);
    editPrefs.remove(PREF.API_KEY);
    editPrefs.remove(PREF.SHOPPING_LIST_LAST_ID);
    editPrefs.remove(PREF.GROCY_VERSION);
    editPrefs.remove(PREF.CURRENT_USER_ID);
    editPrefs.apply();
  }

  public static boolean isDebuggingEnabled(SharedPreferences sharedPrefs) {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.DEBUGGING.ENABLE_DEBUGGING,
        Constants.SETTINGS_DEFAULT.DEBUGGING.ENABLE_DEBUGGING
    );
  }

  public static boolean isDebuggingEnabled(Context context) {
    return isDebuggingEnabled(PreferenceManager.getDefaultSharedPreferences(context));
  }

  public static boolean isServerUrlEmpty(SharedPreferences sharedPrefs) {
    String server = sharedPrefs.getString(Constants.PREF.SERVER_URL, null);
    return server == null || server.isEmpty();
  }

  public static int getModeNight(SharedPreferences sharedPrefs) {
    return sharedPrefs.getInt(
        SETTINGS.APPEARANCE.DARK_MODE, SETTINGS_DEFAULT.APPEARANCE.DARK_MODE
    );
  }

  public static boolean areHapticsEnabled(SharedPreferences sharedPrefs, Context context) {
    return sharedPrefs.getBoolean(
        Constants.SETTINGS.BEHAVIOR.HAPTIC, HapticUtil.areSystemHapticsTurnedOn(context)
    );
  }
}
