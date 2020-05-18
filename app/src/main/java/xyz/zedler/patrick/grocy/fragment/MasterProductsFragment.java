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
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.VolleyError;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.adapter.MasterProductAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterProductBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.view.InputChip;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class MasterProductsFragment extends Fragment
        implements MasterProductAdapter.MasterProductAdapterListener {

    private final static String TAG = Constants.UI.MASTER_PRODUCTS;
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private AppBarBehavior appBarBehavior;
    private WebRequest request;
    private MasterProductAdapter masterProductAdapter;
    private ClickUtil clickUtil = new ClickUtil();

    private ArrayList<Product> products = new ArrayList<>();
    private ArrayList<Product> filteredProducts = new ArrayList<>();
    private ArrayList<Product> displayedProducts = new ArrayList<>();
    private ArrayList<QuantityUnit> quantityUnits = new ArrayList<>();
    private ArrayList<Location> locations = new ArrayList<>();
    private ArrayList<ProductGroup> productGroups = new ArrayList<>();

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

        // WEB REQUESTS

        request = new WebRequest(activity.getRequestQueue());
        grocyApi = activity.getGrocy();

        // INITIALIZE VIEWS

        activity.findViewById(R.id.frame_back_master_products).setOnClickListener(
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

        appBarBehavior = new AppBarBehavior(activity, R.id.linear_app_bar_master_products_default);

        // SWIPE REFRESH

        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        swipeRefreshLayout.setOnRefreshListener(this::refresh);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        activity,
                        LinearLayoutManager.VERTICAL,
                        false
                )
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
        downloadQuantityUnits();
        downloadLocations();
        downloadProductGroups();
        downloadProducts();
    }

    private void downloadQuantityUnits() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNITS),
                TAG,
                response -> {
                    quantityUnits = gson.fromJson(
                            response,
                            new TypeToken<List<QuantityUnit>>(){}.getType()
                    );
                    if(DEBUG) Log.i(
                            TAG, "downloadQuantityUnits: quantityUnits = " + quantityUnits
                    );
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
                            new TypeToken<List<Location>>(){}.getType()
                    );
                    if(DEBUG) Log.i(TAG, "downloadLocations: locations = " + locations);
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
                            new TypeToken<List<ProductGroup>>(){}.getType()
                    );
                    if(DEBUG) Log.i(
                            TAG, "downloadProductGroups: productGroups = " + productGroups
                    );
                    setMenuProductGroupFilters();
                },
                this::onDownloadError,
                this::onQueueEmpty
        );
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
                    if(DEBUG) Log.i(TAG, "downloadProducts: products = " + products);
                },
                this::onDownloadError,
                this::onQueueEmpty
        );
    }

    private void onQueueEmpty() {
        swipeRefreshLayout.setRefreshing(false);
        filterProducts();
    }

    private void onDownloadError(VolleyError error) {
        request.cancelAll(TAG);
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
        } else {
            if(displayedProducts != filteredProducts) {
                displayedProducts = filteredProducts;
                sortProducts(sortAscending);
            }
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

    private QuantityUnit getQuantityUnit(int id) {
        for(QuantityUnit quantityUnit : quantityUnits) {
            if(quantityUnit.getId() == id) {
                return quantityUnit;
            }
        } return null;
    }

    private Location getLocation(int id) {
        for(Location location : locations) {
            if(location.getId() == id) {
                return location;
            }
        } return null;
    }

    private ProductGroup getProductGroup(String id) {
        if(id == null || id.isEmpty()) return null;
        for(ProductGroup productGroup : productGroups) {
            if(productGroup.getId() == Integer.parseInt(id)) {
                return productGroup;
            }
        } return null;
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

    private void setMenuProductGroupFilters() {
        MenuItem menuItem = activity.getBottomMenu().findItem(R.id.action_filter);
        if(menuItem != null) {
            SubMenu menuProductGroups = menuItem.getSubMenu();
            menuProductGroups.clear();
            for(ProductGroup productGroup : productGroups) {
                menuProductGroups.add(productGroup.getName()).setOnMenuItemClickListener(item -> {
                    //if(!uiMode.equals(Constants.UI.STOCK_DEFAULT)) return false;
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
            Location location = getLocation(product.getLocationId());
            QuantityUnit quantityUnitPurchase = getQuantityUnit(product.getQuIdPurchase());
            QuantityUnit quantityUnitStock = getQuantityUnit(product.getQuIdPurchase());
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
            appBarBehavior.replaceLayout(R.id.linear_app_bar_master_products_search, true);
            editTextSearch.setText("");
        }
        textInputLayoutSearch.requestFocus();
        activity.showKeyboard(editTextSearch);

        activity.findViewById(R.id.frame_close_master_products_search).setOnClickListener(
                v -> dismissSearch()
        );

        activity.updateUI(Constants.UI.MASTER_PRODUCTS_SEARCH, TAG);
    }

    public void dismissSearch() {
        appBarBehavior.replaceLayout(R.id.linear_app_bar_master_products_default, true);
        activity.hideKeyboard();
        search = "";
        filterProducts(); // TODO: buggy animation

        activity.updateUI(Constants.UI.MASTER_PRODUCTS_DEFAULT, TAG);
    }

    public void checkForStock(Product product) {
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
                        activity.showMessage(
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
                    int index = getProductPosition(product.getId());
                    if(index != -1) {
                        displayedProducts.remove(index);
                        masterProductAdapter.notifyItemRemoved(index);
                    } else {
                        // product not found, fall back to complete refresh
                        refresh();
                    }
                },
                error -> showErrorMessage()
        );
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
