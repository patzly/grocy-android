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
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;
import com.kizitonwose.calendar.core.Week;
import com.kizitonwose.calendar.core.WeekDay;
import com.kizitonwose.calendar.core.WeekDayPosition;
import com.kizitonwose.calendar.view.ViewContainer;
import com.kizitonwose.calendar.view.WeekDayBinder;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.MealPlanPagerAdapter;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMealPlanBinding;
import xyz.zedler.patrick.grocy.databinding.ViewCalendarDayLayoutBinding;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.viewmodel.MealPlanViewModel;

public class MealPlanFragment extends BaseFragment {

  private final static String TAG = MealPlanFragment.class.getSimpleName();

  private MainActivity activity;
  private MealPlanViewModel viewModel;
  private ClickUtil clickUtil;
  private FragmentMealPlanBinding binding;
  private SystemBarBehavior systemBarBehavior;

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
    if (binding != null) {
      binding = null;
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    viewModel = new ViewModelProvider(this, new MealPlanViewModel
        .MealPlanViewModelFactory(activity.getApplication())
    ).get(MealPlanViewModel.class);
    viewModel.setOfflineLive(!activity.isOnline());
    binding.setViewModel(viewModel);
    binding.setActivity(activity);
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    clickUtil = new ClickUtil();

    systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipe);
    systemBarBehavior.applyStatusBarInsetOnContainer(false);
    systemBarBehavior.applyAppBarInsetOnContainer(false);
    systemBarBehavior.setUp();

