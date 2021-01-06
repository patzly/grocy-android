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

    Copyright 2020 by Patrick Zedler & Dominic Zedler
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
import java.util.HashMap;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.ShoppingListItemEditFragmentArgs;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.BaseBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputNameBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputNameBottomSheetArgs;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.FormDataShoppingListItemEdit;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.repository.ShoppingListItemEditRepository;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class ShoppingListItemEditViewModel extends AndroidViewModel {

    private static final String TAG = ShoppingListItemEditViewModel.class.getSimpleName();

    private final SharedPreferences sharedPrefs;
    private final DownloadHelper dlHelper;
    private final GrocyApi grocyApi;
    private final EventHandler eventHandler;
    private final ShoppingListItemEditRepository repository;
    private final FormDataShoppingListItemEdit formData;
    private final ShoppingListItemEditFragmentArgs args;

    private final MutableLiveData<Boolean> isLoadingLive;
    private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
    private final MutableLiveData<Boolean> offlineLive;

    private ArrayList<ShoppingList> shoppingLists;
    private ArrayList<Product> products;
    private ArrayList<ProductBarcode> barcodes;
    private ArrayList<QuantityUnit> quantityUnits;
    private ArrayList<QuantityUnitConversion> unitConversions;

    private DownloadHelper.Queue currentQueueLoading;
    private final boolean debug;
    private final boolean isActionEdit;

    public ShoppingListItemEditViewModel(
            @NonNull Application application,
            @NonNull ShoppingListItemEditFragmentArgs startupArgs
    ) {
        super(application);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        isLoadingLive = new MutableLiveData<>(false);
        dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
        grocyApi = new GrocyApi(getApplication());
        eventHandler = new EventHandler();
        repository = new ShoppingListItemEditRepository(application);
        formData = new FormDataShoppingListItemEdit(application);
        args = startupArgs;
        isActionEdit = startupArgs.getAction().equals(Constants.ACTION.EDIT);

        infoFullscreenLive = new MutableLiveData<>();
        offlineLive = new MutableLiveData<>(false);
    }

    public FormDataShoppingListItemEdit getFormData() {
        return formData;
    }

    public void loadFromDatabase(boolean downloadAfterLoading) {
        repository.loadFromDatabase((shoppingLists, products, barcodes, qUs, conversions) -> {
            this.shoppingLists = shoppingLists;
            this.products = products;
            this.barcodes = barcodes;
            this.quantityUnits = qUs;
            this.unitConversions = conversions;
            formData.getProductsLive().setValue(products);
            if(!isActionEdit) formData.getShoppingListLive().setValue(getLastShoppingList());
            fillWithSoppingListItemIfNecessary();
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
                dlHelper.updateShoppingLists(dbChangedTime, shoppingLists -> {
                    this.shoppingLists = shoppingLists;
                    if(!isActionEdit) {
                        formData.getShoppingListLive().setValue(getLastShoppingList());
                    }
                }), dlHelper.updateProducts(dbChangedTime, products -> {
                    this.products = products;
                    formData.getProductsLive().setValue(products);
                }), dlHelper.updateQuantityUnitConversions(dbChangedTime, conversions -> {
                    this.unitConversions = conversions;
                }), dlHelper.updateProductBarcodes(dbChangedTime, barcodes -> {
                    this.barcodes = barcodes;
                }), dlHelper.updateQuantityUnits(dbChangedTime, quantityUnits -> {
                    this.quantityUnits = quantityUnits;
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
        fillWithSoppingListItemIfNecessary();
        repository.updateDatabase(shoppingLists, products, barcodes,
                quantityUnits, unitConversions, () -> {});
    }

    private void onDownloadError(@Nullable VolleyError error) {
        if(debug) Log.e(TAG, "onError: VolleyError: " + error);
        showMessage(getString(R.string.msg_no_connection));
        if(!isOffline()) setOfflineLive(true);
    }

    public void saveItem() {
        /*if(isFormIncomplete()) return;

        editProductBarcodes();

        JSONObject jsonObject = new JSONObject();
        try {
            Editable amountEdit = binding.editTextShoppingListItemEditAmount.getText();
            String amount = (amountEdit != null ? amountEdit : "").toString().trim();
            jsonObject.put("shopping_list_id", selectedShoppingListId);
            jsonObject.put("amount", amount);
            Product product = getProductFromName(
                    binding.autoCompleteShoppingListItemEditProduct.getText().toString().trim()
            );
            if(product != null) {
                jsonObject.put("product_id", product.getId());
            } else {
                jsonObject.put("product_id", "");
            }
            Editable note = binding.editTextShoppingListItemEditNote.getText();
            assert note != null;
            jsonObject.put("note", note.toString().trim());
        } catch (JSONException e) {
            if(debug) Log.e(TAG, "saveShoppingListItem: " + e);
        }
        if(args.getAction().equals(Constants.ACTION.EDIT)) {
            ShoppingListItem shoppingListItem = args.getShoppingListItem();
            assert shoppingListItem != null;
            dlHelper.put(
                    grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, shoppingListItem.getId()),
                    jsonObject,
                    response -> {
                        editProductBarcodes(); // ADD BARCODES TO PRODUCT
                        activity.navigateUp();
                    },
                    error -> {
                        showErrorMessage();
                        if(debug) Log.e(TAG, "saveShoppingListItem: " + error);
                    }
            );
        } else {
            dlHelper.post(
                    grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LIST),
                    jsonObject,
                    response -> {
                        editProductBarcodes(); // ADD BARCODES TO PRODUCT
                        activity.navigateUp();
                    },
                    error -> {
                        showErrorMessage();
                        if(debug) Log.e(TAG, "saveShoppingListItem: " + error);
                    }
            );
        }*/
    }

    private ArrayList<String> getProductNames(ArrayList<Product> products) {
        ArrayList<String> productNames = new ArrayList<>();
        for(Product product : products) productNames.add(product.getName());
        return productNames;
    }

    public void setShoppingListForPreviousFragment(int selectedId) {
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedId);
        sendEvent(Event.SET_SHOPPING_LIST_ID, bundle);
    }

    private void fillWithSoppingListItemIfNecessary() {
        if(!isActionEdit || formData.isFilledWithShoppingListItem()) return;

        ShoppingListItem item = args.getShoppingListItem();
        assert item != null;

        ShoppingList shoppingList = getShoppingList(item.getShoppingListId());
        formData.getShoppingListLive().setValue(shoppingList);

        double amount = item.getAmount();

        if(item.getProductId() != null) {
            Product product = getProduct(Integer.parseInt(item.getProductId()));
            formData.getProductLive().setValue(product);
            formData.getProductNameLive().setValue(product.getName());
            HashMap<QuantityUnit, Double> unitFactors = setProductQuantityUnitsAndFactors(product);

            if(item.getQuId() != null) {
                QuantityUnit quantityUnit = getQuantityUnit(Integer.parseInt(item.getQuId()));
                if(unitFactors != null && unitFactors.containsKey(quantityUnit)) {
                    Double factor = unitFactors.get(quantityUnit);
                    assert factor != null;
                    if(factor == -1) factor = product.getQuFactorPurchaseToStockDouble();
                    formData.getAmountLive().setValue(NumUtil.trim(amount * factor));
                } else {
                    formData.getAmountLive().setValue(NumUtil.trim(amount));
                }
                formData.getQuantityUnitLive().setValue(quantityUnit);
            } else {
                formData.getAmountLive().setValue(NumUtil.trim(amount));
            }
        } else {
            formData.getAmountLive().setValue(NumUtil.trim(amount));
        }

        formData.getNoteLive().setValue(item.getNote());
        formData.setFilledWithShoppingListItem(true);
    }

    private HashMap<QuantityUnit, Double> setProductQuantityUnitsAndFactors(Product product) {
        QuantityUnit stock = getQuantityUnit(product.getQuIdStock());
        QuantityUnit purchase = getQuantityUnit(product.getQuIdPurchase());

        if(stock == null || purchase == null) {
            showMessage(getString(R.string.error_loading_qus));
            return null;
        }

        HashMap<QuantityUnit, Double> unitFactors = new HashMap<>();
        ArrayList<Integer> quIdsInHashMap = new ArrayList<>();
        unitFactors.put(stock, (double) -1);
        quIdsInHashMap.add(stock.getId());
        if(!quIdsInHashMap.contains(purchase.getId())) {
            unitFactors.put(purchase, product.getQuFactorPurchaseToStockDouble());
        }
        for(QuantityUnitConversion conversion : unitConversions) {
            if(product.getId() != conversion.getProductId()) continue;
            QuantityUnit unit = getQuantityUnit(conversion.getToQuId());
            if(unit == null || quIdsInHashMap.contains(unit.getId())) continue;
            unitFactors.put(unit, conversion.getFactor());
        }
        formData.getQuantityUnitsFactorsLive().setValue(unitFactors);

        if(!isActionEdit) {
            formData.getQuantityUnitLive().setValue(purchase);
        }
        return unitFactors;
    }

    public void setProduct(Product product) {
        if(product == null) return;
        formData.getProductLive().setValue(product);
        setProductQuantityUnitsAndFactors(product);
        formData.isFormValid();
    }

    public void onBarcodeRecognized(String barcode) {
        Product product = getProductFromBarcode(barcode);
        if(product != null) {
            setProduct(product);
        } else {
            formData.getBarcodeLive().setValue(barcode);
        }
    }

    public Product checkProductInput() {
        formData.isProductNameValid();
        String input = formData.getProductNameLive().getValue();
        if(input == null || input.isEmpty()) return null;
        Product product = getProductFromName(input);
        if(product != null) {
            setProduct(product);
        } else {
            showBottomSheet(
                    new InputNameBottomSheet(),
                    new InputNameBottomSheetArgs.Builder(input).build().toBundle()
            );
        }
        return product;
    }

    private QuantityUnit getQuantityUnit(int id) {
        for(QuantityUnit quantityUnit : quantityUnits) {
            if(quantityUnit.getId() == id) return quantityUnit;
        } return null;
    }

    private ShoppingList getLastShoppingList() {
        int lastId = sharedPrefs.getInt(Constants.PREF.SHOPPING_LIST_LAST_ID, 1);
        return getShoppingList(lastId);
    }

    private ShoppingList getShoppingList(int id) {
        for(ShoppingList shoppingList : shoppingLists) {
            if(shoppingList.getId() == id) return shoppingList;
        } return null;
    }

    public Product getProduct(int id) {
        for(Product product : products) {
            if(product.getId() == id) return product;
        } return null;
    }

    private Product getProductFromName(String name) {
        for(Product product : products) {
            if(product.getName().equals(name)) return product;
        } return null;
    }

    private Product getProductFromBarcode(String barcode) {
        for(ProductBarcode code : barcodes) {
            if(code.getBarcode().equals(barcode)) return getProduct(code.getProductId());
        } return null;
    }

    public boolean isActionEdit() {
        return isActionEdit;
    }

    public boolean isFeatureMultiShoppingListsEnabled() {
        return sharedPrefs.getBoolean(
                Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS, true
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

    public static class ShoppingListItemEditViewModelFactory implements ViewModelProvider.Factory {
        private final Application application;
        private final ShoppingListItemEditFragmentArgs args;

        public ShoppingListItemEditViewModelFactory(
                Application application,
                ShoppingListItemEditFragmentArgs args
        ) {
            this.application = application;
            this.args = args;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new ShoppingListItemEditViewModel(application, args);
        }
    }
}
