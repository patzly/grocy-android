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
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import com.bumptech.glide.load.model.LazyHeaders;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataFields;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataFields.Field;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.MealPlanEntry;
import xyz.zedler.patrick.grocy.model.MealPlanSection;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductLastPurchased;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.Userfield;
import xyz.zedler.patrick.grocy.repository.MealPlanRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.web.RequestHeaders;

public class MealPlanViewModel extends BaseViewModel {

  private final static String TAG = ShoppingListViewModel.class.getSimpleName();
  public final static String[] DISPLAYED_USERFIELD_ENTITIES = { ENTITY.RECIPES, ENTITY.PRODUCTS };

  public final static String FIELD_WEEK_COSTS = "field_week_costs";
  public final static String FIELD_FULFILLMENT = "field_fulfillment";
  public final static String FIELD_ENERGY = "field_energy";
  public final static String FIELD_PRICE = "field_price";
  public final static String FIELD_PICTURE = "field_picture";
  public final static String FIELD_AMOUNT = "field_amount";
  public final static String FIELD_DAY_SUMMARY = "field_day_summary";

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final LazyHeaders grocyAuthHeaders;
  private final MealPlanRepository repository;
  private final PluralUtil pluralUtil;
  private final DateTimeFormatter dateFormatter;
  private final DateTimeFormatter weekFormatter;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;
  private final FilterChipLiveDataFields filterChipLiveDataHeaderFields;
  private final FilterChipLiveDataFields filterChipLiveDataEntriesFields;
  private final MutableLiveData<LocalDate> selectedDateLive;
  private final MutableLiveData<String> weekCostsTextLive;
  private final MutableLiveData<HashMap<String, List<MealPlanEntry>>> mealPlanEntriesLive;

  private List<MealPlanEntry> mealPlanEntries;
  private List<MealPlanSection> mealPlanSections;
  private List<Recipe> shadowRecipes;
  private HashMap<Integer, Recipe> recipeHashMap;
  private HashMap<Integer, Product> productHashMap;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMap;
  private HashMap<String, RecipeFulfillment> recipeResolvedFulfillmentHashMap;
  private HashMap<Integer, StockItem> stockItemHashMap;
  private HashMap<String, Userfield> userfieldHashMap;

