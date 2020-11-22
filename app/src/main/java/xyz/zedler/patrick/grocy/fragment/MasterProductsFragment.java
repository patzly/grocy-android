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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.VolleyError;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;

import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.adapter.MasterProductAdapter;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterProductsBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterProductBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.EmptyStateHelper;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.AnimUtil;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.view.InputChip;

public class MasterProductsFragment extends Fragment
        implements MasterProductAdapter.MasterProductAdapterListener {

    private final static String TAG = Constants.UI.MASTER_PRODUCTS;

    private MainActivity activity;
    private DownloadHelper dlHelper;
    private AppBarBehavior appBarBehavior;
    private SharedPreferences sharedPrefs;
    private MasterProductAdapter masterProductAdapter;
    private FragmentMasterProductsBinding binding;
    private ClickUtil clickUtil;
    private AnimUtil animUtil;
    private EmptyStateHelper emptyStateHelper;

    private InputChip inputChipFilterProductGroup;

    private ArrayList<Product> products;
    private ArrayList<Product> filteredProducts;
    private ArrayList<Product> displayedProducts;
    private ArrayList<ProductGroup> productGroups;
    private ArrayList<Location> locations;
    private ArrayList<QuantityUnit> quantityUnits;

    private HashMap<Integer, ProductGroup> productGroupsMap;
    private HashMap<Integer, Location> locationsMap;
    private HashMap<Integer, QuantityUnit> quantityUnitsMap;

    private String search;
    private String errorState;
    private int filterProductGroupId;
    private boolean sortAscending;
    private boolean isRestoredInstance;
    private boolean debug;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentMasterProductsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if(emptyStateHelper != null) {
            emptyStateHelper.destroyInstance();
            emptyStateHelper = null;
        }
        if(binding != null) {
            binding.recyclerMasterProducts.animate().cancel();
            binding = null;
        }
        if(dlHelper != null) {
            dlHelper.destroy();
            dlHelper = null;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if(isHidden()) return;

        activity = (MainActivity) getActivity();
        assert activity != null;

        // UTILS

        clickUtil = new ClickUtil();
        animUtil = new AnimUtil();

        // PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        // WEB

        dlHelper = new DownloadHelper(activity, TAG);

        // VARIABLES

        products = new ArrayList<>();
        filteredProducts = new ArrayList<>();
        displayedProducts = new ArrayList<>();
        productGroups = new ArrayList<>();
        locations = new ArrayList<>();
        quantityUnits = new ArrayList<>();

        locationsMap = new HashMap<>();
        productGroupsMap = new HashMap<>();
        quantityUnitsMap = new HashMap<>();

        search = "";
        errorState = Constants.STATE.NONE;
        filterProductGroupId = -1;
        sortAscending = true;

        // VIEWS

        binding.frameMasterProductsBack.setOnClickListener(v -> activity.onBackPressed());
        binding.frameMasterProductsSearchClose.setOnClickListener(v -> dismissSearch());
        binding.editTextMasterProductsSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                search = s.toString();
            }
        });
        binding.editTextMasterProductsSearch.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        Editable search = binding.editTextMasterProductsSearch.getText();
                        searchProducts(search != null ? search.toString() : "");
                        activity.hideKeyboard();
                        return true;
                    } return false;
                });
        emptyStateHelper = new EmptyStateHelper(this, binding.linearEmpty);

        // APP BAR BEHAVIOR

        appBarBehavior = new AppBarBehavior(
                activity,
                R.id.linear_master_products_app_bar_default,
                R.id.linear_master_products_app_bar_search
        );

        // SWIPE REFRESH

        binding.swipeMasterProducts.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        binding.swipeMasterProducts.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        binding.swipeMasterProducts.setOnRefreshListener(this::refresh);

        binding.recyclerMasterProducts.setLayoutManager(
                new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        );
        binding.recyclerMasterProducts.setItemAnimator(new DefaultItemAnimator());
        binding.recyclerMasterProducts.setAdapter(new MasterPlaceholderAdapter());

        if(savedInstanceState == null) {
            load();
        } else {
            restoreSavedInstanceState(savedInstanceState);
        }

        // UPDATE UI

        activity.updateUI(
                appBarBehavior.isPrimaryLayout()
                        ? Constants.UI.MASTER_PRODUCTS_DEFAULT
                        : Constants.UI.MASTER_PRODUCTS_SEARCH,
                savedInstanceState == null,
                TAG
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(isHidden()) return;

        outState.putParcelableArrayList("products", products);
        outState.putParcelableArrayList("filteredProducts", filteredProducts);
        outState.putParcelableArrayList("displayedProducts", displayedProducts);
        outState.putParcelableArrayList("productGroups", productGroups);
        outState.putParcelableArrayList("locations", locations);
        outState.putParcelableArrayList("quantityUnits", quantityUnits);

        outState.putString("errorState", errorState);
        outState.putString("search", search);
        outState.putInt("filterProductGroupId", filterProductGroupId);
        outState.putBoolean("sortAscending", sortAscending);

        appBarBehavior.saveInstanceState(outState);
    }

    private void restoreSavedInstanceState(@NonNull Bundle savedInstanceState) {
        if(isHidden()) return;

        errorState = savedInstanceState.getString("errorState", Constants.STATE.NONE);
        setError(errorState, false);

        products = savedInstanceState.getParcelableArrayList("products");
        filteredProducts = savedInstanceState.getParcelableArrayList("filteredProducts");
        displayedProducts = savedInstanceState.getParcelableArrayList("displayedProducts");
        productGroups = savedInstanceState.getParcelableArrayList("productGroups");
        locations = savedInstanceState.getParcelableArrayList("locations");
        quantityUnits = savedInstanceState.getParcelableArrayList("quantityUnits");

        productGroupsMap = ArrayUtil.getProductGroupsHashMap(productGroups);
        locationsMap = ArrayUtil.getLocationsHashMap(locations);
        quantityUnitsMap = ArrayUtil.getQuantityUnitsHashMap(quantityUnits);

        appBarBehavior.restoreInstanceState(savedInstanceState);

        binding.swipeMasterProducts.setRefreshing(false);

        // SEARCH
        search = savedInstanceState.getString("search", "");
        binding.editTextMasterProductsSearch.setText(search);

        // FILTERS
        updateProductGroupFilter(
                savedInstanceState.getInt("filterProductGroupId", -1)
        );
        sortAscending = savedInstanceState.getBoolean("sortAscending", true);
        isRestoredInstance = true;
        filterProducts();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if(!hidden && getView() != null) onViewCreated(getView(), null);
    }

    private void load() {
        if(activity.isOnline()) {
            download();
        } else {
            setError(Constants.STATE.OFFLINE, false);
        }
    }

    private void refresh() {
        if(activity.isOnline()) {
            setError(Constants.STATE.NONE, true);
            download();
        } else {
            binding.swipeMasterProducts.setRefreshing(false);
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
    }

    private void setError(String state, boolean animated) {
        errorState = state;

        binding.linearError.buttonErrorRetry.setOnClickListener(v -> refresh());

        View viewIn = binding.linearError.linearError;
        View viewOut = binding.scrollMasterProducts;

        switch (state) {
            case Constants.STATE.OFFLINE:
                binding.linearError.imageError.setImageResource(R.drawable.illustration_broccoli);
                binding.linearError.textErrorTitle.setText(R.string.error_offline);
                binding.linearError.textErrorSubtitle.setText(R.string.error_offline_subtitle);
                emptyStateHelper.clearState();
                break;
            case Constants.STATE.ERROR:
                binding.linearError.imageError.setImageResource(R.drawable.illustration_popsicle);
                binding.linearError.textErrorTitle.setText(R.string.error_unknown);
                binding.linearError.textErrorSubtitle.setText(R.string.error_undefined);
                emptyStateHelper.clearState();
                break;
            case Constants.STATE.NONE:
                viewIn = binding.scrollMasterProducts;
                viewOut = binding.linearError.linearError;
                break;
        }

        animUtil.replaceViews(viewIn, viewOut, animated);
    }

    private void download() {
        binding.swipeMasterProducts.setRefreshing(true);
        DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);
        queue.append(
                dlHelper.getProductGroups(productGroups -> {
                    this.productGroups = productGroups;
                    productGroupsMap = ArrayUtil.getProductGroupsHashMap(productGroups);
                    setMenuProductGroupFilters();
                }),
                dlHelper.getQuantityUnits(quantityUnits -> {
                    this.quantityUnits = quantityUnits;
                    quantityUnitsMap = ArrayUtil.getQuantityUnitsHashMap(quantityUnits);
                }),
                dlHelper.getProducts(products -> this.products = products)
        );
        if(isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
            queue.append(
                    dlHelper.getLocations(locations -> {
                        this.locations = locations;
                        locationsMap = ArrayUtil.getLocationsHashMap(locations);
                    })
            );
        }
        queue.start();
    }

    private void onQueueEmpty() {
        binding.swipeMasterProducts.setRefreshing(false);
        filterProducts();
    }

    private void onDownloadError(VolleyError error) {
        binding.swipeMasterProducts.setRefreshing(false);
        setError(Constants.STATE.ERROR, true);
        if(debug) Log.i(TAG, "onDownloadError: " + error);
    }

    private void filterProducts() {
        filteredProducts = products;
        // PRODUCT GROUP
        if(filterProductGroupId != -1) {
            ArrayList<Product> tempProducts = new ArrayList<>();
            for(Product product : filteredProducts) {
                String groupId = product.getProductGroupId();
                if(groupId == null || groupId.isEmpty()) continue;
                if(filterProductGroupId == Integer.parseInt(groupId)) {
                    tempProducts.add(product);
                }
            }
            filteredProducts = tempProducts;
        }
        // SEARCH
        if(!search.isEmpty()) { // active search
            searchProducts(search);
        } else {
            // EMPTY STATES
            if(filteredProducts.isEmpty() && errorState.equals(Constants.STATE.NONE)) {
                if(filterProductGroupId != -1) {
                    emptyStateHelper.setNoFilterResults();
                } else {
                    emptyStateHelper.setEmpty();
                }
            } else {
                emptyStateHelper.clearState();
            }
            // SORTING
            if(displayedProducts != filteredProducts || isRestoredInstance) {
                displayedProducts = filteredProducts;
                sortProducts(sortAscending);
            }
            isRestoredInstance = false;
        }
        if(debug) Log.i(TAG, "filterProducts: filteredProducts = " + filteredProducts);
    }

    private void searchProducts(String search) {
        search = search.toLowerCase();
        if(debug) Log.i(TAG, "searchProducts: search = " + search);
        this.search = search;
        if(search.isEmpty()) {
            filterProducts();
        } else { // only if search contains something
            ArrayList<Product> searchedProducts = new ArrayList<>();
            for(Product product : filteredProducts) {
                String name = product.getName();
                String description = product.getDescription();
                name = name != null ? name.toLowerCase() : "";
                description = description != null ? description.toLowerCase() : "";
                if(name.contains(search) || description.contains(search)) {
                    searchedProducts.add(product);
                }
            }
            if(searchedProducts.isEmpty() && errorState.equals(Constants.STATE.NONE)) {
                emptyStateHelper.setNoSearchResults();
            } else {
                emptyStateHelper.clearState();
            }
            if(displayedProducts != searchedProducts) {
                displayedProducts = searchedProducts;
                sortProducts(sortAscending);
            }
            if(debug) Log.i(TAG, "searchProducts: searchedProducts = " + searchedProducts);
        }
    }

    private void filterProductGroup(ProductGroup productGroup) {
        if(filterProductGroupId != productGroup.getId()) {
            if(debug) Log.i(TAG, "filterProductGroup: " + productGroup);
            filterProductGroupId = productGroup.getId();
            if(inputChipFilterProductGroup != null) {
                inputChipFilterProductGroup.changeText(productGroup.getName());
            } else {
                inputChipFilterProductGroup = new InputChip(
                        activity,
                        productGroup.getName(),
                        R.drawable.ic_round_category,
                        true,
                        () -> {
                            filterProductGroupId = -1;
                            inputChipFilterProductGroup = null;
                            filterProducts();
                        });
                binding.linearMasterProductsFilterContainer.addView(inputChipFilterProductGroup);
            }
            filterProducts();
        } else {
            if(debug) Log.i(TAG, "filterProductGroup: " + productGroup + " already filtered");
        }
    }

    /**
     * Sets the product group filter without filtering
     */
    private void updateProductGroupFilter(int filterProductGroupId) {
        ProductGroup productGroup = productGroupsMap.get(filterProductGroupId);
        if(productGroup == null) return;

        this.filterProductGroupId = filterProductGroupId;
        if(inputChipFilterProductGroup != null) {
            inputChipFilterProductGroup.changeText(productGroup.getName());
        } else {
            inputChipFilterProductGroup = new InputChip(
                    activity,
                    productGroup.getName(),
                    R.drawable.ic_round_category,
                    true,
                    () -> {
                        this.filterProductGroupId = -1;
                        inputChipFilterProductGroup = null;
                        filterProducts();
                    });
            binding.linearMasterProductsFilterContainer.addView(inputChipFilterProductGroup);
        }
    }

    private void sortProducts(boolean ascending) {
        if(debug) Log.i(TAG, "sortProducts: sort by name, ascending = " + ascending);
        sortAscending = ascending;
        SortUtil.sortProductsByName(displayedProducts, ascending);
        refreshAdapter(new MasterProductAdapter(displayedProducts, this));
    }

    private void refreshAdapter(MasterProductAdapter adapter) {
        masterProductAdapter = adapter;
        binding.recyclerMasterProducts.animate().alpha(0).setDuration(150).withEndAction(() -> {
            binding.recyclerMasterProducts.setAdapter(adapter);
            binding.recyclerMasterProducts.animate().alpha(1).setDuration(150).start();
        }).start();
    }

    /**
     * Returns index in the displayed products.
     * Used for providing a safe and up-to-date value
     * e.g. when the products are filtered/sorted before server responds
     */
    private int getProductPosition(int productId) {
        for(int i = 0; i < displayedProducts.size(); i++) {
            if(displayedProducts.get(i).getId() == productId) {
                return i;
            }
        }
        return -1;
    }

    private void showMessage(String message) {
        activity.showMessage(
                Snackbar.make(
                        activity.binding.frameMainContainer,
                        message,
                        Snackbar.LENGTH_SHORT
                )
        );
    }

    private void setMenuProductGroupFilters() {
        MenuItem menuItem = activity.getBottomMenu().findItem(R.id.action_filter);
        if(menuItem != null) {
            SubMenu menuProductGroups = menuItem.getSubMenu();
            menuProductGroups.clear();
            SortUtil.sortProductGroupsByName(productGroups, true);
            for(ProductGroup productGroup : productGroups) {
                menuProductGroups.add(productGroup.getName()).setOnMenuItemClickListener(item -> {
                    filterProductGroup(productGroup);
                    return true;
                });
            }
            menuItem.setVisible(!productGroups.isEmpty());
        }
    }

    private void setMenuSorting() {
        MenuItem sortAscending = activity.getBottomMenu().findItem(R.id.action_sort_ascending);
        sortAscending.setChecked(this.sortAscending);
        sortAscending.setOnMenuItemClickListener(item -> {
            item.setChecked(!item.isChecked());
            sortProducts(item.isChecked());
            return true;
        });
    }

    public void setUpBottomMenu() {
        setMenuProductGroupFilters();
        setMenuSorting();
        MenuItem search = activity.getBottomMenu().findItem(R.id.action_search);
        if(search != null) {
            search.setOnMenuItemClickListener(item -> {
                IconUtil.start(item);
                setUpSearch();
                return true;
            });
        }
    }

    @Override
    public void onItemRowClicked(int position) {
        if(clickUtil.isDisabled()) return;
        showProductSheet(displayedProducts.get(position));
    }

    public void editProduct(Product product) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.EDIT);
        bundle.putParcelable(Constants.ARGUMENT.PRODUCT, product);
        activity.replaceFragment(
                Constants.UI.MASTER_PRODUCT_SIMPLE,
                bundle,
                true
        );
    }

    private void showProductSheet(Product product) {
        if(product == null) return;
        Location location = locationsMap.get(product.getLocationId());
        QuantityUnit quantityUnitPurchase = quantityUnitsMap.get(product.getQuIdPurchase());
        QuantityUnit quantityUnitStock = quantityUnitsMap.get(product.getQuIdStock());
        ProductGroup productGroup = NumUtil.isStringInt(product.getProductGroupId())
                ? productGroupsMap.get(Integer.parseInt(product.getProductGroupId()))
                : null;

        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.ARGUMENT.PRODUCT, product);
        bundle.putParcelable(Constants.ARGUMENT.LOCATION, location);
        bundle.putParcelable(Constants.ARGUMENT.QUANTITY_UNIT_PURCHASE, quantityUnitPurchase);
        bundle.putParcelable(Constants.ARGUMENT.QUANTITY_UNIT_STOCK, quantityUnitStock);
        bundle.putParcelable(Constants.ARGUMENT.PRODUCT_GROUP, productGroup);

        activity.showBottomSheet(new MasterProductBottomSheetDialogFragment(), bundle);
    }

    private void setUpSearch() {
        if(search.isEmpty()) { // only if no search is active
            appBarBehavior.switchToSecondary();
            binding.editTextMasterProductsSearch.setText("");
        }
        binding.textInputMasterProductsSearch.requestFocus();
        activity.showKeyboard(binding.editTextMasterProductsSearch);

        activity.setUI(Constants.UI.MASTER_PRODUCTS_SEARCH);
    }

    public void dismissSearch() {
        appBarBehavior.switchToPrimary();
        activity.hideKeyboard();
        binding.editTextMasterProductsSearch.setText("");
        filterProducts();

        activity.setUI(Constants.UI.MASTER_PRODUCTS_DEFAULT);
    }

    public void checkForStock(Product product) {
        if(product == null) return;
        dlHelper.getProductDetails(product.getId(), productDetails -> {
            if(productDetails != null && productDetails.getStockAmount() == 0) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(Constants.ARGUMENT.PRODUCT, product);
                bundle.putString(Constants.ARGUMENT.TYPE, Constants.ARGUMENT.PRODUCT);
                activity.showBottomSheet(new MasterDeleteBottomSheetDialogFragment(), bundle);
            } else {
                showMessage(activity.getString(R.string.msg_master_delete_stock));
            }
        }, error -> showMessage(activity.getString(R.string.error_undefined)))
                .perform(dlHelper.getUuid());
    }

    public void deleteProduct(Product product) {
        if(product == null) return;
        dlHelper.deleteProduct(product.getId(), response -> {
            int index = getProductPosition(product.getId());
            if(index != -1) {
                displayedProducts.remove(index);
                masterProductAdapter.notifyItemRemoved(index);
            } else {
                refresh();  // product not found, fall back to complete refresh
            }
        }, error -> showMessage(activity.getString(R.string.error_undefined)));
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
