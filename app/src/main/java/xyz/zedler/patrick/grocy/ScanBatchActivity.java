package xyz.zedler.patrick.grocy;

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
import android.os.SystemClock;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.BBDateBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.BatchChooseBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.BatchConfigBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ExitScanBatchBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LocationsBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.PriceBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StoresBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.model.BatchPurchaseEntry;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.MissingBatchItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.scan.ScanBatchCaptureManager;
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
    private ActionButton buttonFlash,buttonConfig;
    private TextView textViewCount;
    private boolean isTorchOn;
    private long lastClick = 0;

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
    private String currentProductName;
    private ProductType currentProductType;
    private String currentDefaultStoreId;
    private String currentLastPrice;
    private int currentDefaultBestBeforeDays = 0;
    private int currentDefaultLocationId = -1;

    private enum ProductType {
        PRODUCT, MISSING_BATCH_ITEM
    }

    private String bestBeforeDate;
    private String price;
    private String storeId;
    private String locationId;

    private Map<String, String> savedBestBeforeDates = new HashMap<>();
    private Map<String, String> savedPrices = new HashMap<>();
    private Map<String, String> savedStoreIds = new HashMap<>();
    private Map<String, String> savedLocationIds = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        intent = getIntent();
        actionType = intent.getStringExtra(Constants.ARGUMENT.TYPE);
        if(actionType == null) showSnackbarMessage(getString(R.string.msg_error));

        isTorchOn = false;

        // PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // WEB REQUESTS

        requestQueue = RequestQueueSingleton.getInstance(getApplicationContext()).getRequestQueue();
        request = new WebRequest(requestQueue);

        // API

        grocyApi = new GrocyApi(this);

        // INITIALIZE VIEWS

        setContentView(R.layout.activity_scan_batch);

        ActionButton buttonClose = findViewById(R.id.button_scan_batch_close);
        buttonClose.setOnClickListener(v -> onBackPressed());
        buttonClose.setTooltipText(getString(R.string.action_close));

        textViewCount = findViewById(R.id.text_scan_batch_count);
        refreshCounter();

        MaterialCardView cardViewCount = findViewById(R.id.card_scan_batch_count);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            cardViewCount.setTooltipText(getString(R.string.tooltip_new_products_count));
        }
        String type = intent.getStringExtra(Constants.ARGUMENT.TYPE);
        if(type == null) finish();
        assert type != null;
        cardViewCount.setVisibility(
                type.equals(Constants.ACTION.PURCHASE)
                        ? View.VISIBLE
                        : View.GONE
        );

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
                showSnackbarMessage(getString(R.string.msg_batch_no_products));
            }
        });

        barcodeScannerView = findViewById(R.id.barcode_scan_batch);
        barcodeScannerView.setTorchOff();
        barcodeScannerView.setTorchListener(this);

        buttonConfig = findViewById(R.id.button_scan_batch_config);
        buttonConfig.setOnClickListener(v -> {
            if (SystemClock.elapsedRealtime() - lastClick < 1000) return;
            lastClick = SystemClock.elapsedRealtime();
            pauseScan();
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

        // LOAD NECESSARY OBJECTS
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
                        showSnackbarMessage(getString(R.string.msg_batch_tare_weight));
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
                                        showSnackbarMessage(getString(R.string.msg_error));
                                        resumeScan();
                                    }
                            );
                        }
                    } else {
                        showSnackbarMessage(getString(R.string.msg_error));
                        resumeScan();
                    }
                }
        );
    }

    private void consumeProduct() {
        JSONObject body = new JSONObject();
        try {
            body.put("amount", 1);
            body.put("transaction_type", "consume");
            body.put("spoiled", false);
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
                    snackbar.show();
                    resumeScan();
                },
                error -> {
                    NetworkResponse networkResponse = error.networkResponse;
                    if(networkResponse != null && networkResponse.statusCode == 400) {
                        showSnackbarMessage(getString(
                                R.string.msg_not_in_stock,
                                currentProductName
                        ));
                    } else {
                        showSnackbarMessage(getString(R.string.msg_error));
                    }
                    if(DEBUG) Log.i(TAG, "consumeProduct: " + error);
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
            if(!price.equals("")) {
                body.put("price", NumUtil.formatPrice(price));
            }
            if(!storeId.equals("") && Integer.parseInt(storeId) > -1) {
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
                    showSnackbarMessage(getString(R.string.msg_error));
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
                    showSnackbarMessage(getString(R.string.msg_undone_transaction));
                    if(DEBUG) Log.i(TAG, "undoTransaction: undone");
                }, error -> {
                    showSnackbarMessage(getString(R.string.msg_error));
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
        if(!missingBatchItem.getIsDefaultBestBeforeDaysSet()) {
            missingBatchItem.setDefaultBestBeforeDays(DateUtil.getDaysFromNow(bestBeforeDate));
        }
        if(missingBatchItem.getDefaultLocationId() == -1) {
            missingBatchItem.setDefaultLocationId(Integer.parseInt(locationId));
        }
        if(missingBatchItem.getDefaultStoreId() == null) {
            missingBatchItem.setDefaultStoreId(storeId);
        }
        Log.i(TAG, "purchaseBatchItem: " + batchPurchaseEntry.toString());
        showSnackbarMessage(
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
        if(bestBeforeDate != null && !bestBeforeDate.equals("")) {
            savedBestBeforeDates.put(currentProductName, bestBeforeDate);
        }
        bestBeforeDate = null;
        // PRICE
        savedPrices.put(currentProductName, price);
        price = null;
        // STORE
        savedStoreIds.put(currentProductName, storeId);
        storeId = null;
        // LOCATION
        savedLocationIds.put(currentProductName, locationId);
        locationId = null;
    }

    @SuppressLint("SimpleDateFormat")
    public void askNecessaryDetails() {

        if(actionType.equals(Constants.ACTION.CONSUME)) {

            // TODO: SPECIFIC & STOCK LOCATION

            consumeProduct();

        } else if(actionType.equals(Constants.ACTION.PURCHASE)) {

            // BEST BEFORE DATE
            int askForBestBeforeDate = sharedPrefs.getInt(
                    Constants.PREF.BATCH_CONFIG_BBD, 0
            );
            if(askForBestBeforeDate == 0 && bestBeforeDate == null) {
                if(currentDefaultBestBeforeDays == 0) {
                    if(savedBestBeforeDates.containsKey(currentProductName)) {
                        bestBeforeDate = savedBestBeforeDates.get(currentProductName);
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
                if(savedBestBeforeDates.containsKey(currentProductName)) {
                    bestBeforeDate = savedBestBeforeDates.get(currentProductName);
                } else {
                    showBBDateBottomSheet();
                    return;
                }
            } else if(askForBestBeforeDate == 2 && bestBeforeDate == null) {
                if(savedBestBeforeDates.containsKey(currentProductName)) {
                    bestBeforeDate = savedBestBeforeDates.get(currentProductName);
                }
                showBBDateBottomSheet();
                return;
            }

            // PRICE
            int askForPrice = sharedPrefs.getInt(Constants.PREF.BATCH_CONFIG_PRICE, 0);
            if(askForPrice == 0 && price == null) {
                price = "";  // price is never required
            } else if(askForPrice == 1 && price == null) {
                if(savedPrices.containsKey(currentProductName)) {
                    price = savedPrices.get(currentProductName);
                } else {
                    if(currentLastPrice != null) price = currentLastPrice;
                    showPriceBottomSheet();
                    return;
                }
            } else if(askForPrice == 2 && price == null) {
                if(savedPrices.containsKey(currentProductName)) {
                    price = savedPrices.get(currentProductName);
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
                if(savedStoreIds.containsKey(currentProductName)) {
                    storeId = savedStoreIds.get(currentProductName);
                } else {
                    if(currentDefaultStoreId != null) storeId = currentDefaultStoreId;
                    showStoresBottomSheet();
                    return;
                }
            } else if(askForStore == 2 && storeId == null) {
                if(savedStoreIds.containsKey(currentProductName)) {
                    storeId = savedStoreIds.get(currentProductName);
                } else if(currentDefaultStoreId != null) {
                    storeId = currentDefaultStoreId;
                }
                showStoresBottomSheet();
                return;
            }

            // LOCATION
            int askForLocation = sharedPrefs.getInt(
                    Constants.PREF.BATCH_CONFIG_LOCATION, 0
            );
            if(askForLocation == 0 && locationId == null) {
                if(currentDefaultLocationId == -1) {
                    showLocationsBottomSheet();
                    return;
                } else {
                    locationId = String.valueOf(currentDefaultLocationId);
                }
            } else if(askForLocation == 1 && locationId == null) {
                if(savedLocationIds.containsKey(currentProductName)) {
                    locationId = savedLocationIds.get(currentProductName);
                } else {
                    locationId = String.valueOf(currentDefaultLocationId);
                    showLocationsBottomSheet();
                    return;
                }
            } else if(askForLocation == 2 && locationId == null) {
                if(savedLocationIds.containsKey(currentProductName)) {
                    locationId = savedLocationIds.get(currentProductName);
                } else {
                    locationId = String.valueOf(currentDefaultLocationId);
                }
                showLocationsBottomSheet();
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
        resumeScan();
    }

    private void showChooseBottomSheet(String barcode) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ARGUMENT.TYPE, intent.getStringExtra(Constants.ARGUMENT.TYPE));
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

    private void showStoresBottomSheet() {
        if(stores.isEmpty()) {
            downloadStores(
                    response -> showStoresBottomSheet(),
                    error -> {
                        discardCurrentProduct();
                        showSnackbarMessage(getString(R.string.msg_error));
                    }
            );
            return;
        }
        Bundle bundle = new Bundle();
        if(storeId != null && !storeId.equals("")) {
            bundle.putInt(Constants.ARGUMENT.SELECTED_ID, Integer.parseInt(storeId));
        } else {
            bundle.putInt(Constants.ARGUMENT.SELECTED_ID, -1);
        }
        bundle.putParcelableArrayList(Constants.ARGUMENT.STORES, stores);
        showBottomSheet(new StoresBottomSheetDialogFragment(), bundle);
    }

    private void showLocationsBottomSheet() {
        if(locations.isEmpty()) {
            downloadLocations(
                    response -> showLocationsBottomSheet(),
                    error -> {
                        discardCurrentProduct();
                        showSnackbarMessage(getString(R.string.msg_error));
                    }
            );
            return;
        }
        Bundle bundle = new Bundle();
        if(locationId != null && !locationId.equals("")) {
            bundle.putInt(Constants.ARGUMENT.SELECTED_ID, Integer.parseInt(locationId));
        } else {
            bundle.putInt(Constants.ARGUMENT.SELECTED_ID, -1);
        }
        bundle.putParcelableArrayList(Constants.ARGUMENT.LOCATIONS, locations);
        showBottomSheet(new LocationsBottomSheetDialogFragment(), bundle);
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

    public void setMissingBatchItems(ArrayList<MissingBatchItem> missingBatchItems) {
        this.missingBatchItems = missingBatchItems;
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

    private void refreshCounter() {
        String text = missingBatchItems.size() + " ";
        textViewCount.setText(text);
    }

    public MissingBatchItem getBatchItemFromBarcode(String barcode) {
        for(MissingBatchItem missingBatchItem : missingBatchItems) {
            String barcodesString = missingBatchItem.getBarcodes();
            if(barcodesString == null || barcodesString.trim().equals("")) return null;
            String[] barcodes = barcodesString.trim().split(",");
            for(String tmpBarcode : barcodes) {
                if(tmpBarcode.trim().equals(barcode)) return missingBatchItem;
            }
        }
        return null;
    }

    private void showSnackbarMessage(String msg) {
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

    public boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE
        );
        assert connectivityManager != null;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public void showKeyboard(EditText editText) {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(
                        editText,
                        InputMethodManager.SHOW_IMPLICIT
                );
    }

    public void hideKeyboard() {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
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
