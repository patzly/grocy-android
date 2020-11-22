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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
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
import java.util.Locale;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.activity.ScanInputActivity;
import xyz.zedler.patrick.grocy.adapter.MatchArrayAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.OpenFoodFactsApi;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterProductSimpleBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LocationsBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductGroupsBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.TextEditBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.CreateProduct;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.view.InputChip;

public class MasterProductSimpleFragment extends Fragment {

    private final static String TAG = Constants.UI.MASTER_PRODUCT_SIMPLE;

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private DownloadHelper dlHelper;
    private FragmentMasterProductSimpleBinding binding;
    private ArrayAdapter<String> adapterProducts;

    private ArrayList<Product> products;
    private ArrayList<Location> locations;
    private ArrayList<ProductGroup> productGroups;
    private ArrayList<QuantityUnit> quantityUnits;
    private ArrayList<String> productNames;

    private Product editProduct, productParent;

    private CreateProduct createProductObj;

    private SwipeRefreshLayout swipeRefreshLayout;
    private MaterialAutoCompleteTextView autoCompleteTextViewParentProduct;
    private LinearLayout linearLayoutBarcodeContainer;
    private TextInputLayout
            textInputName,
            textInputParentProduct,
            textInputBarcodes,
            textInputMinAmount,
            textInputDays,
            textInputQUFactor;
    private EditText
            editTextName,
            editTextBarcodes,
            editTextMinAmount,
            editTextDays,
            editTextQUFactor;
    private TextView
            textViewLocation,
            textViewLocationLabel,
            textViewProductGroup,
            textViewQUPurchase,
            textViewQUPurchaseLabel,
            textViewQUStock,
            textViewQUStockLabel,
            textViewDescription;
    private ImageView imageViewName, imageViewMinAmount, imageViewDays, imageViewQUFactor;
    private int selectedLocationId = -1,
            selectedQUPurchaseId = -1,
            selectedQUStockId = -1,
            selectedProductGroupId = -1;
    private String productDescriptionHtml = "";
    private String intendedAction;
    private double minAmount;
    private int quantityUnitFactor;
    private int bestBeforeDays;
    private boolean debug;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentMasterProductSimpleBinding.inflate(
                inflater, container, false
        );
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        dlHelper.destroy();
    }

    @Override
    public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (MainActivity) getActivity();
        assert activity != null;

        // PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        // WEB

        dlHelper = new DownloadHelper(activity, TAG);
        grocyApi = activity.getGrocy();

        // VARIABLES

        products = new ArrayList<>();
        locations = new ArrayList<>();
        productGroups = new ArrayList<>();
        quantityUnits = new ArrayList<>();
        productNames = new ArrayList<>();
        editProduct = null;
        productParent = null;
        createProductObj = null;

        selectedLocationId = -1;
        selectedQUPurchaseId = -1;
        selectedQUStockId = -1;
        selectedProductGroupId = -1;

        productDescriptionHtml = "";
        intendedAction = null;
        minAmount = 0;
        quantityUnitFactor = 1;
        bestBeforeDays = 0;

        // INITIALIZE VIEWS

        activity.findViewById(R.id.frame_master_product_simple_cancel).setOnClickListener(
                v -> activity.onBackPressed()
        );

        // swipe refresh
        swipeRefreshLayout = activity.findViewById(R.id.swipe_master_product_simple);
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        swipeRefreshLayout.setOnRefreshListener(this::refresh);

        // name
        textInputName = activity.findViewById(R.id.text_input_master_product_simple_name);
        imageViewName = activity.findViewById(R.id.image_master_product_simple_name);
        editTextName = textInputName.getEditText();
        assert editTextName != null;
        editTextName.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) IconUtil.start(imageViewName);
        });

        // parent product
        textInputParentProduct = activity.findViewById(
                R.id.text_input_master_product_simple_parent_product
        );
        textInputParentProduct.setErrorIconDrawable(null);
        textInputParentProduct.setEndIconOnClickListener(v -> startActivityForResult(
                new Intent(activity, ScanInputActivity.class),
                Constants.REQUEST.SCAN_PARENT_PRODUCT
        ));
        autoCompleteTextViewParentProduct =
                (MaterialAutoCompleteTextView) textInputParentProduct.getEditText();
        assert autoCompleteTextViewParentProduct != null;
        autoCompleteTextViewParentProduct.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(!hasFocus || !productNames.isEmpty()) return;
            // try again to download products
            dlHelper.getProducts(products -> {
                this.products = products;
                productNames = getProductNames();
                adapterProducts = new MatchArrayAdapter(activity, productNames);
                autoCompleteTextViewParentProduct.setAdapter(adapterProducts);
            }).perform(dlHelper.getUuid());
        });
        autoCompleteTextViewParentProduct.setOnItemClickListener(
                (parent, v, position, id) -> productParent = getProductFromName(
                        String.valueOf(parent.getItemAtPosition(position))
                )
        );
        autoCompleteTextViewParentProduct.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        editTextBarcodes.requestFocus();
                        return true;
                    } return false;
                });

        // description

        activity.findViewById(
                R.id.linear_master_product_simple_description
        ).setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString(Constants.ARGUMENT.HTML, productDescriptionHtml);
            bundle.putString(Constants.ARGUMENT.TITLE, "Edit description");
            bundle.putString(Constants.ARGUMENT.HINT, "Product description");
            activity.showBottomSheet(new TextEditBottomSheetDialogFragment(), bundle);
        });
        textViewDescription = activity.findViewById(R.id.text_master_product_simple_description);

        // barcodes

        textInputBarcodes = activity.findViewById(
                R.id.text_input_master_product_simple_barcodes
        );
        textInputBarcodes.setErrorIconDrawable(null);
        textInputBarcodes.setEndIconOnClickListener(v -> startActivityForResult(
                new Intent(activity, ScanInputActivity.class),
                Constants.REQUEST.SCAN
        ));
        editTextBarcodes = textInputBarcodes.getEditText();
        assert editTextBarcodes != null;
        editTextBarcodes.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addInputAsBarcode();
                return true;
            } return false;
        });
        linearLayoutBarcodeContainer = activity.findViewById(
                R.id.linear_master_product_simple_barcode_container
        );

        // location
        activity.findViewById(
                R.id.linear_master_product_simple_location
        ).setOnClickListener(v -> {
            IconUtil.start(activity, R.id.image_master_product_simple_location);
            if(!locations.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList(
                        Constants.ARGUMENT.LOCATIONS,
                        locations
                );
                bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedLocationId);
                activity.showBottomSheet(new LocationsBottomSheetDialogFragment(), bundle);
            }
        });
        textViewLocation = activity.findViewById(R.id.text_master_product_simple_location);
        textViewLocationLabel = activity.findViewById(
                R.id.text_master_product_simple_location_label
        );

        // min stock amount
        textInputMinAmount = activity.findViewById(
                R.id.text_input_master_product_simple_amount
        );
        imageViewMinAmount = activity.findViewById(R.id.image_master_product_simple_amount);
        editTextMinAmount = textInputMinAmount.getEditText();
        assert editTextMinAmount != null;
        editTextMinAmount.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if(!input.isEmpty()) {
                    minAmount = Double.parseDouble(input);
                } else {
                    minAmount = 0;
                }
            }
        });
        editTextMinAmount.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) IconUtil.start(imageViewMinAmount);
        });
        editTextMinAmount.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                editTextMinAmount.clearFocus();
                activity.hideKeyboard();
                return true;
            } return false;
        });

        activity.findViewById(
                R.id.button_master_product_simple_amount_more
        ).setOnClickListener(v -> {
            IconUtil.start(imageViewMinAmount);
            if(editTextMinAmount.getText().toString().isEmpty()) {
                editTextMinAmount.setText(String.valueOf(0));
            } else {
                double amountNew = Double.parseDouble(editTextMinAmount.getText().toString()) + 1;
                editTextMinAmount.setText(NumUtil.trim(amountNew));
            }
        });

        activity.findViewById(
                R.id.button_master_product_simple_amount_less
        ).setOnClickListener(v -> {
            if(!editTextMinAmount.getText().toString().isEmpty()) {
                IconUtil.start(imageViewMinAmount);
                double amountNew = Double.parseDouble(editTextMinAmount.getText().toString()) - 1;
                if(amountNew >= 0) {
                    editTextMinAmount.setText(NumUtil.trim(amountNew));
                }
            }
        });

        // best before days
        textInputDays = activity.findViewById(R.id.text_input_master_product_simple_days);
        imageViewDays = activity.findViewById(R.id.image_master_product_simple_days);
        editTextDays = textInputDays.getEditText();
        assert editTextDays != null;
        editTextDays.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if(!input.isEmpty()) {
                    try {
                        bestBeforeDays = Integer.parseInt(input);
                    } catch (NumberFormatException e) {
                        bestBeforeDays = -1;
                    }
                } else {
                    bestBeforeDays = 0;
                }
            }
        });
        editTextDays.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) {
                IconUtil.start(imageViewDays);
            }
        });
        editTextDays.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                editTextDays.clearFocus();
                activity.hideKeyboard();
                return true;
            } return false;
        });

        activity.findViewById(
                R.id.button_master_product_simple_days_more
        ).setOnClickListener(v -> {
            IconUtil.start(imageViewDays);
            if(editTextDays.getText().toString().isEmpty()) {
                editTextDays.setText(String.valueOf(0));
            } else {
                int daysNew = Integer.parseInt(editTextDays.getText().toString()) + 1;
                editTextDays.setText(String.valueOf(daysNew));
            }
        });

        activity.findViewById(
                R.id.button_master_product_simple_days_less
        ).setOnClickListener(v -> {
            if(!editTextDays.getText().toString().isEmpty()) {
                IconUtil.start(imageViewDays);
                int daysNew = Integer.parseInt(editTextDays.getText().toString()) - 1;
                if(daysNew >= -1) {
                    editTextDays.setText(String.valueOf(daysNew));
                }
            }
        });

        // product group
        activity.findViewById(
                R.id.linear_master_product_simple_product_group
        ).setOnClickListener(v -> {
            IconUtil.start(activity, R.id.image_master_product_simple_product_group);
            if(!productGroups.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList(Constants.ARGUMENT.PRODUCT_GROUPS, productGroups);
                bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedProductGroupId);
                activity.showBottomSheet(new ProductGroupsBottomSheetDialogFragment(), bundle);
            }
        });
        textViewProductGroup = activity.findViewById(
                R.id.text_master_product_simple_product_group
        );

        // quantity unit purchase
        activity.findViewById(
                R.id.linear_master_product_simple_qu_purchase
        ).setOnClickListener(v -> {
            IconUtil.start(activity, R.id.image_master_product_simple_qu_purchase);
            if(!quantityUnits.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList(Constants.ARGUMENT.QUANTITY_UNITS, quantityUnits);
                bundle.putString(Constants.ARGUMENT.TYPE, Constants.ARGUMENT.QU_PURCHASE);
                bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedQUPurchaseId);
                activity.showBottomSheet(new QuantityUnitsBottomSheetDialogFragment(), bundle);
            }
        });
        textViewQUPurchase = activity.findViewById(
                R.id.text_master_product_simple_qu_purchase
        );
        textViewQUPurchaseLabel = activity.findViewById(
                R.id.text_master_product_simple_qu_label_purchase
        );

        // quantity unit stock
        activity.findViewById(
                R.id.linear_master_product_simple_qu_stock
        ).setOnClickListener(v -> {
            IconUtil.start(activity, R.id.image_master_product_simple_qu_stock);
            if(!quantityUnits.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList(Constants.ARGUMENT.QUANTITY_UNITS, quantityUnits);
                bundle.putString(Constants.ARGUMENT.TYPE, Constants.ARGUMENT.QU_STOCK);
                bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedQUStockId);
                activity.showBottomSheet(new QuantityUnitsBottomSheetDialogFragment(), bundle);
            }
        });
        textViewQUStock = activity.findViewById(
                R.id.text_master_product_simple_qu_stock
        );
        textViewQUStockLabel = activity.findViewById(
                R.id.text_master_product_simple_qu_label_stock
        );

        // quantity unit factor
        textInputQUFactor = activity.findViewById(
                R.id.text_input_master_product_simple_qu_factor
        );
        imageViewQUFactor = activity.findViewById(R.id.image_master_product_simple_qu_factor);
        editTextQUFactor = textInputQUFactor.getEditText();
        assert editTextQUFactor != null;
        editTextQUFactor.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if(!input.isEmpty() && NumUtil.isStringInt(input) && Integer.parseInt(input) > 0) {
                    quantityUnitFactor = Integer.parseInt(input);
                    textInputQUFactor.setErrorEnabled(false);
                } else if(!input.isEmpty()) {
                    textInputQUFactor.setError(activity.getString(R.string.error_invalid_factor));
                } else {
                    quantityUnitFactor = 1;
                }
            }
        });
        editTextQUFactor.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) IconUtil.start(imageViewQUFactor);
        });
        editTextQUFactor.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                editTextQUFactor.clearFocus();
                activity.hideKeyboard();
                return true;
            } return false;
        });

        activity.findViewById(
                R.id.button_master_product_simple_qu_factor_more
        ).setOnClickListener(v -> {
            IconUtil.start(imageViewQUFactor);
            if(editTextQUFactor.getText().toString().isEmpty()) {
                editTextQUFactor.setText(String.valueOf(1));
            } else {
                double amountNew = Double.parseDouble(editTextQUFactor.getText().toString()) + 1;
                editTextQUFactor.setText(NumUtil.trim(amountNew));
            }
        });

        activity.findViewById(
                R.id.button_master_product_simple_qu_factor_less
        ).setOnClickListener(v -> {
            if(!editTextQUFactor.getText().toString().isEmpty()) {
                IconUtil.start(imageViewQUFactor);
                double amountNew = Double.parseDouble(editTextQUFactor.getText().toString()) - 1;
                if(amountNew >= 1) {
                    editTextQUFactor.setText(NumUtil.trim(amountNew));
                }
            }
        });

        // STARTUP BUNDLE

        Bundle bundle = getArguments();
        if(bundle != null && bundle.getString(Constants.ARGUMENT.TYPE) != null) {
            intendedAction = bundle.getString(Constants.ARGUMENT.TYPE);
        }

        if(intendedAction == null) {
            intendedAction = Constants.ACTION.CREATE;
        }

        if(bundle == null) {
            resetAll();
        } else {
            switch (intendedAction) {
                case Constants.ACTION.EDIT:
                case Constants.ACTION.EDIT_THEN_PURCHASE_BATCH:
                case Constants.ACTION.EDIT_THEN_CONSUME:
                case Constants.ACTION.EDIT_THEN_PURCHASE:
                case Constants.ACTION.EDIT_THEN_SHOPPING_LIST_ITEM_EDIT:
                    editProduct = bundle.getParcelable(Constants.ARGUMENT.PRODUCT);
                    break;
                case Constants.ACTION.CREATE:
                case Constants.ACTION.CREATE_THEN_PURCHASE:
                case Constants.ACTION.CREATE_THEN_PURCHASE_BATCH:
                case Constants.ACTION.CREATE_THEN_SHOPPING_LIST_ITEM_EDIT:
                    createProductObj = bundle.getParcelable(
                            Constants.ARGUMENT.CREATE_PRODUCT_OBJECT
                    );
            }
        }

        hideDisabledFeatures();

        // START

        if(savedInstanceState == null) {
            load();
        } else {
            restoreSavedInstanceState(savedInstanceState);
        }

        // UPDATE UI

        activity.updateUI(
                Constants.UI.MASTER_PRODUCT_SIMPLE,
                savedInstanceState == null,
                TAG
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(isHidden()) return;

        outState.putParcelableArrayList("products", products);
        outState.putParcelableArrayList("locations", locations);
        outState.putParcelableArrayList("productGroups", productGroups);
        outState.putParcelableArrayList("quantityUnits", quantityUnits);

        outState.putParcelable("editProduct", editProduct);
        outState.putParcelable("productParent", productParent);
        outState.putParcelable("createProductObj", createProductObj);

        outState.putInt("selectedLocationId", selectedLocationId);
        outState.putInt("selectedQUPurchaseId", selectedQUPurchaseId);
        outState.putInt("selectedQUStockId", selectedQUStockId);
        outState.putInt("selectedProductGroupId", selectedProductGroupId);

        outState.putString("productDescriptionHtml", productDescriptionHtml);
        outState.putString("intendedAction", intendedAction);

        outState.putDouble("minAmount", minAmount);

        outState.putInt("quantityUnitFactor", quantityUnitFactor);
        outState.putInt("bestBeforeDays", bestBeforeDays);
    }

    private void restoreSavedInstanceState(@NonNull Bundle savedInstanceState) {
        if(isHidden()) return;

        products = savedInstanceState.getParcelableArrayList("products");
        locations = savedInstanceState.getParcelableArrayList("locations");
        productGroups = savedInstanceState.getParcelableArrayList("productGroups");
        quantityUnits = savedInstanceState.getParcelableArrayList("quantityUnits");

        productNames = getProductNames();
        adapterProducts = new MatchArrayAdapter(activity, productNames);
        autoCompleteTextViewParentProduct.setAdapter(adapterProducts);

        editProduct = savedInstanceState.getParcelable("editProduct");
        productParent = savedInstanceState.getParcelable("productParent");
        createProductObj = savedInstanceState.getParcelable("createProductObj");

        selectedLocationId = savedInstanceState.getInt("selectedLocationId");
        selectedQUPurchaseId = savedInstanceState.getInt("selectedQUPurchaseId");
        selectedQUStockId = savedInstanceState.getInt("selectedQUStockId");
        selectedProductGroupId = savedInstanceState.getInt("selectedProductGroupId");

        productDescriptionHtml = savedInstanceState.getString("productDescriptionHtml");
        intendedAction = savedInstanceState.getString("intendedAction");

        minAmount = savedInstanceState.getDouble("minAmount");

        quantityUnitFactor = savedInstanceState.getInt("quantityUnitFactor");
        bestBeforeDays = savedInstanceState.getInt("bestBeforeDays");

        onQueueEmpty();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if(!hidden && getView() != null) onViewCreated(getView(), null);
    }

    private void load() {
        if(activity.isOnline()) {
            download();
        }
    }

    private void refresh() {
        // for only fill with up-to-date data on refresh,
        // not on startup as the bundle should contain everything needed
        if(activity.isOnline()) {
            download();
        } else {
            swipeRefreshLayout.setRefreshing(false);
            activity.showMessage(
                    Snackbar.make(
                            activity.findViewById(R.id.frame_main_container),
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
    }

    private void download() {
        swipeRefreshLayout.setRefreshing(true);

        DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
        queue.append(
                dlHelper.getProducts(products -> {
                    this.products = products;
                    productNames = getProductNames();
                    adapterProducts = new MatchArrayAdapter(activity, productNames);
                    autoCompleteTextViewParentProduct.setAdapter(adapterProducts);
                }),
                dlHelper.getProductGroups(productGroups -> {
                    this.productGroups = productGroups;
                    SortUtil.sortProductGroupsByName(this.productGroups, true);
                    // Insert NONE as first element
                    productGroups.add(
                            0,
                            new ProductGroup(
                                    -1,
                                    activity.getString(R.string.subtitle_none_selected)
                            )
                    );
                }),
                dlHelper.getQuantityUnits(quantityUnits -> {
                    this.quantityUnits = quantityUnits;
                    SortUtil.sortQuantityUnitsByName(this.quantityUnits, true);
                })
        );
        if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
            queue.append(
                    dlHelper.getLocations(locations -> {
                        this.locations = locations;
                        SortUtil.sortLocationsByName(this.locations, true);
                    })
            );
        }
        queue.start();
    }

    private void onDownloadError(VolleyError error) {
        if(debug) Log.e(TAG, "onError: VolleyError: " + error);
        swipeRefreshLayout.setRefreshing(false);
        activity.showMessage(
                Snackbar.make(
                        activity.findViewById(R.id.frame_main_container),
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

    private void onQueueEmpty() {
        swipeRefreshLayout.setRefreshing(false);

        if(editProduct != null) {
            fillWithEditReferences();
            isFormInvalid();
        } else if(createProductObj != null) {
            fillWithCreateProductObject();
            fillWithPresets();
            isFormInvalid();
        } else {
            resetAll();
            fillWithPresets();
        }
    }

    private ArrayList<String> getProductNames() {
        ArrayList<String> names = new ArrayList<>();
        if(products != null) {
            for(Product product : products) {
                if(editProduct != null) {
                    if(product.getId() != editProduct.getId()) {
                        names.add(product.getName());
                    }
                } else {
                    names.add(product.getName());
                }
            }
        }
        return names;
    }

    private Product getProduct(int productId) {
        for(Product product : products) {
            if(product.getId() == productId) {
                return product;
            }
        } return null;
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

    public void editDescription(String descriptionHtml, String description) {
        if(description == null || descriptionHtml == null || description.trim().isEmpty()) {
            textViewDescription.setText(activity.getString(R.string.subtitle_empty));
            productDescriptionHtml = "";
        } else {
            description = description.trim();
            productDescriptionHtml = descriptionHtml;
            if(description.length() > 100) {
                textViewDescription.setText(description.subSequence(0, 100));
            } else {
                textViewDescription.setText(description);
            }
        }
    }

    public void selectLocation(int selectedId) {
        selectedLocationId = selectedId;
        String locationText = null;
        if(locations.isEmpty()) {
            locationText = activity.getString(R.string.subtitle_none_selected);
        } else {
            Location location = getLocation(selectedId);
            if(location != null) {
                locationText = location.getName();
            }
            textViewLocationLabel.setTextColor(getColor(R.color.on_background_secondary));
        }
        textViewLocation.setText(locationText);
    }

    private Location getLocation(int locationId) {
        if(locationId == -1 || locations.isEmpty()) return null;
        for(Location location : locations) {
            if(location.getId() == locationId) {
                return location;
            }
        } return null;
    }

    public void selectProductGroup(int selectedId) {
        selectedProductGroupId = selectedId;
        String productGroupText;
        if(productGroups.isEmpty()) {
            productGroupText = activity.getString(R.string.subtitle_none_selected);
        } else {
            ProductGroup productGroup = getProductGroup(selectedId);
            if(productGroup != null) {
                productGroupText = productGroup.getName();
            } else {
                productGroupText = activity.getString(R.string.subtitle_none_selected);
            }
        }
        textViewProductGroup.setText(productGroupText);
    }

    private ProductGroup getProductGroup(String productGroupId) {
        if(productGroupId == null || productGroupId.isEmpty()) return null;
        for(ProductGroup productGroup : productGroups) {
            if(productGroup.getId() == Integer.parseInt(productGroupId)) {
                return productGroup;
            }
        }
        return null;
    }

    private ProductGroup getProductGroup(int productGroupId) {
        if(productGroupId == -1) return null;
        for(ProductGroup productGroup : productGroups) {
            if(productGroup.getId() == productGroupId) {
                return productGroup;
            }
        }
        return null;
    }

    public void selectQuantityUnitPurchase(int selectedId) {
        selectedQUPurchaseId = selectedId;
        String quantityUnitText = null;
        if(quantityUnits.isEmpty()) {
            quantityUnitText = activity.getString(R.string.subtitle_none_selected);
        } else {
            QuantityUnit quantityUnit = getQuantityUnit(selectedId);
            if(quantityUnit != null) {
                quantityUnitText = quantityUnit.getName();
            }
            textViewQUPurchaseLabel.setTextColor(getColor(R.color.on_background_secondary));
        }
        textViewQUPurchase.setText(quantityUnitText);
        if(selectedQUStockId == -1) {
            selectQuantityUnitStock(selectedId);
        }
    }

    public void selectQuantityUnitStock(int selectedId) {
        selectedQUStockId = selectedId;
        String quantityUnitText = null;
        if(quantityUnits.isEmpty()) {
            quantityUnitText = activity.getString(R.string.subtitle_none_selected);
        } else {
            QuantityUnit quantityUnit = getQuantityUnit(selectedId);
            if(quantityUnit != null) {
                quantityUnitText = quantityUnit.getName();
            }
            textViewQUStockLabel.setTextColor(getColor(R.color.on_background_secondary));
        }
        textViewQUStock.setText(quantityUnitText);
    }

    private QuantityUnit getQuantityUnit(int quantityUnitId) {
        if(quantityUnitId == -1) return null;
        for(QuantityUnit quantityUnit : quantityUnits) {
            if(quantityUnit.getId() == quantityUnitId) {
                return quantityUnit;
            }
        } return null;
    }

    private void fillWithEditReferences() {
        clearInputFocusAndErrors();
        if(editProduct != null) {
            // name
            editTextName.setText(editProduct.getName());
            // parent product
            Product parentProduct = null;
            if(NumUtil.isStringInt(editProduct.getParentProductId())) {
                parentProduct = getProduct(Integer.parseInt(editProduct.getParentProductId()));
            }
            if(parentProduct != null) {
                autoCompleteTextViewParentProduct.setText(parentProduct.getName());
            } else {
                autoCompleteTextViewParentProduct.setText(null);
            }
            // description
            if(editProduct.getDescription() != null) {
                editDescription(
                        editProduct.getDescription(),
                        Html.fromHtml(editProduct.getDescription()).toString()
                );
            } else {
                editDescription(
                        editProduct.getDescription(),
                        ""
                );
            }
            // barcodes
            if(editProduct.getBarcode() != null && !editProduct.getBarcode().trim().isEmpty()) {
                String[] barcodes = editProduct.getBarcode().split(",");
                linearLayoutBarcodeContainer.removeAllViews();
                for(String tmpBarcode : barcodes) {
                    InputChip inputChipBarcode = new InputChip(
                            activity,
                            tmpBarcode.trim(),
                            false,
                            () -> { });
                    linearLayoutBarcodeContainer.addView(inputChipBarcode);
                }
            } else {
                if(linearLayoutBarcodeContainer.getChildCount() > 0) {
                    for(int i = 0; i < linearLayoutBarcodeContainer.getChildCount(); i++) {
                        InputChip inputChip = (InputChip)linearLayoutBarcodeContainer.getChildAt(i);
                        inputChip.close();
                    }
                }
            }
            // location
            Location location = getLocation(editProduct.getLocationId());
            if(location != null) {
                textViewLocation.setText(location.getName());
            }
            selectedLocationId = editProduct.getLocationId();
            // min stock amount
            editTextMinAmount.setText(NumUtil.trim(editProduct.getMinStockAmount()));
            // best before days
            editTextDays.setText(String.valueOf(editProduct.getDefaultBestBeforeDays()));
            // product group
            ProductGroup productGroup = getProductGroup(editProduct.getProductGroupId());
            if(productGroup != null) {
                textViewProductGroup.setText(productGroup.getName());
                selectedProductGroupId = productGroup.getId();
            }
            // quantity unit purchase
            QuantityUnit quantityUnitPurchase = getQuantityUnit(editProduct.getQuIdPurchase());
            if(quantityUnitPurchase != null) {
                textViewQUPurchase.setText(quantityUnitPurchase.getName());
                selectedQUPurchaseId = quantityUnitPurchase.getId();
            }
            // quantity unit stock
            QuantityUnit quantityUnitStock = getQuantityUnit(editProduct.getQuIdStock());
            if(quantityUnitStock != null) {
                textViewQUStock.setText(quantityUnitStock.getName());
                selectedQUStockId = quantityUnitStock.getId();
            }
            // quantity unit factor
            editTextQUFactor.setText(NumUtil.trim(editProduct.getQuFactorPurchaseToStock()));
        }
    }

    private void fillWithPresets() {
        int locationId = sharedPrefs.getInt(Constants.PREF.PRODUCT_PRESETS_LOCATION_ID, -1);
        Location location = getLocation(locationId);
        if(selectedLocationId == -1 && location != null) {
            textViewLocation.setText(location.getName());
            selectedLocationId = location.getId();
        }

        int productGroupId = sharedPrefs.getInt(
                Constants.PREF.PRODUCT_PRESETS_PRODUCT_GROUP_ID,
                -1
        );
        ProductGroup productGroup = getProductGroup(productGroupId);
        if(selectedProductGroupId == -1 && productGroup != null) {
            textViewProductGroup.setText(productGroup.getName());
            selectedProductGroupId = productGroup.getId();
        }

        int quantityUnitId = sharedPrefs.getInt(Constants.PREF.PRODUCT_PRESETS_QU_ID, -1);
        QuantityUnit quantityUnit = getQuantityUnit(quantityUnitId);
        if(selectedQUPurchaseId == -1 && quantityUnit != null) {
            textViewQUPurchase.setText(quantityUnit.getName());
            selectedQUPurchaseId = quantityUnit.getId();
        }
        if(selectedQUStockId == -1 && quantityUnit != null) {
            textViewQUStock.setText(quantityUnit.getName());
            selectedQUStockId = quantityUnit.getId();
        }

        editTextQUFactor.setText(String.valueOf(1));
    }

    private void fillWithCreateProductObject() {
        clearInputFocusAndErrors();

        if(createProductObj.getProductName() != null
                && !createProductObj.getProductName().isEmpty()
        ) {
            editTextName.setText(createProductObj.getProductName());
        } else if(createProductObj.getBarcodes() != null
                && !createProductObj.getBarcodes().isEmpty()
                && sharedPrefs.getBoolean(Constants.PREF.FOOD_FACTS, false)
        ) {
            // get product name from open food facts
            dlHelper.get(
                    OpenFoodFactsApi.getProduct(
                            Arrays.asList(createProductObj.getBarcodes().split(",")).get(0)
                    ),
                    response -> {
                        String language = Locale.getDefault().getLanguage();
                        String country = Locale.getDefault().getCountry();
                        String both = language + "_" + country;
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            JSONObject product = jsonObject.getJSONObject("product");
                            String name = product.optString("product_name_" + both);
                            if(name.isEmpty()) {
                                name = product.optString("product_name_" + language);
                            }
                            if(name.isEmpty()) {
                                name = product.optString("product_name");
                            }
                            editTextName.setText(name);
                            if(debug) Log.i(
                                    TAG,
                                    "fillWithCreateProductObject: OpenFoodFacts = " + name
                            );
                        } catch (JSONException e) {
                            if(debug) Log.e(TAG, "fillWithCreateProductObject: " + e);
                        }
                    },
                    error -> {},
                    OpenFoodFactsApi.getUserAgent(activity)
            );
        }

        if(createProductObj.getBarcodes() != null && !createProductObj.getBarcodes().isEmpty()) {
            List<String> barcodes = Arrays.asList(createProductObj.getBarcodes().split(","));
            linearLayoutBarcodeContainer.removeAllViews();
            for(int i = 0; i < barcodes.size(); i++) {
                InputChip inputChipBarcode = new InputChip(
                        activity,
                        barcodes.get(i).trim(),
                        false,
                        () -> { });
                linearLayoutBarcodeContainer.addView(inputChipBarcode);
            }
        }
        editTextDays.setText(createProductObj.getDefaultBestBeforeDays());
        if(NumUtil.isStringInt(createProductObj.getDefaultLocationId())) {
            selectLocation(Integer.parseInt(createProductObj.getDefaultLocationId()));
        }
    }

    private void clearInputFocusAndErrors() {
        activity.hideKeyboard();
        textInputName.clearFocus();
        textInputName.setErrorEnabled(false);
        autoCompleteTextViewParentProduct.clearFocus();
        textInputParentProduct.clearFocus();
        textInputParentProduct.setErrorEnabled(false);
        textInputBarcodes.clearFocus();
        textViewLocationLabel.setTextColor(getColor(R.color.on_background_secondary));
        textInputMinAmount.clearFocus();
        textInputMinAmount.setErrorEnabled(false);
        textInputDays.clearFocus();
        textInputDays.setErrorEnabled(false);
        textViewQUPurchaseLabel.setTextColor(getColor(R.color.on_background_secondary));
        textViewQUStockLabel.setTextColor(getColor(R.color.on_background_secondary));
        textInputQUFactor.clearFocus();
        textInputQUFactor.setErrorEnabled(false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == Activity.RESULT_OK && data != null) {
            if(requestCode == Constants.REQUEST.SCAN) {
                addBarcode(data.getStringExtra(Constants.EXTRA.SCAN_RESULT));
            } else if(requestCode == Constants.REQUEST.SCAN_PARENT_PRODUCT) {
                loadParentProductByBarcode(data.getStringExtra(Constants.EXTRA.SCAN_RESULT));
            }
        }
    }

    private void loadParentProductByBarcode(String barcode) {
        swipeRefreshLayout.setRefreshing(true);
        dlHelper.get(
                grocyApi.getStockProductByBarcode(barcode),
                response -> {
                    swipeRefreshLayout.setRefreshing(false);
                    ProductDetails productDetails = gson.fromJson(
                            response,
                            new TypeToken<ProductDetails>(){}.getType()
                    );
                    Product newParentProduct = productDetails.getProduct();
                    if(newParentProduct != null && editProduct != null) {
                        if (editProduct.getId() != newParentProduct.getId()) {
                            productParent = newParentProduct;
                            autoCompleteTextViewParentProduct.setText(productParent.getName());
                            textInputParentProduct.clearFocus();
                        } else {
                            activity.showMessage(
                                    Snackbar.make(
                                            activity.findViewById(R.id.frame_main_container),
                                            "Product can't be parent of itself",
                                            Snackbar.LENGTH_SHORT
                                    )
                            );
                        }
                    } else if(newParentProduct != null) {
                        productParent = newParentProduct;
                        autoCompleteTextViewParentProduct.setText(productParent.getName());
                        textInputParentProduct.clearFocus();
                    } else {
                        showErrorMessage();
                    }
                }, error -> {
                    NetworkResponse response = error.networkResponse;
                    if(response != null && response.statusCode == 400) {
                        activity.showMessage(
                                Snackbar.make(
                                        activity.findViewById(R.id.frame_main_container),
                                        "Product not found",
                                        Snackbar.LENGTH_SHORT
                                )
                        );
                    } else {
                        showErrorMessage();
                    }
                    swipeRefreshLayout.setRefreshing(false);
                }
        );
    }

    private void addInputAsBarcode() {
        addBarcode(editTextBarcodes.getText().toString());
        editTextBarcodes.setText(null);
        textInputBarcodes.clearFocus();
        activity.hideKeyboard();
    }

    private void addBarcode(String barcode) {
        barcode = barcode.trim();
        if(barcode.isEmpty()) return;
        for(int i = 0; i < linearLayoutBarcodeContainer.getChildCount(); i++) {
            InputChip inputChip = (InputChip) linearLayoutBarcodeContainer.getChildAt(i);
            if(inputChip.getText().equals(barcode)) {
                activity.showMessage(
                        Snackbar.make(
                                activity.findViewById(R.id.frame_main_container),
                                activity.getString(R.string.msg_barcode_duplicate),
                                Snackbar.LENGTH_SHORT
                        )
                );
                return;
            }
        }
        linearLayoutBarcodeContainer.addView(new InputChip(activity, barcode, true));
    }

    public void saveProduct() {
        if(isFormInvalid()) return;

        String productName = editTextName.getText().toString();

        JSONObject jsonObject = new JSONObject();
        try {
            // name
            jsonObject.put("name", productName);
            // parent product json null shit
            if(productParent != null) {
                if(String.valueOf(autoCompleteTextViewParentProduct.getText()).trim().isEmpty()) {
                    jsonObject.put("parent_product_id", JSONObject.NULL);
                } else {
                    jsonObject.put("parent_product_id", productParent.getId());
                }
            } else {
                jsonObject.put("parent_product_id", JSONObject.NULL);
            }
            // others
            jsonObject.put("description", productDescriptionHtml);
            jsonObject.put("barcode", getBarcodes());
            jsonObject.put("min_stock_amount", minAmount);
            if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
                jsonObject.put("default_best_before_days", bestBeforeDays);
            }
            jsonObject.put(
                    "product_group_id",
                    selectedProductGroupId != -1
                            ? selectedProductGroupId
                            : JSONObject.NULL
            );
            jsonObject.put("qu_id_purchase", selectedQUPurchaseId);
            jsonObject.put("qu_id_stock", selectedQUStockId);
            jsonObject.put("qu_factor_purchase_to_stock", quantityUnitFactor);

            if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)
                    || editProduct != null
            ) {
                jsonObject.put("location_id", selectedLocationId);
            } else {
                jsonObject.put("location_id", 1);
            }
        } catch (JSONException e) {
            if(debug) Log.e(TAG, "saveProduct: " + e);
        }

        if(editProduct != null) {
            dlHelper.put(
                    grocyApi.getObject(GrocyApi.ENTITY.PRODUCTS, editProduct.getId()),
                    jsonObject,
                    response -> {
                        Bundle bundle = new Bundle();
                        bundle.putString(Constants.ARGUMENT.TYPE, intendedAction);
                        bundle.putInt(Constants.ARGUMENT.PRODUCT_ID, editProduct.getId());
                        bundle.putString(Constants.ARGUMENT.PRODUCT_NAME, productName);
                        activity.dismissFragment(bundle);
                    },
                    error -> {
                        showErrorMessage();
                        if(debug) Log.e(TAG, "saveProduct: " + error);
                    }
            );
        } else {
            dlHelper.post(
                    grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS),
                    jsonObject,
                    response -> {
                        try {
                            Bundle bundle = new Bundle();
                            bundle.putString(Constants.ARGUMENT.TYPE, intendedAction);
                            bundle.putString(Constants.ARGUMENT.PRODUCT_NAME, productName);
                            bundle.putParcelable(  // to search for old name in batch items
                                    Constants.ARGUMENT.CREATE_PRODUCT_OBJECT,
                                    createProductObj
                            );
                            bundle.putInt(
                                    Constants.ARGUMENT.PRODUCT_ID,
                                    response.getInt("created_object_id")
                            );
                            activity.dismissFragment(bundle);
                        } catch (JSONException e) {
                            if(debug) Log.e(TAG, "saveProduct: " + e.toString());
                            showErrorMessage();
                        }
                    },
                    error -> {
                        showErrorMessage();
                        if(debug) Log.e(TAG, "saveProduct: " + error);
                    }
            );
        }
    }

    private String getBarcodes() {
        List<String> barcodes = new ArrayList<>();
        for(int i = 0; i < linearLayoutBarcodeContainer.getChildCount(); i++) {
            InputChip inputChip = (InputChip) linearLayoutBarcodeContainer.getChildAt(i);
            barcodes.add(inputChip.getText().trim());
        }
        return barcodes.isEmpty() ? "" : TextUtils.join(",", barcodes);
    }

    private boolean isFormInvalid() {
        clearInputFocusAndErrors();

        boolean isInvalid = false;

        String name = String.valueOf(editTextName.getText()).trim();
        if(name.isEmpty()) {
            textInputName.setError(activity.getString(R.string.error_empty));
            isInvalid = true;
        } else if(!productNames.isEmpty() && productNames.contains(name)) {
            textInputName.setError(activity.getString(R.string.error_duplicate));
            isInvalid = true;
        }

        String parentProduct = String.valueOf(autoCompleteTextViewParentProduct.getText()).trim();
        if(!parentProduct.isEmpty() && parentProduct.equals(name)) {
            textInputParentProduct.setError(activity.getString(R.string.error_parent));
            isInvalid = true;
        } else if(!parentProduct.isEmpty() && !productNames.contains(parentProduct)) {
            textInputParentProduct.setError(activity.getString(R.string.error_invalid_product));
            isInvalid = true;
        }

        if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
            ArrayList<Integer> locationIds = new ArrayList<>();
            for(Location location : locations) locationIds.add(location.getId());
            if(selectedLocationId == -1
                    || selectedLocationId > -1 && !locationIds.contains(selectedLocationId)
            ) {
                textViewLocationLabel.setTextColor(getColor(R.color.error));
                isInvalid = true;
            }
        }

        if(minAmount < 0) {
            textInputMinAmount.setError(activity.getString(R.string.error_invalid_amount));
            isInvalid = true;
        }

        if(bestBeforeDays < -1 && isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
            textInputDays.setError(activity.getString(R.string.error_invalid_best_before_days));
            isInvalid = true;
        }

        if(selectedQUPurchaseId == -1) {
            textViewQUPurchaseLabel.setTextColor(getColor(R.color.error));
            isInvalid = true;
        }

        if(selectedQUStockId == -1) {
            textViewQUStockLabel.setTextColor(getColor(R.color.error));
            isInvalid = true;
        }

        if(editTextQUFactor.getText().toString().trim().isEmpty()
                || !NumUtil.isStringInt(editTextQUFactor.getText().toString())
                || Integer.parseInt(editTextQUFactor.getText().toString()) < 1
        ) {
            textInputQUFactor.setError(activity.getString(R.string.error_invalid_factor));
            isInvalid = true;
        }
        return isInvalid;
    }

    private void resetAll() {
        if(editProduct != null) return;
        clearInputFocusAndErrors();

        if(createProductObj != null) {
            editTextName.setText(createProductObj.getProductName());
        } else {
            editTextName.setText(null);
        }

        if(createProductObj != null && createProductObj.getBarcodes() != null) {
            List<String> barcodes = Arrays.asList(createProductObj.getBarcodes().split(","));
            linearLayoutBarcodeContainer.removeAllViews();
            for(int i = 0; i < barcodes.size(); i++) {
                InputChip inputChipBarcode = new InputChip(
                        activity,
                        barcodes.get(i).trim(),
                        false,
                        () -> { });
                linearLayoutBarcodeContainer.addView(inputChipBarcode);
            }
        } else {
            if(linearLayoutBarcodeContainer.getChildCount() > 0) {
                for(int i = 0; i < linearLayoutBarcodeContainer.getChildCount(); i++) {
                    InputChip inputChip = (InputChip)linearLayoutBarcodeContainer.getChildAt(i);
                    inputChip.close();
                }
            }
        }

        autoCompleteTextViewParentProduct.setText(null);
        editTextBarcodes.setText(null);


        textViewLocation.setText(R.string.subtitle_none_selected);
        selectedLocationId = -1;

        if(createProductObj != null && NumUtil.isStringInt(createProductObj.getDefaultLocationId())) {
            selectLocation(Integer.parseInt(createProductObj.getDefaultLocationId()));
        }

        editTextMinAmount.setText(String.valueOf(0));

        if(createProductObj != null && createProductObj.getDefaultBestBeforeDays() != null) {
            editTextDays.setText(createProductObj.getDefaultBestBeforeDays());
        } else {
            editTextDays.setText(String.valueOf(0));
        }

        textViewProductGroup.setText(R.string.subtitle_none_selected);
        selectedProductGroupId = -1;

        textViewQUPurchase.setText(R.string.subtitle_none_selected);
        selectedQUPurchaseId = -1;

        textViewQUStock.setText(R.string.subtitle_none_selected);
        selectedQUStockId = -1;

        editTextQUFactor.setText(String.valueOf(1));
    }

    private void checkForStock(Product product) {
        dlHelper.get(
                grocyApi.getStockProductDetails(product.getId()),
                response -> {
                    ProductDetails productDetails = new Gson().fromJson(
                            response,
                            new TypeToken<ProductDetails>(){}.getType()
                    );
                    if(productDetails != null && productDetails.getStockAmount() == 0) {
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(Constants.ARGUMENT.PRODUCT, product);
                        bundle.putString(Constants.ARGUMENT.TYPE, Constants.ARGUMENT.PRODUCT);
                        activity.showBottomSheet(
                                new MasterDeleteBottomSheetDialogFragment(),
                                bundle
                        );
                    } else {
                        activity.showMessage(
                                Snackbar.make(
                                        activity.findViewById(R.id.frame_main_container),
                                        activity.getString(R.string.msg_master_delete_stock),
                                        Snackbar.LENGTH_LONG
                                )
                        );
                    }
                },
                error -> { }
        );
    }

    public void deleteProduct(Product product) {
        dlHelper.delete(
                grocyApi.getObject(GrocyApi.ENTITY.PRODUCTS, product.getId()),
                response -> {
                    if(intendedAction.equals(Constants.ACTION.EDIT_THEN_PURCHASE_BATCH)) {
                        Bundle bundle = new Bundle();
                        bundle.putString(
                                Constants.ARGUMENT.TYPE,
                                Constants.ACTION.DELETE_THEN_PURCHASE_BATCH
                        );
                        bundle.putInt(Constants.ARGUMENT.PRODUCT_ID, product.getId());
                        activity.dismissFragment(bundle);
                    } else {
                        activity.dismissFragment();
                    }
                },
                error -> showErrorMessage()
        );
    }

    private void showErrorMessage() {
        activity.showMessage(
                Snackbar.make(
                        activity.findViewById(R.id.frame_main_container),
                        activity.getString(R.string.error_undefined),
                        Snackbar.LENGTH_SHORT
                )
        );
    }

    public String getIntendedAction() {
        return intendedAction;
    }

    private void hideDisabledFeatures() {
        if(!isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
            activity.findViewById(
                    R.id.linear_master_product_simple_location
            ).setVisibility(View.GONE);
        }
        if(!isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
            activity.findViewById(R.id.linear_master_product_simple_days).setVisibility(View.GONE);
        }
    }

    public void setUpBottomMenu() {
        MenuItem delete = activity.getBottomMenu().findItem(R.id.action_delete);
        if(delete != null) {
            delete.setOnMenuItemClickListener(item -> {
                IconUtil.start(item);
                checkForStock(editProduct);
                return true;
            });
            delete.setVisible(editProduct != null);
        }
    }

    private int getColor(@ColorRes int color) {
        return ContextCompat.getColor(activity, color);
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
