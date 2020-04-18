package xyz.zedler.patrick.grocy.fragment;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialAutoCompleteTextView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.ScanInputActivity;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.StockEntries;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.model.StockLocations;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.view.InputChip;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class ConsumeFragment extends Fragment {

    private final static String TAG = ConsumeFragment.class.getSimpleName();
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private WebRequest request;
    private ArrayAdapter<String> adapterProducts;
    private ProductDetails productDetails;

    private List<Product> products = new ArrayList<>();
    private List<StockLocation> stockLocations = new ArrayList<>();
    private List<StockEntry> stockEntries = new ArrayList<>();
    private List<String> productNames = new ArrayList<>();

    private SwipeRefreshLayout swipeRefreshLayout;
    private MaterialAutoCompleteTextView autoCompleteTextViewProduct;
    private LinearLayout linearLayoutBarcodesContainer;
    private TextInputLayout textInputProduct, textInputAmount;
    private EditText editTextAmount;
    private TextView textViewLocation, textViewSpecific;
    private MaterialCheckBox checkBoxSpoiled;
    private ImageView imageViewAmount;
    private int selectedLocationId;
    private String selectedStockEntryId;
    private double amount, maxAmount, minAmount;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_consume, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (MainActivity) getActivity();
        assert activity != null;

        // GET PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        // WEB REQUESTS

        request = new WebRequest(activity.getRequestQueue());
        grocyApi = activity.getGrocy();

        // INITIALIZE VIEWS

        activity.findViewById(R.id.frame_back_consume).setOnClickListener(
                v -> activity.onBackPressed()
        );

        // swipe refresh

        swipeRefreshLayout = activity.findViewById(R.id.swipe_consume);
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        swipeRefreshLayout.setOnRefreshListener(this::refresh);

        // product

        textInputProduct = activity.findViewById(R.id.text_input_consume_product);
        textInputProduct.setErrorIconDrawable(null);
        textInputProduct.setEndIconOnClickListener(v -> startActivityForResult(
                new Intent(activity, ScanInputActivity.class),
                Constants.REQUEST.SCAN
        ));
        autoCompleteTextViewProduct = (MaterialAutoCompleteTextView) textInputProduct.getEditText();
        assert autoCompleteTextViewProduct != null;
        autoCompleteTextViewProduct.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) {
                startAnimatedIcon(R.id.image_consume_product);
                // try again to download products
                if(productNames.isEmpty()) downloadProductNames();
            }
        });
        autoCompleteTextViewProduct.setOnItemClickListener(
                (parent, view, position, id) -> loadProductDetails(
                        products.get(
                                productNames.indexOf(
                                        String.valueOf(parent.getItemAtPosition(position))
                                )
                        ).getId()
                )
        );
        autoCompleteTextViewProduct.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        editTextAmount.requestFocus();
                        return true;
                    } return false;
        });

        // barcodes

        linearLayoutBarcodesContainer = activity.findViewById(
                R.id.linear_consume_barcode_container
        );

        // amount

        textInputAmount = activity.findViewById(R.id.text_input_consume_amount);
        imageViewAmount = activity.findViewById(R.id.image_consume_amount);
        editTextAmount = textInputAmount.getEditText();
        assert editTextAmount != null;
        editTextAmount.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if(!input.equals("")) {
                    amount = Double.parseDouble(input);
                } else {
                    amount = 0;
                }
                isAmountValid();
            }
        });
        editTextAmount.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) {
                startAnimatedIcon(imageViewAmount);
                // editTextAmount.selectAll();
            }
        });
        editTextAmount.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                clearInputFocus();
                return true;
            } return false;
        });

        activity.findViewById(R.id.button_consume_amount_more).setOnClickListener(v -> {
            startAnimatedIcon(R.id.image_consume_amount);
            if(editTextAmount.getText().toString().equals("")) {
                editTextAmount.setText(String.valueOf(1));
            } else {
                double amountNew = Double.parseDouble(editTextAmount.getText().toString()) + 1;
                if(amountNew <= maxAmount) {
                    editTextAmount.setText(NumUtil.trim(amountNew));
                }
            }
        });

        activity.findViewById(R.id.button_consume_amount_less).setOnClickListener(v -> {
            if(!editTextAmount.getText().toString().equals("")) {
                startAnimatedIcon(R.id.image_consume_amount);
                double amountNew = Double.parseDouble(editTextAmount.getText().toString()) - 1;
                if(amountNew >= minAmount) {
                    editTextAmount.setText(NumUtil.trim(amountNew));
                }
            }
        });

        // location

        activity.findViewById(R.id.linear_consume_location).setOnClickListener(v -> {
            startAnimatedIcon(R.id.image_consume_location);
            if(productDetails != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(
                        Constants.ARGUMENT.STOCK_LOCATIONS,
                        new StockLocations(stockLocations)
                );
                bundle.putParcelable(Constants.ARGUMENT.PRODUCT_DETAILS, productDetails);
                bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedLocationId);
                activity.showBottomSheet(new StockLocationsBottomSheetDialogFragment(), bundle);
            } else {
                // no product selected
                textInputProduct.setError(activity.getString(R.string.error_select_product));
            }
        });
        textViewLocation = activity.findViewById(R.id.text_consume_location);

        // specific

        activity.findViewById(R.id.linear_consume_specific).setOnClickListener(v -> {
            startAnimatedIcon(R.id.image_consume_specific);
            if(productDetails != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(
                        Constants.ARGUMENT.STOCK_ENTRIES,
                        new StockEntries(stockEntries)
                );
                bundle.putString(Constants.ARGUMENT.SELECTED_ID, selectedStockEntryId);
                activity.showBottomSheet(new StockEntriesBottomSheetDialogFragment(), bundle);
            } else {
                // no product selected
                textInputProduct.setError(activity.getString(R.string.error_select_product));
            }
        });
        textViewSpecific = activity.findViewById(R.id.text_consume_specific);

        // spoiled

        checkBoxSpoiled = activity.findViewById(R.id.checkbox_consume_spoiled);
        checkBoxSpoiled.setOnCheckedChangeListener(
                (buttonView, isChecked) -> startAnimatedIcon(R.id.image_consume_spoiled)
        );
        activity.findViewById(R.id.linear_consume_spoiled).setOnClickListener(v -> {
            startAnimatedIcon(R.id.image_consume_spoiled);
            checkBoxSpoiled.setChecked(!checkBoxSpoiled.isChecked());
        });

        // consume
        activity.findViewById(R.id.button_consume_consume).setOnClickListener(
                v -> consumeProduct()
        );
        // open
        activity.findViewById(R.id.button_consume_open).setOnClickListener(
                v -> openProduct()
        );

        // START

        load();

        // UPDATE UI

        activity.updateUI(Constants.UI.CONSUME, TAG);
    }

    private void load() {
        if(activity.isOnline()) {
            download();
        }
    }

    private void refresh() {
        if(activity.isOnline()) {
            download();
        } else {
            swipeRefreshLayout.setRefreshing(false);
            activity.showSnackbar(
                    Snackbar.make(
                            activity.findViewById(R.id.linear_container_main),
                            activity.getString(R.string.msg_no_connection),
                            Snackbar.LENGTH_SHORT
                    ).setActionTextColor(
                            ContextCompat.getColor(activity, R.color.secondary)
                    ).setAction(
                            activity.getString(R.string.action_retry),
                            v1 -> refresh()
                    )
            );
        }

        clearAll();
    }

    private void download() {
        swipeRefreshLayout.setRefreshing(true);
        downloadProductNames();
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
                            activity, android.R.layout.simple_list_item_1, productNames
                    );
                    autoCompleteTextViewProduct.setAdapter(adapterProducts);
                    // download finished
                    swipeRefreshLayout.setRefreshing(false);
                }, error -> {
                    swipeRefreshLayout.setRefreshing(false);
                    activity.showSnackbar(
                            Snackbar.make(
                                    activity.findViewById(R.id.linear_container_main),
                                    activity.getString(R.string.msg_error),
                                    Snackbar.LENGTH_SHORT
                            ).setActionTextColor(
                                    ContextCompat.getColor(activity, R.color.secondary)
                            ).setAction(
                                    activity.getString(R.string.action_retry),
                                    v1 -> download()
                            )
                    );
                }
        );
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == Constants.REQUEST.SCAN && resultCode == Activity.RESULT_OK) {
            if(data != null) {
                loadProductDetailsByBarcode(data.getStringExtra(Constants.EXTRA.SCAN_RESULT));
            }
        }
    }

    private void fillWithProductDetails() {
        clearInputFocus();

        boolean isTareWeightHandlingEnabled = productDetails
                .getProduct()
                .getEnableTareWeightHandling() == 1;

        if(productDetails.getStockAmount() == 0) { // check if stock is empty
            activity.showSnackbar(
                    Snackbar.make(
                            activity.findViewById(R.id.linear_container_main),
                            activity.getString(
                                    R.string.msg_not_in_stock,
                                    productDetails.getProduct().getName()
                            ),
                            Snackbar.LENGTH_LONG
                    )
            );
            clearAll();
            return;
        }

        // PRODUCT
        autoCompleteTextViewProduct.setText(productDetails.getProduct().getName());
        textInputProduct.setErrorEnabled(false);

        // AMOUNT
        textInputAmount.setHint(
                activity.getString(
                        R.string.property_amount_in,
                        productDetails.getQuantityUnitStock().getNamePlural()
                )
        );
        setAmountBounds();
        // leave amount empty if tare weight handling enabled
        editTextAmount.setText(
                isTareWeightHandlingEnabled
                        ? null
                        : NumUtil.trim(
                                sharedPrefs.getFloat(
                                        Constants.PREF.STOCK_DEFAULT_CONSUME_AMOUNT,
                                        1
                                )
                )
        );
        // focus amount field if tare weight handling enabled
        if(isTareWeightHandlingEnabled) {
            editTextAmount.requestFocus();
            activity.showKeyboard(editTextAmount);
        }
        // set icon for tare weight, else for normal amount
        imageViewAmount.setImageResource(
                isTareWeightHandlingEnabled
                        ? R.drawable.ic_round_scale_anim
                        : R.drawable.ic_round_scatter_plot_anim
        );

        // LOCATION
        selectDefaultLocation();
        selectStockEntry(null);
        // load other info for bottomSheet and then for displaying the selected
        loadStockLocations();
        loadStockEntries();

        // SPECIFIC
        textViewSpecific.setText(activity.getString(R.string.subtitle_none));

        // SPOILED
        checkBoxSpoiled.setChecked(false);

        // DETAILS
        refreshProductOverviewIcon();
    }

    private void clearInputFocus() {
        activity.hideKeyboard();
        textInputProduct.clearFocus();
        textInputAmount.clearFocus();
    }

    private void loadProductDetails(int productId) {
        request.get(
                grocyApi.getStockProductDetails(productId),
                response -> {
                    productDetails = gson.fromJson(
                            response,
                            new TypeToken<ProductDetails>(){}.getType()
                    );
                    fillWithProductDetails();
                }, error -> {}
        );
    }

    private void loadStockLocations() {
        request.get(
                grocyApi.getStockLocationsFromProduct(productDetails.getProduct().getId()),
                response -> {
                    stockLocations = gson.fromJson(
                            response,
                            new TypeToken<List<StockLocation>>(){}.getType()
                    );
                    SortUtil.sortStockLocationItemsByName(stockLocations);
                }, error -> {}
        );
    }

    private void loadStockEntries() {
        request.get(
                grocyApi.getStockEntriesFromProduct(productDetails.getProduct().getId()),
                response -> {
                    stockEntries = gson.fromJson(
                            response,
                            new TypeToken<List<StockEntry>>(){}.getType()
                    );
                    stockEntries.add(0, new StockEntry());
                    //SortUtil.sortStockLocationItemsByName(stockLocations);
                }, error -> {}
        );
    }

    private void loadProductDetailsByBarcode(String barcode) {
        swipeRefreshLayout.setRefreshing(true);
        request.get(
                grocyApi.getStockProductByBarcode(barcode),
                response -> {
                    swipeRefreshLayout.setRefreshing(false);
                    productDetails = gson.fromJson(
                            response,
                            new TypeToken<ProductDetails>(){}.getType()
                    );
                    fillWithProductDetails();
                }, error -> {
                    NetworkResponse response = error.networkResponse;
                    if(response != null && response.statusCode == 400) {
                        autoCompleteTextViewProduct.setText(barcode);
                        activity.showBottomSheet(
                                new ConsumeBarcodeBottomSheetDialogFragment(), null
                        );
                    } else {
                        activity.showSnackbar(
                                Snackbar.make(
                                        activity.findViewById(R.id.linear_container_main),
                                        activity.getString(R.string.msg_error),
                                        Snackbar.LENGTH_SHORT
                                )
                        );
                    }
                    swipeRefreshLayout.setRefreshing(false);
                }
        );
    }

    private boolean isFormIncomplete() {
        String input = autoCompleteTextViewProduct.getText().toString().trim();
        if(!productNames.isEmpty() && !productNames.contains(input) && !input.equals("")) {
            activity.showBottomSheet(
                    new ConsumeBarcodeBottomSheetDialogFragment(), null
            );
            return true;
        } else if(productDetails == null || !isAmountValid()) {
            if(productDetails == null) {
                textInputProduct.setError(activity.getString(R.string.error_select_product));
            }
            if(!isAmountValid()) {
                textInputAmount.setError(activity.getString(R.string.error_invalid_amount));
            }
            return true;
        } else {
            return false;
        }
    }

    private void consumeProduct() {
        if(isFormIncomplete()) return;
        boolean isSpoiled = checkBoxSpoiled.isChecked();
        JSONObject body = new JSONObject();
        try {
            body.put("amount", amount);
            body.put("transaction_type", "consume");
            body.put("spoiled", isSpoiled);
        } catch (JSONException e) {
            if(DEBUG) Log.e(TAG, "consumeProduct: " + e);
        }
        request.post(
                grocyApi.consumeProduct(productDetails.getProduct().getId()),
                body,
                response -> {
                    // ADD BARCODES TO PRODUCT
                    editProductBarcodes();

                    // UNDO OPTION
                    String transactionId = null;
                    try {
                        transactionId = response.getString("transaction_id");
                    } catch (JSONException e) {
                        if(DEBUG) Log.e(TAG, "consumeProduct: " + e);
                    }
                    if(DEBUG) Log.i(TAG, "consumeProduct: consumed " + amount);

                    double amountConsumed;
                    if(productDetails.getProduct().getEnableTareWeightHandling() == 0) {
                        amountConsumed = amount;
                    } else {
                        // calculate difference of amount if tare weight handling enabled
                        amountConsumed = productDetails.getStockAmount() - amount
                                + productDetails.getProduct().getTareWeight();
                    }

                    Snackbar snackbar = Snackbar.make(
                            activity.findViewById(R.id.linear_container_main),
                            activity.getString(
                                    isSpoiled
                                            ? R.string.msg_consumed_spoiled
                                            : R.string.msg_consumed,
                                    NumUtil.trim(amountConsumed),
                                    amount == 1
                                            ? productDetails.getQuantityUnitStock().getName()
                                            : productDetails.getQuantityUnitStock().getNamePlural(),
                                    productDetails.getProduct().getName()
                            ), Snackbar.LENGTH_LONG
                    );

                    if(transactionId != null) {
                        String transId = transactionId;
                        snackbar.setActionTextColor(
                                ContextCompat.getColor(activity, R.color.secondary)
                        ).setAction(
                                activity.getString(R.string.action_undo),
                                v -> undoTransaction(transId)
                        );
                    }
                    activity.showSnackbar(snackbar);

                    // CLEAR USER INPUT
                    clearAll();
                },
                error -> {
                    showErrorMessage(error);
                    if(DEBUG) Log.i(TAG, "consumeProduct: " + error);
                }
        );
    }

    private void openProduct() {
        if(isFormIncomplete()) return;
        JSONObject body = new JSONObject();
        try {
            body.put("amount", amount);
        } catch (JSONException e) {
            if(DEBUG) Log.e(TAG, "openProduct: " + e);
        }
        request.post(
                grocyApi.openProduct(productDetails.getProduct().getId()),
                body,
                response -> {
                    // ADD BARCODES TO PRODUCT
                    editProductBarcodes();

                    // UNDO OPTION
                    String transactionId = null;
                    try {
                        transactionId = response.getString("transaction_id");
                    } catch (JSONException e) {
                        if(DEBUG) Log.e(TAG, "openProduct: " + e);
                    }
                    if(DEBUG) Log.i(TAG, "openProduct: opened " + amount);

                    double amountConsumed;
                    if(productDetails.getProduct().getEnableTareWeightHandling() == 0) {
                        amountConsumed = amount;
                    } else {
                        // calculate difference of amount if tare weight handling enabled
                        amountConsumed = productDetails.getStockAmount() - amount
                                + productDetails.getProduct().getTareWeight();
                    }

                    Snackbar snackbar = Snackbar.make(
                            activity.findViewById(R.id.linear_container_main),
                            activity.getString(
                                    R.string.msg_opened,
                                    NumUtil.trim(amountConsumed),
                                    productDetails.getQuantityUnitStock().getName(),
                                    productDetails.getProduct().getName()
                            ), Snackbar.LENGTH_LONG
                    );

                    if(transactionId != null) {
                        final String transId = transactionId;
                        snackbar.setActionTextColor(
                                ContextCompat.getColor(activity, R.color.secondary)
                        ).setAction(
                                activity.getString(R.string.action_undo),
                                v -> undoTransaction(transId)
                        );
                    }
                    activity.showSnackbar(snackbar);

                    // CLEAR USER INPUT
                    clearAll();
                },
                error -> {
                    showErrorMessage(error);
                    if(DEBUG) Log.i(TAG, "openProduct: " + error);
                }
        );
    }

    private void undoTransaction(String transactionId) {
        request.post(
                grocyApi.undoStockTransaction(transactionId),
                success -> {
                    activity.showSnackbar(
                            Snackbar.make(
                                    activity.findViewById(R.id.linear_container_main),
                                    activity.getString(R.string.msg_undone_transaction),
                                    Snackbar.LENGTH_SHORT
                            )
                    );
                    if(DEBUG) Log.i(TAG, "undoTransaction: undone");
                },
                this::showErrorMessage
        );
    }

    private void editProductBarcodes() {
        List<String> barcodes = new ArrayList<>(
                Arrays.asList(
                        productDetails.getProduct().getBarcode().split(",")
                )
        );
        for(int i = 0; i < linearLayoutBarcodesContainer.getChildCount(); i++) {
            InputChip inputChip = (InputChip) linearLayoutBarcodesContainer.getChildAt(i);
            if(!barcodes.contains(inputChip.getText())) {
                barcodes.add(inputChip.getText());
            }
        }
        if(DEBUG) Log.i(TAG, "editProductBarcodes: " + barcodes);
        JSONObject body = new JSONObject();
        try {
            body.put("barcode", TextUtils.join(",", barcodes));
        } catch (JSONException e) {
            if(DEBUG) Log.e(TAG, "editProductBarcodes: " + e);
        }
        request.put(
                grocyApi.getObject(GrocyApi.ENTITY.PRODUCTS, productDetails.getProduct().getId()),
                body,
                response -> { },
                error -> {
                    if(DEBUG) Log.i(TAG, "editProductBarcodes: " + error);
                }
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

    private void selectDefaultLocation() {
        if(productDetails != null) {
            selectedLocationId = productDetails.getLocation().getId();
            textViewLocation.setText(productDetails.getLocation().getName());
        }
    }

    public void selectLocation(int selectedId) {
        this.selectedLocationId = selectedId;
        String location = null;
        if(stockLocations.isEmpty()) {
            location = productDetails.getLocation().getName();
        } else {
            StockLocation stockLocation = getStockLocation(selectedId);
            if(stockLocation != null) {
                location = stockLocation.getLocationName();
            }
        }
        textViewLocation.setText(location);
    }

    public void selectStockEntry(String selectedId) {
        // stockId is a String
        this.selectedStockEntryId = selectedId;
        textViewSpecific.setText(
                activity.getString(
                        selectedId == null
                                ? R.string.subtitle_none
                                : R.string.subtitle_selected
                )
        );
        setAmountBounds();
    }

    private void setAmountBounds() {
        if(selectedStockEntryId == null) {
            // called from fillWithProductDetails
            maxAmount = productDetails.getStockAmount();
        } else {
            StockEntry stockEntry = getStockEntry(selectedStockEntryId);
            if(stockEntry != null) {
                maxAmount = stockEntry.getAmount();
            }
        }
        if(productDetails.getProduct().getEnableTareWeightHandling() == 0) {
            minAmount = 1;
            if(selectedStockEntryId == null) {
                // called from fillWithProductDetails
                maxAmount = productDetails.getStockAmount();
            } else {
                StockEntry stockEntry = getStockEntry(selectedStockEntryId);
                if(stockEntry != null) {
                    maxAmount = stockEntry.getAmount();
                }
            }
        } else {
            minAmount = productDetails.getProduct().getTareWeight();
            maxAmount = productDetails.getProduct().getTareWeight()
                    + productDetails.getStockAmount();
        }
    }

    private boolean isAmountValid() {
        if(!editTextAmount.getText().toString().equals("")) {
            if(amount >= minAmount && amount <= maxAmount) {
                textInputAmount.setErrorEnabled(false);
                return true;
            } else {
                if(productDetails != null) {
                    textInputAmount.setError(
                            activity.getString(
                                    R.string.error_bounds,
                                    NumUtil.trim(minAmount),
                                    NumUtil.trim(maxAmount)
                            )
                    );
                }
                return false;
            }
        } else {
            textInputAmount.setErrorEnabled(false);
            return false;
        }
    }

    private StockLocation getStockLocation(int locationId) {
        for(StockLocation stockLocation : stockLocations) {
            if(stockLocation.getLocationId() == locationId) {
                return stockLocation;
            }
        } return null;
    }

    private StockEntry getStockEntry(String stockId) {
        for(StockEntry stockEntry : stockEntries) {
            if(stockEntry.getStockId() != null && stockEntry.getStockId().equals(stockId)) {
                return stockEntry;
            }
        } return null;
    }

    public void refreshProductOverviewIcon() {
        MenuItem menuItem = activity.getBottomMenu().findItem(R.id.action_product_overview);
        if(menuItem != null) {
            menuItem.setEnabled(productDetails != null);

            Drawable icon = menuItem.getIcon();
            ValueAnimator alphaAnimator = ValueAnimator.ofInt(
                    icon.getAlpha(), (productDetails != null) ? 255 : 100
            );
            alphaAnimator.addUpdateListener(
                    animation -> icon.setAlpha((int) (animation.getAnimatedValue()))
            );
            alphaAnimator.setDuration(200).start();

            menuItem.setOnMenuItemClickListener(item -> {
                ((Animatable) icon).start();
                if(productDetails != null) {
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(Constants.ARGUMENT.PRODUCT_DETAILS, productDetails);
                    bundle.putBoolean(Constants.ARGUMENT.SET_UP_WITH_PRODUCT_DETAILS, true);
                    bundle.putBoolean(Constants.ARGUMENT.SHOW_ACTIONS, false);
                    activity.showBottomSheet(
                            new ProductOverviewBottomSheetDialogFragment(),
                            bundle
                    );
                }
                return true;
            });
        }
    }

    public void addInputAsBarcode() {
        String input = autoCompleteTextViewProduct.getText().toString().trim();
        if(input.equals("")) return;
        for(int i = 0; i < linearLayoutBarcodesContainer.getChildCount(); i++) {
            InputChip inputChip = (InputChip) linearLayoutBarcodesContainer.getChildAt(i);
            if(inputChip.getText().equals(input)) {
                activity.showSnackbar(
                        Snackbar.make(
                                activity.findViewById(R.id.linear_container_main),
                                activity.getString(R.string.msg_barcode_duplicate),
                                Snackbar.LENGTH_SHORT
                        )
                );
                autoCompleteTextViewProduct.setText(null);
                autoCompleteTextViewProduct.requestFocus();
                return;
            }
        }
        InputChip inputChipBarcode = new InputChip(
                activity, input, R.drawable.ic_round_barcode_scan, true
        );
        inputChipBarcode.setPadding(0, 0, 0, 8);
        linearLayoutBarcodesContainer.addView(inputChipBarcode);
        autoCompleteTextViewProduct.setText(null);
        autoCompleteTextViewProduct.requestFocus();
    }

    public void clearAll() {
        textInputProduct.setErrorEnabled(false);
        autoCompleteTextViewProduct.setText(null);
        textInputAmount.setErrorEnabled(false);
        textInputAmount.setHint(activity.getString(R.string.property_amount));
        editTextAmount.setText(null);
        imageViewAmount.setImageResource(R.drawable.ic_round_scatter_plot_anim);
        textViewLocation.setText(activity.getString(R.string.subtitle_none));
        textViewSpecific.setText(activity.getString(R.string.subtitle_none));
        if(checkBoxSpoiled.isChecked()) checkBoxSpoiled.setChecked(false);
        clearInputFocus();
        for(int i = 0; i < linearLayoutBarcodesContainer.getChildCount(); i++) {
            ((InputChip) linearLayoutBarcodesContainer.getChildAt(i)).close();
        }
        productDetails = null;
    }

    private void showErrorMessage(VolleyError error) {
        activity.showSnackbar(
                Snackbar.make(
                        activity.findViewById(R.id.linear_container_main),
                        activity.getString(R.string.msg_error),
                        Snackbar.LENGTH_SHORT
                )
        );
    }

    private void startAnimatedIcon(@IdRes int viewId) {
        startAnimatedIcon(activity.findViewById(viewId));
    }

    private void startAnimatedIcon(View view) {
        try {
            ((Animatable) ((ImageView) view).getDrawable()).start();
        } catch (ClassCastException cla) {
            Log.e(TAG, "startAnimatedIcon(Drawable) requires AVD!");
        }
    }
}
