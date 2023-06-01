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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.StockOverviewFragmentArgs;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.MealPlanEntry;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.model.VolatileItem;
import xyz.zedler.patrick.grocy.repository.MealPlanRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.GrocycodeUtil;
import xyz.zedler.patrick.grocy.util.GrocycodeUtil.Grocycode;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

public class MealPlanViewModel extends BaseViewModel {

  private final static String TAG = ShoppingListViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final MealPlanRepository repository;
  private final PluralUtil pluralUtil;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;
  private final MutableLiveData<LocalDate> selectedDateLive;
  private final MutableLiveData<ArrayList<StockItem>> filteredStockItemsLive;

  private List<MealPlanEntry> mealPlanEntries;
  private List<Product> products;
  private HashMap<Integer, ProductGroup> productGroupHashMap;
  private HashMap<String, ProductBarcode> productBarcodeHashMap;
  private HashMap<Integer, Product> productHashMap;
  private List<ShoppingListItem> shoppingListItems;
  private ArrayList<String> shoppingListItemsProductIds;
  private HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private HashMap<Integer, MissingItem> productIdsMissingItems;
  private HashMap<Integer, Location> locationHashMap;
  private HashMap<Integer, HashMap<Integer, StockLocation>> stockLocationsHashMap;

  private String searchInput;
  private ArrayList<String> searchResultsFuzzy;
  private final int maxDecimalPlacesAmount;
  private final boolean debug;

  public MealPlanViewModel(@NonNull Application application, StockOverviewFragmentArgs args) {
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
    repository = new MealPlanRepository(application);
    pluralUtil = new PluralUtil(application);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
    selectedDateLive = new MutableLiveData<>(LocalDate.now());
    filteredStockItemsLive = new MutableLiveData<>();
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      quantityUnitHashMap = ArrayUtil.getQuantityUnitsHashMap(data.getQuantityUnits());
      this.products = data.getProducts();
      productHashMap = ArrayUtil.getProductsHashMap(data.getProducts());
      this.mealPlanEntries = data.getMealPlanEntries();

      updateFilteredStockItems();
      if (downloadAfterLoading) {
        downloadData();
      }
    }, error -> onError(error, TAG));
  }

  public void downloadData() {
    if (isOffline()) { // skip downloading and update recyclerview
      isLoadingLive.setValue(false);
      updateFilteredStockItems();
      return;
    }
    dlHelper.updateData(
        () -> loadFromDatabase(false),
        error -> onError(error, TAG),
        QuantityUnit.class,
        ProductGroup.class,
        MealPlanEntry.class,
        Product.class,
        ProductBarcode.class,
        VolatileItem.class,
        ShoppingListItem.class,
        Location.class,
        StockLocation.class
    );
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.putString(PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_PRODUCT_GROUPS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_MEAL_PLAN_ENTRIES, null);
    editPrefs.putString(PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_PRODUCT_BARCODES, null);
    editPrefs.putString(PREF.DB_LAST_TIME_VOLATILE, null);
    editPrefs.putString(PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_LOCATIONS, null);
    editPrefs.putString(PREF.DB_LAST_TIME_STOCK_LOCATIONS, null);
    editPrefs.apply();
    downloadData();
  }

  public void updateFilteredStockItems() {
    ArrayList<StockItem> filteredStockItems = new ArrayList<>();

    Product productSearch = null;
    ProductBarcode productBarcodeSearch = null;
    if (searchInput != null && !searchInput.isEmpty()) {
      Grocycode grocycode = GrocycodeUtil.getGrocycode(searchInput);
      if (grocycode != null && grocycode.isProduct()) {
        productSearch = productHashMap.get(grocycode.getObjectId());
      }
      if (productSearch == null) {
        productBarcodeSearch = productBarcodeHashMap.get(searchInput);
      }
    }

    filteredStockItemsLive.setValue(filteredStockItems);
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

  public ArrayList<Integer> getProductIdsMissingItems() {
    return new ArrayList<>(productIdsMissingItems.keySet());
  }

  public HashMap<Integer, ProductGroup> getProductGroupHashMap() {
    return productGroupHashMap;
  }

  public HashMap<Integer, Product> getProductHashMap() {
    return productHashMap;
  }

  public ArrayList<String> getShoppingListItemsProductIds() {
    return shoppingListItemsProductIds;
  }

  public HashMap<Integer, Location> getLocationHashMap() {
    return locationHashMap;
  }

  public Location getLocationFromId(int id) {
    return locationHashMap.get(id);
  }

  public HashMap<Integer, QuantityUnit> getQuantityUnitHashMap() {
    return quantityUnitHashMap;
  }

  public QuantityUnit getQuantityUnitFromId(int id) {
    return quantityUnitHashMap.get(id);
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
    private final StockOverviewFragmentArgs args;

    public MealPlanViewModelFactory(
        Application application,
        StockOverviewFragmentArgs args
    ) {
      this.application = application;
      this.args = args;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new MealPlanViewModel(application, args);
    }
  }
}
