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
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.CalendarWeekAdapter;
import xyz.zedler.patrick.grocy.adapter.StockOverviewItemAdapter;
import xyz.zedler.patrick.grocy.adapter.StockPlaceholderAdapter;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMealPlanBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.helper.SnapToBlockHelper;
import xyz.zedler.patrick.grocy.helper.SnapToBlockHelper.SnapBlockCallback;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.view.singlerowcalendar.Week;
import xyz.zedler.patrick.grocy.viewmodel.MealPlanViewModel;

public class MealPlanFragment extends BaseFragment {

  private final static String TAG = MealPlanFragment.class.getSimpleName();

  private MainActivity activity;
  private MealPlanViewModel viewModel;
  private ClickUtil clickUtil;
  private FragmentMealPlanBinding binding;
  private InfoFullscreenHelper infoFullscreenHelper;
  private SystemBarBehavior systemBarBehavior;
  private CalendarWeekAdapter calendarWeekAdapter;

  private boolean suppressNextSelectEvent;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentMealPlanBinding.inflate(inflater, container, false);
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
    StockOverviewFragmentArgs args = StockOverviewFragmentArgs.fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this, new MealPlanViewModel
        .MealPlanViewModelFactory(activity.getApplication(), args)
    ).get(MealPlanViewModel.class);
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

    binding.toolbarDefault.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());

    binding.recycler.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    binding.recycler.setAdapter(new StockPlaceholderAdapter());

    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    setupCalendarView();

    viewModel.getFilteredStockItemsLive().observe(getViewLifecycleOwner(), items -> {
      if (items == null) return;
      if (binding.recycler.getAdapter() instanceof StockOverviewItemAdapter) {

      } else {
      }
    });

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(((SnackbarMessage) event).getSnackbar(
            activity.binding.coordinatorMain
        ));
      }
    });

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

  private void setupCalendarView() {
    LinearLayoutManager layoutManagerCalendar = new LinearLayoutManager(
        requireContext(),
        LinearLayoutManager.HORIZONTAL,
        false
    );
    binding.recyclerCalendar.setLayoutManager(layoutManagerCalendar);
    calendarWeekAdapter = new CalendarWeekAdapter(
        requireContext(),
        week -> {
          if (week == null) return;
          viewModel.showMessage(week.getSelectedDate().toString());
        },
        CalendarWeekAdapter.DIFF_CALLBACK
    );
    binding.recyclerCalendar.setAdapter(calendarWeekAdapter);

    SnapToBlockHelper snapToBlockHelper = new SnapToBlockHelper(1);
    snapToBlockHelper.attachToRecyclerView(binding.recyclerCalendar);
    snapToBlockHelper.setSnapBlockCallback(new SnapBlockCallback() {
      @Override
      public void onBlockSnap(int snapPosition) {}

      @Override
      public void onBlockSnapped(int snapPosition) {
        Week previousSelected = viewModel.getSelectedWeek();
        if (!suppressNextSelectEvent) {
          calendarWeekAdapter.onSelect(snapPosition, previousSelected.getSelectedDayOfWeek());
        } else {
          suppressNextSelectEvent = false;
        }
        updateWeekDatesText(snapPosition);
      }
    });

    LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(requireContext());


    viewModel.getHorizontalCalendarSource().observe(getViewLifecycleOwner(), weeks -> {
      boolean initial =
          calendarWeekAdapter.getCurrentList() == null || calendarWeekAdapter.getItemCount() == 0;
      calendarWeekAdapter.submitList(weeks);

      if (initial && weeks.size() > 0) {
        new Handler().postDelayed(() -> {
          int position = calendarWeekAdapter.getPositionOfWeek(
              viewModel.getSelectedWeek().getStartDate()
          );
          if (position == -1) {
            return;
          }
          binding.recyclerCalendar.scrollToPosition(position);
          updateWeekDatesText(position);
        }, 100);
      }
    });

    binding.today.setOnClickListener(v -> {
      LocalDate today = viewModel.getToday();
      int position = viewModel.getCalendarPosition(today);

      if (position == -1) {
        return;
      }

      linearSmoothScroller.setTargetPosition(position);
      if (layoutManagerCalendar.findFirstVisibleItemPosition() != position) {
        suppressNextSelectEvent = true;
      }
      calendarWeekAdapter.onSelect(position, viewModel.getDayOffsetToWeekStart(today));
      layoutManagerCalendar.startSmoothScroll(linearSmoothScroller);
    });
  }

  private void updateWeekDatesText(int position) {
    PagedList<Week> currentList = this.calendarWeekAdapter.getCurrentList();
    if (currentList == null || position+1 > currentList.size()) return;
    Week week = currentList.get(position);
    if (week == null) return;
    binding.weekDates.setText(
        getString(
            R.string.date_timespan,
            week.getStartDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
            week.getStartDate().plusDays(6).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        )
    );
  }

  @Override
  public void performAction(String action, StockItem stockItem) {
    viewModel.performAction(action, stockItem);
  }

  private boolean onMenuItemClick(MenuItem item) {
    return false;
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

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}