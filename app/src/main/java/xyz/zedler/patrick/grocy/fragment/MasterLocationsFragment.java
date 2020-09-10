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
import xyz.zedler.patrick.grocy.adapter.MasterLocationAdapter;
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterLocationsBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterLocationBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.EmptyStateHelper;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.util.AnimUtil;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;

public class MasterLocationsFragment extends BaseFragment
        implements MasterLocationAdapter.MasterLocationAdapterListener {

    private final static String TAG = Constants.UI.MASTER_LOCATIONS;

    private MainActivity activity;
    private Gson gson;
    private GrocyApi grocyApi;
    private AppBarBehavior appBarBehavior;
    private DownloadHelper dlHelper;
    private MasterLocationAdapter masterLocationAdapter;
    private FragmentMasterLocationsBinding binding;
    private ClickUtil clickUtil;
    private AnimUtil animUtil;
    private EmptyStateHelper emptyStateHelper;

    private ArrayList<Location> locations;
    private ArrayList<Location> filteredLocations;
    private ArrayList<Location> displayedLocations;
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
        binding = FragmentMasterLocationsBinding.inflate(inflater, container, false);
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
            binding.recyclerMasterLocations.animate().cancel();
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

        locations = new ArrayList<>();
        filteredLocations = new ArrayList<>();
        displayedLocations = new ArrayList<>();
        products = new ArrayList<>();

        search = "";
        errorState = Constants.STATE.NONE;
        sortAscending = true;
        isRestoredInstance = savedInstanceState != null;

        // VIEWS

        binding.frameMasterLocationsBack.setOnClickListener(v -> activity.onBackPressed());
        binding.frameMasterLocationsSearchClose.setOnClickListener(v -> dismissSearch());
        binding.editTextMasterLocationsSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                search = s.toString();
            }
        });
        binding.editTextMasterLocationsSearch.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        Editable search = binding.editTextMasterLocationsSearch.getText();
                        searchLocations(search != null ? search.toString() : "");
                        activity.hideKeyboard();
                        return true;
                    } return false;
                });
        emptyStateHelper = new EmptyStateHelper(this, binding.linearEmpty);

        // APP BAR BEHAVIOR

        appBarBehavior = new AppBarBehavior(
                activity,
                R.id.linear_master_locations_app_bar_default,
                R.id.linear_master_locations_app_bar_search
        );

        // SWIPE REFRESH

        binding.swipeMasterLocations.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        binding.swipeMasterLocations.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        binding.swipeMasterLocations.setOnRefreshListener(this::refresh);

        binding.recyclerMasterLocations.setLayoutManager(
                new LinearLayoutManager(
                        activity,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );
        binding.recyclerMasterLocations.setItemAnimator(new DefaultItemAnimator());
        binding.recyclerMasterLocations.setAdapter(new MasterPlaceholderAdapter());

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
        activity.getScrollBehavior().setUpScroll(R.id.scroll_master_locations);
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
                () -> navigate(MasterLocationsFragmentDirections
                        .actionMasterLocationsFragmentToMasterLocationFragment())
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(isHidden()) return;

        outState.putParcelableArrayList("locations", locations);
        outState.putParcelableArrayList("filteredLocations", filteredLocations);
        outState.putParcelableArrayList("displayedLocations", displayedLocations);
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

        locations = savedInstanceState.getParcelableArrayList("locations");
        filteredLocations = savedInstanceState.getParcelableArrayList("filteredLocations");
        displayedLocations = savedInstanceState.getParcelableArrayList("displayedLocations");
        products = savedInstanceState.getParcelableArrayList("products");

        search = savedInstanceState.getString("search");
        errorState = savedInstanceState.getString("errorState");
        sortAscending = savedInstanceState.getBoolean("sortAscending");

        appBarBehavior.restoreInstanceState(savedInstanceState);

        binding.swipeMasterLocations.setRefreshing(false);

        // SEARCH
        search = savedInstanceState.getString("search", "");
        binding.editTextMasterLocationsSearch.setText(search);

        // FILTERS
        isRestoredInstance = true;
        filterLocations();
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
            binding.swipeMasterLocations.setRefreshing(false);
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
        View viewOut = binding.scrollMasterLocations;

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
                viewIn = binding.scrollMasterLocations;
                viewOut = binding.relativeError.relativeError;
                break;
        }

        animUtil.replaceViews(viewIn, viewOut, animated);
    }

    private void download() {
        binding.swipeMasterLocations.setRefreshing(true);
        downloadLocations();
        downloadProducts();
    }

    private void downloadLocations() {
        dlHelper.get(
                grocyApi.getObjects(GrocyApi.ENTITY.LOCATIONS),
                response -> {
                    locations = gson.fromJson(
                            response,
                            new TypeToken<List<Location>>(){}.getType()
                    );
                    if(debug) Log.i(TAG, "downloadLocations: locations = " + locations);
                    binding.swipeMasterLocations.setRefreshing(false);
                    filterLocations();
                },
                error -> {
                    binding.swipeMasterLocations.setRefreshing(false);
                    setError(Constants.STATE.OFFLINE, true);
                    if(debug) Log.e(TAG, "downloadLocations: " + error);
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

    private void filterLocations() {
        filteredLocations = locations;
        // SEARCH
        if(!search.isEmpty()) { // active search
            searchLocations(search);
        } else {
            // EMPTY STATES
            if(filteredLocations.isEmpty() && errorState.equals(Constants.STATE.NONE)) {
                emptyStateHelper.setEmpty();
            } else {
                emptyStateHelper.clearState();
            }
            // SORTING
            if(displayedLocations != filteredLocations || isRestoredInstance) {
                displayedLocations = filteredLocations;
                sortLocations();
            }
            isRestoredInstance = false;
        }
        if(debug) Log.i(TAG, "filterLocations: filteredLocations = " + filteredLocations);
    }

    private void searchLocations(String search) {
        search = search.toLowerCase();
        if(debug) Log.i(TAG, "searchLocations: search = " + search);
        this.search = search;
        if(search.isEmpty()) {
            filterLocations();
        } else { // only if search contains something
            ArrayList<Location> searchedLocations = new ArrayList<>();
            for(Location location : filteredLocations) {
                String name = location.getName();
                String description = location.getDescription();
                name = name != null ? name.toLowerCase() : "";
                description = description != null ? description.toLowerCase() : "";
                if(name.contains(search) || description.contains(search)) {
                    searchedLocations.add(location);
                }
            }
            if(searchedLocations.isEmpty() && errorState.equals(Constants.STATE.NONE)) {
                emptyStateHelper.setNoSearchResults();
            } else {
                emptyStateHelper.clearState();
            }
            if(displayedLocations != searchedLocations) {
                displayedLocations = searchedLocations;
                sortLocations();
            }
            if(debug) Log.i(TAG, "searchLocations: searchedLocations = " + searchedLocations);
        }
    }

    private void sortLocations() {
        if(debug) Log.i(TAG, "sortLocations: sort by name, ascending = " + sortAscending);
        SortUtil.sortLocationsByName(displayedLocations, sortAscending);
        refreshAdapter(new MasterLocationAdapter(displayedLocations, this));
    }

    private void refreshAdapter(MasterLocationAdapter adapter) {
        masterLocationAdapter = adapter;
        binding.recyclerMasterLocations.animate().alpha(0).setDuration(150).withEndAction(() -> {
            binding.recyclerMasterLocations.setAdapter(adapter);
            binding.recyclerMasterLocations.animate().alpha(1).setDuration(150).start();
        }).start();
    }

    /**
     * Returns index in the displayed items.
     * Used for providing a safe and up-to-date value
     * e.g. when the items are filtered/sorted before server responds
     */
    private int getLocationPosition(int locationId) {
        for(int i = 0; i < displayedLocations.size(); i++) {
            if(displayedLocations.get(i).getId() == locationId) {
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
            sortLocations();
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
        showLocationSheet(displayedLocations.get(position));
    }

    public void editLocation(Location location) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.ARGUMENT.LOCATION, location);
        activity.replaceFragment(Constants.UI.MASTER_LOCATION, bundle, true);
    }

    private void showLocationSheet(Location location) {
        if(location != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(Constants.ARGUMENT.LOCATION, location);
            activity.showBottomSheet(new MasterLocationBottomSheet(), bundle);
        }
    }

    public void setUpSearch() {
        if(search.isEmpty()) { // only if no search is active
            appBarBehavior.switchToSecondary();
            binding.editTextMasterLocationsSearch.setText("");
        }
        binding.textInputMasterLocationsSearch.requestFocus();
        activity.showKeyboard(binding.editTextMasterLocationsSearch);

        setIsSearchVisible(true);
    }

    @Override
    public void dismissSearch() {
        appBarBehavior.switchToPrimary();
        activity.hideKeyboard();
        binding.editTextMasterLocationsSearch.setText("");
        filterLocations();

        emptyStateHelper.clearState();

        setIsSearchVisible(false);
    }

    public void checkForUsage(Location location) {
        if(!products.isEmpty()) {
            for(Product product : products) {
                if(product.getLocationId() == location.getId()) {
                    activity.showSnackbar(
                            Snackbar.make(
                                    activity.binding.frameMainContainer,
                                    activity.getString(
                                            R.string.msg_master_delete_usage,
                                            activity.getString(R.string.property_location)
                                    ),
                                    Snackbar.LENGTH_LONG
                            )
                    );
                    return;
                }
            }
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.ARGUMENT.LOCATION, location);
        bundle.putString(Constants.ARGUMENT.TYPE, Constants.ARGUMENT.LOCATION);
        activity.showBottomSheet(new MasterDeleteBottomSheet(), bundle);
    }

    public void deleteLocation(Location location) {
        dlHelper.delete(
                grocyApi.getObject(GrocyApi.ENTITY.LOCATIONS, location.getId()),
                response -> {
                    int index = getLocationPosition(location.getId());
                    if(index != -1) {
                        displayedLocations.remove(index);
                        masterLocationAdapter.notifyItemRemoved(index);
                    } else {
                        // location not found, fall back to complete refresh
                        refresh();
                    }
                },
                error -> showMessage(activity.getString(R.string.error_undefined))
        );
    }

    private void showMessage(String message) {
        activity.showSnackbar(
                Snackbar.make(activity.binding.frameMainContainer, message, Snackbar.LENGTH_SHORT)
        );
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
