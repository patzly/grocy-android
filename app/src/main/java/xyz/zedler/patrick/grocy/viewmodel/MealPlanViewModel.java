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
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import com.bumptech.glide.load.model.LazyHeaders;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.MealPlanEntry;
import xyz.zedler.patrick.grocy.model.MealPlanSection;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.repository.MealPlanRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.web.RequestHeaders;

public class MealPlanViewModel extends BaseViewModel {

  private final static String TAG = ShoppingListViewModel.class.getSimpleName();

  public final static String FIELD_FULFILLMENT = "field_fulfillment";
  public final static String FIELD_CALORIES = "field_calories";
  public final static String FIELD_PICTURE = "field_picture";

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final LazyHeaders grocyAuthHeaders;
  private final MealPlanRepository repository;
  private final PluralUtil pluralUtil;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;
  private final MutableLiveData<LocalDate> selectedDateLive;
  private final MutableLiveData<HashMap<String, List<MealPlanEntry>>> mealPlanEntriesLive;

  private List<MealPlanEntry> mealPlanEntries;
  private List<MealPlanSection> mealPlanSections;
  private List<Product> products;
  private HashMap<Integer, Recipe> recipeHashMap;
  private HashMap<Integer, Product> productHashMap;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private boolean isSmoothScrolling;

  private final int maxDecimalPlacesAmount;
  private boolean initialScrollDone;
  private final boolean debug;

  public MealPlanViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);
    maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
    initialScrollDone = false;

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue, getOfflineLive());
    grocyApi = new GrocyApi(getApplication());
    grocyAuthHeaders = RequestHeaders.getGlideGrocyAuthHeaders(getApplication());
    repository = new MealPlanRepository(application);
    pluralUtil = new PluralUtil(application);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
    selectedDateLive = new MutableLiveData<>(LocalDate.now());
    mealPlanEntriesLive = new MutableLiveData<>();
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      quantityUnitHashMap = ArrayUtil.getQuantityUnitsHashMap(data.getQuantityUnits());
      this.products = data.getProducts();
      productHashMap = ArrayUtil.getProductsHashMap(data.getProducts());
      recipeHashMap = ArrayUtil.getRecipesHashMap(data.getRecipes());
      this.mealPlanSections = data.getMealPlanSections();
      SortUtil.sortMealPlanSections(this.mealPlanSections);
      this.mealPlanEntries = data.getMealPlanEntries();
      mealPlanEntriesLive.setValue(ArrayUtil.getMealPlanEntriesForDayHashMap(
          data.getMealPlanEntries()
      ));

      if (downloadAfterLoading) {
        downloadData(false);
      }
    }, error -> onError(error, TAG));
  }

  public void downloadData(boolean forceUpdate) {
    if (isOffline()) { // skip downloading and update recyclerview
      isLoadingLive.setValue(false);
      return;
    }
    dlHelper.updateData(
        updated -> {
          if (updated) loadFromDatabase(false);
        },
        error -> onError(error, TAG),
        forceUpdate,
        true,
        QuantityUnit.class,
        ProductGroup.class,
        MealPlanEntry.class,
        MealPlanSection.class,
        Recipe.class,
        Product.class
    );
  }

  public DayOfWeek getFirstDayOfWeek() {
    return DateUtil.getMealPlanFirstDayOfWeek(sharedPrefs);
  }

  public MutableLiveData<LocalDate> getSelectedDateLive() {
    return selectedDateLive;
  }

  public LocalDate getSelectedDate() {
    return selectedDateLive.getValue();
  }

  public List<MealPlanEntry> getMealPlanEntries() {
    return mealPlanEntries;
  }

  public MutableLiveData<HashMap<String, List<MealPlanEntry>>> getMealPlanEntriesLive() {
    return mealPlanEntriesLive;
  }

  public List<MealPlanSection> getMealPlanSections() {
    return mealPlanSections;
  }

  public HashMap<Integer, Recipe> getRecipeHashMap() {
    return recipeHashMap;
  }

  public HashMap<Integer, Product> getProductHashMap() {
    return productHashMap;
  }

  public HashMap<Integer, QuantityUnit> getQuantityUnitHashMap() {
    return quantityUnitHashMap;
  }

  public boolean isInitialScrollDone() {
    return initialScrollDone;
  }

  public void setInitialScrollDone(boolean initialScrollDone) {
    this.initialScrollDone = initialScrollDone;
  }

  public boolean isSmoothScrolling() {
    return isSmoothScrolling;
  }

  public void setSmoothScrolling(boolean smoothScrolling) {
    isSmoothScrolling = smoothScrolling;
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

  public GrocyApi getGrocyApi() {
    return grocyApi;
  }

  public LazyHeaders getGrocyAuthHeaders() {
    return grocyAuthHeaders;
  }

  public boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  public String getCurrency() {
    return sharedPrefs.getString(
        PREF.CURRENCY,
        ""
    );
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }

  public static class MealPlanViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;

    public MealPlanViewModelFactory(Application application) {
      this.application = application;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new MealPlanViewModel(application);
    }
  }
}
