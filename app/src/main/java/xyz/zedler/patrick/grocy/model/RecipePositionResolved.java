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
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnMultiTypeErrorListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnObjectsResponseListener;
import xyz.zedler.patrick.grocy.helper.DownloadHelper.OnStringResponseListener;
import xyz.zedler.patrick.grocy.web.NetworkQueue.QueueItem;

@Entity(tableName = "recipe_pos_resolved_table")
public class RecipePositionResolved {

  @PrimaryKey
  @ColumnInfo(name = "id")
  private int id;

  @ColumnInfo(name = "recipe_id")
  private int recipeId;

  @ColumnInfo(name = "recipe_pos_id")
  private int recipePosId;

  @ColumnInfo(name = "product_id")
  private int productId;

  @ColumnInfo(name = "recipe_amount")
  private double recipeAmount;

  @ColumnInfo(name = "stock_amount")
  private double stockAmount;

  @ColumnInfo(name = "need_fulfilled")
  private int needFulfilled;

  @ColumnInfo(name = "missing_amount")
  private double missingAmount;

  @ColumnInfo(name = "amount_on_shopping_list")
  private double amountOnShoppingList;

  @ColumnInfo(name = "need_fulfilled_with_shopping_list")
  @SerializedName("need_fulfilled_with_shopping_list")
  private int needFulfilledWithShoppingList;

  @ColumnInfo(name = "qu_id")
  private int quId;

  @ColumnInfo(name = "costs")
  private double costs;

  @ColumnInfo(name = "is_nested_recipe_pos")
  private int isNestedRecipePos;

  @ColumnInfo(name = "ingredient_group")
  private String ingredientGroup;

  @ColumnInfo(name = "product_group")
  private String productGroup;

  @ColumnInfo(name = "recipe_type")
  private String recipeType;

  @ColumnInfo(name = "child_recipe_id")
  private int childRecipeId;

  @ColumnInfo(name = "note")
  private String note;

  @ColumnInfo(name = "recipe_variable_amount")
  private String recipeVariableAmount;

  @ColumnInfo(name = "only_check_single_unit_in_stock")
  private int onlyCheckSingleUnitInStock;

  @ColumnInfo(name = "calories")
  private int calories;

  @ColumnInfo(name = "product_active")
  private int productActive;

  @ColumnInfo(name = "due_score")
  private int dueScore;

  @ColumnInfo(name = "product_id_effective")
  private int productIdEffective;

  @ColumnInfo(name = "product_name")
  private String productName;

  public RecipePositionResolved() {
  }  // for Room

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

  public int getRecipePosId() {
    return recipePosId;
  }

  public void setRecipePosId(int recipePosId) {
    this.recipePosId = recipePosId;
  }

  public int getProductId() {
    return productId;
  }

  public void setProductId(int productId) {
    this.productId = productId;
  }

  public double getRecipeAmount() {
    return recipeAmount;
  }

  public void setRecipeAmount(double recipeAmount) {
    this.recipeAmount = recipeAmount;
  }

  public double getStockAmount() {
    return stockAmount;
  }

  public void setStockAmount(double stockAmount) {
    this.stockAmount = stockAmount;
  }

  public int getNeedFulfilled() {
    return needFulfilled;
  }

  public void setNeedFulfilled(int needFulfilled) {
    this.needFulfilled = needFulfilled;
  }

  public double getMissingAmount() {
    return missingAmount;
  }

  public void setMissingAmount(double missingAmount) {
    this.missingAmount = missingAmount;
  }

  public double getAmountOnShoppingList() {
    return amountOnShoppingList;
  }

  public void setAmountOnShoppingList(double amountOnShoppingList) {
    this.amountOnShoppingList = amountOnShoppingList;
  }

  public int getNeedFulfilledWithShoppingList() {
    return needFulfilledWithShoppingList;
  }

  public void setNeedFulfilledWithShoppingList(int needFulfilledWithShoppingList) {
    this.needFulfilledWithShoppingList = needFulfilledWithShoppingList;
  }

  public int getQuId() {
    return quId;
  }

  public void setQuId(int quId) {
    this.quId = quId;
  }

  public double getCosts() {
    return costs;
  }

  public void setCosts(double costs) {
    this.costs = costs;
  }

