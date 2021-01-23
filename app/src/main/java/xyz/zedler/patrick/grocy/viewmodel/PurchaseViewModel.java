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
import androidx.preference.PreferenceManager;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.BaseBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.DueDateBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LocationsBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StoresBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
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
import xyz.zedler.patrick.grocy.util.NetUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class PurchaseViewModel extends AndroidViewModel {

    private static final String TAG = PurchaseViewModel.class.getSimpleName();
    private final SharedPreferences sharedPrefs;
    private final boolean debug;

    private final DownloadHelper dlHelper;
    private final NetUtil netUtil;
    private final Gson gson;
    private final GrocyApi grocyApi;
    private final PurchaseRepository repository;
    private final EventHandler eventHandler;
    private final FormDataPurchase formData;

    private ArrayList<Product> products;
    private ArrayList<QuantityUnit> quantityUnits;
    private ArrayList<QuantityUnitConversion> unitConversions;
    private ArrayList<ProductBarcode> barcodes;
    private ArrayList<Store> stores;
    private ArrayList<Location> locations;

    private final SingleLiveEvent<ArrayList<Product>> productsLive;
    private final SingleLiveEvent<ArrayList<Location>> locationsLive;
    private final SingleLiveEvent<ArrayList<Store>> storesLive;
    private final SingleLiveEvent<ArrayList<QuantityUnit>> quantityUnitsLive;
    private final SingleLiveEvent<ArrayList<String>> productNamesLive;
    private final SingleLiveEvent<ProductDetails> productDetailsLive;

    private final MutableLiveData<Boolean> isLoadingLive;
    private final MutableLiveData<Boolean> totalPriceCheckedLive;
    private final MutableLiveData<String> bestBeforeDateLive;
    private final MutableLiveData<String> priceLive;
    private final MutableLiveData<String> amountLive;
    private final MutableLiveData<InfoFullscreen> infoFullscreenLive;
    private final MutableLiveData<Integer> storeIdLive;
    private final MutableLiveData<Integer> locationIdLive;
    private final MutableLiveData<Integer> shoppingListItemPosLive;

    private ArrayList<Runnable> queueEmptyActions;
    private String forcedAmount;

    public PurchaseViewModel(@NonNull Application application) {
        super(application);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        isLoadingLive = new MutableLiveData<>(false);
        dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
        netUtil = new NetUtil(getApplication());
        gson = new Gson();
        grocyApi = new GrocyApi(getApplication());
        repository = new PurchaseRepository(application);
        eventHandler = new EventHandler();
        formData = new FormDataPurchase(application, sharedPrefs);

        productsLive = new SingleLiveEvent<>();
        productNamesLive = new SingleLiveEvent<>();
        quantityUnitsLive = new SingleLiveEvent<>();
        locationsLive = new SingleLiveEvent<>();
        storesLive = new SingleLiveEvent<>();
        productDetailsLive = new SingleLiveEvent<>();

        amountLive = new MutableLiveData<>();
        priceLive = new MutableLiveData<>();
        infoFullscreenLive = new MutableLiveData<>();
        storeIdLive = new MutableLiveData<>(-1);
        locationIdLive = new MutableLiveData<>(-1);
        shoppingListItemPosLive = new MutableLiveData<>(-1);
        bestBeforeDateLive = new MutableLiveData<>();
        totalPriceCheckedLive = new MutableLiveData<>(false);

        barcodes = new ArrayList<>();
        queueEmptyActions = new ArrayList<>();
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
        if(queue.isEmpty()) return;

        //currentQueueLoading = queue;
        queue.start();
    }

    public void downloadData() {
        downloadData(null);
    }

    private void onQueueEmpty() {
        repository.updateDatabase(products, barcodes,
                quantityUnits, unitConversions, stores, locations, () -> {});
    }

    private void onDownloadError(@Nullable VolleyError error) {
        if(debug) Log.e(TAG, "onError: VolleyError: " + error);
        showMessage(getString(R.string.msg_no_connection));
    }

    /*public void downloadData(PurchaseFragmentArgs args) {
        DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
        queue.append(
                dlHelper.getProducts(products -> {
                    productsLive.setValue(products);
                    productNamesLive.setValue(createProductNamesList(products));
                }),
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
        if(args.getShoppingListItems() != null) {
            queue.append(dlHelper.getQuantityUnits(quUnits -> quantityUnitsLive.setValue(quUnits)));
        }
        queue.start();
    }*/

    public void setProduct(Product product) {
        if(product == null) return;
        dlHelper.getProductDetails(
                product.getId(),
                productDetails -> {
                    Product updatedProduct = productDetails.getProduct();
                    formData.getProductDetailsLive().setValue(productDetails);
                    formData.getProductNameLive().setValue(updatedProduct.getName());
                    setProductQuantityUnitsAndFactors(updatedProduct);
                    if(!isTareWeightEnabled(productDetails)) {
                        String defaultAmount = sharedPrefs.getString(
                                Constants.SETTINGS.STOCK.DEFAULT_PURCHASE_AMOUNT,
                                Constants.SETTINGS_DEFAULT.STOCK.DEFAULT_PURCHASE_AMOUNT
                        );
                        if(NumUtil.isStringDouble(defaultAmount)) {
                            defaultAmount = NumUtil.trim(Double.parseDouble(defaultAmount));
                        }
                        formData.getAmountLive().setValue(defaultAmount);
                    }
                    int defaultBestBeforeDays = productDetails.getProduct().getDefaultDueDays();
                    if(defaultBestBeforeDays < 0) {
                        formData.getDueDateLive().setValue(Constants.DATE.NEVER_EXPIRES);
                    } else if (defaultBestBeforeDays == 0) {
                        formData.getDueDateLive().setValue(null);
                    } else {
                        formData.getDueDateLive()
                                .setValue(DateUtil.getTodayWithDaysAdded(defaultBestBeforeDays));
                    }
                    String lastPrice = productDetails.getLastPrice();
                    if(lastPrice != null && !lastPrice.isEmpty()) {
                        lastPrice = NumUtil.trimPrice(Double.parseDouble(lastPrice));
                    }
                    formData.getPriceLive().setValue(lastPrice);
                    String storeId = productDetails.getLastShoppingLocationId();
                    if(!NumUtil.isStringInt(storeId)) {
                        storeId = productDetails.getDefaultShoppingLocationId();
                    }
                    Store store = NumUtil.isStringInt(storeId)
                            ? getStore(Integer.parseInt(storeId)) : null;
                    formData.getStoreLive().setValue(store);
                    formData.getLocationLive().setValue(productDetails.getLocation());
                    formData.isFormValid();
                },
                error -> {
                    showMessage(getString(R.string.error_no_product_details));
                    formData.clearForm();
                }
        ).perform(dlHelper.getUuid());
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
        formData.getQuantityUnitLive().setValue(purchase);

        return unitFactors;
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

    public void loadProductDetails(int productId) {
        dlHelper.get(
                grocyApi.getStockProductDetails(productId),
                response -> productDetailsLive.setValue(
                        gson.fromJson(
                                response,
                                new TypeToken<ProductDetails>(){}.getType()
                        )
                ), error -> {}
        );
    }

    public void loadProductDetailsByBarcode(String barcode) {
        dlHelper.get(
                grocyApi.getStockProductByBarcode(barcode),
                response -> {
                    productDetailsLive.setValue(
                            gson.fromJson(
                                    response,
                                    new TypeToken<ProductDetails>(){}.getType()
                            )
                    );
                }, error -> {
                    NetworkResponse response = error.networkResponse;
                    if(response != null && response.statusCode == 400) {
                        Bundle bundle = new Bundle();
                        bundle.putString(Constants.ARGUMENT.BARCODE, barcode);
                        sendEvent(Event.BARCODE_UNKNOWN, bundle);
                    } else {
                        showMessage(getString(R.string.error_undefined));
                    }
                }
        );
    }

    public void writeDefaultValues() {
        ProductDetails productDetails = getProductDetailsLive().getValue();
        if(productDetails == null) return;

        // BBD
        int defaultBestBeforeDays = productDetails.getProduct().getDefaultDueDays();
        if(defaultBestBeforeDays < 0) {
            bestBeforeDateLive.setValue(Constants.DATE.NEVER_EXPIRES);
        } else if (defaultBestBeforeDays == 0) {
            bestBeforeDateLive.setValue(null);
        } else {
            bestBeforeDateLive.setValue(DateUtil.getTodayWithDaysAdded(defaultBestBeforeDays));
        }

        // PRICE
        String lastPrice = productDetails.getLastPrice();
        if(lastPrice != null && !lastPrice.isEmpty()) {
            lastPrice = NumUtil.trimPrice(Double.parseDouble(lastPrice));
        }
        priceLive.setValue(lastPrice);

        // STORE
        String storeId = productDetails.getLastShoppingLocationId();
        if(storeId == null || storeId.isEmpty()) {
            storeId = productDetails.getLastShoppingLocationId();
        } else {
            storeId = productDetails.getProduct().getStoreId();
        }
        if(storeId == null || storeId.isEmpty()) {
            storeIdLive.setValue(-1);
        } else {
            storeIdLive.setValue(Integer.parseInt(storeId));
        }

        // LOCATION
        if(productDetails.getLocation() == null) {
            locationIdLive.setValue(-1);
        } else {
            locationIdLive.setValue(productDetails.getLocation().getId());
        }
    }

    public void purchaseProduct() {
        assert getProductDetails() != null && getAmount() != null;
        ProductDetails productDetails = getProductDetails();
        Product product = productDetails.getProduct();
        double amount = NumUtil.toDouble(getAmount());
        double amountMultiplied = amount * product.getQuFactorPurchaseToStockDouble();
        JSONObject body = new JSONObject();
        try {
            body.put("amount", amountMultiplied);
            body.put("transaction_type", "purchase");
            if(getPrice() != null && !getPrice().isEmpty()) {
                double price = NumUtil.toDouble(getPrice());
                assert totalPriceCheckedLive.getValue() != null;
                if(totalPriceCheckedLive.getValue()) {
                    price = price / amount;
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
                grocyApi.purchaseProduct(product.getId()),
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
                    if(product.getEnableTareWeightHandling() == 0) {
                        amountAdded = amountMultiplied;
                    } else {
                        // calculate difference of amount if tare weight handling enabled
                        amountAdded = amountMultiplied - product.getTareWeightDouble()
                                - productDetails.getStockAmount();
                    }

                    SnackbarMessage snackbarMessage = new SnackbarMessage(
                            getString(
                                    R.string.msg_purchased,
                                    NumUtil.trim(amountAdded),
                                    amountMultiplied == 1
                                            ? productDetails.getQuantityUnitStock().getName()
                                            : productDetails.getQuantityUnitStock().getNamePlural(),
                                    product.getName()
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
    private ArrayList<String> createProductNamesList(@NonNull ArrayList<Product> products) {
        ArrayList<String> names = new ArrayList<>();
        for(Product product : products) {
            names.add(product.getName());
        }
        return names;
    }

    @Nullable
    public Product getProductFromName(@Nullable String name) {
        if(productsLive.getValue() == null || name == null) return null;
        for(Product product : productsLive.getValue()) {
            if(product.getName().equals(name)) {
                return product;
            }
        }
        return null;
    }

    public void showDueDateBottomSheet() {
        if(!formData.isProductNameValid()) return;
        Product product = formData.getProductDetailsLive().getValue().getProduct();
        Bundle bundle = new Bundle();
        bundle.putString(
                Constants.ARGUMENT.DEFAULT_DUE_DAYS,
                String.valueOf(product.getDefaultDueDays())
        );
        bundle.putString(
                Constants.ARGUMENT.SELECTED_DATE,
                formData.getDueDateLive().getValue()
        );
        showBottomSheet(new DueDateBottomSheet(), bundle);
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
    public MutableLiveData<Boolean> getIsLoadingLive() {
        return isLoadingLive;
    }

    @NonNull
    public Boolean getIsDownloading() {
        assert isLoadingLive.getValue() != null;
        return isLoadingLive.getValue();
    }

    @NonNull
    public MutableLiveData<String> getBestBeforeDateLive() {
        return bestBeforeDateLive;
    }

    @Nullable
    public String getBestBeforeDate() {
        return bestBeforeDateLive.getValue();
    }

    @NonNull
    public MutableLiveData<String> getAmountLive() {
        return amountLive;
    }

    @Nullable
    public String getAmount() {
        return amountLive.getValue();
    }

    /*public void changeAmountLess() {
        if(!NumUtil.isDouble(getAmount())) {
            amountLive.setValue(NumUtil.trim(1));
        } else {
            double amountNew = NumUtil.toDouble(getAmount()) - 1;
            if(amountNew < getMinAmount()) return;
            amountLive.setValue(NumUtil.trim(amountNew));
        }
    }

    public Double getMinAmount() {
        double minAmount;
        if(getProductDetails() == null || !isTareWeightEnabled(getProductDetails())) {
            minAmount = 1;
        } else {
            minAmount = getProductDetails().getProduct().getTareWeightDouble();
            minAmount += getProductDetails().getStockAmount();
        }
        return minAmount;
    }*/

    public void setForcedAmount(String forcedAmount) {
        this.forcedAmount = forcedAmount;
    }

    public String getForcedAmount() {
        return forcedAmount;
    }

    public boolean isTareWeightEnabled(ProductDetails productDetails) {
        if(productDetails == null) return false;
        return productDetails.getProduct().getEnableTareWeightHandling() == 1;
    }

    @NonNull
    public MutableLiveData<String> getPriceLive() {
        return priceLive;
    }

    @Nullable
    public String getPrice() {
        return priceLive.getValue();
    }

    @NonNull
    public MutableLiveData<Boolean> getTotalPriceCheckedLive() {
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

    @NonNull
    public MutableLiveData<Integer> getLocationIdLive() {
        return locationIdLive;
    }

    public int getLocationId() {
        assert locationIdLive.getValue() != null;
        return locationIdLive.getValue();
    }

    @Nullable
    public Location getLocationFromId(int locationId) {
        if(getLocations() == null || getLocations().isEmpty() || locationId == -1) return null;
        for(Location locationTmp : getLocations()) {
            if (locationTmp.getId() == locationId) return locationTmp;
        } return null;
    }

    @NonNull
    public MutableLiveData<Integer> getShoppingListItemPosLive() {
        return shoppingListItemPosLive;
    }

    public void nextShoppingListItemPos() {
        assert shoppingListItemPosLive.getValue() != null;
        int pos = shoppingListItemPosLive.getValue();
        if(pos == -1) return;
        shoppingListItemPosLive.setValue(pos + 1);
    }

    @NonNull
    public SingleLiveEvent<ArrayList<Store>> getStoresLive() {
        return storesLive;
    }

    @Nullable
    public ArrayList<Store> getStores() {
        return storesLive.getValue();
    }

    @NonNull
    public MutableLiveData<Integer> getStoreIdLive() {
        return storeIdLive;
    }

    public int getStoreId() {
        assert storeIdLive.getValue() != null;
        return storeIdLive.getValue();
    }

    @Nullable
    public Store getStoreFromId(int storeId) {
        if(getStores() == null || getStores().isEmpty() || storeId == -1) return null;
        for(Store storeTmp : getStores()) {
            if (storeTmp.getId() == storeId) return storeTmp;
        } return null;
    }

    @NonNull
    public SingleLiveEvent<ArrayList<QuantityUnit>> getQuantityUnitsLive() {
        return quantityUnitsLive;
    }

    @NonNull
    public MutableLiveData<InfoFullscreen> getInfoFullscreenLive() {
        return infoFullscreenLive;
    }

    public void addQueueEmptyAction(Runnable runnable) {
        queueEmptyActions.add(runnable);
    }

    public void showErrorMessage() {
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
}
