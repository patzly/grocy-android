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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.activity.ScanInputActivity;
import xyz.zedler.patrick.grocy.adapter.MatchArrayAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.FragmentShoppingListItemEditBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputBarcodeBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputNameBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ShoppingListsBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.TextUtil;
import xyz.zedler.patrick.grocy.view.InputChip;

public class ShoppingListItemEditFragment extends BaseFragment {

    private final static String TAG = Constants.UI.SHOPPING_LIST_ITEM_EDIT;

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private Gson gson;
    private GrocyApi grocyApi;
    private DownloadHelper dlHelper;
    private ArrayAdapter<String> adapterProducts;
    private ShoppingListItemEditFragmentArgs args;
    private FragmentShoppingListItemEditBinding binding;

    private ArrayList<Product> products;
    private ArrayList<ShoppingList> shoppingLists;
    private ArrayList<String> productNames;

    private ProductDetails productDetails;

    private double amount;
    private boolean nameAutoFilled;
    private int selectedShoppingListId;
    private boolean debug;

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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        dlHelper.destroy();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if(isHidden()) return;

        activity = (MainActivity) getActivity();
        assert activity != null && getArguments() != null;

        args = ShoppingListItemEditFragmentArgs.fromBundle(getArguments());

        // PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        // WEB

        grocyApi = activity.getGrocy();
        gson = new Gson();
        dlHelper = new DownloadHelper(activity, TAG);

        // VARIABLES

        products = new ArrayList<>();
        shoppingLists = new ArrayList<>();
        productNames = new ArrayList<>();

        productDetails = null;

        amount = 0;
        nameAutoFilled = false;
        selectedShoppingListId = -1;

        // VIEWS

        binding.frameShoppingListItemEditBack.setOnClickListener(v -> activity.onBackPressed());

        // title

