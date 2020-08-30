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
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;

public class PurchaseViewModel extends AndroidViewModel {

    private static final String TAG = PurchaseViewModel.class.getSimpleName();
    private SharedPreferences sharedPrefs;
    private boolean debug;

    private DownloadHelper dlHelper;
    private Gson gson;
    private GrocyApi grocyApi;
    private SnackbarMessage snackbarText;

    private MutableLiveData<ArrayList<Product>> productsLive;
    private MutableLiveData<ArrayList<Location>> locationsLive;
    private MutableLiveData<ArrayList<String>> productNamesLive;
    private MutableLiveData<ProductDetails> productDetailsLive;
    private MutableLiveData<String> bestBeforeDateLive;
    private MutableLiveData<Double> amountLive;
    private MutableLiveData<Boolean> isDownloadingLive;

    public PurchaseViewModel(@NonNull Application application) {
        super(application);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        dlHelper = new DownloadHelper(getApplication(), TAG);
        gson = new Gson();
        grocyApi = new GrocyApi(getApplication());
        snackbarText = new SnackbarMessage();

        productsLive = new MutableLiveData<>();
        productNamesLive = new MutableLiveData<>();
        productDetailsLive = new MutableLiveData<>();
        bestBeforeDateLive = new MutableLiveData<>();
        amountLive = new MutableLiveData<>();
        locationsLive = new MutableLiveData<>();
        isDownloadingLive = new MutableLiveData<>();
        amountLive.setValue((double) 0);
    }

    public void updateProducts() {
        dlHelper.getProducts(
                products -> this.productsLive.setValue(products)
        ).perform(dlHelper.getUuid());
    }

    @NonNull
    public MutableLiveData<ArrayList<Product>> getProductsLive() {
        return productsLive;
    }

    @NonNull
    public MutableLiveData<ArrayList<String>> getProductNamesLive() {
        return productNamesLive;
    }

    @Nullable
    public ArrayList<String> getProductNames() {
        return productNamesLive.getValue();
    }

    @NonNull
    public MutableLiveData<String> getBestBeforeDateLive() {
        return bestBeforeDateLive;
    }

    @NonNull
    public MutableLiveData<Double> getAmountLive() {
        return amountLive;
    }

    public Double getAmount() {
        return amountLive.getValue();
    }

    @NonNull
    public MutableLiveData<ArrayList<Location>> getLocationsLive() {
        return locationsLive;
    }

    @Nullable
    public ArrayList<Location> getLocations() {
        return locationsLive.getValue();
    }

    @NonNull
    public MutableLiveData<Boolean> getIsDownloadingLive() {
        return isDownloadingLive;
    }

    public void downloadData() {
        DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
        queue.append(
                dlHelper.getProducts(products -> this.productsLive.setValue(products)),
                dlHelper.getStores(stores -> {
                    /*SortUtil.sortStoresByName(stores, true);
                    this.stores = stores;
                    // Insert NONE as first element
                    stores.add(
                            0,
                            new Store(-1, activity.getString(R.string.subtitle_none_selected))
                    );*/
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
                        dlHelper.getQuantityUnits(quUnits -> this.quantityUnits = quUnits)
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
        showSnackbarMessage(new SnackbarMessage.Message(
                getApplication().getString(R.string.error_undefined),
                Constants.MessageType.DOWNLOAD_ERROR_REFRESH
        ));
    }

    @NonNull
    public MutableLiveData<ProductDetails> getProductDetailsLive() {
        return productDetailsLive;
    }

    @Nullable
    public ProductDetails getProductDetails() {
        return productDetailsLive.getValue();
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
        /*assert getProductDetails() != null;
        assert getAmount() != null;
        double amountMultiplied = getAmount() * getProductDetails().getProduct().getQuFactorPurchaseToStock();
        JSONObject body = new JSONObject();
        try {
            body.put("amount", amountMultiplied);
            body.put("transaction_type", "purchase");
            if(!getPrice().isEmpty()) {
                double price = NumUtil.stringToDouble(getPrice());
                if(binding.checkboxPurchaseTotalPrice.isChecked()) {
                    price = price / getAmount();
                }
                body.put("price", price);
            }
            if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
                body.put("best_before_date", bestBeforeDateLive);
            } else {
                body.put("best_before_date", Constants.DATE.NEVER_EXPIRES);
            }
            if(selectedStoreId > -1) {
                body.put("shopping_location_id", selectedStoreId);
            }
            if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
                body.put("location_id", selectedLocationId);
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

                    showSnackbarMessage(new SnackbarMessage.Message(
                            getApplication().getString(
                                    R.string.msg_purchased,
                                    NumUtil.trim(amountAdded),
                                    amountMultiplied == 1
                                            ? getProductDetails().getQuantityUnitStock().getName()
                                            : getProductDetails().getQuantityUnitStock().getNamePlural(),
                                    getProductDetails().getProduct().getName()
                            )
                    ));

                    if(transactionId != null) {
                        String transId = transactionId;
                        snackbar.setActionTextColor(
                                ContextCompat.getColor(activity, R.color.secondary)
                        ).setAction(
                                activity.getString(R.string.action_undo),
                                v -> undoTransaction(transId)
                        );
                    }
                    activity.showMessage(snackbar);

                    assert getArguments() != null;
                    if(PurchaseFragmentArgs.fromBundle(getArguments()).getCloseWhenFinished()) {
                        navigateUp(this, activity);
                    } else {
                        clearAll();
                    }
                },
                error -> {
                    showErrorMessage();
                    if(debug) Log.i(TAG, "purchaseProduct: " + error);
                }
        );*/
    }

    @NonNull
    public SnackbarMessage getSnackbarMessage() {
        return snackbarText;
    }

    private void showSnackbarMessage(SnackbarMessage.Message message) {
        snackbarText.setValue(message);
    }

    public boolean isFeatureEnabled(String pref) {
        if(pref == null) return true;
        return sharedPrefs.getBoolean(pref, true);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
