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
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
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
import java.util.Objects;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.activity.ScanBatchActivity;
import xyz.zedler.patrick.grocy.activity.ScanInputActivity;
import xyz.zedler.patrick.grocy.adapter.MatchArrayAdapter;
import xyz.zedler.patrick.grocy.adapter.ShoppingListItemAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.FragmentPurchaseBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.BBDateBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputBarcodeBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.InputNameBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LocationsBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ProductOverviewBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.StoresBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.view.InputChip;

public class PurchaseFragment extends Fragment {

    private final static String TAG = Constants.UI.PURCHASE;

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private Gson gson;
    private GrocyApi grocyApi;
    private DownloadHelper dlHelper;
    private DateUtil dateUtil;
    private ArrayAdapter<String> adapterProducts;
    private Bundle startupBundle;
    private FragmentPurchaseBinding binding;

    private ArrayList<Product> products;
    private ArrayList<Location> locations;
    private ArrayList<Store> stores;
    private ArrayList<String> productNames;
    private ArrayList<QuantityUnit> quantityUnits;

    private ProductDetails productDetails;

    private int selectedLocationId;
    private int selectedStoreId;
    private int shoppingListItemPos;
    private String selectedBestBeforeDate;
    private double amount;
    private double minAmount;
    private boolean debug;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentPurchaseBinding.inflate(inflater, container, false);
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

        if(getArguments() != null) startupBundle = getArguments();

        // GET PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        // WEB REQUESTS

        grocyApi = activity.getGrocy();
        gson = new Gson();
        dlHelper = new DownloadHelper(activity, TAG);

        // UTILS

        dateUtil = new DateUtil(activity);

        // INITIALIZE VARIABLES

        products = new ArrayList<>();
        locations = new ArrayList<>();
        stores = new ArrayList<>();
        productNames = new ArrayList<>();
        quantityUnits = new ArrayList<>();

        productDetails = null;
        selectedLocationId = -1;
        selectedStoreId = -1;
        selectedBestBeforeDate = null;
        amount = 0;
        minAmount = 0;
        shoppingListItemPos = 0;

        // INITIALIZE VIEWS

        binding.framePurchaseBack.setOnClickListener(v -> activity.onBackPressed());

        binding.linearPurchaseShoppingListItem.linearShoppingListItemContainer.setBackground(
                ContextCompat.getDrawable(activity, R.drawable.bg_list_item_visible_ripple)
        );

        // swipe refresh

        binding.swipePurchase.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        binding.swipePurchase.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        binding.swipePurchase.setOnRefreshListener(this::refresh);

        // product

