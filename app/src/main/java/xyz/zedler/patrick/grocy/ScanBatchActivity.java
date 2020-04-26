package xyz.zedler.patrick.grocy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.model.BatchItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.scan.ScanBatchCaptureManager;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.view.ActionButton;
import xyz.zedler.patrick.grocy.view.BarcodeRipple;
import xyz.zedler.patrick.grocy.web.RequestQueueSingleton;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class ScanBatchActivity extends AppCompatActivity
        implements ScanBatchCaptureManager.BarcodeListener, DecoratedBarcodeView.TorchListener {

    private final static String TAG = Constants.UI.CONSUME_BATCH;
    private final static boolean DEBUG = true;

    private ScanBatchCaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private BarcodeRipple barcodeRipple;
    private ActionButton actionButtonFlash;
    private boolean isTorchOn;

    private SharedPreferences sharedPrefs;
    private FragmentManager fragmentManager;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private RequestQueue requestQueue;
    private WebRequest request;
    private ArrayAdapter<String> adapterProducts;
    private ProductDetails productDetails;

    private List<Product> products = new ArrayList<>();
    private List<BatchItem> batchItems = new ArrayList<>();
    private List<String> productNames = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isTorchOn = false;

        // WEB REQUESTS

        requestQueue = RequestQueueSingleton.getInstance(getApplicationContext()).getRequestQueue();
        request = new WebRequest(requestQueue);

        // API

        grocyApi = new GrocyApi(this);

        downloadProductNames();

        // INITIALIZE VIEWS

        setContentView(R.layout.activity_scan_batch);

        findViewById(R.id.button_scan_batch_close).setOnClickListener(v -> {
            finish();
        });
        findViewById(R.id.button_scan_batch_flash).setOnClickListener(v -> {
            switchTorch();
        });

        barcodeScannerView = findViewById(R.id.barcode_scan_batch);
        barcodeScannerView.setTorchOff();
        barcodeScannerView.setTorchListener(this);

        actionButtonFlash = findViewById(R.id.button_scan_batch_flash);
        actionButtonFlash.setIcon(R.drawable.ic_round_flash_off_to_on);

        barcodeRipple = findViewById(R.id.ripple_scan);

        if(!hasFlash()) {
            findViewById(R.id.frame_scan_flash).setVisibility(View.GONE);
        }

        fragmentManager = getSupportFragmentManager();

        capture = new ScanBatchCaptureManager(this, barcodeScannerView, this);
        capture.decode();
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

    private void downloadProductNames() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS),
                response -> {
                    products = gson.fromJson(
                            response,
                            new TypeToken<List<Product>>(){}.getType()
                    );
                    productNames = getProductNames();
                    adapterProducts = new ArrayAdapter<>(
                            this, android.R.layout.simple_list_item_1, productNames
                    );
                    // download finished
                    //swipeRefreshLayout.setRefreshing(false);
                }, error -> {
                    //swipeRefreshLayout.setRefreshing(false);
                    showSnackbar(
                            Snackbar.make(
                                    findViewById(R.id.barcode_scan_batch),
                                    getString(R.string.msg_error),
                                    Snackbar.LENGTH_SHORT
                            ).setActionTextColor(
                                    ContextCompat.getColor(this, R.color.secondary)
                            ).setAction(
                                    getString(R.string.action_retry),
                                    v1 -> downloadProductNames()
                            )
                    );
                }
        );
    }

    private void loadProductDetailsByBarcode(String barcode) {
        request.get(
                grocyApi.getStockProductByBarcode(barcode),
                response -> {
                    productDetails = gson.fromJson(
                            response,
                            new TypeToken<ProductDetails>(){}.getType()
                    );
                    consumeProduct();
                    //TextView textView = findViewById(R.id.scan_batch_status_view);
                    //textView.setText(productDetails.getProduct().getName());
                }, error -> {
                    NetworkResponse response = error.networkResponse;
                    if(response != null && response.statusCode == 400) {
                        showSnackbar(
                                Snackbar.make(
                                        findViewById(R.id.barcode_scan_batch),
                                        "This product is not in database",
                                        Snackbar.LENGTH_LONG
                                ).setActionTextColor(
                                        ContextCompat.getColor(this, R.color.secondary)
                                ).setAction(
                                        getString(R.string.action_create),
                                        v1 -> {}
                                )
                        );
                    } else {
                        showSnackbar(
                                Snackbar.make(
                                        findViewById(R.id.barcode_scan_batch),
                                        getString(R.string.msg_error),
                                        Snackbar.LENGTH_SHORT
                                )
                        );
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
                grocyApi.consumeProduct(productDetails.getProduct().getId()),
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
                                    productDetails.getQuantityUnitStock().getName(),
                                    productDetails.getProduct().getName()
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
                    showSnackbar(snackbar);
                },
                error -> {
                    showErrorMessage(error);
                    if(DEBUG) Log.i(TAG, "consumeProduct: " + error);
                }
        );
    }

    private void undoTransaction(String transactionId) {
        request.post(
                grocyApi.undoStockTransaction(transactionId),
                success -> {
                    showSnackbar(
                            Snackbar.make(
                                    findViewById(R.id.barcode_scan_batch),
                                    getString(R.string.msg_undone_transaction),
                                    Snackbar.LENGTH_SHORT
                            )
                    );
                    if(DEBUG) Log.i(TAG, "undoTransaction: undone");
                },
                this::showErrorMessage
        );
    }

    private List<String> getProductNames() {
        List<String> names = new ArrayList<>();
        if(products != null) {
            for(Product product : products) {
                names.add(product.getName());
            }
        }
        return names;
    }

    private void showErrorMessage(VolleyError error) {
        showSnackbar(
                Snackbar.make(
                        findViewById(R.id.barcode_scan_batch),
                        getString(R.string.msg_error),
                        Snackbar.LENGTH_SHORT
                )
        );
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
        loadProductDetailsByBarcode(result.getText());

        barcodeRipple.pauseAnimation();

        new Handler().postDelayed(() -> {
            barcodeRipple.resumeAnimation();
            capture.onResume();
        }, 700);

        Intent resultIntent = new Intent("test");
        resultIntent.putExtra(Constants.EXTRA.SCAN_RESULT, result.getText());
        //sendBroadcast(resultIntent);
        //setResult(Activity.RESULT_OK, resultIntent);
        //finish();
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

    public void hideKeyboard() {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(
                        findViewById(android.R.id.content).getWindowToken(),
                        0
                );
    }

    public boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE
        );
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public void showSnackbar(Snackbar snackbar) {
        snackbar.show();
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
        actionButtonFlash.setIcon(R.drawable.ic_round_flash_off_to_on);
        actionButtonFlash.startIconAnimation();
        isTorchOn = true;
    }

    @Override
    public void onTorchOff() {
        actionButtonFlash.setIcon(R.drawable.ic_round_flash_on_to_off);
        actionButtonFlash.startIconAnimation();
        isTorchOn = false;
    }
}
