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
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
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
import xyz.zedler.patrick.grocy.adapter.MasterLocationAdapter;
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterLocationsBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterLocationBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.util.AnimUtil;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class MasterLocationsFragment extends Fragment
        implements MasterLocationAdapter.MasterLocationAdapterListener {

    private final static String TAG = Constants.UI.MASTER_LOCATIONS;
    private final static boolean DEBUG = false;

    private MainActivity activity;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private AppBarBehavior appBarBehavior;
    private WebRequest request;
    private MasterLocationAdapter masterLocationAdapter;
    private FragmentMasterLocationsBinding binding;
    private ClickUtil clickUtil = new ClickUtil();
    private AnimUtil animUtil = new AnimUtil();

    private ArrayList<Location> locations;
    private ArrayList<Location> filteredLocations;
    private ArrayList<Location> displayedLocations;
    private ArrayList<Product> products;

    private String search;
    private String errorState;
    private boolean sortAscending;
    private boolean isRestoredInstance;

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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (MainActivity) getActivity();
        assert activity != null;

        // WEB REQUESTS

        request = new WebRequest(activity.getRequestQueue());
        grocyApi = activity.getGrocy();

        // INITIALIZE VARIABLES

        locations = new ArrayList<>();
        filteredLocations = new ArrayList<>();
        displayedLocations = new ArrayList<>();
        products = new ArrayList<>();

        search = "";
        errorState = Constants.STATE.NONE;
        sortAscending = true;
        isRestoredInstance = savedInstanceState != null;

        // INITIALIZE VIEWS

        binding.frameMasterLocationsBack.setOnClickListener(v -> activity.onBackPressed());
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

        activity.updateUI(
                appBarBehavior.isPrimaryLayout()
                        ? Constants.UI.MASTER_LOCATIONS_DEFAULT
                        : Constants.UI.MASTER_LOCATIONS_SEARCH,
                savedInstanceState == null,
                TAG
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(!isHidden()) {
            outState.putParcelableArrayList("locations", locations);
            outState.putParcelableArrayList("filteredLocations", filteredLocations);
            outState.putParcelableArrayList("displayedLocations", displayedLocations);
            outState.putParcelableArrayList("products", products);

            outState.putString("search", search);
            outState.putString("errorState", errorState);
            outState.putBoolean("sortAscending", sortAscending);

            appBarBehavior.saveInstanceState(outState);
        }
        super.onSaveInstanceState(outState);
    }

    private void restoreSavedInstanceState(@NonNull Bundle savedInstanceState) {
        if(isHidden()) return;

        errorState = savedInstanceState.getString("errorState", Constants.STATE.NONE);
        setError(errorState, false);
        if(errorState.equals(Constants.STATE.OFFLINE) || errorState.equals(Constants.STATE.ERROR)) {
            return;
        }

        locations = savedInstanceState.getParcelableArrayList("locations");
        filteredLocations = savedInstanceState.getParcelableArrayList("filteredLocations");
        displayedLocations = savedInstanceState.getParcelableArrayList("displayedLocations");
        products = savedInstanceState.getParcelableArrayList("products");

        search = savedInstanceState.getString("search");
        errorState = savedInstanceState.getString("errorState");
        sortAscending = savedInstanceState.getBoolean("sortAscending");

        appBarBehavior.restoreInstanceState(savedInstanceState);
        activity.setUI(
                appBarBehavior.isPrimaryLayout()
                        ? Constants.UI.MASTER_PRODUCTS_DEFAULT
                        : Constants.UI.MASTER_PRODUCTS_SEARCH
        );

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
        if(!hidden) onActivityCreated(null);
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
        View viewOut = binding.scrollMasterLocations;

        switch (state) {
            case Constants.STATE.OFFLINE:
                binding.linearError.imageError.setImageResource(R.drawable.illustration_broccoli);
                binding.linearError.textErrorTitle.setText(R.string.error_offline);
                binding.linearError.textErrorSubtitle.setText(R.string.error_offline_subtitle);
                setEmptyState(Constants.STATE.NONE);
                break;
            case Constants.STATE.ERROR:
                binding.linearError.imageError.setImageResource(R.drawable.illustration_popsicle);
                binding.linearError.textErrorTitle.setText(R.string.error_unknown);
                binding.linearError.textErrorSubtitle.setText(R.string.error_unknown_subtitle);
                setEmptyState(Constants.STATE.NONE);
                break;
            case Constants.STATE.NONE:
                viewIn = binding.scrollMasterLocations;
                viewOut = binding.linearError.linearError;
                break;
        }

        animUtil.replaceViews(viewIn, viewOut, animated);
    }

    private void setEmptyState(String state) {
        LinearLayout container = binding.linearEmpty.linearEmpty;
        new Handler().postDelayed(() -> {
            switch (state) {
                case Constants.STATE.EMPTY:
                    binding.linearEmpty.imageEmpty.setImageResource(R.drawable.illustration_toast);
                    binding.linearEmpty.textEmptyTitle.setText(R.string.error_empty_locations);
                    binding.linearEmpty.textEmptySubtitle.setText(
                            R.string.error_empty_locations_sub
                    );
                    break;
                case Constants.STATE.NO_SEARCH_RESULTS:
                    binding.linearEmpty.imageEmpty.setImageResource(R.drawable.illustration_jar);
                    binding.linearEmpty.textEmptyTitle.setText(R.string.error_search);
                    binding.linearEmpty.textEmptySubtitle.setText(R.string.error_search_sub);
                    break;
                case Constants.STATE.NO_FILTER_RESULTS:
                    binding.linearEmpty.imageEmpty.setImageResource(R.drawable.illustration_coffee);
                    binding.linearEmpty.textEmptyTitle.setText(R.string.error_filter);
                    binding.linearEmpty.textEmptySubtitle.setText(R.string.error_filter_sub);
                    break;
                case Constants.STATE.NONE:
                    if(container.getVisibility() == View.GONE) return;
                    break;
            }
        }, 125);
        // show new empty state with delay or hide it if NONE
        if(state.equals(Constants.STATE.NONE)) {
            container.animate().alpha(0).setDuration(125).withEndAction(
                    () -> container.setVisibility(View.GONE)
            ).start();
        } else {
            if(container.getVisibility() == View.VISIBLE) {
                // first hide previous empty state if needed
                container.animate().alpha(0).setDuration(125).start();
            }
            new Handler().postDelayed(() -> {
                container.setAlpha(0);
                container.setVisibility(View.VISIBLE);
                container.animate().alpha(1).setDuration(125).start();
            }, 150);
        }
    }

    private void download() {
        binding.swipeMasterLocations.setRefreshing(true);
        downloadLocations();
        downloadProducts();
    }

    private void downloadLocations() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.LOCATIONS),
                response -> {
                    locations = gson.fromJson(
                            response,
                            new TypeToken<List<Location>>(){}.getType()
                    );
                    if(DEBUG) Log.i(TAG, "downloadLocations: locations = " + locations);
                    binding.swipeMasterLocations.setRefreshing(false);
                    filterLocations();
                },
                error -> {
                    binding.swipeMasterLocations.setRefreshing(false);
                    setError(Constants.STATE.OFFLINE, true);
                    Log.e(TAG, "downloadLocations: " + error);
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

    private void filterLocations() {
        filteredLocations = locations;
        // SEARCH
        if(!search.isEmpty()) { // active search
            searchLocations(search);
        } else {
            // EMPTY STATES
            setEmptyState(
                    filteredLocations.isEmpty() ? Constants.STATE.EMPTY : Constants.STATE.NONE
            );
            // SORTING
            if(displayedLocations != filteredLocations || isRestoredInstance) {
                displayedLocations = filteredLocations;
                sortLocations();
            }
            isRestoredInstance = false;
        }
        if(DEBUG) Log.i(TAG, "filterLocations: filteredLocations = " + filteredLocations);
    }

    private void searchLocations(String search) {
        search = search.toLowerCase();
        if(DEBUG) Log.i(TAG, "searchLocations: search = " + search);
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
            setEmptyState(
                    searchedLocations.isEmpty()
                            ? Constants.STATE.NO_SEARCH_RESULTS
                            : Constants.STATE.NONE
            );
            if(displayedLocations != searchedLocations) {
                displayedLocations = searchedLocations;
                sortLocations();
            }
            if(DEBUG) Log.i(TAG, "searchLocations: searchedLocations = " + searchedLocations);
        }
    }

    private void sortLocations() {
        if(DEBUG) Log.i(TAG, "sortLocations: sort by name, ascending = " + sortAscending);
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
            activity.showBottomSheet(new MasterLocationBottomSheetDialogFragment(), bundle);
        }
    }

    public void setUpSearch() {
        if(search.isEmpty()) { // only if no search is active
            appBarBehavior.switchToSecondary();
            binding.editTextMasterLocationsSearch.setText("");
        }
        binding.textInputMasterLocationsSearch.requestFocus();
        activity.showKeyboard(binding.editTextMasterLocationsSearch);

        binding.frameMasterLocationsSearchClose.setOnClickListener(v -> dismissSearch());

        activity.setUI(Constants.UI.MASTER_LOCATIONS_SEARCH);
    }

    public void dismissSearch() {
        appBarBehavior.switchToPrimary();
        activity.hideKeyboard();
        search = "";
        filterLocations();

        activity.setUI(Constants.UI.MASTER_LOCATIONS_DEFAULT);
    }

    public void checkForUsage(Location location) {
        if(!products.isEmpty()) {
            for(Product product : products) {
                if(product.getLocationId() == location.getId()) {
                    activity.showMessage(
                            Snackbar.make(
                                    activity.binding.frameMainContainer,
                                    activity.getString(
                                            R.string.msg_master_delete_usage,
                                            activity.getString(R.string.type_location)
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
        activity.showBottomSheet(new MasterDeleteBottomSheetDialogFragment(), bundle);
    }

    public void deleteLocation(Location location) {
        request.delete(
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
                error -> showMessage(activity.getString(R.string.msg_error))
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
