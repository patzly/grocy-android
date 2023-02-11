/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment;

import android.animation.LayoutTransition;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ACTION;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.MasterObjectListAdapter;
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterObjectListBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.HorizontalFilterBarMulti;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
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
  private SystemBarBehavior systemBarBehavior;

  private String entity;

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
      case ENTITY.TASK_CATEGORIES:
        title = R.string.property_task_categories;
        break;
      default: // STORES
        title = R.string.property_stores;
    }
    binding.toolbarDefault.setTitle(title);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();

    if (infoFullscreenHelper != null) {
      infoFullscreenHelper.destroyInstance();
      infoFullscreenHelper = null;
    }
    if (binding != null) {
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
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipe);
    systemBarBehavior.setRecycler(binding.recycler);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);
    binding.setSystemBarBehavior(systemBarBehavior);

    viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), state -> {
      binding.swipe.setRefreshing(state);
      if (!state) {
        viewModel.setCurrentQueueLoading(null);
      }
    });
    binding.swipe.setOnRefreshListener(() -> viewModel.downloadDataForceUpdate());
    // for offline info in app bar
    binding.swipe.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

    viewModel.getDisplayedItemsLive().observe(getViewLifecycleOwner(), objects -> {
      if (objects == null) {
        return;
      }
      if (objects.isEmpty()) {
        InfoFullscreen info;
        if (viewModel.isSearchActive()) {
          info = new InfoFullscreen(InfoFullscreen.INFO_NO_SEARCH_RESULTS);
        } else {
          int fullscreenType;
          switch (entity) {
            case GrocyApi.ENTITY.PRODUCTS:
              fullscreenType = InfoFullscreen.INFO_EMPTY_PRODUCTS;
              break;
            case GrocyApi.ENTITY.QUANTITY_UNITS:
              fullscreenType = InfoFullscreen.INFO_EMPTY_QUS;
              break;
            case GrocyApi.ENTITY.LOCATIONS:
              fullscreenType = InfoFullscreen.INFO_EMPTY_LOCATIONS;
              break;
            case GrocyApi.ENTITY.PRODUCT_GROUPS:
              fullscreenType = InfoFullscreen.INFO_EMPTY_PRODUCT_GROUPS;
              break;
            case ENTITY.TASK_CATEGORIES:
              fullscreenType = InfoFullscreen.INFO_EMPTY_TASK_CATEGORIES;
              break;
            default: // STORES
              fullscreenType = InfoFullscreen.INFO_EMPTY_STORES;
          }
          info = new InfoFullscreen(fullscreenType);
        }
        viewModel.getInfoFullscreenLive().setValue(info);
      } else {
        viewModel.getInfoFullscreenLive().setValue(null);
      }
      if (binding.recycler.getAdapter() instanceof MasterObjectListAdapter) {
        ((MasterObjectListAdapter) binding.recycler.getAdapter()).updateData(objects);
      } else {
        binding.recycler.setAdapter(new MasterObjectListAdapter(
            getContext(),
            entity,
            objects,
            this,
            viewModel.getHorizontalFilterBarMulti()
        ));
        binding.recycler.scheduleLayoutAnimation();
      }
    });

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        SnackbarMessage msg = (SnackbarMessage) event;
        Snackbar snackbar = msg.getSnackbar(activity.binding.coordinatorMain);
        activity.showSnackbar(snackbar);
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      }
    });

    if (savedInstanceState == null) {
      viewModel.deleteSearch(); // delete search if navigating back from other fragment
    }

    // INITIALIZE VIEWS

    binding.toolbarDefault.setNavigationOnClickListener(v -> activity.onBackPressed());
    binding.searchClose.setOnClickListener(v -> dismissSearch());
    binding.editTextSearch.addTextChangedListener(new TextWatcher() {
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      public void afterTextChanged(Editable s) {
        if (appBarBehavior.isPrimaryLayout()) {
          return;
        }
        viewModel.setSearch(s != null ? s.toString() : "");
      }
    });
    binding.editTextSearch.setOnEditorActionListener(
        (TextView v, int actionId, KeyEvent event) -> {
          if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            activity.hideKeyboard();
            return true;
          }
          return false;
        });

    infoFullscreenHelper = new InfoFullscreenHelper(binding.frameContainer);
    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    // APP BAR BEHAVIOR

    appBarBehavior = new AppBarBehavior(
        activity,
        binding.appBarDefault,
        binding.appBarSearch,
        savedInstanceState
    );
    if (viewModel.isSearchActive()) {
      appBarBehavior.switchToSecondary();
    }

    binding.recycler.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    binding.recycler.setItemAnimator(new DefaultItemAnimator());
    binding.recycler.setAdapter(new MasterPlaceholderAdapter());

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    // UPDATE UI

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.recycler, true, true
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(
        true,
        !entity.equals(GrocyApi.ENTITY.PRODUCTS)
            ? viewModel.isSortAscending() ? R.menu.menu_master_items_asc : R.menu.menu_master_items_desc
            : viewModel.isSortAscending() ? R.menu.menu_master_products_asc : R.menu.menu_master_products_desc,
        getBottomMenuClickListener()
    );
    activity.updateFab(
        R.drawable.ic_round_add_anim,
        R.string.action_add,
        Constants.FAB.TAG.ADD,
        true,
        () -> {
          switch (entity) {
            case GrocyApi.ENTITY.QUANTITY_UNITS:
              activity.navigateFragment(MasterObjectListFragmentDirections
                  .actionMasterObjectListFragmentToMasterQuantityUnitFragment());
              break;
            case GrocyApi.ENTITY.PRODUCT_GROUPS:
              activity.navigateFragment(MasterObjectListFragmentDirections
                  .actionMasterObjectListFragmentToMasterProductGroupFragment());
              break;
            case GrocyApi.ENTITY.LOCATIONS:
              activity.navigateFragment(MasterObjectListFragmentDirections
                  .actionMasterObjectListFragmentToMasterLocationFragment());
              break;
            case GrocyApi.ENTITY.STORES:
              activity.navigateFragment(MasterObjectListFragmentDirections
                  .actionMasterObjectListFragmentToMasterStoreFragment());
              break;
            case GrocyApi.ENTITY.PRODUCTS:
              activity.navigateFragment(MasterObjectListFragmentDirections
                  .actionMasterObjectListFragmentToMasterProductFragment(Constants.ACTION.CREATE));
              break;
            case ENTITY.TASK_CATEGORIES:
              activity.navigateFragment(MasterObjectListFragmentDirections
                  .actionMasterObjectListFragmentToMasterTaskCategoryFragment());
              break;
          }
        }
    );
  }

  public Toolbar.OnMenuItemClickListener getBottomMenuClickListener() {
    return item -> {
      if (item.getItemId() == R.id.action_search) {
        ViewUtil.startIcon(item);
        setUpSearch();
        return true;
      } else if (item.getItemId() == R.id.action_filter) {
        SubMenu menuProductGroups = item.getSubMenu();
        menuProductGroups.clear();
        List<ProductGroup> productGroups = viewModel.getProductGroups();
        if (productGroups != null && !productGroups.isEmpty()) {
          ArrayList<ProductGroup> sorted = new ArrayList<>(productGroups);
          SortUtil.sortProductGroupsByName(sorted, true);
          for (ProductGroup pg : sorted) {
            menuProductGroups.add(pg.getName()).setOnMenuItemClickListener(itemTemp -> {
              if (binding.recycler.getAdapter() == null) {
                return false;
              }
              viewModel.getHorizontalFilterBarMulti().addFilter(
                  HorizontalFilterBarMulti.PRODUCT_GROUP,
                  new HorizontalFilterBarMulti.Filter(pg.getName(), pg.getId())
              );
              binding.recycler.getAdapter().notifyItemChanged(0);
              return true;
            });
          }
        } else {
          activity.showSnackbar(R.string.error_undefined);
        }
        return true;
      } else if (item.getItemId() == R.id.action_sort_ascending) {
        viewModel.setSortAscending(!viewModel.isSortAscending());
        item.setIcon(
            viewModel.isSortAscending()
                ? R.drawable.ic_round_sort_asc_to_desc
                : R.drawable.ic_round_sort_desc_to_asc_anim
        );
        item.getIcon().setAlpha(255);
        item.setChecked(viewModel.isSortAscending());
        ViewUtil.startIcon(item);
        return true;
      }
      return false;
    };
  }

  @Override
  public void onItemRowClicked(Object object) {
    if (clickUtil.isDisabled()) {
      return;
    }
    switch (entity) {
      case GrocyApi.ENTITY.QUANTITY_UNITS:
        activity.navigateFragment(MasterObjectListFragmentDirections
            .actionMasterObjectListFragmentToMasterQuantityUnitFragment()
            .setQuantityUnit((QuantityUnit) object));
        break;
      case GrocyApi.ENTITY.PRODUCT_GROUPS:
        activity.navigateFragment(MasterObjectListFragmentDirections
            .actionMasterObjectListFragmentToMasterProductGroupFragment()
            .setProductGroup((ProductGroup) object));
        break;
      case GrocyApi.ENTITY.LOCATIONS:
        activity.navigateFragment(MasterObjectListFragmentDirections
            .actionMasterObjectListFragmentToMasterLocationFragment()
            .setLocation((Location) object));
        break;
      case GrocyApi.ENTITY.STORES:
        activity.navigateFragment(MasterObjectListFragmentDirections
            .actionMasterObjectListFragmentToMasterStoreFragment()
            .setStore((Store) object));
        break;
      case GrocyApi.ENTITY.TASK_CATEGORIES:
        activity.navigateFragment(MasterObjectListFragmentDirections
            .actionMasterObjectListFragmentToMasterTaskCategoryFragment()
            .setTaskCategory((TaskCategory) object));
        break;
      case GrocyApi.ENTITY.PRODUCTS:
        viewModel.showProductBottomSheet((Product) object);
        break;
    }
  }

  @Override
  public void editObject(Object object) {
    if (ENTITY.PRODUCTS.equals(entity)) {
      activity.navigateFragment(MasterObjectListFragmentDirections
          .actionMasterObjectListFragmentToMasterProductFragment(
              ACTION.EDIT
          ).setProduct((Product) object));
    }
  }

  private void setUpSearch() {
    if (!viewModel.isSearchActive()) { // only if no search is active
      appBarBehavior.switchToSecondary();
      binding.editTextSearch.setText("");
    }
    binding.editTextSearch.requestFocus();
    activity.showKeyboard(binding.editTextSearch);

    viewModel.setIsSearchVisible(true);
  }

  @Override
  public boolean isSearchVisible() {
    return viewModel.isSearchVisible();
  }

  @Override
  public void dismissSearch() {
    appBarBehavior.switchToPrimary();
    activity.hideKeyboard();
    binding.editTextSearch.setText("");
    viewModel.setIsSearchVisible(false);
    viewModel.setSearch(null);
  }

  @Override
  public void copyProduct(Product product) {
    activity.navigateFragment(MasterObjectListFragmentDirections
        .actionMasterObjectListFragmentToMasterProductFragment(Constants.ACTION.CREATE)
        .setProduct(product));
  }

  @Override
  public void deleteObjectSafely(Object object) {
    viewModel.deleteObjectSafely(object);
  }

  @Override
  public void deleteObject(int objectId) {
    viewModel.deleteObject(objectId);
  }

  @Override
  public void updateConnectivity(boolean online) {
    if (!online == viewModel.isOffline()) {
      return;
    }
    viewModel.setOfflineLive(!online);
    if (online) viewModel.downloadData();
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
