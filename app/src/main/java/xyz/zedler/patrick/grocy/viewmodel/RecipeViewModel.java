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

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import java.util.HashMap;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.fragment.RecipeFragmentArgs;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversionResolved;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.repository.RecipesRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

public class RecipeViewModel extends BaseViewModel {

  private final static String TAG = RecipeViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final RecipesRepository repository;
  private final RecipeFragmentArgs args;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;
  private final MutableLiveData<Recipe> recipeLive;
  private final MutableLiveData<String> servingsDesiredLive;
  private final MutableLiveData<Boolean> servingsDesiredSaveEnabledLive;

  private List<Recipe> recipes;
  private List<RecipePosition> recipePositions;
  private List<Product> products;
  private List<QuantityUnit> quantityUnits;
  private List<QuantityUnitConversionResolved> quantityUnitConversions;
  private HashMap<Integer, StockItem> stockItemHashMap;
  private List<ShoppingListItem> shoppingListItems;
  private RecipeFulfillment recipeFulfillment;

  private final int maxDecimalPlacesAmount;
  private final int decimalPlacesPriceDisplay;
  private final boolean debug;

  public RecipeViewModel(@NonNull Application application, RecipeFragmentArgs args) {
    super(application);

    this.args = args;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    repository = new RecipesRepository(application);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
    recipeLive = new MutableLiveData<>();
    servingsDesiredLive = new MutableLiveData<>();
    servingsDesiredSaveEnabledLive = new MutableLiveData<>(false);

    maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
    decimalPlacesPriceDisplay = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_PRICES_DISPLAY,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_PRICES_DISPLAY
    );
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      recipes = data.getRecipes();
      recipeFulfillment = RecipeFulfillment
          .getRecipeFulfillmentFromRecipeId(data.getRecipeFulfillments(), args.getRecipeId());
      recipePositions = RecipePosition
          .getRecipePositionsFromRecipeId(data.getRecipePositions(), args.getRecipeId());
      products = data.getProducts();
      quantityUnits = data.getQuantityUnits();
      quantityUnitConversions = data.getQuantityUnitConversionsResolved();
      stockItemHashMap = ArrayUtil.getStockItemHashMap(data.getStockItems());
      shoppingListItems = data.getShoppingListItems();

