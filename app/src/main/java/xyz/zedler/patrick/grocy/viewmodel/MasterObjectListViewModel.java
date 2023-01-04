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
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import com.android.volley.VolleyError;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.BoundExtractedResult;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterProductBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.HorizontalFilterBarMulti;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.repository.MasterObjectListRepository;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.util.LocaleUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.ObjectUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;
import xyz.zedler.patrick.grocy.web.NetworkQueue;

public class MasterObjectListViewModel extends BaseViewModel {

  private static final String TAG = MasterObjectListViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final GrocyApi grocyApi;
  private final MasterObjectListRepository repository;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
  private final MutableLiveData<Boolean> offlineLive;
  private final MutableLiveData<ArrayList<Object>> displayedItemsLive;

  private List<?> objects;
  private List<ProductGroup> productGroups;
  private List<QuantityUnit> quantityUnits;
  private List<Location> locations;

  private NetworkQueue currentQueueLoading;
  private final HorizontalFilterBarMulti horizontalFilterBarMulti;
  private boolean sortAscending;
  private String search;
  private final boolean debug;
  private final String entity;

  public MasterObjectListViewModel(@NonNull Application application, String entity) {
    super(application);

    this.entity = entity;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
    debug = PrefsUtil.isDebuggingEnabled(sharedPrefs);

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    grocyApi = new GrocyApi(getApplication());
    repository = new MasterObjectListRepository(application);

    infoFullscreenLive = new MutableLiveData<>();
    offlineLive = new MutableLiveData<>(false);
    displayedItemsLive = new MutableLiveData<>();

    objects = new ArrayList<>();

    horizontalFilterBarMulti = new HorizontalFilterBarMulti(this::displayItems);
    sortAscending = true;
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      switch (entity) {
        case ENTITY.PRODUCTS:
          this.objects = data.getProducts();
          this.productGroups = data.getProductGroups();
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

      displayItems();
      if (downloadAfterLoading) {
        downloadData();
      }
    });
  }

  public void downloadData(@Nullable String dbChangedTime) {
    if (currentQueueLoading != null) {
      currentQueueLoading.reset(true);
      currentQueueLoading = null;
    }
    if (isOffline()) { // skip downloading
      isLoadingLive.setValue(false);
      return;
    }
    if (dbChangedTime == null) {
      dlHelper.getTimeDbChanged(
          this::downloadData,
          () -> onDownloadError(null)
      );
      return;
    }

    NetworkQueue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
    if (entity.equals(GrocyApi.ENTITY.STORES)) {
      queue.append(dlHelper.updateStores(dbChangedTime, stores -> objects = stores));
    }
    if ((entity.equals(GrocyApi.ENTITY.LOCATIONS) || entity.equals(GrocyApi.ENTITY.PRODUCTS))) {
      queue.append(dlHelper.updateLocations(dbChangedTime, locations -> {
        if (entity.equals(GrocyApi.ENTITY.LOCATIONS)) {
          objects = locations;
        } else {
          this.locations = locations;
        }
      }));
    }
    if ((entity.equals(GrocyApi.ENTITY.PRODUCT_GROUPS) || entity
        .equals(GrocyApi.ENTITY.PRODUCTS))) {
      queue.append(dlHelper.updateProductGroups(dbChangedTime, productGroups -> {
        if (entity.equals(GrocyApi.ENTITY.PRODUCT_GROUPS)) {
          objects = productGroups;
        } else {
          this.productGroups = productGroups;
        }
      }));
    }
    if ((entity.equals(GrocyApi.ENTITY.QUANTITY_UNITS) || entity
        .equals(GrocyApi.ENTITY.PRODUCTS))) {
      queue.append(dlHelper.updateQuantityUnits(dbChangedTime, quantityUnits -> {
        if (entity.equals(GrocyApi.ENTITY.QUANTITY_UNITS)) {
          objects = quantityUnits;
        } else {
          this.quantityUnits = quantityUnits;
        }
      }));
    }
    if ((entity.equals(ENTITY.TASK_CATEGORIES))) {
      queue.append(dlHelper.updateTaskCategories(
          dbChangedTime,
          taskCategories -> this.objects = taskCategories
      ));
    }
    if (entity.equals(GrocyApi.ENTITY.PRODUCTS)) {
      queue.append(dlHelper.updateProducts(
          dbChangedTime,
          products -> objects = products)
      );
    }

    if (queue.isEmpty()) {
      return;
    }

    currentQueueLoading = queue;
    queue.start();
  }

  public void downloadData() {
    downloadData(null);
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    switch (entity) {
      case GrocyApi.ENTITY.STORES:
        editPrefs.putString(Constants.PREF.DB_LAST_TIME_STORES, null);
        break;
      case GrocyApi.ENTITY.LOCATIONS:
        editPrefs.putString(Constants.PREF.DB_LAST_TIME_LOCATIONS, null);
        break;
      case GrocyApi.ENTITY.PRODUCT_GROUPS:
        editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCT_GROUPS, null);
        break;
      case GrocyApi.ENTITY.QUANTITY_UNITS:
        editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
        break;
      case GrocyApi.ENTITY.TASK_CATEGORIES:
        editPrefs.putString(Constants.PREF.DB_LAST_TIME_TASK_CATEGORIES, null);
        break;
      default:  // products
        editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCTS, null);
        editPrefs.putString(Constants.PREF.DB_LAST_TIME_LOCATIONS, null);
        editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCT_GROUPS, null);
        editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
        break;
    }
    editPrefs.apply();
    downloadData();
  }

  private void onQueueEmpty() {
    if (isOffline()) {
      setOfflineLive(false);
    }
    displayItems();
  }

  private void onDownloadError(@Nullable VolleyError error) {
    if (debug) {
      Log.e(TAG, "onError: VolleyError: " + error);
    }
    showMessage(getString(R.string.msg_no_connection));
    if (!isOffline()) {
      setOfflineLive(true);
    }
    displayItems(); // maybe objects can be loaded partially
  }

  public void displayItems() {
    // search items
    ArrayList<Object> searchedItems;
    if (search != null && !search.isEmpty()) {

      ArrayList<Object> searchResultsFuzzy = new ArrayList<>(objects.size());
      List results = FuzzySearch.extractSorted(
          search,
          objects,
          item -> {
            String name = ObjectUtil.getObjectName(item, entity);
            return name != null ? name.toLowerCase() : "";
          },
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

      sortObjectsByName(searchedItems);

      for (Object object : searchResultsFuzzy) {
        if (objectIdsInList.contains(ObjectUtil.getObjectId(object, entity))) {
          continue;
        }
        searchedItems.add(object);
      }
    } else {
      searchedItems = new ArrayList<>(objects);
      sortObjectsByName(searchedItems);
    }

    // filter items
    ArrayList<Object> filteredItems;
    if (entity.equals(GrocyApi.ENTITY.PRODUCTS) && horizontalFilterBarMulti.areFiltersActive()) {
      filteredItems = new ArrayList<>();
      HorizontalFilterBarMulti.Filter filter = horizontalFilterBarMulti
          .getFilter(HorizontalFilterBarMulti.PRODUCT_GROUP);
      for (Object object : searchedItems) {
        if (!NumUtil.isStringInt(((Product) object).getProductGroupId())) {
          continue;
        }
        int productGroupId = Integer.parseInt(((Product) object).getProductGroupId());
        if (productGroupId == filter.getObjectId()) {
          filteredItems.add(object);
        }
      }
    } else {
      filteredItems = searchedItems;
    }

    displayedItemsLive.setValue(filteredItems);
  }

  public void sortObjectsByName(ArrayList<Object> objects) {
    if (objects == null) {
      return;
    }

    Locale locale = LocaleUtil.getLocale();

    Collections.sort(objects, (item1, item2) -> {
      String name1 = ObjectUtil.getObjectName(sortAscending ? item1 : item2, entity);
      String name2 = ObjectUtil.getObjectName(sortAscending ? item2 : item1, entity);
      if (name1 == null || name2 == null) {
        return 0;
      }

      return Collator.getInstance(locale).compare(
              name1.toLowerCase(locale),
              name2.toLowerCase(locale));
    });
  }

  public void showProductBottomSheet(Product product) {
    if (product == null) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putParcelable(Constants.ARGUMENT.PRODUCT, product);
    bundle.putParcelable(
        Constants.ARGUMENT.LOCATION,
        getLocation(product.getLocationIdInt())
    );
    bundle.putParcelable(
        Constants.ARGUMENT.QUANTITY_UNIT_PURCHASE,
        getQuantityUnit(product.getQuIdPurchaseInt())
    );
    bundle.putParcelable(
        Constants.ARGUMENT.QUANTITY_UNIT_STOCK,
        getQuantityUnit(product.getQuIdStockInt())
    );
    ProductGroup productGroup = NumUtil.isStringInt(product.getProductGroupId())
        ? getProductGroup(Integer.parseInt(product.getProductGroupId()))
        : null;
    bundle.putParcelable(Constants.ARGUMENT.PRODUCT_GROUP, productGroup);
    showBottomSheet(new MasterProductBottomSheet(), bundle);
  }

  public void deleteObjectSafely(Object object) {
    String objectName = ObjectUtil.getObjectName(object, entity);
    int objectId = ObjectUtil.getObjectId(object, entity);
    showMasterDeleteBottomSheet(entity, objectName, objectId);
  }

  public void deleteObject(int objectId) {
    dlHelper.delete(
        grocyApi.getObject(entity, objectId),
        response -> downloadData(),
        error -> showMessage(getString(R.string.error_undefined))
    );
  }

  private void showMasterDeleteBottomSheet(String entity, String objectName, int objectId) {
    Bundle argsBundle = new Bundle();
    argsBundle.putString(Constants.ARGUMENT.ENTITY, entity);
    argsBundle.putInt(Constants.ARGUMENT.OBJECT_ID, objectId);
    argsBundle.putString(Constants.ARGUMENT.OBJECT_NAME, objectName);
    showBottomSheet(new MasterDeleteBottomSheet(), argsBundle);
  }

  @Nullable
  private Location getLocation(int id) {
    if (locations == null) {
      return null;
    }
    for (Location location : locations) {
      if (location.getId() == id) {
        return location;
      }
    }
    return null;
  }

  @Nullable
  private ProductGroup getProductGroup(int id) {
    if (productGroups == null) {
      return null;
    }
    for (ProductGroup productGroup : productGroups) {
      if (productGroup.getId() == id) {
        return productGroup;
      }
    }
    return null;
  }

  @Nullable
  private QuantityUnit getQuantityUnit(int id) {
    if (quantityUnits == null) {
      return null;
    }
    for (QuantityUnit quantityUnit : quantityUnits) {
      if (quantityUnit.getId() == id) {
        return quantityUnit;
      }
    }
    return null;
  }

  @Nullable
  public List<ProductGroup> getProductGroups() {
    return productGroups;
  }

  public void setSortAscending(boolean ascending) {
    this.sortAscending = ascending;
    displayItems();
  }

  public boolean isSortAscending() {
    return sortAscending;
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

  public HorizontalFilterBarMulti getHorizontalFilterBarMulti() {
    return horizontalFilterBarMulti;
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

  public void setCurrentQueueLoading(NetworkQueue queueLoading) {
    currentQueueLoading = queueLoading;
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
