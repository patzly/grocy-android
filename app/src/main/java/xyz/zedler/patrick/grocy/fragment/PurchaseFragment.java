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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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

import androidx.annotation.ColorRes;
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
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.ScanBatchActivity;
import xyz.zedler.patrick.grocy.ScanInputActivity;
import xyz.zedler.patrick.grocy.adapter.MatchArrayAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.BBDateBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputBarcodeBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputNameBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LocationsBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StoresBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.view.InputChip;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class PurchaseFragment extends Fragment {

    private final static String TAG = Constants.UI.CONSUME;
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private WebRequest request;
    private DateUtil dateUtil;
    private ArrayAdapter<String> adapterProducts;
    private ProductDetails productDetails;
    private Bundle startupBundle;

    private ArrayList<Product> products = new ArrayList<>();
    private ArrayList<Location> locations = new ArrayList<>();
    private ArrayList<Store> stores = new ArrayList<>();
    private ArrayList<String> productNames = new ArrayList<>();

    private SwipeRefreshLayout swipeRefreshLayout;
    private MaterialAutoCompleteTextView autoCompleteTextViewProduct;
    private LinearLayout linearLayoutBarcodesContainer, linearLayoutTotalPrice;
    private TextInputLayout textInputProduct, textInputAmount, textInputPrice;
    private EditText editTextAmount, editTextPrice;
    private TextView textViewLocation, textViewStore, textViewBestBeforeDate;
    private TextView textViewLocationLabel, textViewBbdLabel;
    private ImageView imageViewAmount, imageViewPrice;
    private MaterialCheckBox checkBoxTotalPrice;
    private int selectedLocationId, selectedStoreId;
    private String selectedBestBeforeDate;
    private double amount, minAmount;
    private boolean nameAutoFilled;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_purchase, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (MainActivity) getActivity();
        assert activity != null;

        if(getArguments() != null) startupBundle = getArguments();

        // GET PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        // WEB REQUESTS

        request = new WebRequest(activity.getRequestQueue());
        grocyApi = activity.getGrocy();

        // UTILS

        dateUtil = new DateUtil(activity);

        // INITIALIZE VIEWS

        activity.findViewById(R.id.frame_purchase_back).setOnClickListener(
                v -> activity.onBackPressed()
        );

        // swipe refresh

        swipeRefreshLayout = activity.findViewById(R.id.swipe_purchase);
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        swipeRefreshLayout.setOnRefreshListener(this::refresh);

        // product

        textInputProduct = activity.findViewById(R.id.text_input_purchase_product);
        textInputProduct.setErrorIconDrawable(null);
        textInputProduct.setEndIconOnClickListener(v -> startActivityForResult(
                new Intent(activity, ScanInputActivity.class),
                Constants.REQUEST.SCAN
        ));
        autoCompleteTextViewProduct = (MaterialAutoCompleteTextView) textInputProduct.getEditText();
        assert autoCompleteTextViewProduct != null;
        autoCompleteTextViewProduct.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) {
                IconUtil.start(activity, R.id.image_purchase_product);
                // try again to download products
                if(productNames.isEmpty()) downloadProductNames();
            }
        });
        autoCompleteTextViewProduct.setOnItemClickListener(
                (parent, view, position, id) -> loadProductDetails(
                        getProductFromName(
                                String.valueOf(parent.getItemAtPosition(position))
                        ).getId()
                )
        );
        autoCompleteTextViewProduct.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        clearInputFocus();
                        String input = autoCompleteTextViewProduct.getText().toString().trim();
                        if(!productNames.isEmpty() && !productNames.contains(input) && !input.isEmpty()
                                && !nameAutoFilled
                        ) {
                            Bundle bundle = new Bundle();
                            bundle.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.CREATE_THEN_PURCHASE);
                            bundle.putString(Constants.ARGUMENT.PRODUCT_NAME, input);
                            activity.showBottomSheet(
                                    new InputNameBottomSheetDialogFragment(), bundle
                            );
                        }
                        return true;
                    } return false;
        });
        nameAutoFilled = false;

        // barcodes

        linearLayoutBarcodesContainer = activity.findViewById(
                R.id.linear_purchase_barcode_container
        );

        // best before date

        activity.findViewById(R.id.linear_purchase_bbd).setOnClickListener(v -> {
            if(productDetails != null) {
                Bundle bundle = new Bundle();
                bundle.putString(
                        Constants.ARGUMENT.DEFAULT_BEST_BEFORE_DAYS,
                        String.valueOf(productDetails.getProduct().getDefaultBestBeforeDays())
                );
                bundle.putString(Constants.ARGUMENT.SELECTED_DATE, selectedBestBeforeDate);
                activity.showBottomSheet(new BBDateBottomSheetDialogFragment(), bundle);
            } else {
                // no product selected
                textInputProduct.setError(activity.getString(R.string.error_select_product));
            }
        });
        textViewBestBeforeDate = activity.findViewById(R.id.text_purchase_bbd);
        textViewBbdLabel = activity.findViewById(R.id.text_purchase_bbd_label);

        // amount

        textInputAmount = activity.findViewById(R.id.text_input_purchase_amount);
        imageViewAmount = activity.findViewById(R.id.image_purchase_amount);
        editTextAmount = textInputAmount.getEditText();
        assert editTextAmount != null;
        editTextAmount.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if(!input.isEmpty()) {
                    amount = Double.parseDouble(input);
                } else {
                    amount = 0;
                }
                isAmountValid();
            }
        });
        editTextAmount.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) {
                IconUtil.start(imageViewAmount);
                // editTextAmount.selectAll();
            }
        });
        editTextAmount.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                clearInputFocus();
                return true;
            } return false;
        });

        activity.findViewById(R.id.button_purchase_amount_more).setOnClickListener(v -> {
            IconUtil.start(activity, R.id.image_purchase_amount);
            if(editTextAmount.getText().toString().isEmpty()) {
                editTextAmount.setText(String.valueOf(1));
            } else {
                double amountNew = Double.parseDouble(editTextAmount.getText().toString()) + 1;
                editTextAmount.setText(NumUtil.trim(amountNew));
            }
        });

        activity.findViewById(R.id.button_purchase_amount_less).setOnClickListener(v -> {
            if(!editTextAmount.getText().toString().isEmpty()) {
                IconUtil.start(activity, R.id.image_purchase_amount);
                double amountNew = Double.parseDouble(editTextAmount.getText().toString()) - 1;
                if(amountNew >= minAmount) {
                    editTextAmount.setText(NumUtil.trim(amountNew));
                }
            }
        });

        // price

        textInputPrice = activity.findViewById(R.id.text_input_purchase_price);
        String currency = sharedPrefs.getString(Constants.PREF.CURRENCY, "");
        if(currency.isEmpty()) {
            textInputPrice.setHint(getString(R.string.property_price));
        } else {
            textInputPrice.setHint(getString(R.string.property_price_in, currency));
        }
        imageViewPrice = activity.findViewById(R.id.image_purchase_price);
        editTextPrice = textInputPrice.getEditText();
        assert editTextPrice != null;
        editTextPrice.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {isPriceValid();}
        });
        editTextPrice.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) {
                IconUtil.start(imageViewPrice);
                // editTextAmount.selectAll();
            }
        });
        editTextPrice.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                clearInputFocus();
                return true;
            } return false;
        });

        activity.findViewById(R.id.button_purchase_price_more).setOnClickListener(v -> {
            IconUtil.start(activity, R.id.image_purchase_price);
            if(editTextPrice.getText().toString().isEmpty()) {
                editTextPrice.setText(NumUtil.trimPrice(1));
            } else {
                double priceNew = NumUtil.stringToDouble(editTextPrice.getText().toString()) + 1;
                editTextPrice.setText(NumUtil.trimPrice(priceNew));
            }
        });
        activity.findViewById(R.id.button_purchase_price_less).setOnClickListener(v -> {
            if(!editTextPrice.getText().toString().isEmpty()) {
                IconUtil.start(activity, R.id.image_purchase_price);
                double priceNew = NumUtil.stringToDouble(editTextPrice.getText().toString()) - 1;
                if(priceNew >= 0) {
                    editTextPrice.setText(NumUtil.trimPrice(priceNew));
                }
            }
        });

        checkBoxTotalPrice = activity.findViewById(R.id.checkbox_purchase_total_price);

        linearLayoutTotalPrice = activity.findViewById(R.id.linear_purchase_total_price);
        linearLayoutTotalPrice.setOnClickListener(
                v -> checkBoxTotalPrice.setChecked(!checkBoxTotalPrice.isChecked())
        );

        // store

        activity.findViewById(R.id.linear_purchase_store).setOnClickListener(v -> {
            if(productDetails != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList(Constants.ARGUMENT.STORES, stores);
                bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedStoreId);
                activity.showBottomSheet(new StoresBottomSheetDialogFragment(), bundle);
            } else {
                // no product selected
                textInputProduct.setError(activity.getString(R.string.error_select_product));
            }
        });
        textViewStore = activity.findViewById(R.id.text_purchase_store);

        // location

        activity.findViewById(R.id.linear_purchase_location).setOnClickListener(v -> {
            if(productDetails != null) {
                IconUtil.start(activity, R.id.image_purchase_location);
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList(Constants.ARGUMENT.LOCATIONS, locations);
                bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedLocationId);
                activity.showBottomSheet(new LocationsBottomSheetDialogFragment(), bundle);
            } else {
                // no product selected
                textInputProduct.setError(activity.getString(R.string.error_select_product));
            }
        });
        textViewLocation = activity.findViewById(R.id.text_purchase_location);
        textViewLocationLabel = activity.findViewById(R.id.text_purchase_location_label);

        hideDisabledFeatures();

        // START

        refresh();

        // UPDATE UI

        activity.updateUI(Constants.UI.PURCHASE, TAG);
    }

    public void giveBundle(Bundle bundle) {
        startupBundle = bundle;
    }

    private void refresh() {
        if(activity.isOnline()) {
            download();
        } else {
            swipeRefreshLayout.setRefreshing(false);
            activity.showMessage(
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
        downloadStores();
        downloadLocations();
    }

    private void downloadProductNames() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS),
                TAG,
                response -> {
                    products = gson.fromJson(
                            response,
                            new TypeToken<List<Product>>(){}.getType()
                    );
                    productNames = getProductNames();
                    adapterProducts = new MatchArrayAdapter(activity, productNames);
                    autoCompleteTextViewProduct.setAdapter(adapterProducts);
                },
                this::onError,
                this::onQueueEmpty
        );
    }

    private void downloadStores() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.STORES),
                TAG,
                response -> {
                    stores = gson.fromJson(
                            response,
                            new TypeToken<List<Store>>(){}.getType()
                    );
                    SortUtil.sortStoresByName(stores, true);
                    // Insert NONE as first element
                    stores.add(
                            0,
                            new Store(-1, activity.getString(R.string.subtitle_none))
                    );
                },
                this::onError,
                this::onQueueEmpty
        );
    }

    private void downloadLocations() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.LOCATIONS),
                TAG,
                response -> {
                    locations = gson.fromJson(
                            response,
                            new TypeToken<List<Location>>(){}.getType()
                    );
                    SortUtil.sortLocationsByName(locations, true);
                },
                this::onError,
                this::onQueueEmpty
        );
    }

    private void onError(VolleyError error) {
        Log.e(TAG, "onError: VolleyError: " + error);
        request.cancelAll(TAG);
        swipeRefreshLayout.setRefreshing(false);
        activity.showMessage(
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

    private void onQueueEmpty() {
        String action = null;
        if(startupBundle != null) {
            action = startupBundle.getString(Constants.ARGUMENT.TYPE);
        }
        if(action != null) {
            if(action.equals(Constants.ACTION.CREATE_THEN_PURCHASE)) {
                Product product = getProductFromName(
                        startupBundle.getString(Constants.ARGUMENT.PRODUCT_NAME)
                );
                if(product != null) {
                    loadProductDetails(product.getId());
                } else {
                    autoCompleteTextViewProduct.setText(
                            startupBundle.getString(Constants.ARGUMENT.PRODUCT_NAME)
                    );
                }
            } else if(action.equals(Constants.ACTION.PURCHASE_THEN_SHOPPING_LIST)
                    || action.equals(Constants.ACTION.PURCHASE_THEN_STOCK)
            ) {
                Product product = getProductFromName(
                        startupBundle.getString(Constants.ARGUMENT.PRODUCT_NAME)
                );
                if(product != null) {
                    loadProductDetails(product.getId());
                }
            }
        }
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == Constants.REQUEST.SCAN && resultCode == Activity.RESULT_OK) {
            if(data != null) {
                loadProductDetailsByBarcode(data.getStringExtra(Constants.EXTRA.SCAN_RESULT));
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private void fillWithProductDetails() {
        nameAutoFilled = true;

        clearInputFocus();

        boolean isTareWeightHandlingEnabled = productDetails
                .getProduct()
                .getEnableTareWeightHandling() == 1;

        // PRODUCT
        autoCompleteTextViewProduct.setText(productDetails.getProduct().getName());
        textInputProduct.setErrorEnabled(false);

        // BBD
        int defaultBestBeforeDays = productDetails.getProduct().getDefaultBestBeforeDays();
        if(defaultBestBeforeDays < 0) {
            selectedBestBeforeDate = Constants.DATE.NEVER_EXPIRES;
            textViewBestBeforeDate.setText(getString(R.string.subtitle_never_expires));
        } else if (defaultBestBeforeDays == 0) {
            selectedBestBeforeDate = null;
            textViewBestBeforeDate.setText(getString(R.string.subtitle_none));
        } else {
            // add default best before days to today
            selectedBestBeforeDate = DateUtil.getTodayWithDaysAdded(defaultBestBeforeDays);
            textViewBestBeforeDate.setText(
                    dateUtil.getLocalizedDate(selectedBestBeforeDate, DateUtil.FORMAT_MEDIUM)
            );
        }

        // AMOUNT
        textInputAmount.setHint(
                activity.getString(
                        R.string.property_amount_in,
                        productDetails.getQuantityUnitPurchase().getNamePlural()
                )
        );
        if(!isTareWeightHandlingEnabled) {
            minAmount = 1;
        } else {
            minAmount = productDetails.getProduct().getTareWeight()
                    + productDetails.getStockAmount();
        }

        if(startupBundle != null && startupBundle.getString(Constants.ARGUMENT.AMOUNT) != null) {
            double amount = Double.parseDouble(Objects.requireNonNull(
                    startupBundle.getString(Constants.ARGUMENT.AMOUNT)
            ));
            editTextAmount.setText(NumUtil.trim(amount));
        } else {
            // leave amount empty if tare weight handling enabled
            if(!isTareWeightHandlingEnabled) {
                String defaultAmount = sharedPrefs.getString(
                        Constants.PREF.STOCK_DEFAULT_PURCHASE_AMOUNT,
                        "1"
                );
                if(defaultAmount.isEmpty()) {
                    editTextAmount.setText(null);
                } else {
                    editTextAmount.setText(NumUtil.trim(Double.parseDouble(defaultAmount)));
                }
            } else {
                editTextAmount.setText(null);
            }
        }

        if(editTextAmount.getText().toString().isEmpty()) {
            editTextAmount.requestFocus();
            activity.showKeyboard(editTextAmount);
        }

        // set icon for tare weight, else for normal amount
        imageViewAmount.setImageResource(
                isTareWeightHandlingEnabled
                        ? R.drawable.ic_round_scale_anim
                        : R.drawable.ic_round_scatter_plot_anim
        );

        // PRICE

        if(productDetails.getLastPrice() != null && !productDetails.getLastPrice().isEmpty()) {
            editTextPrice.setText(
                    NumUtil.trimPrice(Double.parseDouble(productDetails.getLastPrice()))
            );
        } else {
            editTextPrice.setText(null);
        }

        checkBoxTotalPrice.setChecked(false);

        // deactivate checkbox if tare weight handling is on
        if(isTareWeightHandlingEnabled) {
            linearLayoutTotalPrice.setEnabled(false);
            linearLayoutTotalPrice.setAlpha(0.5f);
            checkBoxTotalPrice.setEnabled(false);
        } else {
            linearLayoutTotalPrice.setEnabled(true);
            linearLayoutTotalPrice.setAlpha(1.0f);
            checkBoxTotalPrice.setEnabled(true);
        }

        // STORE
        String storeId;
        if(productDetails.getLastShoppingLocationId() != null
                && !productDetails.getLastShoppingLocationId().isEmpty()
        ) {
            storeId = productDetails.getLastShoppingLocationId();
        } else {
            storeId = productDetails.getProduct().getStoreId();
        }
        if(storeId == null || storeId.isEmpty()) {
            selectedStoreId = -1;
            textViewStore.setText(getString(R.string.subtitle_none));
        } else {
            selectedStoreId = Integer.parseInt(storeId);
            Store store = getStore(selectedStoreId);
            if(store != null) {
                textViewStore.setText(store.getName());
            } else {
                textViewStore.setText(getString(R.string.subtitle_none));
            }
        }

        // LOCATION
        if(productDetails.getLocation() != null) {
            selectedLocationId = productDetails.getLocation().getId();
            textViewLocation.setText(productDetails.getLocation().getName());
        }
    }

    private void clearInputFocus() {
        activity.hideKeyboard();
        textInputProduct.clearFocus();
        textInputAmount.clearFocus();
        textInputPrice.clearFocus();
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
                        nameAutoFilled = true;
                        Bundle bundle = new Bundle();
                        bundle.putString(Constants.ARGUMENT.BARCODES, barcode);
                        activity.showBottomSheet(
                                new InputBarcodeBottomSheetDialogFragment(), bundle
                        );
                    } else {
                        activity.showMessage(
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
        if(!productNames.isEmpty()
                && !productNames.contains(input)
                && !input.isEmpty()
                && !nameAutoFilled
        ) {
            Bundle bundle = new Bundle();
            bundle.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.CREATE_THEN_PURCHASE);
            bundle.putString(Constants.ARGUMENT.PRODUCT_NAME, input);
            activity.showBottomSheet(
                    new InputNameBottomSheetDialogFragment(), bundle
            );
            return true;
        } else if(productDetails == null) {
            textInputProduct.setError(activity.getString(R.string.error_select_product));
            return true;
        } else return !isBestBeforeDateValid() || !isAmountValid()
                || !isPriceValid() || !isLocationValid();
    }

    public void purchaseProduct() {
        if(isFormIncomplete()) return;
        double amountMultiplied = amount * productDetails.getProduct().getQuFactorPurchaseToStock();
        JSONObject body = new JSONObject();
        try {
            body.put("amount", amountMultiplied);
            body.put("transaction_type", "purchase");
            if(!editTextPrice.getText().toString().isEmpty()) {
                double price = NumUtil.stringToDouble(editTextPrice.getText().toString());
                if(checkBoxTotalPrice.isChecked()) {
                    price = price / amount;
                }
                body.put("price", price);
            }
            body.put("best_before_date", selectedBestBeforeDate);
            if(selectedStoreId > -1) {
                body.put("shopping_location_id", selectedStoreId);
            }
            body.put("location_id", selectedLocationId);
        } catch (JSONException e) {
            if(DEBUG) Log.e(TAG, "purchaseProduct: " + e);
        }
        request.post(
                grocyApi.purchaseProduct(productDetails.getProduct().getId()),
                body,
                response -> {
                    // ADD BARCODES TO PRODUCT
                    editProductBarcodes();

                    // UNDO OPTION
                    String transactionId = null;
                    try {
                        transactionId = response.getString("transaction_id");
                    } catch (JSONException e) {
                        if(DEBUG) Log.e(TAG, "purchaseProduct: " + e);
                    }
                    if(DEBUG) Log.i(TAG, "purchaseProduct: purchased " + amountMultiplied);

                    double amountAdded;
                    if(productDetails.getProduct().getEnableTareWeightHandling() == 0) {
                        amountAdded = amountMultiplied;
                    } else {
                        // calculate difference of amount if tare weight handling enabled
                        amountAdded = amountMultiplied - productDetails.getProduct().getTareWeight()
                                - productDetails.getStockAmount();
                    }

                    Snackbar snackbar = Snackbar.make(
                            activity.findViewById(R.id.linear_container_main),
                            activity.getString(
                                    R.string.msg_purchased,
                                    NumUtil.trim(amountAdded),
                                    amountMultiplied == 1
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

                    String action = null;
                    if(startupBundle != null) {
                        action = startupBundle.getString(Constants.ARGUMENT.TYPE);
                    }
                    if(action != null) {
                        if(action.equals(Constants.ACTION.PURCHASE_THEN_SHOPPING_LIST)) {
                            // delete entry from shopping list
                            ShoppingListItem shoppingListItem = startupBundle.getParcelable(
                                    Constants.ARGUMENT.SHOPPING_LIST_ITEM
                            );
                            assert shoppingListItem != null;
                            request.delete(
                                    grocyApi.getObject(
                                            GrocyApi.ENTITY.SHOPPING_LIST,
                                            shoppingListItem.getId()
                                    ),
                                    response1 -> activity.dismissFragment(),
                                    error -> activity.dismissFragment()
                            );
                            return;
                        } else if(action.equals(Constants.ACTION.PURCHASE_THEN_STOCK)) {
                            activity.dismissFragment();
                            return;
                        }
                    }

                    // CLEAR USER INPUT
                    nameAutoFilled = false;
                    clearAll();
                },
                error -> {
                    showErrorMessage();
                    if(DEBUG) Log.i(TAG, "purchaseProduct: " + error);
                }
        );
    }

    private void undoTransaction(String transactionId) {
        request.post(
                grocyApi.undoStockTransaction(transactionId),
                success -> {
                    activity.showMessage(
                            Snackbar.make(
                                    activity.findViewById(R.id.linear_container_main),
                                    activity.getString(R.string.msg_undone_transaction),
                                    Snackbar.LENGTH_SHORT
                            )
                    );
                    if(DEBUG) Log.i(TAG, "undoTransaction: undone");
                },
                error -> showErrorMessage()
        );
    }

    private void editProductBarcodes() {
        if(linearLayoutBarcodesContainer.getChildCount() == 0) return;

        String barcodesString = productDetails.getProduct().getBarcode();
        ArrayList<String> barcodes;
        if(barcodesString == null || barcodesString.isEmpty()) {
            barcodes = new ArrayList<>();
        } else {
            barcodes = new ArrayList<>(
                    Arrays.asList(
                            productDetails.getProduct().getBarcode().split(",")
                    )
            );
        }

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

    private void hideDisabledFeatures() {
        if(!sharedPrefs.getBoolean(
                Constants.PREF.FEATURE_FLAG_STOCK_PRICE_TRACKING,
                true
        )) {
            activity.findViewById(R.id.linear_purchase_total_price).setVisibility(View.GONE);
            activity.findViewById(R.id.linear_purchase_price).setVisibility(View.GONE);
        }
    }

    public void setUpBottomMenu() {
        MenuItem menuItemBatch, menuItemDetails;
        menuItemBatch = activity.getBottomMenu().findItem(R.id.action_batch_mode);
        menuItemBatch.setOnMenuItemClickListener(item -> {
            Intent intent = new Intent(activity, ScanBatchActivity.class);
            intent.putExtra(Constants.ARGUMENT.TYPE, Constants.ACTION.PURCHASE);
            activity.startActivityForResult(intent, Constants.REQUEST.SCAN_BATCH);
            return true;
        });
        menuItemDetails = activity.getBottomMenu().findItem(R.id.action_product_overview);
        if(menuItemDetails != null) {
            menuItemDetails.setOnMenuItemClickListener(item -> {
                IconUtil.start(menuItemDetails);
                if(productDetails != null) {
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(Constants.ARGUMENT.PRODUCT_DETAILS, productDetails);
                    activity.showBottomSheet(
                            new ProductOverviewBottomSheetDialogFragment(),
                            bundle
                    );
                } else {
                    textInputProduct.setError(activity.getString(R.string.error_select_product));
                }
                return true;
            });
        }
    }

    public void selectBestBeforeDate(String selectedBestBeforeDate) {
        this.selectedBestBeforeDate = selectedBestBeforeDate;
        if(selectedBestBeforeDate.equals(Constants.DATE.NEVER_EXPIRES)) {
            textViewBestBeforeDate.setText(getString(R.string.subtitle_never_expires));
        } else {
            textViewBestBeforeDate.setText(
                    dateUtil.getLocalizedDate(selectedBestBeforeDate, DateUtil.FORMAT_MEDIUM)
            );
        }
        isBestBeforeDateValid();
    }

    public void selectStore(int selectedId) {
        this.selectedStoreId = selectedId;
        if(stores.isEmpty()) {
            textViewLocation.setText(getString(R.string.subtitle_none));
        } else {
            Store store = getStore(selectedId);
            if(store != null) {
                textViewStore.setText(store.getName());
            } else {
                textViewStore.setText(getString(R.string.subtitle_none));
                showErrorMessage();
            }
        }
    }

    public void selectLocation(int selectedId) {
        this.selectedLocationId = selectedId;
        if(locations.isEmpty()) {
            textViewLocation.setText(getString(R.string.subtitle_none));
        } else {
            Location location = getLocation(selectedId);
            if(location != null) {
                textViewLocation.setText(location.getName());
            } else {
                textViewLocation.setText(getString(R.string.subtitle_none));
                showErrorMessage();
            }
        }
        isLocationValid();
    }

    private boolean isBestBeforeDateValid() {
        if(selectedBestBeforeDate == null || selectedBestBeforeDate.isEmpty()) {
            textViewBbdLabel.setTextColor(getColor(R.color.error));
            return false;
        } else {
            textViewBbdLabel.setTextColor(getColor(R.color.on_background_secondary));
            return true;
        }
    }

    private boolean isLocationValid() {
        if(selectedLocationId < 0) {
            textViewLocationLabel.setTextColor(getColor(R.color.error));
            return false;
        } else {
            textViewLocationLabel.setTextColor(getColor(R.color.on_background_secondary));
            return true;
        }
    }

    private boolean isAmountValid() {
        if(amount >= minAmount) {
            textInputAmount.setErrorEnabled(false);
            return true;
        } else {
            if(productDetails != null) {
                textInputAmount.setError(
                        activity.getString(
                                R.string.error_bounds_min,
                                NumUtil.trim(minAmount)
                        )
                );
            }
            return false;
        }
    }

    private boolean isPriceValid() {
        if(!editTextPrice.getText().toString().isEmpty()) {
            if(NumUtil.stringToDouble(editTextPrice.getText().toString()) >= 0) {
                textInputPrice.setErrorEnabled(false);
                return true;
            } else {
                if(productDetails != null) {
                    textInputPrice.setError(
                            activity.getString(
                                    R.string.error_bounds_min,
                                    NumUtil.trim(0)
                            )
                    );
                }
                return false;
            }
        } else {
            textInputPrice.setErrorEnabled(false);
            return true;
        }
    }

    private Store getStore(int storeId) {
        for(Store store : stores) {
            if(store.getId() == storeId) {
                return store;
            }
        } return null;
    }

    private Location getLocation(int locationId) {
        for(Location location : locations) {
            if(location.getId() == locationId) {
                return location;
            }
        } return null;
    }

    public void addInputAsBarcode() {
        String input = autoCompleteTextViewProduct.getText().toString().trim();
        if(input.isEmpty()) return;
        for(int i = 0; i < linearLayoutBarcodesContainer.getChildCount(); i++) {
            InputChip inputChip = (InputChip) linearLayoutBarcodesContainer.getChildAt(i);
            if(inputChip.getText().equals(input)) {
                activity.showMessage(
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
                activity, input, R.drawable.ic_round_barcode, true
        );
        inputChipBarcode.setPadding(0, 0, 0, 8);
        linearLayoutBarcodesContainer.addView(inputChipBarcode);
        autoCompleteTextViewProduct.setText(null);
        autoCompleteTextViewProduct.requestFocus();
    }

    public void clearAll() {
        productDetails = null;
        textInputProduct.setErrorEnabled(false);
        autoCompleteTextViewProduct.setText(null);
        textViewBestBeforeDate.setText(activity.getString(R.string.subtitle_none));
        textViewBbdLabel.setTextColor(getColor(R.color.on_background_secondary));
        textInputAmount.setErrorEnabled(false);
        editTextAmount.setText(null);
        imageViewAmount.setImageResource(R.drawable.ic_round_scatter_plot_anim);
        textInputPrice.setErrorEnabled(false);
        editTextPrice.setText(null);
        linearLayoutTotalPrice.setAlpha(1.0f);
        linearLayoutTotalPrice.setEnabled(true);
        checkBoxTotalPrice.setEnabled(true);
        checkBoxTotalPrice.setChecked(false);
        textViewStore.setText(activity.getString(R.string.subtitle_none));
        textViewLocation.setText(activity.getString(R.string.subtitle_none));
        textViewLocationLabel.setTextColor(getColor(R.color.on_background_secondary));
        clearInputFocus();
        for(int i = 0; i < linearLayoutBarcodesContainer.getChildCount(); i++) {
            ((InputChip) linearLayoutBarcodesContainer.getChildAt(i)).close();
        }
        nameAutoFilled = false;
    }

    private void showErrorMessage() {
        activity.showMessage(
                Snackbar.make(
                        activity.findViewById(R.id.linear_container_main),
                        activity.getString(R.string.msg_error),
                        Snackbar.LENGTH_SHORT
                )
        );
    }

    private int getColor(@ColorRes int color) {
        return ContextCompat.getColor(activity, color);
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
