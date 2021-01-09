package xyz.zedler.patrick.grocy.viewmodel;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.android.volley.VolleyError;

import java.util.ArrayList;
import java.util.Collections;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.BaseBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterLocationBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterProductBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterProductGroupBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterQuantityUnitBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterStoreBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.repository.MasterObjectListRepository;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.ObjectUtil;

public class MasterObjectListViewModel extends AndroidViewModel {

    private static final String TAG = MasterObjectListViewModel.class.getSimpleName();

    private final SharedPreferences sharedPrefs;
    private final DownloadHelper dlHelper;
    private final GrocyApi grocyApi;
    private final EventHandler eventHandler;
    private final MasterObjectListRepository repository;

    private final MutableLiveData<Boolean> isLoadingLive;
    private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
    private final MutableLiveData<Boolean> offlineLive;
    private final MutableLiveData<ArrayList<Object>> displayedItemsLive;

    private ArrayList<Object> objects;
    private ArrayList<ProductGroup> productGroups;
    private ArrayList<QuantityUnit> quantityUnits;
    private ArrayList<Location> locations;

    private DownloadHelper.Queue currentQueueLoading;
    private boolean sortAscending;
    private String search;
    private final boolean debug;
    private final String entity;

    public MasterObjectListViewModel(@NonNull Application application, String entity) {
        super(application);

        this.entity = entity;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        isLoadingLive = new MutableLiveData<>(false);
        dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
        grocyApi = new GrocyApi(getApplication());
        eventHandler = new EventHandler();
        repository = new MasterObjectListRepository(application, entity);

        infoFullscreenLive = new MutableLiveData<>();
        offlineLive = new MutableLiveData<>(false);
        displayedItemsLive = new MutableLiveData<>();

        objects = new ArrayList<>();

        sortAscending = true;
    }

    public void loadFromDatabase(boolean downloadAfterLoading) {
        if(!entity.equals(GrocyApi.ENTITY.PRODUCTS)) {
            repository.loadFromDatabase(objects -> {
                this.objects = objects;
                displayItems();
                if(downloadAfterLoading) downloadData();
            });
        } else {
            repository.loadFromDatabaseProducts((objects, productGroups, quantityUnits, locations) -> {
                this.objects = objects;
                this.productGroups = productGroups;
                this.quantityUnits = quantityUnits;
                this.locations = locations;
                displayItems();
                if(downloadAfterLoading) downloadData();
            });
        }
    }

    private String getLastTime(String sharedPref) {
        return sharedPrefs.getString(sharedPref, null);
    }

