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

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.fragment.RecipeFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataFields;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataFields.Field;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversionResolved;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.RecipePosition;
import xyz.zedler.patrick.grocy.model.RecipePositionResolved;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.Userfield;
import xyz.zedler.patrick.grocy.repository.RecipesRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.VersionUtil;

public class RecipeViewModel extends BaseViewModel {

  private final static String TAG = RecipeViewModel.class.getSimpleName();
  public final static String[] DISPLAYED_USERFIELD_ENTITIES = { ENTITY.RECIPES };

  public final static String FIELD_FULFILLMENT = "field_fulfillment";
  public final static String FIELD_NOTE = "field_note";
  public final static String FIELD_ENERGY = "field_energy";
  public final static String FIELD_PRICE = "field_price";

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final RecipesRepository repository;
  private final RecipeFragmentArgs args;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Recipe> recipeLive;
  private final MutableLiveData<String> servingsDesiredLive;
  private final MutableLiveData<Boolean> displayFulfillmentWrongInfo;
  private final FilterChipLiveDataFields filterChipLiveDataRecipeInfoFields;
  private final FilterChipLiveDataFields filterChipLiveDataIngredientFields;

  private List<Recipe> recipes;
  private List<RecipePosition> recipePositions;
  private List<RecipePositionResolved> recipePositionsResolved;
  private List<Product> products;
  private List<QuantityUnit> quantityUnits;
  private List<QuantityUnitConversionResolved> quantityUnitConversions;
  private HashMap<Integer, StockItem> stockItemHashMap;
  private List<ShoppingListItem> shoppingListItems;
  private HashMap<String, Userfield> userfieldHashMap;
  private RecipeFulfillment recipeFulfillment;

  private Timer timerUpdateData;
  private final int maxDecimalPlacesAmount;
  private final int decimalPlacesPriceDisplay;
  private final boolean debug;

  public RecipeViewModel(@NonNull Application application, RecipeFragmentArgs args) {
    super(application);

    this.args = args;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue, getOfflineLive());
    grocyApi = new GrocyApi(getApplication());
    repository = new RecipesRepository(application);

    infoFullscreenLive = new MutableLiveData<>();
    recipeLive = new MutableLiveData<>();
    servingsDesiredLive = new MutableLiveData<>();
    displayFulfillmentWrongInfo = new MutableLiveData<>(false);
    boolean priceTracking = sharedPrefs.getBoolean(PREF.FEATURE_STOCK_PRICE_TRACKING, true);
    filterChipLiveDataRecipeInfoFields = new FilterChipLiveDataFields(
        getApplication(),
        PREF.RECIPE_INFO_FIELDS,
        () -> loadFromDatabase(false),
        new Field(FIELD_FULFILLMENT, getString(R.string.property_requirements_fulfilled), true),
        new Field(FIELD_ENERGY, getString(R.string.property_energy_only), true),
        priceTracking
            ? new Field(FIELD_PRICE, getString(R.string.property_price), true) : null
    );
    filterChipLiveDataIngredientFields = new FilterChipLiveDataFields(
        getApplication(),
        PREF.RECIPE_INGREDIENT_FIELDS,
        () -> loadFromDatabase(false),
        new Field(FIELD_FULFILLMENT, getString(R.string.property_requirements_fulfilled), true),
        new Field(FIELD_NOTE, getString(R.string.property_note), true),
        new Field(FIELD_ENERGY, getString(R.string.property_energy_only), true),
        priceTracking
            ? new Field(FIELD_PRICE, getString(R.string.property_price), true) : null
    );

    timerUpdateData = new Timer();
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
      recipePositionsResolved = RecipePositionResolved
          .getRecipePositionsFromRecipeId(data.getRecipePositionsResolved(), args.getRecipeId());
      RecipePositionResolved.fillRecipePositionsResolvedWithNotCheckStockFulfillment(
          recipePositionsResolved, ArrayUtil.getRecipePositionHashMap(recipePositions)
      );
      products = data.getProducts();
      quantityUnits = data.getQuantityUnits();
      quantityUnitConversions = data.getQuantityUnitConversionsResolved();
      stockItemHashMap = ArrayUtil.getStockItemHashMap(data.getStockItems());
      shoppingListItems = data.getShoppingListItems();
      userfieldHashMap = ArrayUtil.getUserfieldHashMap(data.getUserfields());
      filterChipLiveDataRecipeInfoFields.setUserfields(
          data.getUserfields(),
          DISPLAYED_USERFIELD_ENTITIES
      );

