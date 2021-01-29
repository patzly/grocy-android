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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.preference.PreferenceManager;

import com.android.volley.VolleyError;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
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
    private final LiveData<String> stockDescriptionTextLive;
    private final LiveData<String> shoppingListDescriptionTextLive;

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
        shoppingListItemsLive = new MutableLiveData<>();

        stockDescriptionTextLive = Transformations.map(
                stockItemsLive,
                stockItems -> {
                    if(stockItems == null) return null;
                    int products = stockItems.size();
                    double value = 0;
                    for(StockItem stockItem : stockItems) {
                        value += stockItem.getValueDouble(); // TODO: Wrong calculation? End value is other than on web interface
                    }
                    if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_PRICE_TRACKING)) {
                        return application.getResources().getQuantityString(
                                R.plurals.description_overview_stock_value,
                                products, products,
                                NumUtil.trim(value),
                                sharedPrefs.getString(Constants.PREF.CURRENCY, "")
                        );
                    } else {
                        return application.getResources().getQuantityString(
                                R.plurals.description_overview_stock,
                                products, products
                        );
                    }
                }
        );
        shoppingListDescriptionTextLive = Transformations.map(
                shoppingListItemsLive,
                shoppingListItems -> {
                    if(shoppingListItems == null) return null;
                    int size = shoppingListItems.size();
                    if(isFeatureEnabled(Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS)) {
                        return application.getResources().getQuantityString(
                                R.plurals.description_overview_shopping_list_multi, size, size
                        );
                    } else {
                        return application.getResources().getQuantityString(
                                R.plurals.description_overview_shopping_list, size, size
                        );
                    }
                }
        );
    }

    public void loadFromDatabase(boolean downloadAfterLoading) {
        repository.loadFromDatabase(
                (stockItems, shoppingListItems) -> {
                    this.stockItemsLive.setValue(stockItems);
                    this.shoppingListItemsLive.setValue(shoppingListItems);
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
            dlHelper.getTimeDbChanged(
                    (DownloadHelper.OnStringResponseListener) this::downloadData,
                    () -> onDownloadError(null)
            );
            return;
        }

        DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
        queue.append(
                dlHelper.updateStockItems(dbChangedTime, this.stockItemsLive::setValue),
                dlHelper.updateShoppingListItems(dbChangedTime, this.shoppingListItemsLive::setValue)
        );
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
        editPrefs.apply();
        downloadData();
    }

    private void onQueueEmpty() {
        if(isOffline()) setOfflineLive(false);
        repository.updateDatabase(
                this.stockItemsLive.getValue(),
                this.shoppingListItemsLive.getValue(),
                () -> {}
        );
    }

    private void onDownloadError(@Nullable VolleyError error) {
        if(debug) Log.e(TAG, "onError: VolleyError: " + error);
        String exact = error == null ? null : error.getLocalizedMessage();
        infoFullscreenLive.setValue(
                new InfoFullscreen(InfoFullscreen.ERROR_NETWORK, exact, () -> {
                    infoFullscreenLive.setValue(null);
                    downloadData();
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

    public LiveData<String> getShoppingListDescriptionTextLive() {
        return shoppingListDescriptionTextLive;
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