        if(args.getAction().equals(Constants.ACTION.EDIT)) {
            binding.textShoppingListItemEditTitle.setText(
                    activity.getString(R.string.title_edit_list_entry)
            );
        } else {
            binding.textShoppingListItemEditTitle.setText(
                    activity.getString(R.string.title_create_list_entry)
            );
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

        if(!sharedPrefs.getBoolean(Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS, true)) {
            binding.linearShoppingListItemEditShoppingList.setVisibility(View.GONE);
        }

        // product

        binding.textInputShoppingListItemEditProduct.setErrorIconDrawable(null);
        binding.textInputShoppingListItemEditProduct.setEndIconOnClickListener(
                v -> startActivityForResult(
                        new Intent(activity, ScanInputActivity.class),
                        Constants.REQUEST.SCAN
                )
        );
        binding.autoCompleteShoppingListItemEditProduct.setOnFocusChangeListener(
                (View v, boolean hasFocus) -> {
                    if(!hasFocus) return;
                    IconUtil.start(binding.imageShoppingListItemEditProduct);
                    if(!productNames.isEmpty()) return;
                    // try again to download products
                    dlHelper.getProducts(products -> {
                        this.products = products;
                        productNames = getProductNames();
                        adapterProducts = new MatchArrayAdapter(activity, productNames);
                        binding.autoCompleteShoppingListItemEditProduct.setAdapter(
                                adapterProducts
                        );
                    }).perform(dlHelper.getUuid());
                });
        binding.autoCompleteShoppingListItemEditProduct.setOnItemClickListener(
                (parent, itemView, position, id) -> loadProductDetails(
                        getProductFromName(
                                String.valueOf(parent.getItemAtPosition(position))
                        ).getId()
                )
        );
        binding.autoCompleteShoppingListItemEditProduct.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        clearInputFocus();
                        String input = binding.autoCompleteShoppingListItemEditProduct
                                .getText()
                                .toString()
                                .trim();
                        if(!productNames.contains(input)
                                && !input.isEmpty()
                                && !nameAutoFilled
                        ) {
                            Bundle bundle = new Bundle();
                            bundle.putString(
                                    Constants.ARGUMENT.TYPE,
                                    Constants.ACTION.CREATE_THEN_SHOPPING_LIST_ITEM_EDIT
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

        // amount

        binding.editTextShoppingListItemEditAmount.addTextChangedListener(new TextWatcher() {
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
        binding.editTextShoppingListItemEditAmount.setOnFocusChangeListener(
                (View v, boolean hasFocus) -> {
                    if(hasFocus) {
                        IconUtil.start(binding.imageShoppingListItemEditAmount);
                        // editTextAmount.selectAll();
                    }
                });
        binding.editTextShoppingListItemEditAmount.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        clearInputFocus();
                        return true;
                    } return false;
                });

        binding.buttonShoppingListItemEditAmountMore.setOnClickListener(v -> {
            IconUtil.start(binding.imageShoppingListItemEditAmount);
            Editable amount = binding.editTextShoppingListItemEditAmount.getText();
            if((amount != null ? amount : "").toString().isEmpty()) {
                binding.editTextShoppingListItemEditAmount.setText(String.valueOf(1));
            } else {
                double amountNew = Double.parseDouble(
                        binding.editTextShoppingListItemEditAmount.getText().toString()
                ) + 1;
                binding.editTextShoppingListItemEditAmount.setText(NumUtil.trim(amountNew));
            }
        });

        binding.buttonShoppingListItemEditAmountLess.setOnClickListener(v -> {
            Editable amount = binding.editTextShoppingListItemEditAmount.getText();
            if(!(amount != null ? amount : "").toString().isEmpty()) {
                IconUtil.start(binding.imageShoppingListItemEditAmount);
                double amountNew = Double.parseDouble(
                        binding.editTextShoppingListItemEditAmount.getText().toString()
                ) - 1;
                if(amountNew >= 1) {
                    binding.editTextShoppingListItemEditAmount.setText(NumUtil.trim(amountNew));
                }
            }
        });

        // START

        if(savedInstanceState == null) {
            refresh();
        } else {
            restoreSavedInstanceState(savedInstanceState);
        }

        // UPDATE UI
        updateUI((getArguments() == null
                || getArguments().getBoolean(Constants.ARGUMENT.ANIMATED, true))
                && savedInstanceState == null);

        NavBackStackEntry currentBackStackEntry = NavHostFragment
                .findNavController(this)
                .getCurrentBackStackEntry();
        assert currentBackStackEntry != null;
        MutableLiveData<String> liveData = currentBackStackEntry
                .getSavedStateHandle()
                .getLiveData(Constants.ARGUMENT.PRODUCT_NAME);
        liveData.observe(getViewLifecycleOwner(), (String productName) -> {
            // TODO: Prioritize productName over args.getProductName(), no idea yet
            currentBackStackEntry.getSavedStateHandle().remove(Constants.ARGUMENT.PRODUCT_NAME);
        });
    }

    private void updateUI(boolean animated) {
        activity.showHideDemoIndicator(this, animated);
        activity.getScrollBehavior().setUpScroll(R.id.scroll_shopping_list_item_edit);
        activity.getScrollBehavior().setHideOnScroll(true);
        activity.updateBottomAppBar(
                Constants.FAB.POSITION.END,
                R.menu.menu_shopping_list_item_edit,
                animated,
                this::setUpBottomMenu
        );
        activity.updateFab(
                R.drawable.ic_round_backup,
                R.string.action_save,
                Constants.FAB.TAG.SAVE,
                animated,
                this::saveItem
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(!isHidden()) {
            outState.putParcelableArrayList("products", products);
            outState.putParcelableArrayList("shoppingLists", shoppingLists);

            outState.putStringArrayList("productNames", productNames);

            outState.putParcelable("productDetails", productDetails);

            outState.putDouble("amount", amount);
            outState.putBoolean("nameAutoFilled", nameAutoFilled);
            outState.putInt("selectedShoppingListId", selectedShoppingListId);
        }
        super.onSaveInstanceState(outState);
    }

    private void restoreSavedInstanceState(@NonNull Bundle savedInstanceState) {
        if(isHidden()) return;

        products = savedInstanceState.getParcelableArrayList("products");
        shoppingLists = savedInstanceState.getParcelableArrayList("shoppingLists");

        productNames = savedInstanceState.getStringArrayList("productNames");
        adapterProducts = new MatchArrayAdapter(activity, productNames);
        binding.autoCompleteShoppingListItemEditProduct.setAdapter(adapterProducts);

        productDetails = savedInstanceState.getParcelable("productDetails");

        amount = savedInstanceState.getDouble("amount");
        nameAutoFilled = savedInstanceState.getBoolean("nameAutoFilled");
        selectedShoppingListId = savedInstanceState.getInt("selectedShoppingListId");
        selectShoppingList(selectedShoppingListId);

        binding.swipeShoppingListItemEdit.setRefreshing(false);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if(!hidden && getView() != null) onViewCreated(getView(), null);
    }

    private void refresh() {
        if(activity.isOnline()) {
            download();
        } else {
            binding.swipeShoppingListItemEdit.setRefreshing(false);
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
        binding.swipeShoppingListItemEdit.setRefreshing(true);
        DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onError);
        queue.append(
                dlHelper.getProducts(products -> {
                    this.products = products;
                    productNames = getProductNames();
                    adapterProducts = new MatchArrayAdapter(activity, productNames);
                    binding.autoCompleteShoppingListItemEditProduct.setAdapter(adapterProducts);
                }),
                dlHelper.getShoppingLists(shoppingLists -> this.shoppingLists = shoppingLists)
        ).start();
    }

    private void onError(VolleyError error) {
        if(debug) Log.e(TAG, "onError: VolleyError: " + error);
        binding.swipeShoppingListItemEdit.setRefreshing(false);
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

    private void onQueueEmpty() {
        binding.swipeShoppingListItemEdit.setRefreshing(false);

        switch (args.getAction()) {
            case Constants.ACTION.EDIT: {
                ShoppingListItem shoppingListItem = args.getShoppingListItem();
                if (shoppingListItem == null) return;
                String productName = args.getProductName();
                if (productName != null) {
                    // is given after new product was created from this fragment
                    // with method (setProductName)
                    binding.autoCompleteShoppingListItemEditProduct.setText(productName);
                } else if (shoppingListItem.getProduct() != null) {
                    binding.autoCompleteShoppingListItemEditProduct.setText(
                            shoppingListItem.getProduct().getName()
                    );
                }
                binding.editTextShoppingListItemEditAmount.setText(
                        NumUtil.trim(shoppingListItem.getAmount())
                );
                selectShoppingList(shoppingListItem.getShoppingListId());
                binding.editTextShoppingListItemEditNote.setText(
                        TextUtil.trimCharSequence(shoppingListItem.getNote())
                );
                break;
            }
            case Constants.ACTION.CREATE: {
                String productName = args.getProductName();
                if (productName != null) {
                    // is given after new product was created from this fragment
                    // with method (setProductName)
                    binding.autoCompleteShoppingListItemEditProduct.setText(productName);
                }
                if (shoppingLists.size() >= 1 && args.getSelectedShoppingListId() != -1) {
                    selectShoppingList(args.getSelectedShoppingListId());
                } else if(shoppingLists.size() >= 1) {
                    selectShoppingList(shoppingLists.get(0).getId());
                } else {
                    selectShoppingList(-1);
                }
                break;
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
        binding.autoCompleteShoppingListItemEditProduct.setText(
                productDetails.getProduct().getName()
        );
        binding.textInputShoppingListItemEditProduct.setErrorEnabled(false);

        // AMOUNT
        binding.textInputShoppingListItemEditAmount.setHint(
                activity.getString(
                        R.string.property_amount_in,
                        productDetails.getQuantityUnitStock().getNamePlural()
                )
        );
    }

    private void clearInputFocus() {
        activity.hideKeyboard();
        binding.textInputShoppingListItemEditProduct.clearFocus();
        binding.textInputShoppingListItemEditAmount.clearFocus();
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

    private void loadProductDetailsByBarcode(String barcode) {
        binding.swipeShoppingListItemEdit.setRefreshing(true);
        dlHelper.get(
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
                        binding.autoCompleteShoppingListItemEditProduct.setText(barcode);
                        nameAutoFilled = true;
                        Bundle bundle = new Bundle();
                        bundle.putString(Constants.ARGUMENT.BARCODES, barcode);
                        activity.showBottomSheet(
                                new InputBarcodeBottomSheetDialogFragment(), bundle
                        );
                    } else {
                        showErrorMessage();
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
            Editable amountEdit = binding.editTextShoppingListItemEditAmount.getText();
            String amount = (amountEdit != null ? amountEdit : "").toString().trim();
            jsonObject.put("shopping_list_id", selectedShoppingListId);
            jsonObject.put("amount", amount);
            Product product = getProductFromName(
                    binding.autoCompleteShoppingListItemEditProduct.getText().toString().trim()
            );
            if(product != null) {
                jsonObject.put("product_id", product.getId());
            } else {
                jsonObject.put("product_id", "");
            }
            Editable note = binding.editTextShoppingListItemEditNote.getText();
            assert note != null;
            jsonObject.put("note", note.toString().trim());
        } catch (JSONException e) {
            if(debug) Log.e(TAG, "saveShoppingListItem: " + e);
        }
        if(args.getAction().equals(Constants.ACTION.EDIT)) {
            ShoppingListItem shoppingListItem = args.getShoppingListItem();
            assert shoppingListItem != null;
            dlHelper.put(
                    grocyApi.getObject(GrocyApi.ENTITY.SHOPPING_LIST, shoppingListItem.getId()),
                    jsonObject,
                    response -> {
                        editProductBarcodes(); // ADD BARCODES TO PRODUCT
                        NavHostFragment.findNavController(this).navigateUp();
                    },
                    error -> {
                        showErrorMessage();
                        if(debug) Log.e(TAG, "saveShoppingListItem: " + error);
                    }
            );
        } else {
            dlHelper.post(
                    grocyApi.getObjects(GrocyApi.ENTITY.SHOPPING_LIST),
                    jsonObject,
                    response -> {
                        editProductBarcodes(); // ADD BARCODES TO PRODUCT
                        activity.navigateUp();
                    },
                    error -> {
                        showErrorMessage();
                        if(debug) Log.e(TAG, "saveShoppingListItem: " + error);
                    }
            );
        }
    }

    private boolean isFormIncomplete() {
        String input = binding.autoCompleteShoppingListItemEditProduct.getText().toString().trim();
        if(binding.linearShoppingListItemEditBarcodeContainer.getChildCount() > 0 && input.isEmpty()
        ) {
            binding.textInputShoppingListItemEditProduct.setError(
                    activity.getString(R.string.error_empty)
            );
            return true;
        } else {
            binding.textInputShoppingListItemEditProduct.setErrorEnabled(false);
        }
        if(!productNames.contains(input)
                && !input.isEmpty()
                && !nameAutoFilled
        ) {
            Bundle bundle = new Bundle();
            bundle.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.CREATE_THEN_PURCHASE);
            bundle.putString(Constants.ARGUMENT.PRODUCT_NAME, input);
            activity.showBottomSheet(new InputNameBottomSheetDialogFragment(), bundle);
            return true;
        } else return !isAmountValid();
    }

    private void editProductBarcodes() {
        String input = binding.autoCompleteShoppingListItemEditProduct.getText().toString().trim();
        if(input.isEmpty()) return;
        Product product = getProductFromName(input);
        if(product == null) return;
        if(binding.linearShoppingListItemEditBarcodeContainer.getChildCount() == 0) return;

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
        for(
                int i = 0;
                i < binding.linearShoppingListItemEditBarcodeContainer.getChildCount();
                i++
        ) {
            InputChip inputChip = (InputChip) binding.linearShoppingListItemEditBarcodeContainer
                    .getChildAt(i);
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
                grocyApi.getObject(GrocyApi.ENTITY.PRODUCTS, product.getId()),
                body,
                response -> { },
                error -> {
                    if(debug) Log.i(TAG, "editProductBarcodes: " + error);
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

    public void selectShoppingList(int selectedId) {
        if(sharedPrefs.getBoolean(Constants.PREF.FEATURE_MULTIPLE_SHOPPING_LISTS, true)) {
            this.selectedShoppingListId = selectedId;
            TextView textView = binding.textShoppingListItemEditShoppingList;
            ShoppingList shoppingList = getShoppingList(selectedId);
            if(shoppingList != null) {
                textView.setText(shoppingList.getName());
            } else {
                textView.setText(getString(R.string.subtitle_none_selected));
            }
        } else {
            this.selectedShoppingListId = 1;
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
        MenuItem menuItemDelete, menuItemDetails;
        menuItemDelete = activity.getBottomMenu().findItem(R.id.action_delete);
        if(menuItemDelete != null) {
            menuItemDelete.setVisible(args.getAction().equals(Constants.ACTION.EDIT));
            if(menuItemDelete.isVisible()) {
                menuItemDelete.setOnMenuItemClickListener(item -> {
                    ((Animatable) menuItemDelete.getIcon()).start();
                    ShoppingListItem shoppingListItem = args.getShoppingListItem();
                    assert shoppingListItem != null;
                    dlHelper.delete(
                            grocyApi.getObject(
                                    GrocyApi.ENTITY.SHOPPING_LIST,
                                    shoppingListItem.getId()
                            ),
                            response -> activity.dismissFragment(),
                            error -> {
                                showErrorMessage();
                                if(debug) Log.i(
                                        TAG,
                                        "setUpBottomMenu: deleteItem: " + error
                                );
                            }
                    );
                    return true;
                });
            }
        }

        menuItemDetails = activity.getBottomMenu().findItem(R.id.action_product_overview);
        if(menuItemDetails != null) {
            menuItemDetails.setOnMenuItemClickListener(item -> {
                IconUtil.start(menuItemDetails);
                String input = binding.autoCompleteShoppingListItemEditProduct.getText()
                        .toString()
                        .trim();
                if(productDetails != null && input.equals(productDetails.getProduct().getName())) {
                    NavHostFragment.findNavController(this).navigate(
                                    ShoppingListItemEditFragmentDirections
                                            .actionShoppingListItemEditFragmentToProductOverviewBottomSheetDialogFragment()
                                            .setProductDetails(productDetails)
                    );
                } else {
                    Product product = getProductFromName(input);
                    if(product != null) {
                        dlHelper.get(
                                grocyApi.getStockProductDetails(product.getId()),
                                response -> {
                                    productDetails = gson.fromJson(
                                            response,
                                            new TypeToken<ProductDetails>() {
                                            }.getType()
                                    );
                                    NavHostFragment.findNavController(this).navigate(
                                            ShoppingListItemEditFragmentDirections
                                                    .actionShoppingListItemEditFragmentToProductOverviewBottomSheetDialogFragment()
                                                    .setProductDetails(productDetails)
                                    );
                                }, error -> {
                                }
                        );
                    } else if(!productNames.isEmpty()) {
                        showMessage(activity.getString(R.string.error_invalid_product));
                    } else {
                        showErrorMessage();
                    }
                }
                return true;
            });
        }
    }

    private boolean isAmountValid() {
        if(amount >= 1) {
            binding.textInputShoppingListItemEditAmount.setErrorEnabled(false);
            return true;
        } else {
            binding.textInputShoppingListItemEditAmount.setError(
                    activity.getString(
                            R.string.error_bounds_min,
                            NumUtil.trim(1)
                    )
            );
            return false;
        }
    }

    public void addInputAsBarcode() {
        String input = binding.autoCompleteShoppingListItemEditProduct.getText().toString().trim();
        if(input.isEmpty()) return;
        for(
                int i = 0;
                i < binding.linearShoppingListItemEditBarcodeContainer.getChildCount();
                i++
        ) {
            InputChip inputChip = (InputChip) binding.linearShoppingListItemEditBarcodeContainer
                    .getChildAt(i);
            if(inputChip.getText().equals(input)) {
                showMessage(activity.getString(R.string.msg_barcode_duplicate));
                binding.autoCompleteShoppingListItemEditProduct.setText(null);
                binding.autoCompleteShoppingListItemEditProduct.requestFocus();
                return;
            }
        }
        InputChip inputChipBarcode = new InputChip(
                activity, input, R.drawable.ic_round_barcode, true
        );
        inputChipBarcode.setPadding(0, 0, 0, 8);
        binding.linearShoppingListItemEditBarcodeContainer.addView(inputChipBarcode);
        binding.autoCompleteShoppingListItemEditProduct.setText(null);
        binding.autoCompleteShoppingListItemEditProduct.requestFocus();
    }

    public void clearAll() {
        binding.textInputShoppingListItemEditProduct.setErrorEnabled(false);
        binding.autoCompleteShoppingListItemEditProduct.setText(null);
        binding.textInputShoppingListItemEditAmount.setErrorEnabled(false);
        binding.editTextShoppingListItemEditAmount.setText(NumUtil.trim(1));
        binding.imageShoppingListItemEditAmount.setImageResource(
                R.drawable.ic_round_scatter_plot_anim
        );
        clearInputFocus();
        for(
                int i = 0;
                i < binding.linearShoppingListItemEditBarcodeContainer.getChildCount();
                i++
        ) {
            ((InputChip) binding.linearShoppingListItemEditBarcodeContainer.getChildAt(i)).close();
        }
        productDetails = null;
        nameAutoFilled = false;
    }

    private void showMessage(String msg) {
        activity.showMessage(
                Snackbar.make(activity.binding.frameMainContainer, msg, Snackbar.LENGTH_SHORT)
        );
    }

    private void showErrorMessage() {
        showMessage(activity.getString(R.string.error_undefined));
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
