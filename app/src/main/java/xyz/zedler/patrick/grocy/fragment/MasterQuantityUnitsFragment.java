package xyz.zedler.patrick.grocy.fragment;

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
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.adapter.MasterQuantityUnitAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterQuantityUnitBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class MasterQuantityUnitsFragment extends Fragment
        implements MasterQuantityUnitAdapter.MasterQuantityUnitAdapterListener {

    private final static String TAG = Constants.UI.MASTER_QUANTITY_UNITS;
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private AppBarBehavior appBarBehavior;
    private WebRequest request;
    private MasterQuantityUnitAdapter masterQuantityUnitAdapter;

    private ArrayList<QuantityUnit> quantityUnits = new ArrayList<>();
    private ArrayList<QuantityUnit> filteredQuantityUnits = new ArrayList<>();
    private ArrayList<QuantityUnit> displayedQuantityUnits = new ArrayList<>();
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
        return inflater.inflate(R.layout.fragment_master_quantity_units, container, false);
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

        activity.findViewById(R.id.frame_master_quantity_units_back).setOnClickListener(
                v -> activity.onBackPressed()
        );
        linearLayoutError = activity.findViewById(R.id.linear_master_quantity_units_error);
        swipeRefreshLayout = activity.findViewById(R.id.swipe_master_quantity_units);
        scrollView = activity.findViewById(R.id.scroll_master_quantity_units);
        // retry button on offline error page
        activity.findViewById(R.id.button_master_quantity_units_error_retry).setOnClickListener(
                v -> refresh()
        );
        recyclerView = activity.findViewById(R.id.recycler_master_quantity_units);
        textInputLayoutSearch = activity.findViewById(R.id.text_input_master_quantity_units_search);
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
                searchQuantityUnits(editTextSearch.getText().toString());
                activity.hideKeyboard();
                return true;
            } return false;
        });

        // APP BAR BEHAVIOR

        appBarBehavior = new AppBarBehavior(activity, R.id.linear_master_quantity_units_app_bar_default);

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

        activity.updateUI(Constants.UI.MASTER_QUANTITY_UNITS_DEFAULT, TAG);
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
            activity.showSnackbar(
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
        downloadProducts();
    }

    private void downloadQuantityUnits() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.QUANTITY_UNITS),
                response -> {
                    quantityUnits = gson.fromJson(
                            response,
                            new TypeToken<List<QuantityUnit>>(){}.getType()
                    );
                    if(DEBUG) Log.i(TAG, "downloadQuantityUnits: quantityUnits = " + quantityUnits);
                    swipeRefreshLayout.setRefreshing(false);
                    filterQuantityUnits();
                },
                error -> {
                    swipeRefreshLayout.setRefreshing(false);
                    setError(true, true);
                    if(DEBUG) Log.e(TAG, "downloadQuantityUnits: " + error);
                }
        );
    }

    private void downloadProducts() {
        request.get(
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
        if(!search.equals("")) { // active search
            searchQuantityUnits(search);
        } else {
            if(displayedQuantityUnits != filteredQuantityUnits) {
                displayedQuantityUnits = filteredQuantityUnits;
                sortQuantityUnits();
            }
        }
        if(DEBUG) Log.i(TAG, "filterQuantityUnits: filteredQuantityUnits = " + filteredQuantityUnits);
    }

    private void searchQuantityUnits(String search) {
        search = search.toLowerCase();
        if(DEBUG) Log.i(TAG, "searchQuantityUnits: search = " + search);
        this.search = search;
        if(search.equals("")) {
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
            if(displayedQuantityUnits != searchedQuantityUnits) {
                displayedQuantityUnits = searchedQuantityUnits;
                sortQuantityUnits();
            }
            if(DEBUG) Log.i(TAG, "searchQuantityUnits: searchedQuantityUnits = " + searchedQuantityUnits);
        }
    }

    private void sortQuantityUnits() {
        if(DEBUG) Log.i(TAG, "sortQuantityUnits: sort by name, ascending = " + sortAscending);
        SortUtil.sortQuantityUnitsByName(displayedQuantityUnits, sortAscending);
        refreshAdapter(new MasterQuantityUnitAdapter(displayedQuantityUnits, this));
    }

    private void refreshAdapter(MasterQuantityUnitAdapter adapter) {
        masterQuantityUnitAdapter = adapter;
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
    private int getQuantityUnitPosition(int quantityUnitId) {
        for(int i = 0; i < displayedQuantityUnits.size(); i++) {
            if(displayedQuantityUnits.get(i).getId() == quantityUnitId) {
                return i;
            }
        }
        return 0;
    }

    private void showErrorMessage() {
        activity.showSnackbar(
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
            activity.startAnimatedIcon(item);
            sortQuantityUnits();
            return true;
        });
    }

    public void setUpBottomMenu() {
        setMenuSorting();
        MenuItem search = activity.getBottomMenu().findItem(R.id.action_search);
        if(search != null) {
            search.setOnMenuItemClickListener(item -> {
                activity.startAnimatedIcon(item);
                setUpSearch();
                return true;
            });
        }
    }

    @Override
    public void onItemRowClicked(int position) {
        // MASTER PRODUCT CLICK
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
        if(search.equals("")) { // only if no search is active
            appBarBehavior.replaceLayout(
                    R.id.linear_master_quantity_units_app_bar_search,
                    true
            );
            editTextSearch.setText("");
        }
        textInputLayoutSearch.requestFocus();
        activity.showKeyboard(editTextSearch);

        activity.findViewById(R.id.frame_master_quantity_units_search_close).setOnClickListener(
                v -> dismissSearch()
        );

        activity.updateUI(Constants.UI.MASTER_QUANTITY_UNITS_SEARCH, TAG);
    }

    public void dismissSearch() {
        appBarBehavior.replaceLayout(R.id.linear_master_quantity_units_app_bar_default, true);
        activity.hideKeyboard();
        search = "";
        filterQuantityUnits(); // TODO: buggy animation

        activity.updateUI(Constants.UI.MASTER_QUANTITY_UNITS_DEFAULT, TAG);
    }

    public void checkForUsage(QuantityUnit quantityUnit) {
        if(!products.isEmpty()) {
            for(Product product : products) {
                if(product.getQuIdStock() != quantityUnit.getId()
                        && product.getQuIdPurchase() != quantityUnit.getId()) continue;
                activity.showSnackbar(
                        Snackbar.make(
                                activity.findViewById(R.id.linear_container_main),
                                activity.getString(
                                        R.string.msg_master_delete_usage,
                                        activity.getString(R.string.type_quantity_unit)
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
        request.delete(
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
                error -> showErrorMessage()
        );
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
