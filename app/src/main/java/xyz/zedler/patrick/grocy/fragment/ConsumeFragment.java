package xyz.zedler.patrick.grocy.fragment;

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

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.android.volley.NetworkResponse;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.activity.ScanBatchActivity;
import xyz.zedler.patrick.grocy.activity.ScanInputActivity;
import xyz.zedler.patrick.grocy.adapter.MatchArrayAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.FragmentConsumeBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputBarcodeBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StockEntriesBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StockLocationsBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.view.InputChip;

public class ConsumeFragment extends Fragment {

    private final static String TAG = Constants.UI.CONSUME;

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private Gson gson;
    private GrocyApi grocyApi;
    private DownloadHelper dlHelper;
    private ArrayAdapter<String> adapterProducts;
    private Bundle startupBundle;
    private FragmentConsumeBinding binding;

    private ArrayList<Product> products;
    private ArrayList<StockLocation> stockLocations;
    private ArrayList<StockEntry> stockEntries;
    private ArrayList<String> productNames;

    private ProductDetails productDetails;

    private boolean debug;
    private int selectedLocationId;
    private String selectedStockEntryId;
    private double amount;
    private double maxAmount;
    private double minAmount;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentConsumeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        dlHelper.destroy();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if(isHidden()) return;

        activity = (MainActivity) getActivity();
        assert activity != null;

        if(getArguments() != null) startupBundle = getArguments();

        // GET PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        // WEB REQUESTS

        dlHelper = new DownloadHelper(activity, TAG);
        grocyApi = activity.getGrocy();
        gson = new Gson();

        // INITIALIZE VARIABLES

        products = new ArrayList<>();
        stockLocations = new ArrayList<>();
        stockEntries = new ArrayList<>();
        productNames = new ArrayList<>();

        productDetails = null;
        selectedLocationId = -1;
        selectedStockEntryId = null;
        amount = 0;
        maxAmount = 0;
        minAmount = 0;

        // INITIALIZE VIEWS

        binding.frameConsumeBack.setOnClickListener(v -> activity.onBackPressed());

        // swipe refresh

        binding.swipeConsume.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        binding.swipeConsume.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        binding.swipeConsume.setOnRefreshListener(this::refresh);

        // product

