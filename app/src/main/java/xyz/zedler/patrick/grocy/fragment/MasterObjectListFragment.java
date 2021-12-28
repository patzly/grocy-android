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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment;

import android.animation.LayoutTransition;
import android.os.Bundle;
import android.text.Editable;
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
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.MasterObjectListAdapter;
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.api.GrocyApi.ENTITY;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
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
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.ACTION;
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
    binding.title.setText(title);
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

    viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), state -> {
      binding.swipe.setRefreshing(state);
      if (!state) {
        viewModel.setCurrentQueueLoading(null);
      }
    });
    binding.swipe.setOnRefreshListener(() -> viewModel.downloadDataForceUpdate());
    // for offline info in app bar
    binding.swipe.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
    binding.swipe.setProgressBackgroundColorSchemeColor(
        ContextCompat.getColor(activity, R.color.surface)
    );
    binding.swipe.setColorSchemeColors(ContextCompat.getColor(activity, R.color.secondary));

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
        Snackbar snackbar = msg.getSnackbar(activity, activity.binding.frameMainContainer);
        activity.showSnackbar(snackbar);
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      }
    });

    viewModel.getOfflineLive().observe(getViewLifecycleOwner(), this::appBarOfflineInfo);

    if (savedInstanceState == null) {
      viewModel.deleteSearch(); // delete search if navigating back from other fragment
    }

    // INITIALIZE VIEWS

    binding.back.setOnClickListener(v -> activity.onBackPressed());
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
    updateUI(true);
  }

  private void updateUI(boolean animated) {
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
                  .actionMasterObjectListFragmentToMasterProductFragment(Constants.ACTION.CREATE));
              break;
            case ENTITY.TASK_CATEGORIES:
              break;
          }
        }
    );
  }

  public void setUpBottomMenu() {
    // sorting
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
      ViewUtil.startIcon(item);
      return true;
    });

    // search
    MenuItem search = activity.getBottomMenu().findItem(R.id.action_search);
    if (search == null) {
      return;
    }
    search.setOnMenuItemClickListener(item -> {
      ViewUtil.startIcon(item);
      setUpSearch();
      return true;
    });

    // product group filter
    if (entity.equals(GrocyApi.ENTITY.PRODUCTS)) {
      MenuItem menuItem = activity.getBottomMenu().findItem(R.id.action_filter);
      if (menuItem == null) {
        return;
      }
      SubMenu menuProductGroups = menuItem.getSubMenu();
      menuProductGroups.clear();
      ArrayList<ProductGroup> productGroups = viewModel.getProductGroups();
      if (productGroups != null && !productGroups.isEmpty()) {
        ArrayList<ProductGroup> sorted = new ArrayList<>(productGroups);
        SortUtil.sortProductGroupsByName(requireContext(), sorted, true);
        for (ProductGroup pg : sorted) {
          menuProductGroups.add(pg.getName()).setOnMenuItemClickListener(item -> {
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
        menuItem.setVisible(true);
      } else {
        menuItem.setVisible(false);
      }
    }
  }

  @Override
  public void onItemRowClicked(Object object) {
    if (clickUtil.isDisabled()) {
      return;
    }
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
        viewModel.showProductBottomSheet((Product) object);
        break;
    }
  }

  @Override
  public void editObject(Object object) {
    if (ENTITY.PRODUCTS.equals(entity)) {
      navigate(MasterObjectListFragmentDirections
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
    navigate(MasterObjectListFragmentDirections
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

  private void appBarOfflineInfo(boolean visible) {
    boolean currentState = binding.linearOfflineError.getVisibility() == View.VISIBLE;
    if (visible == currentState) {
      return;
    }
    binding.linearOfflineError.setVisibility(visible ? View.VISIBLE : View.GONE);
  }

  @Override
  public void updateConnectivity(boolean online) {
    if (!online == viewModel.isOffline()) {
      return;
    }
    viewModel.setOfflineLive(!online);
    viewModel.downloadData();
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
