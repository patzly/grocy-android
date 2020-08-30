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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.preference.PreferenceManager;

import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;

public class PurchaseViewModel extends AndroidViewModel {

    private static final String TAG = PurchaseViewModel.class.getSimpleName();
    private SharedPreferences sharedPrefs;
    private boolean debug;

    private DownloadHelper dlHelper;
    private Gson gson;
    private GrocyApi grocyApi;
    private EventHandler eventHandler;

    private SingleLiveEvent<ArrayList<Product>> productsLive;
    private SingleLiveEvent<ArrayList<Location>> locationsLive;
    private SingleLiveEvent<ArrayList<Store>> storesLive;
    private SingleLiveEvent<ArrayList<QuantityUnit>> quantityUnitsLive;
    private SingleLiveEvent<ArrayList<String>> productNamesLive;
    private SingleLiveEvent<ProductDetails> productDetailsLive;

    private SingleLiveEvent<Boolean> isDownloadingLive;
    private SingleLiveEvent<Boolean> totalPriceCheckedLive;
    private SingleLiveEvent<String> bestBeforeDateLive;
    private SingleLiveEvent<String> priceLive;
    private SingleLiveEvent<Double> amountLive;
    private SingleLiveEvent<Integer> storeIdLive;
    private SingleLiveEvent<Integer> locationIdLive;

    public PurchaseViewModel(@NonNull Application application) {
        super(application);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        dlHelper = new DownloadHelper(getApplication(), TAG);
        gson = new Gson();
        grocyApi = new GrocyApi(getApplication());
        eventHandler = new EventHandler();

        productsLive = new SingleLiveEvent<>();
        productNamesLive = new SingleLiveEvent<>();
        quantityUnitsLive = new SingleLiveEvent<>();
        locationsLive = new SingleLiveEvent<>();
        storesLive = new SingleLiveEvent<>();
        isDownloadingLive = new SingleLiveEvent<>();
        totalPriceCheckedLive = new SingleLiveEvent<>();
        isDownloadingLive.setValue(false);
        totalPriceCheckedLive.setValue(false);

        productDetailsLive = new SingleLiveEvent<>();
        amountLive = new SingleLiveEvent<>();
        priceLive = new SingleLiveEvent<>();
        storeIdLive = new SingleLiveEvent<>();
        locationIdLive = new SingleLiveEvent<>();
        bestBeforeDateLive = new SingleLiveEvent<>();
        amountLive.setValue(0d);
        storeIdLive.setValue(-1);
        locationIdLive.setValue(-1);
    }

    public void updateProducts() {
        dlHelper.getProducts(
                products -> this.productsLive.setValue(products)
        ).perform(dlHelper.getUuid());
    }

