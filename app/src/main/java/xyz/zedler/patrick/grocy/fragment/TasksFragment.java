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

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.TasksItemAdapter;
import xyz.zedler.patrick.grocy.adapter.TasksPlaceholderAdapter;
import xyz.zedler.patrick.grocy.behavior.AppBarBehaviorNew;
import xyz.zedler.patrick.grocy.behavior.SwipeBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentTasksBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.HorizontalFilterBarMultiTasks;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.viewmodel.TasksViewModel;

public class TasksFragment extends BaseFragment implements
    TasksItemAdapter.TasksItemAdapterListener {

  private final static String TAG = TasksFragment.class.getSimpleName();

  private MainActivity activity;
  private TasksViewModel viewModel;
  private AppBarBehaviorNew appBarBehavior;
  private ClickUtil clickUtil;
  private SwipeBehavior swipeBehavior;
  private FragmentTasksBinding binding;
  private InfoFullscreenHelper infoFullscreenHelper;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentTasksBinding.inflate(inflater, container, false);
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
    viewModel = new ViewModelProvider(this).get(TasksViewModel.class);
    viewModel.setOfflineLive(!activity.isOnline());
    binding.setViewModel(viewModel);
    binding.setActivity(activity);
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    infoFullscreenHelper = new InfoFullscreenHelper(binding.frame);
    clickUtil = new ClickUtil();

    // APP BAR BEHAVIOR

    appBarBehavior = new AppBarBehaviorNew(
        activity,
        binding.appBarDefault,
        binding.appBarSearch,
        savedInstanceState
    );

    binding.recycler.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    binding.recycler.setAdapter(new TasksPlaceholderAdapter());

    if (savedInstanceState == null) {
      binding.recycler.scrollToPosition(0);
      viewModel.resetSearch();
    }

    viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), state -> {
      if (!state) {
        viewModel.setCurrentQueueLoading(null);
      }
    });

    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    viewModel.getFilteredTasksLive().observe(getViewLifecycleOwner(), items -> {
      if (items == null) {
        return;
      }
      if (items.isEmpty()) {
        InfoFullscreen info;
        if (viewModel.isSearchActive()) {
          info = new InfoFullscreen(InfoFullscreen.INFO_NO_SEARCH_RESULTS);
        } else if (!viewModel.getHorizontalFilterBarTasksSingle().isNoFilterActive()) {
          info = new InfoFullscreen(InfoFullscreen.INFO_NO_FILTER_RESULTS);
        } else if (viewModel.getHorizontalFilterBarTasksMulti().areFiltersActive()) {
          info = new InfoFullscreen(InfoFullscreen.INFO_NO_FILTER_RESULTS);
        } else {
          info = new InfoFullscreen(InfoFullscreen.INFO_EMPTY_STOCK);
        }
        viewModel.getInfoFullscreenLive().setValue(info);
      } else {
        viewModel.getInfoFullscreenLive().setValue(null);
      }
      if (binding.recycler.getAdapter() instanceof TasksItemAdapter) {
        ((TasksItemAdapter) binding.recycler.getAdapter()).updateData(
            items,
            viewModel.getTasksDoneCount(),
            viewModel.getTasksNotDoneCount(),
            viewModel.getTasksDueCount(),
            viewModel.getTasksOverdueCount(),
            viewModel.getSortMode()
        );
      } else {
        binding.recycler.setAdapter(
            new TasksItemAdapter(
                requireContext(),
                items,
                this,
                viewModel.getHorizontalFilterBarTasksSingle(),
                viewModel.getHorizontalFilterBarTasksMulti(),
                viewModel.getTasksDoneCount(),
                viewModel.getTasksNotDoneCount(),
                viewModel.getTasksDueCount(),
                viewModel.getTasksOverdueCount(),
                true,
                5,
                viewModel.getSortMode()
            )
        );
      }
    });

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(((SnackbarMessage) event).getSnackbar(
            activity,
            activity.binding.frameMainContainer
        ));
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
          int position = viewHolder.getAdapterPosition() - 2;
          ArrayList<Task> displayedItems = viewModel.getFilteredTasksLive()
              .getValue();
          if (displayedItems == null || position < 0
              || position >= displayedItems.size()) {
            return;
          }
          Task task = displayedItems.get(position);
          if (task.getAmountAggregatedDouble() > 0
              && task.getProduct().getEnableTareWeightHandlingInt() == 0
          ) {
            underlayButtons.add(new UnderlayButton(
                R.drawable.ic_round_consume_product,
                pos -> {
                  if (pos - 2 >= displayedItems.size()) {
                    return;
                  }
                  swipeBehavior.recoverLatestSwipedItem();
                  viewModel.performAction(
                      Constants.ACTION.CONSUME,
                      displayedItems.get(pos - 2)
                  );
                }
            ));
          }
          if (task.getAmountAggregatedDouble()
              > task.getAmountOpenedAggregatedDouble()
              && task.getProduct().getEnableTareWeightHandlingInt() == 0
              && viewModel.isFeatureEnabled(Constants.PREF.FEATURE_STOCK_OPENED_TRACKING)
          ) {
            underlayButtons.add(new UnderlayButton(
                R.drawable.ic_round_open,
                pos -> {
                  if (pos - 2 >= displayedItems.size()) {
                    return;
                  }
                  swipeBehavior.recoverLatestSwipedItem();
                  viewModel.performAction(
                      Constants.ACTION.OPEN,
                      displayedItems.get(pos - 2)
                  );
                }
            ));
          }
          if (underlayButtons.isEmpty()) {
            underlayButtons.add(new UnderlayButton(
                R.drawable.ic_round_close,
                pos -> swipeBehavior.recoverLatestSwipedItem()
            ));
          }
        }
      };
    }
    swipeBehavior.attachToRecyclerView(binding.recycler);

    hideDisabledFeatures();

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    updateUI(ShoppingListFragmentArgs.fromBundle(requireArguments()).getAnimateStart()
        && savedInstanceState == null);
  }

  private void updateUI(boolean animated) {
    activity.getScrollBehavior().setUpScroll(binding.recycler);
    activity.getScrollBehavior().setHideOnScroll(true);
    activity.updateBottomAppBar(
        Constants.FAB.POSITION.GONE,
        R.menu.menu_tasks,
        this::onMenuItemClick
    );
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    if (appBarBehavior != null) {
      appBarBehavior.saveInstanceState(outState);
    }
  }

  @Override
  public void performAction(String action, Task task) {
    viewModel.performAction(action, task);
  }

  private boolean showOfflineError() {
    if (viewModel.isOffline()) {
      showMessage(getString(R.string.error_offline));
      return true;
    }
    return false;
  }

  private boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.action_search) {
      IconUtil.start(item);
      setUpSearch();
      return true;
    } else if (item.getItemId() == R.id.action_sort) {
      SubMenu menuSort = item.getSubMenu();
      MenuItem sortName = menuSort.findItem(R.id.action_sort_name);
      MenuItem sortBBD = menuSort.findItem(R.id.action_sort_bbd);
      MenuItem sortAscending = menuSort.findItem(R.id.action_sort_ascending);
      switch (viewModel.getSortMode()) {
        case TasksViewModel.SORT_NAME:
          sortName.setChecked(true);
          break;
        case TasksViewModel.SORT_DUE_DATE:
          sortBBD.setChecked(true);
          break;
      }
      sortAscending.setChecked(viewModel.isSortAscending());
      return true;
    } else if (item.getItemId() == R.id.action_sort_name) {
      if (!item.isChecked()) {
        item.setChecked(true);
        viewModel.setSortMode(TasksViewModel.SORT_NAME);
        viewModel.updateFilteredTasks();
      }
      return true;
    } else if (item.getItemId() == R.id.action_sort_bbd) {
      if (!item.isChecked()) {
        item.setChecked(true);
        viewModel.setSortMode(TasksViewModel.SORT_DUE_DATE);
        viewModel.updateFilteredTasks();
      }
      return true;
    } else if (item.getItemId() == R.id.action_sort_ascending) {
      item.setChecked(!item.isChecked());
      viewModel.setSortAscending(item.isChecked());
      return true;
    } else if (item.getItemId() == R.id.action_filter_task_group) {
      SubMenu menuProductGroups = item.getSubMenu();
      menuProductGroups.clear();
      ArrayList<TaskCategory> taskCategories = viewModel.getTaskCategoriesLive().getValue();
      if (taskCategories == null) {
        return true;
      }
      SortUtil.sortTaskCategoriesByName(requireContext(), taskCategories, true);
      for (TaskCategory tc : taskCategories) {
        menuProductGroups.add(tc.getName()).setOnMenuItemClickListener(pgItem -> {
          if (binding.recycler.getAdapter() == null) {
            return false;
          }
          viewModel.getHorizontalFilterBarTasksMulti().addFilter(
              HorizontalFilterBarMultiTasks.TASK_CATEGORY,
              new HorizontalFilterBarMultiTasks.Filter(tc.getName(), tc.getId())
          );
          binding.recycler.getAdapter().notifyItemChanged(1);
          return true;
        });
      }
    } else if (item.getItemId() == R.id.action_filter_location) {
      SubMenu menuLocations = item.getSubMenu();
      menuLocations.clear();
    }
    return false;
  }

  @Override
  public void onItemRowClicked(Task task) {
    if (clickUtil.isDisabled()) {
      return;
    }
    if (task == null) {
      return;
    }
    if (swipeBehavior != null) {
      swipeBehavior.recoverLatestSwipedItem();
    }
    showTaskOverview(task);
  }

  private void showTaskOverview(Task task) {
    if (task == null) {
      return;
    }
    navigate(TasksFragmentDirections
        .actionTasksFragmentToTaskOverviewBottomSheetDialogFragment()
        .setShowActions(true)
        .setTask(task));
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

  private void hideDisabledFeatures() {
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

  private void lockOrUnlockRotation() {
    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
  }

  private void showMessage(String msg) {
    activity.showSnackbar(
        Snackbar.make(activity.binding.frameMainContainer, msg, Snackbar.LENGTH_SHORT)
    );
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