  private final int maxDecimalPlacesAmount;
  private final int decimalPlacesPriceDisplay;
  private final String currency;
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
    this.decimalPlacesPriceDisplay = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_PRICES_DISPLAY,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_PRICES_DISPLAY
    );
    this.currency = sharedPrefs.getString(PREF.CURRENCY, "");
    initialScrollDone = false;
    dateFormatter = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd").toFormatter();
    weekFormatter = new DateTimeFormatterBuilder().appendPattern("yyyy-ww").toFormatter();

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue, getOfflineLive());
    grocyApi = new GrocyApi(getApplication());
    grocyAuthHeaders = RequestHeaders.getGlideGrocyAuthHeaders(getApplication());
    repository = new MealPlanRepository(application);
    pluralUtil = new PluralUtil(application);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
    selectedDateLive = new MutableLiveData<>(LocalDate.now());
    weekCostsTextLive = new MutableLiveData<>();
    mealPlanEntriesLive = new MutableLiveData<>();
    filterChipLiveDataHeaderFields = new FilterChipLiveDataFields(
        getApplication(),
        PREF.MEAL_PLAN_HEADER_FIELDS,
        () -> {},
        new Field(FIELD_WEEK_COSTS, getString(R.string.property_week_costs), isFeatureEnabled(PREF.FEATURE_STOCK_PRICE_TRACKING))
    );
    filterChipLiveDataEntriesFields = new FilterChipLiveDataFields(
        getApplication(),
        PREF.MEAL_PLAN_ENTRIES_FIELDS,
        () -> loadFromDatabase(false),
        new Field(FIELD_DAY_SUMMARY, getString(R.string.property_day_summary), true),
        new Field(FIELD_AMOUNT, getString(R.string.property_amount), true),
        new Field(FIELD_FULFILLMENT, getString(R.string.property_requirements_fulfilled), true),
        new Field(FIELD_ENERGY, getString(R.string.property_energy_only), true),
        new Field(FIELD_PRICE, getString(R.string.property_price), true),
        new Field(FIELD_PICTURE, getString(R.string.property_picture), true)
    );
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      quantityUnitHashMap = ArrayUtil.getQuantityUnitsHashMap(data.getQuantityUnits());
      productHashMap = ArrayUtil.getProductsHashMap(data.getProducts());
      productLastPurchasedHashMap = ArrayUtil
          .getProductLastPurchasedHashMap(data.getProductsLastPurchased());
      shadowRecipes = ArrayUtil.getShadowRecipes(data.getRecipes());
      recipeHashMap = ArrayUtil.getRecipesHashMap(data.getRecipes());
      recipeResolvedFulfillmentHashMap = ArrayUtil.getRecipeResolvedFulfillmentForMealplanHashMap(
          ArrayUtil.getRecipeFulfillmentHashMap(data.getRecipeFulfillments()), data.getRecipes()
      );
      weekCostsTextLive.setValue(getWeekCostsText());
      stockItemHashMap = ArrayUtil.getStockItemHashMap(data.getStockItems());
      userfieldHashMap = ArrayUtil.getUserfieldHashMap(data.getUserfields());
      this.mealPlanSections = data.getMealPlanSections();
      SortUtil.sortMealPlanSections(this.mealPlanSections);
      this.mealPlanEntries = data.getMealPlanEntries();
      mealPlanEntriesLive.setValue(ArrayUtil.getMealPlanEntriesForDayHashMap(
          data.getMealPlanEntries()
      ));
      filterChipLiveDataEntriesFields.setUserfields(
          data.getUserfields(),
          DISPLAYED_USERFIELD_ENTITIES
      );

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
        MealPlanEntry.class,
        MealPlanSection.class,
        Recipe.class,
        RecipeFulfillment.class,
        Product.class,
        StockItem.class,
        Userfield.class
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

  public DateTimeFormatter getDateFormatter() {
    return dateFormatter;
  }

  public String getWeekCostsText() {
    if (shadowRecipes == null || !filterChipLiveDataHeaderFields.getActiveFields()
        .contains(MealPlanViewModel.FIELD_WEEK_COSTS)) {
      return getString(R.string.property_week_costs_insert, getString(R.string.subtitle_unknown));
    };
    LocalDate selectedDate = getSelectedDate();
    String weekFormatted = selectedDate.format(weekFormatter);
    RecipeFulfillment recipeFulfillment = recipeResolvedFulfillmentHashMap.get(weekFormatted);
    double costs = recipeFulfillment != null ? recipeFulfillment.getCosts() : 0;
    return getString(R.string.property_week_costs_insert, getString(
        R.string.property_price_with_currency,
        NumUtil.trimPrice(costs, decimalPlacesPriceDisplay),
        currency
    ));
  }

  public MutableLiveData<HashMap<String, List<MealPlanEntry>>> getMealPlanEntriesLive() {
    return mealPlanEntriesLive;
  }

  public List<MealPlanSection> getMealPlanSections() {
    return mealPlanSections;
  }

  public MutableLiveData<String> getWeekCostsTextLive() {
    return weekCostsTextLive;
  }

  public HashMap<Integer, Recipe> getRecipeHashMap() {
    return recipeHashMap;
  }

  public HashMap<String, RecipeFulfillment> getRecipeResolvedFulfillmentHashMap() {
    return recipeResolvedFulfillmentHashMap;
  }

  public HashMap<Integer, Product> getProductHashMap() {
    return productHashMap;
  }

  public HashMap<Integer, StockItem> getStockItemHashMap() {
    return stockItemHashMap;
  }

  public HashMap<Integer, QuantityUnit> getQuantityUnitHashMap() {
    return quantityUnitHashMap;
  }

  public HashMap<Integer, ProductLastPurchased> getProductLastPurchasedHashMap() {
    return productLastPurchasedHashMap;
  }

  public HashMap<String, Userfield> getUserFieldHashMap() {
    return userfieldHashMap;
  }

  public FilterChipLiveDataFields getFilterChipLiveDataHeaderFields() {
    return filterChipLiveDataHeaderFields;
  }

  public FilterChipLiveDataFields getFilterChipLiveDataEntriesFields() {
    return filterChipLiveDataEntriesFields;
  }

  public boolean isInitialScrollDone() {
    return initialScrollDone;
  }

  public void setInitialScrollDone(boolean initialScrollDone) {
    this.initialScrollDone = initialScrollDone;
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
