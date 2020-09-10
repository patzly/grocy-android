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
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.adapter.MasterProductGroupAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterProductGroupsBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterProductGroupBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.EmptyStateHelper;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.util.AnimUtil;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;

public class MasterProductGroupsFragment extends BaseFragment
        implements MasterProductGroupAdapter.MasterProductGroupAdapterListener {

    private final static String TAG = Constants.UI.MASTER_PRODUCT_GROUPS;

    private MainActivity activity;
    private Gson gson;
    private GrocyApi grocyApi;
    private AppBarBehavior appBarBehavior;
    private DownloadHelper dlHelper;
    private MasterProductGroupAdapter masterProductGroupAdapter;
    private FragmentMasterProductGroupsBinding binding;
    private ClickUtil clickUtil;
    private AnimUtil animUtil;
    private EmptyStateHelper emptyStateHelper;

    private ArrayList<ProductGroup> productGroups;
    private ArrayList<ProductGroup> filteredProductGroups;
    private ArrayList<ProductGroup> displayedProductGroups;
    private ArrayList<Product> products;

    private String search;
    private String errorState;
    private boolean sortAscending;
    private boolean isRestoredInstance;
    private boolean debug;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentMasterProductGroupsBinding.inflate(
                inflater, container, false
        );
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
            binding.recyclerMasterProductGroups.animate().cancel();
            binding = null;
        }
        dlHelper.destroy();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if(isHidden()) return;

        activity = (MainActivity) getActivity();
        assert activity != null;

        // PREFERENCES

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        // UTILS

        clickUtil = new ClickUtil();
        animUtil = new AnimUtil();

        // WEB

        dlHelper = new DownloadHelper(activity, TAG);
        grocyApi = activity.getGrocy();
        gson = new Gson();

        // VARIABLES

        productGroups = new ArrayList<>();
        filteredProductGroups = new ArrayList<>();
        displayedProductGroups = new ArrayList<>();
        products = new ArrayList<>();

        search = "";
        errorState = Constants.STATE.NONE;
        sortAscending = true;
        isRestoredInstance = savedInstanceState != null;

        // VIEWS

        binding.frameMasterProductGroupsBack.setOnClickListener(v -> activity.onBackPressed());
        binding.frameMasterProductGroupsSearchClose.setOnClickListener(v -> dismissSearch());
        binding.editTextMasterProductGroupsSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                search = s.toString();
            }
        });
        binding.editTextMasterProductGroupsSearch.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        Editable search = binding.editTextMasterProductGroupsSearch.getText();
                        searchProductGroups(search != null ? search.toString() : "");
                        activity.hideKeyboard();
                        return true;
                    } return false;
                });

        emptyStateHelper = new EmptyStateHelper(this, binding.linearEmpty);

        appBarBehavior = new AppBarBehavior(
                activity,
                R.id.linear_master_product_groups_app_bar_default,
                R.id.linear_master_product_groups_app_bar_search
        );

        binding.swipeMasterProductGroups.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        binding.swipeMasterProductGroups.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        binding.swipeMasterProductGroups.setOnRefreshListener(this::refresh);

        binding.recyclerMasterProductGroups.setLayoutManager(
                new LinearLayoutManager(
                        activity,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );
        binding.recyclerMasterProductGroups.setItemAnimator(new DefaultItemAnimator());
        binding.recyclerMasterProductGroups.setAdapter(new MasterPlaceholderAdapter());

        if(savedInstanceState == null) {
            load();
        } else {
            restoreSavedInstanceState(savedInstanceState);
        }

        // UPDATE UI
        updateUI((getArguments() == null
                || getArguments().getBoolean(Constants.ARGUMENT.ANIMATED, true))
                && savedInstanceState == null);
    }

    private void updateUI(boolean animated) {
        activity.showHideDemoIndicator(this, animated);
        activity.getScrollBehavior().setUpScroll(R.id.scroll_master_product_groups);
        activity.getScrollBehavior().setHideOnScroll(true);
        activity.updateBottomAppBar(
                Constants.FAB.POSITION.CENTER,
                R.menu.menu_master_items,
                animated,
                this::setUpBottomMenu
        );
        activity.updateFab(
                R.drawable.ic_round_add_anim,
                R.string.action_add,
                Constants.FAB.TAG.ADD,
                animated,
                () -> navigate(MasterProductGroupsFragmentDirections
                        .actionMasterProductGroupsFragmentToMasterProductGroupFragment())
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(isHidden()) return;

        outState.putParcelableArrayList("productGroups", productGroups);
        outState.putParcelableArrayList("filteredProductGroups", filteredProductGroups);
        outState.putParcelableArrayList("displayedProductGroups", displayedProductGroups);
        outState.putParcelableArrayList("products", products);

        outState.putString("search", search);
        outState.putString("errorState", errorState);
        outState.putBoolean("sortAscending", sortAscending);

        appBarBehavior.saveInstanceState(outState);
    }

    private void restoreSavedInstanceState(@NonNull Bundle savedInstanceState) {
        if(isHidden()) return;

        errorState = savedInstanceState.getString("errorState", Constants.STATE.NONE);
        setError(errorState, false);

        productGroups = savedInstanceState.getParcelableArrayList("productGroups");
        filteredProductGroups = savedInstanceState.getParcelableArrayList(
                "filteredProductGroups"
        );
        displayedProductGroups = savedInstanceState.getParcelableArrayList(
                "displayedProductGroups"
        );
        products = savedInstanceState.getParcelableArrayList("products");

        search = savedInstanceState.getString("search");
        errorState = savedInstanceState.getString("errorState");
        sortAscending = savedInstanceState.getBoolean("sortAscending");

        appBarBehavior.restoreInstanceState(savedInstanceState);

        binding.swipeMasterProductGroups.setRefreshing(false);

        // SEARCH
        search = savedInstanceState.getString("search", "");
        binding.editTextMasterProductGroupsSearch.setText(search);

        // FILTERS
        isRestoredInstance = true;
        filterProductGroups();
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
            binding.swipeMasterProductGroups.setRefreshing(false);
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

        binding.relativeError.buttonErrorRetry.setOnClickListener(v -> refresh());

        View viewIn = binding.relativeError.relativeError;
        View viewOut = binding.scrollMasterProductGroups;

        switch (state) {
            case Constants.STATE.OFFLINE:
                binding.relativeError.imageError.setImageResource(R.drawable.illustration_broccoli);
                binding.relativeError.textErrorTitle.setText(R.string.error_offline);
                binding.relativeError.textErrorSubtitle.setText(R.string.error_offline_subtitle);
                emptyStateHelper.clearState();
                break;
            case Constants.STATE.ERROR:
                binding.relativeError.imageError.setImageResource(R.drawable.illustration_popsicle);
                binding.relativeError.textErrorTitle.setText(R.string.error_unknown);
                binding.relativeError.textErrorSubtitle.setText(R.string.error_undefined);
                emptyStateHelper.clearState();
                break;
            case Constants.STATE.NONE:
                viewIn = binding.scrollMasterProductGroups;
                viewOut = binding.relativeError.relativeError;
                break;
        }

        animUtil.replaceViews(viewIn, viewOut, animated);
    }

    private void download() {
        binding.swipeMasterProductGroups.setRefreshing(true);
        downloadProductGroups();
        downloadProducts();
    }

    private void downloadProductGroups() {
        dlHelper.get(
                grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_GROUPS),
                response -> {
                    productGroups = gson.fromJson(
                            response,
                            new TypeToken<List<ProductGroup>>(){}.getType()
                    );
                    if(debug) Log.i(
                            TAG, "downloadProductGroups: productGroups = " + productGroups
                    );
                    binding.swipeMasterProductGroups.setRefreshing(false);
                    filterProductGroups();
                },
                error -> {
                    binding.swipeMasterProductGroups.setRefreshing(false);
                    setError(Constants.STATE.OFFLINE, true);
                    if(debug) Log.e(TAG, "downloadProductGroups: " + error);
                }
        );
    }

    private void downloadProducts() {
        dlHelper.get(
                grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS),
                response -> products = gson.fromJson(
                        response,
                        new TypeToken<List<Product>>(){}.getType()
                ), error -> {}
        );
    }

    private void filterProductGroups() {
        filteredProductGroups = productGroups;
        // SEARCH
        if(!search.isEmpty()) { // active search
            searchProductGroups(search);
        } else {
            // EMPTY STATES
            if(filteredProductGroups.isEmpty() && errorState.equals(Constants.STATE.NONE)) {
                emptyStateHelper.setEmpty();
            } else {
                emptyStateHelper.clearState();
            }
            // SORTING
            if(displayedProductGroups != filteredProductGroups || isRestoredInstance) {
                displayedProductGroups = filteredProductGroups;
                sortProductGroups();
            }
            isRestoredInstance = false;
        }
        if(debug) Log.i(
                TAG, "filterProductGroups: filteredProductGroups = " + filteredProductGroups
        );
    }

    private void searchProductGroups(String search) {
        search = search.toLowerCase();
        if(debug) Log.i(TAG, "searchProductGroups: search = " + search);
        this.search = search;
        if(search.isEmpty()) {
            filterProductGroups();
        } else { // only if search contains something
            ArrayList<ProductGroup> searchedProductGroups = new ArrayList<>();
            for(ProductGroup productGroup : filteredProductGroups) {
                String name = productGroup.getName();
                String description = productGroup.getDescription();
                name = name != null ? name.toLowerCase() : "";
                description = description != null ? description.toLowerCase() : "";
                if(name.contains(search) || description.contains(search)) {
                    searchedProductGroups.add(productGroup);
                }
            }
            if(searchedProductGroups.isEmpty() && errorState.equals(Constants.STATE.NONE)) {
                emptyStateHelper.setNoSearchResults();
            } else {
                emptyStateHelper.clearState();
            }
            if(displayedProductGroups != searchedProductGroups) {
                displayedProductGroups = searchedProductGroups;
                sortProductGroups();
            }
            if(debug) Log.i(TAG, "searchProductGroups: products = " + searchedProductGroups);
        }
    }

    private void sortProductGroups() {
        if(debug) Log.i(TAG, "sortProductGroups: sort by name, ascending = " + sortAscending);
        SortUtil.sortProductGroupsByName(displayedProductGroups, sortAscending);
        refreshAdapter(new MasterProductGroupAdapter(displayedProductGroups, this));
    }

    private void refreshAdapter(MasterProductGroupAdapter adapter) {
        masterProductGroupAdapter = adapter;
        binding.recyclerMasterProductGroups.animate().alpha(0).setDuration(150).withEndAction(() -> {
            binding.recyclerMasterProductGroups.setAdapter(adapter);
            binding.recyclerMasterProductGroups.animate().alpha(1).setDuration(150).start();
        }).start();
    }

    /**
     * Returns index in the displayed items.
     * Used for providing a safe and up-to-date value
     * e.g. when the items are filtered/sorted before server responds
     */
    private int getProductGroupPosition(int productGroupId) {
        for(int i = 0; i < displayedProductGroups.size(); i++) {
            if(displayedProductGroups.get(i).getId() == productGroupId) {
                return i;
            }
        }
        return 0;
    }

    private void showErrorMessage() {
        activity.showMessage(
                Snackbar.make(
                        activity.binding.frameMainContainer,
                        activity.getString(R.string.error_undefined),
                        Snackbar.LENGTH_SHORT
                )
        );
    }

    private void setMenuSorting() {
        MenuItem itemSort = activity.getBottomMenu().findItem(R.id.action_sort_ascending);
        itemSort.setIcon(
                sortAscending
                        ? R.drawable.ic_round_sort_desc_to_asc_anim
                        : R.drawable.ic_round_sort_asc_to_desc
        );
        itemSort.getIcon().setAlpha(255);
        itemSort.setOnMenuItemClickListener(item -> {
            sortAscending = !sortAscending;
            itemSort.setIcon(
                    sortAscending
                            ? R.drawable.ic_round_sort_asc_to_desc
                            : R.drawable.ic_round_sort_desc_to_asc_anim
            );
            itemSort.getIcon().setAlpha(255);
            IconUtil.start(item);
            sortProductGroups();
            return true;
        });
    }

    public void setUpBottomMenu() {
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
        showProductGroupSheet(displayedProductGroups.get(position));
    }

    public void editProductGroup(ProductGroup productGroup) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.ARGUMENT.PRODUCT_GROUP, productGroup);
        activity.replaceFragment(Constants.UI.MASTER_PRODUCT_GROUP, bundle, true);
    }

    private void showProductGroupSheet(ProductGroup productGroup) {
        if(productGroup != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(Constants.ARGUMENT.PRODUCT_GROUP, productGroup);
            activity.showBottomSheet(new MasterProductGroupBottomSheet(), bundle);
        }
    }

    private void setUpSearch() {
        if(search.isEmpty()) { // only if no search is active
            appBarBehavior.switchToSecondary();
            binding.editTextMasterProductGroupsSearch.setText("");
        }
        binding.textInputMasterProductGroupsSearch.requestFocus();
        activity.showKeyboard(binding.editTextMasterProductGroupsSearch);

        setIsSearchVisible(true);
    }

    @Override
    public void dismissSearch() {
        appBarBehavior.switchToPrimary();
        activity.hideKeyboard();
        binding.editTextMasterProductGroupsSearch.setText("");
        filterProductGroups();

        setIsSearchVisible(false);
    }

    public void checkForUsage(ProductGroup productGroup) {
        if(!products.isEmpty()) {
            for(Product product : products) {
                if(product.getProductGroupId() == null) continue;
                if(product.getProductGroupId().equals(String.valueOf(productGroup.getId()))) {
                    activity.showMessage(
                            Snackbar.make(
                                    activity.binding.frameMainContainer,
                                    activity.getString(
                                            R.string.msg_master_delete_usage,
                                            activity.getString(R.string.property_product_group)
                                    ),
                                    Snackbar.LENGTH_LONG
                            )
                    );
                    return;
                }
            }
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.ARGUMENT.PRODUCT_GROUP, productGroup);
        bundle.putString(Constants.ARGUMENT.TYPE, Constants.ARGUMENT.PRODUCT_GROUP);
        activity.showBottomSheet(new MasterDeleteBottomSheet(), bundle);
    }

    public void deleteProductGroup(ProductGroup productGroup) {
        dlHelper.delete(
                grocyApi.getObject(GrocyApi.ENTITY.PRODUCT_GROUPS, productGroup.getId()),
                response -> {
                    int index = getProductGroupPosition(productGroup.getId());
                    if(index != -1) {
                        displayedProductGroups.remove(index);
                        masterProductGroupAdapter.notifyItemRemoved(index);
                    } else {
                        // productGroup not found, fall back to complete refresh
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
