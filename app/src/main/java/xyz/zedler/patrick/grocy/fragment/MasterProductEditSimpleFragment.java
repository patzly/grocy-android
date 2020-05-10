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
import android.graphics.drawable.Animatable;
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
import androidx.annotation.IdRes;
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

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.ScanInputActivity;
import xyz.zedler.patrick.grocy.adapter.MatchArrayAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LocationsBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductGroupsBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.QuantityUnitsBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.model.CreateProduct;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.view.InputChip;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class MasterProductEditSimpleFragment extends Fragment {

    private final static String TAG = Constants.UI.MASTER_PRODUCT_SIMPLE;
    private final static boolean DEBUG = false;

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private WebRequest request;
    private ArrayAdapter<String> adapterProducts;

    private ArrayList<Product> products = new ArrayList<>();
    private ArrayList<Location> locations = new ArrayList<>();
    private ArrayList<ProductGroup> productGroups = new ArrayList<>();
    private ArrayList<QuantityUnit> quantityUnits = new ArrayList<>();
    private ArrayList<String> productNames = new ArrayList<>();

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
            textInputDays;
    private EditText editTextName, editTextBarcodes, editTextMinAmount, editTextDays;
    private TextView
            textViewLocation,
            textViewLocationLabel,
            textViewProductGroup,
            textViewQuantityUnit,
            textViewQuantityUnitLabel;
    private ImageView imageViewName, imageViewMinAmount, imageViewDays;
    private int selectedLocationId = -1, selectedQuantityUnitId = -1, selectedProductGroupId = -1;
    private String intendedAction;
    private double minAmount;
    private int bestBeforeDays;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_master_product_edit_simple,
                container,
                false
        );
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

        activity.findViewById(R.id.frame_master_product_edit_simple_cancel).setOnClickListener(
                v -> activity.onBackPressed()
        );

        // swipe refresh
        swipeRefreshLayout = activity.findViewById(R.id.swipe_master_product_edit_simple);
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        swipeRefreshLayout.setOnRefreshListener(this::refresh);

        // name
        textInputName = activity.findViewById(R.id.text_input_master_product_edit_simple_name);
        imageViewName = activity.findViewById(R.id.image_master_product_edit_simple_name);
        editTextName = textInputName.getEditText();
        assert editTextName != null;
        editTextName.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) startAnimatedIcon(imageViewName);
        });

        // parent product
        textInputParentProduct = activity.findViewById(
                R.id.text_input_master_product_edit_simple_parent_product
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
            if(hasFocus) {
                // try again to download products
                if(productNames.isEmpty()) downloadProducts();
            }
        });
        autoCompleteTextViewParentProduct.setOnItemClickListener(
                (parent, view, position, id) -> productParent = getProductFromName(
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

        // barcodes

        textInputBarcodes = activity.findViewById(
                R.id.text_input_master_product_edit_simple_barcodes
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
                R.id.linear_master_product_edit_simple_barcode_container
        );

        // location
        activity.findViewById(
                R.id.linear_master_product_edit_simple_location
        ).setOnClickListener(v -> {
            startAnimatedIcon(R.id.image_master_product_edit_simple_location);
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
        textViewLocation = activity.findViewById(R.id.text_master_product_edit_simple_location);
        textViewLocationLabel = activity.findViewById(
                R.id.text_master_product_edit_simple_location_label
        );

        // min stock amount
        textInputMinAmount = activity.findViewById(
                R.id.text_input_master_product_edit_simple_amount
        );
        imageViewMinAmount = activity.findViewById(R.id.image_master_product_edit_simple_amount);
        editTextMinAmount = textInputMinAmount.getEditText();
        assert editTextMinAmount != null;
        editTextMinAmount.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if(!input.equals("")) {
                    minAmount = Double.parseDouble(input);
                } else {
                    minAmount = 0;
                }
            }
        });
        editTextMinAmount.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) startAnimatedIcon(imageViewMinAmount);
        });
        editTextMinAmount.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                editTextMinAmount.clearFocus();
                activity.hideKeyboard();
                return true;
            } return false;
        });

        activity.findViewById(
                R.id.button_master_product_edit_simple_amount_more
        ).setOnClickListener(v -> {
            startAnimatedIcon(imageViewMinAmount);
            if(editTextMinAmount.getText().toString().equals("")) {
                editTextMinAmount.setText(String.valueOf(0));
            } else {
                double amountNew = Double.parseDouble(editTextMinAmount.getText().toString()) + 1;
                editTextMinAmount.setText(NumUtil.trim(amountNew));
            }
        });

        activity.findViewById(
                R.id.button_master_product_edit_simple_amount_less
        ).setOnClickListener(v -> {
            if(!editTextMinAmount.getText().toString().equals("")) {
                startAnimatedIcon(imageViewMinAmount);
                double amountNew = Double.parseDouble(editTextMinAmount.getText().toString()) - 1;
                if(amountNew >= 0) {
                    editTextMinAmount.setText(NumUtil.trim(amountNew));
                }
            }
        });

        // best before days
        textInputDays = activity.findViewById(R.id.text_input_master_product_edit_simple_days);
        imageViewDays = activity.findViewById(R.id.image_master_product_edit_simple_days);
        editTextDays = textInputDays.getEditText();
        assert editTextDays != null;
        editTextDays.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                if(!input.equals("")) {
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
                startAnimatedIcon(imageViewDays);
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
                R.id.button_master_product_edit_simple_days_more
        ).setOnClickListener(v -> {
            startAnimatedIcon(imageViewDays);
            if(editTextDays.getText().toString().equals("")) {
                editTextDays.setText(String.valueOf(0));
            } else {
                int daysNew = Integer.parseInt(editTextDays.getText().toString()) + 1;
                editTextDays.setText(String.valueOf(daysNew));
            }
        });

        activity.findViewById(
                R.id.button_master_product_edit_simple_days_less
        ).setOnClickListener(v -> {
            if(!editTextDays.getText().toString().equals("")) {
                startAnimatedIcon(imageViewDays);
                int daysNew = Integer.parseInt(editTextDays.getText().toString()) - 1;
                if(daysNew >= -1) {
                    editTextDays.setText(String.valueOf(daysNew));
                }
            }
        });

        // product group
        activity.findViewById(
                R.id.linear_master_product_edit_simple_product_group
        ).setOnClickListener(v -> {
            startAnimatedIcon(R.id.image_master_product_edit_simple_product_group);
            if(!productGroups.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList(Constants.ARGUMENT.PRODUCT_GROUPS, productGroups);
                bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedProductGroupId);
                activity.showBottomSheet(new ProductGroupsBottomSheetDialogFragment(), bundle);
            }
        });
        textViewProductGroup = activity.findViewById(
                R.id.text_master_product_edit_simple_product_group
        );

        // quantity unit
        activity.findViewById(
                R.id.linear_master_product_edit_simple_quantity_unit
        ).setOnClickListener(v -> {
            startAnimatedIcon(R.id.image_master_product_edit_simple_quantity_unit);
            if(!quantityUnits.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList(Constants.ARGUMENT.QUANTITY_UNITS, quantityUnits);
                bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedQuantityUnitId);
                activity.showBottomSheet(new QuantityUnitsBottomSheetDialogFragment(), bundle);
            }
        });
        textViewQuantityUnit = activity.findViewById(
                R.id.text_master_product_edit_simple_quantity_unit
        );
        textViewQuantityUnitLabel = activity.findViewById(
                R.id.text_master_product_edit_simple_quantity_unit_label
        );

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
        } else if(intendedAction.equals(Constants.ACTION.EDIT)
                || intendedAction.equals(Constants.ACTION.EDIT_THEN_PURCHASE_BATCH)) {
            editProduct = bundle.getParcelable(Constants.ARGUMENT.PRODUCT);
        } else if(intendedAction.equals(Constants.ACTION.CREATE_THEN_PURCHASE)) {
            createProductObj = bundle.getParcelable(Constants.ARGUMENT.CREATE_PRODUCT_OBJECT);
        } else if(intendedAction.equals(Constants.ACTION.CREATE_THEN_PURCHASE_BATCH)) {
            createProductObj = bundle.getParcelable(Constants.ARGUMENT.CREATE_PRODUCT_OBJECT);
        }

        // START

        load();

        // UPDATE UI

        activity.updateUI(toString(), TAG);
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
    }

    private void download() {
        swipeRefreshLayout.setRefreshing(true);

        downloadProducts();
        downloadLocations();
        downloadProductGroups();
        downloadQuantityUnits();
    }

    private void downloadProducts() {
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
                    autoCompleteTextViewParentProduct.setAdapter(adapterProducts);
                },
                this::onDownloadError,
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
                            new TypeToken<ArrayList<Location>>(){}.getType()
                    );
                    SortUtil.sortLocationsByName(locations, true);
                },
                this::onDownloadError,
                this::onQueueEmpty
        );
    }

    private void downloadProductGroups() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_GROUPS),
                TAG,
                response -> {
                    productGroups = gson.fromJson(
                            response,
                            new TypeToken<ArrayList<ProductGroup>>(){}.getType()
                    );
                    SortUtil.sortProductGroupsByName(productGroups, true);
                    // Insert NONE as first element
                    productGroups.add(
                            0,
                            new ProductGroup(-1, activity.getString(R.string.subtitle_none))
                    );
                },
                this::onDownloadError,
                this::onQueueEmpty
        );
    }

    private void downloadQuantityUnits() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNITS),
                TAG,
                response -> {
                    quantityUnits = gson.fromJson(
                            response,
                            new TypeToken<ArrayList<QuantityUnit>>(){}.getType()
                    );
                    SortUtil.sortQuantityUnitsByName(quantityUnits, true);
                },
                this::onDownloadError,
                this::onQueueEmpty
        );
    }

    private void onDownloadError(VolleyError error) {
        request.cancelAll(TAG);
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

    @SuppressLint("LongLogTag")
    private void onQueueEmpty() {
        swipeRefreshLayout.setRefreshing(false);


        if(editProduct != null
                || intendedAction.equals(Constants.ACTION.CREATE_THEN_PURCHASE)
                || intendedAction.equals(Constants.ACTION.CREATE_THEN_PURCHASE_BATCH)
        ) {
            switch (intendedAction) {
                case Constants.ACTION.EDIT:
                case Constants.ACTION.EDIT_THEN_PURCHASE_BATCH:
                    fillWithEditReferences();
                    break;
                case Constants.ACTION.CREATE_THEN_PURCHASE:
                case Constants.ACTION.CREATE_THEN_PURCHASE_BATCH:
                    fillWithCreateProductObject();
                    fillWithPresets();
                    isFormInvalid();
                    break;
            }
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

    public void selectLocation(int selectedId) {
        selectedLocationId = selectedId;
        String locationText = null;
        if(locations.isEmpty()) {
            locationText = "None";
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
        if(locationId == -1) return null;
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
            productGroupText = activity.getString(R.string.subtitle_none);
        } else {
            ProductGroup productGroup = getProductGroup(selectedId);
            if(productGroup != null) {
                productGroupText = productGroup.getName();
            } else {
                productGroupText = activity.getString(R.string.subtitle_none);
            }
        }
        textViewProductGroup.setText(productGroupText);
    }

    private ProductGroup getProductGroup(String productGroupId) {
        if(productGroupId == null || productGroupId.equals("")) return null;
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

    public void selectQuantityUnit(int selectedId) {
        selectedQuantityUnitId = selectedId;
        String quantityUnitText = null;
        if(quantityUnits.isEmpty()) {
            quantityUnitText = "None";
        } else {
            QuantityUnit quantityUnit = getQuantityUnit(selectedId);
            if(quantityUnit != null) {
                quantityUnitText = quantityUnit.getName();
            }
            textViewQuantityUnitLabel.setTextColor(getColor(R.color.on_background_secondary));
        }
        textViewQuantityUnit.setText(quantityUnitText);
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
            if(editProduct.getParentProductId() != null) {
                parentProduct = getProduct(Integer.parseInt(editProduct.getParentProductId()));
            }
            if(parentProduct != null) {
                autoCompleteTextViewParentProduct.setText(parentProduct.getName());
            } else {
                autoCompleteTextViewParentProduct.setText(null);
            }
            // barcodes
            if(editProduct.getBarcode() != null && !editProduct.getBarcode().trim().equals("")) {
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
                selectedLocationId = location.getId();
            }
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
            // quantity unit
            QuantityUnit quantityUnit = getQuantityUnit(editProduct.getQuIdStock());
            if(quantityUnit != null) {
                textViewQuantityUnit.setText(quantityUnit.getName());
                selectedQuantityUnitId = quantityUnit.getId();
            }
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
        if(selectedQuantityUnitId == -1 && quantityUnit != null) {
            textViewQuantityUnit.setText(quantityUnit.getName());
            selectedQuantityUnitId = quantityUnit.getId();
        }
    }

    private void fillWithCreateProductObject() {
        clearInputFocusAndErrors();

        editTextName.setText(createProductObj.getProductName());

        if(createProductObj.getBarcodes() != null) {
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
        if(createProductObj.getDefaultLocationId() != null) {
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
        textViewQuantityUnitLabel.setTextColor(getColor(R.color.on_background_secondary));
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
        request.get(
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
                            activity.showSnackbar(
                                    Snackbar.make(
                                            activity.findViewById(R.id.linear_container_main),
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
                        activity.showSnackbar(
                                Snackbar.make(
                                        activity.findViewById(R.id.linear_container_main),
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
        if(barcode.equals("")) return;
        for(int i = 0; i < linearLayoutBarcodeContainer.getChildCount(); i++) {
            InputChip inputChip = (InputChip) linearLayoutBarcodeContainer.getChildAt(i);
            if(inputChip.getText().equals(barcode)) {
                activity.showSnackbar(
                        Snackbar.make(
                                activity.findViewById(R.id.linear_container_main),
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
                if(String.valueOf(autoCompleteTextViewParentProduct.getText()).trim().equals("")) {
                    jsonObject.put("parent_product_id", JSONObject.NULL);
                } else {
                    jsonObject.put("parent_product_id", productParent.getId());
                }
            } else {
                jsonObject.put("parent_product_id", JSONObject.NULL);
            }
            // others
            jsonObject.put("barcode", getBarcodes());
            jsonObject.put("location_id", selectedLocationId);
            jsonObject.put("min_stock_amount", minAmount);
            jsonObject.put("default_best_before_days", bestBeforeDays);
            jsonObject.put(
                    "product_group_id",
                    selectedProductGroupId != -1
                            ? selectedProductGroupId
                            : JSONObject.NULL
            );
            jsonObject.put("qu_id_purchase", selectedQuantityUnitId);
            jsonObject.put("qu_id_stock", selectedQuantityUnitId);
            jsonObject.put("qu_factor_purchase_to_stock", 1);

        } catch (JSONException e) {
            if(DEBUG) Log.e(TAG, "saveProduct: " + e);
        }
        if(editProduct != null) {
            request.put(
                    grocyApi.getObject(GrocyApi.ENTITY.PRODUCTS, editProduct.getId()),
                    jsonObject,
                    response -> {
                        if(intendedAction.equals(Constants.ACTION.EDIT_THEN_PURCHASE_BATCH)) {
                            Bundle bundle = new Bundle();
                            bundle.putString(Constants.ARGUMENT.TYPE, intendedAction);
                            bundle.putInt(Constants.ARGUMENT.PRODUCT_ID, editProduct.getId());
                            bundle.putString(Constants.ARGUMENT.PRODUCT_NAME, productName);
                            activity.dismissFragment(bundle);
                        } else {
                            activity.dismissFragment();
                        }
                    },
                    error -> {
                        showErrorMessage();
                        Log.e(TAG, "saveProduct: " + error);
                    }
            );
        } else {
            request.post(
                    grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS),
                    jsonObject,
                    response -> {
                        if(intendedAction.equals(Constants.ACTION.CREATE_THEN_PURCHASE)
                                || intendedAction.equals(
                                        Constants.ACTION.CREATE_THEN_PURCHASE_BATCH)
                        ) {
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
                                Log.e(TAG, "saveProduct: " + e.toString());
                                showErrorMessage();
                            }
                        } else {
                            activity.dismissFragment();
                        }
                    },
                    error -> {
                        showErrorMessage();
                        Log.e(TAG, "saveProduct: " + error);
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
        if(name.equals("")) {
            textInputName.setError(activity.getString(R.string.error_empty));
            isInvalid = true;
        } else if(!productNames.isEmpty() && productNames.contains(name)) {
            textInputName.setError(activity.getString(R.string.error_duplicate));
            isInvalid = true;
        }

        String parentProduct = String.valueOf(autoCompleteTextViewParentProduct.getText()).trim();
        if(!parentProduct.equals("") && parentProduct.equals(name)) {
            textInputParentProduct.setError(activity.getString(R.string.error_parent));
            isInvalid = true;
        } else if(!parentProduct.equals("") && !productNames.contains(parentProduct)) {
            textInputParentProduct.setError(activity.getString(R.string.error_invalid_product));
            isInvalid = true;
        }

        if(selectedLocationId == -1) {
            textViewLocationLabel.setTextColor(getColor(R.color.error));
            isInvalid = true;
        }

        if(minAmount < 0) {
            textInputMinAmount.setError(activity.getString(R.string.error_invalid_amount));
            isInvalid = true;
        }

        if(bestBeforeDays < -1) {
            textInputDays.setError(activity.getString(R.string.error_invalid_best_before_days));
            isInvalid = true;
        }

        if(selectedQuantityUnitId == -1) {
            textViewQuantityUnitLabel.setTextColor(getColor(R.color.error));
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


        textViewLocation.setText(R.string.subtitle_none);
        selectedLocationId = -1;

        if(createProductObj != null && createProductObj.getDefaultLocationId() != null) {
            selectLocation(Integer.parseInt(createProductObj.getDefaultLocationId()));
        }

        editTextMinAmount.setText(String.valueOf(0));

        if(createProductObj != null && createProductObj.getDefaultBestBeforeDays() != null) {
            editTextDays.setText(createProductObj.getDefaultBestBeforeDays());
        } else {
            editTextDays.setText(String.valueOf(0));
        }

        textViewProductGroup.setText(R.string.subtitle_none);
        selectedProductGroupId = -1;

        textViewQuantityUnit.setText(R.string.subtitle_none);
        selectedQuantityUnitId = -1;
    }

    private void checkForStock(Product product) {
        request.get(
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
                        activity.showSnackbar(
                                Snackbar.make(
                                        activity.findViewById(R.id.linear_container_main),
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
        request.delete(
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
        activity.showSnackbar(
                Snackbar.make(
                        activity.findViewById(R.id.linear_container_main),
                        activity.getString(R.string.msg_error),
                        Snackbar.LENGTH_SHORT
                )
        );
    }

    public String getIntendedAction() {
        return intendedAction;
    }

    public void setUpBottomMenu() {
        MenuItem delete = activity.getBottomMenu().findItem(R.id.action_delete);
        if(delete != null) {
            delete.setOnMenuItemClickListener(item -> {
                activity.startAnimatedIcon(item);
                checkForStock(editProduct);
                return true;
            });
            delete.setVisible(editProduct != null);
        }
    }

    private void startAnimatedIcon(@IdRes int viewId) {
        startAnimatedIcon(activity.findViewById(viewId));
    }

    @SuppressLint("LongLogTag")
    private void startAnimatedIcon(View view) {
        try {
            ((Animatable) ((ImageView) view).getDrawable()).start();
        } catch (ClassCastException cla) {
            Log.e(TAG, "startAnimatedIcon(Drawable) requires AVD!");
        }
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
