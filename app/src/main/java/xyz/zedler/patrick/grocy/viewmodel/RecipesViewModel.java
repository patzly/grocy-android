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
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.FilterChipLiveData;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataRecipesExtraField;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataRecipesSort;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataRecipesStatus;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.repository.RecipesRepository;
import xyz.zedler.patrick.grocy.util.Constants.PREF;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;

public class RecipesViewModel extends BaseViewModel {

  private final static String TAG = RecipesViewModel.class.getSimpleName();
  public final static String SORT_NAME = "sort_name";
  public final static String SORT_CALORIES = "sort_calories";
  public final static String SORT_DUE_SCORE = "sort_due_score";

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final RecipesRepository repository;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;
  private final MutableLiveData<ArrayList<Recipe>> filteredRecipesLive;
  private final FilterChipLiveDataRecipesStatus filterChipLiveDataStatus;
  private final FilterChipLiveDataRecipesSort filterChipLiveDataSort;
  private final FilterChipLiveDataRecipesExtraField filterChipLiveDataExtraField;

  private List<Recipe> recipes;
  private List<RecipeFulfillment> recipeFulfillments;
  private List<RecipePosition> recipePositions;
  private List<Product> products;
  private List<QuantityUnit> quantityUnits;

  private String searchInput;
  private final boolean debug;

  public RecipesViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    repository = new RecipesRepository(application);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
    filteredRecipesLive = new MutableLiveData<>();

    filterChipLiveDataStatus = new FilterChipLiveDataRecipesStatus(
        getApplication(),
        this::updateFilteredRecipes
    );
    filterChipLiveDataSort = new FilterChipLiveDataRecipesSort(
        getApplication(),
        this::updateFilteredRecipes
    );
    filterChipLiveDataExtraField = new FilterChipLiveDataRecipesExtraField(
        getApplication(),
        this::updateFilteredRecipes
    );
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      recipes = data.getRecipes();
      recipeFulfillments = data.getRecipeFulfillments();
      recipePositions = data.getRecipePositions();
      products = data.getProducts();
      quantityUnits = data.getQuantityUnits();

