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

import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS_DEFAULT;

public class ConfigUtil {

  private final static String TAG = ConfigUtil.class.getSimpleName();

  public static void loadInfo(
      DownloadHelper dlHelper,
      GrocyApi api,
      SharedPreferences prefs,
      Runnable onSuccessAction,
      DownloadHelper.OnErrorListener onError
  ) {

    boolean debug = prefs.getBoolean(Constants.PREF.DEBUG, false);

    DownloadHelper.Queue queue = dlHelper.newQueue(() -> {
      if (onSuccessAction != null) {
        onSuccessAction.run();
      }
    }, volleyError -> {
      if (onError != null) {
        onError.onError(volleyError);
      }
    });

    queue.append(
        dlHelper.getStringData(
            api.getSystemConfig(),
            response -> storeSystemConfig(response, prefs, debug)
        ),
        dlHelper.getStringData(
            api.getUserSettings(),
            response -> storeUserSettings(response, prefs, debug)
        ),
        dlHelper.getStringData(
            api.getSystemInfo(),
            response -> storeSystemInfo(response, prefs, debug)
        )
    ).start();
  }

  private static void storeSystemConfig(String response, SharedPreferences prefs, boolean debug) {
    try {
      JSONObject jsonObject = new JSONObject(response);
      prefs.edit()
          .putString(
              Constants.PREF.CURRENCY,
              jsonObject.getString("CURRENCY")
          )
          .putBoolean(
              Constants.PREF.FEATURE_STOCK,
              jsonObject.getBoolean("FEATURE_FLAG_STOCK")
          )
          .putBoolean(
              Constants.PREF.FEATURE_SHOPPING_LIST,
              jsonObject.getBoolean("FEATURE_FLAG_SHOPPINGLIST")
          )
          .putBoolean(
              Constants.PREF.FEATURE_STOCK_PRICE_TRACKING,
              jsonObject.getBoolean(
                  "FEATURE_FLAG_STOCK_PRICE_TRACKING"
              )
          )
          .putBoolean(
              Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS,
              jsonObject.getBoolean(
                  "FEATURE_FLAG_SHOPPINGLIST_MULTIPLE_LISTS"
              )
          )
          .putBoolean(
              Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING,
              jsonObject.getBoolean(
                  "FEATURE_FLAG_STOCK_LOCATION_TRACKING"
              )
          )
          .putBoolean(
              Constants.PREF.FEATURE_STOCK_BBD_TRACKING,
              jsonObject.getBoolean(
                  "FEATURE_FLAG_STOCK_BEST_BEFORE_DATE_TRACKING"
              )
          )
          .putBoolean(
              Constants.PREF.FEATURE_STOCK_OPENED_TRACKING,
              jsonObject.getBoolean(
                  "FEATURE_FLAG_STOCK_PRODUCT_OPENED_TRACKING"
              )
          ).apply();
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "downloadConfig: " + e);
      }
    }
    if (debug) {
      Log.i(TAG, "downloadConfig: config = " + response);
    }
  }

  private static void storeUserSettings(String response, SharedPreferences prefs, boolean debug) {
    try {
      JSONObject jsonObject = new JSONObject(response);
      prefs.edit().putInt(
          STOCK.LOCATION,
          jsonObject.getInt(STOCK.LOCATION)
      ).putInt(
          STOCK.PRODUCT_GROUP,
          jsonObject.getInt(STOCK.PRODUCT_GROUP)
      ).putInt(
          STOCK.QUANTITY_UNIT,
          jsonObject.getInt(STOCK.QUANTITY_UNIT)
      ).putString(
          STOCK.DUE_SOON_DAYS,
          jsonObject.getString(STOCK.DUE_SOON_DAYS)
      ).putBoolean(
          STOCK.DISPLAY_DOTS_IN_STOCK,
          getBoolean(jsonObject, STOCK.DISPLAY_DOTS_IN_STOCK,
              SETTINGS_DEFAULT.STOCK.DISPLAY_DOTS_IN_STOCK, prefs)
      ).putBoolean(
          STOCK.SHOW_PURCHASED_DATE,
          getBoolean(jsonObject, STOCK.SHOW_PURCHASED_DATE,
              SETTINGS_DEFAULT.STOCK.SHOW_PURCHASED_DATE, prefs)
      ).putString(
          STOCK.DEFAULT_PURCHASE_AMOUNT,
          jsonObject.getString(STOCK.DEFAULT_PURCHASE_AMOUNT)
      ).putString(
          STOCK.DEFAULT_CONSUME_AMOUNT,
          jsonObject.getString(STOCK.DEFAULT_CONSUME_AMOUNT)
      ).putBoolean(
          STOCK.USE_QUICK_CONSUME_AMOUNT,
          getBoolean(jsonObject, STOCK.USE_QUICK_CONSUME_AMOUNT,
              SETTINGS_DEFAULT.STOCK.USE_QUICK_CONSUME_AMOUNT, prefs)
      ).putString(
          Constants.PREF.RECIPE_INGREDIENTS_GROUP_BY_PRODUCT_GROUP,
          jsonObject.getString(
              "recipe_ingredients_group_by_product_group"
          )
      ).apply();
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "downloadUserSettings: " + e);
      }
    }
    if (debug) {
      Log.i(TAG, "downloadUserSettings: settings = " + response);
    }
  }

  private static boolean getBoolean(
      JSONObject jsonObject,
      String settingKey,
      boolean settingDefault,
      SharedPreferences prefs
  ) {
    try {
      Object settingValue = jsonObject.get(settingKey);
      if (settingValue instanceof Integer) {
        return ((int) settingValue) == 1;
      } else if (settingValue instanceof Boolean) {
        return ((boolean) settingValue);
      } else if (settingValue instanceof String && ((String) settingValue).isEmpty()) {
        return false;
      } else if (settingValue instanceof String && NumUtil.isStringInt((String) settingValue)) {
        return Integer.parseInt((String) settingValue) == 1;
      } else if (settingValue instanceof String && ((String) settingValue).equals("false")) {
        return false;
      } else if (settingValue instanceof String && ((String) settingValue).equals("true")) {
        return true;
      } else {
        return prefs.getBoolean(settingKey, settingDefault);
      }
    } catch (JSONException e) {
      Log.e(TAG, "downloadUserSettings: getBoolean: settingKey="
          + settingKey + " Exception:" + e);
      return prefs.getBoolean(settingKey, settingDefault);
    }
  }

  private static void storeSystemInfo(String response, SharedPreferences prefs, boolean debug) {
    try {
      prefs.edit()
          .putString(
              Constants.PREF.GROCY_VERSION,
              new JSONObject(response).getJSONObject(
                  "grocy_version"
              ).getString("Version")
          ).apply();
      if (debug) {
        Log.i(TAG, "downloadSystemInfo: " + response);
      }
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "downloadSystemInfo: " + e);
      }
    }
  }
}
