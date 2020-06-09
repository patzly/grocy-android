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
import xyz.zedler.patrick.grocy.adapter.MasterProductGroupAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterProductGroupsBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterProductGroupBottomSheetDialogFragment;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.web.WebRequest;

public class MasterProductGroupsFragment extends Fragment
        implements MasterProductGroupAdapter.MasterProductGroupAdapterListener {

    private final static String TAG = Constants.UI.MASTER_PRODUCT_GROUPS;
    private final static boolean DEBUG = true;

    private MainActivity activity;
    private Gson gson = new Gson();
    private GrocyApi grocyApi;
    private AppBarBehavior appBarBehavior;
    private WebRequest request;
    private MasterProductGroupAdapter masterProductGroupAdapter;
    private FragmentMasterProductGroupsBinding binding;
    private ClickUtil clickUtil = new ClickUtil();

    private ArrayList<ProductGroup> productGroups = new ArrayList<>();
    private ArrayList<ProductGroup> filteredProductGroups = new ArrayList<>();
    private ArrayList<ProductGroup> displayedProductGroups = new ArrayList<>();
    private ArrayList<Product> products = new ArrayList<>();

    private String search = "";
    private boolean sortAscending = true;

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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = (MainActivity) getActivity();
        assert activity != null;

        // WEB REQUESTS

        request = new WebRequest(activity.getRequestQueue());
        grocyApi = activity.getGrocy();

        // INITIALIZE VIEWS

        binding.frameMasterProductGroupsBack.setOnClickListener(v -> activity.onBackPressed());
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
                        searchProductGroups(
                                binding.editTextMasterProductGroupsSearch.getText().toString()
                        );
                        activity.hideKeyboard();
                        return true;
                    } return false;
                });

        // APP BAR BEHAVIOR

        appBarBehavior = new AppBarBehavior(
                activity,
                R.id.linear_master_product_groups_app_bar_default,
                R.id.linear_master_product_groups_app_bar_search
        );

        // SWIPE REFRESH

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

        load();

        // UPDATE UI

        activity.updateUI(Constants.UI.MASTER_PRODUCT_GROUPS_DEFAULT, TAG);
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

    private void setError(boolean isError, boolean animated) {
        // TODO: different errors
        if(animated) {
            View viewOut = isError
                    ? binding.scrollMasterProductGroups
                    : binding.linearError.linearError;
            View viewIn = isError
                    ? binding.linearError.linearError
                    : binding.scrollMasterProductGroups;
            if(viewOut.getVisibility() == View.VISIBLE && viewIn.getVisibility() == View.GONE) {
                viewOut.animate().alpha(0).setDuration(150).withEndAction(() -> {
                    viewIn.setAlpha(0);
                    viewOut.setVisibility(View.GONE);
                    viewIn.setVisibility(View.VISIBLE);
                    viewIn.animate().alpha(1).setDuration(150).start();
                }).start();
            }
        } else {
            binding.scrollMasterProductGroups.setVisibility(isError ? View.GONE : View.VISIBLE);
            binding.linearError.linearError.setVisibility(isError ? View.VISIBLE : View.GONE);
        }
    }

    private void download() {
        binding.swipeMasterProductGroups.setRefreshing(true);
        downloadProductGroups();
        downloadProducts();
    }

    private void downloadProductGroups() {
        request.get(
                grocyApi.getObjects(GrocyApi.ENTITY.PRODUCT_GROUPS),
                response -> {
                    productGroups = gson.fromJson(
                            response,
                            new TypeToken<List<ProductGroup>>(){}.getType()
                    );
                    if(DEBUG) Log.i(
                            TAG, "downloadProductGroups: productGroups = " + productGroups
                    );
                    binding.swipeMasterProductGroups.setRefreshing(false);
                    filterProductGroups();
                },
                error -> {
                    binding.swipeMasterProductGroups.setRefreshing(false);
                    setError(true, true);
                    Log.e(TAG, "downloadProductGroups: " + error);
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

    private void filterProductGroups() {
        filteredProductGroups = productGroups;
        // SEARCH
        if(!search.isEmpty()) { // active search
            searchProductGroups(search);
        } else {
            if(displayedProductGroups != filteredProductGroups) {
                displayedProductGroups = filteredProductGroups;
                sortProductGroups();
            }
        }
        if(DEBUG) Log.i(
                TAG, "filterProductGroups: filteredProductGroups = " + filteredProductGroups
        );
    }

    private void searchProductGroups(String search) {
        search = search.toLowerCase();
        if(DEBUG) Log.i(TAG, "searchProductGroups: search = " + search);
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
            if(displayedProductGroups != searchedProductGroups) {
                displayedProductGroups = searchedProductGroups;
                sortProductGroups();
            }
            if(DEBUG) Log.i(TAG, "searchProductGroups: searchedProductGroups = " + searchedProductGroups);
        }
    }

    private void sortProductGroups() {
        if(DEBUG) Log.i(TAG, "sortProductGroups: sort by name, ascending = " + sortAscending);
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
            activity.showBottomSheet(new MasterProductGroupBottomSheetDialogFragment(), bundle);
        }
    }

    private void setUpSearch() {
        if(search.isEmpty()) { // only if no search is active
            appBarBehavior.switchToSecondary();
            binding.editTextMasterProductGroupsSearch.setText("");
        }
        binding.textInputMasterProductGroupsSearch.requestFocus();
        activity.showKeyboard(binding.editTextMasterProductGroupsSearch);

        binding.frameMasterProductGroupsSearchClose.setOnClickListener(v -> dismissSearch());

        activity.setUI(Constants.UI.MASTER_PRODUCT_GROUPS_SEARCH);
    }

    public void dismissSearch() {
        appBarBehavior.switchToPrimary();
        activity.hideKeyboard();
        search = "";
        filterProductGroups();

        activity.setUI(Constants.UI.MASTER_PRODUCT_GROUPS_DEFAULT);
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
                                            activity.getString(R.string.type_product_group)
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
        activity.showBottomSheet(new MasterDeleteBottomSheetDialogFragment(), bundle);
    }

    public void deleteProductGroup(ProductGroup productGroup) {
        request.delete(
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
