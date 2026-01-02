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
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.BoundExtractedResult;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheetArgs;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.FilterChipLiveData;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataProductGroup;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataSort;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataSort.SortOption;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.model.Userfield;
import xyz.zedler.patrick.grocy.repository.MasterObjectListRepository;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.ObjectUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;

public class MasterObjectListViewModel extends BaseViewModel {

  private static final String TAG = MasterObjectListViewModel.class.getSimpleName();

  public final static String SORT_NAME = "sort_name";
  public final static String SORT_CREATED_TIMESTAMP = "sort_created_timestamp";

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final MasterObjectListRepository repository;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<ArrayList<Object>> displayedItemsLive;
  private final FilterChipLiveDataProductGroup filterChipLiveDataProductGroup;
  private final FilterChipLiveDataSort filterChipLiveDataSort;

  private List<?> objects;
  private List<QuantityUnit> quantityUnits;
  private List<Location> locations;
  private HashMap<String, Userfield> userfieldHashMap = new HashMap<>();

  private String search;
  private final String entity;

  public MasterObjectListViewModel(@NonNull Application application, String entity) {
    super(application);

    this.entity = entity;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue, getOfflineLive());
    grocyApi = new GrocyApi(getApplication());
    repository = new MasterObjectListRepository(application);

    infoFullscreenLive = new MutableLiveData<>();
    displayedItemsLive = new MutableLiveData<>();
    filterChipLiveDataProductGroup = new FilterChipLiveDataProductGroup(
        getApplication(),
        this::updateItemsWithTopScroll
    );
    filterChipLiveDataSort = new FilterChipLiveDataSort(
        getApplication(),
        Constants.PREF.MASTER_OBJECTS_SORT_MODE,
        Constants.PREF.MASTER_OBJECTS_SORT_ASCENDING,
        this::updateItemsWithTopScroll,
        SORT_NAME,
        new SortOption(SORT_NAME, getString(R.string.property_name)),
        new SortOption(SORT_CREATED_TIMESTAMP, getString(R.string.property_created_timestamp))
    );

    objects = new ArrayList<>();
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      switch (entity) {
        case ENTITY.PRODUCTS:
          this.objects = data.getProducts();
          filterChipLiveDataProductGroup.setProductGroups(data.getProductGroups());
          this.quantityUnits = data.getQuantityUnits();
          this.locations = data.getLocations();
          break;
        case ENTITY.PRODUCT_GROUPS:
          this.objects = data.getProductGroups();
          break;
        case ENTITY.LOCATIONS:
          this.objects = data.getLocations();
          break;
        case ENTITY.QUANTITY_UNITS:
          this.objects = data.getQuantityUnits();
          break;
        case ENTITY.TASK_CATEGORIES:
          this.objects = data.getTaskCategories();
          break;
        default:
          this.objects = data.getStores();
          break;
      }
      userfieldHashMap = ArrayUtil.getUserfieldHashMap(data.getUserfields());
      filterChipLiveDataSort.setUserfields(data.getUserfields(), entity);

