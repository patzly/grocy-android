package xyz.zedler.patrick.grocy.util;

import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.RequestQueue;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class ConfigUtil {

    private final static String TAG = ConfigUtil.class.getSimpleName();
    private final static boolean DEBUG = true;

    public static boolean isFeatureEnabled(SharedPreferences sharedPrefs, String feature) {
        if(feature == null) return true;
        return sharedPrefs.getBoolean(feature, true);
    }

    public static void loadInfo(
            RequestQueue requestQueue,
            GrocyApi api,
            SharedPreferences prefs,
            Runnable onSuccessAction,
            Runnable onErrorAction
    ) {
        WebRequest request = new WebRequest(requestQueue);
        AtomicBoolean noErrorOccurred = new AtomicBoolean(true);

        // GET CONFIG
        request.get(
                api.getSystemConfig(),
                TAG,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        prefs.edit()
                                .putString(
                                        Constants.PREF.CURRENCY,
                                        jsonObject.getString("CURRENCY")
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
                        Log.e(TAG, "downloadConfig: " + e);
                    }
                    if (DEBUG) Log.i(TAG, "downloadConfig: config = " + response);
                },
                error -> {
                    noErrorOccurred.set(false);
                    if(onErrorAction != null) onErrorAction.run();
                    Log.e(TAG, "downloadConfig: " + error);
                },
                () -> {
                    if(onSuccessAction != null && noErrorOccurred.get()) onSuccessAction.run();
                }
        );
        // GET USER SETTINGS
        request.get(
                api.getUserSettings(),
                TAG,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        prefs.edit().putInt(
                                Constants.PREF.PRODUCT_PRESETS_LOCATION_ID,
                                jsonObject.getInt("product_presets_location_id")
                        ).putInt(
                                Constants.PREF.PRODUCT_PRESETS_PRODUCT_GROUP_ID,
                                jsonObject.getInt("product_presets_product_group_id")
                        ).putInt(
                                Constants.PREF.PRODUCT_PRESETS_QU_ID,
                                jsonObject.getInt("product_presets_qu_id")
                        ).putString(
                                Constants.PREF.STOCK_EXPIRING_SOON_DAYS,
                                jsonObject.getString("stock_expring_soon_days")
                        ).putString(
                                Constants.PREF.STOCK_DEFAULT_PURCHASE_AMOUNT,
                                jsonObject.getString("stock_default_purchase_amount")
                        ).putString(
                                Constants.PREF.STOCK_DEFAULT_CONSUME_AMOUNT,
                                jsonObject.getString("stock_default_consume_amount")
                        ).putString(
                                Constants.PREF.RECIPE_INGREDIENTS_GROUP_BY_PRODUCT_GROUP,
                                jsonObject.getString(
                                        "recipe_ingredients_group_by_product_group"
                                )
                        ).apply();
                    } catch (JSONException e) {
                        Log.e(TAG, "downloadUserSettings: " + e);
                    }
                    try {
                        // try to get boolean for indicator setting – but responses can also
                        // contain this setting as number (0 or 1)
                        prefs.edit().putBoolean(
                                Constants.PREF.SHOW_SHOPPING_LIST_ICON_IN_STOCK,
                                new JSONObject(response).getBoolean(
                                "show_icon_on_stock_overview_page_" +
                                        "when_product_is_on_shopping_list"
                                )
                        ).apply();
                    } catch (JSONException e) {
                        try {
                            // try to get boolean from number in json
                            int stateInt = new JSONObject(response).getInt(
                                    "show_icon_on_stock_overview_page_" +
                                            "when_product_is_on_shopping_list"
                            );
                            prefs.edit().putBoolean(
                                    Constants.PREF.SHOW_SHOPPING_LIST_ICON_IN_STOCK,
                                    stateInt == 1
                            ).apply();
                        } catch (JSONException e2) {
                            Log.e(TAG, "downloadUserSettings: " + e2);
                        }
                    }
                    if(DEBUG) Log.i(TAG, "downloadUserSettings: settings = " + response);
                },
                error -> {
                    noErrorOccurred.set(false);
                    if(onErrorAction != null) onErrorAction.run();
                    Log.e(TAG, "downloadUserSettings: " + error);
                },
                () -> {
                    if(onSuccessAction != null && noErrorOccurred.get()) onSuccessAction.run();
                }
        );
        // GET SYSTEM INFO
        request.get(
                api.getSystemInfo(),
                TAG,
                response -> {
                    try {
                        prefs.edit()
                                .putString(
                                        Constants.PREF.GROCY_VERSION,
                                        new JSONObject(response).getJSONObject(
                                                "grocy_version"
                                        ).getString("Version")
                                ).apply();
                        if(DEBUG) Log.i(TAG, "downloadSystemInfo: " + response);
                    } catch (JSONException e) {
                        Log.e(TAG, "downloadSystemInfo: " + e);
                    }
                },
                error -> {
                    noErrorOccurred.set(false);
                    if(onErrorAction != null) onErrorAction.run();
                    Log.e(TAG, "downloadSystemInfo: " + error);
                },
                () -> {
                    if(onSuccessAction != null && noErrorOccurred.get()) onSuccessAction.run();
                }
        );
    }
}