  public int getIsNestedRecipePos() {
    return isNestedRecipePos;
  }

  public void setIsNestedRecipePos(int isNestedRecipePos) {
    this.isNestedRecipePos = isNestedRecipePos;
  }

  public String getIngredientGroup() {
    return ingredientGroup;
  }

  public void setIngredientGroup(String ingredientGroup) {
    this.ingredientGroup = ingredientGroup;
  }

  public String getProductGroup() {
    return productGroup;
  }

  public void setProductGroup(String productGroup) {
    this.productGroup = productGroup;
  }

  public String getRecipeType() {
    return recipeType;
  }

  public void setRecipeType(String recipeType) {
    this.recipeType = recipeType;
  }

  public int getChildRecipeId() {
    return childRecipeId;
  }

  public void setChildRecipeId(int childRecipeId) {
    this.childRecipeId = childRecipeId;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public String getRecipeVariableAmount() {
    return recipeVariableAmount;
  }

  public void setRecipeVariableAmount(String recipeVariableAmount) {
    this.recipeVariableAmount = recipeVariableAmount;
  }

  public int getOnlyCheckSingleUnitInStock() {
    return onlyCheckSingleUnitInStock;
  }

  public void setOnlyCheckSingleUnitInStock(int onlyCheckSingleUnitInStock) {
    this.onlyCheckSingleUnitInStock = onlyCheckSingleUnitInStock;
  }

  public int getCalories() {
    return calories;
  }

  public void setCalories(int calories) {
    this.calories = calories;
  }

  public int getProductActive() {
    return productActive;
  }

  public void setProductActive(int productActive) {
    this.productActive = productActive;
  }

  public int getDueScore() {
    return dueScore;
  }

  public void setDueScore(int dueScore) {
    this.dueScore = dueScore;
  }

  public int getProductIdEffective() {
    return productIdEffective;
  }

  public void setProductIdEffective(int productIdEffective) {
    this.productIdEffective = productIdEffective;
  }

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  @NonNull
  @Override
  public String toString() {
    return "RecipePositionResolved(" + id + ")";
  }

  @SuppressLint("CheckResult")
  public static QueueItem updateRecipePositionsResolved(
      DownloadHelper dlHelper,
      String dbChangedTime,
      boolean forceUpdate,
      OnObjectsResponseListener<RecipePositionResolved> onResponseListener
  ) {
    String lastTime = !forceUpdate ? dlHelper.sharedPrefs.getString(  // get last offline db-changed-time value
        PREF.DB_LAST_TIME_RECIPE_POSITIONS_RESOLVED, null
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
              dlHelper.grocyApi.getRecipePositionsResolved(),
              uuid,
              response -> {
                Type type = new TypeToken<List<RecipePositionResolved>>() {
                }.getType();
                ArrayList<RecipePositionResolved> recipePositionsResolved = dlHelper.gson
                    .fromJson(response, type);
                if (dlHelper.debug) {
                  Log.i(dlHelper.tag, "download RecipePositionResolved: "
                      + recipePositionsResolved);
                }
                // fix crash, amount can be NaN according to a user
                for (int i = 0; i < recipePositionsResolved.size(); i++) {
                  RecipePositionResolved recipePos = recipePositionsResolved.get(i);
                  if (Double.isNaN(recipePos.getRecipeAmount())) {
                    recipePos.setRecipeAmount(0);
                  }
                  if (Double.isNaN(recipePos.getStockAmount())) {
                    recipePos.setStockAmount(0);
                  }
                }
                Single.fromCallable(() -> {
                      dlHelper.appDatabase.recipePositionDao()
                          .deleteRecipePositions().blockingSubscribe();
                      dlHelper.appDatabase.recipePositionResolvedDao()
                          .insertRecipePositionsResolved(recipePositionsResolved)
                          .blockingSubscribe();
                      dlHelper.sharedPrefs.edit()
                          .putString(PREF.DB_LAST_TIME_RECIPE_POSITIONS_RESOLVED, dbChangedTime)
                          .apply();
                      return true;
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> {
                      if (onResponseListener != null) {
                        onResponseListener.onResponse(recipePositionsResolved);
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
        Log.i(dlHelper.tag, "downloadData: skipped RecipePositionResolved download");
      }
      return null;
    }
  }
}
