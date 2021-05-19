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
          STOCK.SHOW_PURCHASED_DATE,
          jsonObject.getBoolean(STOCK.SHOW_PURCHASED_DATE)
      ).putString(
          STOCK.DEFAULT_PURCHASE_AMOUNT,
          jsonObject.getString(STOCK.DEFAULT_PURCHASE_AMOUNT)
      ).putString(
          STOCK.DEFAULT_CONSUME_AMOUNT,
          jsonObject.getString(STOCK.DEFAULT_CONSUME_AMOUNT)
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
    try {
      // try to get boolean for indicator setting – but responses can also
      // contain this setting as number (0 or 1)
      prefs.edit().putBoolean(
          STOCK.DISPLAY_DOTS_IN_STOCK,
          new JSONObject(response).getBoolean(STOCK.DISPLAY_DOTS_IN_STOCK)
      ).apply();
    } catch (JSONException e) {
      try {
        // try to get boolean from number in json
        int stateInt = new JSONObject(response).getInt(STOCK.DISPLAY_DOTS_IN_STOCK);
        prefs.edit().putBoolean(STOCK.DISPLAY_DOTS_IN_STOCK, stateInt == 1).apply();
      } catch (JSONException e2) {
        if (debug) {
          Log.e(TAG, "downloadUserSettings: " + e2);
        }
      }
    }
    if (debug) {
      Log.i(TAG, "downloadUserSettings: settings = " + response);
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
