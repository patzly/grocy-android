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
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.DateBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputProductBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LocationsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheetNew;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ScanModeConfirmBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StoresBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.FormDataPurchase;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.repository.PurchaseRepository;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class PurchaseViewModel extends BaseViewModel {

    private static final String TAG = PurchaseViewModel.class.getSimpleName();
    private final SharedPreferences sharedPrefs;
    private final boolean debug;

    private final DownloadHelper dlHelper;
    private final GrocyApi grocyApi;
    private final PurchaseRepository repository;
    private final FormDataPurchase formData;

    private ArrayList<Product> products;
    private ArrayList<QuantityUnit> quantityUnits;
    private ArrayList<QuantityUnitConversion> unitConversions;
    private ArrayList<ProductBarcode> barcodes;
    private ArrayList<Store> stores;
    private ArrayList<Location> locations;

    private final MutableLiveData<Boolean> isLoadingLive;
    private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
    private final MutableLiveData<Boolean> scanModeEnabled;

    private Runnable queueEmptyAction;

    public PurchaseViewModel(@NonNull Application application) {
        super(application);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        isLoadingLive = new MutableLiveData<>(false);
        dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
        grocyApi = new GrocyApi(getApplication());
        repository = new PurchaseRepository(application);
        formData = new FormDataPurchase(application, sharedPrefs);

        infoFullscreenLive = new MutableLiveData<>();
        scanModeEnabled = new MutableLiveData<>(sharedPrefs.getBoolean(
                Constants.SETTINGS.BEHAVIOR.SCAN_MODE_PURCHASE,
                Constants.SETTINGS_DEFAULT.BEHAVIOR.SCAN_MODE_PURCHASE
        ));

        barcodes = new ArrayList<>();
    }

    public FormDataPurchase getFormData() {
        return formData;
    }

    public void loadFromDatabase(boolean downloadAfterLoading) {
        repository.loadFromDatabase((products, barcodes, qUs, conversions, stores, locations) -> {
            this.products = products;
            this.barcodes = barcodes;
            this.quantityUnits = qUs;
            this.unitConversions = conversions;
            this.stores = stores;
            this.locations = locations;
            formData.getProductsLive().setValue(products);
            if(downloadAfterLoading) downloadData();
        });
    }

    public void downloadData(@Nullable String dbChangedTime) {
        /*if(isOffline()) { // skip downloading
            isLoadingLive.setValue(false);
            return;
        }*/
        if(dbChangedTime == null) {
            dlHelper.getTimeDbChanged(this::downloadData, () -> onDownloadError(null));
            return;
        }

        DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
        queue.append(
                dlHelper.updateProducts(dbChangedTime, products -> {
                    this.products = products;
                    formData.getProductsLive().setValue(products);
                }), dlHelper.updateQuantityUnitConversions(
                        dbChangedTime, conversions -> this.unitConversions = conversions
                ), dlHelper.updateProductBarcodes(
                        dbChangedTime, barcodes -> this.barcodes = barcodes
                ), dlHelper.updateQuantityUnits(
                        dbChangedTime, quantityUnits -> this.quantityUnits = quantityUnits
                ), dlHelper.updateStores(
                        dbChangedTime, stores -> this.stores = stores
                ), dlHelper.updateLocations(
                        dbChangedTime, locations -> this.locations = locations
                )
        );
        if(queue.isEmpty()) {
            if(queueEmptyAction != null) {
                queueEmptyAction.run();
                queueEmptyAction = null;
            }
            return;
        }

        //currentQueueLoading = queue;
        queue.start();
    }

    public void downloadData() {
        downloadData(null);
    }

    public void downloadDataForceUpdate() {
        SharedPreferences.Editor editPrefs = sharedPrefs.edit();
        editPrefs.putString(Constants.PREF.DB_LAST_TIME_LOCATIONS, null);
        editPrefs.putString(Constants.PREF.DB_LAST_TIME_STORES, null);
        editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNIT_CONVERSIONS, null);
        editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCT_BARCODES, null);
        editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
        editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCTS, null);
        editPrefs.apply();
        downloadData();
    }

    private void onQueueEmpty() {
        repository.updateDatabase(products, barcodes,
                quantityUnits, unitConversions, stores, locations, () -> {});
        if(queueEmptyAction != null) {
            queueEmptyAction.run();
            queueEmptyAction = null;
        }
    }

    private void onDownloadError(@Nullable VolleyError error) {
        if (debug) Log.e(TAG, "onError: VolleyError: " + error);
        showMessage(getString(R.string.msg_no_connection));
    }

    public void setProduct(int productId, ProductBarcode barcode) {
        DownloadHelper.OnProductDetailsResponseListener listener = productDetails -> {
            Product updatedProduct = productDetails.getProduct();
            formData.getProductDetailsLive().setValue(productDetails);
            formData.getProductNameLive().setValue(updatedProduct.getName());

            // quantity unit
            double initialUnitFactor = setProductQuantityUnitsAndFactors(updatedProduct, barcode);

            // amount
            boolean isTareWeightEnabled = formData.isTareWeightEnabled();
            if(!isTareWeightEnabled && barcode != null && barcode.hasAmount()) {
                // if barcode contains amount, take this (with tare weight handling off)
                // scan mode status doesn't matter
                formData.getAmountLive().setValue(NumUtil.trim(barcode.getAmountDouble()));
            } else if(!isTareWeightEnabled && !isScanModeEnabled()) {
                String defaultAmount = sharedPrefs.getString(
                        Constants.SETTINGS.STOCK.DEFAULT_PURCHASE_AMOUNT,
                        Constants.SETTINGS_DEFAULT.STOCK.DEFAULT_PURCHASE_AMOUNT
                );
                if(NumUtil.isStringDouble(defaultAmount)) {
                    defaultAmount = NumUtil.trim(Double.parseDouble(defaultAmount));
                }
                if(NumUtil.isStringDouble(defaultAmount)
                        && Double.parseDouble(defaultAmount) > 0) {
                    formData.getAmountLive().setValue(defaultAmount);
                }
            } else if(!isTareWeightEnabled) {
                // if scan mode enabled, always fill with amount 1
                formData.getAmountLive().setValue(NumUtil.trim(1));
            }

            // purchased date
            if(formData.getPurchasedDateEnabled()) {
                formData.getPurchasedDateLive().setValue(DateUtil.getDateStringToday());
            }

            // due days
            int dueDays = productDetails.getProduct().getDefaultDueDays();
            if(dueDays < 0) {
                formData.getDueDateLive().setValue(Constants.DATE.NEVER_OVERDUE);
            } else if (dueDays == 0) {
                formData.getDueDateLive().setValue(null);
            } else {
                formData.getDueDateLive()
                        .setValue(DateUtil.getTodayWithDaysAdded(dueDays));
            }

            // price
            String lastPrice;
            if(barcode != null && barcode.hasLastPrice()) {
                // if barcode contains last price, take this
                lastPrice = barcode.getLastPrice();
            } else {
                lastPrice = productDetails.getLastPrice();
            }
            if(lastPrice != null && !lastPrice.isEmpty()) {
                lastPrice = NumUtil.trimPrice(Double.parseDouble(lastPrice) * initialUnitFactor);
            }
            formData.getPriceLive().setValue(lastPrice);

            // store
            String storeId;
            if(barcode != null && barcode.hasStoreId()) {
                // if barcode contains store, take this
                storeId = barcode.getStoreId();
            } else {
                storeId = productDetails.getLastShoppingLocationId();
            }
            if(!NumUtil.isStringInt(storeId)) {
                storeId = productDetails.getDefaultShoppingLocationId();
            }
            Store store = NumUtil.isStringInt(storeId)
                    ? getStore(Integer.parseInt(storeId)) : null;
            formData.getStoreLive().setValue(store);

            // location
            formData.getLocationLive().setValue(productDetails.getLocation());

            formData.isFormValid();
            if(isScanModeEnabled()) sendEvent(Event.FOCUS_INVALID_VIEWS);
        };

        dlHelper.getProductDetails(productId, listener, error -> {
            showMessage(getString(R.string.error_no_product_details));
            formData.clearForm();
        }).perform(dlHelper.getUuid());
    }

    private double setProductQuantityUnitsAndFactors( // returns factor for unit which was set
            Product product,
            ProductBarcode barcode
    ) {
        QuantityUnit stock = getQuantityUnit(product.getQuIdStock());
        QuantityUnit purchase = getQuantityUnit(product.getQuIdPurchase());

        if(stock == null || purchase == null) {
            showMessage(getString(R.string.error_loading_qus));
            return 1;
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

        QuantityUnit barcodeUnit = null;
        if(barcode != null && barcode.hasQuId()) {
            barcodeUnit = getQuantityUnit(barcode.getQuIdInt());
        }
        Double factor;
        if(barcodeUnit != null && unitFactors.containsKey(barcodeUnit)) {
            formData.getQuantityUnitLive().setValue(barcodeUnit);
            factor =  unitFactors.get(barcodeUnit);
        } else {
            formData.getQuantityUnitLive().setValue(purchase);
            factor = unitFactors.get(purchase);
        }
        return factor != null && factor != -1 ? factor : 1;
    }

    public void onBarcodeRecognized(String barcode) {
        ProductBarcode productBarcode = null;
        Product product = null;
        for(ProductBarcode code : barcodes) {
            if(code.getBarcode().equals(barcode)) {
                productBarcode = code;
                product = getProduct(code.getProductId());
            }
        }
        if(product != null) {
            setProduct(product.getId(), productBarcode);
        } else {
            formData.getBarcodeLive().setValue(barcode);
            formData.isFormValid();
            if(isScanModeEnabled()) sendEvent(Event.FOCUS_INVALID_VIEWS);
        }
    }

    public void checkProductInput() {
        formData.isProductNameValid();
        String input = formData.getProductNameLive().getValue();
        if(input == null || input.isEmpty()) return;
        Product product = getProductFromName(input);

        if(product == null) {
            ProductBarcode productBarcode = null;
            for(ProductBarcode code : barcodes) {
                if(code.getBarcode().equals(input.trim())) {
                    productBarcode = code;
                    product = getProduct(code.getProductId());
                }
            }
            if(product != null) {
                setProduct(product.getId(), productBarcode);
                return;
            }
        }

        ProductDetails currentProductDetails = formData.getProductDetailsLive().getValue();
        Product currentProduct = currentProductDetails != null
                ? currentProductDetails.getProduct() : null;
        if(currentProduct != null && product != null && currentProduct.getId() == product.getId()) {
            return;
        }

        if(product != null) {
            setProduct(product.getId(), null);
        } else {
            showInputProductBottomSheet(input);
        }
    }

    public void addBarcodeToExistingProduct(String barcode) {
        formData.getBarcodeLive().setValue(barcode);
        formData.getProductNameLive().setValue(null);
    }

    public void purchaseProduct() {
        if(!formData.isFormValid()) {
            showMessage(R.string.error_missing_information);
            return;
        }
        if(formData.getBarcodeLive().getValue() != null) {
            uploadProductBarcode(this::purchaseProduct);
            return;
        }

        Product product = formData.getProductDetailsLive().getValue().getProduct();
        JSONObject body = formData.getFilledJSONObject();
        dlHelper.postWithArray(
                grocyApi.purchaseProduct(product.getId()),
                body,
                response -> {
                    // UNDO OPTION
                    String transactionId = null;
                    try {
                        JSONObject jsonObject = (JSONObject) response.get(0);
                        transactionId = jsonObject.getString("transaction_id");
                    } catch (JSONException e) {
                        if(debug) Log.e(TAG, "purchaseProduct: " + e);
                    }
                    if(debug) Log.i(TAG, "purchaseProduct: transaction successful");

                    SnackbarMessage snackbarMessage = new SnackbarMessage(
                            formData.getTransactionSuccessMsg()
                    );
                    if(transactionId != null) {
                        String transId = transactionId;
                        snackbarMessage.setAction(
                                getString(R.string.action_undo),
                                v -> undoTransaction(transId)
                        );
                        snackbarMessage.setDurationSecs(20);
                    }
                    showSnackbar(snackbarMessage);
                    sendEvent(Event.PURCHASE_SUCCESS);
                },
                error -> {
                    showErrorMessage();
                    if(debug) Log.i(TAG, "purchaseProduct: " + error);
                }
        );
    }

    private void undoTransaction(String transactionId) {
        dlHelper.post(
                grocyApi.undoStockTransaction(transactionId),
                success -> {
                    showMessage(getString(R.string.msg_undone_transaction));
                    if(debug) Log.i(TAG, "undoTransaction: undone");
                },
                error -> showErrorMessage()
        );
    }

    private void uploadProductBarcode(Runnable onSuccess) {
        ProductBarcode productBarcode = formData.fillProductBarcode();
        JSONObject body = productBarcode.getJsonFromProductBarcode(debug, TAG);
        dlHelper.addProductBarcode(body, () -> {
            formData.getBarcodeLive().setValue(null);
            barcodes.add(productBarcode); // add to list so it will be found on next scan without reload
            if(onSuccess != null) onSuccess.run();
        }, error -> showMessage(R.string.error_failed_barcode_upload)).perform(dlHelper.getUuid());
    }

    @Nullable
    public Product getProductFromName(@Nullable String name) {
        if(name == null) return null;
        for(Product product : products) {
            if(product.getName().equals(name)) return product;
        } return null;
    }

    public Product getProduct(int id) {
        for(Product product : products) {
            if(product.getId() == id) return product;
        } return null;
    }

    private QuantityUnit getQuantityUnit(int id) {
        for(QuantityUnit quantityUnit : quantityUnits) {
            if(quantityUnit.getId() == id) return quantityUnit;
        } return null;
    }

    private Store getStore(int id) {
        for(Store store : stores) {
            if(store.getId() == id) return store;
        } return null;
    }

    public void showInputProductBottomSheet(@NonNull String input) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ARGUMENT.PRODUCT_INPUT, input);
        showBottomSheet(new InputProductBottomSheet(), bundle);
    }

    public void showQuantityUnitsBottomSheet(boolean hasFocus) {
        if(!hasFocus) return;
        HashMap<QuantityUnit, Double> unitsFactors = getFormData()
                .getQuantityUnitsFactorsLive().getValue();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(
                Constants.ARGUMENT.QUANTITY_UNITS,
                unitsFactors != null ? new ArrayList<>(unitsFactors.keySet()) : null
        );
        showBottomSheet(new QuantityUnitsBottomSheetNew(), bundle);
    }

    public void showPurchasedDateBottomSheet() {
        if(!formData.isProductNameValid()) return;
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ARGUMENT.DEFAULT_DAYS_FROM_NOW, String.valueOf(0));
        bundle.putString(
                Constants.ARGUMENT.SELECTED_DATE,
                formData.getPurchasedDateLive().getValue()
        );
        bundle.putInt(DateBottomSheet.DATE_TYPE, DateBottomSheet.PURCHASED_DATE);
        showBottomSheet(new DateBottomSheet(), bundle);
    }

    public void showDueDateBottomSheet(boolean hasFocus) {
        if(!hasFocus || !formData.isProductNameValid()) return;
        Product product = formData.getProductDetailsLive().getValue().getProduct();
        Bundle bundle = new Bundle();
        bundle.putString(
                Constants.ARGUMENT.DEFAULT_DAYS_FROM_NOW,
                String.valueOf(product.getDefaultDueDays())
        );
        bundle.putString(
                Constants.ARGUMENT.SELECTED_DATE,
                formData.getDueDateLive().getValue()
        );
        bundle.putInt(DateBottomSheet.DATE_TYPE, DateBottomSheet.DUE_DATE);
        showBottomSheet(new DateBottomSheet(), bundle);
    }

    public void showStoresBottomSheet() {
        if(!formData.isProductNameValid()) return;
        Bundle bundle = new Bundle();
        if(stores.get(0).getId() != -1) {
            stores.add(0, new Store(-1, getString(R.string.subtitle_none_selected)));
        }
        bundle.putParcelableArrayList(Constants.ARGUMENT.STORES, stores);
        bundle.putInt(
                Constants.ARGUMENT.SELECTED_ID,
                formData.getStoreLive().getValue() != null
                        ? formData.getStoreLive().getValue().getId()
                        : -1
        );
        showBottomSheet(new StoresBottomSheet(), bundle);
    }

    public void showLocationsBottomSheet() {
        if(!formData.isProductNameValid()) return;
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(Constants.ARGUMENT.LOCATIONS, locations);
        bundle.putInt(
                Constants.ARGUMENT.SELECTED_ID,
                formData.getLocationLive().getValue() != null
                        ? formData.getLocationLive().getValue().getId()
                        : -1
        );
        showBottomSheet(new LocationsBottomSheet(), bundle);
    }

    public void showConfirmationBottomSheet() {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ARGUMENT.TEXT, formData.getConfirmationText());
        showBottomSheet(new ScanModeConfirmBottomSheet(), bundle);
    }

    @NonNull
    public MutableLiveData<Boolean> getIsLoadingLive() {
        return isLoadingLive;
    }

    @NonNull
    public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
        return infoFullscreenLive;
    }

    public void setQueueEmptyAction(Runnable queueEmptyAction) {
        this.queueEmptyAction = queueEmptyAction;
    }

    public boolean isScanModeEnabled() {
        if(scanModeEnabled.getValue() == null) return false;
        return scanModeEnabled.getValue();
    }

    public MutableLiveData<Boolean> getScanModeEnabled() {
        return scanModeEnabled;
    }

    public boolean toggleScanModeEnabled() {
        scanModeEnabled.setValue(!isScanModeEnabled());
        sendEvent(isScanModeEnabled() ? Event.SCAN_MODE_ENABLED : Event.SCAN_MODE_DISABLED);
        return true;
    }

    public boolean getExternalScannerEnabled() {
        return sharedPrefs.getBoolean(
                Constants.SETTINGS.SCANNER.EXTERNAL_SCANNER,
                Constants.SETTINGS_DEFAULT.SCANNER.EXTERNAL_SCANNER
        );
    }

    public boolean getUseFrontCam() {
        return sharedPrefs.getBoolean(
                Constants.SETTINGS.SCANNER.FRONT_CAM,
                Constants.SETTINGS_DEFAULT.SCANNER.FRONT_CAM
        );
    }

    public boolean isFeatureEnabled(String pref) {
        if(pref == null) return true;
        return sharedPrefs.getBoolean(pref, true);
    }

    @Override
    protected void onCleared() {
        dlHelper.destroy();
        super.onCleared();
    }
}
