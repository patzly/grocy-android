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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

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
import xyz.zedler.patrick.grocy.databinding.FragmentShoppingListItemEditBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputBarcodeBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputNameBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListsBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.TextUtil;
import xyz.zedler.patrick.grocy.view.InputChip;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class ShoppingListItemEditFragment extends Fragment {

    private final static String TAG = Constants.UI.SHOPPING_LIST_ITEM_EDIT;
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private WebRequest request;
    private ArrayAdapter<String> adapterProducts;
    private ProductDetails productDetails;
    private Bundle startupBundle;

    private ArrayList<Product> products = new ArrayList<>();
    private ArrayList<ShoppingList> shoppingLists = new ArrayList<>();
    private ArrayList<String> productNames = new ArrayList<>();

    private FragmentShoppingListItemEditBinding binding;
    private MaterialAutoCompleteTextView autoCompleteTextViewProduct;
    private LinearLayout linearLayoutBarcodesContainer;
    private TextInputLayout textInputProduct, textInputAmount;
    private EditText editTextAmount, editTextNote;
    private ImageView imageViewAmount;
    private double amount;
    private boolean nameAutoFilled;
    private int selectedShoppingListId = -1;
    private Product selectedProduct;
    private String action;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentShoppingListItemEditBinding.inflate(
                inflater,
                container,
                false
        );
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (MainActivity) getActivity();
        assert activity != null;

        startupBundle = getArguments();
        if(startupBundle != null) {
            action = startupBundle.getString(Constants.ARGUMENT.TYPE);
            if(action == null) action = Constants.ACTION.CREATE;
        }

        // GET PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        // WEB REQUESTS

        request = new WebRequest(activity.getRequestQueue());
        grocyApi = activity.getGrocy();

        // INITIALIZE VIEWS

        binding.frameShoppingListItemEditBack.setOnClickListener(v -> activity.onBackPressed());

        // title

        if(action.equals(Constants.ACTION.EDIT)) {
            binding.textShoppingListItemEditTitle.setText("Edit entry");
        } else {
            binding.textShoppingListItemEditTitle.setText("Create new entry");
        }

        // swipe refresh

        binding.swipeShoppingListItemEdit.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        binding.swipeShoppingListItemEdit.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        binding.swipeShoppingListItemEdit.setOnRefreshListener(this::refresh);

        // shopping list

        binding.linearShoppingListItemEditShoppingList.setOnClickListener(v -> {
            if(shoppingLists.isEmpty()) {
                showErrorMessage();
                return;
            }
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(Constants.ARGUMENT.SHOPPING_LISTS, shoppingLists);
            bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedShoppingListId);
            activity.showBottomSheet(new ShoppingListsBottomSheetDialogFragment(), bundle);
        });

        // product

        textInputProduct = binding.textInputShoppingListItemEditProduct;
        textInputProduct.setErrorIconDrawable(null);
        textInputProduct.setEndIconOnClickListener(v -> startActivityForResult(
                new Intent(activity, ScanInputActivity.class),
                Constants.REQUEST.SCAN
        ));
        autoCompleteTextViewProduct = (MaterialAutoCompleteTextView) textInputProduct.getEditText();
        assert autoCompleteTextViewProduct != null;
        autoCompleteTextViewProduct.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) {
                IconUtil.start(binding.imageShoppingListItemEditProduct);
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
                        if(!productNames.isEmpty() && !productNames.contains(input)
                                && !input.isEmpty()
                                && !nameAutoFilled
                        ) {
                            Bundle bundle = new Bundle();
                            bundle.putString(
                                    Constants.ARGUMENT.TYPE,
                                    Constants.ACTION.CREATE_THEN_SHOPPING_LIST_ITEM
                            );
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

        linearLayoutBarcodesContainer = binding.linearShoppingListItemEditBarcodeContainer;

        // amount

        textInputAmount = binding.textInputShoppingListItemEditAmount;
        imageViewAmount = binding.imageShoppingListItemEditAmount;
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

        binding.buttonShoppingListItemEditAmountMore.setOnClickListener(v -> {
            IconUtil.start(binding.imageShoppingListItemEditAmount);
            if(editTextAmount.getText().toString().isEmpty()) {
                editTextAmount.setText(String.valueOf(1));
            } else {
                double amountNew = Double.parseDouble(editTextAmount.getText().toString()) + 1;
                editTextAmount.setText(NumUtil.trim(amountNew));
            }
        });

        binding.buttonShoppingListItemEditAmountLess.setOnClickListener(v -> {
            if(!editTextAmount.getText().toString().isEmpty()) {
                IconUtil.start(binding.imageShoppingListItemEditAmount);
                double amountNew = Double.parseDouble(editTextAmount.getText().toString()) - 1;
                if(amountNew >= 1) {
                    editTextAmount.setText(NumUtil.trim(amountNew));
                }
            }
        });

        // note

        editTextNote = binding.textInputShoppingListItemEditNote.getEditText();

        // START

        refresh();

        // UPDATE UI

        activity.updateUI(
                Constants.UI.SHOPPING_LIST_ITEM_EDIT,
                getArguments() == null || getArguments().getBoolean(
                        Constants.ARGUMENT.ANIMATED, true
                ),
                TAG
        );
    }

    private void refresh() {
        if(activity.isOnline()) {
            download();
        } else {
            binding.swipeShoppingListItemEdit.setRefreshing(false);
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
        binding.swipeShoppingListItemEdit.setRefreshing(true);
        downloadProductNames();
        downloadShoppingLists();
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

    private void downloadShoppingLists() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LISTS),
                TAG,
                response -> {
                    shoppingLists = gson.fromJson(
                            response,
                            new TypeToken<List<ShoppingList>>(){}.getType()
                    );
                    if(DEBUG) Log.i(
                            TAG,
                            "downloadShoppingLists: shoppingLists = " + shoppingLists
                    );
                },
                this::onError,
                this::onQueueEmpty
        );
    }

    private void onError(VolleyError error) {
        Log.e(TAG, "onError: VolleyError: " + error);
        request.cancelAll(TAG);
        binding.swipeShoppingListItemEdit.setRefreshing(false);
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
        binding.swipeShoppingListItemEdit.setRefreshing(false);

        String action = null;
        if(startupBundle != null) {
            action = startupBundle.getString(Constants.ARGUMENT.TYPE);
        }
        if(action != null && action.equals(Constants.ACTION.EDIT)) {
            ShoppingListItem shoppingListItem = startupBundle.getParcelable(
                    Constants.ARGUMENT.SHOPPING_LIST_ITEM
            );
            if(shoppingListItem == null) return;
            if(shoppingListItem.getProduct() != null) {
                selectedProduct = shoppingListItem.getProduct();
                autoCompleteTextViewProduct.setText(
                        selectedProduct.getName()
                );
            }
            editTextAmount.setText(NumUtil.trim(shoppingListItem.getAmount()));
            selectShoppingList(shoppingListItem.getShoppingListId());
            editTextNote.setText(TextUtil.getFromHtml(shoppingListItem.getNote()));
        } else if(action != null && action.equals(Constants.ACTION.CREATE)) {
            if(shoppingLists.size() >= 1) {
                selectShoppingList(shoppingLists.get(0).getId());
            } else {
                selectShoppingList(-1);
            }
        } else if(action != null && action.equals(Constants.ACTION.CREATE_FROM_STOCK)) {
            ProductDetails productDetails = startupBundle.getParcelable(
                    Constants.ARGUMENT.PRODUCT_DETAILS
            );
            if(productDetails == null) return;
            if(shoppingLists.size() >= 1) {
                selectShoppingList(shoppingLists.get(0).getId());
            } else {
                selectShoppingList(-1);
            }
        }
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
        nameAutoFilled = true;

        clearInputFocus();

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

    private void loadProductDetailsByBarcode(String barcode) {
        binding.swipeShoppingListItemEdit.setRefreshing(true);
        request.get(
                grocyApi.getStockProductByBarcode(barcode),
                response -> {
                    binding.swipeShoppingListItemEdit.setRefreshing(false);
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
                    binding.swipeShoppingListItemEdit.setRefreshing(false);
                }
        );
    }

    public void saveItem() {
        if(isFormIncomplete()) return;

        editProductBarcodes();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("shopping_list_id", selectedShoppingListId);
            jsonObject.put("amount", editTextAmount.getText().toString().trim());
            Product product = getProductFromName(
                    autoCompleteTextViewProduct.getText().toString().trim()
            );
            if(product != null) {
                jsonObject.put("product_id", product.getId());
            } else {
                jsonObject.put("product_id", "");
            }
            jsonObject.put("note", editTextNote.getText().toString().trim());
        } catch (JSONException e) {
            if(DEBUG) Log.e(TAG, "saveShoppingListItem: " + e);
        }
        if(action.equals(Constants.ACTION.EDIT)) {
            ShoppingListItem shoppingListItem = startupBundle.getParcelable(
                    Constants.ARGUMENT.SHOPPING_LIST_ITEM
            );
            assert shoppingListItem != null;
            request.put(
                    grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, shoppingListItem.getId()),
                    jsonObject,
                    response -> {
                        editProductBarcodes(); // ADD BARCODES TO PRODUCT
                        activity.dismissFragment();
                    },
                    error -> {
                        showErrorMessage();
                        Log.e(TAG, "saveShoppingListItem: " + error);
                    }
            );
        } else {
            request.post(
                    grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LIST),
                    jsonObject,
                    response -> {
                        editProductBarcodes(); // ADD BARCODES TO PRODUCT
                        activity.dismissFragment();
                    },
                    error -> {
                        showErrorMessage();
                        Log.e(TAG, "saveShoppingListItem: " + error);
                    }
            );
        }
    }

    private boolean isFormIncomplete() {
        String input = autoCompleteTextViewProduct.getText().toString().trim();
        if(linearLayoutBarcodesContainer.getChildCount() > 0 && input.isEmpty()) {
            textInputProduct.setError(activity.getString(R.string.error_empty));
            return true;
        } else {
            textInputProduct.setErrorEnabled(false);
        }
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
        } else return !isAmountValid();
    }

    private void editProductBarcodes() {
        String input = autoCompleteTextViewProduct.getText().toString().trim();
        if(input.isEmpty()) return;
        Product product = getProductFromName(input);
        if(product == null) return;
        if(linearLayoutBarcodesContainer.getChildCount() == 0) return;

        ArrayList<String> barcodes;
        if(product.getBarcode() == null || product.getBarcode().isEmpty()) {
            barcodes = new ArrayList<>();
        } else {
            barcodes = new ArrayList<>(
                    Arrays.asList(
                            product.getBarcode().split(",")
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
                grocyApi.getObject(GrocyApi.ENTITY.PRODUCTS, product.getId()),
                body,
                response -> { },
                error -> {
                    if(DEBUG) Log.i(TAG, "editProductBarcodes: " + error);
                }
        );
    }

    private Product getProductFromName(String name) {
        if(name != null && !name.isEmpty()) {
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

    public void setProductName(String productName) {  // TODO: Does not work (after creating product)
        autoCompleteTextViewProduct.setText(productName);
    }

    public void selectShoppingList(int selectedId) {
        this.selectedShoppingListId = selectedId;
        TextView textView = binding.textShoppingListItemEditShoppingList;
        ShoppingList shoppingList = getShoppingList(selectedId);
        if(shoppingList != null) {
            textView.setText(shoppingList.getName());
        } else {
            textView.setText(getString(R.string.subtitle_none));
        }
    }

    private ShoppingList getShoppingList(int shoppingListId) {
        if(shoppingListId == -1) return null;
        for(ShoppingList shoppingList : shoppingLists) {
            if(shoppingList.getId() == shoppingListId) {
                return shoppingList;
            }
        } return null;
    }

    public void setUpBottomMenu() {
        MenuItem menuItemDelete;
        menuItemDelete = activity.getBottomMenu().findItem(R.id.action_delete);
        if(menuItemDelete != null) {
            // hide action if type create
            // TODO: ugly flickering
            menuItemDelete.setVisible(action.equals(Constants.ACTION.EDIT));
            if(action.equals(Constants.ACTION.CREATE)) return;

            menuItemDelete.setOnMenuItemClickListener(item -> {
                ((Animatable) menuItemDelete.getIcon()).start();
                // TODO: Removes the given amount of the given product from the given shopping list, if it's on it
                /*request.delete(
                        grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, shoppingListItem.getId()),
                        response -> activity.dismissFragment(),
                        error -> {
                            showErrorMessage();
                            if(DEBUG) Log.i(TAG, "setUpMenu: deleteItem: " + error);
                        }
                );*/
                return true;
            });
        }
    }

    private boolean isAmountValid() {
        if(amount >= 1) {
            textInputAmount.setErrorEnabled(false);
            return true;
        } else {
            textInputAmount.setError(
                    activity.getString(
                            R.string.error_bounds_min,
                            NumUtil.trim(1)
                    )
            );
            return false;
        }
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
        textInputProduct.setErrorEnabled(false);
        autoCompleteTextViewProduct.setText(null);
        textInputAmount.setErrorEnabled(false);
        editTextAmount.setText(NumUtil.trim(1));
        imageViewAmount.setImageResource(R.drawable.ic_round_scatter_plot_anim);
        clearInputFocus();
        for(int i = 0; i < linearLayoutBarcodesContainer.getChildCount(); i++) {
            ((InputChip) linearLayoutBarcodesContainer.getChildAt(i)).close();
        }
        productDetails = null;
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

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
