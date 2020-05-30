package xyz.zedler.patrick.grocy;

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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.BBDateBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.BatchChooseBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.BatchConfigBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ExitScanBatchBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LocationsBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.PriceBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StockEntriesBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StockLocationsBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StoresBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.model.BatchPurchaseEntry;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.MissingBatchItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.scan.ScanBatchCaptureManager;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.view.ActionButton;
import xyz.zedler.patrick.grocy.view.BarcodeRipple;
import xyz.zedler.patrick.grocy.web.RequestQueueSingleton;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class ScanBatchActivity extends AppCompatActivity
        implements ScanBatchCaptureManager.BarcodeListener, DecoratedBarcodeView.TorchListener {

    private final static String TAG = Constants.UI.BATCH_SCAN;
    private final static boolean DEBUG = true;

    private ScanBatchCaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private BarcodeRipple barcodeRipple;
    private ActionButton buttonFlash;
    private TextView textViewCount, textViewType;
    private MaterialCardView cardViewCount, cardViewType;
    private boolean isTorchOn;
    private ClickUtil clickUtil = new ClickUtil();

    private Intent intent;
    private String actionType;
    private FragmentManager fragmentManager;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private RequestQueue requestQueue;
    private WebRequest request;
    private SharedPreferences sharedPrefs;

    private ArrayList<Product> products = new ArrayList<>();
    private ArrayList<Store> stores = new ArrayList<>();
    private ArrayList<Location> locations = new ArrayList<>();
    private ArrayList<String> productNames = new ArrayList<>();
    private ArrayList<MissingBatchItem> missingBatchItems = new ArrayList<>();

    private ProductDetails currentProductDetails;
    private MissingBatchItem currentMissingBatchItem;
    private ProductType currentProductType;
    private String currentProductName, currentDefaultStoreId, currentLastPrice;
    private int currentDefaultBestBeforeDays = 0, currentDefaultLocationId = -1;

    private enum ProductType {
        PRODUCT, MISSING_BATCH_ITEM
    }

    private String bestBeforeDate;
    private String price;
    private String storeId;
    private String locationId;
    private String entryId;
    private String stockLocationId;

    private Map<String, String> sessionBestBeforeDates = new HashMap<>();
    private Map<String, String> sessionPrices = new HashMap<>();
    private Map<String, String> sessionStoreIds = new HashMap<>();
    private Map<String, String> sessionLocationIds = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scan_batch);

        intent = getIntent();
        actionType = intent.getStringExtra(Constants.ARGUMENT.TYPE);
        if(actionType == null) finish();

        isTorchOn = false;

        // PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // WEB REQUESTS

        requestQueue = RequestQueueSingleton.getInstance(getApplicationContext()).getRequestQueue();
        request = new WebRequest(requestQueue);

        // API

        grocyApi = new GrocyApi(this);

        // INITIALIZE VIEWS

        ActionButton buttonClose = findViewById(R.id.button_scan_batch_close);
        buttonClose.setOnClickListener(v -> onBackPressed());
        buttonClose.setTooltipText(getString(R.string.action_close));

        textViewType = findViewById(R.id.text_scan_batch_type);
        textViewCount = findViewById(R.id.text_scan_batch_count);
        refreshCounter();

        cardViewCount = findViewById(R.id.card_scan_batch_count);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            cardViewCount.setTooltipText(getString(R.string.tooltip_new_products_count));
        }
        cardViewCount.setOnClickListener(v -> {
            if(missingBatchItems.size() > 0) {
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList(Constants.ARGUMENT.BATCH_ITEMS, missingBatchItems);
                setResult(
                        Activity.RESULT_OK,
                        new Intent().putExtra(Constants.ARGUMENT.BUNDLE, bundle)
                );
                finish();
            } else {
                showMessage(getString(R.string.msg_batch_no_products));
            }
        });

        cardViewType = findViewById(R.id.card_scan_batch_type);
        cardViewType.setOnClickListener(v -> setActionType(
                actionType.equals(Constants.ACTION.CONSUME)
                        ? Constants.ACTION.PURCHASE
                        : Constants.ACTION.CONSUME,
                true
        ));

        setActionType(actionType, false);

        barcodeScannerView = findViewById(R.id.barcode_scan_batch);
        barcodeScannerView.setTorchOff();
        barcodeScannerView.setTorchListener(this);

        findViewById(R.id.button_scan_batch_config).setOnClickListener(v -> {
            if(clickUtil.isDisabled()) return;
            new Handler().postDelayed(this::pauseScan, 300);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.ARGUMENT.TYPE, actionType);
            showBottomSheet(new BatchConfigBottomSheetDialogFragment(), bundle);
        });

        buttonFlash = findViewById(R.id.button_scan_batch_flash);
        buttonFlash.setOnClickListener(v -> switchTorch());
        buttonFlash.setIcon(R.drawable.ic_round_flash_off_to_on);

        barcodeRipple = findViewById(R.id.ripple_scan);

        if(!hasFlash()) {
            findViewById(R.id.frame_scan_flash).setVisibility(View.GONE);
        }

        fragmentManager = getSupportFragmentManager();

        Bundle bundle = intent.getBundleExtra(Constants.ARGUMENT.BUNDLE);
        if(bundle != null) {
            if(bundle.getParcelableArrayList(Constants.ARGUMENT.BATCH_ITEMS) != null) {
                missingBatchItems = bundle.getParcelableArrayList(Constants.ARGUMENT.BATCH_ITEMS);
                refreshCounter();
            }
        }

        capture = new ScanBatchCaptureManager(this, barcodeScannerView, this);
        capture.decode();

        hideInfo();

        // DOWNLOAD NECESSARY OBJECTS
        downloadProducts(response -> {}, error -> {});
        downloadStores(response -> {}, error -> {});
        downloadLocations(response -> {}, error -> {});
    }

    @Override
    protected void onResume() {
        super.onResume();
        capture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        barcodeScannerView.setTorchOff();
        capture.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if(missingBatchItems.size() > 0) {
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(Constants.ARGUMENT.BATCH_ITEMS, missingBatchItems);
            showBottomSheet(new ExitScanBatchBottomSheetDialogFragment(), bundle);
            pauseScan();
        } else {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    private void downloadProducts(
            OnResponseListener responseListener,
            OnErrorListener errorListener
    ) {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS),
                response -> {
                    products = gson.fromJson(
                            response,
                            new TypeToken<List<Product>>(){}.getType()
                    );
                    productNames = getProductNames();
                    for(MissingBatchItem missingBatchItem : missingBatchItems) {
                        if(!missingBatchItem.getIsOnServer()
                                && !productNames.contains(missingBatchItem.getProductName())
                        ) {
                            productNames.add(missingBatchItem.getProductName());
                        }
                    }
                    responseListener.onResponse(response);
                }, errorListener::onError
        );
    }

    private void downloadStores(
            OnResponseListener responseListener,
            OnErrorListener errorListener
    ) {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.STORES),
                response -> {
                    stores = gson.fromJson(
                            response,
                            new TypeToken<List<Store>>(){}.getType()
                    );
                    responseListener.onResponse(response);
                }, errorListener::onError
        );
    }

    private void downloadLocations(
            OnResponseListener responseListener,
            OnErrorListener errorListener
    ) {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.LOCATIONS),
                response -> {
                    locations = gson.fromJson(
                            response,
                            new TypeToken<List<Location>>(){}.getType()
                    );
                    responseListener.onResponse(response);
                }, errorListener::onError
        );
    }

    public void loadProductDetailsByBarcode(String barcode) {
        request.get(
                grocyApi.getStockProductByBarcode(barcode),
                response -> {
                    currentProductDetails = gson.fromJson(
                            response,
                            new TypeToken<ProductDetails>(){}.getType()
                    );
                    setCurrentValues(
                            ProductType.PRODUCT,
                            currentProductDetails.getProduct().getName(),
                            currentProductDetails.getProduct().getStoreId(),
                            currentProductDetails.getLastPrice(),
                            currentProductDetails.getProduct().getDefaultBestBeforeDays(),
                            currentProductDetails.getProduct().getLocationId()
                    );

                    if(currentProductDetails.getProduct().getEnableTareWeightHandling() == 1) {
                        showMessage(getString(R.string.msg_batch_tare_weight));
                        return;
                    }
                    askNecessaryDetails();
                }, error -> {
                    NetworkResponse response = error.networkResponse;
                    if(response != null && response.statusCode == 400) {
                        MissingBatchItem missingBatchItem = getBatchItemFromBarcode(barcode);
                        if(missingBatchItem != null) {
                            currentMissingBatchItem = missingBatchItem;
                            setCurrentValues(
                                    ProductType.MISSING_BATCH_ITEM,
                                    missingBatchItem.getProductName(),
                                    missingBatchItem.getDefaultStoreId(),
                                    null,
                                    missingBatchItem.getDefaultBestBeforeDays(),
                                    missingBatchItem.getDefaultLocationId()
                            );
                            askNecessaryDetails();
                        } else if(!products.isEmpty()) {
                            showChooseBottomSheet(barcode);
                        } else {
                            downloadProducts(
                                    response1 -> showChooseBottomSheet(barcode),
                                    error1 -> {
                                        showMessage(getString(R.string.msg_error));
                                        resumeScan();
                                    }
                            );
                        }
                    } else {
                        showMessage(getString(R.string.msg_error));
                        resumeScan();
                    }
                }
        );
    }

    private void setActionType(String actionType, boolean animated) {
        this.actionType = actionType;

        if(animated) {
            cardViewType.animate().alpha(0).setDuration(300).withEndAction(() -> {
                if(actionType.equals(Constants.ACTION.CONSUME)) {
                    textViewType.setText(getString(R.string.action_consume));
                } else {
                    textViewType.setText(getString(R.string.action_purchase));
                }
                cardViewType.animate().alpha(1).setDuration(300).start();
            }).start();
        } else {
            if(actionType.equals(Constants.ACTION.CONSUME)) {
                textViewType.setText(getString(R.string.action_consume));
            } else {
                textViewType.setText(getString(R.string.action_purchase));
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(actionType.equals(Constants.ACTION.CONSUME)) {
                cardViewType.setTooltipText(getString(R.string.tooltip_switch_purchase));
            } else if(actionType.equals(Constants.ACTION.PURCHASE)) {
                cardViewType.setTooltipText(getString(R.string.tooltip_switch_consume));
            } else {
                cardViewType.setTooltipText(null);
            }
        }

        cardViewCount.setVisibility(
                actionType.equals(Constants.ACTION.CONSUME)
                        ? View.GONE
                        : View.VISIBLE
        );
    }

    private void consumeProduct() {
        JSONObject body = new JSONObject();
        try {
            body.put("amount", 1);
            body.put("transaction_type", "consume");
            body.put("spoiled", false);
            if(entryId != null && !entryId.isEmpty()) {
                body.put("stock_entry_id", entryId);
            }
        } catch (JSONException e) {
            if(DEBUG) Log.e(TAG, "consumeProduct: " + e);
        }
        request.post(
                grocyApi.consumeProduct(currentProductDetails.getProduct().getId()),
                body,
                response -> {
                    // UNDO OPTION
                    String transactionId = null;
                    try {
                        transactionId = response.getString("transaction_id");
                    } catch (JSONException e) {
                        if(DEBUG) Log.e(TAG, "consumeProduct: " + e);
                    }
                    if(DEBUG) Log.i(TAG, "consumeProduct: consumed 1");

                    Snackbar snackbar = Snackbar.make(
                            findViewById(R.id.barcode_scan_batch),
                            getString(
                                    R.string.msg_consumed,
                                    NumUtil.trim(1),
                                    currentProductDetails.getQuantityUnitStock().getName(),
                                    currentProductName
                            ), Snackbar.LENGTH_LONG
                    );

                    if(transactionId != null) {
                        String transId = transactionId;
                        snackbar.setActionTextColor(
                                ContextCompat.getColor(this, R.color.secondary)
                        ).setAction(
                                getString(R.string.action_undo),
                                v -> undoTransaction(transId)
                        );
                    }
                    storeResetSelectedValues();
                    snackbar.show();
                    resumeScan();
                },
                error -> {
                    NetworkResponse networkResponse = error.networkResponse;
                    if(networkResponse != null && networkResponse.statusCode == 400) {
                        showMessage(getString(
                                R.string.msg_not_in_stock,
                                currentProductName
                        ));
                    } else {
                        showMessage(getString(R.string.msg_error));
                    }
                    if(DEBUG) Log.i(TAG, "consumeProduct: " + error);
                    storeResetSelectedValues();
                    resumeScan();
                }
        );
    }

    public void purchaseProduct() {
        JSONObject body = new JSONObject();
        try {
            body.put("amount", 1);
            body.put("transaction_type", "purchase");
            body.put("best_before_date", bestBeforeDate);
            if(!price.isEmpty()) {
                body.put("price", NumUtil.formatPrice(price));
            }
            if(!storeId.isEmpty() && Integer.parseInt(storeId) > -1) {
                body.put("shopping_location_id", Integer.parseInt(storeId));
            }
            body.put("location_id", locationId);
        } catch (JSONException e) {
            if(DEBUG) Log.e(TAG, "purchaseProduct: " + e);
        }
        request.post(
                grocyApi.purchaseProduct(currentProductDetails.getProduct().getId()),
                body,
                response -> {
                    // UNDO OPTION
                    String transactionId = null;
                    try {
                        transactionId = response.getString("transaction_id");
                    } catch (JSONException e) {
                        if (DEBUG) Log.e(TAG, "purchaseProduct: " + e);
                    }
                    if (DEBUG) Log.i(TAG, "purchaseProduct: purchased 1 in batch mode");

                    Snackbar snackbar = Snackbar.make(
                            findViewById(R.id.barcode_scan_batch),
                            getString(
                                    R.string.msg_purchased,
                                    String.valueOf(1),
                                    currentProductDetails.getQuantityUnitStock().getName(),
                                    currentProductDetails.getProduct().getName()
                            ), Snackbar.LENGTH_LONG
                    );
                    if (transactionId != null) {
                        String transId = transactionId;
                        snackbar.setActionTextColor(
                                ContextCompat.getColor(this, R.color.secondary)
                        ).setAction(
                                getString(R.string.action_undo),
                                v -> undoTransaction(transId)
                        );
                    }
                    snackbar.show();
                    storeResetSelectedValues();
                    resumeScan();
                },
                error -> {
                    showMessage(getString(R.string.msg_error));
                    if(DEBUG) Log.i(TAG, "purchaseProduct: " + error);
                    storeResetSelectedValues();
                    resumeScan();
                }
        );
    }

    private void undoTransaction(String transactionId) {
        request.post(
                grocyApi.undoStockTransaction(transactionId),
                success -> {
                    showMessage(getString(R.string.msg_undone_transaction));
                    if(DEBUG) Log.i(TAG, "undoTransaction: undone");
                }, error -> {
                    showMessage(getString(R.string.msg_error));
                    if(DEBUG) Log.i(TAG, "undoTransaction: error: " + error);
                }
        );
    }

    public void purchaseBatchItem(MissingBatchItem missingBatchItem) {
        BatchPurchaseEntry batchPurchaseEntry = new BatchPurchaseEntry(
                bestBeforeDate,
                locationId,
                price,
                storeId
        );
        missingBatchItem.addPurchaseEntry(batchPurchaseEntry);
        // Store default values, so that the fields can be filled in the product creator
        if(!missingBatchItem.getIsDefaultBestBeforeDaysSet()
                && bestBeforeDate.equals(Constants.DATE.NEVER_EXPIRES)
        ) {
            missingBatchItem.setDefaultBestBeforeDays(-1);
        } else {
            missingBatchItem.setDefaultBestBeforeDays(DateUtil.getDaysFromNow(bestBeforeDate));
        }
        if(missingBatchItem.getDefaultLocationId() == -1) {
            missingBatchItem.setDefaultLocationId(Integer.parseInt(locationId));
        }
        if(missingBatchItem.getDefaultStoreId() == null) {
            missingBatchItem.setDefaultStoreId(storeId);
        }
        if(price != null && !price.isEmpty()) {
            missingBatchItem.setLastPrice(price);
        }
        Log.i(TAG, "purchaseBatchItem: " + batchPurchaseEntry.toString());
        showMessage(
                getString(R.string.msg_saved_purchase,
                        missingBatchItem.getProductName())
        );
        storeResetSelectedValues();
        resumeScan();
    }

    public void setCurrentValues(
            ProductType currentProductType,
            String currentProductName,
            String currentDefaultStoreId,
            String currentLastPrice,
            int currentDefaultBestBeforeDays,
            int currentDefaultLocationId
    ) {
        this.currentProductType = currentProductType;
        this.currentProductName = currentProductName;
        this.currentDefaultStoreId = currentDefaultStoreId;
        this.currentLastPrice = currentLastPrice;
        this.currentDefaultBestBeforeDays = currentDefaultBestBeforeDays;
        this.currentDefaultLocationId = currentDefaultLocationId;
    }

    private void storeResetSelectedValues() {
        // BEST BEFORE DATE
        if(bestBeforeDate != null && !bestBeforeDate.isEmpty()) {
            sessionBestBeforeDates.put(currentProductName, bestBeforeDate);
        }
        bestBeforeDate = null;
        // PRICE
        sessionPrices.put(currentProductName, price);
        price = null;
        // STORE
        sessionStoreIds.put(currentProductName, storeId);
        storeId = null;
        // LOCATION
        sessionLocationIds.put(currentProductName, locationId);
        locationId = null;
        // STOCK ENTRY
        entryId = null;
        // STOCK LOCATION
        stockLocationId = null;
    }

    @SuppressLint("SimpleDateFormat")
    public void askNecessaryDetails() {
        if(actionType.equals(Constants.ACTION.CONSUME)) {

            // STOCK LOCATION
            int askForStockLocation = sharedPrefs.getInt(
                    Constants.PREF.BATCH_CONFIG_STOCK_LOCATION, 0
            );
            if(askForStockLocation == 0 && stockLocationId == null) {
                stockLocationId = "";
            } else if(askForStockLocation == 2 && stockLocationId == null) {
                showStockLocationsBottomSheet();
                return;
            }

            // SPECIFIC STOCK ENTRY
            int askForSpecific = sharedPrefs.getInt(
                    Constants.PREF.BATCH_CONFIG_SPECIFIC, 0
            );
            if(askForSpecific == 0 && entryId == null) {
                entryId = "";
            } else if(askForSpecific == 2 && entryId == null) {
                showSpecificEntryBottomSheet();
                return;
            }

            consumeProduct();

        } else if(actionType.equals(Constants.ACTION.PURCHASE)) {

            // BEST BEFORE DATE
            int askForBestBeforeDate = sharedPrefs.getInt(
                    Constants.PREF.BATCH_CONFIG_BBD, 0
            );
            if(askForBestBeforeDate == 0 && bestBeforeDate == null) {
                if(currentDefaultBestBeforeDays == 0) {
                    if(sessionBestBeforeDates.containsKey(currentProductName)) {
                        bestBeforeDate = sessionBestBeforeDates.get(currentProductName);
                    }
                    showBBDateBottomSheet();
                    return;
                } else if(currentDefaultBestBeforeDays == -1) {
                    bestBeforeDate = Constants.DATE.NEVER_EXPIRES;
                } else {
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DAY_OF_MONTH, currentDefaultBestBeforeDays);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    bestBeforeDate = dateFormat.format(calendar.getTime());
                }
            } else if(askForBestBeforeDate == 1 && bestBeforeDate == null) {
                if(sessionBestBeforeDates.containsKey(currentProductName)) {
                    bestBeforeDate = sessionBestBeforeDates.get(currentProductName);
                } else {
                    showBBDateBottomSheet();
                    return;
                }
            } else if(askForBestBeforeDate == 2 && bestBeforeDate == null) {
                if(sessionBestBeforeDates.containsKey(currentProductName)) {
                    bestBeforeDate = sessionBestBeforeDates.get(currentProductName);
                }
                showBBDateBottomSheet();
                return;
            }

            // PRICE
            int askForPrice = sharedPrefs.getInt(Constants.PREF.BATCH_CONFIG_PRICE, 0);
            if(askForPrice == 0 && price == null) {
                price = "";  // price is never required
            } else if(askForPrice == 1 && price == null) {
                if(sessionPrices.containsKey(currentProductName)) {
                    price = sessionPrices.get(currentProductName);
                } else {
                    if(currentLastPrice != null) price = currentLastPrice;
                    showPriceBottomSheet();
                    return;
                }
            } else if(askForPrice == 2 && price == null) {
                if(sessionPrices.containsKey(currentProductName)) {
                    price = sessionPrices.get(currentProductName);
                } else if(currentLastPrice != null) {
                    price = currentLastPrice;
                }
                showPriceBottomSheet();
                return;
            }

            // STORE
            int askForStore = sharedPrefs.getInt(Constants.PREF.BATCH_CONFIG_STORE, 0);
            if(askForStore == 0 && storeId == null) {
                if(currentDefaultStoreId == null) {
                    storeId = "";
                } else {
                    storeId = currentDefaultStoreId;
                }
            } else if(askForStore == 1 && storeId == null) {
                if(sessionStoreIds.containsKey(currentProductName)) {
                    storeId = sessionStoreIds.get(currentProductName);
                } else {
                    if(currentDefaultStoreId != null) storeId = currentDefaultStoreId;
                    showStoresBottomSheet(true);
                    return;
                }
            } else if(askForStore == 2 && storeId == null) {
                if(sessionStoreIds.containsKey(currentProductName)) {
                    storeId = sessionStoreIds.get(currentProductName);
                } else if(currentDefaultStoreId != null) {
                    storeId = currentDefaultStoreId;
                }
                showStoresBottomSheet(true);
                return;
            }

            // LOCATION
            int askForLocation = sharedPrefs.getInt(
                    Constants.PREF.BATCH_CONFIG_LOCATION, 0
            );
            if(askForLocation == 0 && locationId == null) {
                if(currentDefaultLocationId == -1) {
                    showLocationsBottomSheet(true);
                    return;
                } else {
                    locationId = String.valueOf(currentDefaultLocationId);
                }
            } else if(askForLocation == 1 && locationId == null) {
                if(sessionLocationIds.containsKey(currentProductName)) {
                    locationId = sessionLocationIds.get(currentProductName);
                } else {
                    locationId = String.valueOf(currentDefaultLocationId);
                    showLocationsBottomSheet(true);
                    return;
                }
            } else if(askForLocation == 2 && locationId == null) {
                if(sessionLocationIds.containsKey(currentProductName)) {
                    locationId = sessionLocationIds.get(currentProductName);
                } else {
                    locationId = String.valueOf(currentDefaultLocationId);
                }
                showLocationsBottomSheet(true);
                return;
            }

            if(currentProductType == ProductType.PRODUCT) {
                purchaseProduct();
            } else if(currentProductType == ProductType.MISSING_BATCH_ITEM) {
                purchaseBatchItem(currentMissingBatchItem);
            }

        }
    }

    public void discardCurrentProduct() {
        bestBeforeDate = null;
        price = null;
        storeId = null;
        locationId = null;
        entryId = null;
        stockLocationId = null;
        resumeScan();
    }

    private void showChooseBottomSheet(String barcode) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ARGUMENT.TYPE, actionType);
        bundle.putString(Constants.ARGUMENT.BARCODE, barcode);
        bundle.putParcelableArrayList(Constants.ARGUMENT.PRODUCTS, products);
        bundle.putStringArrayList(Constants.ARGUMENT.PRODUCT_NAMES, productNames);
        bundle.putParcelableArrayList(Constants.ARGUMENT.BATCH_ITEMS, missingBatchItems);
        showBottomSheet(new BatchChooseBottomSheetDialogFragment(), bundle);
    }

    private void showBBDateBottomSheet() {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ARGUMENT.SELECTED_DATE, bestBeforeDate);
        bundle.putString(
                Constants.ARGUMENT.DEFAULT_BEST_BEFORE_DAYS,
                String.valueOf(currentDefaultBestBeforeDays)
        );
        showBottomSheet(new BBDateBottomSheetDialogFragment(), bundle);
    }

    private void showPriceBottomSheet() {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ARGUMENT.PRICE, price);
        String currency = sharedPrefs.getString(Constants.PREF.CURRENCY, "");
        bundle.putString(Constants.ARGUMENT.CURRENCY, currency);
        showBottomSheet(new PriceBottomSheetDialogFragment(), bundle);
    }

    private void showStoresBottomSheet(boolean tryDownload) {
        if(stores.isEmpty() && tryDownload) {
            downloadStores(
                    response -> showStoresBottomSheet(false),
                    error -> {
                        discardCurrentProduct();
                        showMessage(getString(R.string.msg_error));
                    }
            );
            return;
        } else if(stores.isEmpty()) {
            storeId = "";
            askNecessaryDetails();
            return;
        }
        Bundle bundle = new Bundle();
        if(storeId != null && !storeId.isEmpty()) {
            bundle.putInt(Constants.ARGUMENT.SELECTED_ID, Integer.parseInt(storeId));
        } else {
            bundle.putInt(Constants.ARGUMENT.SELECTED_ID, -1);
        }
        bundle.putParcelableArrayList(Constants.ARGUMENT.STORES, stores);
        showBottomSheet(new StoresBottomSheetDialogFragment(), bundle);
    }

    private void showLocationsBottomSheet(boolean tryDownload) {
        if(locations.isEmpty() && tryDownload) {
            downloadLocations(
                    response -> showLocationsBottomSheet(false),
                    error -> {
                        discardCurrentProduct();
                        showMessage(getString(R.string.msg_error));
                    }
            );
            return;
        } else if(locations.isEmpty()) {
            showMessage(getString(R.string.msg_error));
            discardCurrentProduct();
            return;
        }
        Bundle bundle = new Bundle();
        if(locationId != null && !locationId.isEmpty()) {
            bundle.putInt(Constants.ARGUMENT.SELECTED_ID, Integer.parseInt(locationId));
        } else {
            bundle.putInt(Constants.ARGUMENT.SELECTED_ID, -1);
        }
        bundle.putParcelableArrayList(Constants.ARGUMENT.LOCATIONS, locations);
        showBottomSheet(new LocationsBottomSheetDialogFragment(), bundle);
    }

    private void showSpecificEntryBottomSheet() {
        request.get(
                grocyApi.getStockEntriesFromProduct(currentProductDetails.getProduct().getId()),
                response -> {
                    ArrayList<StockEntry> stockEntries = gson.fromJson(
                            response,
                            new TypeToken<ArrayList<StockEntry>>(){}.getType()
                    );
                    if(stockEntries.isEmpty()) {
                        showMessage(getString(
                                R.string.msg_not_in_stock,
                                currentProductName
                        ));
                        discardCurrentProduct();
                        return;
                    }
                    ArrayList<StockEntry> filteredStockEntries = new ArrayList<>();
                    if(stockLocationId != null && !stockLocationId.isEmpty()) {
                        for(StockEntry stockEntry : stockEntries) {
                            if(stockEntry.getLocationId() == Integer.parseInt(stockLocationId)) {
                                filteredStockEntries.add(stockEntry);
                            }
                        }
                        if(filteredStockEntries.size() == 1) {
                            setEntryId(String.valueOf(filteredStockEntries.get(0).getId()));
                            askNecessaryDetails();
                            return;
                        }
                    } else {
                        filteredStockEntries.addAll(stockEntries);
                    }
                    Bundle bundle = new Bundle();
                    bundle.putParcelableArrayList(
                            Constants.ARGUMENT.STOCK_ENTRIES,
                            filteredStockEntries
                    );
                    bundle.putString(Constants.ARGUMENT.SELECTED_ID, entryId);
                    showBottomSheet(new StockEntriesBottomSheetDialogFragment(), bundle);
                },
                error -> showMessage(getString(R.string.msg_error))
        );
    }

    private void showStockLocationsBottomSheet() {
        request.get(
                grocyApi.getStockLocationsFromProduct(currentProductDetails.getProduct().getId()),
                response -> {
                    ArrayList<StockLocation> stockLocations = gson.fromJson(
                            response,
                            new TypeToken<ArrayList<StockLocation>>(){}.getType()
                    );
                    if(stockLocations.isEmpty()) {
                        showMessage(getString(
                                R.string.msg_not_in_stock,
                                currentProductName
                        ));
                        discardCurrentProduct();
                        return;
                    }
                    if(stockLocations.size() == 1) {
                        setStockLocationId(String.valueOf(stockLocations.get(0).getLocationId()));
                        askNecessaryDetails();
                        return;
                    }
                    Bundle bundle = new Bundle();
                    bundle.putParcelableArrayList(
                            Constants.ARGUMENT.STOCK_LOCATIONS,
                            stockLocations
                    );
                    bundle.putString(Constants.ARGUMENT.SELECTED_ID, stockLocationId);
                    bundle.putParcelable(Constants.ARGUMENT.PRODUCT_DETAILS, currentProductDetails);
                    showBottomSheet(new StockLocationsBottomSheetDialogFragment(), bundle);
                },
                error -> showMessage(getString(R.string.msg_error))
        );
    }

    private ArrayList<String> getProductNames() {
        ArrayList<String> names = new ArrayList<>();
        if(products != null) {
            for(Product product : products) {
                names.add(product.getName());
            }
        }
        return names;
    }

    public void createMissingBatchItemPurchase(String productName, String barcode) {
        MissingBatchItem missingBatchItem = new MissingBatchItem(productName, barcode);
        missingBatchItems.add(missingBatchItem);
        productNames.add(productName);
        refreshCounter();

        // purchase it
        currentMissingBatchItem = missingBatchItem;
        setCurrentValues(
                ProductType.MISSING_BATCH_ITEM,
                missingBatchItem.getProductName(),
                missingBatchItem.getDefaultStoreId(),
                null,
                missingBatchItem.getDefaultBestBeforeDays(),
                missingBatchItem.getDefaultLocationId()
        );
        askNecessaryDetails();
    }

    public void addBatchItemBarcode(String barcode, String inputText) {
        List<String> barcodes;
        MissingBatchItem missingBatchItem = null;
        for(MissingBatchItem missingBatchItemTmp : missingBatchItems) {
            if(missingBatchItemTmp.getProductName().equals(inputText)) {
                missingBatchItem = missingBatchItemTmp;
                break;
            }
        }
        if(missingBatchItem == null) {
            showMessage(getString(R.string.msg_error));
            return;
        }

        if(missingBatchItem.getBarcodes() != null && !missingBatchItem.getBarcodes().isEmpty()) {
            barcodes = new ArrayList<>(Arrays.asList(
                    missingBatchItem.getBarcodes().split(",")
            ));
        } else {
            barcodes = new ArrayList<>();
        }
        barcodes.add(barcode);
        missingBatchItem.setBarcodes(TextUtils.join(",", barcodes));

        // purchase it
        currentMissingBatchItem = missingBatchItem;
        setCurrentValues(
                ProductType.MISSING_BATCH_ITEM,
                missingBatchItem.getProductName(),
                missingBatchItem.getDefaultStoreId(),
                missingBatchItem.getLastPrice(),
                missingBatchItem.getDefaultBestBeforeDays(),
                missingBatchItem.getDefaultLocationId()
        );
        askNecessaryDetails();
    }

    private void refreshCounter() {
        String text = missingBatchItems.size() + " ";
        textViewCount.setText(text);
    }

    public MissingBatchItem getBatchItemFromBarcode(String barcode) {
        for(MissingBatchItem missingBatchItem : missingBatchItems) {
            String barcodesString = missingBatchItem.getBarcodes();
            if(barcodesString == null || barcodesString.trim().isEmpty()) return null;
            String[] barcodes = barcodesString.trim().split(",");
            for(String tmpBarcode : barcodes) {
                if(tmpBarcode.trim().equals(barcode)) return missingBatchItem;
            }
        }
        return null;
    }

    private void showMessage(String msg) {
        Snackbar.make(
                findViewById(R.id.barcode_scan_batch),
                msg,
                Snackbar.LENGTH_SHORT
        ).show();
    }

    public void pauseScan() {
        barcodeRipple.pauseAnimation();
        capture.onPause();
    }

    public void resumeScan() {
        barcodeRipple.resumeAnimation();
        capture.onResume();
    }

    private boolean hasFlash() {
        return getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH
        );
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBarcodeResult(BarcodeResult result) {
        barcodeRipple.pauseAnimation();

        loadProductDetailsByBarcode(result.getText());
    }

    public void showBottomSheet(BottomSheetDialogFragment bottomSheet, Bundle bundle) {
        String tag = bottomSheet.toString();
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment == null || !fragment.isVisible()) {
            if(bundle != null) bottomSheet.setArguments(bundle);
            fragmentManager.beginTransaction().add(bottomSheet, tag).commit();
            if(DEBUG) Log.i(TAG, "showBottomSheet: " + tag);
        } else if(DEBUG) Log.e(TAG, "showBottomSheet: sheet already visible");
    }

    public RequestQueue getRequestQueue() {
        return requestQueue;
    }

    public GrocyApi getGrocy() {
        return grocyApi;
    }

    public void setBestBeforeDate(String bestBeforeDate) {
        this.bestBeforeDate = bestBeforeDate;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public void setStockLocationId(String stockLocationId) {
        this.stockLocationId = stockLocationId;
    }

    public boolean isOpenFoodFactsEnabled() {
        return sharedPrefs.getBoolean(Constants.PREF.FOOD_FACTS, false);
    }

    public boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE
        );
        assert connectivityManager != null;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public void showKeyboard(EditText editText) {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(
                editText,
                InputMethodManager.SHOW_IMPLICIT
        );
    }

    public void hideKeyboard() {
        ((InputMethodManager) Objects
                .requireNonNull(getSystemService(Context.INPUT_METHOD_SERVICE)))
                .hideSoftInputFromWindow(
                        findViewById(android.R.id.content).getWindowToken(),
                        0
                );
    }

    private void hideInfo() {
        findViewById(R.id.card_scan_batch_info)
                .animate()
                .alpha(0)
                .setDuration(300)
                .setStartDelay(4000)
                .start();
    }

    private void switchTorch() {
        if(isTorchOn) {
            barcodeScannerView.setTorchOff();
        } else {
            barcodeScannerView.setTorchOn();
        }
    }

    @Override
    public void onTorchOn() {
        buttonFlash.setIcon(R.drawable.ic_round_flash_off_to_on);
        buttonFlash.startIconAnimation();
        isTorchOn = true;
    }

    @Override
    public void onTorchOff() {
        buttonFlash.setIcon(R.drawable.ic_round_flash_on_to_off);
        buttonFlash.startIconAnimation();
        isTorchOn = false;
    }

    public interface OnResponseListener {
        void onResponse(String response);
    }

    public interface OnErrorListener {
        void onError(VolleyError error);
    }
}
