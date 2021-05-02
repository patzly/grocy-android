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

package xyz.zedler.patrick.grocy.api;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.Constants;

public class GrocyApi {

  private final static String TAG = GrocyApi.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final String baseUrl;

  public final static class ENTITY {

    public final static String PRODUCTS = "products";
    public final static String PRODUCT_BARCODES = "product_barcodes";
    public final static String LOCATIONS = "locations";
    public final static String STOCK_CURRENT_LOCATIONS = "stock_current_locations";
    public final static String STORES = "shopping_locations";
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

  public GrocyApi(Application application) {
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    baseUrl = sharedPrefs.getString(
        Constants.PREF.SERVER_URL,
        application.getString(R.string.url_grocy_demo)
    );
  }

  public GrocyApi(Application application, String serverUrl) {
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    baseUrl = serverUrl;
  }

  public void loadCredentials() {

  }

  private String getUrl(String command) {
    return baseUrl + "/api" + command;
  }

  private String getUrl(String command, String... params) {
    StringBuilder url = new StringBuilder(getUrl(command));
    if (params.length > 0) {
      url.append("?");
    }
    for (String param : params) {
      url.append(param);
      if (!param.equals(params[params.length - 1])) {
        url.append("&");
      }
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

  /**
   * Returns all objects of the given entity and filter
   */
  public String getObjectsEqualValue(String entity, String field, String value) {
    return getUrl("/objects/" + entity + "?query%5B%5D=" + field + "%3D" + value);
  }

  /**
   * Returns a single object of the given entity
   */
  public String getObject(String entity, int id) {
    return getUrl("/objects/" + entity + "/" + id);
  }

  // SYSTEM

  /**
   * Returns information about the installed grocy, PHP and SQLite version
   */
  public String getSystemInfo() {
    return getUrl("/system/info");
  }

  /**
   * Returns all config settings
   */
  public String getSystemConfig() {
    return getUrl("/system/config");
  }

  /**
   * Returns the time when the database was last changed
   */
  public String getDbChangedTime() {
    return getUrl("/system/db-changed-time");
  }

  // USER

  /**
   * Returns all settings of the currently logged in user
   */
  public String getUserSettings() {
    return getUrl("/user/settings");
  }

  /**
   * Returns the given setting of the currently logged in user
   */
  public String getUserSetting(String key) {
    return getUrl("/user/settings/" + key);
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
  public String getStockProductDetails(int productId) {
    return getUrl("/stock/products/" + productId);
  }

  /**
   * Returns all locations where the given product currently has stock
   */
  public String getStockLocationsFromProduct(int productId) {
    return getUrl("/stock/products/" + productId + "/locations?include_sub_products=true");
  }

  /**
   * Returns all stock entries of the given product in order of next use (first expiring first, then
   * first in first out)
   */
  public String getStockEntriesFromProduct(int productId) {
    return getUrl("/stock/products/" + productId + "/entries?include_sub_products=true");
  }

  /**
   * Returns all products which are currently in stock incl. the next due date per product
   */
  public String getStockVolatile() {
    // TODO: https://de.demo.grocy.info/api/user/settings/stock_expring_soon_days PUT {"value":"5"}
    // We have to make value changeable in the App, but also update it with the request above
    // on the server; and get the value on the first App start from server
    return getUrl(
        "/stock/volatile", "due_soon_days="
            + sharedPrefs.getString(
            Constants.PREF.STOCK_DUE_SOON_DAYS,
            String.valueOf(5)
        )
    );
  }

  /**
   * Returns the price history of the given product
   */
  public String getPriceHistory(int productId) {
    return getUrl("/stock/products/" + productId + "/price-history");
  }

  /**
   * Adds the given amount of the given product to stock
   */
  public String purchaseProduct(int productId) {
    return getUrl("/stock/products/" + productId + "/add");
  }

  /**
   * Removes the given amount of the given product from stock
   */
  public String consumeProduct(int productId) {
    return getUrl("/stock/products/" + productId + "/consume");
  }

  /**
   * Transfers the given amount of the given product from one location to another
   * (this is currently not supported for tare weight handling enabled products)
   */
  public String transferProduct(int productId) {
    return getUrl("/stock/products/" + productId + "/transfer");
  }

  /**
   * Inventories the given product (adds/removes based on the given new amount)
   */
  public String inventoryProduct(int productId) {
    return getUrl("/stock/products/" + productId + "/inventory");
  }

  /**
   * Marks the given amount of the given product as opened
   */
  public String openProduct(int productId) {
    return getUrl("/stock/products/" + productId + "/open");
  }

  /**
   * Undoes a transaction
   */
  public String undoStockTransaction(String transactionId) {
    return getUrl("/stock/transactions/" + transactionId + "/undo");
  }

  // STOCK BY BARCODE

  /**
   * Returns details of the given product by its barcode
   */
  public String getStockProductByBarcode(String barcode) {
    return getUrl("/stock/products/by-barcode/" + barcode);
  }

  // SHOPPING LIST

  /**
   * Adds currently missing products to the given shopping list
   */
  public String addMissingProducts() {
    return getUrl("/stock/shoppinglist/add-missing-products");
  }

  /**
   * Removes all items from the given shopping list
   */
  public String clearShoppingList() {
    return getUrl("/stock/shoppinglist/clear");
  }
}