        binding.textInputConsumeProduct.setErrorIconDrawable(null);
        binding.textInputConsumeProduct.setEndIconOnClickListener(v -> startActivityForResult(
                new Intent(activity, ScanInputActivity.class),
                Constants.REQUEST.SCAN
        ));
        binding.autoCompleteConsumeProduct.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) {
                IconUtil.start(activity, R.id.image_consume_product);
                // try again to download products
                if(productNames.isEmpty()) downloadProductNames();
            } else {
                String input = binding.autoCompleteConsumeProduct.getText().toString();
                if(!productNames.isEmpty() && !productNames.contains(input)) {
                    binding.textInputConsumeProduct.setError(
                            activity.getString(R.string.error_invalid_product)
                    );
                }
            }
        });
        binding.autoCompleteConsumeProduct.setOnItemClickListener(
                (parent, itemView, position, id) -> loadProductDetails(
                        getProductFromName(
                                String.valueOf(parent.getItemAtPosition(position))
                        ).getId()
                )
        );
        binding.autoCompleteConsumeProduct.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        String input = binding.autoCompleteConsumeProduct.getText().toString();
                        if(!productNames.isEmpty() && productNames.contains(input)) {
                            Product product = getProductFromName(input);
                            if(product != null) loadProductDetails(product.getId());
                        }
                        binding.editTextConsumeAmount.requestFocus();
                        return true;
                    } return false;
                });

        // amount

        binding.editTextConsumeAmount.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                if(productDetails == null) return;
                amount = NumUtil.stringToDouble(s.toString());
                isAmountValid();
            }
        });
        binding.editTextConsumeAmount.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) {
                IconUtil.start(binding.imageConsumeAmount);
                // editTextAmount.selectAll();
            }
        });
        binding.editTextConsumeAmount.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        clearInputFocus();
                        return true;
                    } return false;
                });

        binding.buttonConsumeAmountMore.setOnClickListener(v -> {
            IconUtil.start(activity, R.id.image_consume_amount);
            if(getAmount().isEmpty()) {
                binding.editTextConsumeAmount.setText(String.valueOf(1));
            } else {
                double amountNew = Double.parseDouble(getAmount()) + 1;
                if(amountNew <= maxAmount) {
                    binding.editTextConsumeAmount.setText(NumUtil.trim(amountNew));
                }
            }
        });

        binding.buttonConsumeAmountLess.setOnClickListener(v -> {
            if(!getAmount().isEmpty()) {
                IconUtil.start(activity, R.id.image_consume_amount);
                double amountNew = Double.parseDouble(getAmount()) - 1;
                if(amountNew >= minAmount) {
                    binding.editTextConsumeAmount.setText(NumUtil.trim(amountNew));
                }
            }
        });

        // location

        binding.linearConsumeLocation.setOnClickListener(v -> {
            IconUtil.start(activity, R.id.image_consume_location);
            if(productDetails != null && !stockLocations.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList(Constants.ARGUMENT.STOCK_LOCATIONS, stockLocations);
                bundle.putParcelable(Constants.ARGUMENT.PRODUCT_DETAILS, productDetails);
                bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedLocationId);
                activity.showBottomSheet(new StockLocationsBottomSheetDialogFragment(), bundle);
            } else if(productDetails != null) {
                activity.showMessage(
                        Snackbar.make(
                                activity.binding.frameMainContainer,
                                activity.getString(
                                        R.string.msg_no_stock_locations,
                                        productDetails.getProduct().getName()
                                ),
                                Snackbar.LENGTH_SHORT
                        )
                );
            } else {
                // no product selected
                binding.textInputConsumeProduct.setError(
                        activity.getString(R.string.error_select_product)
                );
            }
        });

        // specific

        binding.linearConsumeSpecific.setOnClickListener(v -> {
            IconUtil.start(activity, R.id.image_consume_specific);
            if(productDetails != null && !stockEntries.isEmpty()) {
                ArrayList<StockEntry> filteredStockEntries = new ArrayList<>();
                for(StockEntry stockEntry : stockEntries) {
                    if(stockEntry.getLocationId() == selectedLocationId) {
                        filteredStockEntries.add(stockEntry);
                    }
                }
                if(filteredStockEntries.isEmpty()) {
                    activity.showMessage(
                            Snackbar.make(
                                    activity.binding.frameMainContainer,
                                    activity.getString(
                                            R.string.msg_no_stock_entries,
                                            productDetails.getProduct().getName()
                                    ),
                                    Snackbar.LENGTH_SHORT
                            )
                    );
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putParcelableArrayList(
                            Constants.ARGUMENT.STOCK_ENTRIES,
                            filteredStockEntries
                    );
                    bundle.putString(Constants.ARGUMENT.SELECTED_ID, selectedStockEntryId);
                    activity.showBottomSheet(new StockEntriesBottomSheetDialogFragment(), bundle);
                }
            } else {
                // no product selected
                binding.textInputConsumeProduct.setError(
                        activity.getString(R.string.error_select_product)
                );
            }
        });

        // spoiled

        binding.checkboxConsumeSpoiled.setOnCheckedChangeListener(
                (buttonView, isChecked) -> IconUtil.start(activity, R.id.image_consume_spoiled)
        );
        binding.linearConsumeSpoiled.setOnClickListener(v -> {
            IconUtil.start(activity, R.id.image_consume_spoiled);
            binding.checkboxConsumeSpoiled.setChecked(!binding.checkboxConsumeSpoiled.isChecked());
        });

        hideDisabledFeatures();

        // START

        if(savedInstanceState == null) {
            refresh();
        } else {
            restoreSavedInstanceState(savedInstanceState);
        }

        // UPDATE UI

        activity.updateUI(Constants.UI.CONSUME, savedInstanceState == null, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(isHidden()) return;

        outState.putParcelableArrayList("products", products);
        outState.putParcelableArrayList("stockLocations", stockLocations);
        outState.putParcelableArrayList("stockEntries", stockEntries);

        outState.putParcelable("productDetails", productDetails);

        outState.putInt("selectedLocationId", selectedLocationId);
        outState.putString("selectedStockEntryId", selectedStockEntryId);

        outState.putDouble("amount", amount);
        outState.putDouble("maxAmount", maxAmount);
        outState.putDouble("minAmount", minAmount);
    }

    private void restoreSavedInstanceState(@NonNull Bundle savedInstanceState) {
        if(isHidden()) return;

        products = savedInstanceState.getParcelableArrayList("products");
        stockLocations = savedInstanceState.getParcelableArrayList("stockLocations");
        stockEntries = savedInstanceState.getParcelableArrayList("stockEntries");

        productNames = getProductNames();
        adapterProducts = new MatchArrayAdapter(activity, productNames);
        binding.autoCompleteConsumeProduct.setAdapter(adapterProducts);

        productDetails = savedInstanceState.getParcelable("productDetails");

        selectedLocationId = savedInstanceState.getInt("selectedLocationId");
        selectLocation(selectedLocationId);
        selectedStockEntryId = savedInstanceState.getString("selectedStockEntryId");
        selectStockEntry(selectedStockEntryId);

        amount = savedInstanceState.getDouble("amount");
        maxAmount = savedInstanceState.getDouble("maxAmount");
        minAmount = savedInstanceState.getDouble("minAmount");

        binding.swipeConsume.setRefreshing(false);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if(!hidden && getView() != null) onViewCreated(getView(), null);
    }

    public void giveBundle(Bundle bundle) {
        startupBundle = bundle;
    }

    private void refresh() {
        if(activity.isOnline()) {
            download();
        } else {
            binding.swipeConsume.setRefreshing(false);
            activity.showMessage(
                    Snackbar.make(
                            activity.binding.frameMainContainer,
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
        binding.swipeConsume.setRefreshing(true);
        downloadProductNames();
    }

    private void downloadProductNames() {
        dlHelper.get(
                grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS),
                response -> {
                    products = gson.fromJson(
                            response,
                            new TypeToken<List<Product>>(){}.getType()
                    );
                    productNames = getProductNames();
                    adapterProducts = new MatchArrayAdapter(activity, productNames);
                    binding.autoCompleteConsumeProduct.setAdapter(adapterProducts);

                    // fill with product from bundle
                    String action = null;
                    if(startupBundle != null) {
                        action = startupBundle.getString(Constants.ARGUMENT.TYPE);
                    }
                    if(action != null) {
                        if(action.equals(Constants.ACTION.CONSUME_THEN_STOCK)
                                || action.equals(Constants.ACTION.EDIT_THEN_CONSUME)
                        ) {
                            Product product = getProductFromName(
                                    startupBundle.getString(Constants.ARGUMENT.PRODUCT_NAME)
                            );
                            if(product != null) loadProductDetails(product.getId());
                        }
                    }
                    binding.swipeConsume.setRefreshing(false);
                }, error -> {
                    binding.swipeConsume.setRefreshing(false);
                    activity.showMessage(
                            Snackbar.make(
                                    activity.binding.frameMainContainer,
                                    activity.getString(R.string.error_undefined),
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
            activity.showMessage(
                    Snackbar.make(
                            activity.binding.frameMainContainer,
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
        binding.autoCompleteConsumeProduct.setText(productDetails.getProduct().getName());
        binding.autoCompleteConsumeProduct.dismissDropDown(); // necessary for lower Android versions, tested on 5.1
        binding.textInputConsumeProduct.setErrorEnabled(false);

        // AMOUNT
        binding.textInputConsumeAmount.setHint(
                activity.getString(
                        R.string.property_amount_in,
                        productDetails.getQuantityUnitStock().getNamePlural()
                )
        );
        setAmountBounds();

        // leave amount empty if tare weight handling enabled
        if(!isTareWeightHandlingEnabled) {
            String defaultAmount = sharedPrefs.getString(
                    Constants.PREF.STOCK_DEFAULT_CONSUME_AMOUNT, null
            );
            if(defaultAmount == null) defaultAmount = String.valueOf(1);
            if(defaultAmount.isEmpty()) {
                binding.editTextConsumeAmount.setText(null);
            } else if(Double.parseDouble(defaultAmount) > productDetails.getStockAmount()) {
                binding.editTextConsumeAmount.setText(
                        NumUtil.trim(productDetails.getStockAmount())
                );
            } else {
                binding.editTextConsumeAmount.setText(
                        NumUtil.trim(Double.parseDouble(defaultAmount))
                );
            }
        } else {
            binding.editTextConsumeAmount.setText(null);
        }

        if(getAmount().isEmpty()) {
            binding.editTextConsumeAmount.requestFocus();
            activity.showKeyboard(binding.editTextConsumeAmount);
        }

        // disable open action if needed
        setOpenEnabled(!isTareWeightHandlingEnabled);

        // set icon for tare weight, else for normal amount
        binding.imageConsumeAmount.setImageResource(
                isTareWeightHandlingEnabled
                        ? R.drawable.ic_round_scale_anim
                        : R.drawable.ic_round_scatter_plot_anim
        );

        // LOCATION
        if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
            selectDefaultLocation();
            loadStockLocations();
        }

        // SPECIFIC
        selectStockEntry(null);
        loadStockEntries();

        // SPOILED
        binding.checkboxConsumeSpoiled.setChecked(false);

        // mark fields with invalid or missing content as invalid
        isFormIncomplete();

        // update actions (e.g. hide open action if necessary)
        setUpBottomMenu();
    }

    private void clearInputFocus() {
        activity.hideKeyboard();
        binding.textInputConsumeProduct.clearFocus();
        binding.textInputConsumeAmount.clearFocus();
    }

    private void loadProductDetails(int productId) {
        dlHelper.get(
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
        dlHelper.get(
                grocyApi.getStockLocationsFromProduct(productDetails.getProduct().getId()),
                response -> {
                    stockLocations = gson.fromJson(
                            response,
                            new TypeToken<ArrayList<StockLocation>>(){}.getType()
                    );
                    SortUtil.sortStockLocationItemsByName(stockLocations);
                }, error -> {}
        );
    }

    private void loadStockEntries() {
        dlHelper.get(
                grocyApi.getStockEntriesFromProduct(productDetails.getProduct().getId()),
                response -> {
                    stockEntries = gson.fromJson(
                            response,
                            new TypeToken<ArrayList<StockEntry>>(){}.getType()
                    );
                    stockEntries.add(0, new StockEntry());
                }, error -> {}
        );
    }

    private void loadProductDetailsByBarcode(String barcode) {
        binding.swipeConsume.setRefreshing(true);
        dlHelper.get(
                grocyApi.getStockProductByBarcode(barcode),
                response -> {
                    binding.swipeConsume.setRefreshing(false);
                    productDetails = gson.fromJson(
                            response,
                            new TypeToken<ProductDetails>(){}.getType()
                    );
                    fillWithProductDetails();
                }, error -> {
                    NetworkResponse response = error.networkResponse;
                    if(response != null && response.statusCode == 400) {
                        binding.autoCompleteConsumeProduct.setText(barcode);
                        activity.showBottomSheet(
                                new InputBarcodeBottomSheetDialogFragment(), null
                        );
                    } else {
                        activity.showMessage(
                                Snackbar.make(
                                        activity.binding.frameMainContainer,
                                        activity.getString(R.string.error_undefined),
                                        Snackbar.LENGTH_SHORT
                                )
                        );
                    }
                    binding.swipeConsume.setRefreshing(false);
                }
        );
    }

    private boolean isFormIncomplete() {
        String input = binding.autoCompleteConsumeProduct.getText().toString();
        if(!productNames.isEmpty() && !productNames.contains(input) && !input.isEmpty()) {
            binding.textInputConsumeProduct.setError(
                    activity.getString(R.string.error_invalid_product)
            );
            return true;
        } else if(productDetails == null || !isAmountValid()) {
            if(productDetails == null) {
                binding.textInputConsumeProduct.setError(
                        activity.getString(R.string.error_select_product)
                );
            }
            isAmountValid();
            return true;
        } else {
            return false;
        }
    }

    public void consumeProduct() {
        if(isFormIncomplete()) return;
        boolean isSpoiled = binding.checkboxConsumeSpoiled.isChecked();
        JSONObject body = new JSONObject();
        try {
            body.put("amount", amount);
            body.put("transaction_type", "consume");
            body.put("spoiled", isSpoiled);
            if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)
                    && selectedLocationId != -1
            ) {
                body.put("location_id", selectedLocationId);
            }
            if(selectedStockEntryId != null && !selectedStockEntryId.isEmpty()) {
                body.put("stock_entry_id", selectedStockEntryId);
            }
        } catch (JSONException e) {
            if(debug) Log.e(TAG, "consumeProduct: " + e);
        }
        dlHelper.post(
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
                        if(debug) Log.e(TAG, "consumeProduct: " + e);
                    }
                    if(debug) Log.i(TAG, "consumeProduct: consumed " + amount);

                    double amountConsumed;
                    if(productDetails.getProduct().getEnableTareWeightHandling() == 0) {
                        amountConsumed = amount;
                    } else {
                        // calculate difference of amount if tare weight handling enabled
                        amountConsumed = productDetails.getStockAmount() - amount
                                + productDetails.getProduct().getTareWeight();
                    }

                    Snackbar snackbar = Snackbar.make(
                            activity.binding.frameMainContainer,
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
                    activity.showMessage(snackbar);

                    // CLEAR USER INPUT
                    clearAll();
                },
                error -> {
                    showMessage(activity.getString(R.string.error_undefined));
                    if(debug) Log.e(TAG, "consumeProduct: " + error);
                }
        );
    }

    private void openProduct() {
        if(isFormIncomplete()) return;
        JSONObject body = new JSONObject();
        try {
            body.put("amount", amount);
            if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)
                    && selectedLocationId != -1
            ) {
                body.put("location_id", selectedLocationId);
            }
            if(selectedStockEntryId != null && !selectedStockEntryId.isEmpty()) {
                body.put("stock_entry_id", selectedStockEntryId);
            }
        } catch (JSONException e) {
            if(debug) Log.e(TAG, "openProduct: " + e);
        }
        dlHelper.post(
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
                        if(debug) Log.e(TAG, "openProduct: " + e);
                    }
                    if(debug) Log.i(TAG, "openProduct: opened " + amount);

                    double amountConsumed;
                    if(productDetails.getProduct().getEnableTareWeightHandling() == 0) {
                        amountConsumed = amount;
                    } else {
                        // calculate difference of amount if tare weight handling enabled
                        amountConsumed = productDetails.getStockAmount() - amount
                                + productDetails.getProduct().getTareWeight();
                    }

                    Snackbar snackbar = Snackbar.make(
                            activity.binding.frameMainContainer,
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
                    activity.showMessage(snackbar);

                    // CLEAR USER INPUT
                    clearAll();
                },
                error -> {
                    showMessage(activity.getString(R.string.error_undefined));
                    if(debug) Log.e(TAG, "openProduct: " + error);
                }
        );
    }

    private void undoTransaction(String transactionId) {
        if(binding == null || activity != null && activity.isDestroyed()) return;
        dlHelper.post(
                grocyApi.undoStockTransaction(transactionId),
                success -> {
                    activity.showMessage(
                            Snackbar.make(
                                    activity.binding.frameMainContainer,
                                    activity.getString(R.string.msg_undone_transaction),
                                    Snackbar.LENGTH_SHORT
                            )
                    );
                    if(debug) Log.i(TAG, "undoTransaction: undone");
                },
                error -> {
                    showMessage(activity.getString(R.string.error_undefined));
                    if(debug) Log.e(TAG, "undoTransaction: " + error);
                }
        );
    }

    private void editProductBarcodes() {
        if(binding.linearConsumeBarcodeContainer.getChildCount() == 0) return;

        String barcodesString = productDetails.getProduct().getBarcode();
        ArrayList<String> barcodes;
        if(barcodesString != null && !barcodesString.isEmpty()) {
            barcodes = new ArrayList<>(Arrays.asList(barcodesString.split(",")));
        } else {
            barcodes = new ArrayList<>();
        }

        for(int i = 0; i < binding.linearConsumeBarcodeContainer.getChildCount(); i++) {
            InputChip inputChip = (InputChip) binding.linearConsumeBarcodeContainer.getChildAt(i);
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
                grocyApi.getObject(GrocyApi.ENTITY.PRODUCTS, productDetails.getProduct().getId()),
                body,
                response -> { },
                error -> {
                    if(debug) Log.i(TAG, "editProductBarcodes: " + error);
                }
        );
    }

    private Product getProductFromName(String name) {
        if(name != null) {
            for(Product product : products) {
                if(product.getName().equals(name)) {
                    return product;
                }
            }
        }
        return null;
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

    private void selectDefaultLocation() {
        if(productDetails != null) {
            if(productDetails.getLocation() != null) {
                selectedLocationId = productDetails.getLocation().getId();
                binding.textConsumeLocation.setText(productDetails.getLocation().getName());
            } else {
                selectedLocationId = -1;
                binding.textConsumeLocation.setText(activity.getString(R.string.subtitle_none_selected));
            }
        }
    }

    public void selectLocation(int selectedId) {
        this.selectedLocationId = selectedId;
        String location = activity.getString(R.string.subtitle_none_selected);
        if(stockLocations.isEmpty() && productDetails != null) {
            if(productDetails.getLocation() != null) {
                location = productDetails.getLocation().getName();
            }
        } else {
            StockLocation stockLocation = getStockLocation(selectedId);
            if(stockLocation != null) {
                location = stockLocation.getLocationName();
            }
        }
        binding.textConsumeLocation.setText(location);
    }

    public void selectStockEntry(String selectedId) {
        // stockId is a String
        this.selectedStockEntryId = selectedId;
        binding.textConsumeSpecific.setText(
                activity.getString(
                        selectedId == null
                                ? R.string.subtitle_none_selected
                                : R.string.subtitle_selected
                )
        );
        if(productDetails != null) setAmountBounds();
    }

    private void setAmountBounds() {
        if(productDetails.getProduct().getEnableTareWeightHandling() == 0) {
            if(productDetails.getProduct().getAllowPartialUnitsInStock() == 0) {
                minAmount = 1;
            } else {
                minAmount = 0.01; // this is the same behavior as the grocy web server
            }
            if(selectedStockEntryId == null) {
                // called from fillWithProductDetails
                maxAmount = productDetails.getStockAmount();
            } else {
                StockEntry stockEntry = getStockEntry(selectedStockEntryId);
                if(stockEntry != null) {
                    maxAmount = stockEntry.getAmount();
                } else {
                    maxAmount = 0;
                }
            }
        } else {
            minAmount = productDetails.getProduct().getTareWeight();
            maxAmount = productDetails.getProduct().getTareWeight()
                    + productDetails.getStockAmount();
        }
    }

    private boolean isAmountValid() {
        if(!getAmount().isEmpty()) {
            if(amount >= minAmount && amount <= maxAmount) {
                if(productDetails != null
                        && amount % 1 != 0 // partial amount, has to be allowed in product master
                        && productDetails.getProduct().getAllowPartialUnitsInStock() == 0
                ) {
                    binding.textInputConsumeAmount.setError(
                            activity.getString(R.string.error_invalid_amount)
                    );
                    return false;
                } else {
                    binding.textInputConsumeAmount.setErrorEnabled(false);
                    return true;
                }
            } else {
                if(productDetails != null) {
                    binding.textInputConsumeAmount.setError(
                            activity.getString(
                                    R.string.error_bounds_min_max,
                                    NumUtil.trim(minAmount),
                                    NumUtil.trim(maxAmount)
                            )
                    );
                }
                return false;
            }
        } else {
            binding.textInputConsumeAmount.setError(
                    activity.getString(
                            R.string.error_bounds_min_max,
                            NumUtil.trim(minAmount),
                            NumUtil.trim(maxAmount)
                    )
            );
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

    private void hideDisabledFeatures() {
        if(!isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
            binding.linearConsumeLocation.setVisibility(View.GONE);
        }
    }

    public void setUpBottomMenu() {
        MenuItem menuItemBatch, menuItemDetails, menuItemOpen;
        menuItemBatch = activity.getBottomMenu().findItem(R.id.action_batch_mode);
        if(menuItemBatch != null) menuItemBatch.setOnMenuItemClickListener(item -> {
            Intent intent = new Intent(activity, ScanBatchActivity.class);
            intent.putExtra(Constants.ARGUMENT.TYPE, Constants.ACTION.CONSUME);
            activity.startActivityForResult(intent, Constants.REQUEST.SCAN_BATCH);
            return true;
        });
        menuItemDetails = activity.getBottomMenu().findItem(R.id.action_product_overview);
        if(menuItemDetails != null) menuItemDetails.setOnMenuItemClickListener(item -> {
            IconUtil.start(menuItemDetails);
            if(productDetails != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(Constants.ARGUMENT.PRODUCT_DETAILS, productDetails);
                activity.showBottomSheet(
                        new ProductOverviewBottomSheetDialogFragment(),
                        bundle
                );
            } else {
                binding.textInputConsumeProduct.setError(
                        activity.getString(R.string.error_select_product)
                );
            }
            return true;
        });
        menuItemOpen = activity.getBottomMenu().findItem(R.id.action_open);
        if(menuItemOpen != null && isFeatureEnabled(Constants.PREF.FEATURE_STOCK_OPENED_TRACKING)) {
            Drawable icon = menuItemOpen.getIcon();
            icon.setAlpha(0);
            menuItemOpen.setVisible(true);
            menuItemOpen.setOnMenuItemClickListener(item -> {
                openProduct();
                return true;
            });
            ValueAnimator alphaAnimator = ValueAnimator.ofInt(icon.getAlpha(), 255);
            alphaAnimator.addUpdateListener(
                    animation -> icon.setAlpha((int) (animation.getAnimatedValue()))
            );
            alphaAnimator.setDuration(200).start();
        }
    }

    private void setOpenEnabled(boolean enabled) {
        MenuItem menuItemOpen = activity.getBottomMenu().findItem(R.id.action_open);
        if(menuItemOpen == null) return;

        menuItemOpen.setEnabled(enabled);

        Drawable icon = menuItemOpen.getIcon();
        ValueAnimator alphaAnimator = ValueAnimator.ofInt(
                icon.getAlpha(), enabled ? 255 : 100
        );
        alphaAnimator.addUpdateListener(
                animation -> icon.setAlpha((int) (animation.getAnimatedValue()))
        );
        alphaAnimator.setDuration(200).start();
    }

    public void addInputAsBarcode() {
        String input = binding.autoCompleteConsumeProduct.getText().toString().trim();
        if(input.isEmpty()) return;
        for(int i = 0; i < binding.linearConsumeBarcodeContainer.getChildCount(); i++) {
            InputChip inputChip = (InputChip) binding.linearConsumeBarcodeContainer.getChildAt(i);
            if(inputChip.getText().equals(input)) {
                activity.showMessage(
                        Snackbar.make(
                                activity.binding.frameMainContainer,
                                activity.getString(R.string.msg_barcode_duplicate),
                                Snackbar.LENGTH_SHORT
                        )
                );
                binding.autoCompleteConsumeProduct.setText(null);
                binding.autoCompleteConsumeProduct.requestFocus();
                return;
            }
        }
        InputChip inputChipBarcode = new InputChip(
                activity, input, R.drawable.ic_round_barcode, true
        );
        inputChipBarcode.setPadding(0, 0, 0, 8);
        binding.linearConsumeBarcodeContainer.addView(inputChipBarcode);
        binding.autoCompleteConsumeProduct.setText(null);
        binding.autoCompleteConsumeProduct.requestFocus();
    }

    public void clearAll() {
        productDetails = null;
        binding.textInputConsumeProduct.setErrorEnabled(false);
        binding.autoCompleteConsumeProduct.setText(null);
        binding.textInputConsumeAmount.setErrorEnabled(false);
        binding.textInputConsumeAmount.setHint(activity.getString(R.string.property_amount));
        binding.editTextConsumeAmount.setText(null);
        binding.imageConsumeAmount.setImageResource(R.drawable.ic_round_scatter_plot_anim);
        binding.textConsumeLocation.setText(activity.getString(R.string.subtitle_none_selected));
        binding.textConsumeSpecific.setText(activity.getString(R.string.subtitle_none_selected));
        if(binding.checkboxConsumeSpoiled.isChecked()) {
            binding.checkboxConsumeSpoiled.setChecked(false);
        }
        clearInputFocus();
        for(int i = 0; i < binding.linearConsumeBarcodeContainer.getChildCount(); i++) {
            ((InputChip) binding.linearConsumeBarcodeContainer.getChildAt(i)).close();
        }
    }

    private void showMessage(String text) {
        activity.showMessage(
                Snackbar.make(activity.binding.frameMainContainer, text, Snackbar.LENGTH_SHORT)
        );
    }

    private String getAmount() {
        Editable amount = binding.editTextConsumeAmount.getText();
        if(amount == null) return "";
        return amount.toString();
    }

    private boolean isFeatureEnabled(String pref) {
        if(pref == null) return true;
        return sharedPrefs.getBoolean(pref, true);
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
