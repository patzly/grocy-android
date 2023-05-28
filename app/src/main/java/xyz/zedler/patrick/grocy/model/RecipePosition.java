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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.QueueItem;

@Entity(tableName = "recipe_pos_table")
public class RecipePosition implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "id")
  @SerializedName("id")
  private int id;

  @ColumnInfo(name = "recipe_id")
  @SerializedName("recipe_id")
  private int recipeId;

  @ColumnInfo(name = "product_id")
  @SerializedName("product_id")
  private int productId;

  @ColumnInfo(name = "amount")
  @SerializedName("amount")
  private double amount;

  @ColumnInfo(name = "note")
  @SerializedName("note")
  private String note;

  @ColumnInfo(name = "qu_id")
  @SerializedName("qu_id")
  private int quantityUnitId;

  @ColumnInfo(name = "only_check_single_unit_in_stock")
  @SerializedName("only_check_single_unit_in_stock")
  private int onlyCheckSingleUnitInStock;

  @ColumnInfo(name = "ingredient_group")
  @SerializedName("ingredient_group")
  private String ingredientGroup;

  @ColumnInfo(name = "not_check_stock_fulfillment")
  @SerializedName("not_check_stock_fulfillment")
  private int notCheckStockFulfillment;

  @ColumnInfo(name = "variable_amount")
  @SerializedName("variable_amount")
  private String variableAmount;

  @ColumnInfo(name = "price_factor")
  @SerializedName("price_factor")
  private double priceFactor;

  @Ignore
  private boolean checked;

  public RecipePosition() {
  }  // for Room

  @Ignore
  public RecipePosition(Parcel parcel) {
    id = parcel.readInt();
    recipeId = parcel.readInt();
    productId = parcel.readInt();
    amount = parcel.readDouble();
    note = parcel.readString();
    quantityUnitId = parcel.readInt();
    onlyCheckSingleUnitInStock = parcel.readInt();
    ingredientGroup = parcel.readString();
    notCheckStockFulfillment = parcel.readInt();
    variableAmount = parcel.readString();
    priceFactor = parcel.readDouble();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(id);
    dest.writeInt(recipeId);
    dest.writeInt(productId);
    dest.writeDouble(amount);
    dest.writeString(note);
    dest.writeInt(quantityUnitId);
    dest.writeInt(onlyCheckSingleUnitInStock);
    dest.writeString(ingredientGroup);
    dest.writeInt(notCheckStockFulfillment);
    dest.writeString(variableAmount);
    dest.writeDouble(priceFactor);
  }

  public static final Creator<RecipePosition> CREATOR = new Creator<>() {

    @Override
    public RecipePosition createFromParcel(Parcel in) {
      return new RecipePosition(in);
    }

    @Override
    public RecipePosition[] newArray(int size) {
      return new RecipePosition[size];
    }
  };

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getRecipeId() {
    return recipeId;
  }

  public void setRecipeId(int recipeId) {
    this.recipeId = recipeId;
  }

  public int getProductId() {
    return productId;
  }

  public void setProductId(int productId) {
    this.productId = productId;
  }

  public double getAmount() {
    return amount;
  }

  public void setAmount(double amount) {
    this.amount = amount;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public int getQuantityUnitId() {
    return quantityUnitId;
  }

  public void setQuantityUnitId(int quantityUnitId) {
    this.quantityUnitId = quantityUnitId;
  }

  public int getOnlyCheckSingleUnitInStock() {
    return onlyCheckSingleUnitInStock;
  }

  public boolean isOnlyCheckSingleUnitInStock() {
    return onlyCheckSingleUnitInStock == 1;
  }

  public void setOnlyCheckSingleUnitInStock(int onlyCheckSingleUnitInStock) {
    this.onlyCheckSingleUnitInStock = onlyCheckSingleUnitInStock;
  }

  public void setOnlyCheckSingleUnitInStock(boolean onlyCheckSingleUnitInStock) {
    this.onlyCheckSingleUnitInStock = onlyCheckSingleUnitInStock ? 1 : 0;
  }

  public String getIngredientGroup() {
    return ingredientGroup;
  }

  public void setIngredientGroup(String ingredientGroup) {
    this.ingredientGroup = ingredientGroup;
  }

  public int getNotCheckStockFulfillment() {
    return notCheckStockFulfillment;
  }

  public boolean isNotCheckStockFulfillment() {
    return notCheckStockFulfillment == 1;
  }

  public void setNotCheckStockFulfillment(int notCheckStockFulfillment) {
    this.notCheckStockFulfillment = notCheckStockFulfillment;
  }

  public void setNotCheckStockFulfillment(boolean notCheckStockFulfillment) {
    this.notCheckStockFulfillment = notCheckStockFulfillment ? 1 : 0;
  }

  public String getVariableAmount() {
    return variableAmount;
  }

  public void setVariableAmount(String variableAmount) {
    this.variableAmount = variableAmount;
  }

  public double getPriceFactor() {
    return priceFactor;
  }

  public void setPriceFactor(double priceFactor) {
    this.priceFactor = priceFactor;
  }

  public boolean isChecked() {
    return checked;
  }

  public void setChecked(boolean checked) {
    this.checked = checked;
  }

  public void toggleChecked() {
    checked = !checked;
  }

  public static JSONObject getJsonFromRecipe(RecipePosition recipe, boolean debug, String TAG) {
    JSONObject json = new JSONObject();
    try {
      Object recipeId = recipe.recipeId;
      Object productId = recipe.productId;
      Object amount = recipe.amount;
      Object note = recipe.note;
      Object quantityUnitId = recipe.quantityUnitId;
      Object onlyCheckSingleUnitInStock = recipe.onlyCheckSingleUnitInStock;
      Object ingredientGroup = recipe.ingredientGroup;
      Object notCheckStockFulfillment = recipe.notCheckStockFulfillment;
      Object variableAmount = recipe.variableAmount;
      Object priceFactor = recipe.priceFactor;

      json.put("recipe_id", recipeId);
      json.put("product_id", productId);
      json.put("amount", amount);
      json.put("note", note);
      json.put("qu_id", quantityUnitId);
      json.put("only_check_single_unit_in_stock", onlyCheckSingleUnitInStock);
      json.put("ingredient_group", ingredientGroup);
      json.put("not_check_stock_fulfillment", notCheckStockFulfillment);
      json.put("variable_amount", variableAmount);
      json.put("price_factor", priceFactor);
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "getJsonFromRecipePos: " + e);
      }
    }
    return json;
  }

  public static List<RecipePosition> getRecipePositionsFromRecipeId(List<RecipePosition> recipePositions, int recipeId) {
    List<RecipePosition> result = new ArrayList<>();
    for (RecipePosition recipePosition : recipePositions) {
      if (recipePosition.getRecipeId() == recipeId) {
        result.add(recipePosition);
      }
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
    RecipePosition recipePosition = (RecipePosition) o;
    return Objects.equals(id, recipePosition.id) &&
        Objects.equals(recipeId, recipePosition.recipeId) &&
        Objects.equals(productId, recipePosition.productId) &&
        Objects.equals(amount, recipePosition.amount) &&
        Objects.equals(note, recipePosition.note) &&
        Objects.equals(quantityUnitId, recipePosition.quantityUnitId) &&
        Objects.equals(onlyCheckSingleUnitInStock, recipePosition.onlyCheckSingleUnitInStock) &&
        Objects.equals(ingredientGroup, recipePosition.ingredientGroup) &&
        Objects.equals(notCheckStockFulfillment, recipePosition.notCheckStockFulfillment) &&
        Objects.equals(variableAmount, recipePosition.variableAmount) &&
        Objects.equals(priceFactor, recipePosition.priceFactor);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(id, recipeId, productId, amount, note, quantityUnitId, onlyCheckSingleUnitInStock, ingredientGroup, notCheckStockFulfillment, variableAmount, priceFactor);
  }

  @NonNull
  @Override
  public String toString() {
    return "RecipePosition(" + id + ")";
  }

  public static QueueItem updateRecipePositions(
      DownloadHelper dlHelper,
      String dbChangedTime,
      OnObjectsResponseListener<RecipePosition> onResponseListener
  ) {
    String lastTime = dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        PREF.DB_LAST_TIME_RECIPE_POSITIONS, null
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
              dlHelper.grocyApi.getRecipePositions(),
              uuid,
              response -> {
                Type type = new TypeToken<List<RecipePosition>>() {
                }.getType();
                ArrayList<RecipePosition> recipePositions = dlHelper.gson.fromJson(response, type);
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "download RecipePositions: " + recipePositions);
                }
                // fix crash, amount can be NaN according to a user
                for (int i = 0; i < recipePositions.size(); i++) {
                  RecipePosition recipePos = recipePositions.get(i);
                  if (Double.isNaN(recipePos.getAmount())) {
                    recipePos.setAmount(0);
                  }
                }
                Single.fromCallable(() -> {
                  dlHelper.appDatabase.recipePositionDao()
                      .deleteRecipePositions().blockingSubscribe();
                  dlHelper.appDatabase.recipePositionDao()
                      .insertRecipePositions(recipePositions).blockingSubscribe();
                  dlHelper.sharedPrefs.edit()
                      .putString(PREF.DB_LAST_TIME_RECIPE_POSITIONS, dbChangedTime).apply();
                  return true;
                })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError(throwable -> {
                      if (errorListener != null) {
                        errorListener.onError(throwable);
                      }
                    })
                    .doFinally(() -> {
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(recipePositions);
                      }
                      if (responseListener != null) {
                        responseListener.onResponse(response);
                      }
                    }).subscribe();
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
        Log.i(dlHelper.tag, "downloadData: skipped Recipe positions download");
      }
      return null;
    }
  }
}