    binding.toolbarDefault.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());

    binding.calendarView.setDayBinder(new WeekDayBinder<DayViewContainer>() {
      @NonNull
      @Override
      public DayViewContainer create(@NonNull View view) {
        return new DayViewContainer(view);
      }

      @Override
      public void bind(@NonNull DayViewContainer container, WeekDay data) {
        container.binding.weekday.setText(
            data.getDate().getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.getDefault())
        );
        container.binding.day.setText(String.valueOf(data.getDate().getDayOfMonth()));

        int colorOutline = ResUtil.getColor(activity, R.attr.colorOutline);
        if (viewModel.getSelectedDate().isEqual(data.getDate())) {
          container.binding.card.setStrokeColor(colorOutline);
          container.binding.card.setCardBackgroundColor(colorOutline);
          container.binding.weekday.setTextColor(Color.WHITE);
          container.binding.day.setTextColor(Color.WHITE);
        } else {
          container.binding.card.setStrokeColor(colorOutline);
          container.binding.card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.transparent));
          int textColor = ResUtil.getColor(activity, R.attr.colorOnSurface);
          container.binding.weekday.setTextColor(textColor);
          container.binding.day.setTextColor(textColor);
        }
        container.binding.card.setOnClickListener((view) -> selectDate(data));
      }
    });

    YearMonth currentMonth = YearMonth.now();
    LocalDate startDate = currentMonth.minusMonths(5).atDay(1);
    LocalDate endDate = currentMonth.plusMonths(7).atEndOfMonth();
    binding.calendarView.setup(startDate, endDate, viewModel.getFirstDayOfWeek());
    binding.calendarView.setWeekScrollListener(week -> {
      binding.weekDates.setText(
          getString(
              R.string.date_timespan,
              week.getDays().get(0).getDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
              week.getDays().get(week.getDays().size()-1).getDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
          )
      );
      DayOfWeek selected = viewModel.getSelectedDate().getDayOfWeek();
      LocalDate newSelectedDay = week.getDays().get(0).getDate()
          .with(TemporalAdjusters.nextOrSame(DayOfWeek.of(selected.getValue())));
      selectDate(new WeekDay(newSelectedDay, WeekDayPosition.RangeDate));
      return null;
    });
    binding.calendarView.scrollToWeek(viewModel.getSelectedDate());
    binding.today.setOnClickListener(v -> {
      LocalDate date = LocalDate.now();
      DayOfWeek selected = date.getDayOfWeek();
      Week currentWeek = binding.calendarView.findFirstVisibleWeek();
      if (currentWeek == null) {
        selectDate(new WeekDay(date, WeekDayPosition.RangeDate));
        return;
      }
      LocalDate newSelectedDay = currentWeek.getDays().get(0).getDate()
          .with(TemporalAdjusters.nextOrSame(DayOfWeek.of(selected.getValue())));
      selectDate(new WeekDay(newSelectedDay, WeekDayPosition.RangeDate));
      binding.calendarView.smoothScrollToWeek(date);
    });

    MealPlanPagerAdapter adapter = new MealPlanPagerAdapter(
        this,
        viewModel.getSelectedDate()
    );
    binding.viewPager.setAdapter(adapter);


    binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
      @Override
      public void onPageSelected(int position) {
        LocalDate date = LocalDate.now().plusDays(position - Integer.MAX_VALUE / 2);
        selectDate(new WeekDay(date, WeekDayPosition.RangeDate));
        binding.calendarView.scrollToWeek(date);
      }
    });

    viewModel.getSelectedDateLive().observe(getViewLifecycleOwner(), date -> {
      int position = (int) ChronoUnit.DAYS.between(LocalDate.now(), date) + Integer.MAX_VALUE / 2;
      binding.viewPager.setCurrentItem(position, viewModel.isInitialScrollDone());
      if (!viewModel.isInitialScrollDone()) viewModel.setInitialScrollDone(true);
      viewModel.getWeekCostsTextLive().setValue(viewModel.getWeekCostsText());
    });

    viewModel.getFilterChipLiveDataHeaderFields().observe(getViewLifecycleOwner(), data -> {
      List<String> activeFields = viewModel.getFilterChipLiveDataHeaderFields().getActiveFields();
      if (activeFields.contains(MealPlanViewModel.FIELD_WEEK_COSTS)) {
        viewModel.getWeekCostsTextLive().setValue(viewModel.getWeekCostsText());
        binding.weekCosts.setVisibility(View.VISIBLE);
      } else {
        binding.weekCosts.setVisibility(View.GONE);
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

    activity.getScrollBehavior().setUpScroll(binding.appBar, false, null);
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(false, R.menu.menu_meal_plan, this::onMenuItemClick);
  }

  private boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.action_refresh) {
      viewModel.downloadData(true);
      return true;
    }
    return false;
  }

  public void showFieldsMenu() {
    PopupMenu popupMenu = new PopupMenu(requireContext(), binding.fieldsMenuButton);
    popupMenu.inflate(R.menu.menu_meal_plan_fields);
    popupMenu.setOnMenuItemClickListener(getFieldsMenuItemClickListener());
    popupMenu.show();
  }

  public PopupMenu.OnMenuItemClickListener getFieldsMenuItemClickListener() {
    return item -> {
      if (item.getItemId() == R.id.action_configure_sections) {
        viewModel.showMessageWithAction(
            R.string.msg_not_implemented_yet,
            R.string.action_open_server,
            () -> {
              Intent browserIntent = new Intent(
                  Intent.ACTION_VIEW,
                  Uri.parse(activity.getGrocyApi().getBaseUrl() + "/mealplansections")
              );
              startActivity(browserIntent);
            },
            getSharedPrefs().getInt(
                Constants.SETTINGS.BEHAVIOR.MESSAGE_DURATION,
                Constants.SETTINGS_DEFAULT.BEHAVIOR.MESSAGE_DURATION
            )
        );
        return true;
      } else if (item.getItemId() == R.id.action_fields_header) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), binding.fieldsMenuButton);
        viewModel.getFilterChipLiveDataHeaderFields().populateMenu(popupMenu.getMenu());
        popupMenu.show();
        return true;
      } else if (item.getItemId() == R.id.action_fields_entries) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), binding.fieldsMenuButton);
        viewModel.getFilterChipLiveDataEntriesFields().populateMenu(popupMenu.getMenu());
        popupMenu.show();
        return true;
      }
      return false;
    };
  }

  private void selectDate(WeekDay data) {
    if (data.getPosition() == WeekDayPosition.RangeDate) {
      LocalDate currentSelection = viewModel.getSelectedDate();
      if (currentSelection != data.getDate()) {
        viewModel.getSelectedDateLive().setValue(data.getDate());
        binding.calendarView.notifyWeekChanged(currentSelection);
        binding.calendarView.notifyDateChanged(data.getDate());
      }
    }
  }

  @Override
  public void updateConnectivity(boolean isOnline) {
    if (!isOnline == viewModel.isOffline()) {
      return;
    }
    viewModel.setOfflineLive(!isOnline);
    systemBarBehavior.refresh();
    if (isOnline) {
      viewModel.downloadData(false);
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }

  static class DayViewContainer extends ViewContainer {
    private final ViewCalendarDayLayoutBinding binding;

    public DayViewContainer(View view) {
      super(view);
      binding = ViewCalendarDayLayoutBinding.bind(view);
    }
  }
}