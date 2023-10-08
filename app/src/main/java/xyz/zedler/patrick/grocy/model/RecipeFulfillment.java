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
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.web.NetworkQueue.QueueItem;

@Entity(tableName = "recipe_fulfillment_table")
public class RecipeFulfillment implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "recipe_id")
  @SerializedName("recipe_id")
  private int recipeId;

  @ColumnInfo(name = "need_fulfilled")
  @SerializedName("need_fulfilled")
  private int needFulfilled;

  @ColumnInfo(name = "need_fulfilled_with_shopping_list")
  @SerializedName("need_fulfilled_with_shopping_list")
  private int needFulfilledWithShoppingList;

  @ColumnInfo(name = "missing_products_count")
  @SerializedName("missing_products_count")
  private int missingProductsCount;

  @ColumnInfo(name = "costs")
  @SerializedName("costs")
  private double costs;

  @ColumnInfo(name = "costs_per_serving")
  @SerializedName("costs_per_serving")
  private double costsPerServing;

  @ColumnInfo(name = "calories")
  @SerializedName("calories")
  private double calories;

  @ColumnInfo(name = "due_score")
  @SerializedName("due_score")
  private int dueScore;

  @ColumnInfo(name = "product_names_comma_separated")
  @SerializedName("product_names_comma_separated")
  private String productNamesCommaSeparated;

  public RecipeFulfillment() {
  }  // for Room

  @Ignore
  public RecipeFulfillment(Parcel parcel) {
    recipeId = parcel.readInt();
    needFulfilled = parcel.readInt();
    needFulfilledWithShoppingList = parcel.readInt();
    missingProductsCount = parcel.readInt();
    costs = parcel.readDouble();
    costsPerServing = parcel.readDouble();
    calories = parcel.readDouble();
    dueScore = parcel.readInt();
    productNamesCommaSeparated = parcel.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(recipeId);
    dest.writeInt(needFulfilled);
    dest.writeInt(needFulfilledWithShoppingList);
    dest.writeInt(missingProductsCount);
    dest.writeDouble(costs);
    dest.writeDouble(costsPerServing);
    dest.writeDouble(calories);
    dest.writeInt(dueScore);
    dest.writeString(productNamesCommaSeparated);
  }

  public static final Creator<RecipeFulfillment> CREATOR = new Creator<>() {

    @Override
    public RecipeFulfillment createFromParcel(Parcel in) {
      return new RecipeFulfillment(in);
    }

    @Override
    public RecipeFulfillment[] newArray(int size) {
      return new RecipeFulfillment[size];
    }
  };

  public int getRecipeId() {
    return recipeId;
  }

  public void setRecipeId(int recipeId) {
    this.recipeId = recipeId;
  }

  public int getNeedFulfilled() {
    return needFulfilled;
  }

  public boolean isNeedFulfilled() {
    return needFulfilled == 1;
  }

  public void setNeedFulfilled(int needFulfilled) {
    this.needFulfilled = needFulfilled;
  }

  public void setNeedFulfilled(boolean needFulfilled) {
    this.needFulfilled = needFulfilled ? 1 : 0;
  }

  public int getNeedFulfilledWithShoppingList() {
    return needFulfilledWithShoppingList;
  }

  public boolean isNeedFulfilledWithShoppingList() {
    return needFulfilledWithShoppingList == 1;
  }

  public void setNeedFulfilledWithShoppingList(int needFulfilledWithShoppingList) {
    this.needFulfilledWithShoppingList = needFulfilledWithShoppingList;
  }

  public void setNeedFulfilledWithShoppingList(boolean needFulfilledWithShoppingList) {
    this.needFulfilledWithShoppingList = needFulfilledWithShoppingList ? 1 : 0;
  }

  public int getMissingProductsCount() {
    return missingProductsCount;
  }

  public void setMissingProductsCount(int missingProductsCount) {
    this.missingProductsCount = missingProductsCount;
  }

  public double getCosts() {
    return costs;
  }

  public void setCosts(double costs) {
    this.costs = costs;
  }

  public double getCostsPerServing() {
    return costsPerServing;
  }

  public void setCostsPerServing(double costsPerServing) {
    this.costsPerServing = costsPerServing;
  }

  public double getCalories() {
    return calories;
  }

  public void setCalories(double calories) {
    this.calories = calories;
  }

  public int getDueScore() {
    return dueScore;
  }

  public void setDueScore(int dueScore) {
    this.dueScore = dueScore;
  }

  public String getProductNamesCommaSeparated() {
    return productNamesCommaSeparated;
  }

  public void setProductNamesCommaSeparated(String productNamesCommaSeparated) {
    this.productNamesCommaSeparated = productNamesCommaSeparated;
  }

  public static JSONObject getJsonFromRecipe(RecipeFulfillment recipeFulfillment, boolean debug, String TAG) {
    JSONObject json = new JSONObject();
    try {
      Object recipeId = recipeFulfillment.recipeId;
      Object needFulfilled = recipeFulfillment.needFulfilled;
      Object needFulfilledWithShoppingList = recipeFulfillment.needFulfilledWithShoppingList;
      Object missingProductsCount = recipeFulfillment.missingProductsCount;
      Object costs = recipeFulfillment.costs;
      Object costsPerServing = recipeFulfillment.costsPerServing;
      Object calories = recipeFulfillment.calories;
      Object dueScore = recipeFulfillment.dueScore;
      Object productNamesCommaSeparated = recipeFulfillment.productNamesCommaSeparated;

      json.put("recipe_id", recipeId);
      json.put("need_fulfilled", needFulfilled);
      json.put("need_fulfilled_with_shopping_list", needFulfilledWithShoppingList);
      json.put("missing_products_count", missingProductsCount);
      json.put("costs", costs);
      json.put("costs_per_serving", costsPerServing);
      json.put("calories", calories);
      json.put("due_score", dueScore);
      json.put("product_names_comma_separated", productNamesCommaSeparated);
    } catch (JSONException e) {
      if (debug) {
        Log.e(TAG, "getJsonFromRecipe: " + e);
      }
    }
    return json;
  }

  public static RecipeFulfillment getRecipeFulfillmentFromRecipeId(List<RecipeFulfillment> recipeFulfillments, int recipeId) {
    for (RecipeFulfillment recipeFulfillment : recipeFulfillments) {
      if (recipeFulfillment.getRecipeId() == recipeId) {
        return recipeFulfillment;
      }
    }
    return null;
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
    RecipeFulfillment recipe = (RecipeFulfillment) o;
    return Objects.equals(recipeId, recipe.recipeId) &&
        Objects.equals(needFulfilled, recipe.needFulfilled) &&
        Objects.equals(needFulfilledWithShoppingList, recipe.needFulfilledWithShoppingList) &&
        Objects.equals(missingProductsCount, recipe.missingProductsCount) &&
        Objects.equals(costs, recipe.costs) &&
        Objects.equals(costsPerServing, recipe.costsPerServing) &&
        Objects.equals(calories, recipe.calories) &&
        Objects.equals(dueScore, recipe.dueScore) &&
        Objects.equals(productNamesCommaSeparated, recipe.productNamesCommaSeparated);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(recipeId, needFulfilled, needFulfilledWithShoppingList, missingProductsCount, costs, costsPerServing, calories, dueScore, productNamesCommaSeparated);
  }

  @NonNull
  @Override
  public String toString() {
    return "RecipeFulfillment(" + recipeId + ")";
  }

  @SuppressLint("CheckResult")
  public static QueueItem updateRecipeFulfillments(
      DownloadHelper dlHelper,
      String dbChangedTime,
      boolean forceUpdate,
      OnObjectsResponseListener<RecipeFulfillment> onResponseListener
  ) {
    String lastTime = !forceUpdate ? dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        PREF.DB_LAST_TIME_RECIPE_FULFILLMENTS, null
    ) : null;
    if (lastTime == null || !lastTime.equals(dbChangedTime)) {
      return new QueueItem() {
        @Override
        public void perform(
            @Nullable OnStringResponseListener responseListener,
            @Nullable OnMultiTypeErrorListener errorListener,
            @Nullable String uuid
        ) {
          dlHelper.get(
              dlHelper.grocyApi.getRecipeFulfillments(),
              uuid,
              response -> {
                Type type = new TypeToken<List<RecipeFulfillment>>() {
                }.getType();
                ArrayList<RecipeFulfillment> recipeFulfillments = dlHelper.gson.fromJson(response, type);
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "download RecipeFulfillments: " + recipeFulfillments);
                }
                Single.fromCallable(() -> {
                  dlHelper.appDatabase.recipeFulfillmentDao()
                      .deleteRecipeFulfillments().blockingSubscribe();
                  dlHelper.appDatabase.recipeFulfillmentDao()
                      .insertRecipeFulfillments(recipeFulfillments).blockingSubscribe();
                  dlHelper.sharedPrefs.edit()
                      .putString(PREF.DB_LAST_TIME_RECIPE_FULFILLMENTS, dbChangedTime).apply();
                  return true;
                })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(recipeFulfillments);
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
        Log.i(dlHelper.tag, "downloadData: skipped Recipe fulfillments download");
      }
      return null;
    }
  }
}