        binding.textInputPurchaseProduct.setErrorIconDrawable(null);
        binding.textInputPurchaseProduct.setEndIconOnClickListener(v -> startActivityForResult(
                new Intent(activity, ScanInputActivity.class),
                Constants.REQUEST.SCAN
        ));
        binding.autoCompletePurchaseProduct.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(!hasFocus) return;
            IconUtil.start(binding.imagePurchaseProduct);
            if(!productNames.isEmpty()) return;
            // try again to download products
            dlHelper.getProducts(products -> {
                this.products = products;
                productNames = getProductNames();
                adapterProducts = new MatchArrayAdapter(activity, new ArrayList<>(productNames));
                binding.autoCompletePurchaseProduct.setAdapter(adapterProducts);
            }).perform(dlHelper.getUuid());
        });
        binding.autoCompletePurchaseProduct.setOnItemClickListener(
                (parent, v, position, id) -> loadProductDetails(
                        getProductFromName(
                                String.valueOf(parent.getItemAtPosition(position))
                        ).getId()
                )
        );
        binding.autoCompletePurchaseProduct.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        clearInputFocus();
                        String input = binding.autoCompletePurchaseProduct.getText().toString();
                        if(!productNames.isEmpty() && !productNames.contains(input) && !input.isEmpty()) {
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

        // best before date

        binding.linearPurchaseBbd.setOnClickListener(v -> {
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
                binding.textInputPurchaseProduct.setError(activity.getString(R.string.error_select_product));
            }
        });

        // amount

        binding.editTextPurchaseAmount.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                amount = NumUtil.stringToDouble(s.toString());
                isAmountValid();
            }
        });
        binding.editTextPurchaseAmount.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) {
                IconUtil.start(binding.imagePurchaseAmount);
                // editTextAmount.selectAll();
            }
        });
        binding.editTextPurchaseAmount.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        clearInputFocus();
                        return true;
                    } return false;
                });

        binding.buttonPurchaseAmountMore.setOnClickListener(v -> {
            IconUtil.start(activity, R.id.image_purchase_amount);
            if(getAmount().isEmpty()) {
                binding.editTextPurchaseAmount.setText(String.valueOf(1));
            } else {
                double amountNew = Double.parseDouble(getAmount()) + 1;
                binding.editTextPurchaseAmount.setText(NumUtil.trim(amountNew));
            }
        });

        binding.buttonPurchaseAmountLess.setOnClickListener(v -> {
            if(!getAmount().isEmpty()) {
                IconUtil.start(activity, R.id.image_purchase_amount);
                double amountNew = Double.parseDouble(getAmount()) - 1;
                if(amountNew >= minAmount) {
                    binding.editTextPurchaseAmount.setText(NumUtil.trim(amountNew));
                }
            }
        });

        // price

        String currency = sharedPrefs.getString(Constants.PREF.CURRENCY, "");
        if(currency == null || currency.isEmpty()) {
            binding.textInputPurchasePrice.setHint(getString(R.string.property_price));
        } else {
            binding.textInputPurchasePrice.setHint(getString(R.string.property_price_in, currency));
        }
        binding.editTextPurchasePrice.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {isPriceValid();}
        });
        binding.editTextPurchasePrice.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if(hasFocus) {
                IconUtil.start(binding.imagePurchasePrice);
                // editTextAmount.selectAll();
            }
        });
        binding.editTextPurchasePrice.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        clearInputFocus();
                        return true;
                    } return false;
                });

        binding.buttonPurchasePriceMore.setOnClickListener(v -> {
            IconUtil.start(activity, R.id.image_purchase_price);
            if(getPrice().isEmpty()) {
                binding.editTextPurchasePrice.setText(NumUtil.trimPrice(1));
            } else {
                double priceNew = NumUtil.stringToDouble(getPrice()) + 1;
                binding.editTextPurchasePrice.setText(NumUtil.trimPrice(priceNew));
            }
        });
        binding.buttonPurchasePriceLess.setOnClickListener(v -> {
            if(!getPrice().isEmpty()) {
                IconUtil.start(activity, R.id.image_purchase_price);
                double priceNew = NumUtil.stringToDouble(getPrice()) - 1;
                if(priceNew >= 0) {
                    binding.editTextPurchasePrice.setText(NumUtil.trimPrice(priceNew));
                }
            }
        });

        binding.linearPurchaseTotalPrice.setOnClickListener(
                v -> binding.checkboxPurchaseTotalPrice.setChecked(
                        !binding.checkboxPurchaseTotalPrice.isChecked()
                )
        );

        // store

        binding.linearPurchaseStore.setOnClickListener(v -> {
            if(productDetails != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList(Constants.ARGUMENT.STORES, stores);
                bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedStoreId);
                activity.showBottomSheet(new StoresBottomSheetDialogFragment(), bundle);
            } else {
                // no product selected
                binding.textInputPurchaseProduct.setError(activity.getString(R.string.error_select_product));
            }
        });

        // location

        binding.linearPurchaseLocation.setOnClickListener(v -> {
            if(productDetails != null) {
                IconUtil.start(activity, R.id.image_purchase_location);
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList(Constants.ARGUMENT.LOCATIONS, locations);
                bundle.putInt(Constants.ARGUMENT.SELECTED_ID, selectedLocationId);
                activity.showBottomSheet(new LocationsBottomSheetDialogFragment(), bundle);
            } else {
                // no product selected
                binding.textInputPurchaseProduct.setError(activity.getString(R.string.error_select_product));
            }
        });

        hideDisabledFeatures();

        // show or hide shopping list item section
        if(startupBundle != null) {
            String type = startupBundle.getString(Constants.ARGUMENT.TYPE);
            if(type != null && type.equals(Constants.ACTION.PURCHASE_MULTI_THEN_SHOPPING_LIST)) {
                binding.linearPurchaseBatchModeSection.setVisibility(View.VISIBLE);
            } else {
                binding.linearPurchaseBatchModeSection.setVisibility(View.GONE);
            }
        } else {
            binding.linearPurchaseBatchModeSection.setVisibility(View.GONE);
        }

        // START

        if(savedInstanceState == null) {
            refresh();
        } else {
            restoreSavedInstanceState(savedInstanceState);
        }

        // UPDATE UI

        activity.updateUI(Constants.UI.PURCHASE, savedInstanceState == null, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(isHidden()) return;

        outState.putParcelableArrayList("products", products);
        outState.putParcelableArrayList("locations", locations);
        outState.putParcelableArrayList("stores", stores);
        outState.putParcelableArrayList("quantityUnits", quantityUnits);

        outState.putParcelable("productDetails", productDetails);

        outState.putInt("selectedLocationId", selectedLocationId);
        outState.putInt("selectedStoreId", selectedStoreId);
        outState.putInt("shoppingListItemPos", shoppingListItemPos);
        outState.putString("selectedBestBeforeDate", selectedBestBeforeDate);

        outState.putDouble("amount", amount);
        outState.putDouble("minAmount", minAmount);
    }

    private void restoreSavedInstanceState(@NonNull Bundle savedInstanceState) {
        if(isHidden()) return;

        products = savedInstanceState.getParcelableArrayList("products");
        locations = savedInstanceState.getParcelableArrayList("locations");
        stores = savedInstanceState.getParcelableArrayList("stores");
        quantityUnits = savedInstanceState.getParcelableArrayList("quantityUnits");

        productNames = getProductNames();
        adapterProducts = new MatchArrayAdapter(activity, new ArrayList<>(productNames));
        binding.autoCompletePurchaseProduct.setAdapter(adapterProducts);

        productDetails = savedInstanceState.getParcelable("productDetails");

        selectedLocationId = savedInstanceState.getInt("selectedLocationId");
        selectedStoreId = savedInstanceState.getInt("selectedStoreId");
        shoppingListItemPos = savedInstanceState.getInt("shoppingListItemPos");
        selectedBestBeforeDate = savedInstanceState.getString("selectedBestBeforeDate");

        amount = savedInstanceState.getDouble("amount");
        minAmount = savedInstanceState.getDouble("minAmount");

        fillWithShoppingListItem();

        binding.swipePurchase.setRefreshing(false);
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
            binding.swipePurchase.setRefreshing(false);
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
        binding.swipePurchase.setRefreshing(true);
        DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onError);
        queue.append(
                dlHelper.getProducts(products -> {
                    this.products = products;
                    productNames = getProductNames();
                    adapterProducts = new MatchArrayAdapter(activity, new ArrayList<>(productNames));
                    binding.autoCompletePurchaseProduct.setAdapter(adapterProducts);
                }),
                dlHelper.getStores(stores -> {
                    this.stores = stores;
                    SortUtil.sortStoresByName(this.stores, true);
                    // Insert NONE as first element
                    stores.add(
                            0,
                            new Store(-1, activity.getString(R.string.subtitle_none_selected))
                    );
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
        // only load quantity units if shopping list items have to be displayed
        if(startupBundle != null) {
            String type = startupBundle.getString(Constants.ARGUMENT.TYPE);
            if(type != null && type.equals(Constants.ACTION.PURCHASE_MULTI_THEN_SHOPPING_LIST)) {
                queue.append(
                        dlHelper.getQuantityUnits(quUnits -> this.quantityUnits = quUnits)
                );
            }
        }

        queue.start();
    }

    private void onError(VolleyError error) {
        if(debug) Log.e(TAG, "onError: VolleyError: " + error);
        binding.swipePurchase.setRefreshing(false);
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
        String action = null;
        if(startupBundle != null) {
            action = startupBundle.getString(Constants.ARGUMENT.TYPE);
        }
        if(action != null) {
            Product product;
            switch (action) {
                case Constants.ACTION.CREATE_THEN_PURCHASE:
                case Constants.ACTION.EDIT_THEN_PURCHASE:
                    product = getProductFromName(
                            startupBundle.getString(Constants.ARGUMENT.PRODUCT_NAME)
                    );
                    if(product != null) {
                        loadProductDetails(product.getId());
                    } else {
                        binding.autoCompletePurchaseProduct.setText(
                                startupBundle.getString(Constants.ARGUMENT.PRODUCT_NAME)
                        );
                    }
                    break;
                case Constants.ACTION.PURCHASE_THEN_SHOPPING_LIST:
                case Constants.ACTION.PURCHASE_THEN_STOCK:
                    product = getProductFromName(
                            startupBundle.getString(Constants.ARGUMENT.PRODUCT_NAME)
                    );
                    if(product != null) {
                        loadProductDetails(product.getId());
                    }
                    break;
                case Constants.ACTION.PURCHASE_MULTI_THEN_SHOPPING_LIST:
                    fillWithShoppingListItem();
                    break;
            }
        }
        binding.swipePurchase.setRefreshing(false);
    }

    private void fillWithShoppingListItem() {
        ArrayList<ShoppingListItem> listItems = getShoppingListItems();
        if(listItems == null) return;
        ShoppingListItem listItem = getCurrentShoppingListItem(listItems);
        if(listItem == null) return;

        binding.textPurchaseBatch.setText(activity.getString(
                R.string.subtitle_entry_num_of_num,
                shoppingListItemPos+1,
                listItems.size()
        ));
        ShoppingListItemAdapter.fillShoppingListItem(
                activity,
                listItem,
                binding.linearPurchaseShoppingListItem,
                quantityUnits
        );
        startupBundle.putString(Constants.ARGUMENT.AMOUNT, String.valueOf(listItem.getAmount()));
        if(listItem.getProductId() != null) {
            loadProductDetails(Integer.parseInt(listItem.getProductId()));
        } else {
            fillAmount(false);
        }
    }

    private ArrayList<ShoppingListItem> getShoppingListItems() {
        if(startupBundle == null) return null;
        String type = startupBundle.getString(Constants.ARGUMENT.TYPE);
        if(type == null || !type.equals(Constants.ACTION.PURCHASE_MULTI_THEN_SHOPPING_LIST)) {
            return null;
        }
        return startupBundle.getParcelableArrayList(
                Constants.ARGUMENT.SHOPPING_LIST_ITEMS
        );
    }

    private ShoppingListItem getCurrentShoppingListItem(ArrayList<ShoppingListItem> listItems) {
        if(shoppingListItemPos+1 > listItems.size()) return null;
        return listItems.get(shoppingListItemPos);
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
        clearInputFocus();

        boolean isTareWeightHandlingEnabled = productDetails
                .getProduct()
                .getEnableTareWeightHandling() == 1;

        // PRODUCT
        binding.autoCompletePurchaseProduct.setText(productDetails.getProduct().getName());
        binding.autoCompletePurchaseProduct.dismissDropDown(); // necessary for lower Android versions, tested on 5.1
        binding.textInputPurchaseProduct.setErrorEnabled(false);

        // BBD
        int defaultBestBeforeDays = productDetails.getProduct().getDefaultBestBeforeDays();
        if(defaultBestBeforeDays < 0) {
            selectedBestBeforeDate = Constants.DATE.NEVER_EXPIRES;
            binding.textPurchaseBbd.setText(getString(R.string.subtitle_never_expires));
        } else if (defaultBestBeforeDays == 0) {
            selectedBestBeforeDate = null;
            binding.textPurchaseBbd.setText(getString(R.string.subtitle_none_selected));
        } else {
            // add default best before days to today
            selectedBestBeforeDate = DateUtil.getTodayWithDaysAdded(defaultBestBeforeDays);
            binding.textPurchaseBbd.setText(
                    dateUtil.getLocalizedDate(selectedBestBeforeDate, DateUtil.FORMAT_MEDIUM)
            );
        }

        // AMOUNT

        fillAmount(isTareWeightHandlingEnabled);

        // PRICE

        if(productDetails.getLastPrice() != null && !productDetails.getLastPrice().isEmpty()) {
            binding.editTextPurchasePrice.setText(
                    NumUtil.trimPrice(Double.parseDouble(productDetails.getLastPrice()))
            );
        } else {
            binding.editTextPurchasePrice.setText(null);
        }

        binding.checkboxPurchaseTotalPrice.setChecked(false);

        // deactivate checkbox if tare weight handling is on
        if(isTareWeightHandlingEnabled) {
            binding.linearPurchaseTotalPrice.setEnabled(false);
            binding.linearPurchaseTotalPrice.setAlpha(0.5f);
            binding.checkboxPurchaseTotalPrice.setEnabled(false);
        } else {
            binding.linearPurchaseTotalPrice.setEnabled(true);
            binding.linearPurchaseTotalPrice.setAlpha(1.0f);
            binding.checkboxPurchaseTotalPrice.setEnabled(true);
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
            binding.textPurchaseStore.setText(getString(R.string.subtitle_none_selected));
        } else {
            selectedStoreId = Integer.parseInt(storeId);
            Store store = getStore(selectedStoreId);
            if(store != null) {
                binding.textPurchaseStore.setText(store.getName());
            } else {
                binding.textPurchaseStore.setText(getString(R.string.subtitle_none_selected));
            }
        }

        // LOCATION
        if(productDetails.getLocation() != null) {
            selectedLocationId = productDetails.getLocation().getId();
            binding.textPurchaseLocation.setText(productDetails.getLocation().getName());
        } else {
            selectedLocationId = -1;
        }

        // mark fields with invalid or missing content as invalid
        isFormIncomplete();
    }

    private void fillAmount(boolean isTareWeightHandlingEnabled) {
        if(productDetails != null) {
            binding.textInputPurchaseAmount.setHint(
                    activity.getString(
                            R.string.property_amount_in,
                            productDetails.getQuantityUnitPurchase().getNamePlural()
                    )
            );
        } else {
            binding.textInputPurchaseAmount.setHint(activity.getString(R.string.property_amount));
        }
        if(!isTareWeightHandlingEnabled || productDetails == null) {
            minAmount = 1;
        } else {
            minAmount = productDetails.getProduct().getTareWeight();
            minAmount += productDetails.getStockAmount();
        }

        if(startupBundle != null && startupBundle.getString(Constants.ARGUMENT.AMOUNT) != null) {
            double amount = Double.parseDouble(Objects.requireNonNull(
                    startupBundle.getString(Constants.ARGUMENT.AMOUNT)
            ));
            binding.editTextPurchaseAmount.setText(NumUtil.trim(amount));
        } else {
            // leave amount empty if tare weight handling enabled
            if(!isTareWeightHandlingEnabled) {
                String defaultAmount = sharedPrefs.getString(
                        Constants.PREF.STOCK_DEFAULT_PURCHASE_AMOUNT, "1"
                );
                if(defaultAmount == null || defaultAmount.isEmpty()) {
                    binding.editTextPurchaseAmount.setText(null);
                } else {
                    binding.editTextPurchaseAmount.setText(
                            NumUtil.trim(Double.parseDouble(defaultAmount))
                    );
                }
            } else {
                binding.editTextPurchaseAmount.setText(null);
            }
        }

        if(getAmount().isEmpty()) {
            binding.editTextPurchaseAmount.requestFocus();
            activity.showKeyboard(binding.editTextPurchaseAmount);
        }

        // set icon for tare weight, else for normal amount
        binding.imagePurchaseAmount.setImageResource(
                isTareWeightHandlingEnabled
                        ? R.drawable.ic_round_scale_anim
                        : R.drawable.ic_round_scatter_plot_anim
        );
    }

    private void clearInputFocus() {
        activity.hideKeyboard();
        binding.textInputPurchaseProduct.clearFocus();
        binding.textInputPurchaseAmount.clearFocus();
        binding.textInputPurchasePrice.clearFocus();
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
        binding.swipePurchase.setRefreshing(true);
        dlHelper.get(
                grocyApi.getStockProductByBarcode(barcode),
                response -> {
                    binding.swipePurchase.setRefreshing(false);
                    productDetails = gson.fromJson(
                            response,
                            new TypeToken<ProductDetails>(){}.getType()
                    );
                    fillWithProductDetails();
                }, error -> {
                    NetworkResponse response = error.networkResponse;
                    if(response != null && response.statusCode == 400) {
                        binding.autoCompletePurchaseProduct.setText(barcode);
                        Bundle bundle = new Bundle();
                        bundle.putString(Constants.ARGUMENT.BARCODES, barcode);
                        activity.showBottomSheet(
                                new InputBarcodeBottomSheetDialogFragment(), bundle
                        );
                    } else {
                        showMessage(activity.getString(R.string.error_undefined));
                    }
                    binding.swipePurchase.setRefreshing(false);
                }
        );
    }

    private boolean isFormIncomplete() {
        boolean isIncomplete = false;
        String input = binding.autoCompletePurchaseProduct.getText().toString();
        if(!productNames.isEmpty() && !productNames.contains(input) && !input.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.CREATE_THEN_PURCHASE);
            bundle.putString(Constants.ARGUMENT.PRODUCT_NAME, input);
            activity.showBottomSheet(
                    new InputNameBottomSheetDialogFragment(), bundle
            );
            isIncomplete = true;
        }
        if(productDetails == null) {
            binding.textInputPurchaseProduct.setError(activity.getString(R.string.error_select_product));
            isIncomplete = true;
        }
        if(!isBestBeforeDateValid()) isIncomplete = true;
        if(!isAmountValid()) isIncomplete = true;
        if(!isPriceValid()) isIncomplete = true;
        if(!isLocationValid()) isIncomplete = true;
        return isIncomplete;
    }

    public void purchaseProduct() {
        if(isFormIncomplete()) return;
        double amountMultiplied = amount * productDetails.getProduct().getQuFactorPurchaseToStock();
        JSONObject body = new JSONObject();
        try {
            body.put("amount", amountMultiplied);
            body.put("transaction_type", "purchase");
            if(!getPrice().isEmpty()) {
                double price = NumUtil.stringToDouble(getPrice());
                if(binding.checkboxPurchaseTotalPrice.isChecked()) {
                    price = price / amount;
                }
                body.put("price", price);
            }
            if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
                body.put("best_before_date", selectedBestBeforeDate);
            } else {
                body.put("best_before_date", Constants.DATE.NEVER_EXPIRES);
            }
            if(selectedStoreId > -1) {
                body.put("shopping_location_id", selectedStoreId);
            }
            if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
                body.put("location_id", selectedLocationId);
            }
        } catch (JSONException e) {
            if(debug) Log.e(TAG, "purchaseProduct: " + e);
        }
        dlHelper.post(
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
                        if(debug) Log.e(TAG, "purchaseProduct: " + e);
                    }
                    if(debug) Log.i(TAG, "purchaseProduct: purchased " + amountMultiplied);

                    double amountAdded;
                    if(productDetails.getProduct().getEnableTareWeightHandling() == 0) {
                        amountAdded = amountMultiplied;
                    } else {
                        // calculate difference of amount if tare weight handling enabled
                        amountAdded = amountMultiplied - productDetails.getProduct().getTareWeight()
                                - productDetails.getStockAmount();
                    }

                    Snackbar snackbar = Snackbar.make(
                            activity.binding.frameMainContainer,
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
                        switch (action) {
                            case Constants.ACTION.PURCHASE_THEN_SHOPPING_LIST:
                                // delete entry from shopping list
                                ShoppingListItem shoppingListItem = startupBundle.getParcelable(
                                        Constants.ARGUMENT.SHOPPING_LIST_ITEM
                                );
                                assert shoppingListItem != null;
                                dlHelper.delete(
                                        grocyApi.getObject(
                                                GrocyApi.ENTITY.SHOPPING_LIST,
                                                shoppingListItem.getId()
                                        ),
                                        response1 -> activity.dismissFragment(),
                                        error -> activity.dismissFragment()
                                );
                                return;
                            case Constants.ACTION.PURCHASE_THEN_STOCK:
                                activity.dismissFragment();
                                return;
                            case Constants.ACTION.PURCHASE_MULTI_THEN_SHOPPING_LIST:
                                ArrayList<ShoppingListItem> listItems = getShoppingListItems();
                                assert listItems != null;
                                ShoppingListItem listItem = getCurrentShoppingListItem(listItems);
                                assert listItem != null;
                                dlHelper.delete(
                                        grocyApi.getObject(
                                                GrocyApi.ENTITY.SHOPPING_LIST,
                                                listItem.getId()
                                        ),
                                        response1 -> {
                                            shoppingListItemPos += 1;
                                            if(shoppingListItemPos + 1 > listItems.size()) {
                                                activity.dismissFragment();
                                                return;
                                            }
                                            clearAll();
                                            fillWithShoppingListItem();
                                        },
                                        error -> {
                                            shoppingListItemPos += 1;
                                            if(shoppingListItemPos + 1 > listItems.size()) {
                                                activity.dismissFragment();
                                                return;
                                            }
                                            clearAll();
                                            fillWithShoppingListItem();
                                        }
                                );
                            default:
                                clearAll();
                        }
                    } else {
                        clearAll();
                    }
                },
                error -> {
                    showErrorMessage();
                    if(debug) Log.i(TAG, "purchaseProduct: " + error);
                }
        );
    }

    private void undoTransaction(String transactionId) {
        if(binding == null || activity != null && activity.isDestroyed()) return;
        dlHelper.post(
                grocyApi.undoStockTransaction(transactionId),
                success -> {
                    showMessage(activity.getString(R.string.msg_undone_transaction));
                    if(debug) Log.i(TAG, "undoTransaction: undone");
                },
                error -> showErrorMessage()
        );
    }

    private void editProductBarcodes() {
        if(binding.linearPurchaseBarcodeContainer.getChildCount() == 0) return;

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

        for(int i = 0; i < binding.linearPurchaseBarcodeContainer.getChildCount(); i++) {
            InputChip inputChip = (InputChip) binding.linearPurchaseBarcodeContainer.getChildAt(i);
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

    private void hideDisabledFeatures() {
        if(!isFeatureEnabled(Constants.PREF.FEATURE_STOCK_PRICE_TRACKING)) {
            binding.linearPurchaseTotalPrice.setVisibility(View.GONE);
            binding.linearPurchasePrice.setVisibility(View.GONE);
        }
        if(!isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
            binding.linearPurchaseLocation.setVisibility(View.GONE);
        }
        if(!isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
            binding.linearPurchaseBbd.setVisibility(View.GONE);
        }
    }

    public void setUpBottomMenu() {
        MenuItem menuItemBatch, menuItemDetails, menuItemSkipItem;
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
                    binding.textInputPurchaseProduct.setError(
                            activity.getString(R.string.error_select_product)
                    );
                }
                return true;
            });
        }
        menuItemSkipItem = activity.getBottomMenu().findItem(R.id.action_shopping_list_item_skip);
        if(menuItemSkipItem != null) {
            String action = null;
            if(startupBundle != null) action = startupBundle.getString(Constants.ARGUMENT.TYPE);
            if(action != null && action.equals(
                    Constants.ACTION.PURCHASE_MULTI_THEN_SHOPPING_LIST
            )) {
                menuItemSkipItem.setVisible(true);
                menuItemSkipItem.setOnMenuItemClickListener(item -> {
                    IconUtil.start(menuItemSkipItem);
                    ArrayList<ShoppingListItem> listItems = startupBundle
                            .getParcelableArrayList(
                                    Constants.ARGUMENT.SHOPPING_LIST_ITEMS
                            );
                    assert listItems != null;
                    shoppingListItemPos += 1;
                    if(shoppingListItemPos + 1 > listItems.size()) {
                        activity.dismissFragment();
                        return true;
                    }
                    clearAll();
                    fillWithShoppingListItem();
                    return true;
                });
            }
        }
    }

    public void selectBestBeforeDate(String selectedBestBeforeDate) {
        if(selectedBestBeforeDate == null) return;
        this.selectedBestBeforeDate = selectedBestBeforeDate;
        if(selectedBestBeforeDate.equals(Constants.DATE.NEVER_EXPIRES)) {
            binding.textPurchaseBbd.setText(getString(R.string.subtitle_never_expires));
        } else {
            binding.textPurchaseBbd.setText(
                    dateUtil.getLocalizedDate(selectedBestBeforeDate, DateUtil.FORMAT_MEDIUM)
            );
        }
        isBestBeforeDateValid();
    }

    public void selectStore(int selectedId) {
        this.selectedStoreId = selectedId;
        if(stores.isEmpty()) {
            binding.textPurchaseLocation.setText(getString(R.string.subtitle_none_selected));
        } else {
            Store store = getStore(selectedId);
            if(store != null) {
                binding.textPurchaseStore.setText(store.getName());
            } else {
                binding.textPurchaseStore.setText(getString(R.string.subtitle_none_selected));
                showErrorMessage();
            }
        }
    }

    public void selectLocation(int selectedId) {
        this.selectedLocationId = selectedId;
        if(locations.isEmpty()) {
            binding.textPurchaseLocation.setText(getString(R.string.subtitle_none_selected));
        } else {
            Location location = getLocation(selectedId);
            if(location != null) {
                binding.textPurchaseLocation.setText(location.getName());
            } else {
                binding.textPurchaseLocation.setText(getString(R.string.subtitle_none_selected));
                showErrorMessage();
            }
        }
        isLocationValid();
    }

    private boolean isBestBeforeDateValid() {
        if(!isFeatureEnabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
            return true;
        } else if(selectedBestBeforeDate == null || selectedBestBeforeDate.isEmpty()) {
            binding.textPurchaseBbdLabel.setTextColor(getColor(R.color.error));
            return false;
        } else {
            binding.textPurchaseBbdLabel.setTextColor(getColor(R.color.on_background_secondary));
            return true;
        }
    }

    private boolean isLocationValid() {
        if(!isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
            return true;
        } else if(selectedLocationId < 0) {
            binding.textPurchaseLocationLabel.setTextColor(getColor(R.color.error));
            return false;
        } else {
            binding.textPurchaseLocationLabel.setTextColor(
                    getColor(R.color.on_background_secondary)
            );
            return true;
        }
    }

    private boolean isAmountValid() {
        if(amount >= minAmount) {
            if(productDetails != null
                    && amount % 1 != 0 // partial amount, has to be allowed in product master
                    && productDetails.getProduct().getAllowPartialUnitsInStock() == 0
            ) {
                binding.textInputPurchaseAmount.setError(
                        activity.getString(R.string.error_invalid_amount)
                );
                return false;
            } else {
                binding.textInputPurchaseAmount.setErrorEnabled(false);
                return true;
            }
        } else {
            if(productDetails != null) {
                binding.textInputPurchaseAmount.setError(
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
        if(!getPrice().isEmpty()) {
            if(NumUtil.stringToDouble(getPrice()) >= 0) {
                binding.textInputPurchasePrice.setErrorEnabled(false);
                return true;
            } else {
                if(productDetails != null) {
                    binding.textInputPurchasePrice.setError(
                            activity.getString(
                                    R.string.error_bounds_min,
                                    NumUtil.trim(0)
                            )
                    );
                }
                return false;
            }
        } else {
            binding.textInputPurchasePrice.setErrorEnabled(false);
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
        String input = binding.autoCompletePurchaseProduct.getText().toString().trim();
        if(input.isEmpty()) return;
        for(int i = 0; i < binding.linearPurchaseBarcodeContainer.getChildCount(); i++) {
            InputChip inputChip = (InputChip) binding.linearPurchaseBarcodeContainer.getChildAt(i);
            if(inputChip.getText().equals(input)) {
                showMessage(activity.getString(R.string.msg_barcode_duplicate));
                binding.autoCompletePurchaseProduct.setText(null);
                binding.autoCompletePurchaseProduct.requestFocus();
                return;
            }
        }
        InputChip inputChipBarcode = new InputChip(
                activity, input, R.drawable.ic_round_barcode, true
        );
        inputChipBarcode.setPadding(0, 0, 0, 8);
        binding.linearPurchaseBarcodeContainer.addView(inputChipBarcode);
        binding.autoCompletePurchaseProduct.setText(null);
        binding.autoCompletePurchaseProduct.requestFocus();
    }

    public void clearAll() {
        productDetails = null;
        binding.textInputPurchaseProduct.setErrorEnabled(false);
        binding.autoCompletePurchaseProduct.setText(null);
        binding.textPurchaseBbd.setText(activity.getString(R.string.subtitle_none_selected));
        binding.textPurchaseBbdLabel.setTextColor(getColor(R.color.on_background_secondary));
        binding.textInputPurchaseAmount.setErrorEnabled(false);
        binding.editTextPurchaseAmount.setText(null);
        binding.imagePurchaseAmount.setImageResource(R.drawable.ic_round_scatter_plot_anim);
        binding.textInputPurchasePrice.setErrorEnabled(false);
        binding.editTextPurchasePrice.setText(null);
        binding.linearPurchaseTotalPrice.setAlpha(1.0f);
        binding.linearPurchaseTotalPrice.setEnabled(true);
        binding.checkboxPurchaseTotalPrice.setEnabled(true);
        binding.checkboxPurchaseTotalPrice.setChecked(false);
        binding.textPurchaseStore.setText(activity.getString(R.string.subtitle_none_selected));
        binding.textPurchaseLocation.setText(activity.getString(R.string.subtitle_none_selected));
        binding.textPurchaseLocationLabel.setTextColor(getColor(R.color.on_background_secondary));
        clearInputFocus();
        for(int i = 0; i < binding.linearPurchaseBarcodeContainer.getChildCount(); i++) {
            ((InputChip) binding.linearPurchaseBarcodeContainer.getChildAt(i)).close();
        }
    }

    private void showErrorMessage() {
        showMessage(activity.getString(R.string.error_undefined));
    }

    private void showMessage(String text) {
        activity.showMessage(
                Snackbar.make(activity.binding.frameMainContainer, text, Snackbar.LENGTH_SHORT)
        );
    }

    private int getColor(@ColorRes int color) {
        return ContextCompat.getColor(activity, color);
    }

    private String getAmount() {
        Editable amount = binding.editTextPurchaseAmount.getText();
        if(amount == null) return "";
        return amount.toString();
    }

    private String getPrice() {
        Editable price = binding.editTextPurchasePrice.getText();
        if(price == null) return "";
        return price.toString();
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
