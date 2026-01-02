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
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.repository;

import android.app.Application;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import xyz.zedler.patrick.grocy.database.AppDatabase;
import xyz.zedler.patrick.grocy.model.MealPlanEntry;
import xyz.zedler.patrick.grocy.model.MealPlanSection;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductLastPurchased;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.Userfield;
import xyz.zedler.patrick.grocy.util.RxJavaUtil;

public class MealPlanRepository {

  private final AppDatabase appDatabase;

  public MealPlanRepository(Application application) {
    this.appDatabase = AppDatabase.getAppDatabase(application);
  }

  public interface MealPlanDataListener {

    void actionFinished(MealPlanData data);
  }

  public static class MealPlanData {

    private final List<Recipe> recipes;
    private final List<RecipeFulfillment> recipeFulfillments;
    private final List<RecipePosition> recipePositions;
    private final List<Product> products;
    private final List<QuantityUnit> quantityUnits;
    private final List<ProductLastPurchased> productsLastPurchased;
    private final List<MealPlanEntry> mealPlanEntries;
    private final List<MealPlanSection> mealPlanSections;
    private final List<StockItem> stockItems;
    private final List<Userfield> userfields;

    public MealPlanData(
        List<Recipe> recipes,
        List<RecipeFulfillment> recipeFulfillments,
        List<RecipePosition> recipePositions,
        List<Product> products,
        List<QuantityUnit> quantityUnits,
        List<ProductLastPurchased> productsLastPurchased,
        List<MealPlanEntry> mealPlanEntries,
        List<MealPlanSection> mealPlanSections,
        List<StockItem> stockItems,
        List<Userfield> userfields
    ) {
      this.recipes = recipes;
      this.recipeFulfillments = recipeFulfillments;
      this.recipePositions = recipePositions;
      this.products = products;
      this.quantityUnits = quantityUnits;
      this.productsLastPurchased = productsLastPurchased;
      this.mealPlanEntries = mealPlanEntries;
      this.mealPlanSections = mealPlanSections;
      this.stockItems = stockItems;
      this.userfields = userfields;
    }

    public List<Recipe> getRecipes() {
      return recipes;
    }

    public List<RecipeFulfillment> getRecipeFulfillments() {
      return recipeFulfillments;
    }

    public List<RecipePosition> getRecipePositions() {
      return recipePositions;
    }

    public List<Product> getProducts() {
      return products;
    }

    public List<QuantityUnit> getQuantityUnits() {
      return quantityUnits;
    }

    public List<ProductLastPurchased> getProductsLastPurchased() {
      return productsLastPurchased;
    }

    public List<MealPlanEntry> getMealPlanEntries() {
      return mealPlanEntries;
    }

    public List<MealPlanSection> getMealPlanSections() {
      return mealPlanSections;
    }

    public List<StockItem> getStockItems() {
      return stockItems;
    }

    public List<Userfield> getUserfields() {
      return userfields;
    }
  }

  public void loadFromDatabase(MealPlanDataListener onSuccess, Consumer<Throwable> onError) {
    RxJavaUtil
        .zip(
            appDatabase.recipeDao().getRecipes(),
            appDatabase.recipeFulfillmentDao().getRecipeFulfillments(),
            appDatabase.recipePositionDao().getRecipePositions(),
            appDatabase.productDao().getProducts(),
            appDatabase.quantityUnitDao().getQuantityUnits(),
            appDatabase.productLastPurchasedDao().getProductsLastPurchased(),
            appDatabase.mealPlanEntryDao().getMealPlanEntries(),
            appDatabase.mealPlanSectionDao().getMealPlanSections(),
            appDatabase.stockItemDao().getStockItems(),
            appDatabase.userfieldDao().getUserfields(),
            MealPlanData::new
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(onSuccess::actionFinished)
        .doOnError(onError)
        .onErrorComplete()
        .subscribe();
  }
}