      updateFilteredRecipes();
      if (downloadAfterLoading) {
        downloadData();
      }
    });
  }

  public void downloadData(@Nullable String dbChangedTime) {
    if (isOffline()) { // skip downloading and update recyclerview
      isLoadingLive.setValue(false);
      updateFilteredRecipes();
      return;
    }

    dlHelper.updateData(
        () -> loadFromDatabase(false),
        this::onDownloadError,
        Recipe.class,
        RecipeFulfillment.class,
        RecipePosition.class,
        Product.class,
        QuantityUnit.class
    );
  }

  public void downloadData() {
    downloadData(null);
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.putString(PREF.DB_LAST_TIME_RECIPES, null);
    editPrefs.putString(PREF.DB_LAST_TIME_RECIPE_FULFILLMENTS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_RECIPE_POSITIONS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.apply();
    downloadData();
  }

  private void onDownloadError(@Nullable VolleyError error) {
    if (debug) {
      Log.e(TAG, "onError: VolleyError: " + error);
    }
    showMessage(getString(R.string.msg_no_connection));
    if (!isOffline()) {
      setOfflineLive(true);
    }
  }

  public void updateFilteredRecipes() {
    ArrayList<Recipe> filteredRecipes = new ArrayList<>();

    int enoughInStockCount = 0;
    int notEnoughInStockButInShoppingListCount = 0;
    int notEnoughInStockCount = 0;

    for (Recipe recipe : this.recipes) {
      RecipeFulfillment recipeFulfillment = RecipeFulfillment.getRecipeFulfillmentFromRecipeId(recipeFulfillments, recipe.getId());

      if (recipeFulfillment != null) {
        if (recipeFulfillment.isNeedFulfilled()) {
          enoughInStockCount++;
        } else if (recipeFulfillment.isNeedFulfilledWithShoppingList()) {
          notEnoughInStockButInShoppingListCount++;
        } else {
          notEnoughInStockCount++;
        }

        if (filterChipLiveDataStatus.getStatus() != FilterChipLiveDataRecipesStatus.STATUS_ALL) {
          if (filterChipLiveDataStatus.getStatus() == FilterChipLiveDataRecipesStatus.STATUS_ENOUGH_IN_STOCK
              && !recipeFulfillment.isNeedFulfilled()
              || filterChipLiveDataStatus.getStatus() == FilterChipLiveDataRecipesStatus.STATUS_NOT_ENOUGH_BUT_IN_SHOPPING_LIST
              && (recipeFulfillment.isNeedFulfilled() || !recipeFulfillment.isNeedFulfilledWithShoppingList())
              || filterChipLiveDataStatus.getStatus() == FilterChipLiveDataRecipesStatus.STATUS_NOT_ENOUGH
              && (recipeFulfillment.isNeedFulfilled() || recipeFulfillment.isNeedFulfilledWithShoppingList())) {
            continue;
          }
        }
      }

      boolean searchContainsItem = true;
      if (searchInput != null && !searchInput.isEmpty()) {
        searchContainsItem = recipe.getName().toLowerCase().contains(searchInput);

        if (!searchContainsItem && recipeFulfillment != null)
          searchContainsItem = recipeFulfillment.getProductNamesCommaSeparated().toLowerCase().contains(searchInput);
      }

      if (!searchContainsItem) {
        continue;
      }

      filteredRecipes.add(recipe);
    }

    boolean sortAscending = filterChipLiveDataSort.isSortAscending();
    switch (filterChipLiveDataSort.getSortMode()) {
      case SORT_NAME:
        SortUtil.sortRecipesByName(getApplication(), filteredRecipes, sortAscending);
        break;
      case SORT_CALORIES:
        SortUtil.sortRecipesByCalories(getApplication(), filteredRecipes, recipeFulfillments, sortAscending);
        break;
      case SORT_DUE_SCORE:
        SortUtil.sortRecipesByDueScore(getApplication(), filteredRecipes, recipeFulfillments, sortAscending);
        break;
    }

    filterChipLiveDataStatus
            .setEnoughInStockCount(enoughInStockCount)
            .setNotEnoughButInShoppingListCount(notEnoughInStockButInShoppingListCount)
            .setNotEnoughCount(notEnoughInStockCount)
            .emitCounts();

    filteredRecipesLive.setValue(filteredRecipes);
  }

  public void deleteRecipe(int recipeId) {
    dlHelper.delete(
        grocyApi.getObject(ENTITY.RECIPES, recipeId),
        response -> downloadData(),
        this::showErrorMessage
    );
  }

  public void consumeRecipe(int recipeId) {
    dlHelper.get(
        grocyApi.consumeRecipe(recipeId),
        response -> downloadData(),
        this::showErrorMessage
    );
  }

  public void addNotFulfilledProductsToCartForRecipe(int recipeId) {
    dlHelper.get(
        grocyApi.addNotFulfilledProductsToCartForRecipe(recipeId),
        response -> downloadData(),
        this::showErrorMessage
    );
  }

  public void copyRecipe(int recipeId) {
    dlHelper.post(
        grocyApi.copyRecipe(recipeId),
        response -> downloadData(),
        this::showErrorMessage
    );
  }

  public ArrayList<RecipeFulfillment> getRecipeFulfillments() {
    return new ArrayList<>(recipeFulfillments);
  }

  public ArrayList<RecipePosition> getRecipePositions() {
    return new ArrayList<>(recipePositions);
  }

  public ArrayList<Product> getProducts() {
    return new ArrayList<>(products);
  }

  public ArrayList<QuantityUnit> getQuantityUnits() {
    return new ArrayList<>(quantityUnits);
  }

  public boolean isSearchActive() {
    return searchInput != null && !searchInput.isEmpty();
  }

  public void resetSearch() {
    searchInput = null;
    setIsSearchVisible(false);
  }

  public MutableLiveData<ArrayList<Recipe>> getFilteredRecipesLive() {
    return filteredRecipesLive;
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataStatus() {
    return () -> filterChipLiveDataStatus;
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataSort() {
    return () -> filterChipLiveDataSort;
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataExtraField() {
    return () -> filterChipLiveDataExtraField;
  }

  public String getExtraField() {
    return filterChipLiveDataExtraField.getExtraField();
  }

  public void updateSearchInput(String input) {
    this.searchInput = input.toLowerCase();
    updateFilteredRecipes();
  }

  public String getSortMode() {
    return filterChipLiveDataSort.getSortMode();
  }

  public boolean isSortAscending() {
    return filterChipLiveDataSort.isSortAscending();
  }

  @NonNull
  public MutableLiveData<Boolean> getOfflineLive() {
    return offlineLive;
  }

  public Boolean isOffline() {
    return offlineLive.getValue();
  }

  public void setOfflineLive(boolean isOffline) {
    offlineLive.setValue(isOffline);
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
  }

  public boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }
}
