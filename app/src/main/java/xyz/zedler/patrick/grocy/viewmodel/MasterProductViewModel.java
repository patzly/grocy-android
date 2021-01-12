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

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.MasterProductFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.BaseBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.FormDataMasterProduct;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.repository.MasterProductRepository;
import xyz.zedler.patrick.grocy.util.Constants;

public class MasterProductViewModel extends AndroidViewModel {

    private static final String TAG = MasterProductViewModel.class.getSimpleName();

    private final SharedPreferences sharedPrefs;
    private final DownloadHelper dlHelper;
    private final GrocyApi grocyApi;
    private final EventHandler eventHandler;
    private final MasterProductRepository repository;
    private final FormDataMasterProduct formData;
    private final MasterProductFragmentArgs args;

    private final MutableLiveData<Boolean> isLoadingLive;
    private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
    private final MutableLiveData<Boolean> offlineLive;

    private ArrayList<Product> products;

    private DownloadHelper.Queue currentQueueLoading;
    private final boolean debug;
    private final MutableLiveData<Boolean> actionEditLive;

    public MasterProductViewModel(
            @NonNull Application application,
            @NonNull MasterProductFragmentArgs startupArgs
    ) {
        super(application);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        isLoadingLive = new MutableLiveData<>(false);
        dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
        grocyApi = new GrocyApi(getApplication());
        eventHandler = new EventHandler();
        repository = new MasterProductRepository(application);
        formData = new FormDataMasterProduct(application);
        args = startupArgs;
        actionEditLive = new MutableLiveData<>();
        actionEditLive.setValue(startupArgs.getAction().equals(Constants.ACTION.EDIT));

        infoFullscreenLive = new MutableLiveData<>();
        offlineLive = new MutableLiveData<>(false);

        if(isActionEdit()) {
            assert args.getProduct() != null;
            setCurrentProduct(args.getProduct());
        } else {
            setCurrentProduct(new Product(sharedPrefs));
        }
    }

    public boolean isActionEdit() {
        return actionEditLive.getValue();
    }

    public MutableLiveData<Boolean> getActionEditLive() {
        return actionEditLive;
    }

    public FormDataMasterProduct getFormData() {
        return formData;
    }

    public void setCurrentProduct(Product product) {
        formData.getProductLive().setValue(product);
        formData.isFormValid();
    }

    public Product getFilledProduct() {
        return formData.fillProduct(formData.getProductLive().getValue());
    }

    public void loadFromDatabase(boolean downloadAfterLoading) {
        repository.loadProductsFromDatabase(products -> {
            this.products = products;
            formData.getProductsLive().setValue(products);
            if(downloadAfterLoading) downloadData();
        });
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

        DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
        queue.append(
                dlHelper.updateProducts(dbChangedTime, products -> {
                    this.products = products;
                    formData.getProductsLive().setValue(products);
                })
        );
        if(queue.isEmpty()) return;

        currentQueueLoading = queue;
        queue.start();
    }

    public void downloadData() {
        downloadData(null);
    }

    private void onQueueEmpty() {
        if(isOffline()) setOfflineLive(false);
        repository.updateDatabase(products, () -> {});
    }

    private void onDownloadError(@Nullable VolleyError error) {
        if(debug) Log.e(TAG, "onError: VolleyError: " + error);
        showMessage(getString(R.string.msg_no_connection));
        if(!isOffline()) setOfflineLive(true);
    }

    public void saveItem() {
        if(!formData.isFormValid()) return;

        /*ShoppingListItem item = null;
        if(isActionEdit) item = args.getShoppingListItem();
        item = formData.fillShoppingListItem(item);
        JSONObject jsonObject = ShoppingListItem.getJsonFromShoppingListItem(item, debug, TAG);

        if(isActionEdit) {
            dlHelper.put(
                    grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, item.getId()),
                    jsonObject,
                    response -> saveProductBarcodeAndNavigateUp(),
                    error -> {
                        showErrorMessage();
                        if(debug) Log.e(TAG, "saveItem: " + error);
                    }
            );
        } else {
            dlHelper.post(
                    grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LIST),
                    jsonObject,
                    response -> saveProductBarcodeAndNavigateUp(),
                    error -> {
                        showErrorMessage();
                        if(debug) Log.e(TAG, "saveItem: " + error);
                    }
            );
        }*/
    }

    private void saveProductBarcodeAndNavigateUp() {
        ProductBarcode productBarcode = formData.fillProductBarcode(null);
        if(productBarcode.getBarcode() == null) {
            navigateUp();
            return;
        }
        dlHelper.addProductBarcode(
                ProductBarcode.getJsonFromProductBarcode(productBarcode, debug, TAG),
                this::navigateUp,
                error -> navigateUp()
        ).perform(dlHelper.getUuid());
    }

    public void deleteItem() {
        if(!isActionEdit()) return;
        /*ShoppingListItem shoppingListItem = args.getShoppingListItem();
        assert shoppingListItem != null;
        dlHelper.delete(
                grocyApi.getObject(
                        GrocyApi.ENTITY.SHOPPING_LIST,
                        shoppingListItem.getId()
                ),
                response -> navigateUp(),
                error -> showErrorMessage()
        );*/
    }

    public Product getProduct(int id) {
        for(Product product : products) {
            if(product.getId() == id) return product;
        } return null;
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

    private void navigateUp() {
        eventHandler.setValue(new Event() {
            @Override
            public int getType() {return Event.NAVIGATE_UP;}
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

    @Override
    protected void onCleared() {
        dlHelper.destroy();
        super.onCleared();
    }

    public static class MasterProductViewModelFactory implements ViewModelProvider.Factory {
        private final Application application;
        private final MasterProductFragmentArgs args;

        public MasterProductViewModelFactory(
                Application application,
                MasterProductFragmentArgs args
        ) {
            this.application = application;
            this.args = args;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new MasterProductViewModel(application, args);
        }
    }
}
