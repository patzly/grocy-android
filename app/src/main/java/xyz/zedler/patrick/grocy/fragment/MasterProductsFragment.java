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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.VolleyError;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashMap;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.adapter.MasterProductAdapter;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterProductBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.view.InputChip;

public class MasterProductsFragment extends Fragment
        implements MasterProductAdapter.MasterProductAdapterListener {

    private final static String TAG = Constants.UI.MASTER_PRODUCTS;
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private DownloadHelper downloadHelper;
    private AppBarBehavior appBarBehavior;
    private SharedPreferences sharedPrefs;
    private MasterProductAdapter masterProductAdapter;
    private ClickUtil clickUtil = new ClickUtil();

    private ArrayList<Product> products = new ArrayList<>();
    private ArrayList<Product> filteredProducts = new ArrayList<>();
    private ArrayList<Product> displayedProducts = new ArrayList<>();
    private ArrayList<ProductGroup> productGroups = new ArrayList<>();

    private HashMap<Integer, Location> locations = new HashMap<>();
    private HashMap<Integer, QuantityUnit> quantityUnits = new HashMap<>();

    private String search = "";
    private int filterProductGroupId = -1;
    private boolean sortAscending = true;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextInputLayout textInputLayoutSearch;
    private EditText editTextSearch;
    private InputChip inputChipFilterProductGroup;
    private LinearLayout linearLayoutError, linearLayoutFilterContainer;
    private NestedScrollView scrollView;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_master_products, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (MainActivity) getActivity();
        assert activity != null;

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        downloadHelper = new DownloadHelper(
                activity,
                TAG,
                this::onDownloadError,
                this::onQueueEmpty
        );

        // INITIALIZE VIEWS

        activity.findViewById(R.id.frame_master_products_back).setOnClickListener(
                v -> activity.onBackPressed()
        );
        linearLayoutFilterContainer = activity.findViewById(
                R.id.linear_master_products_filter_container
        );
        linearLayoutError = activity.findViewById(R.id.linear_master_products_error);
        swipeRefreshLayout = activity.findViewById(R.id.swipe_master_products);
        scrollView = activity.findViewById(R.id.scroll_master_products);
        // retry button on offline error page
        activity.findViewById(R.id.button_master_products_error_retry).setOnClickListener(
                v -> refresh()
        );
        recyclerView = activity.findViewById(R.id.recycler_master_products);
        textInputLayoutSearch = activity.findViewById(R.id.text_input_master_products_search);
        editTextSearch = textInputLayoutSearch.getEditText();
        assert editTextSearch != null;
        editTextSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                search = s.toString();
            }
        });
        editTextSearch.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchProducts(editTextSearch.getText().toString());
                activity.hideKeyboard();
                return true;
            } return false;
        });

        // APP BAR BEHAVIOR

        appBarBehavior = new AppBarBehavior(activity, R.id.linear_master_products_app_bar_default);

        // SWIPE REFRESH

        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        swipeRefreshLayout.setOnRefreshListener(this::refresh);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        );
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(new MasterPlaceholderAdapter());

        load();

        // UPDATE UI

        activity.updateUI(Constants.UI.MASTER_PRODUCTS_DEFAULT, TAG);
    }

    private void load() {
        if(activity.isOnline()) {
            download();
        } else {
            setError(true, false);
        }
    }

    private void refresh() {
        if(activity.isOnline()) {
            setError(false, true);
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

    private void setError(boolean isError, boolean animated) {
        // TODO: different errors
        if(animated) {
            View viewOut = isError ? scrollView : linearLayoutError;
            View viewIn = isError ? linearLayoutError : scrollView;
            if(viewOut.getVisibility() == View.VISIBLE && viewIn.getVisibility() == View.GONE) {
                viewOut.animate().alpha(0).setDuration(150).withEndAction(() -> {
                    viewIn.setAlpha(0);
                    viewOut.setVisibility(View.GONE);
                    viewIn.setVisibility(View.VISIBLE);
                    viewIn.animate().alpha(1).setDuration(150).start();
                }).start();
            }
        } else {
            scrollView.setVisibility(isError ? View.GONE : View.VISIBLE);
            linearLayoutError.setVisibility(isError ? View.VISIBLE : View.GONE);
        }
    }

    private void download() {
        swipeRefreshLayout.setRefreshing(true);

        downloadHelper.downloadProductGroups(productGroups -> this.productGroups = productGroups);
        downloadHelper.downloadQuantityUnits(quantityUnits -> {
            for (QuantityUnit q : quantityUnits) this.quantityUnits.put(q.getId(), q);
        });
        downloadHelper.downloadProducts(products -> this.products = products);
        if (isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
            downloadHelper.downloadLocations(locations -> {
                for(Location l : locations) this.locations.put(l.getId(), l);
            });
        }
    }

    private void onQueueEmpty() {
        swipeRefreshLayout.setRefreshing(false);
        filterProducts();
    }

    private void onDownloadError(VolleyError error) {
        swipeRefreshLayout.setRefreshing(false);
        setError(true, true);
        if(DEBUG) Log.i(TAG, "onDownloadError: " + error);
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
        } else if(displayedProducts != filteredProducts) {
            displayedProducts = filteredProducts;
            sortProducts(sortAscending);
        }
        if(DEBUG) Log.i(TAG, "filterProducts: filteredProducts = " + filteredProducts);
    }

    private void searchProducts(String search) {
        search = search.toLowerCase();
        if(DEBUG) Log.i(TAG, "searchProducts: search = " + search);
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
            if(displayedProducts != searchedProducts) {
                displayedProducts = searchedProducts;
                sortProducts(sortAscending);
            }
            if(DEBUG) Log.i(TAG, "searchProducts: searchedProducts = " + searchedProducts);
        }
    }

    private void filterProductGroup(ProductGroup productGroup) {
        if(filterProductGroupId != productGroup.getId()) {
            if(DEBUG) Log.i(TAG, "filterProductGroup: " + productGroup);
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
                linearLayoutFilterContainer.addView(inputChipFilterProductGroup);
            }
            filterProducts();
        } else {
            if(DEBUG) Log.i(TAG, "filterProductGroup: " + productGroup + " already filtered");
        }
    }

    private void sortProducts(boolean ascending) {
        if(DEBUG) Log.i(TAG, "sortProducts: sort by name, ascending = " + ascending);
        sortAscending = ascending;
        SortUtil.sortProductsByName(displayedProducts, ascending);
        refreshAdapter(new MasterProductAdapter(displayedProducts, this));
    }

    private void refreshAdapter(MasterProductAdapter adapter) {
        masterProductAdapter = adapter;
        recyclerView.animate().alpha(0).setDuration(150).withEndAction(() -> {
            recyclerView.setAdapter(adapter);
            recyclerView.animate().alpha(1).setDuration(150).start();
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

    private ProductGroup getProductGroup(String id) {
        if(id == null || id.isEmpty()) return null;
        for(ProductGroup productGroup : productGroups) {
            if(productGroup.getId() == Integer.parseInt(id)) {
                return productGroup;
            }
        } return null;
    }

    private void showMessage(String message) {
        activity.showMessage(
                Snackbar.make(
                        activity.findViewById(R.id.frame_main_container),
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
        sortAscending.setChecked(true);
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
        if(product != null) {
            Location location = locations.get(product.getLocationId());
            QuantityUnit quantityUnitPurchase = quantityUnits.get(product.getQuIdPurchase());
            QuantityUnit quantityUnitStock = quantityUnits.get(product.getQuIdStock());
            ProductGroup productGroup = getProductGroup(product.getProductGroupId());

            Bundle bundle = new Bundle();
            bundle.putParcelable(Constants.ARGUMENT.PRODUCT, product);
            bundle.putParcelable(Constants.ARGUMENT.LOCATION, location);
            bundle.putParcelable(Constants.ARGUMENT.QUANTITY_UNIT_PURCHASE, quantityUnitPurchase);
            bundle.putParcelable(Constants.ARGUMENT.QUANTITY_UNIT_STOCK, quantityUnitStock);
            bundle.putParcelable(Constants.ARGUMENT.PRODUCT_GROUP, productGroup);

            activity.showBottomSheet(new MasterProductBottomSheetDialogFragment(), bundle);
        }
    }

    private void setUpSearch() {
        if(search.isEmpty()) { // only if no search is active
            appBarBehavior.replaceLayout(R.id.linear_master_products_app_bar_search, true);
            editTextSearch.setText("");
        }
        textInputLayoutSearch.requestFocus();
        activity.showKeyboard(editTextSearch);

        activity.findViewById(R.id.frame_close_master_products_search).setOnClickListener(
                v -> dismissSearch()
        );

        activity.setUI(Constants.UI.MASTER_PRODUCTS_SEARCH);
    }

    public void dismissSearch() {
        appBarBehavior.replaceLayout(R.id.linear_master_products_app_bar_default, true);
        activity.hideKeyboard();
        search = "";
        filterProducts();

        activity.setUI(Constants.UI.MASTER_PRODUCTS_DEFAULT);
    }

    public void checkForStock(Product product) {
        if(product == null) return;
        downloadHelper.downloadProductDetails(product.getId(), productDetails -> {
            if(productDetails != null && productDetails.getStockAmount() == 0) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(Constants.ARGUMENT.PRODUCT, product);
                bundle.putString(Constants.ARGUMENT.TYPE, Constants.ARGUMENT.PRODUCT);
                activity.showBottomSheet(new MasterDeleteBottomSheetDialogFragment(), bundle);
            } else {
                showMessage(activity.getString(R.string.msg_master_delete_stock));
            }
        }, error -> showMessage(activity.getString(R.string.msg_error)));
    }

    public void deleteProduct(Product product) {
        if(product == null) return;
        downloadHelper.deleteProduct(product.getId(), response -> {
            int index = getProductPosition(product.getId());
            if(index != -1) {
                displayedProducts.remove(index);
                masterProductAdapter.notifyItemRemoved(index);
            } else {
                refresh();  // product not found, fall back to complete refresh
            }
        }, error -> showMessage(activity.getString(R.string.msg_error)));
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
