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
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.transition.TransitionInflater;

import com.android.volley.VolleyError;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.MasterObjectListAdapter;
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterObjectListBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterLocationBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterProductBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterProductGroupBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterQuantityUnitBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterStoreBottomSheet;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.helper.EmptyStateHelper;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.AnimUtil;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.ObjectUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;

public class MasterObjectListFragment extends BaseFragment
        implements MasterObjectListAdapter.MasterObjectListAdapterListener {

    private final static String TAG = MasterObjectListFragment.class.getSimpleName();

    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private GrocyApi grocyApi;
    private AppBarBehavior appBarBehavior;
    private DownloadHelper dlHelper;
    private MasterObjectListAdapter masterObjectListAdapter;
    private ClickUtil clickUtil;
    private AnimUtil animUtil;
    private EmptyStateHelper emptyStateHelper;
    private FragmentMasterObjectListBinding binding;

    private ArrayList<Object> objects;
    private ArrayList<Object> filteredObjects;
    private ArrayList<Object> displayedObjects;
    private ArrayList<Object> products;
    private ArrayList<ProductGroup> productGroups;
    private HashMap<Integer, ProductGroup> productGroupsMap;
    private HashMap<Integer, Location> locationsMap;
    private HashMap<Integer, QuantityUnit> quantityUnitsMap;

    private String entity;
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
        entity = MasterObjectListFragmentArgs.fromBundle(requireArguments()).getEntity();
        binding = FragmentMasterObjectListBinding.inflate(
                inflater, container, false
        );
        int title;
        switch (entity) {
            case GrocyApi.ENTITY.PRODUCTS:
                title = R.string.property_products;
                break;
            case GrocyApi.ENTITY.QUANTITY_UNITS:
                title = R.string.property_quantity_units;
                break;
            case GrocyApi.ENTITY.LOCATIONS:
                title = R.string.property_locations;
                break;
            case GrocyApi.ENTITY.PRODUCT_GROUPS:
                title = R.string.property_product_groups;
                break;
            default: // STORES
                title = R.string.property_stores;
        }
        binding.title.setText(title);

        String transitionName = MasterObjectListFragmentArgs
                .fromBundle(requireArguments()).getTransitionName();
        if(!TextUtils.isEmpty(transitionName)) {
            binding.title.setTransitionName(transitionName);
            setSharedElementEnterTransition(
                    TransitionInflater.from(requireContext())
                            .inflateTransition(android.R.transition.move)
            );
        }

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
            binding.recyclerMasterObjectList.animate().cancel();
            binding = null;
        }
        dlHelper.destroy();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if(isHidden()) return;

        activity = (MainActivity) requireActivity();

        // PREFERENCES

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        // UTILS

        clickUtil = new ClickUtil();
        animUtil = new AnimUtil();

        // WEB REQUESTS

        dlHelper = new DownloadHelper(activity, TAG);
        grocyApi = activity.getGrocy();

        // INITIALIZE VARIABLES

        objects = new ArrayList<>();
        filteredObjects = new ArrayList<>();
        displayedObjects = new ArrayList<>();
        products = new ArrayList<>();
        productGroups = new ArrayList<>();
        productGroupsMap = new HashMap<>();
        locationsMap = new HashMap<>();
        quantityUnitsMap = new HashMap<>();

        search = "";
        errorState = Constants.STATE.NONE;
        sortAscending = true;
        isRestoredInstance = savedInstanceState != null;

        // INITIALIZE VIEWS

        binding.frameMasterObjectListBack.setOnClickListener(v -> activity.onBackPressed());
        binding.frameMasterObjectListSearchClose.setOnClickListener(v -> dismissSearch());
        binding.editTextMasterObjectListSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                search = s != null ? s.toString() : "";
                searchObjects(search);
            }
        });
        binding.editTextMasterObjectListSearch.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        Editable search = binding.editTextMasterObjectListSearch.getText();
                        searchObjects(search != null ? search.toString() : "");
                        activity.hideKeyboard();
                        return true;
                    } return false;
                });
        @StringRes int emptyTitle;
        switch (entity) {
            case GrocyApi.ENTITY.LOCATIONS:
                emptyTitle = R.string.error_empty_locations;
                break;
            case GrocyApi.ENTITY.QUANTITY_UNITS:
                emptyTitle = R.string.error_empty_qu;
                break;
            case GrocyApi.ENTITY.PRODUCT_GROUPS:
                emptyTitle = R.string.error_empty_product_groups;
                break;
            case GrocyApi.ENTITY.PRODUCTS:
                emptyTitle = R.string.error_empty_products;
                break;
            default: // STORES
                emptyTitle = R.string.error_empty_stores;
        }
        emptyStateHelper = new EmptyStateHelper(
                binding.linearEmpty,
                emptyTitle,
                R.string.error_empty_master_data_sub
        );

        // APP BAR BEHAVIOR

        appBarBehavior = new AppBarBehavior(
                activity,
                R.id.linear_master_object_list_app_bar_default,
                R.id.linear_master_object_list_app_bar_search
        );

        // SWIPE REFRESH

        binding.swipeMasterObjectList.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        binding.swipeMasterObjectList.setColorSchemeColors(
                ContextCompat.getColor(activity, R.color.secondary)
        );
        binding.swipeMasterObjectList.setOnRefreshListener(this::refresh);

        binding.recyclerMasterObjectList.setLayoutManager(
                new LinearLayoutManager(
                        activity,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );
        binding.recyclerMasterObjectList.setItemAnimator(new DefaultItemAnimator());
        binding.recyclerMasterObjectList.setAdapter(new MasterPlaceholderAdapter());

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
        activity.getScrollBehavior().setUpScroll(binding.recyclerMasterObjectList);
        activity.getScrollBehavior().setHideOnScroll(true);
        activity.updateBottomAppBar(
                Constants.FAB.POSITION.CENTER,
                !entity.equals(GrocyApi.ENTITY.PRODUCTS)
                        ? R.menu.menu_master_items
                        : R.menu.menu_master_products,
                animated,
                this::setUpBottomMenu
        );
        activity.updateFab(
                R.drawable.ic_round_add_anim,
                R.string.action_add,
                Constants.FAB.TAG.ADD,
                animated,
                () -> {
                    switch (entity) {
                        case GrocyApi.ENTITY.QUANTITY_UNITS:
                            navigate(MasterObjectListFragmentDirections
                                    .actionMasterObjectListFragmentToMasterQuantityUnitFragment());
                            break;
                        case GrocyApi.ENTITY.PRODUCT_GROUPS:
                            navigate(MasterObjectListFragmentDirections
                                    .actionMasterObjectListFragmentToMasterProductGroupFragment());
                            break;
                        case GrocyApi.ENTITY.LOCATIONS:
                            navigate(MasterObjectListFragmentDirections
                                    .actionMasterObjectListFragmentToMasterLocationFragment());
                            break;
                        case GrocyApi.ENTITY.STORES:
                            navigate(MasterObjectListFragmentDirections
                                    .actionMasterObjectListFragmentToMasterStoreFragment());
                            break;
                        case GrocyApi.ENTITY.PRODUCTS:
                            navigate(MasterObjectListFragmentDirections
                                    .actionMasterObjectListFragmentToMasterProductFragment());
                    }
                }
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if(isHidden()) return;

        /*outState.putParcelableArrayList("quantityUnits", objects);
        outState.putParcelableArrayList("filteredQuantityUnits", filteredObjects);
        outState.putParcelableArrayList("displayedQuantityUnits", displayedObjects);*/
        //outState.putParcelableArrayList("products", products);

        outState.putString("search", search);
        outState.putString("errorState", errorState);
        outState.putBoolean("sortAscending", sortAscending);

        appBarBehavior.saveInstanceState(outState);
    }

    private void restoreSavedInstanceState(@NonNull Bundle savedInstanceState) {
        if(isHidden()) return;

        errorState = savedInstanceState.getString("errorState", Constants.STATE.NONE);
        setError(errorState, false);

        /*objects = savedInstanceState.getParcelableArrayList("quantityUnits");
        filteredObjects = savedInstanceState.getParcelableArrayList(
                "filteredQuantityUnits"
        );
        displayedObjects = savedInstanceState.getParcelableArrayList(
                "displayedQuantityUnits"
        );*/
        //products = savedInstanceState.getParcelableArrayList("products");

        search = savedInstanceState.getString("search");
        errorState = savedInstanceState.getString("errorState");
        sortAscending = savedInstanceState.getBoolean("sortAscending");

        appBarBehavior.restoreInstanceState(savedInstanceState);

        binding.swipeMasterObjectList.setRefreshing(false);

        // SEARCH
        search = savedInstanceState.getString("search", "");
        binding.editTextMasterObjectListSearch.setText(search);

        // FILTERS
        isRestoredInstance = true;
        filterObjects();
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
            binding.swipeMasterObjectList.setRefreshing(false);
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

        binding.relativeError.retry.setOnClickListener(v -> refresh());

        View viewIn = binding.relativeError.relativeError;
        View viewOut = binding.recyclerMasterObjectList;

        switch (state) {
            case Constants.STATE.OFFLINE:
                binding.relativeError.image.setImageResource(R.drawable.illustration_broccoli);
                binding.relativeError.title.setText(R.string.error_offline);
                binding.relativeError.subtitle.setText(R.string.error_offline_subtitle);
                emptyStateHelper.clearState();
                break;
            case Constants.STATE.ERROR:
                binding.relativeError.image.setImageResource(R.drawable.illustration_popsicle);
                binding.relativeError.title.setText(R.string.error_unknown);
                binding.relativeError.subtitle.setText(R.string.error_undefined);
                emptyStateHelper.clearState();
                break;
            case Constants.STATE.NONE:
                viewIn = binding.recyclerMasterObjectList;
                viewOut = binding.relativeError.relativeError;
                break;
        }

        animUtil.replaceViews(viewIn, viewOut, animated);
    }

    private void download() {
        binding.swipeMasterObjectList.setRefreshing(true);

        DownloadHelper.Queue queue = dlHelper.newQueue(this::onQueueEmpty, this::onDownloadError);

        queue.append(dlHelper.getObjects(objectList -> objects = objectList, entity));

        if(entity.equals(GrocyApi.ENTITY.PRODUCTS)) {
            queue.append(
                    dlHelper.getProductGroups(productGroups -> {
                        this.productGroups = productGroups;
                        productGroupsMap = ArrayUtil.getProductGroupsHashMap(productGroups);
                        setMenuProductGroupFilters();
                    }),
                    dlHelper.getQuantityUnits(quantityUnits ->
                            quantityUnitsMap = ArrayUtil.getQuantityUnitsHashMap(quantityUnits)
                    )
            );
            if(sharedPrefs.getBoolean(
                    Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING,
                    true
            )) {
                queue.append(
                        dlHelper.getLocations(locations ->
                                locationsMap = ArrayUtil.getLocationsHashMap(locations)
                        )
                );
            }
        } else {
            queue.append(dlHelper.getObjects(
                    productList -> products = productList,
                    GrocyApi.ENTITY.PRODUCTS
            ));
        }
        queue.start();
    }

    private void onQueueEmpty() {
        if(entity.equals(GrocyApi.ENTITY.PRODUCTS)) {
            products = new ArrayList<>(objects);
        }
        if(debug) Log.i(TAG, "downloadQuantityUnits: quantityUnits = " + objects);
        binding.swipeMasterObjectList.setRefreshing(false);
        filterObjects();
    }

    private void onDownloadError(VolleyError error) {
        binding.swipeMasterObjectList.setRefreshing(false);
        setError(Constants.STATE.OFFLINE, true);
        if(debug) Log.e(TAG, "downloadQuantityUnits: " + error);
    }

    private void filterObjects() {
        filteredObjects = objects;
        // SEARCH
        if(!search.isEmpty()) { // active search
            searchObjects(search);
        } else {
            // EMPTY STATES
            if(filteredObjects.isEmpty() && errorState.equals(Constants.STATE.NONE)) {
                emptyStateHelper.setEmpty();
            } else {
                emptyStateHelper.clearState();
            }
            // SORTING
            if(displayedObjects != filteredObjects || isRestoredInstance) {
                displayedObjects = filteredObjects;
                sortObjects();
            }
            isRestoredInstance = false;
        }
        if(debug) Log.i(
                TAG, "filterQuantityUnits: filteredQuantityUnits = " + filteredObjects
        );
    }

    private void searchObjects(String search) {
        search = search.toLowerCase();
        if(debug) Log.i(TAG, "searchObjects: search = " + search);
        this.search = search;
        if(search.isEmpty()) {
            filterObjects();
        } else { // only if search contains something
            ArrayList<Object> searchedObjects = new ArrayList<>();
            for(Object object : filteredObjects) {
                String name = ObjectUtil.getObjectName(object, entity);
                String description = ObjectUtil.getObjectDescription(object, entity);
                name = name != null ? name.toLowerCase() : "";
                description = description != null ? description.toLowerCase() : "";
                if(name.contains(search) || description.contains(search)) {
                    searchedObjects.add(object);
                }
            }
            if(searchedObjects.isEmpty() && errorState.equals(Constants.STATE.NONE)) {
                emptyStateHelper.setNoSearchResults();
            } else {
                emptyStateHelper.clearState();
            }
            if(displayedObjects != searchedObjects) {
                displayedObjects = searchedObjects;
                sortObjects();
            }
            if(debug) Log.i(
                    TAG,
                    "searchObjects: searchedObjects = " + searchedObjects
            );
        }
    }

    private void filterProductGroup(ProductGroup productGroup) {
        /*if(filterProductGroupId != productGroup.getId()) {
            if(debug) Log.i(TAG, "filterProductGroup: " + productGroup);
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
                binding.linearMasterProductsFilterContainer.addView(inputChipFilterProductGroup);
            }
            filterObjects();
        } else {
            if(debug) Log.i(TAG, "filterProductGroup: " + productGroup + " already filtered");
        }*/
    }

    /**
     * Sets the product group filter without filtering
     */
    private void updateProductGroupFilter(int filterProductGroupId) {
        /*ProductGroup productGroup = productGroupsMap.get(filterProductGroupId);
        if(productGroup == null) return;

        this.filterProductGroupId = filterProductGroupId;
        if(inputChipFilterProductGroup != null) {
            inputChipFilterProductGroup.changeText(productGroup.getName());
        } else {
            inputChipFilterProductGroup = new InputChip(
                    activity,
                    productGroup.getName(),
                    R.drawable.ic_round_category,
                    true,
                    () -> {
                        this.filterProductGroupId = -1;
                        inputChipFilterProductGroup = null;
                        filterProducts();
                    });
            binding.linearMasterProductsFilterContainer.addView(inputChipFilterProductGroup);
        }*/
    }

    private void sortObjects() {
        if(debug) Log.i(TAG, "sortObjects: sort by name, ascending = " + sortAscending);
        sortObjectsByName(displayedObjects, sortAscending);
        updateAdapter();
    }

    public void sortObjectsByName(ArrayList<Object> objects, boolean ascending) {
        if(objects == null) return;
        Collections.sort(objects, (item1, item2) -> {
            String name1 = ObjectUtil.getObjectName(ascending ? item1 : item2, entity);
            String name2 = ObjectUtil.getObjectName(ascending ? item2 : item1, entity);
            if(name1 == null || name2 == null) return 0;
            return name1.toLowerCase().compareTo(name2.toLowerCase());
        });
    }

    private void updateAdapter() {
        if(binding.recyclerMasterObjectList.getAdapter() == null) return;
        if(binding.recyclerMasterObjectList.getAdapter() instanceof MasterPlaceholderAdapter) {
            masterObjectListAdapter = new MasterObjectListAdapter(
                    entity, displayedObjects, this
            );
            binding.recyclerMasterObjectList.animate().alpha(0).setDuration(150).withEndAction(() -> {
                binding.recyclerMasterObjectList.setAdapter(masterObjectListAdapter);
                binding.recyclerMasterObjectList.animate().alpha(1).setDuration(150).start();
            }).start();
        } else {
            masterObjectListAdapter.updateData(displayedObjects);
        }
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
            sortObjects();
            return true;
        });
    }

    private void setMenuProductGroupFilters() {
        MenuItem menuItem = activity.getBottomMenu().findItem(R.id.action_filter);
        if(menuItem == null) return;
        SubMenu menuProductGroups = menuItem.getSubMenu();
        menuProductGroups.clear();
        SortUtil.sortProductGroupsByName(productGroups, true);
        for(ProductGroup productGroup : productGroups) {
            menuProductGroups.add(productGroup.getName()).setOnMenuItemClickListener(item -> {
                //filterProductGroup(productGroup); TODO
                return true;
            });
        }
        menuItem.setVisible(!productGroups.isEmpty());
    }

    public void setUpBottomMenu() {
        setMenuSorting();
        if(entity.equals(GrocyApi.ENTITY.PRODUCTS)) setMenuProductGroupFilters();
        MenuItem search = activity.getBottomMenu().findItem(R.id.action_search);
        if(search == null) return;
        search.setOnMenuItemClickListener(item -> {
            IconUtil.start(item);
            setUpSearch();
            return true;
        });
    }

    @Override
    public void onItemRowClicked(int position) {
        if(clickUtil.isDisabled()) return;
        showObjectBottomSheet(displayedObjects.get(position));
    }

    @Override
    public void editObject(Object object) {
        switch (entity) {
            case GrocyApi.ENTITY.QUANTITY_UNITS:
                navigate(MasterObjectListFragmentDirections
                        .actionMasterObjectListFragmentToMasterQuantityUnitFragment()
                        .setQuantityUnit((QuantityUnit) object));
                break;
            case GrocyApi.ENTITY.PRODUCT_GROUPS:
                navigate(MasterObjectListFragmentDirections
                        .actionMasterObjectListFragmentToMasterProductGroupFragment()
                        .setProductGroup((ProductGroup) object));
                break;
            case GrocyApi.ENTITY.LOCATIONS:
                navigate(MasterObjectListFragmentDirections
                        .actionMasterObjectListFragmentToMasterLocationFragment()
                        .setLocation((Location) object));
                break;
            case GrocyApi.ENTITY.STORES:
                navigate(MasterObjectListFragmentDirections
                        .actionMasterObjectListFragmentToMasterStoreFragment()
                        .setStore((Store) object));
                break;
            case GrocyApi.ENTITY.PRODUCTS:
                navigate(MasterObjectListFragmentDirections
                        .actionMasterObjectListFragmentToMasterProductSimpleFragment(
                                Constants.ACTION.EDIT
                        ).setProduct((Product) object));
        }
    }

    private void showObjectBottomSheet(Object object) {
        if(object == null) return;
        Bundle bundle = new Bundle();
        switch (entity) {
            case GrocyApi.ENTITY.QUANTITY_UNITS:
                bundle.putParcelable(Constants.ARGUMENT.QUANTITY_UNIT, (QuantityUnit) object);
                activity.showBottomSheet(new MasterQuantityUnitBottomSheet(), bundle);
                break;
            case GrocyApi.ENTITY.PRODUCT_GROUPS:
                bundle.putParcelable(Constants.ARGUMENT.PRODUCT_GROUP, (ProductGroup) object);
                activity.showBottomSheet(new MasterProductGroupBottomSheet(), bundle);
                break;
            case GrocyApi.ENTITY.LOCATIONS:
                bundle.putParcelable(Constants.ARGUMENT.LOCATION, (Location) object);
                activity.showBottomSheet(new MasterLocationBottomSheet(), bundle);
                break;
            case GrocyApi.ENTITY.STORES:
                bundle.putParcelable(Constants.ARGUMENT.STORE, (Store) object);
                activity.showBottomSheet(new MasterStoreBottomSheet(), bundle);
                break;
            case GrocyApi.ENTITY.PRODUCTS:
                Product product = (Product) object;
                bundle.putParcelable(Constants.ARGUMENT.PRODUCT, product);
                bundle.putParcelable(
                        Constants.ARGUMENT.LOCATION,
                        locationsMap.get(product.getLocationId())
                );
                bundle.putParcelable(
                        Constants.ARGUMENT.QUANTITY_UNIT_PURCHASE,
                        quantityUnitsMap.get(product.getQuIdPurchase())
                );
                bundle.putParcelable(
                        Constants.ARGUMENT.QUANTITY_UNIT_STOCK,
                        quantityUnitsMap.get(product.getQuIdStock())
                );
                ProductGroup productGroup = NumUtil.isStringInt(product.getProductGroupId())
                        ? productGroupsMap.get(Integer.parseInt(product.getProductGroupId()))
                        : null;
                bundle.putParcelable(Constants.ARGUMENT.PRODUCT_GROUP, productGroup);
                activity.showBottomSheet(new MasterProductBottomSheet(), bundle);
        }
    }

    private void setUpSearch() {
        if(search.isEmpty()) { // only if no search is active
            appBarBehavior.switchToSecondary();
            binding.editTextMasterObjectListSearch.setText("");
        }
        binding.editTextMasterObjectListSearch.requestFocus();
        activity.showKeyboard(binding.editTextMasterObjectListSearch);

        setIsSearchVisible(true);
    }

    @Override
    public void dismissSearch() {
        appBarBehavior.switchToPrimary();
        activity.hideKeyboard();
        binding.editTextMasterObjectListSearch.setText("");
        filterObjects();

        setIsSearchVisible(false);
    }

    @Override
    public void deleteObjectSafely(Object object) {
        String objectName = ObjectUtil.getObjectName(object, entity);
        int objectId = ObjectUtil.getObjectId(object, entity);
        int entityStrId;
        switch (entity) {
            case GrocyApi.ENTITY.PRODUCTS:
                entityStrId = R.string.property_product;
                break;
            case GrocyApi.ENTITY.QUANTITY_UNITS:
                entityStrId = R.string.property_quantity_unit;
                break;
            case GrocyApi.ENTITY.LOCATIONS:
                entityStrId = R.string.property_location;
                break;
            case GrocyApi.ENTITY.PRODUCT_GROUPS:
                entityStrId = R.string.property_product_group;
                break;
            default: // STORES
                entityStrId = R.string.property_store;
        }
        String entityText = activity.getString(entityStrId);

        if(!entity.equals(GrocyApi.ENTITY.PRODUCTS)) {
            for(Object p : products) {
                Product product = (Product) p;
                if(entity.equals(GrocyApi.ENTITY.QUANTITY_UNITS)
                        && product.getQuIdStock() != objectId
                        && product.getQuIdPurchase() != objectId
                        || entity.equals(GrocyApi.ENTITY.LOCATIONS)
                        && product.getLocationId() == objectId
                        || entity.equals(GrocyApi.ENTITY.PRODUCT_GROUPS)
                        && NumUtil.isStringInt(product.getProductGroupId())
                        && Integer.parseInt(product.getProductGroupId()) == objectId
                        || entity.equals(GrocyApi.ENTITY.STORES)
                        && NumUtil.isStringInt(product.getStoreId())
                        && Integer.parseInt(product.getStoreId()) == objectId
                ) continue;

                activity.showSnackbar(
                        Snackbar.make(
                                activity.binding.frameMainContainer,
                                activity.getString(R.string.msg_master_delete_usage, entityText),
                                Snackbar.LENGTH_LONG
                        )
                );
                return;
            }
            showMasterDeleteBottomSheet(entityText, objectName, objectId);
        } else { // PRODUCTS
            dlHelper.getProductDetails(ObjectUtil.getObjectId(object, entity), productDetails -> {
                if(productDetails != null && productDetails.getStockAmount() == 0) {
                    showMasterDeleteBottomSheet(entityText, objectName, objectId);
                } else {
                    showMessage(activity.getString(R.string.msg_master_delete_stock));
                }
            }, error -> showMessage(activity.getString(R.string.error_check_usage)))
                    .perform(dlHelper.getUuid());
        }
    }

    private void showMasterDeleteBottomSheet(String entityText, String objectName, int objectId) {
        Bundle argsBundle = new Bundle();
        argsBundle.putString(Constants.ARGUMENT.ENTITY_TEXT, entityText);
        argsBundle.putInt(Constants.ARGUMENT.OBJECT_ID, objectId);
        argsBundle.putString(Constants.ARGUMENT.OBJECT_NAME, objectName);
        activity.showBottomSheet(new MasterDeleteBottomSheet(), argsBundle);
    }

    @Override
    public void deleteObject(int objectId) {
        dlHelper.delete(
                grocyApi.getObject(entity, objectId),
                response -> {
                    int index = getObjectPosition(objectId);
                    if(index != -1) {
                        displayedObjects.remove(index);
                        masterObjectListAdapter.notifyItemRemoved(index);
                    } else {
                        // quantityUnit not found, fall back to complete refresh
                        refresh();
                    }
                },
                error -> showMessage(activity.getString(R.string.error_undefined))
        );
    }

    /**
     * Returns index in the displayed items.
     * Used for providing a safe and up-to-date value
     * e.g. when the items are filtered/sorted before server responds
     */
    private int getObjectPosition(int quantityUnitId) {
        for(int i = 0; i < displayedObjects.size(); i++) {
            if(ObjectUtil.getObjectId(displayedObjects.get(i), entity) == quantityUnitId) {
                return i;
            }
        }
        return 0;
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
