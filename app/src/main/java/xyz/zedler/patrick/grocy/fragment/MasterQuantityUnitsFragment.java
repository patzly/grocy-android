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
import androidx.fragment.app.Fragment;
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
import xyz.zedler.patrick.grocy.adapter.MasterQuantityUnitAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterQuantityUnitsBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterQuantityUnitBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.EmptyStateHelper;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.AnimUtil;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;

public class MasterQuantityUnitsFragment extends Fragment
        implements MasterQuantityUnitAdapter.MasterQuantityUnitAdapterListener {

    private final static String TAG = Constants.UI.MASTER_QUANTITY_UNITS;

    private MainActivity activity;
    private Gson gson;
    private GrocyApi grocyApi;
    private AppBarBehavior appBarBehavior;
    private DownloadHelper dlHelper;
    private MasterQuantityUnitAdapter masterQuantityUnitAdapter;
    private ClickUtil clickUtil;
    private AnimUtil animUtil;
    private EmptyStateHelper emptyStateHelper;
    private FragmentMasterQuantityUnitsBinding binding;

    private ArrayList<QuantityUnit> quantityUnits;
    private ArrayList<QuantityUnit> filteredQuantityUnits;
    private ArrayList<QuantityUnit> displayedQuantityUnits;
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
        binding = FragmentMasterQuantityUnitsBinding.inflate(
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
            binding.recyclerMasterQuantityUnits.animate().cancel();
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

        // WEB REQUESTS

        dlHelper = new DownloadHelper(activity, TAG);
        grocyApi = activity.getGrocy();
        gson = new Gson();

        // INITIALIZE VARIABLES

        quantityUnits = new ArrayList<>();
        filteredQuantityUnits = new ArrayList<>();
        displayedQuantityUnits = new ArrayList<>();
        products = new ArrayList<>();

        search = "";
        errorState = Constants.STATE.NONE;
        sortAscending = true;
        isRestoredInstance = savedInstanceState != null;

        // INITIALIZE VIEWS

        binding.frameMasterQuantityUnitsBack.setOnClickListener(v -> activity.onBackPressed());
        binding.frameMasterQuantityUnitsSearchClose.setOnClickListener(v -> dismissSearch());
        binding.editTextMasterQuantityUnitsSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                search = s.toString();
            }
        });
        binding.editTextMasterQuantityUnitsSearch.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        Editable search = binding.editTextMasterQuantityUnitsSearch.getText();
                        searchQuantityUnits(search != null ? search.toString() : "");
                        activity.hideKeyboard();
                        return true;
                    } return false;
                });
        emptyStateHelper = new EmptyStateHelper(this, binding.linearEmpty);

        // APP BAR BEHAVIOR

        appBarBehavior = new AppBarBehavior(
                activity,
                R.id.linear_master_quantity_units_app_bar_default,
                R.id.linear_master_quantity_units_app_bar_search
        );

        // SWIPE REFRESH

        binding.swipeMasterQuantityUnits.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        binding.swipeMasterQuantityUnits.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        binding.swipeMasterQuantityUnits.setOnRefreshListener(this::refresh);

        binding.recyclerMasterQuantityUnits.setLayoutManager(
                new LinearLayoutManager(
                        activity,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );
        binding.recyclerMasterQuantityUnits.setItemAnimator(new DefaultItemAnimator());
        binding.recyclerMasterQuantityUnits.setAdapter(new MasterPlaceholderAdapter());

        if(savedInstanceState == null) {
            load();
        } else {
            restoreSavedInstanceState(savedInstanceState);
        }

        // UPDATE UI

        activity.updateUI(
                appBarBehavior.isPrimaryLayout()
                        ? Constants.UI.MASTER_QUANTITY_UNITS_DEFAULT
                        : Constants.UI.MASTER_QUANTITY_UNITS_SEARCH,
                savedInstanceState == null,
                TAG
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(isHidden()) return;

        outState.putParcelableArrayList("quantityUnits", quantityUnits);
        outState.putParcelableArrayList("filteredQuantityUnits", filteredQuantityUnits);
        outState.putParcelableArrayList("displayedQuantityUnits", displayedQuantityUnits);
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

        quantityUnits = savedInstanceState.getParcelableArrayList("quantityUnits");
        filteredQuantityUnits = savedInstanceState.getParcelableArrayList(
                "filteredQuantityUnits"
        );
        displayedQuantityUnits = savedInstanceState.getParcelableArrayList(
                "displayedQuantityUnits"
        );
        products = savedInstanceState.getParcelableArrayList("products");

        search = savedInstanceState.getString("search");
        errorState = savedInstanceState.getString("errorState");
        sortAscending = savedInstanceState.getBoolean("sortAscending");

        appBarBehavior.restoreInstanceState(savedInstanceState);

        binding.swipeMasterQuantityUnits.setRefreshing(false);

        // SEARCH
        search = savedInstanceState.getString("search", "");
        binding.editTextMasterQuantityUnitsSearch.setText(search);

        // FILTERS
        isRestoredInstance = true;
        filterQuantityUnits();
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
            binding.swipeMasterQuantityUnits.setRefreshing(false);
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
        View viewOut = binding.scrollMasterQuantityUnits;

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
                viewIn = binding.scrollMasterQuantityUnits;
                viewOut = binding.linearError.linearError;
                break;
        }

        animUtil.replaceViews(viewIn, viewOut, animated);
    }

    private void download() {
        binding.swipeMasterQuantityUnits.setRefreshing(true);
        downloadQuantityUnits();
        downloadProducts();
    }

    private void downloadQuantityUnits() {
        dlHelper.get(
                grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNITS),
                response -> {
                    quantityUnits = gson.fromJson(
                            response,
                            new TypeToken<List<QuantityUnit>>(){}.getType()
                    );
                    if(debug) Log.i(TAG, "downloadQuantityUnits: quantityUnits = " + quantityUnits);
                    binding.swipeMasterQuantityUnits.setRefreshing(false);
                    filterQuantityUnits();
                },
                error -> {
                    binding.swipeMasterQuantityUnits.setRefreshing(false);
                    setError(Constants.STATE.OFFLINE, true);
                    if(debug) Log.e(TAG, "downloadQuantityUnits: " + error);
                }
        );
    }

    private void downloadProducts() {
        dlHelper.get(
                grocyApi.getObjects(GrocyApi.ENTITY.PRODUCTS),
                response -> products = gson.fromJson(
                        response,
                        new TypeToken<ArrayList<Product>>(){}.getType()
                ), error -> {}
        );
    }

    private void filterQuantityUnits() {
        filteredQuantityUnits = quantityUnits;
        // SEARCH
        if(!search.isEmpty()) { // active search
            searchQuantityUnits(search);
        } else {
            // EMPTY STATES
            if(filteredQuantityUnits.isEmpty() && errorState.equals(Constants.STATE.NONE)) {
                emptyStateHelper.setEmpty();
            } else {
                emptyStateHelper.clearState();
            }
            // SORTING
            if(displayedQuantityUnits != filteredQuantityUnits || isRestoredInstance) {
                displayedQuantityUnits = filteredQuantityUnits;
                sortQuantityUnits();
            }
            isRestoredInstance = false;
        }
        if(debug) Log.i(
                TAG, "filterQuantityUnits: filteredQuantityUnits = " + filteredQuantityUnits
        );
    }

    private void searchQuantityUnits(String search) {
        search = search.toLowerCase();
        if(debug) Log.i(TAG, "searchQuantityUnits: search = " + search);
        this.search = search;
        if(search.isEmpty()) {
            filterQuantityUnits();
        } else { // only if search contains something
            ArrayList<QuantityUnit> searchedQuantityUnits = new ArrayList<>();
            for(QuantityUnit quantityUnit : filteredQuantityUnits) {
                String name = quantityUnit.getName();
                String description = quantityUnit.getDescription();
                name = name != null ? name.toLowerCase() : "";
                description = description != null ? description.toLowerCase() : "";
                if(name.contains(search) || description.contains(search)) {
                    searchedQuantityUnits.add(quantityUnit);
                }
            }
            if(searchedQuantityUnits.isEmpty() && errorState.equals(Constants.STATE.NONE)) {
                emptyStateHelper.setNoSearchResults();
            } else {
                emptyStateHelper.clearState();
            }
            if(displayedQuantityUnits != searchedQuantityUnits) {
                displayedQuantityUnits = searchedQuantityUnits;
                sortQuantityUnits();
            }
            if(debug) Log.i(
                    TAG,
                    "searchQuantityUnits: searchedQuantityUnits = " + searchedQuantityUnits
            );
        }
    }

    private void sortQuantityUnits() {
        if(debug) Log.i(TAG, "sortQuantityUnits: sort by name, ascending = " + sortAscending);
        SortUtil.sortQuantityUnitsByName(displayedQuantityUnits, sortAscending);
        refreshAdapter(new MasterQuantityUnitAdapter(displayedQuantityUnits, this));
    }

    private void refreshAdapter(MasterQuantityUnitAdapter adapter) {
        masterQuantityUnitAdapter = adapter;
        binding.recyclerMasterQuantityUnits.animate().alpha(0).setDuration(150).withEndAction(() -> {
            binding.recyclerMasterQuantityUnits.setAdapter(adapter);
            binding.recyclerMasterQuantityUnits.animate().alpha(1).setDuration(150).start();
        }).start();
    }

    /**
     * Returns index in the displayed items.
     * Used for providing a safe and up-to-date value
     * e.g. when the items are filtered/sorted before server responds
     */
    private int getQuantityUnitPosition(int quantityUnitId) {
        for(int i = 0; i < displayedQuantityUnits.size(); i++) {
            if(displayedQuantityUnits.get(i).getId() == quantityUnitId) {
                return i;
            }
        }
        return 0;
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
            sortQuantityUnits();
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
        showQuantityUnitSheet(displayedQuantityUnits.get(position));
    }

    public void editQuantityUnit(QuantityUnit quantityUnit) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.ARGUMENT.QUANTITY_UNIT, quantityUnit);
        activity.replaceFragment(Constants.UI.MASTER_QUANTITY_UNIT, bundle, true);
    }

    private void showQuantityUnitSheet(QuantityUnit quantityUnit) {
        if(quantityUnit != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(Constants.ARGUMENT.QUANTITY_UNIT, quantityUnit);
            activity.showBottomSheet(new MasterQuantityUnitBottomSheetDialogFragment(), bundle);
        }
    }

    private void setUpSearch() {
        if(search.isEmpty()) { // only if no search is active
            appBarBehavior.switchToSecondary();
            binding.editTextMasterQuantityUnitsSearch.setText("");
        }
        binding.textInputMasterQuantityUnitsSearch.requestFocus();
        activity.showKeyboard(binding.editTextMasterQuantityUnitsSearch);

        activity.setUI(Constants.UI.MASTER_QUANTITY_UNITS_SEARCH);
    }

    public void dismissSearch() {
        appBarBehavior.switchToPrimary();
        activity.hideKeyboard();
        binding.editTextMasterQuantityUnitsSearch.setText("");
        filterQuantityUnits();

        activity.setUI(Constants.UI.MASTER_QUANTITY_UNITS_DEFAULT);
    }

    public void checkForUsage(QuantityUnit quantityUnit) {
        if(!products.isEmpty()) {
            for(Product product : products) {
                if(product.getQuIdStock() != quantityUnit.getId()
                        && product.getQuIdPurchase() != quantityUnit.getId()) continue;
                activity.showMessage(
                        Snackbar.make(
                                activity.binding.frameMainContainer,
                                activity.getString(
                                        R.string.msg_master_delete_usage,
                                        activity.getString(R.string.property_quantity_unit)
                                ),
                                Snackbar.LENGTH_LONG
                        )
                );
                return;
            }
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.ARGUMENT.QUANTITY_UNIT, quantityUnit);
        bundle.putString(Constants.ARGUMENT.TYPE, Constants.ARGUMENT.QUANTITY_UNIT);
        activity.showBottomSheet(new MasterDeleteBottomSheetDialogFragment(), bundle);
    }

    public void deleteQuantityUnit(QuantityUnit quantityUnit) {
        dlHelper.delete(
                grocyApi.getObject(GrocyApi.ENTITY.QUANTITY_UNITS, quantityUnit.getId()),
                response -> {
                    int index = getQuantityUnitPosition(quantityUnit.getId());
                    if(index != -1) {
                        displayedQuantityUnits.remove(index);
                        masterQuantityUnitAdapter.notifyItemRemoved(index);
                    } else {
                        // quantityUnit not found, fall back to complete refresh
                        refresh();
                    }
                },
                error -> showMessage(activity.getString(R.string.error_undefined))
        );
    }

    private void showMessage(String message) {
        activity.showMessage(
                Snackbar.make(activity.binding.frameMainContainer, message, Snackbar.LENGTH_SHORT)
        );
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