      Recipe recipe = Recipe.getRecipeFromId(recipes, args.getRecipeId());
      recipeLive.setValue(recipe);
      if ((servingsDesiredLive.getValue() == null || servingsDesiredLive.getValue().isBlank())
          && recipe != null) {
        servingsDesiredLive.setValue(
            NumUtil.trimAmount(recipe.getDesiredServings(), maxDecimalPlacesAmount)
        );
      }
      if (downloadAfterLoading) {
        downloadData(false);
      }
    }, error -> onError(error, TAG));
  }

  public void downloadData(boolean forceUpdate) {
    dlHelper.updateData(
        updated -> {
          if (updated) loadFromDatabase(false);
        },
        error -> onError(error, TAG),
        forceUpdate,
        true,
        Recipe.class,
        RecipeFulfillment.class,
        RecipePosition.class,
        VersionUtil.isGrocyServerMin400(sharedPrefs) ? RecipePositionResolved.class : null,
        Product.class,
        QuantityUnit.class,
        QuantityUnitConversionResolved.class,
        StockItem.class,
        ShoppingListItem.class,
        Userfield.class
    );
  }

  public void showAmountBottomSheet() {
    Bundle bundle = new Bundle();
    if (NumUtil.isStringDouble(servingsDesiredLive.getValue())) {
      bundle.putDouble(ARGUMENT.NUMBER, NumUtil.toDouble(servingsDesiredLive.getValue()));
    } else {
      bundle.putDouble(ARGUMENT.NUMBER, 1);
    }
    bundle.putString(ARGUMENT.HINT, getString(R.string.property_servings_desired));
    showBottomSheet(new InputBottomSheet(), bundle);
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
    timerUpdateData.cancel();
    timerUpdateData = new Timer();
    timerUpdateData.schedule(
        new TimerTask() {
          @Override
          public void run() {
            saveDesiredServings();
          }
        },
        500
    );
  }

  public void saveDesiredServings() {
    double servingsDesired;
    if (NumUtil.isStringDouble(servingsDesiredLive.getValue())) {
      servingsDesired = NumUtil.toDouble(servingsDesiredLive.getValue());
    } else {
      servingsDesired = 1;
      servingsDesiredLive.setValue(NumUtil.trimAmount(servingsDesired, maxDecimalPlacesAmount));
    }

    JSONObject body = new JSONObject();
    try {
      body.put(
          "desired_servings", NumUtil.trimAmount(servingsDesired, maxDecimalPlacesAmount)
      );
    } catch (JSONException e) {
      showErrorMessage();
      return;
    }

    Recipe.editRecipe(
        dlHelper,
        args.getRecipeId(),
        body,
        response -> dlHelper.updateData(
            updated -> loadFromDatabase(false),
            error -> onError(error, TAG),
            false,
            false,
            Recipe.class,
            RecipeFulfillment.class,
            RecipePosition.class,
            VersionUtil.isGrocyServerMin400(sharedPrefs) ? RecipePositionResolved.class : null
        ),
        error -> onError(error, TAG)
    ).perform(dlHelper.getUuid());
  }

  public void deleteRecipe(int recipeId) {
    dlHelper.delete(
        grocyApi.getObject(ENTITY.RECIPES, recipeId),
        response -> downloadData(false),
        this::showNetworkErrorMessage
    );
  }

  public void consumeRecipe(int recipeId) {
    dlHelper.post(
        grocyApi.consumeRecipe(recipeId),
        response -> downloadData(false),
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
        response -> downloadData(false),
        this::showNetworkErrorMessage
    );
  }

  public void copyRecipe(int recipeId) {
    dlHelper.post(
        grocyApi.copyRecipe(recipeId),
        response -> navigateUp(),
        this::showNetworkErrorMessage
    );
  }

  public RecipeFulfillment getRecipeFulfillment() {
    return recipeFulfillment;
  }

  public List<RecipePosition> getRecipePositions() {
    return recipePositions;
  }

  public List<RecipePositionResolved> getRecipePositionsResolved() {
    return recipePositionsResolved;
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

  public FilterChipLiveDataFields getFilterChipLiveDataRecipeInfoFields() {
    return filterChipLiveDataRecipeInfoFields;
  }

  public FilterChipLiveDataFields getFilterChipLiveDataIngredientFields() {
    return filterChipLiveDataIngredientFields;
  }

  public HashMap<String, Userfield> getUserfieldHashMap() {
    return userfieldHashMap;
  }

  public MutableLiveData<Recipe> getRecipeLive() {
    return recipeLive;
  }

  public MutableLiveData<String> getServingsDesiredLive() {
    return servingsDesiredLive;
  }

  public MutableLiveData<Boolean> getDisplayFulfillmentWrongInfo() {
    return displayFulfillmentWrongInfo;
  }

  public void toggleDisplayFulfillmentWrongInfo() {
    assert displayFulfillmentWrongInfo.getValue() != null;
    displayFulfillmentWrongInfo.setValue(!displayFulfillmentWrongInfo.getValue());
  }

  public boolean isGrocyVersionMin400() {
    return VersionUtil.isGrocyServerMin400(sharedPrefs);
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

  public String getEnergyUnit() {
    return sharedPrefs.getString(PREF.ENERGY_UNIT, PREF.ENERGY_UNIT_DEFAULT);
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