    @SuppressWarnings("unchecked")
    public void downloadData(@Nullable String dbChangedTime) {
        if(currentQueueLoading != null) {
            currentQueueLoading.reset(true);
            currentQueueLoading = null;
        }
        if(isOffline()) { // skip downloading
            isLoadingLive.setValue(false);
            return;
        }
        if(dbChangedTime == null) {
            dlHelper.getTimeDbChanged(
                    (DownloadHelper.OnStringResponseListener) this::downloadData,
                    () -> onDownloadError(null)
            );
            return;
        }

        // get last offline db-changed-time values
        String lastTimeStores = getLastTime(Constants.PREF.DB_LAST_TIME_STORES);
        String lastTimeLocations = getLastTime(Constants.PREF.DB_LAST_TIME_LOCATIONS);
        String lastTimeProductGroups = getLastTime(Constants.PREF.DB_LAST_TIME_PRODUCT_GROUPS);
        String lastTimeQuantityUnits = getLastTime(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS);
        String lastTimeProducts = getLastTime(Constants.PREF.DB_LAST_TIME_PRODUCTS);

        SharedPreferences.Editor editPrefs = sharedPrefs.edit();
        DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
        if(entity.equals(GrocyApi.ENTITY.STORES)
                && (lastTimeStores == null || !lastTimeStores.equals(dbChangedTime))) {
            queue.append(dlHelper.getStores(stores -> {
                objects = (ArrayList<Object>) (Object) stores;
                editPrefs.putString(Constants.PREF.DB_LAST_TIME_STORES, dbChangedTime);
                editPrefs.apply();
            }));
        } else if(debug) Log.i(TAG, "downloadData: skipped Stores download");
        if((entity.equals(GrocyApi.ENTITY.LOCATIONS) || entity.equals(GrocyApi.ENTITY.PRODUCTS))
                && (lastTimeLocations == null || !lastTimeLocations.equals(dbChangedTime))) {
            queue.append(dlHelper.getLocations(locations -> {
                objects = (ArrayList<Object>) (Object) locations;
                editPrefs.putString(Constants.PREF.DB_LAST_TIME_LOCATIONS, dbChangedTime);
                editPrefs.apply();
            }));
        } else if(debug) Log.i(TAG, "downloadData: skipped Locations download");
        if((entity.equals(GrocyApi.ENTITY.PRODUCT_GROUPS) || entity.equals(GrocyApi.ENTITY.PRODUCTS))
                && (lastTimeProductGroups == null || !lastTimeProductGroups.equals(dbChangedTime))) {
            queue.append(dlHelper.getProductGroups(productGroups -> {
                if(entity.equals(GrocyApi.ENTITY.PRODUCT_GROUPS)) {
                    objects = (ArrayList<Object>) (Object) productGroups;
                } else {
                    this.productGroups = productGroups;
                }
                editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCT_GROUPS, dbChangedTime);
                editPrefs.apply();
            }));
        } else if(debug) Log.i(TAG, "downloadData: skipped ProductGroups download");
        if((entity.equals(GrocyApi.ENTITY.QUANTITY_UNITS) || entity.equals(GrocyApi.ENTITY.PRODUCTS))
                && (lastTimeQuantityUnits == null || !lastTimeQuantityUnits.equals(dbChangedTime))) {
            queue.append(dlHelper.getQuantityUnits(quantityUnits -> {
                if(entity.equals(GrocyApi.ENTITY.QUANTITY_UNITS)) {
                    objects = (ArrayList<Object>) (Object) quantityUnits;
                } else {
                    this.quantityUnits = quantityUnits;
                }
                editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, dbChangedTime);
                editPrefs.apply();
            }));
        } else if(debug) Log.i(TAG, "downloadData: skipped QuantityUnits download");
        if(entity.equals(GrocyApi.ENTITY.PRODUCTS)
                && (lastTimeProducts == null || !lastTimeProducts.equals(dbChangedTime))) {
            queue.append(dlHelper.getProducts(products -> {
                objects = (ArrayList<Object>) (Object) products;
                editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCTS, dbChangedTime);
                editPrefs.apply();
            }));
        } else if(debug) Log.i(TAG, "downloadData: skipped Products download");

        if(queue.isEmpty()) return;

        currentQueueLoading = queue;
        queue.start();
    }

    public void downloadData() {
        downloadData(null);
    }

    private void onQueueEmpty() {
        if(isOffline()) setOfflineLive(false);
        if(!entity.equals(GrocyApi.ENTITY.PRODUCTS)) {
            repository.updateDatabase(objects, this::displayItems);
        } else {
            repository.updateDatabaseProducts(
                    objects, productGroups, quantityUnits, locations, this::displayItems
            );
        }
    }

    private void onDownloadError(@Nullable VolleyError error) {
        if(debug) Log.e(TAG, "onError: VolleyError: " + error);
        showMessage(getString(R.string.msg_no_connection));
        if(!isOffline()) setOfflineLive(true);
        displayItems(); // maybe objects can be loaded partially
    }

    public void displayItems() {
        // filter items
        ArrayList<Object> filteredItems;
        if(search != null && !search.isEmpty()) {
            filteredItems = new ArrayList<>();
            for(Object object : objects) {
                String name = ObjectUtil.getObjectName(object, entity);
                String description = ObjectUtil.getObjectDescription(object, entity);
                name = name != null ? name.toLowerCase() : "";
                description = description != null ? description.toLowerCase() : "";
                if(name.contains(search) || description.contains(search)) {
                    filteredItems.add(object);
                }
            }
        } else {
            filteredItems = new ArrayList<>(objects);
        }

        // sort items
        sortObjectsByName(filteredItems);
        displayedItemsLive.setValue(filteredItems);
    }

    public void sortObjectsByName(ArrayList<Object> objects) {
        if(objects == null) return;
        Collections.sort(objects, (item1, item2) -> {
            String name1 = ObjectUtil.getObjectName(sortAscending ? item1 : item2, entity);
            String name2 = ObjectUtil.getObjectName(sortAscending ? item2 : item1, entity);
            if(name1 == null || name2 == null) return 0;
            return name1.toLowerCase().compareTo(name2.toLowerCase());
        });
    }

    public void showObjectBottomSheetOfDisplayedItem(int position) {
        Object object = getDisplayedItem(position);
        if(object == null) return;
        Bundle bundle = new Bundle();
        switch (entity) {
            case GrocyApi.ENTITY.QUANTITY_UNITS:
                bundle.putParcelable(Constants.ARGUMENT.QUANTITY_UNIT, (QuantityUnit) object);
                showBottomSheet(new MasterQuantityUnitBottomSheet(), bundle);
                break;
            case GrocyApi.ENTITY.PRODUCT_GROUPS:
                bundle.putParcelable(Constants.ARGUMENT.PRODUCT_GROUP, (ProductGroup) object);
                showBottomSheet(new MasterProductGroupBottomSheet(), bundle);
                break;
            case GrocyApi.ENTITY.LOCATIONS:
                bundle.putParcelable(Constants.ARGUMENT.LOCATION, (Location) object);
                showBottomSheet(new MasterLocationBottomSheet(), bundle);
                break;
            case GrocyApi.ENTITY.STORES:
                bundle.putParcelable(Constants.ARGUMENT.STORE, (Store) object);
                showBottomSheet(new MasterStoreBottomSheet(), bundle);
                break;
            case GrocyApi.ENTITY.PRODUCTS:
                Product product = (Product) object;
                bundle.putParcelable(Constants.ARGUMENT.PRODUCT, product);
                bundle.putParcelable(
                        Constants.ARGUMENT.LOCATION,
                        getLocation(product.getLocationIdInt())
                );
                bundle.putParcelable(
                        Constants.ARGUMENT.QUANTITY_UNIT_PURCHASE,
                        getQuantityUnit(product.getQuIdPurchase())
                );
                bundle.putParcelable(
                        Constants.ARGUMENT.QUANTITY_UNIT_STOCK,
                        getQuantityUnit(product.getQuIdStock())
                );
                ProductGroup productGroup = NumUtil.isStringInt(product.getProductGroupId())
                        ? getProductGroup(Integer.parseInt(product.getProductGroupId()))
                        : null;
                bundle.putParcelable(Constants.ARGUMENT.PRODUCT_GROUP, productGroup);
                showBottomSheet(new MasterProductBottomSheet(), bundle);
        }
    }

    public void deleteObjectSafely(Object object) {
        String objectName = ObjectUtil.getObjectName(object, entity);
        int objectId = ObjectUtil.getObjectId(object, entity);
        int entityStrId;
        switch (entity) {
            case GrocyApi.ENTITY.PRODUCTS:
                entityStrId = R.string.property_product;
                break;
            case GrocyApi.ENTITY.QUANTITY_UNITS:
                entityStrId = R.string.property_quantity_unit;
                break;
            case GrocyApi.ENTITY.LOCATIONS:
                entityStrId = R.string.property_location;
                break;
            case GrocyApi.ENTITY.PRODUCT_GROUPS:
                entityStrId = R.string.property_product_group;
                break;
            default: // STORES
                entityStrId = R.string.property_store;
        }
        String entityText = getString(entityStrId);

        if(!entity.equals(GrocyApi.ENTITY.PRODUCTS)) {
            dlHelper.getProducts(products -> {
                for(Object p : products) {
                    Product product = (Product) p;
                    if(entity.equals(GrocyApi.ENTITY.QUANTITY_UNITS)
                            && product.getQuIdStock() != objectId
                            && product.getQuIdPurchase() != objectId
                            || entity.equals(GrocyApi.ENTITY.LOCATIONS)
                            && product.getLocationIdInt() == objectId
                            || entity.equals(GrocyApi.ENTITY.PRODUCT_GROUPS)
                            && NumUtil.isStringInt(product.getProductGroupId())
                            && Integer.parseInt(product.getProductGroupId()) == objectId
                            || entity.equals(GrocyApi.ENTITY.STORES)
                            && NumUtil.isStringInt(product.getStoreId())
                            && Integer.parseInt(product.getStoreId()) == objectId
                    ) {
                        showMessage(getString(R.string.msg_master_delete_usage, entityText));
                        return;
                    }
                }
                showMasterDeleteBottomSheet(entityText, objectName, objectId);
            }, error -> showMessage(getString(R.string.error_network)))
                    .perform(dlHelper.getUuid());
        } else { // PRODUCTS
            dlHelper.getProductDetails(ObjectUtil.getObjectId(object, entity), productDetails -> {
                if(productDetails != null && productDetails.getStockAmount() == 0) {
                    showMasterDeleteBottomSheet(entityText, objectName, objectId);
                } else {
                    showMessage(getString(R.string.msg_master_delete_stock));
                }
            }, error -> showMessage(getString(R.string.error_check_usage)))
                    .perform(dlHelper.getUuid());
        }
    }

    public void deleteObject(int objectId) {
        dlHelper.delete(
                grocyApi.getObject(entity, objectId),
                response -> downloadData(),
                error -> showMessage(getString(R.string.error_undefined))
        );
    }

    private void showMasterDeleteBottomSheet(String entityText, String objectName, int objectId) {
        Bundle argsBundle = new Bundle();
        argsBundle.putString(Constants.ARGUMENT.ENTITY_TEXT, entityText);
        argsBundle.putInt(Constants.ARGUMENT.OBJECT_ID, objectId);
        argsBundle.putString(Constants.ARGUMENT.OBJECT_NAME, objectName);
        showBottomSheet(new MasterDeleteBottomSheet(), argsBundle);
    }

    @Nullable
    private Object getDisplayedItem(int position) {
        ArrayList<Object> displayedItems = displayedItemsLive.getValue();
        if(displayedItems == null || position+1 > displayedItems.size()) return null;
        return displayedItems.get(position);
    }

    @Nullable
    private Location getLocation(int id) {
        if(locations == null) return null;
        for(Location location : locations) {
            if(location.getId() == id) return location;
        }
        return null;
    }

    @Nullable
    private ProductGroup getProductGroup(int id) {
        if(productGroups == null) return null;
        for(ProductGroup productGroup : productGroups) {
            if(productGroup.getId() == id) return productGroup;
        }
        return null;
    }

    @Nullable
    private QuantityUnit getQuantityUnit(int id) {
        if(quantityUnits == null) return null;
        for(QuantityUnit quantityUnit : quantityUnits) {
            if(quantityUnit.getId() == id) return quantityUnit;
        }
        return null;
    }

    @Nullable
    public ArrayList<ProductGroup> getProductGroups() {
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
        if(search != null) this.search = search.toLowerCase();
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

    public void setCurrentQueueLoading(DownloadHelper.Queue queueLoading) {
        currentQueueLoading = queueLoading;
    }

    private void showErrorMessage() {
        showMessage(getString(R.string.error_undefined));
    }

    private void showMessage(@NonNull String message) {
        showSnackbar(new SnackbarMessage(message));
    }

    private void showSnackbar(@NonNull SnackbarMessage snackbarMessage) {
        eventHandler.setValue(snackbarMessage);
    }

    private void showBottomSheet(BaseBottomSheet bottomSheet, Bundle bundle) {
        eventHandler.setValue(new BottomSheetEvent(bottomSheet, bundle));
    }

    private void sendEvent(int type) {
        eventHandler.setValue(new Event() {
            @Override
            public int getType() {return type;}
        });
    }

    private void sendEvent(int type, Bundle bundle) {
        eventHandler.setValue(new Event() {
            @Override
            public int getType() {return type;}

            @Override
            public Bundle getBundle() {return bundle;}
        });
    }

    @NonNull
    public EventHandler getEventHandler() {
        return eventHandler;
    }

    public boolean isFeatureEnabled(String pref) {
        if(pref == null) return true;
        return sharedPrefs.getBoolean(pref, true);
    }

    private String getString(@StringRes int resId) {
        return getApplication().getString(resId);
    }

    private String getString(@StringRes int resId, Object... formatArgs) {
        return getApplication().getString(resId, formatArgs);
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
