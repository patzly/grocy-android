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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.StockEntryAdapter;
import xyz.zedler.patrick.grocy.adapter.StockEntryAdapter.StockEntryAdapterListener;
import xyz.zedler.patrick.grocy.adapter.StockPlaceholderAdapter;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.behavior.SwipeBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentStockEntriesBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScanner.BarcodeListener;
import xyz.zedler.patrick.grocy.scanner.EmbeddedFragmentScannerBundle;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.StockEntriesViewModel;

public class StockEntriesFragment extends BaseFragment implements StockEntryAdapterListener,
    BarcodeListener {

  private final static String TAG = StockEntriesFragment.class.getSimpleName();

  private MainActivity activity;
  private StockEntriesViewModel viewModel;
  private AppBarBehavior appBarBehavior;
  private ClickUtil clickUtil;
  private SwipeBehavior swipeBehavior;
  private FragmentStockEntriesBinding binding;
  private InfoFullscreenHelper infoFullscreenHelper;
  private EmbeddedFragmentScanner embeddedFragmentScanner;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentStockEntriesBinding.inflate(inflater, container, false);
    embeddedFragmentScanner = new EmbeddedFragmentScannerBundle(
        this,
        binding.containerScanner,
        this
    );
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
      binding.recycler.setAdapter(null);
      binding = null;
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    viewModel = new ViewModelProvider(this, new StockEntriesViewModel
        .StockEntriesViewModelFactory(activity.getApplication(),
        StockEntriesFragmentArgs.fromBundle(requireArguments())
    )).get(StockEntriesViewModel.class);
    viewModel.setOfflineLive(!activity.isOnline());
    binding.setViewModel(viewModel);
    binding.setActivity(activity);
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    infoFullscreenHelper = new InfoFullscreenHelper(binding.frame);
    clickUtil = new ClickUtil();

    // APP BAR BEHAVIOR

    appBarBehavior = new AppBarBehavior(
        activity,
        binding.appBarDefault,
        binding.appBarSearch,
        savedInstanceState
    );

    binding.recycler.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    binding.recycler.setAdapter(new StockPlaceholderAdapter());

    if (savedInstanceState == null) {
      binding.recycler.scrollToPosition(0);
      viewModel.resetSearch();
    }

    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    viewModel.getFilteredStockEntriesLive().observe(getViewLifecycleOwner(), items -> {
      if (items == null) return;
      if (binding.recycler.getAdapter() instanceof StockEntryAdapter) {
        ((StockEntryAdapter) binding.recycler.getAdapter()).updateData(
            requireContext(),
            items,
            viewModel.getQuantityUnitHashMap(),
            viewModel.getProductHashMap(),
            viewModel.getLocationHashMap(),
            viewModel.getStoreHashMap(),
            viewModel.getSortMode(),
            viewModel.isSortAscending(),
            viewModel.getGroupingMode()
        );
      } else {
        binding.recycler.setAdapter(
            new StockEntryAdapter(
                requireContext(),
                items,
                viewModel.getQuantityUnitHashMap(),
                viewModel.getProductHashMap(),
                viewModel.getLocationHashMap(),
                viewModel.getStoreHashMap(),
                this,
                viewModel.getSortMode(),
                viewModel.isSortAscending(),
                viewModel.getGroupingMode()
            )
        );
        binding.recycler.scheduleLayoutAnimation();
      }
    });

    embeddedFragmentScanner.setScannerVisibilityLive(viewModel.getScannerVisibilityLive());

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(((SnackbarMessage) event).getSnackbar(
            activity,
            activity.binding.coordinatorMain
        ));
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      }
    });

    if (swipeBehavior == null) {
      swipeBehavior = new SwipeBehavior(
          activity,
          swipeStarted -> binding.swipe.setEnabled(!swipeStarted)
      ) {
        @Override
        public void instantiateUnderlayButton(
            RecyclerView.ViewHolder viewHolder,
            List<UnderlayButton> underlayButtons
        ) {
          if (viewHolder.getItemViewType() != GroupedListItem.TYPE_ENTRY) return;
          if (!(binding.recycler.getAdapter() instanceof StockEntryAdapter)) return;
          int position = viewHolder.getAdapterPosition();
          ArrayList<GroupedListItem> groupedListItems =
              ((StockEntryAdapter) binding.recycler.getAdapter()).getGroupedListItems();
          if (groupedListItems == null || position < 0
              || position >= groupedListItems.size()) {
            return;
          }
          GroupedListItem item = groupedListItems.get(position);
          if (!(item instanceof StockEntry)) {
            return;
          }
          StockEntry stockEntry = (StockEntry) item;
          underlayButtons.add(new UnderlayButton(
              activity,
              R.drawable.ic_round_consume_product,
              pos -> {
                if (pos >= groupedListItems.size()) {
                  return;
                }
                swipeBehavior.recoverLatestSwipedItem();
                viewModel.performAction(Constants.ACTION.CONSUME, stockEntry);
              }
          ));
          Product product = viewModel.getProductHashMap().get(stockEntry.getProductId());
          if (product != null && product.getEnableTareWeightHandlingInt() == 0
              && viewModel.isFeatureEnabled(PREF.FEATURE_STOCK_OPENED_TRACKING)
              && stockEntry.getOpen() == 0
          ) {
            underlayButtons.add(new UnderlayButton(
                activity,
                R.drawable.ic_round_open,
                pos -> {
                  if (pos >= groupedListItems.size()) {
                    return;
                  }
                  swipeBehavior.recoverLatestSwipedItem();
                  viewModel.performAction(Constants.ACTION.OPEN, stockEntry);
                }
            ));
          }
        }
      };
    }
    swipeBehavior.attachToRecyclerView(binding.recycler);

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    updateUI();
  }

  private void updateUI() {
    activity.getScrollBehaviorOld().setUpScroll(binding.recycler);
    activity.getScrollBehaviorOld().setHideOnScroll(true);
    activity.updateBottomAppBar(
        false,
        viewModel.hasProductFilter() ? R.menu.menu_empty : R.menu.menu_stock_entries,
        this::onMenuItemClick
    );
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    if (appBarBehavior != null) {
      appBarBehavior.saveInstanceState(outState);
    }
  }

  public void toggleScannerVisibility() {
    viewModel.toggleScannerVisibility();
    if (viewModel.isScannerVisible()) {
      binding.editTextSearch.clearFocus();
      activity.hideKeyboard();
    } else {
      activity.showKeyboard(binding.editTextSearch);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    embeddedFragmentScanner.onResume();
  }

  @Override
  public void onPause() {
    embeddedFragmentScanner.onPause();
    super.onPause();
  }

  @Override
  public void onDestroy() {
    if (embeddedFragmentScanner != null) embeddedFragmentScanner.onDestroy();
    super.onDestroy();
  }

  @Override
  public void onBarcodeRecognized(String rawValue) {
    viewModel.toggleScannerVisibility();
    binding.editTextSearch.setText(rawValue);
  }

  public void toggleTorch() {
    embeddedFragmentScanner.toggleTorch();
  }

  @Override
  public void performAction(String action, StockEntry stockEntry) {
    viewModel.performAction(action, stockEntry);
  }

  private boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.action_search) {
      ViewUtil.startIcon(item);
      setUpSearch();
      return true;
    }
    return false;
  }

  @Override
  public void onItemRowClicked(StockEntry stockEntry) {
    if (clickUtil.isDisabled()) {
      return;
    }
    if (stockEntry == null) {
      return;
    }
    if (swipeBehavior != null) {
      swipeBehavior.recoverLatestSwipedItem();
    }
    viewModel.showStockEntryBottomSheet(stockEntry);
  }

  @Override
  public void updateConnectivity(boolean isOnline) {
    if (!isOnline == viewModel.isOffline()) {
      return;
    }
    viewModel.setOfflineLive(!isOnline);
    if (isOnline) {
      viewModel.downloadData();
    }
  }

  private void setUpSearch() {
    if (!viewModel.isSearchVisible()) {
      appBarBehavior.switchToSecondary();
      binding.editTextSearch.setText("");
    }
    binding.textInputSearch.requestFocus();
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
    if (viewModel.isScannerVisible()) {
      viewModel.toggleScannerVisibility();
    }
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    return setStatusBarColor(transit, enter, nextAnim, activity, R.color.primary);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}