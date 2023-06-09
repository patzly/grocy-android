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

package xyz.zedler.patrick.grocy.api;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import java.nio.charset.StandardCharsets;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.util.LocaleUtil;

public class GrocyApi {

  private final static String TAG = GrocyApi.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final String baseUrl;

  public final static class ENTITY {

    public final static String PRODUCTS = "products";
    public final static String PRODUCTS_LAST_PURCHASED = "products_last_purchased";
    public final static String PRODUCTS_AVERAGE_PRICE = "products_average_price";
    public final static String PRODUCT_BARCODES = "product_barcodes";
    public final static String LOCATIONS = "locations";
    public final static String STOCK_ENTRIES = "stock";
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
    public final static String TASKS = "tasks";
    public final static String TASK_CATEGORIES = "task_categories";
    public final static String CHORES = "chores";
  }

  public final static class COMPARISON_OPERATOR {
    private final String value;

    private COMPARISON_OPERATOR(String value) {
      this.value = value;
    }

    @NonNull
    @Override
    public String toString() {
      return value;
    }

    public final static COMPARISON_OPERATOR EQUAL = new COMPARISON_OPERATOR("%3D");
    public final static COMPARISON_OPERATOR NOT_EQUAL = new COMPARISON_OPERATOR("!%3D");
    public final static COMPARISON_OPERATOR LIKE = new COMPARISON_OPERATOR("~");
    public final static COMPARISON_OPERATOR NOT_LIKE = new COMPARISON_OPERATOR("!~");
    public final static COMPARISON_OPERATOR LESS = new COMPARISON_OPERATOR("%3C");
    public final static COMPARISON_OPERATOR GREATER = new COMPARISON_OPERATOR("%3E");
    public final static COMPARISON_OPERATOR LESS_OR_EQUAL = new COMPARISON_OPERATOR("%3C%3D");
    public final static COMPARISON_OPERATOR GREATER_OR_EQUAL = new COMPARISON_OPERATOR("%3E%3D");
    public final static COMPARISON_OPERATOR REGEX = new COMPARISON_OPERATOR("%C2%A7");
  }

  public final static class COMPARISON {
    private final String field;
    private final COMPARISON_OPERATOR operator;
    private final String value;

    public COMPARISON(String field, COMPARISON_OPERATOR operator, String value) {
      this.field = field;
      this.operator = operator;
      this.value = value;
    }

    public String getQueryParam() {
      return field + operator + value;
    }
  }

  public GrocyApi(Application application) {
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    String demoDomain = LocaleUtil.getLocalizedGrocyDemoDomain(application);
    baseUrl = sharedPrefs.getString(
        Constants.PREF.SERVER_URL,
        demoDomain != null && !demoDomain.isBlank()
            ? "https://" + demoDomain
            : application.getString(R.string.url_grocy_demo_default)
    );
  }

  public GrocyApi(Application application, String serverUrl) {
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    baseUrl = serverUrl;
  }

  private String getUrl(String command) {
    return baseUrl + "/api" + command;
  }

  public String getUrl(String command, COMPARISON... comparisons) {

    StringBuilder url = new StringBuilder(getUrl(command));
    if (comparisons.length > 0) {
      url.append("?");

      for (COMPARISON comparison : comparisons) {
        url.append("query%5B%5D=");
        url.append(comparison.getQueryParam());
        url.append("&");
      }

      // Delete last & character
      url.deleteCharAt(url.length() - 1);
    }

    return url.toString();
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
   * Returns all objects of the given entity and comparisons
   */
  public String getObjects(String entity, COMPARISON... comparisons) {

    return getUrl("/objects/" + entity, comparisons);
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
   * Returns all users
   */
  public String getUsers() {
    return getUrl("/users");
  }

  /**
   * Returns the currently authenticated user
   */
  public String getUser() {
    return getUrl("/user");
  }

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
    return getUrl(
        "/stock/products/" + productId + "/locations",
        "include_sub_products=true"
    );
  }

  /**
   * Returns all stock entries of the given product in order of next use (first expiring first, then
   * first in first out)
   */
  public String getStockEntriesFromProduct(int productId) {
    return getUrl(
        "/stock/products/" + productId + "/entries",
        "include_sub_products=true"
    );
  }

  public String getStockLogEntries(int limit, int offset, int filterProductId) {
    if (filterProductId == -1) {
      return getUrl(
          "/objects/stock_log",
          "limit=" + limit,
          "offset=" + offset,
          "order=id%3Adesc"
      );
    } else {
      return getUrl(
          "/objects/stock_log",
          "query%5B%5D=product_id%3D" + filterProductId,
          "limit=" + limit,
          "offset=" + offset,
          "order=id%3Adesc"
      );
    }
  }

  /**
   * Returns all products which are currently in stock incl. the next due date per product
   */
  public String getStockVolatile() {
    return getUrl(
        "/stock/volatile",
        "due_soon_days=" + sharedPrefs.getString(
            STOCK.DUE_SOON_DAYS,
            SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS
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

  // TASK

  /**
   * Marks the given task as completed
   */
  public String completeTask(int taskId) {
    return getUrl("/tasks/" + taskId + "/complete");
  }

  /**
   * Marks the given task as not completed
   */
  public String undoTask(int taskId) {
    return getUrl("/tasks/" + taskId + "/undo");
  }

  // CHORES

  /**
   * Returns all chores incl. the next estimated execution time per chore
   */
  public String getChores() {
    return getUrl("/chores");
  }

  /**
   * Returns details of the given chore
   */
  public String getChores(int choreId) {
    return getUrl("/chores/" + choreId);
  }

  /**
   * Tracks an execution of the given chore
   */
  public String executeChore(int choreId) {
    return getUrl("/chores/" + choreId + "/execute");
  }

  // RECIPES

  /**
   * Returns all recipes
   */
  public String getRecipes() {
    return getObjects(
            "recipes",
            new COMPARISON("id", COMPARISON_OPERATOR.GREATER, "0")
    );
  }

  public String getRecipeFulfillments() {
    return getUrl(
            "/recipes/fulfillment",
            new COMPARISON("recipe_id", COMPARISON_OPERATOR.GREATER, "0")
    );
  }

  public String getRecipePositions() {
    return getObjects(
            "recipes_pos",
            new COMPARISON("recipe_id", COMPARISON_OPERATOR.GREATER, "0")
    );
  }

  public String consumeRecipe(int recipeId) {
    return getUrl("/recipes/" + recipeId + "/consume");
  }

  public String addNotFulfilledProductsToCartForRecipe(int recipeId) {
    return getUrl("/recipes/" + recipeId + "/add-not-fulfilled-products-to-shoppinglist");
  }

  public String copyRecipe(int recipeId) {
    return getUrl("/recipes/" + recipeId + "/copy");
  }

  // FILES

  public String getRecipePicture(String filename) {
    String fileNameEncoded = new String(Base64.encode(
        filename.getBytes(StandardCharsets.UTF_8),
        Base64.DEFAULT
    ), StandardCharsets.UTF_8);
    return getUrl(
        "/files/recipepictures/"
            + fileNameEncoded.replace("\n", "")
            + "?force_serve_as=picture&best_fit_height=240&best_fit_width=360"
    );
  }

  public String getProductPicture(String filename) {
    String fileNameEncoded = new String(Base64.encode(
        filename.getBytes(StandardCharsets.UTF_8),
        Base64.DEFAULT
    ), StandardCharsets.UTF_8);
    return getUrl(
        "/files/productpictures/"
            + fileNameEncoded.replace("\n", "")
            + "?force_serve_as=picture&best_fit_height=240&best_fit_width=360"
    );
  }
}
