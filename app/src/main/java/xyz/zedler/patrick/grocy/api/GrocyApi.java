package xyz.zedler.patrick.grocy.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.preference.PreferenceManager;

import java.nio.charset.StandardCharsets;

import xyz.zedler.patrick.grocy.util.Constants;

public class GrocyApi {

    private final static String TAG = "GrocyApi";
    private final static boolean DEBUG = true;

    private SharedPreferences sharedPrefs;

    public final static class ENTITY {
        public final static String PRODUCTS = "products";
        public final static String LOCATIONS = "locations";
        public final static String QUANTITY_UNITS = "quantity_units";
        public final static String QUANTITY_UNIT_CONVERSIONS = "quantity_unit_conversions";
        public final static String SHOPPING_LIST = "shopping_list";
        public final static String SHOPPING_LISTS = "shopping_lists";
        public final static String RECIPES = "recipes";
        public final static String RECIPES_POS = "recipes_pos";
        public final static String RECIPES_NEST = "recipes_nestings";
        public final static String PRODUCT_GROUPS = "product_groups";
        public final static String MEAL_PLAN = "meal_plan";
    }

    private String baseUrl, apiKey;

    public GrocyApi(Context context) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        baseUrl = sharedPrefs.getString(
                Constants.PREF.SERVER_URL,
                "https://de.demo.grocy.info"
        );
        apiKey = "?GROCY-API-KEY=" + sharedPrefs.getString(Constants.PREF.API_KEY, "");
    }

    private String getUrl(String command) {
        return baseUrl + "/api" + command + apiKey;
    }

    /*private String getUrl(String command, String param) {
        return getUrl(command) + "&" + param;
    }*/

    private String getUrl(String command, String... params) {
        StringBuilder url = new StringBuilder(getUrl(command));
        for(String param : params) {
            url.append("&").append(param);
        }
        return url.toString();
    }

    // ENTITY

    /**
     * Returns all objects of the given entity
     */
    public String getObjects(String entity) {
        return getUrl("/objects/" + entity);
    }

    public String addObject(String entity) {
        return getUrl("/objects/" + entity);
    }

    // SYSTEM

    /**
     * Returns information about the installed grocy, PHP and SQLite version
     */
    public String getSystemInfo() {
        return getUrl("/system/info");
    }

    // STOCK

    /**
     * Returns all products which are currently in stock incl. the next expiring date per product
     */
    public String getStock() {
        return getUrl("/stock");
    }

    /**
     * Returns details of the given product
     */
    public String getStockProduct(int productId) {
        return getUrl("/stock/products/" + productId);
    }

    /**
     * Returns all products which are currently in stock incl. the next expiring date per product
     */
    public String getStockVolatile() {
        return getUrl(
                "/stock/volatile", "expiring_days="
                        + sharedPrefs.getInt(Constants.PREF.STOCK_EXPIRING_SOON_DAYS, 5)
                );
    }

    // PICTURE

    /**
     * Serves the given picture
     */
    public String getPicture(String fileName, int width) {
        if(fileName != null) {
            return getUrl(
                    "/files/productpictures/" + Base64.encodeToString(
                            fileName.getBytes(StandardCharsets.UTF_8),
                            Base64.DEFAULT
                    ), "force_serve_as=picture",
                    "best_fit_width=" + width
            );
        } else {
            return null;
        }
    }
}
