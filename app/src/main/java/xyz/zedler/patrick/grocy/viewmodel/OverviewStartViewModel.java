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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.preference.PreferenceManager;

import com.android.volley.VolleyError;

import java.util.ArrayList;
import java.util.HashMap;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.MissingItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.repository.OverviewStartRepository;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class OverviewStartViewModel extends BaseViewModel {

    private static final String TAG = OverviewStartViewModel.class.getSimpleName();

    private final SharedPreferences sharedPrefs;
    private final DownloadHelper dlHelper;
    private final OverviewStartRepository repository;

    private final MutableLiveData<Boolean> isLoadingLive;
    private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
    private final MutableLiveData<Boolean> offlineLive;

    private final MutableLiveData<ArrayList<StockItem>> stockItemsLive;
    private final MutableLiveData<ArrayList<ShoppingListItem>> shoppingListItemsLive;
    private final MutableLiveData<ArrayList<Product>> productsLive;
    private final MutableLiveData<Integer> itemsDueNextCountLive;
    private final MutableLiveData<Integer> itemsOverdueCountLive;
    private final MutableLiveData<Integer> itemsExpiredCountLive;
    private final MutableLiveData<Integer> itemsMissingCountLive;
    private final MutableLiveData<Integer> itemsMissingShoppingListCountLive;
    private final LiveData<String> stockDescriptionTextLive;
    private final LiveData<String> stockDescriptionDueNextTextLive;
    private final LiveData<String> stockDescriptionOverdueTextLive;
    private final LiveData<String> stockDescriptionExpiredTextLive;
    private final LiveData<String> stockDescriptionMissingTextLive;
    private final LiveData<String> stockDescriptionMissingShoppingListTextLive;
    private final LiveData<String> shoppingListDescriptionTextLive;
    private final LiveData<String> masterDataDescriptionTextLive;
    private ArrayList<StockItem> stockItemsTemp;
    private ArrayList<StockItem> dueItemsTemp;
    private ArrayList<StockItem> overdueItemsTemp;
    private ArrayList<StockItem> expiredItemsTemp;
    private ArrayList<MissingItem> missingItemsTemp;

    private DownloadHelper.Queue currentQueueLoading;
    private final boolean debug;

    public OverviewStartViewModel(@NonNull Application application) {
        super(application);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        isLoadingLive = new MutableLiveData<>(false);
        dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
        repository = new OverviewStartRepository(application);

        infoFullscreenLive = new MutableLiveData<>();
        offlineLive = new MutableLiveData<>(false);
        stockItemsLive = new MutableLiveData<>();
        itemsDueNextCountLive = new MutableLiveData<>();
        itemsOverdueCountLive = new MutableLiveData<>();
        itemsExpiredCountLive = new MutableLiveData<>();
        itemsMissingCountLive = new MutableLiveData<>();
        itemsMissingShoppingListCountLive = new MutableLiveData<>();
        shoppingListItemsLive = new MutableLiveData<>();
        productsLive = new MutableLiveData<>();

        stockDescriptionTextLive = Transformations.map(
                stockItemsLive,
                stockItems -> {
                    if(stockItems == null) return null;
                    int products = stockItems.size();
                    double value = 0;
                    for(StockItem stockItem : stockItems) {
                        if(stockItem.isItemMissing() && !stockItem.isItemMissingAndPartlyInStock()) {
                            products--;
                            continue;
                        }
                        value += stockItem.getValueDouble();
                    }
                    if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_PRICE_TRACKING)) {
                        return getResources().getQuantityString(
                                R.plurals.description_overview_stock_value,
                                products, products,
                                NumUtil.trim(value),
                                sharedPrefs.getString(Constants.PREF.CURRENCY, "")
                        );
                    } else {
                        return getResources().getQuantityString(
                                R.plurals.description_overview_stock,
                                products, products
                        );
                    }
                }
        );
        stockDescriptionDueNextTextLive = Transformations.map(
                itemsDueNextCountLive,
                count -> {
                    if(count == null) return null;
                    return getResources().getQuantityString(
                            R.plurals.description_overview_stock_due_soon,
                            count, count, 5
                    );
                }
        );
        stockDescriptionOverdueTextLive = Transformations.map(
                itemsOverdueCountLive,
                count -> {
                    if(count == null) return null;
                    return getResources().getQuantityString(
                            R.plurals.description_overview_stock_overdue,
                            count, count
                    );
                }
        );
        stockDescriptionExpiredTextLive = Transformations.map(
                itemsExpiredCountLive,
                count -> {
                    if(count == null) return null;
                    return getResources().getQuantityString(
                            R.plurals.description_overview_stock_expired,
                            count, count
                    );
                }
        );
        stockDescriptionMissingTextLive = Transformations.map(
                itemsMissingCountLive,
                count -> {
                    if(count == null) return null;
                    return getResources().getQuantityString(
                            R.plurals.description_overview_stock_missing,
                            count, count
                    );
                }
        );
        stockDescriptionMissingShoppingListTextLive = Transformations.map(
                itemsMissingShoppingListCountLive,
                count -> {
                    if(count == null) return null;
                    @PluralsRes int string;
                    if(isFeatureEnabled(Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS)) {
                        string = R.plurals.description_overview_stock_missing_shopping_list_multi;
                    } else {
                        string = R.plurals.description_overview_stock_missing_shopping_list_single;
                    }
                    return getResources().getQuantityString(string, count, count);
                }
        );
        shoppingListDescriptionTextLive = Transformations.map(
                shoppingListItemsLive,
                shoppingListItems -> {
                    if(shoppingListItems == null) return null;
                    int size = shoppingListItems.size();
                    if(isFeatureEnabled(Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS)) {
                        return getResources().getQuantityString(
                                R.plurals.description_overview_shopping_list_multi, size, size
                        );
                    } else {
                        return getResources().getQuantityString(
                                R.plurals.description_overview_shopping_list, size, size
                        );
                    }
                }
        );
        masterDataDescriptionTextLive = Transformations.map(
                productsLive,
                products -> {
                    if(products == null) return null;
                    int size = products.size();
                    return getResources().getQuantityString(
                            R.plurals.description_overview_master_data, size, size
                    );
                }
        );
    }

    public void loadFromDatabase(boolean downloadAfterLoading) {
        repository.loadFromDatabase(
                (stockItems, shoppingListItems, products) -> {
                    this.stockItemsLive.setValue(stockItems);
                    this.shoppingListItemsLive.setValue(shoppingListItems);
                    this.productsLive.setValue(products);

                    ArrayList<Integer> shoppingListItemsProductIds = new ArrayList<>();
                    for(ShoppingListItem item : shoppingListItems) {
                        if(!item.hasProduct()) continue;
                        shoppingListItemsProductIds.add(item.getProductIdInt());
                    }

                    int itemsDueCount = 0;
                    int itemsOverdueCount = 0;
                    int itemsExpiredCount = 0;
                    int itemsMissingCount = 0;
                    int missingItemsOnShoppingListCount = 0;
                    for(StockItem stockItem : stockItems) {
                        if(stockItem.isItemDue()) itemsDueCount++;
                        if(stockItem.isItemOverdue()) itemsOverdueCount++;
                        if(stockItem.isItemExpired()) itemsExpiredCount++;
                        if(stockItem.isItemMissing()) {
                            itemsMissingCount++;
                            if(shoppingListItemsProductIds.contains(stockItem.getProductId())) {
                                missingItemsOnShoppingListCount++;
                            }
                        }
                    }
                    itemsDueNextCountLive.setValue(itemsDueCount);
                    itemsOverdueCountLive.setValue(itemsOverdueCount);
                    itemsExpiredCountLive.setValue(itemsExpiredCount);
                    itemsMissingCountLive.setValue(itemsMissingCount);
                    itemsMissingShoppingListCountLive.setValue(missingItemsOnShoppingListCount);

                    if(downloadAfterLoading) downloadData();
                }
        );
    }

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
            dlHelper.getTimeDbChanged(this::downloadData, () -> onDownloadError(null));
            return;
        }

        DownloadHelper.OnQueueEmptyListener onQueueEmptyListener = () -> {
            HashMap<Integer, StockItem> stockItemHashMap = new HashMap<>();
            for(StockItem stockItem : stockItemsTemp) {
                stockItemHashMap.put(stockItem.getProductId(), stockItem);
            }

            for(StockItem stockItemDue : dueItemsTemp) {
                StockItem stockItem = stockItemHashMap.get(stockItemDue.getProductId());
                if(stockItem == null) continue;
                stockItem.setItemDue(true);
            }
            for(StockItem stockItemOverdue : overdueItemsTemp) {
                StockItem stockItem = stockItemHashMap.get(stockItemOverdue.getProductId());
                if(stockItem == null) continue;
                stockItem.setItemOverdue(true);
            }
            for(StockItem stockItemExpired : expiredItemsTemp) {
                StockItem stockItem = stockItemHashMap.get(stockItemExpired.getProductId());
                if(stockItem == null) continue;
                stockItem.setItemExpired(true);
            }

            ArrayList<Integer> shoppingListItemsProductIds = new ArrayList<>();
            if(shoppingListItemsLive.getValue() != null) {
                for(ShoppingListItem item : shoppingListItemsLive.getValue()) {
                    if(!item.hasProduct()) continue;
                    shoppingListItemsProductIds.add(item.getProductIdInt());
                }
            }

            DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);

            int missingItemsOnShoppingListCount = 0;

            for(MissingItem missingItem : missingItemsTemp) {

                if(shoppingListItemsProductIds.contains(missingItem.getId())) {
                    missingItemsOnShoppingListCount++;
                }
                StockItem missingStockItem = stockItemHashMap.get(missingItem.getId());
                if(missingStockItem != null) {
                    missingStockItem.setItemMissing(true);
                    missingStockItem.setItemMissingAndPartlyInStock(true);
                    continue;
                }
                queue.append(dlHelper.getProductDetails(missingItem.getId(), productDetails -> {
                    StockItem stockItem = new StockItem(productDetails);
                    stockItem.setItemMissing(true);
                    stockItem.setItemMissingAndPartlyInStock(false);
                    stockItemsTemp.add(stockItem);
                }));
            }
            itemsMissingShoppingListCountLive.setValue(missingItemsOnShoppingListCount);
            if(queue.getSize() == 0) {
                onQueueEmpty();
                return;
            }
            queue.start();
        };

        DownloadHelper.Queue queue = dlHelper.newQueue(onQueueEmptyListener, this::onDownloadError);
        queue.append(
                dlHelper.updateStockItems(dbChangedTime, stockItems -> stockItemsTemp = stockItems),
                dlHelper.updateShoppingListItems(dbChangedTime, this.shoppingListItemsLive::setValue),
                dlHelper.updateProducts(dbChangedTime, this.productsLive::setValue),
                dlHelper.updateVolatile(dbChangedTime, (due, overdue, expired, missing) -> {
                    this.dueItemsTemp = due;
                    itemsDueNextCountLive.setValue(due.size());
                    this.overdueItemsTemp = overdue;
                    itemsOverdueCountLive.setValue(overdue.size());
                    this.expiredItemsTemp = expired;
                    itemsExpiredCountLive.setValue(expired.size());
                    this.missingItemsTemp = missing;
                    itemsMissingCountLive.setValue(missing.size());
                }));
        if(queue.isEmpty()) return;

        currentQueueLoading = queue;
        queue.start();
    }

    public void downloadData() {
        downloadData(null);
    }

    public void downloadDataForceUpdate() {
        SharedPreferences.Editor editPrefs = sharedPrefs.edit();
        editPrefs.putString(Constants.PREF.DB_LAST_TIME_STOCK_ITEMS, null);
        editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCTS, null);
        editPrefs.putString(Constants.PREF.DB_LAST_TIME_SHOPPING_LIST_ITEMS, null);
        editPrefs.putString(Constants.PREF.DB_LAST_TIME_VOLATILE, null);
        editPrefs.apply();
        downloadData();
    }

    private void onQueueEmpty() {
        if(isOffline()) setOfflineLive(false);
        infoFullscreenLive.setValue(null);
        repository.updateDatabase(
                stockItemsTemp,
                this.shoppingListItemsLive.getValue(),
                this.productsLive.getValue(),
                () -> this.stockItemsLive.setValue(stockItemsTemp)
        );
    }

    private void onDownloadError(@Nullable VolleyError error) {
        if(debug) Log.e(TAG, "onError: VolleyError: " + error);
        String exact = error == null ? null : error.getLocalizedMessage();
        infoFullscreenLive.setValue(
                new InfoFullscreen(InfoFullscreen.ERROR_NETWORK, exact, () -> {
                    infoFullscreenLive.setValue(null);
                    downloadDataForceUpdate();
                })
        );
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

    public LiveData<String> getStockDescriptionTextLive() {
        return stockDescriptionTextLive;
    }

    public LiveData<String> getStockDescriptionDueNextTextLive() {
        return stockDescriptionDueNextTextLive;
    }

    public LiveData<String> getStockDescriptionOverdueTextLive() {
        return stockDescriptionOverdueTextLive;
    }

    public LiveData<String> getStockDescriptionExpiredTextLive() {
        return stockDescriptionExpiredTextLive;
    }

    public LiveData<String> getStockDescriptionMissingTextLive() {
        return stockDescriptionMissingTextLive;
    }

    public LiveData<String> getStockDescriptionMissingShoppingListTextLive() {
        return stockDescriptionMissingShoppingListTextLive;
    }

    public LiveData<String> getShoppingListDescriptionTextLive() {
        return shoppingListDescriptionTextLive;
    }

    public LiveData<String> getMasterDataDescriptionTextLive() {
        return masterDataDescriptionTextLive;
    }

    public void setCurrentQueueLoading(DownloadHelper.Queue queueLoading) {
        currentQueueLoading = queueLoading;
    }

    public boolean isFeatureEnabled(String pref) {
        if(pref == null) return true;
        return sharedPrefs.getBoolean(pref, true);
    }

    public boolean getBeginnerModeEnabled() {
        return sharedPrefs.getBoolean(
                Constants.SETTINGS.BEHAVIOR.BEGINNER_MODE,
                Constants.SETTINGS_DEFAULT.BEHAVIOR.BEGINNER_MODE
        );
    }

    public boolean getIsDemoInstance() {
        String server = sharedPrefs.getString(Constants.PREF.SERVER_URL, null);
        return server != null && server.contains("grocy.info");
    }

    @Override
    protected void onCleared() {
        dlHelper.destroy();
        super.onCleared();
    }
}
