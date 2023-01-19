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
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.StockLogEntryAdapter;
import xyz.zedler.patrick.grocy.adapter.StockLogEntryAdapter.PaginationScrollListener;
import xyz.zedler.patrick.grocy.adapter.StockLogEntryAdapter.StockLogEntryAdapterListener;
import xyz.zedler.patrick.grocy.adapter.StockPlaceholderAdapter;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.behavior.SwipeBehavior;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentStockJournalBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.StockLogEntry;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.StockJournalViewModel;

public class StockJournalFragment extends BaseFragment implements StockLogEntryAdapterListener {

  private final static String TAG = StockJournalFragment.class.getSimpleName();

  private static final String DIALOG_SHOWING = "dialog_showing";
  private static final String DIALOG_ENTRY = "dialog_entry";

  private MainActivity activity;
  private StockJournalViewModel viewModel;
  private AppBarBehavior appBarBehavior;
  private ClickUtil clickUtil;
  private SwipeBehavior swipeBehavior;
  private FragmentStockJournalBinding binding;
  private InfoFullscreenHelper infoFullscreenHelper;
  private AlertDialog dialog;
  private StockLogEntry dialogEntry;
  private SystemBarBehavior systemBarBehavior;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentStockJournalBinding.inflate(inflater, container, false);
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
    viewModel = new ViewModelProvider(this).get(StockJournalViewModel.class);
    viewModel.setOfflineLive(!activity.isOnline());
    binding.setViewModel(viewModel);
    binding.setActivity(activity);
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    infoFullscreenHelper = new InfoFullscreenHelper(binding.frame);
    clickUtil = new ClickUtil();

    systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipe);
    systemBarBehavior.setRecycler(binding.recycler);
    systemBarBehavior.setUp();

    binding.toolbarDefault.setNavigationOnClickListener(v -> activity.navigateUp());

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

    binding.recycler.addOnScrollListener(new PaginationScrollListener(
        (LinearLayoutManager) binding.recycler.getLayoutManager()) {
      @Override
      protected void loadMoreItems() {
        if (binding.recycler.getAdapter() instanceof StockPlaceholderAdapter) return;
        viewModel.setCurrentPage(viewModel.getCurrentPage() + 1);
        viewModel.loadNextPage(stockLogEntries -> {
          if (stockLogEntries.size() == 0) {
            viewModel.setLastPage(true);
            return;
          }
          StockLogEntryAdapter adapter = (StockLogEntryAdapter) binding.recycler.getAdapter();
          if (adapter == null) return;
          adapter.addAll(stockLogEntries);
        });
      }

      @Override
      public boolean isLastPage() {
        return viewModel.isLastPage();
      }

      @Override
      public boolean isLoading() {
        return viewModel.getIsLoadingLive().getValue() != null
            && viewModel.getIsLoadingLive().getValue();
      }
    });

    if (savedInstanceState == null) {
      binding.recycler.scrollToPosition(0);
      viewModel.resetSearch();
    }

    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    viewModel.getFilteredStockLogEntriesLive().observe(getViewLifecycleOwner(), items -> {
      if (items == null) return;
      binding.recycler.setAdapter(
          new StockLogEntryAdapter(
              requireContext(),
              items,
              viewModel.getQuantityUnitHashMap(),
              viewModel.getProductHashMap(),
              viewModel.getLocationHashMap(),
              viewModel.getUserHashMap(),
              this
          )
      );
      binding.recycler.scheduleLayoutAnimation();
      viewModel.setLastPage(false);
      viewModel.setCurrentPage(0);
    });

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
          if (!(binding.recycler.getAdapter() instanceof StockLogEntryAdapter)) return;
          int position = viewHolder.getAdapterPosition();
          ArrayList<StockLogEntry> stockLogEntries =
              ((StockLogEntryAdapter) binding.recycler.getAdapter()).getStockLogEntries();
          if (stockLogEntries == null || position < 0 || position >= stockLogEntries.size()) {
            return;
          }
          StockLogEntry entry = stockLogEntries.get(position);
          if (entry.getUndoneBoolean()) return;
          underlayButtons.add(new UnderlayButton(
              activity,
              R.drawable.ic_round_undo,
              pos -> {
                if (pos >= stockLogEntries.size()) {
                  return;
                }
                swipeBehavior.recoverLatestSwipedItem();
                viewModel.undoTransaction(entry);
              }
          ));
        }
      };
    }
    swipeBehavior.attachToRecyclerView(binding.recycler);

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    // UPDATE UI

    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.recycler, true, true
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(false, R.menu.menu_empty, this::onMenuItemClick);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);

    boolean isShowing = dialog != null && dialog.isShowing();
    outState.putBoolean(DIALOG_SHOWING, isShowing);
    if (isShowing) {
      outState.putParcelable(DIALOG_ENTRY, dialogEntry);
    }
    if (appBarBehavior != null) {
      appBarBehavior.saveInstanceState(outState);
    }
  }

  @Override
  public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);
    if (savedInstanceState != null && savedInstanceState.getBoolean(DIALOG_SHOWING)) {
      StockLogEntry entry = savedInstanceState.getParcelable(DIALOG_ENTRY);
      new Handler(Looper.getMainLooper()).postDelayed(
          () -> showConfirmationDialog(entry), 1
      );
    }
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
  public void onItemRowClicked(StockLogEntry entry) {
    if (clickUtil.isDisabled()) {
      return;
    }
    if (entry == null || entry.getUndoneBoolean()) {
      return;
    }
    if (swipeBehavior != null) {
      swipeBehavior.recoverLatestSwipedItem();
    }
    showConfirmationDialog(entry);
  }

  @Override
  public void updateConnectivity(boolean isOnline) {
    if (!isOnline == viewModel.isOffline()) {
      return;
    }
    viewModel.setOfflineLive(!isOnline);
    systemBarBehavior.refresh();
    if (isOnline) {
      viewModel.downloadData();
    }
  }

  private void showConfirmationDialog(StockLogEntry entry) {
    dialogEntry = entry;
    dialog = new MaterialAlertDialogBuilder(
        activity, R.style.ThemeOverlay_Grocy_AlertDialog
    ).setTitle(R.string.msg_undo_transaction)
        .setPositiveButton(R.string.action_proceed, (dialog, which) -> {
          performHapticClick();
          viewModel.undoTransaction(entry);
        }).setNegativeButton(R.string.action_cancel, (dialog, which) -> performHapticClick())
        .setOnCancelListener(dialog -> performHapticClick())
        .create();
    dialog.show();
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
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}