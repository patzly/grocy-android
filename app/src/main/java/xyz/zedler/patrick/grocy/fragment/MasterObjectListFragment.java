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

import android.animation.LayoutTransition;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.transition.TransitionInflater;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.MasterObjectListAdapter;
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterObjectListBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.MasterDeleteBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.ObjectUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.viewmodel.EventHandler;
import xyz.zedler.patrick.grocy.viewmodel.MasterObjectListViewModel;

public class MasterObjectListFragment extends BaseFragment
        implements MasterObjectListAdapter.MasterObjectListAdapterListener {

    private final static String TAG = MasterObjectListFragment.class.getSimpleName();

    private MainActivity activity;
    private AppBarBehavior appBarBehavior;
    private ClickUtil clickUtil;
    private InfoFullscreenHelper infoFullscreenHelper;
    private FragmentMasterObjectListBinding binding;
    private MasterObjectListViewModel viewModel;

    private ArrayList<Object> objects;
    private ArrayList<Object> filteredObjects;
    private ArrayList<Object> displayedObjects;
    private ArrayList<ProductGroup> productGroups;

    private String entity;
    private String search;

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

        if(infoFullscreenHelper != null) {
            infoFullscreenHelper.destroyInstance();
            infoFullscreenHelper = null;
        }
        if(binding != null) {
            binding.recycler.animate().cancel();
            binding = null;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) requireActivity();
        clickUtil = new ClickUtil();

        viewModel = new ViewModelProvider(this, new MasterObjectListViewModel
                .MasterObjectListViewModelFactory(activity.getApplication(), entity)
        ).get(MasterObjectListViewModel.class);
        viewModel.setOfflineLive(!activity.isOnline());

        viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), state -> {
            binding.swipe.setRefreshing(state);
            if(!state) viewModel.setCurrentQueueLoading(null);
        });
        binding.swipe.setOnRefreshListener(() -> viewModel.downloadData());
        // for offline info in app bar
        binding.swipe.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        binding.swipe.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(activity, R.color.surface)
        );
        binding.swipe.setColorSchemeColors(ContextCompat.getColor(activity, R.color.secondary));

        viewModel.getDisplayedItemsLive().observe(getViewLifecycleOwner(), objects -> {
            if(objects == null) return;
            if(objects.isEmpty()) {
                InfoFullscreen info;
                /*if(viewModel.isSearchActive()) {
                    info = new InfoFullscreen(InfoFullscreen.INFO_NO_SEARCH_RESULTS);
                } else if(viewModel.getFilterState() != -1) {
                    info = new InfoFullscreen(InfoFullscreen.INFO_NO_FILTER_RESULTS);
                } else {
                    info = new InfoFullscreen(InfoFullscreen.INFO_EMPTY_SHOPPING_LIST);
                }
                viewModel.getInfoFullscreenLive().setValue(info);*/
            } else {
                viewModel.getInfoFullscreenLive().setValue(null);
            }
            if(binding.recycler.getAdapter() instanceof MasterObjectListAdapter) {
                ((MasterObjectListAdapter) binding.recycler.getAdapter()).updateData(objects);
            } else {
                binding.recycler.setAdapter(
                        new MasterObjectListAdapter(entity, objects, this)
                );
            }
        });

        viewModel.getEventHandler().observe(getViewLifecycleOwner(),
                (EventHandler.EventObserver) event -> {
            if(event.getType() == Event.SNACKBAR_MESSAGE) {
                SnackbarMessage msg = (SnackbarMessage) event;
                Snackbar snackbar = msg.getSnackbar(activity, activity.binding.frameMainContainer);
                activity.showSnackbar(snackbar);
            } else if(event.getType() == Event.BOTTOM_SHEET) {
                BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
                activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
            }
        });

        viewModel.getOfflineLive().observe(getViewLifecycleOwner(), this::appBarOfflineInfo);

        // INITIALIZE VARIABLES

        objects = new ArrayList<>();
        filteredObjects = new ArrayList<>();
        displayedObjects = new ArrayList<>();
        productGroups = new ArrayList<>();

        search = "";

        // INITIALIZE VIEWS

        binding.back.setOnClickListener(v -> activity.onBackPressed());
        binding.searchClose.setOnClickListener(v -> dismissSearch());
        binding.editTextSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                search = s != null ? s.toString() : "";
                searchObjects(search);
            }
        });
        binding.editTextSearch.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        Editable search = binding.editTextSearch.getText();
                        searchObjects(search != null ? search.toString() : "");
                        activity.hideKeyboard();
                        return true;
                    } return false;
                });

        infoFullscreenHelper = new InfoFullscreenHelper(binding.frameContainer);

        // APP BAR BEHAVIOR

        appBarBehavior = new AppBarBehavior(
                activity,
                R.id.app_bar_default,
                R.id.app_bar_search
        );

        binding.recycler.setLayoutManager(
                new LinearLayoutManager(
                        activity,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );
        binding.recycler.setItemAnimator(new DefaultItemAnimator());
        binding.recycler.setAdapter(new MasterPlaceholderAdapter());

        if(savedInstanceState == null) {
            viewModel.loadFromDatabase(true);
        }

        // UPDATE UI
        updateUI(true);
    }

    private void updateUI(boolean animated) {
        activity.showHideDemoIndicator(this, animated);
        activity.getScrollBehavior().setUpScroll(binding.recycler);
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

    private void filterObjects() {
        filteredObjects = objects;
        // SEARCH
        if(!search.isEmpty()) { // active search
            searchObjects(search);
        } else {
            // EMPTY STATES
            /*if(filteredObjects.isEmpty() && errorState.equals(Constants.STATE.NONE)) {
                infoFullscreenHelper.setEmpty();
            } else {
                infoFullscreenHelper.clearState();
            }*/
            // SORTING
            /*if(displayedObjects != filteredObjects || isRestoredInstance) {
                displayedObjects = filteredObjects;
                sortObjects();
            }
            isRestoredInstance = false;*/
        }
        /*if(debug) Log.i(
                TAG, "filterQuantityUnits: filteredQuantityUnits = " + filteredObjects
        );*/
    }

    private void searchObjects(String search) {
        search = search.toLowerCase();
        //if(debug) Log.i(TAG, "searchObjects: search = " + search);
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
            /*if(searchedObjects.isEmpty() && errorState.equals(Constants.STATE.NONE)) {
                infoFullscreenHelper.setNoSearchResults();
            } else {
                infoFullscreenHelper.clearState();
            }*/
            if(displayedObjects != searchedObjects) {
                displayedObjects = searchedObjects;
                //sortObjects();
            }
            /*if(debug) Log.i(
                    TAG,
                    "searchObjects: searchedObjects = " + searchedObjects
            );*/
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
        // SORTING
        MenuItem itemSort = activity.getBottomMenu().findItem(R.id.action_sort_ascending);
        itemSort.setIcon(
                viewModel.isSortAscending()
                        ? R.drawable.ic_round_sort_desc_to_asc_anim
                        : R.drawable.ic_round_sort_asc_to_desc
        );
        itemSort.getIcon().setAlpha(255);
        itemSort.setOnMenuItemClickListener(item -> {
            viewModel.setSortAscending(!viewModel.isSortAscending());
            item.setIcon(
                    viewModel.isSortAscending()
                            ? R.drawable.ic_round_sort_asc_to_desc
                            : R.drawable.ic_round_sort_desc_to_asc_anim
            );
            item.getIcon().setAlpha(255);
            IconUtil.start(item);
            return true;
        });

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
        viewModel.showObjectBottomSheetOfDisplayedItem(position);
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

    private void setUpSearch() {
        if(search.isEmpty()) { // only if no search is active
            appBarBehavior.switchToSecondary();
            binding.editTextSearch.setText("");
        }
        binding.editTextSearch.requestFocus();
        activity.showKeyboard(binding.editTextSearch);

        setIsSearchVisible(true);
    }

    @Override
    public void dismissSearch() {
        appBarBehavior.switchToPrimary();
        activity.hideKeyboard();
        binding.editTextSearch.setText("");
        filterObjects();

        setIsSearchVisible(false);
    }

    @Override
    public void deleteObjectSafely(Object object) {
        deleteObjectSafely(object);
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
        viewModel.deleteObject(objectId);
    }

    private void appBarOfflineInfo(boolean visible) {
        boolean currentState = binding.linearOfflineError.getVisibility() == View.VISIBLE;
        if(visible == currentState) return;
        binding.linearOfflineError.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void updateConnectivity(boolean online) {
        if(!online == viewModel.isOffline()) return;
        viewModel.setOfflineLive(!online);
        viewModel.downloadData();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
