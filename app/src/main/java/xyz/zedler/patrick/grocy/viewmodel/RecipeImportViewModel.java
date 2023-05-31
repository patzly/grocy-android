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
import android.os.Bundle;
import android.webkit.URLUtil;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Language;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.RecipeParsed;
import xyz.zedler.patrick.grocy.model.RecipeParsed.Ingredient;
import xyz.zedler.patrick.grocy.model.RecipeParsed.IngredientPart;
import xyz.zedler.patrick.grocy.repository.InventoryRepository;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.web.NetworkQueue;

public class RecipeImportViewModel extends BaseViewModel {

  private static final String TAG = RecipeImportViewModel.class.getSimpleName();
  private final SharedPreferences sharedPrefs;
  private final boolean debug;

  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final InventoryRepository repository;

  private List<Product> products;
  private List<QuantityUnitConversion> unitConversions;
  private List<ProductBarcode> barcodes;
  private List<Location> locations;
  private List<QuantityUnit> quantityUnits;
  private RecipeParsed recipeParsed;

  private final MutableLiveData<String> recipeWebsiteLive;
  private final MutableLiveData<String> recipeWebsiteErrorLive;
  private final MutableLiveData<String> recipeTitleLive;
  private final MutableLiveData<String> recipeTitleErrorLive;
  private final MutableLiveData<Language> recipeLanguageLive;
  private final MutableLiveData<String> recipeTimeLive;
  private final MutableLiveData<String> recipeTimeErrorLive;
  private final MutableLiveData<Boolean> insertPreparationTimeInText;
  private final MutableLiveData<String> mappingEntityLive;
  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<Boolean> displayHelpLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;

  private Runnable queueEmptyAction;
  private final int maxDecimalPlacesAmount;

  public RecipeImportViewModel(@NonNull Application application, Bundle args) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    repository = new InventoryRepository(application);

    recipeWebsiteLive = new MutableLiveData<>();
    recipeWebsiteErrorLive = new MutableLiveData<>();
    recipeTitleLive = new MutableLiveData<>();
    recipeTitleErrorLive = new MutableLiveData<>();
    recipeLanguageLive = new MutableLiveData<>();
    recipeTimeLive = new MutableLiveData<>();
    recipeTimeErrorLive = new MutableLiveData<>();
    insertPreparationTimeInText = new MutableLiveData<>();
    mappingEntityLive = new MutableLiveData<>(IngredientPart.ENTITY_AMOUNT);
    displayHelpLive = new MutableLiveData<>(false);
    infoFullscreenLive = new MutableLiveData<>();

