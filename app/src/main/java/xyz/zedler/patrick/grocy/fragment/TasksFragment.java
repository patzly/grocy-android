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
import xyz.zedler.patrick.grocy.adapter.MasterPlaceholderAdapter;
import xyz.zedler.patrick.grocy.adapter.TasksItemAdapter;
import xyz.zedler.patrick.grocy.behavior.AppBarBehavior;
import xyz.zedler.patrick.grocy.behavior.SwipeBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentTasksBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.Task;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.ACTION;
import xyz.zedler.patrick.grocy.util.Constants.FAB.POSITION;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.TasksViewModel;

public class TasksFragment extends BaseFragment implements
    TasksItemAdapter.TasksItemAdapterListener {

  private final static String TAG = TasksFragment.class.getSimpleName();

  private MainActivity activity;
  private TasksViewModel viewModel;
  private AppBarBehavior appBarBehavior;
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

    appBarBehavior = new AppBarBehavior(
        activity,
        binding.appBarDefault,
        binding.appBarSearch,
        savedInstanceState
    );

    binding.recycler.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    binding.recycler.setAdapter(new MasterPlaceholderAdapter());

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
        } else {
          info = new InfoFullscreen(InfoFullscreen.INFO_EMPTY_TASKS);
        }
        viewModel.getInfoFullscreenLive().setValue(info);
      } else {
        viewModel.getInfoFullscreenLive().setValue(null);
      }
      if (binding.recycler.getAdapter() instanceof TasksItemAdapter) {
        ((TasksItemAdapter) binding.recycler.getAdapter()).updateData(
            items,
            viewModel.getSortMode()
        );
      } else {
        binding.recycler.setAdapter(
            new TasksItemAdapter(
                requireContext(),
                items,
                this,
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
          int position = viewHolder.getAdapterPosition();
          ArrayList<Task> displayedItems = viewModel.getFilteredTasksLive()
              .getValue();
          if (displayedItems == null || position < 0
              || position >= displayedItems.size()) {
            return;
          }
          Task task = displayedItems.get(position);
          underlayButtons.add(new UnderlayButton(
              R.drawable.ic_round_done,
              pos -> {
                if (pos >= displayedItems.size()) {
                  return;
                }
                swipeBehavior.recoverLatestSwipedItem();
                viewModel.performAction(
                    ACTION.UNDO,
                    displayedItems.get(pos)
                );
              }
          ));
        }
      };
    }
    swipeBehavior.attachToRecyclerView(binding.recycler);

    hideDisabledFeatures();

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    updateUI(savedInstanceState == null);
  }

  private void updateUI(boolean animated) {
    activity.getScrollBehavior().setUpScroll(binding.recycler);
    activity.getScrollBehavior().setHideOnScroll(true);
    activity.updateBottomAppBar(
        POSITION.CENTER,
        R.menu.menu_tasks,
        this::onMenuItemClick
    );
    activity.updateFab(
        R.drawable.ic_round_add_anim,
        R.string.action_add,
        Constants.FAB.TAG.ADD,
        animated,
        () -> {}
    );
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    if (appBarBehavior != null) {
      appBarBehavior.saveInstanceState(outState);
    }
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
      ViewUtil.startIcon(item);
      setUpSearch();
      return true;
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