      Recipe recipe = Recipe.getRecipeFromId(recipes, args.getRecipeId());
      recipeLive.setValue(recipe);
      if ((servingsDesiredLive.getValue() == null || servingsDesiredLive.getValue().isBlank())
          && recipe != null) {
        servingsDesiredLive.setValue(
            NumUtil.trimAmount(recipe.getDesiredServings(), maxDecimalPlacesAmount)
        );
      }
      if (downloadAfterLoading) {
        downloadData();
      }
    }, error -> onError(error, TAG));
  }

  public void downloadData(boolean skipOfflineCheck) {
    if (!skipOfflineCheck && isOffline()) { // skip downloading and update recyclerview
      isLoadingLive.setValue(false);
      return;
    }

    dlHelper.updateData(
        () -> {
          if (isOffline()) setOfflineLive(false);
          loadFromDatabase(false);
        },
        error -> onError(error, TAG),
        Recipe.class,
        RecipeFulfillment.class,
        RecipePosition.class,
        Product.class,
        QuantityUnit.class,
        QuantityUnitConversion.class,
        StockItem.class,
        ShoppingListItem.class
    );
  }

  public void downloadData() {
    downloadData(false);
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.putString(PREF.DB_LAST_TIME_RECIPES, null);
    editPrefs.putString(PREF.DB_LAST_TIME_RECIPE_FULFILLMENTS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_RECIPE_POSITIONS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_STOCK_ITEMS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, null);
    editPrefs.apply();
    downloadData(true);
  }

  public void changeAmount(boolean more) {
    if (!NumUtil.isStringDouble(servingsDesiredLive.getValue())) {
      servingsDesiredLive.setValue(String.valueOf(1));
    } else {
      double servings = NumUtil.toDouble(servingsDesiredLive.getValue());
      double servingsNew = more ? servings + 1 : servings - 1;
      if (servingsNew <= 0) servingsNew = 1;
      servingsDesiredLive.setValue(NumUtil.trimAmount(servingsNew, maxDecimalPlacesAmount));
    }
  }

  public void saveDesiredServings() {
    double servingsDesired;
    if (NumUtil.isStringDouble(servingsDesiredLive.getValue())) {
      servingsDesired = NumUtil.toDouble(servingsDesiredLive.getValue());
    } else {
      servingsDesired = 1;
      servingsDesiredLive.setValue(NumUtil.trimAmount(servingsDesired, maxDecimalPlacesAmount));
    }
    servingsDesiredSaveEnabledLive.setValue(false);

    JSONObject body = new JSONObject();
    try {
      body.put(
          "desired_servings", NumUtil.trimAmount(servingsDesired, maxDecimalPlacesAmount)
      );
    } catch (JSONException e) {
      showErrorMessage();
      servingsDesiredSaveEnabledLive.setValue(true);
      return;
    }

    Recipe.editRecipe(
        dlHelper,
        args.getRecipeId(),
        body,
        response -> dlHelper.updateData(
            () -> {
              servingsDesiredSaveEnabledLive.setValue(false);
              downloadData();
            },
            volleyError -> {
              showErrorMessage();
              servingsDesiredSaveEnabledLive.setValue(true);
            },
            Recipe.class,
            RecipeFulfillment.class,
            RecipePosition.class
        ),
        error -> {
          showErrorMessage();
          servingsDesiredSaveEnabledLive.setValue(true);
        }
    ).perform(dlHelper.getUuid());
  }

  public void deleteRecipe(int recipeId) {
    dlHelper.delete(
        grocyApi.getObject(ENTITY.RECIPES, recipeId),
        response -> downloadData(),
        this::showNetworkErrorMessage
    );
  }

  public void consumeRecipe(int recipeId) {
    dlHelper.post(
        grocyApi.consumeRecipe(recipeId),
        response -> downloadData(),
        this::showNetworkErrorMessage
    );
  }

  public void addNotFulfilledProductsToCartForRecipe(int recipeId, int[] excludedProductIds) {
    JSONObject jsonObject = new JSONObject();
    try {
      JSONArray array = new JSONArray();
      for (int id : excludedProductIds) array.put(id);
      jsonObject.put("excludedProductIds", array);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
    dlHelper.postWithArray(
        grocyApi.addNotFulfilledProductsToCartForRecipe(recipeId),
        jsonObject,
        response -> downloadData(),
        this::showNetworkErrorMessage
    );
  }

  public void copyRecipe(int recipeId) {
    dlHelper.post(
        grocyApi.copyRecipe(recipeId),
        response -> downloadData(),
        this::showNetworkErrorMessage
    );
  }

  public RecipeFulfillment getRecipeFulfillment() {
    return recipeFulfillment;
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

  public List<QuantityUnitConversionResolved> getQuantityUnitConversions() {
    return quantityUnitConversions;
  }

  public HashMap<Integer, StockItem> getStockItemHashMap() {
    return stockItemHashMap;
  }

  public List<ShoppingListItem> getShoppingListItems() {
    return shoppingListItems;
  }

  public MutableLiveData<Recipe> getRecipeLive() {
    return recipeLive;
  }

  public MutableLiveData<String> getServingsDesiredLive() {
    return servingsDesiredLive;
  }

  public MutableLiveData<Boolean> getServingsDesiredSaveEnabledLive() {
    return servingsDesiredSaveEnabledLive;
  }

  public int getMaxDecimalPlacesAmount() {
    return maxDecimalPlacesAmount;
  }

  public int getDecimalPlacesPriceDisplay() {
    return decimalPlacesPriceDisplay;
  }

  @Override
  public SharedPreferences getSharedPrefs() {
    return sharedPrefs;
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

  public String getCurrency() {
    return sharedPrefs.getString(PREF.CURRENCY, "");
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

  public static class RecipeViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final RecipeFragmentArgs args;

    public RecipeViewModelFactory(
        Application application,
        RecipeFragmentArgs args
    ) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new RecipeViewModel(application, args);
    }
  }
}
