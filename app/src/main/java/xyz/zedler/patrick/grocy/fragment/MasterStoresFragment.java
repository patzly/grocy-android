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
import xyz.zedler.patrick.grocy.adapter.MasterStoreAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterStoresBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterStoreBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.EmptyStateHelper;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.AnimUtil;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;

public class MasterStoresFragment extends BaseFragment
        implements MasterStoreAdapter.MasterStoreAdapterListener {

    private final static String TAG = Constants.UI.MASTER_STORES;

    private MainActivity activity;
    private Gson gson;
    private GrocyApi grocyApi;
    private AppBarBehavior appBarBehavior;
    private DownloadHelper dlHelper;
    private MasterStoreAdapter masterStoreAdapter;
    private FragmentMasterStoresBinding binding;
    private ClickUtil clickUtil;
    private AnimUtil animUtil;
    private EmptyStateHelper emptyStateHelper;

    private ArrayList<Store> stores;
    private ArrayList<Store> filteredStores;
    private ArrayList<Store> displayedStores;
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
        binding = FragmentMasterStoresBinding.inflate(inflater, container, false);
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
            binding.recyclerMasterStores.animate().cancel();
            binding = null;
        }
        dlHelper.destroy();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if(isHidden()) return;

        activity = (MainActivity) getActivity();
        assert activity != null;

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

        stores = new ArrayList<>();
        filteredStores = new ArrayList<>();
        displayedStores = new ArrayList<>();
        products = new ArrayList<>();

        search = "";
        errorState = Constants.STATE.NONE;
        sortAscending = true;
        isRestoredInstance = savedInstanceState != null;

        // VIEWS

        binding.frameMasterStoresBack.setOnClickListener(v -> activity.onBackPressed());
        binding.frameMasterStoresSearchClose.setOnClickListener(v -> dismissSearch());
        binding.editTextMasterStoresSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                search = s.toString();
            }
        });
        binding.editTextMasterStoresSearch.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        Editable search = binding.editTextMasterStoresSearch.getText();
                        searchStores(search != null ? search.toString() : "");
                        activity.hideKeyboard();
                        return true;
                    } return false;
                });
        emptyStateHelper = new EmptyStateHelper(this, binding.linearEmpty);

        // APP BAR BEHAVIOR

        appBarBehavior = new AppBarBehavior(
                activity,
                R.id.linear_master_stores_app_bar_default,
                R.id.linear_master_stores_app_bar_search
        );

        // SWIPE REFRESH

        binding.swipeMasterStores.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        binding.swipeMasterStores.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        binding.swipeMasterStores.setOnRefreshListener(this::refresh);

        binding.recyclerMasterStores.setLayoutManager(
                new LinearLayoutManager(
                        activity,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );
        binding.recyclerMasterStores.setItemAnimator(new DefaultItemAnimator());
        binding.recyclerMasterStores.setAdapter(new MasterPlaceholderAdapter());

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
        activity.getScrollBehavior().setUpScroll(R.id.scroll_master_stores);
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
                () -> navigate(MasterStoresFragmentDirections
                        .actionMasterStoresFragmentToMasterStoreFragment())
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(isHidden()) return;

        outState.putParcelableArrayList("stores", stores);
        outState.putParcelableArrayList("filteredStores", filteredStores);
        outState.putParcelableArrayList("displayedStores", displayedStores);
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

        stores = savedInstanceState.getParcelableArrayList("stores");
        filteredStores = savedInstanceState.getParcelableArrayList("filteredStores");
        displayedStores = savedInstanceState.getParcelableArrayList("displayedStores");
        products = savedInstanceState.getParcelableArrayList("products");

        search = savedInstanceState.getString("search");
        errorState = savedInstanceState.getString("errorState");
        sortAscending = savedInstanceState.getBoolean("sortAscending");

        appBarBehavior.restoreInstanceState(savedInstanceState);

        binding.swipeMasterStores.setRefreshing(false);

        // SEARCH
        search = savedInstanceState.getString("search", "");
        binding.editTextMasterStoresSearch.setText(search);

        // FILTERS
        isRestoredInstance = true;
        filterStores();
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
            binding.swipeMasterStores.setRefreshing(false);
            activity.showSnackbar(
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
        View viewOut = binding.scrollMasterStores;

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
                viewIn = binding.scrollMasterStores;
                viewOut = binding.relativeError.relativeError;
                break;
        }

        animUtil.replaceViews(viewIn, viewOut, animated);
    }

    private void download() {
        binding.swipeMasterStores.setRefreshing(true);
        downloadStores();
        downloadProducts();
    }

    private void downloadStores() {
        dlHelper.get(
                grocyApi.getObjects(GrocyApi.ENTITY.STORES),
                response -> {
                    stores = gson.fromJson(
                            response,
                            new TypeToken<List<Store>>(){}.getType()
                    );
                    if(debug) Log.i(TAG, "downloadStores: stores = " + stores);
                    binding.swipeMasterStores.setRefreshing(false);
                    filterStores();
                },
                error -> {
                    binding.swipeMasterStores.setRefreshing(false);
                    setError(Constants.STATE.OFFLINE, true);
                    if(debug) Log.e(TAG, "downloadStores: " + error);
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

    private void filterStores() {
        filteredStores = stores;
        // SEARCH
        if(!search.isEmpty()) { // active search
            searchStores(search);
        } else {
            // EMPTY STATES
            if(filteredStores.isEmpty() && errorState.equals(Constants.STATE.NONE)) {
                emptyStateHelper.setEmpty();
            } else {
                emptyStateHelper.clearState();
            }
            // SORTING
            if(displayedStores != filteredStores || isRestoredInstance) {
                displayedStores = filteredStores;
                sortStores();
            }
            isRestoredInstance = false;
        }
        if(debug) Log.i(TAG, "filterStores: filteredStores = " + filteredStores);
    }

    private void searchStores(String search) {
        search = search.toLowerCase();
        if(debug) Log.i(TAG, "searchStores: search = " + search);
        this.search = search;
        if(search.isEmpty()) {
            filterStores();
        } else { // only if search contains something
            ArrayList<Store> searchedStores = new ArrayList<>();
            for(Store store : filteredStores) {
                String name = store.getName();
                String description = store.getDescription();
                name = name != null ? name.toLowerCase() : "";
                description = description != null ? description.toLowerCase() : "";
                if(name.contains(search) || description.contains(search)) {
                    searchedStores.add(store);
                }
            }
            if(searchedStores.isEmpty() && errorState.equals(Constants.STATE.NONE)) {
                emptyStateHelper.setNoSearchResults();
            } else {
                emptyStateHelper.clearState();
            }
            if(displayedStores != searchedStores) {
                displayedStores = searchedStores;
                sortStores();
            }
            if(debug) Log.i(TAG, "searchStores: searchedStores = " + searchedStores);
        }
    }

    private void sortStores() {
        if(debug) Log.i(TAG, "sortStores: sort by name, ascending = " + sortAscending);
        SortUtil.sortStoresByName(displayedStores, sortAscending);
        refreshAdapter(new MasterStoreAdapter(displayedStores, this));
    }

    private void refreshAdapter(MasterStoreAdapter adapter) {
        masterStoreAdapter = adapter;
        binding.recyclerMasterStores.animate().alpha(0).setDuration(150).withEndAction(() -> {
            binding.recyclerMasterStores.setAdapter(adapter);
            binding.recyclerMasterStores.animate().alpha(1).setDuration(150).start();
        }).start();
    }

    /**
     * Returns index in the displayed items.
     * Used for providing a safe and up-to-date value
     * e.g. when the items are filtered/sorted before server responds
     */
    private int getStorePosition(int storeId) {
        for(int i = 0; i < displayedStores.size(); i++) {
            if(displayedStores.get(i).getId() == storeId) {
                return i;
            }
        }
        return 0;
    }

    private void showErrorMessage() {
        activity.showSnackbar(
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
            sortStores();
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
        showStoreSheet(displayedStores.get(position));
    }

    public void editStore(Store store) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.ARGUMENT.STORE, store);
        activity.replaceFragment(Constants.UI.MASTER_STORE, bundle, true);
    }

    private void showStoreSheet(Store store) {
        if(store != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(Constants.ARGUMENT.STORE, store);
            activity.showBottomSheet(new MasterStoreBottomSheet(), bundle);
        }
    }

    public void setUpSearch() {
        if(search.isEmpty()) { // only if no search is active
            appBarBehavior.switchToSecondary();
            binding.editTextMasterStoresSearch.setText("");
        }
        binding.textInputMasterStoresSearch.requestFocus();
        activity.showKeyboard(binding.editTextMasterStoresSearch);

        setIsSearchVisible(true);
    }

    @Override
    public void dismissSearch() {
        appBarBehavior.switchToPrimary();
        activity.hideKeyboard();
        binding.editTextMasterStoresSearch.setText("");
        filterStores();

        emptyStateHelper.clearState();

        setIsSearchVisible(false);
    }

    public void checkForUsage(Store store) {
        if(!products.isEmpty()) {
            for(Product product : products) {
                if(product.getStoreId() == null) continue;
                if(product.getStoreId().equals(String.valueOf(store.getId()))) {
                    activity.showSnackbar(
                            Snackbar.make(
                                    activity.binding.frameMainContainer,
                                    activity.getString(
                                            R.string.msg_master_delete_usage,
                                            activity.getString(R.string.property_store)
                                    ),
                                    Snackbar.LENGTH_LONG
                            )
                    );
                    return;
                }
            }
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.ARGUMENT.STORE, store);
        bundle.putString(Constants.ARGUMENT.TYPE, Constants.ARGUMENT.STORE);
        activity.showBottomSheet(new MasterDeleteBottomSheet(), bundle);
    }

    public void deleteStore(Store store) {
        dlHelper.delete(
                grocyApi.getObject(GrocyApi.ENTITY.STORES, store.getId()),
                response -> {
                    int index = getStorePosition(store.getId());
                    if(index != -1) {
                        displayedStores.remove(index);
                        masterStoreAdapter.notifyItemRemoved(index);
                    } else {
                        // store not found, fall back to complete refresh
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
