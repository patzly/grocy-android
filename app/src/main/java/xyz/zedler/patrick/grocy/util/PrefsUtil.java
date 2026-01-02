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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;

public class PrefsUtil {
  private final SharedPreferences sharedPreferences;
  private final Fragment fragment;
  private final ActivityResultLauncher<Intent> exportLauncher;
  private final ActivityResultLauncher<Intent> importLauncher;

  public PrefsUtil(MainActivity activity, Fragment fragment) {
    this.fragment = fragment;
    this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(fragment.requireContext());
    this.exportLauncher = fragment.registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
          if (result.getData() != null) {
            Uri uri = result.getData().getData();
            if (uri != null) {
              exportToFile(
                  uri,
                  () -> activity.showSnackbar(R.string.msg_settings_backup_success, false),
                  () -> activity.showSnackbar(R.string.error_settings_backup, false)
              );
            }
          }
        }
    );
    this.importLauncher = fragment.registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
          if (result.getData() != null) {
            Uri uri = result.getData().getData();
            if (uri != null) {
              importFromFile(
                  uri,
                  () -> {
                    activity.showSnackbar(R.string.msg_settings_restore_success, false);
                    new Handler().postDelayed(
                        () -> RestartUtil.restartApp(fragment.requireContext()), 2000
                    );
                  },
                  () -> activity.showSnackbar(R.string.error_settings_restore, false)
              );
            }
          }
        }
    );
  }

  public void exportPrefs() {
    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("text/plain");
    intent.putExtra(Intent.EXTRA_TITLE, "prefs.txt");
    exportLauncher.launch(intent);
  }

  public void importPrefs() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("text/*");
    importLauncher.launch(intent);
  }

  private void exportToFile(Uri uri, Runnable onSuccess, Runnable onError) {
    try (OutputStream stream = fragment.requireActivity().getContentResolver().openOutputStream(uri);
        OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
      Map<String, ?> allPrefs = sharedPreferences.getAll();
      for (String key : allPrefs.keySet()) {
        Object value = allPrefs.get(key);
        if (value == null) continue;
        String valueType = value.getClass().getSimpleName();
        writer.write(key + "=" + value + ";" + valueType + "\n");
      }
      onSuccess.run();
    } catch (IOException e) {
      Log.e("PrefsUtil", "Error exporting prefs", e);
      onError.run();
    }
  }

  private void importFromFile(Uri uri, Runnable onSuccess, Runnable onError) {
    try (InputStream inputStream = fragment.requireActivity().getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

      // Clear all existing preferences
      SharedPreferences.Editor editor = sharedPreferences.edit();
      editor.clear();
      editor.apply();

      editor = sharedPreferences.edit();
      String line;
      while ((line = reader.readLine()) != null) {
        String[] keyValue = line.split("=", 2);
        if (keyValue.length == 2) {
          String[] valueAndType = keyValue[1].split(";", 2);
          if (valueAndType.length == 2) {
            String value = valueAndType[0];
            String type = valueAndType[1];
            switch (type) {
              case "Integer":
                editor.putInt(keyValue[0], Integer.parseInt(value));
                break;
              case "Long":
                editor.putLong(keyValue[0], Long.parseLong(value));
                break;
              case "Float":
                editor.putFloat(keyValue[0], Float.parseFloat(value));
                break;
              case "Boolean":
                editor.putBoolean(keyValue[0], Boolean.parseBoolean(value));
                break;
              case "String":
                editor.putString(keyValue[0], value);
                break;
              case "HashSet":
                Set<String> stringSet = new HashSet<>(Arrays.asList(value.split(",")));
                editor.putStringSet(keyValue[0], stringSet);
                break;
            }
          }
        }
      }
      editor.apply();
      onSuccess.run();
    } catch (IOException e) {
      Log.e("PrefsUtil", "Error importing prefs", e);
      onError.run();
    }
  }

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
    editPrefs.remove(PREF.DB_LAST_TIME_USERFIELDS);
    editPrefs.remove(PREF.DB_LAST_TIME_MEAL_PLAN_ENTRIES);
    editPrefs.remove(PREF.DB_LAST_TIME_MEAL_PLAN_SECTIONS);
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
