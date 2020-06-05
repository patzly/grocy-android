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

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
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
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterLocationBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
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
    private ClickUtil clickUtil = new ClickUtil();

    private ArrayList<Location> locations = new ArrayList<>();
    private ArrayList<Location> filteredLocations = new ArrayList<>();
    private ArrayList<Location> displayedLocations = new ArrayList<>();
    private ArrayList<Product> products = new ArrayList<>();

    private String search = "";
    private boolean sortAscending = true;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextInputLayout textInputLayoutSearch;
    private EditText editTextSearch;
    private LinearLayout linearLayoutError;
    private NestedScrollView scrollView;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_master_locations, container, false);
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

        activity.findViewById(R.id.frame_master_locations_back).setOnClickListener(
                v -> activity.onBackPressed()
        );
        linearLayoutError = activity.findViewById(R.id.linear_master_locations_error);
        swipeRefreshLayout = activity.findViewById(R.id.swipe_master_locations);
        scrollView = activity.findViewById(R.id.scroll_master_locations);
        // retry button on offline error page
        activity.findViewById(R.id.button_master_locations_error_retry).setOnClickListener(
                v -> refresh()
        );
        recyclerView = activity.findViewById(R.id.recycler_master_locations);
        textInputLayoutSearch = activity.findViewById(R.id.text_input_master_locations_search);
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
                searchLocations(editTextSearch.getText().toString());
                activity.hideKeyboard();
                return true;
            } return false;
        });

        // APP BAR BEHAVIOR

        appBarBehavior = new AppBarBehavior(activity, R.id.linear_master_locations_app_bar_default);

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

        activity.updateUI(Constants.UI.MASTER_LOCATIONS_DEFAULT, TAG);
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
                    swipeRefreshLayout.setRefreshing(false);
                    filterLocations();
                },
                error -> {
                    swipeRefreshLayout.setRefreshing(false);
                    setError(true, true);
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
            if(displayedLocations != filteredLocations) {
                displayedLocations = filteredLocations;
                sortLocations();
            }
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
        recyclerView.animate().alpha(0).setDuration(150).withEndAction(() -> {
            recyclerView.setAdapter(adapter);
            recyclerView.animate().alpha(1).setDuration(150).start();
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

    private void showErrorMessage() {
        activity.showMessage(
                Snackbar.make(
                        activity.findViewById(R.id.linear_container_main),
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
            appBarBehavior.replaceLayout(
                    R.id.linear_master_locations_app_bar_search,
                    true
            );
            editTextSearch.setText("");
        }
        textInputLayoutSearch.requestFocus();
        activity.showKeyboard(editTextSearch);

        activity.findViewById(R.id.frame_master_locations_search_close).setOnClickListener(
                v -> dismissSearch()
        );

        activity.setUI(Constants.UI.MASTER_LOCATIONS_SEARCH);
    }

    public void dismissSearch() {
        appBarBehavior.replaceLayout(R.id.linear_master_locations_app_bar_default, true);
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
                                    activity.findViewById(R.id.linear_container_main),
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
                error -> showErrorMessage()
        );
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