    if (args != null && args.containsKey("url")) {
      recipeWebsiteLive.setValue(args.getString("url"));
    }
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      this.products = data.getProducts();
      this.barcodes = data.getBarcodes();
      this.locations = data.getLocations();
      this.quantityUnits = data.getQuantityUnits();
      this.unitConversions = data.getQuantityUnitConversions();
      if (downloadAfterLoading) {
        downloadData();
      }
    }, error -> onError(error, TAG));
  }

  public void downloadData(@Nullable String dbChangedTime) {
    if (dbChangedTime == null) {
      dlHelper.getTimeDbChanged(this::downloadData, error -> onError(error, TAG));
      return;
    }

    NetworkQueue queue = dlHelper.newQueue(this::onQueueEmpty, error -> onError(error, TAG));
    queue.append(
        Product.updateProducts(dlHelper, dbChangedTime, products -> {
          this.products = products;
        }), ProductBarcode.updateProductBarcodes(
            dlHelper, dbChangedTime, barcodes -> this.barcodes = barcodes
        ), Location.updateLocations(
            dlHelper, dbChangedTime, locations -> this.locations = locations
        ), QuantityUnitConversion.updateQuantityUnitConversions(
            dlHelper, dbChangedTime, conversions -> this.unitConversions = conversions
        ), QuantityUnit.updateQuantityUnits(
            dlHelper,
            dbChangedTime,
            quantityUnits -> this.quantityUnits = quantityUnits
        )
    );
    if (queue.isEmpty()) {
      if (queueEmptyAction != null) {
        queueEmptyAction.run();
        queueEmptyAction = null;
      }
      return;
    }
    queue.start();
  }

  public void downloadData() {
    downloadData(null);
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.putString(PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_PRODUCT_BARCODES, null);
    editPrefs.putString(PREF.DB_LAST_TIME_LOCATIONS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.apply();
    downloadData();
  }

  private void onQueueEmpty() {
    if (queueEmptyAction != null) {
      queueEmptyAction.run();
      queueEmptyAction = null;
    }
  }

  public void scrapeRecipe() {
    dlHelper.scrapeRecipe(getWebsiteUrlTrimmed(), response -> {
      try {
        JSONArray jsonArray = response.getJSONArray("ingredients");
        response.put("ingredients", jsonArray);
        recipeParsed = RecipeParsed.fromJson(response);

        List<String> ingredientStrings = new ArrayList<>();
        for (Ingredient ingredient : recipeParsed.getIngredients()) {
          ingredientStrings.add(ingredient.getIngredientText());
        }

        dlHelper.parseIngredients(ingredientStrings, "de", extract_results -> {
          try {
            recipeParsed.setIngredientParts(extract_results);
            recipeParsed.updateAllWordsWithEntities();
          } catch (JSONException e) {
            showMessage(e.getLocalizedMessage());
          }
          sendEvent(Event.TRANSACTION_SUCCESS);
        }, volleyError -> {
          showMessage(R.string.error_undefined);
          sendEvent(Event.TRANSACTION_SUCCESS);
        });
      } catch (JSONException e) {
        showMessage(e.getLocalizedMessage());
      }
    }, volleyError -> {
      showMessage(R.string.error_undefined);
    });
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
    return infoFullscreenLive;
  }

  public void setQueueEmptyAction(Runnable queueEmptyAction) {
    this.queueEmptyAction = queueEmptyAction;
  }

  public MutableLiveData<String> getRecipeWebsiteLive() {
    return recipeWebsiteLive;
  }

  public MutableLiveData<String> getRecipeWebsiteErrorLive() {
    return recipeWebsiteErrorLive;
  }

  public MutableLiveData<String> getRecipeTitleLive() {
    return recipeTitleLive;
  }

  public MutableLiveData<String> getRecipeTitleErrorLive() {
    return recipeTitleErrorLive;
  }

  public MutableLiveData<String> getRecipeTimeLive() {
    return recipeTimeLive;
  }

  public MutableLiveData<String> getRecipeTimeErrorLive() {
    return recipeTimeErrorLive;
  }

  public MutableLiveData<Boolean> getInsertPreparationTimeInText() {
    return insertPreparationTimeInText;
  }

  public void setInsertPreparationTimeInText(boolean state) {
    this.insertPreparationTimeInText.setValue(state);
  }

  public MutableLiveData<String> getMappingEntityLive() {
    return mappingEntityLive;
  }

  public void setMappingEntityLive(String entity) {
    mappingEntityLive.setValue(entity);
  }

  public void updateAllWordsClickableState() {
    for (Ingredient ingredient : recipeParsed.getIngredients()) {
      ingredient.updateWordsClickableState(mappingEntityLive.getValue());
    }
  }

  public boolean isRecipeWebsiteValid() {
    String serverUrl = getWebsiteUrlTrimmed();
    if (serverUrl.isEmpty()) {
      recipeWebsiteErrorLive.setValue(getString(R.string.error_empty));
      return false;
    } else if (!URLUtil.isHttpUrl(serverUrl) && !URLUtil.isHttpsUrl(serverUrl)) {
      recipeWebsiteErrorLive.setValue(getString(R.string.error_invalid_url));
      return false;
    }
    recipeWebsiteErrorLive.setValue(null);
    return true;
  }

  public boolean isRecipeTitleValid() {
    if (recipeTitleLive.getValue() == null || recipeTitleLive.getValue().isBlank()) {
      recipeTitleErrorLive.setValue(getString(R.string.error_empty));
      return false;
    }
    recipeTitleErrorLive.setValue(null);
    return true;
  }

  @NonNull
  public String getWebsiteUrlTrimmed() {
    if (recipeWebsiteLive.getValue() == null) {
      return "";
    }
    return recipeWebsiteLive.getValue().replaceAll("/+$", "").trim();
  }

  public RecipeParsed getRecipeParsed() {
    return recipeParsed;
  }

  public void setRecipeParsed(RecipeParsed recipeParsed) {
    this.recipeParsed = recipeParsed;
    recipeTitleLive.setValue(recipeParsed.getTitle());
    if (NumUtil.isStringNum(recipeParsed.getTotalTime())) {
      int time = (int) NumUtil.toDouble(recipeParsed.getTotalTime());
      recipeTimeLive.setValue(getApplication().getResources()
          .getQuantityString(R.plurals.date_minutes, time, time));
    } else {
      recipeTimeLive.setValue(recipeParsed.getTotalTime());
    }
    insertPreparationTimeInText.setValue(true);
    sendEvent(Event.LOAD_IMAGE);
  }

  public void openQuantityUnitsBottomSheet(Ingredient ingredient, IngredientPart part) {
    Bundle bundle = new Bundle();
    bundle.putParcelableArrayList(ARGUMENT.QUANTITY_UNITS, new ArrayList<>(quantityUnits));
    bundle.putString(ARGUMENT.TEXT, ingredient.getTextFromPart(part));
    if (part.getAssignedGrocyObjectId() != null) {
      bundle.putInt(ARGUMENT.SELECTED_ID, part.getAssignedGrocyObjectId());
    }
    showBottomSheet(new QuantityUnitsBottomSheet(), bundle);
  }

  public MutableLiveData<Boolean> getDisplayHelpLive() {
    return displayHelpLive;
  }

  public void toggleDisplayHelpLive() {
    assert displayHelpLive.getValue() != null;
    displayHelpLive.setValue(!displayHelpLive.getValue());
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

  public static class RecipeImportViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final Bundle args;

    public RecipeImportViewModelFactory(Application application, Bundle args) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new RecipeImportViewModel(application, args);
    }
  }
}
