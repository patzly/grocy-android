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
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.adapter.MasterStoreAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterStoresBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterStoreBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class MasterStoresFragment extends Fragment
        implements MasterStoreAdapter.MasterStoreAdapterListener {

    private final static String TAG = Constants.UI.MASTER_STORES;
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private AppBarBehavior appBarBehavior;
    private WebRequest request;
    private MasterStoreAdapter masterStoreAdapter;
    private FragmentMasterStoresBinding binding;
    private ClickUtil clickUtil = new ClickUtil();

    private ArrayList<Store> stores = new ArrayList<>();
    private ArrayList<Store> filteredStores = new ArrayList<>();
    private ArrayList<Store> displayedStores = new ArrayList<>();
    private ArrayList<Product> products = new ArrayList<>();

    private String search = "";
    private boolean sortAscending = true;

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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (MainActivity) getActivity();
        assert activity != null;

        // WEB REQUESTS

        request = new WebRequest(activity.getRequestQueue());
        grocyApi = activity.getGrocy();

        // INITIALIZE VIEWS

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

        load();

        // UPDATE UI

        activity.updateUI(Constants.UI.MASTER_STORES_DEFAULT, TAG);
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
            binding.swipeMasterStores.setRefreshing(false);
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

    private void setError(boolean isError, boolean animated) {
        binding.linearError.buttonErrorRetry.setOnClickListener(v -> refresh());
        // TODO: different errors
        if(animated) {
            View viewOut = isError ? binding.scrollMasterStores : binding.linearError.linearError;
            View viewIn = isError ? binding.linearError.linearError : binding.scrollMasterStores;
            if(viewOut.getVisibility() == View.VISIBLE && viewIn.getVisibility() == View.GONE) {
                viewOut.animate().alpha(0).setDuration(150).withEndAction(() -> {
                    viewIn.setAlpha(0);
                    viewOut.setVisibility(View.GONE);
                    viewIn.setVisibility(View.VISIBLE);
                    viewIn.animate().alpha(1).setDuration(150).start();
                }).start();
            }
        } else {
            binding.scrollMasterStores.setVisibility(isError ? View.GONE : View.VISIBLE);
            binding.linearError.linearError.setVisibility(isError ? View.VISIBLE : View.GONE);
        }
    }

    private void download() {
        binding.swipeMasterStores.setRefreshing(true);
        downloadStores();
        downloadProducts();
    }

    private void downloadStores() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.STORES),
                response -> {
                    stores = gson.fromJson(
                            response,
                            new TypeToken<List<Store>>(){}.getType()
                    );
                    if(DEBUG) Log.i(TAG, "downloadStores: stores = " + stores);
                    binding.swipeMasterStores.setRefreshing(false);
                    filterStores();
                },
                error -> {
                    binding.swipeMasterStores.setRefreshing(false);
                    setError(true, true);
                    Log.e(TAG, "downloadStores: " + error);
                }
        );
    }

    private void downloadProducts() {
        request.get(
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
            if(displayedStores != filteredStores) {
                displayedStores = filteredStores;
                sortStores();
            }
        }
        if(DEBUG) Log.i(TAG, "filterStores: filteredStores = " + filteredStores);
    }

    private void searchStores(String search) {
        search = search.toLowerCase();
        if(DEBUG) Log.i(TAG, "searchStores: search = " + search);
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
            if(displayedStores != searchedStores) {
                displayedStores = searchedStores;
                sortStores();
            }
            if(DEBUG) Log.i(TAG, "searchStores: searchedStores = " + searchedStores);
        }
    }

    private void sortStores() {
        if(DEBUG) Log.i(TAG, "sortStores: sort by name, ascending = " + sortAscending);
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
        activity.showMessage(
                Snackbar.make(
                        activity.binding.frameMainContainer,
                        activity.getString(R.string.msg_error),
                        Snackbar.LENGTH_SHORT
                )
        );
    }

    private void setMenuSorting() {
        MenuItem itemSort = activity.getBottomMenu().findItem(R.id.action_sort_ascending);
        itemSort.setIcon(R.drawable.ic_round_sort_desc_to_asc_anim);
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
            activity.showBottomSheet(new MasterStoreBottomSheetDialogFragment(), bundle);
        }
    }

    public void setUpSearch() {
        if(search.isEmpty()) { // only if no search is active
            appBarBehavior.switchToSecondary();
            binding.editTextMasterStoresSearch.setText("");
        }
        binding.textInputMasterStoresSearch.requestFocus();
        activity.showKeyboard(binding.editTextMasterStoresSearch);

        activity.setUI(Constants.UI.MASTER_STORES_SEARCH);
    }

    public void dismissSearch() {
        appBarBehavior.switchToPrimary();
        activity.hideKeyboard();
        search = "";
        filterStores();

        activity.setUI(Constants.UI.MASTER_STORES_DEFAULT);
    }

    public void checkForUsage(Store store) {
        if(!products.isEmpty()) {
            for(Product product : products) {
                if(product.getStoreId() == null) continue;
                if(product.getStoreId().equals(String.valueOf(store.getId()))) {
                    activity.showMessage(
                            Snackbar.make(
                                    activity.binding.frameMainContainer,
                                    activity.getString(
                                            R.string.msg_master_delete_usage,
                                            activity.getString(R.string.type_store)
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
        activity.showBottomSheet(new MasterDeleteBottomSheetDialogFragment(), bundle);
    }

    public void deleteStore(Store store) {
        request.delete(
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
