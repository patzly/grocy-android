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

package xyz.zedler.patrick.grocy.model;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.QueueItem;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;

@Entity(tableName = "product_table")
public class Product extends GroupedListItem implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private int id;

  @ColumnInfo(name = "name")
  @SerializedName("name")
  private String name;

  @ColumnInfo(name = "description")
  @SerializedName("description")
  private String description;

  @ColumnInfo(name = "product_group_id")
  @SerializedName("product_group_id")
  private String productGroupId;

  @ColumnInfo(name = "active")
  @SerializedName("active")
  private String active;

  @ColumnInfo(name = "location_id")
  @SerializedName("location_id")
  private String locationId;

  @ColumnInfo(name = "shopping_location_id")
  @SerializedName("shopping_location_id")
  private String storeId;

  @ColumnInfo(name = "qu_id_purchase")
  @SerializedName("qu_id_purchase")
  private String quIdPurchase; // quantity unit

  @ColumnInfo(name = "qu_id_stock")
  @SerializedName("qu_id_stock")
  private String quIdStock; // quantity unit

  @ColumnInfo(name = "qu_factor_purchase_to_stock")
  @SerializedName("qu_factor_purchase_to_stock")
  private String quFactorPurchaseToStock; // quantity unit

  @ColumnInfo(name = "min_stock_amount")
  @SerializedName("min_stock_amount")
  private String minStockAmount;

  @ColumnInfo(name = "default_best_before_days")
  @SerializedName("default_best_before_days")
  private String defaultDueDays;

  @ColumnInfo(name = "default_best_before_days_after_open")
  @SerializedName("default_best_before_days_after_open")
  private String defaultDueDaysAfterOpen;

  @ColumnInfo(name = "default_best_before_days_after_freezing")
  @SerializedName("default_best_before_days_after_freezing")
  private String defaultDueDaysAfterFreezing;

  @ColumnInfo(name = "default_best_before_days_after_thawing")
  @SerializedName("default_best_before_days_after_thawing")
  private String defaultDueDaysAfterThawing;

  @ColumnInfo(name = "picture_file_name")
  @SerializedName("picture_file_name")
  private String pictureFileName;

  @ColumnInfo(name = "enable_tare_weight_handling")
  @SerializedName("enable_tare_weight_handling")
  private String enableTareWeightHandling;

  @ColumnInfo(name = "tare_weight")
  @SerializedName("tare_weight")
  private String tareWeight;

  @ColumnInfo(name = "not_check_stock_fulfillment_for_recipes")
  @SerializedName("not_check_stock_fulfillment_for_recipes")
  private String notCheckStockFulfillmentForRecipes;

  @ColumnInfo(name = "parent_product_id")
  @SerializedName("parent_product_id")
  private String parentProductId; /// STRING: null for empty

  @ColumnInfo(name = "calories")
  @SerializedName("calories")
  private String calories;

  @ColumnInfo(name = "cumulate_min_stock_amount_of_sub_products")
  @SerializedName("cumulate_min_stock_amount_of_sub_products")
  private String accumulateSubProductsMinStockAmount;

  @ColumnInfo(name = "due_type")
  @SerializedName("due_type")
  private String dueDateType;

  @ColumnInfo(name = "quick_consume_amount")
  @SerializedName("quick_consume_amount")
  private String quickConsumeAmount;

  @ColumnInfo(name = "hide_on_stock_overview")
  @SerializedName("hide_on_stock_overview")
  private String hideOnStockOverview;

  @ColumnInfo(name = "default_stock_label_type")
  @SerializedName("default_stock_label_type")
  private String defaultStockLabelType;

  @ColumnInfo(name = "should_not_be_frozen")
  @SerializedName("should_not_be_frozen")
  private String shouldNotBeFrozen;

  @ColumnInfo(name = "treat_opened_as_out_of_stock")
  @SerializedName("treat_opened_as_out_of_stock")
  private String treatOpenedAsOutOfStock;

  @ColumnInfo(name = "no_own_stock")
  @SerializedName("no_own_stock")
  private String noOwnStock;

  @ColumnInfo(name = "default_consume_location_id")
  @SerializedName("default_consume_location_id")
  private String defaultConsumeLocationId;

  @ColumnInfo(name = "move_on_open")
  @SerializedName("move_on_open")
  private String moveOnOpen;

  @Ignore
  private Integer pendingProductId;

  @Ignore
  private boolean displayDivider;

  public Product() {
  }  // for Room

  @Ignore
  public Product(SharedPreferences sharedPrefs) {
    int presetLocationId = sharedPrefs.getInt(
        STOCK.LOCATION,
        SETTINGS_DEFAULT.STOCK.LOCATION
    );
    int presetProductGroupId = sharedPrefs.getInt(
        STOCK.PRODUCT_GROUP,
        SETTINGS_DEFAULT.STOCK.PRODUCT_GROUP
    );
    int presetQuId = sharedPrefs.getInt(
        STOCK.QUANTITY_UNIT,
        SETTINGS_DEFAULT.STOCK.QUANTITY_UNIT
    );
    int presetDefaultDueDays = sharedPrefs.getInt(
        STOCK.DEFAULT_DUE_DAYS,
        SETTINGS_DEFAULT.STOCK.DEFAULT_DUE_DAYS
    );
    boolean presetTreatOpened = sharedPrefs.getBoolean(
        STOCK.TREAT_OPENED_OUT_OF_STOCK,
        SETTINGS_DEFAULT.STOCK.TREAT_OPENED_OUT_OF_STOCK
    );
    name = null;  // initialize default values (used in masterProductFragment)
    active = "1";
    parentProductId = null;
    description = null;
    if (sharedPrefs.getBoolean(PREF.FEATURE_STOCK_LOCATION_TRACKING, true)) {
      locationId = presetLocationId == -1 ? null : String.valueOf(presetLocationId);
    } else {
      locationId = "1";
    }
    storeId = null;
    minStockAmount = String.valueOf(0);
    accumulateSubProductsMinStockAmount = "0";
    dueDateType = "1";
    defaultDueDays = String.valueOf(presetDefaultDueDays);
    defaultDueDaysAfterOpen = "0";
    productGroupId = presetProductGroupId == -1 ? null : String.valueOf(presetProductGroupId);
    String presetQuIdStr = presetQuId == -1 ? null : String.valueOf(presetQuId);
    quIdStock = presetQuIdStr;
    quIdPurchase = presetQuIdStr;
    quFactorPurchaseToStock = "1";
    enableTareWeightHandling = "0";
    tareWeight = "0";
    notCheckStockFulfillmentForRecipes = "0";
    calories = "0";
    defaultDueDaysAfterFreezing = "0";
    defaultDueDaysAfterThawing = "0";
    quickConsumeAmount = "1";
    hideOnStockOverview = "0";
    defaultStockLabelType = "0";
    shouldNotBeFrozen = "0";
    treatOpenedAsOutOfStock = presetTreatOpened ? "1" : "0";
    noOwnStock = "0";
    defaultConsumeLocationId = null;
    moveOnOpen = "0";
  }

  @Ignore
  public Product(Parcel parcel) {
    id = parcel.readInt();
    name = parcel.readString();
    description = parcel.readString();
    productGroupId = parcel.readString();
    active = parcel.readString();
    locationId = parcel.readString();
    storeId = parcel.readString();
    quIdPurchase = parcel.readString();
    quIdStock = parcel.readString();
    quFactorPurchaseToStock = parcel.readString();
    minStockAmount = parcel.readString();
    defaultDueDays = parcel.readString();
    defaultDueDaysAfterOpen = parcel.readString();
    defaultDueDaysAfterFreezing = parcel.readString();
    defaultDueDaysAfterThawing = parcel.readString();
    pictureFileName = parcel.readString();
    enableTareWeightHandling = parcel.readString();
    tareWeight = parcel.readString();
    notCheckStockFulfillmentForRecipes = parcel.readString();
    parentProductId = parcel.readString();
    calories = parcel.readString();
    accumulateSubProductsMinStockAmount = parcel.readString();
    dueDateType = parcel.readString();
    quickConsumeAmount = parcel.readString();
    hideOnStockOverview = parcel.readString();
    defaultStockLabelType = parcel.readString();
    shouldNotBeFrozen = parcel.readString();
    treatOpenedAsOutOfStock = parcel.readString();
    noOwnStock = parcel.readString();
    defaultConsumeLocationId = parcel.readString();
    moveOnOpen = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeString(name);
    dest.writeString(description);
    dest.writeString(productGroupId);
    dest.writeString(active);
    dest.writeString(locationId);
    dest.writeString(storeId);
    dest.writeString(quIdPurchase);
    dest.writeString(quIdStock);
    dest.writeString(quFactorPurchaseToStock);
    dest.writeString(minStockAmount);
    dest.writeString(defaultDueDays);
    dest.writeString(defaultDueDaysAfterOpen);
    dest.writeString(defaultDueDaysAfterFreezing);
    dest.writeString(defaultDueDaysAfterThawing);
    dest.writeString(pictureFileName);
    dest.writeString(enableTareWeightHandling);
    dest.writeString(tareWeight);
    dest.writeString(notCheckStockFulfillmentForRecipes);
    dest.writeString(parentProductId);
    dest.writeString(calories);
    dest.writeString(accumulateSubProductsMinStockAmount);
    dest.writeString(dueDateType);
    dest.writeString(quickConsumeAmount);
    dest.writeString(hideOnStockOverview);
    dest.writeString(defaultStockLabelType);
    dest.writeString(shouldNotBeFrozen);
    dest.writeString(treatOpenedAsOutOfStock);
    dest.writeString(noOwnStock);
    dest.writeString(defaultConsumeLocationId);
    dest.writeString(moveOnOpen);
  }

  public static final Creator<Product> CREATOR = new Creator<>() {

    @Override
    public Product createFromParcel(Parcel in) {
      return new Product(in);
    }

    @Override
    public Product[] newArray(int size) {
      return new Product[size];
    }
  };

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getProductGroupId() {
    return productGroupId;
  }

  public void setProductGroupId(String productGroupId) {
    this.productGroupId = productGroupId;
  }

  public String getActive() {
    return active;
  }

  public boolean isActive() {
    return NumUtil.isStringInt(active) && Integer.parseInt(active) == 1;
  }

  public void setActive(String active) {
    this.active = active;
  }

  public void setActive(boolean active) {
    this.active = active ? "1" : "0";
  }

  public int getLocationIdInt() {
    return NumUtil.isStringInt(locationId) ? Integer.parseInt(locationId) : -1;
  }

  public String getLocationId() {
    return locationId;
  }

  public void setLocationId(String locationId) {
    this.locationId = locationId;
  }

  public String getStoreId() {
    return storeId;
  }

  public void setStoreId(String storeId) {
    this.storeId = storeId;
  }

  public String getQuIdPurchase() {
    return quIdPurchase;
  }

  public int getQuIdPurchaseInt() {
    return NumUtil.isStringInt(quIdPurchase) ? Integer.parseInt(quIdPurchase) : -1;
  }

  public void setQuIdPurchase(String quIdPurchase) {
    this.quIdPurchase = quIdPurchase;
  }

  public void setQuIdPurchase(int quIdPurchase) {
    this.quIdPurchase = String.valueOf(quIdPurchase);
  }

  public int getQuIdStockInt() {
    return NumUtil.isStringInt(quIdStock) ? Integer.parseInt(quIdStock) : -1;
  }

  public String getQuIdStock() {
    return quIdStock;
  }

  public void setQuIdStock(String quIdStock) {
    this.quIdStock = quIdStock;
  }

  public void setQuIdStock(int quIdStock) {
    this.quIdStock = String.valueOf(quIdStock);
  }

  public double getQuFactorPurchaseToStockDouble() {
    return NumUtil.isStringDouble(quFactorPurchaseToStock)
        ? NumUtil.toDouble(quFactorPurchaseToStock)
        : 1;
  }

  public String getQuFactorPurchaseToStock() {
    return quFactorPurchaseToStock;
  }

  public void setQuFactorPurchaseToStock(String quFactorPurchaseToStock) {
    this.quFactorPurchaseToStock = quFactorPurchaseToStock;
  }

  public String getMinStockAmount() {
    return minStockAmount;
  }

  public double getMinStockAmountDouble() {
    return NumUtil.isStringDouble(minStockAmount) ? NumUtil.toDouble(minStockAmount) : 0;
  }

  public void setMinStockAmount(String minStockAmount) {
    this.minStockAmount = minStockAmount;
  }

  public String getDefaultDueDays() {
    return defaultDueDays;
  }

  public int getDefaultDueDaysInt() {
    return NumUtil.isStringInt(defaultDueDays) ? Integer.parseInt(defaultDueDays) : 0;
  }

  public void setDefaultDueDays(String defaultDueDays) {
    this.defaultDueDays = defaultDueDays;
  }

  public String getDefaultDueDaysAfterOpen() {
    return defaultDueDaysAfterOpen;
  }

  public void setDefaultDueDaysAfterOpen(String defaultDueDaysAfterOpen) {
    this.defaultDueDaysAfterOpen = defaultDueDaysAfterOpen;
  }

  public String getDefaultDueDaysAfterFreezing() {
    return defaultDueDaysAfterFreezing;
  }

  public int getDefaultDueDaysAfterFreezingInt() {
    return NumUtil.isStringInt(defaultDueDaysAfterFreezing) ? Integer.parseInt(defaultDueDaysAfterFreezing) : 0;
  }

  public void setDefaultDueDaysAfterFreezing(String defaultDueDaysAfterFreezing) {
    this.defaultDueDaysAfterFreezing = defaultDueDaysAfterFreezing;
  }

  public String getDefaultDueDaysAfterThawing() {
    return defaultDueDaysAfterThawing;
  }

  public void setDefaultDueDaysAfterThawing(String defaultDueDaysAfterThawing) {
    this.defaultDueDaysAfterThawing = defaultDueDaysAfterThawing;
  }

  public String getPictureFileName() {
    return pictureFileName;
  }

  public void setPictureFileName(String pictureFileName) {
    this.pictureFileName = pictureFileName;
  }

  public String getEnableTareWeightHandling() {
    return enableTareWeightHandling;
  }

  public int getEnableTareWeightHandlingInt() {
    return NumUtil.isStringInt(enableTareWeightHandling)
        ? Integer.parseInt(enableTareWeightHandling) : 0;
  }

  public boolean getEnableTareWeightHandlingBoolean() {
    return getEnableTareWeightHandlingInt() == 1;
  }

  public void setEnableTareWeightHandling(boolean enableTareWeightHandling) {
    this.enableTareWeightHandling = enableTareWeightHandling ? "1" : "0";
  }

  public void setEnableTareWeightHandling(String enableTareWeightHandling) {
    this.enableTareWeightHandling = enableTareWeightHandling;
  }

  public String getTareWeight() {
    return tareWeight;
  }

  public double getTareWeightDouble() {
    return NumUtil.isStringDouble(tareWeight) ? NumUtil.toDouble(tareWeight) : 0;
  }

  public void setTareWeight(String tareWeight) {
    this.tareWeight = tareWeight;
  }

  public String getNotCheckStockFulfillmentForRecipes() {
    return notCheckStockFulfillmentForRecipes;
  }

  public boolean getNotCheckStockFulfillmentForRecipesBoolean() {
    return NumUtil.isStringInt(notCheckStockFulfillmentForRecipes)
        && Integer.parseInt(notCheckStockFulfillmentForRecipes) == 1;
  }

  public void setNotCheckStockFulfillmentForRecipes(String notCheckStockFulfillmentForRecipes) {
    this.notCheckStockFulfillmentForRecipes = notCheckStockFulfillmentForRecipes;
  }

  public void setNotCheckStockFulfillmentForRecipes(boolean notCheckStockFulfillmentForRecipes) {
    this.notCheckStockFulfillmentForRecipes = notCheckStockFulfillmentForRecipes ? "1" : "0";
  }

  public String getParentProductId() {
    return parentProductId;
  }

  public void setParentProductId(String parentProductId) {
    this.parentProductId = parentProductId;
  }

  public String getCalories() {
    return calories;
  }

  public double getCaloriesDouble() {
    return NumUtil.isStringDouble(calories) ? NumUtil.toDouble(calories) : 0;
  }

  public void setCalories(String calories) {
    this.calories = calories;
  }

  public boolean getAccumulateSubProductsMinStockAmountBoolean() {
    return NumUtil.isStringInt(accumulateSubProductsMinStockAmount)
        && Integer.parseInt(accumulateSubProductsMinStockAmount) == 1;
  }

  public String getAccumulateSubProductsMinStockAmount() {
    return accumulateSubProductsMinStockAmount;
  }

  public void setAccumulateSubProductsMinStockAmount(String accumulateSubProductsMinStockAmount) {
    this.accumulateSubProductsMinStockAmount = accumulateSubProductsMinStockAmount;
  }

  public void setAccumulateSubProductsMinStockAmount(boolean accumulateSubProductsMinStockAmount) {
    this.accumulateSubProductsMinStockAmount = accumulateSubProductsMinStockAmount ? "1" : "0";
  }

  public String getDueDateType() {
    return dueDateType;
  }

  public int getDueDateTypeInt() {
    return NumUtil.isStringInt(dueDateType) ? Integer.parseInt(dueDateType) : 1;
  }

  public void setDueDateType(String dueDateType) {
    this.dueDateType = dueDateType;
  }

  public void setDueDateTypeInt(int dueDateType) {
    this.dueDateType = String.valueOf(dueDateType);
  }

  public String getQuickConsumeAmount() {
    return quickConsumeAmount;
  }

  public double getQuickConsumeAmountDouble() {
    return NumUtil.isStringDouble(quickConsumeAmount) ? NumUtil.toDouble(quickConsumeAmount) : 1;
  }

  public void setQuickConsumeAmount(String quickConsumeAmount) {
    this.quickConsumeAmount = quickConsumeAmount;
  }

  public String getHideOnStockOverview() {
    return hideOnStockOverview;
  }

  public boolean getHideOnStockOverviewBoolean() {
    return NumUtil.isStringInt(hideOnStockOverview) && Integer.parseInt(hideOnStockOverview) == 1;
  }

  public void setHideOnStockOverview(String hideOnStockOverview) {
    this.hideOnStockOverview = hideOnStockOverview;
  }

  public void setHideOnStockOverviewBoolean(boolean hideOnStockOverview) {
    this.hideOnStockOverview = hideOnStockOverview ? "1" : "0";
  }

  public int getDefaultStockLabelTypeInt() {
    return NumUtil.isStringInt(defaultStockLabelType) ? Integer.parseInt(defaultStockLabelType) : 0;
  }

  public String getDefaultStockLabelType() {
    return defaultStockLabelType;
  }

  public void setDefaultStockLabelType(String defaultStockLabelType) {
    this.defaultStockLabelType = defaultStockLabelType;
  }

  public String getShouldNotBeFrozen() {
    return shouldNotBeFrozen;
  }

  public boolean getShouldNotBeFrozenBoolean() {
    return NumUtil.isStringInt(shouldNotBeFrozen) && Integer.parseInt(shouldNotBeFrozen) == 1;
  }

  public void setShouldNotBeFrozen(String shouldNotBeFrozen) {
    this.shouldNotBeFrozen = shouldNotBeFrozen;
  }

  public void setShouldNotBeFrozenBoolean(boolean shouldNotBeFrozen) {
    this.shouldNotBeFrozen = shouldNotBeFrozen ? "1" : "0";
  }

  public boolean getTreatOpenedAsOutOfStockBoolean() {
    return NumUtil.isStringInt(treatOpenedAsOutOfStock)
        && Integer.parseInt(treatOpenedAsOutOfStock) == 1;
  }

  public String getTreatOpenedAsOutOfStock() {
    return treatOpenedAsOutOfStock;
  }

  public void setTreatOpenedAsOutOfStock(String treatOpenedAsOutOfStock) {
    this.treatOpenedAsOutOfStock = treatOpenedAsOutOfStock;
  }

  public String getNoOwnStock() {
    return noOwnStock;
  }

  public boolean getNoOwnStockBoolean() {
    return NumUtil.isStringInt(noOwnStock) && Integer.parseInt(noOwnStock) == 1;
  }

  public void setNoOwnStock(String noOwnStock) {
    this.noOwnStock = noOwnStock;
  }

  public void setNoOwnStockBoolean(boolean noOwnStock) {
    this.noOwnStock = noOwnStock ? "1" : "0";
  }

  public String getDefaultConsumeLocationId() {
    return defaultConsumeLocationId;
  }

  public void setDefaultConsumeLocationId(String defaultConsumeLocationId) {
    this.defaultConsumeLocationId = defaultConsumeLocationId;
  }

  public String getMoveOnOpen() {
    return moveOnOpen;
  }

  public boolean getMoveOnOpenBoolean() {
    return NumUtil.isStringInt(moveOnOpen) && Integer.parseInt(moveOnOpen) == 1;
  }

  public void setMoveOnOpen(String moveOnOpen) {
    this.moveOnOpen = moveOnOpen;
  }

  public void setMoveOnOpenBoolean(boolean moveOnOpen) {
    this.moveOnOpen = moveOnOpen ? "1" : "0";
  }

  public Integer getPendingProductId() {
    return pendingProductId;
  }

  public void setPendingProductId(Integer pendingProductId) {
    this.pendingProductId = pendingProductId;
  }

  public boolean isDisplayDivider() {
    return displayDivider;
  }

  public void setDisplayDivider(boolean displayDivider) {
    this.displayDivider = displayDivider;
  }

  public static JSONObject getJsonFromProduct(
      Product product,
      SharedPreferences prefs,
      boolean debug,
      String TAG
  ) {
    JSONObject json = new JSONObject();
    try {
      Object name = product.name;
      Object description = product.description != null ? product.description : JSONObject.NULL;
      Object groupId = product.productGroupId != null ? product.productGroupId : JSONObject.NULL;
      Object storeId = product.storeId != null ? product.storeId : JSONObject.NULL;
      String defaultDueDays = product.defaultDueDays;
      String defaultDueDaysOpen = product.defaultDueDaysAfterOpen;
      String defaultDueDaysFreezing = product.defaultDueDaysAfterFreezing;
      String defaultDueDaysThawing = product.defaultDueDaysAfterThawing;
      Object pictureFile =
          product.pictureFileName != null ? product.pictureFileName : JSONObject.NULL;
      String enableTareWeight = product.enableTareWeightHandling;
      String tareWeight = product.tareWeight;
      String notCheckStock = product.notCheckStockFulfillmentForRecipes;
      Object parentProductId =
          product.parentProductId != null ? product.parentProductId : JSONObject.NULL;
      String calories = product.calories;
      String cumulateAmounts = product.accumulateSubProductsMinStockAmount;
      String dueType = product.dueDateType;
      String quickConsume = product.quickConsumeAmount;
      String hideOnStock = product.hideOnStockOverview;
      String defaultStockLabelType = product.defaultStockLabelType;
      String shouldNotBeFrozen = product.shouldNotBeFrozen;
      String treatOpened = product.treatOpenedAsOutOfStock;
      String noOwnStock = product.noOwnStock;
      Object defaultConsumeLocationId = product.defaultConsumeLocationId != null
          ? product.defaultConsumeLocationId : JSONObject.NULL;
      String moveOnOpen = product.moveOnOpen;

      json.put("name", name);
      json.put("description", description);
      json.put("product_group_id", groupId);
      json.put("active", product.active);
      json.put("location_id", product.locationId);
      json.put("shopping_location_id", storeId);
      json.put("qu_id_purchase", product.quIdPurchase);
      json.put("qu_id_stock", product.quIdStock);
      json.put("qu_factor_purchase_to_stock", product.quFactorPurchaseToStock);
      json.put("min_stock_amount", product.minStockAmount);
      json.put("default_best_before_days", defaultDueDays);
      json.put("default_best_before_days_after_open", defaultDueDaysOpen);
      json.put("default_best_before_days_after_freezing", defaultDueDaysFreezing);
      json.put("default_best_before_days_after_thawing", defaultDueDaysThawing);
      json.put("picture_file_name", pictureFile);
      json.put("enable_tare_weight_handling", enableTareWeight);
      json.put("tare_weight", tareWeight);
      json.put("not_check_stock_fulfillment_for_recipes", notCheckStock);
      json.put("parent_product_id", parentProductId);
      json.put("calories", calories);
      json.put("cumulate_min_stock_amount_of_sub_products", cumulateAmounts);
      json.put("due_type", dueType);
      json.put("quick_consume_amount", quickConsume);
      json.put("hide_on_stock_overview", hideOnStock);
      json.put("default_stock_label_type", defaultStockLabelType);
      json.put("should_not_be_frozen", shouldNotBeFrozen);
      if (treatOpened != null && VersionUtil.isGrocyServerMin320(prefs)) {
        json.put("treat_opened_as_out_of_stock", treatOpened);
      }
      if (noOwnStock != null && VersionUtil.isGrocyServerMin330(prefs)) {
        json.put("no_own_stock", noOwnStock);
      }
      if (VersionUtil.isGrocyServerMin330(prefs)) {
        json.put("default_consume_location_id", defaultConsumeLocationId);
      }
      if (moveOnOpen != null && VersionUtil.isGrocyServerMin331(prefs)) {
        json.put("move_on_open", moveOnOpen);
      }
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "getJsonFromProduct: " + e);
      }
    }
    return json;
  }

  public JSONObject getJsonFromProduct(SharedPreferences prefs, boolean debug, String TAG) {
    return getJsonFromProduct(this, prefs, debug, TAG);
  }

  public static Product getProductFromId(List<Product> products, int id) {
    for (Product product : products) {
      if (product.getId() == id) {
        return product;
      }
    }
    return null;
  }

  public static Product getProductFromName(List<Product> products, String name) {
    if (name == null || name.isEmpty()) return null;
    for (Product product : products) {
      if (product.getName() != null && product.getName().equals(name)) {
        return product;
      }
    }
    return null;
  }

  public static Product getProductFromBarcode(
      List<Product> products,
      List<ProductBarcode> barcodes,
      String barcode
  ) {
    for (ProductBarcode code : barcodes) {
      if (code.getBarcode().equals(barcode)) {
        return getProductFromId(products, code.getProductIdInt());
      }
    }
    return null;
  }

  public static ArrayList<Product> getProductChildren(List<Product> allProducts, int parentProductId) {
    ArrayList<Product> productChildren = new ArrayList<>();
    for (Product product : allProducts) {
      if (NumUtil.isStringInt(product.getParentProductId()) && Integer.parseInt(product.getParentProductId()) == parentProductId) {
        productChildren.add(product);
      }
    }
    return productChildren;
  }

  public static ArrayList<Product> getActiveProductsOnly(List<Product> allProducts) {
    ArrayList<Product> activeProductsOnly = new ArrayList<>();
    for (Product product : allProducts) {
      if (product.isActive()) {
        activeProductsOnly.add(product);
      }
    }
    return activeProductsOnly;
  }

  public static ArrayList<Product> getActiveAndStockEnabledProductsOnly(List<Product> allProducts) {
    ArrayList<Product> activeProductsOnly = new ArrayList<>();
    for (Product product : allProducts) {
      if (product.isActive() && !product.getNoOwnStockBoolean()) {
        activeProductsOnly.add(product);
      }
    }
    return activeProductsOnly;
  }

  public static ArrayList<Product> getProductsForRecipePositions(List<Product> products, List<RecipePosition> recipePositions) {
    ArrayList<Product> result = new ArrayList<>();
    for (RecipePosition recipePosition : recipePositions) {
      Product product = getProductFromId(products, recipePosition.getProductId());
      if (product != null)
        result.add(product);
    }
    return result;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Product product = (Product) o;
    return id == product.id && displayDivider == product.displayDivider && Objects
        .equals(name, product.name) && Objects.equals(description, product.description)
        && Objects.equals(productGroupId, product.productGroupId) && Objects
        .equals(active, product.active) && Objects.equals(locationId, product.locationId)
        && Objects.equals(storeId, product.storeId) && Objects
        .equals(quIdPurchase, product.quIdPurchase) && Objects
        .equals(quIdStock, product.quIdStock) && Objects
        .equals(quFactorPurchaseToStock, product.quFactorPurchaseToStock) && Objects
        .equals(minStockAmount, product.minStockAmount) && Objects
        .equals(defaultDueDays, product.defaultDueDays) && Objects
        .equals(defaultDueDaysAfterOpen, product.defaultDueDaysAfterOpen) && Objects
        .equals(defaultDueDaysAfterFreezing, product.defaultDueDaysAfterFreezing) && Objects
        .equals(defaultDueDaysAfterThawing, product.defaultDueDaysAfterThawing) && Objects
        .equals(pictureFileName, product.pictureFileName) && Objects
        .equals(enableTareWeightHandling, product.enableTareWeightHandling) && Objects
        .equals(tareWeight, product.tareWeight) && Objects
        .equals(notCheckStockFulfillmentForRecipes, product.notCheckStockFulfillmentForRecipes)
        && Objects.equals(parentProductId, product.parentProductId) && Objects
        .equals(calories, product.calories) && Objects
        .equals(accumulateSubProductsMinStockAmount, product.accumulateSubProductsMinStockAmount)
        && Objects.equals(treatOpenedAsOutOfStock, product.treatOpenedAsOutOfStock)
        && Objects.equals(dueDateType, product.dueDateType) && Objects
        .equals(quickConsumeAmount, product.quickConsumeAmount) && Objects
        .equals(hideOnStockOverview, product.hideOnStockOverview) && Objects
        .equals(noOwnStock, product.noOwnStock) && Objects
        .equals(defaultConsumeLocationId, product.defaultConsumeLocationId) && Objects
        .equals(moveOnOpen, product.moveOnOpen) && Objects
        .equals(pendingProductId, product.pendingProductId);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(id, name, description, productGroupId, active, locationId, storeId, quIdPurchase,
            quIdStock, quFactorPurchaseToStock, minStockAmount, defaultDueDays,
            defaultDueDaysAfterOpen, defaultDueDaysAfterFreezing, defaultDueDaysAfterThawing,
            pictureFileName, enableTareWeightHandling, tareWeight,
            notCheckStockFulfillmentForRecipes,
            parentProductId, calories, accumulateSubProductsMinStockAmount, treatOpenedAsOutOfStock,
            dueDateType, quickConsumeAmount, hideOnStockOverview, noOwnStock,
            defaultConsumeLocationId, moveOnOpen, pendingProductId, displayDivider);
  }

  @NonNull
  @Override
  public String toString() {
    return name;
  }

  public static QueueItem updateProducts(
      DownloadHelper dlHelper,
      String dbChangedTime,
      OnObjectsResponseListener<Product> onResponseListener
  ) {
    return updateProducts(dlHelper, dbChangedTime, onResponseListener, false);
  }

  @SuppressLint("CheckResult")
  public static QueueItem updateProducts(
      DownloadHelper dlHelper,
      String dbChangedTime,
      OnObjectsResponseListener<Product> onResponseListener,
      boolean alsoRespondIfNotUpdated
  ) {
    String lastTime = dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        Constants.PREF.DB_LAST_TIME_PRODUCTS, null
    );
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnMultiTypeErrorListener errorListener,
            @Nullable String uuid
        ) {
          dlHelper.get(
              dlHelper.grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS),
              uuid,
              response -> {
                Type type = new TypeToken<List<Product>>() {
                }.getType();
                ArrayList<Product> products = dlHelper.gson.fromJson(response, type);
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "download Products: " + products);
                }
                Single.fromCallable(() -> {
                  dlHelper.appDatabase.productDao()
                      .deleteProducts().blockingSubscribe();
                  dlHelper.appDatabase.productDao()
                      .insertProducts(products).blockingSubscribe();
                  dlHelper.sharedPrefs.edit()
                      .putString(PREF.DB_LAST_TIME_PRODUCTS, dbChangedTime).apply();
                  return true;
                })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(products);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    })
                    .subscribe(ignored -> {}, throwable -> {
                      if (errorListener != null) {
                        errorListener.onError(throwable);
                      }
                    });
              },
              error -> {
                if (errorListener != null) {
                  errorListener.onError(error);
                }
              }
          );
        }
      };
    } else {
      if (dlHelper.debug) {
        Log.i(dlHelper.tag, "downloadData: skipped Products download");
      }
      if (alsoRespondIfNotUpdated) {
        return new QueueItem() {
          @Override
          public void perform(
              @Nullable OnStringResponseListener responseListener,
              @Nullable OnMultiTypeErrorListener errorListener,
              @Nullable String uuid
          ) {
            dlHelper.appDatabase.productDao().getProducts()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(products -> {
                  if (onResponseListener != null) {
                    onResponseListener.onResponse(products);
                  }
                  if (responseListener != null) {
                    responseListener.onResponse(null);
                  }
                })
                .doOnError(throwable -> {
                  if (errorListener != null) {
                    errorListener.onError(throwable);
                  }
                })
                .onErrorComplete()
                .subscribe();
          }
        };
      } else {
        return null;
      }
    }
  }
}