      displayItems();
      if (downloadAfterLoading) {
        downloadData(false);
      }
    }, error -> onError(error, TAG));
  }

  public void downloadData(boolean forceUpdate) {
    dlHelper.updateData(
        updated -> {
          if (updated) loadFromDatabase(false);
        }, error -> onError(error, TAG),
        forceUpdate,
        true,
        entity.equals(GrocyApi.ENTITY.STORES) ? Store.class : null,
        (entity.equals(GrocyApi.ENTITY.LOCATIONS) || entity.equals(GrocyApi.ENTITY.PRODUCTS))
            ? Location.class : null,
        (entity.equals(GrocyApi.ENTITY.PRODUCT_GROUPS) || entity.equals(GrocyApi.ENTITY.PRODUCTS))
            ? ProductGroup.class : null,
        (entity.equals(GrocyApi.ENTITY.QUANTITY_UNITS) || entity.equals(GrocyApi.ENTITY.PRODUCTS))
            ? QuantityUnit.class : null,
        entity.equals(ENTITY.TASK_CATEGORIES) ? TaskCategory.class : null,
        entity.equals(GrocyApi.ENTITY.PRODUCTS) ? Product.class : null,
        Userfield.class
    );
  }

  public void displayItems() {
    // search items
    ArrayList<Object> searchedItems;
    if (search != null && !search.isEmpty()) {

      ArrayList<Object> searchResultsFuzzy = new ArrayList<>(30);
      List results = FuzzySearch.extractTop(
          search,
          objects,
          item -> {
            String name = ObjectUtil.getObjectName(item, entity);
            return name != null ? name.toLowerCase() : "";
          },
          30,
          70
      );
      for (Object result : results) {
        searchResultsFuzzy.add(((BoundExtractedResult<?>) result).getReferent());
      }

      searchedItems = new ArrayList<>();
      ArrayList<Integer> objectIdsInList = new ArrayList<>();
      for (Object object : objects) {
        String name = ObjectUtil.getObjectName(object, entity);
        name = name != null ? name.toLowerCase() : "";
        if (name.contains(search)) {
          searchedItems.add(object);
          objectIdsInList.add(ObjectUtil.getObjectId(object, entity));
        }
      }

      sortObjects(searchedItems);

      for (Object object : searchResultsFuzzy) {
        if (objectIdsInList.contains(ObjectUtil.getObjectId(object, entity))) {
          continue;
        }
        searchedItems.add(object);
      }
    } else {
      searchedItems = new ArrayList<>(objects);
      sortObjects(searchedItems);
    }

    // filter items
    ArrayList<Object> filteredItems;
    if (entity.equals(GrocyApi.ENTITY.PRODUCTS) && filterChipLiveDataProductGroup.isActive()) {
      filteredItems = new ArrayList<>();
      for (Object object : searchedItems) {
        if (!NumUtil.isStringInt(((Product) object).getProductGroupId())) {
          continue;
        }
        int productGroupId = Integer.parseInt(((Product) object).getProductGroupId());
        if (productGroupId == filterChipLiveDataProductGroup.getSelectedId()) {
          filteredItems.add(object);
        }
      }
    } else {
      filteredItems = searchedItems;
    }

    displayedItemsLive.setValue(filteredItems);
  }

  private void updateItemsWithTopScroll() {
    displayItems();
    sendEvent(Event.SCROLL_UP);
  }

  private void sortObjects(ArrayList<Object> objects) {
    String sortMode = filterChipLiveDataSort.getSortMode();
    boolean isAscending = filterChipLiveDataSort.isSortAscending();
    if (sortMode.equals(SORT_NAME)) {
      SortUtil.sortObjectsByName(objects, entity, isAscending);
    } else if (sortMode.equals(SORT_CREATED_TIMESTAMP)) {
      SortUtil.sortObjectsByCreatedTimestamp(objects, entity, isAscending);
    } else if (sortMode.startsWith(Userfield.NAME_PREFIX)) {
      String userfieldName = sortMode.substring(Userfield.NAME_PREFIX.length());
      Userfield userfield = userfieldHashMap.get(userfieldName);
      SortUtil.sortObjectsByUserfieldValue(objects, entity, userfield, isAscending);
    }
  }

  public void showProductBottomSheet(Product product) {
    if (product == null) {
      return;
    }
    Bundle bundle = new ProductOverviewBottomSheetArgs.Builder()
        .setProduct(product)
        .setLocation(Location.getFromId(locations, product.getLocationIdInt()))
        .setQuantityUnitPurchase(QuantityUnit.getFromId(quantityUnits, product.getQuIdPurchaseInt()))
        .setQuantityUnitStock(QuantityUnit.getFromId(quantityUnits, product.getQuIdStockInt()))
        .setShowActions(false)
        .build().toBundle();
    showBottomSheet(new ProductOverviewBottomSheet(), bundle);
  }

  public void deleteObject(int objectId) {
    dlHelper.delete(
        grocyApi.getObject(entity, objectId),
        response -> downloadData(false),
        error -> showMessage(getString(R.string.error_undefined))
    );
  }

  public boolean isSearchActive() {
    return search != null;
  }

  public void setSearch(@Nullable String search) {
    this.search = search != null ? search.toLowerCase() : null;
    displayItems();
  }

  public void deleteSearch() {
    search = null;
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataProductGroup() {
    return () -> filterChipLiveDataProductGroup;
  }

  public FilterChipLiveData.Listener getFilterChipLiveDataSort() {
    return () -> filterChipLiveDataSort;
  }

  @NonNull
  public MutableLiveData<ArrayList<Object>> getDisplayedItemsLive() {
    return displayedItemsLive;
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

  public static class MasterObjectListViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final String entity;

    public MasterObjectListViewModelFactory(Application application, String entity) {
      this.application = application;
      this.entity = entity;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new MasterObjectListViewModel(application, entity);
    }
  }
}