    public void downloadData() {
        DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
        queue.append(
                dlHelper.getProducts(products -> this.productsLive.setValue(products)),
                dlHelper.getStores(stores -> {
                    SortUtil.sortStoresByName(stores, true);
                    stores.add( // Insert NONE as first element
                            0,
                            new Store(-1, getString(R.string.subtitle_none_selected))
                    );
                    storesLive.setValue(stores);
                })
        );
        if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
            queue.append(
                    dlHelper.getLocations(locations -> {
                        SortUtil.sortLocationsByName(locations, true);
                        this.locationsLive.setValue(locations);
                    })
            );
        }
        // only load quantity units if shopping list items have to be displayed
        /*if(startupBundle != null) {
            String type = startupBundle.getString(Constants.ARGUMENT.TYPE);
            if(type != null && type.equals(Constants.ACTION.PURCHASE_MULTI_THEN_SHOPPING_LIST)) {
                queue.append(
                        dlHelper.getQuantityUnits(quUnits -> quantityUnitsLive.setValue(quUnits))
                );
            }
        }*/
        getIsDownloadingLive().setValue(true);
        queue.start();
    }

    private void onQueueEmpty() {
        getIsDownloadingLive().setValue(false);
    }

    private void onDownloadError(VolleyError error) {
        if(debug) Log.e(TAG, "onError: VolleyError: " + error);
        getIsDownloadingLive().setValue(false);
        showSnackbar(
                new SnackbarMessage(getString(R.string.error_undefined)).setAction(
                        getString(R.string.action_retry),
                        v -> downloadData()
                )
        );
    }

    public void loadProductDetails(int productId) {
        dlHelper.get(
                grocyApi.getStockProductDetails(productId),
                response -> {
                    productDetailsLive.setValue(
                            gson.fromJson(
                                    response,
                                    new TypeToken<ProductDetails>(){}.getType()
                            )
                    );
                    writeDefaultValues();
                }, error -> {}
        );
    }

    private void writeDefaultValues() { // TODO
        ProductDetails productDetails = getProductDetailsLive().getValue();
        if(productDetails == null) return;

        // BBD
        int defaultBestBeforeDays = productDetails.getProduct().getDefaultBestBeforeDays();
        if(defaultBestBeforeDays < 0) {
            bestBeforeDateLive.setValue(Constants.DATE.NEVER_EXPIRES);
        } else if (defaultBestBeforeDays == 0) {
            bestBeforeDateLive.setValue(null);
        } else {
            bestBeforeDateLive.setValue(DateUtil.getTodayWithDaysAdded(defaultBestBeforeDays));
        }
    }

    public void purchaseProduct() {

        assert getProductDetails() != null;
        assert getAmount() != null;
        double amountMultiplied = getAmount() * getProductDetails().getProduct().getQuFactorPurchaseToStock();
        JSONObject body = new JSONObject();
        try {
            body.put("amount", amountMultiplied);
            body.put("transaction_type", "purchase");
            if(getPrice() != null && !getPrice().isEmpty()) {
                double price = NumUtil.stringToDouble(getPrice());
                assert totalPriceCheckedLive.getValue() != null;
                if(totalPriceCheckedLive.getValue()) {
                    price = price / getAmount();
                }
                body.put("price", price);
            }
            if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
                body.put("best_before_date", bestBeforeDateLive);
            } else {
                body.put("best_before_date", Constants.DATE.NEVER_EXPIRES);
            }
            assert storeIdLive.getValue() != null;
            if(storeIdLive.getValue() > -1) {
                body.put("shopping_location_id", storeIdLive.getValue());
            }
            if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
                body.put("location_id", locationIdLive.getValue());
            }
        } catch (JSONException e) {
            if(debug) Log.e(TAG, "purchaseProduct: " + e);
        }
        dlHelper.post(
                grocyApi.purchaseProduct(getProductDetails().getProduct().getId()),
                body,
                response -> {
                    // ADD BARCODES TO PRODUCT
                    editProductBarcodes();

                    // UNDO OPTION
                    String transactionId = null;
                    try {
                        transactionId = response.getString("transaction_id");
                    } catch (JSONException e) {
                        if(debug) Log.e(TAG, "purchaseProduct: " + e);
                    }
                    if(debug) Log.i(TAG, "purchaseProduct: purchased " + amountMultiplied);

                    double amountAdded;
                    if(getProductDetails().getProduct().getEnableTareWeightHandling() == 0) {
                        amountAdded = amountMultiplied;
                    } else {
                        // calculate difference of amount if tare weight handling enabled
                        amountAdded = amountMultiplied - getProductDetails().getProduct().getTareWeight()
                                - getProductDetails().getStockAmount();
                    }

                    SnackbarMessage snackbarMessage = new SnackbarMessage(
                            getString(
                                    R.string.msg_purchased,
                                    NumUtil.trim(amountAdded),
                                    amountMultiplied == 1
                                            ? getProductDetails().getQuantityUnitStock().getName()
                                            : getProductDetails().getQuantityUnitStock().getNamePlural(),
                                    getProductDetails().getProduct().getName()
                            )
                    );
                    if(transactionId != null) {
                        String transId = transactionId;
                        snackbarMessage.setAction(
                                getString(R.string.action_undo),
                                v -> undoTransaction(transId)
                        );
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

    private void editProductBarcodes() {
        /*if(binding.linearPurchaseBarcodeContainer.getChildCount() == 0) return;
        if(getProductDetails() == null) return;

        String barcodesString = getProductDetails().getProduct().getBarcode();
        ArrayList<String> barcodes;
        if(barcodesString == null || barcodesString.isEmpty()) {
            barcodes = new ArrayList<>();
        } else {
            barcodes = new ArrayList<>(
                    Arrays.asList(getProductDetails().getProduct().getBarcode().split(","))
            );
        }

        for(int i = 0; i < binding.linearPurchaseBarcodeContainer.getChildCount(); i++) {
            InputChip inputChip = (InputChip) binding.linearPurchaseBarcodeContainer.getChildAt(i);
            if(!barcodes.contains(inputChip.getText())) {
                barcodes.add(inputChip.getText());
            }
        }
        if(debug) Log.i(TAG, "editProductBarcodes: " + barcodes);
        JSONObject body = new JSONObject();
        try {
            body.put("barcode", TextUtils.join(",", barcodes));
        } catch (JSONException e) {
            if(debug) Log.e(TAG, "editProductBarcodes: " + e);
        }
        dlHelper.put(
                grocyApi.getObject(
                        GrocyApi.ENTITY.PRODUCTS,
                        getProductDetails().getProduct().getId()
                ),
                body,
                response -> { },
                error -> {
                    if(debug) Log.i(TAG, "editProductBarcodes: " + error);
                }
        );*/
    }

    @NonNull
    public SingleLiveEvent<ArrayList<Product>> getProductsLive() {
        return productsLive;
    }

    @NonNull
    public SingleLiveEvent<ArrayList<String>> getProductNamesLive() {
        return productNamesLive;
    }

    @Nullable
    public ArrayList<String> getProductNames() {
        return productNamesLive.getValue();
    }

    @NonNull
    public SingleLiveEvent<ProductDetails> getProductDetailsLive() {
        return productDetailsLive;
    }

    @Nullable
    public ProductDetails getProductDetails() {
        return productDetailsLive.getValue();
    }

    @NonNull
    public SingleLiveEvent<Boolean> getIsDownloadingLive() {
        return isDownloadingLive;
    }

    @NonNull
    public SingleLiveEvent<String> getBestBeforeDateLive() {
        return bestBeforeDateLive;
    }

    @Nullable
    public String getBestBeforeDate() {
        return bestBeforeDateLive.getValue();
    }

    @NonNull
    public SingleLiveEvent<Double> getAmountLive() {
        return amountLive;
    }

    public Double getAmount() {
        return amountLive.getValue();
    }

    @NonNull
    public SingleLiveEvent<String> getPriceLive() {
        return priceLive;
    }

    @Nullable
    public String getPrice() {
        return priceLive.getValue();
    }

    public void changePriceMore() {
        if(getPrice() == null || getPrice().isEmpty()) {
            priceLive.setValue(NumUtil.trimPrice(1));
        } else {
            double priceNew = NumUtil.stringToDouble(getPrice()) + 1;
            priceLive.setValue(NumUtil.trimPrice(priceNew));
        }
    }

    public void changePriceLess() {
        if(getPrice() == null || getPrice().isEmpty()) return;
        double priceNew = NumUtil.stringToDouble(getPrice()) - 1;
        if(priceNew >= 0) priceLive.setValue(NumUtil.trimPrice(priceNew));
    }

    @NonNull
    public SingleLiveEvent<Boolean> getTotalPriceCheckedLive() {
        return totalPriceCheckedLive;
    }

    @NonNull
    public SingleLiveEvent<ArrayList<Location>> getLocationsLive() {
        return locationsLive;
    }

    @Nullable
    public ArrayList<Location> getLocations() {
        return locationsLive.getValue();
    }

    public int getLocationId() {
        assert locationIdLive.getValue() != null;
        return locationIdLive.getValue();
    }

    @NonNull
    public SingleLiveEvent<ArrayList<Store>> getStoresLive() {
        return storesLive;
    }

    @Nullable
    public ArrayList<Store> getStores() {
        return storesLive.getValue();
    }

    public int getStoreId() {
        assert storeIdLive.getValue() != null;
        return storeIdLive.getValue();
    }

    @NonNull
    public SingleLiveEvent<ArrayList<QuantityUnit>> getQuantityUnitsLive() {
        return quantityUnitsLive;
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

    private void sendEvent(int type) {
        eventHandler.setValue(new Event() {
            @Override
            public int getType() {return type;}
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
        super.onCleared();
    }
}
