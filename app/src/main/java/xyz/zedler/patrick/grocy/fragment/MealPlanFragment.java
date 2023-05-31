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

import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;
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
import java.util.Locale;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.MealPlanPagerAdapter;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMealPlanBinding;
import xyz.zedler.patrick.grocy.databinding.ViewCalendarDayLayoutBinding;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.util.ClickUtil;
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
    StockOverviewFragmentArgs args = StockOverviewFragmentArgs.fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this, new MealPlanViewModel
        .MealPlanViewModelFactory(activity.getApplication(), args)
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

        if (viewModel.getSelectedDate().isEqual(data.getDate())) {
          container.binding.card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.material_dynamic_secondary50));
          container.binding.card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.material_dynamic_secondary50));
          container.binding.weekday.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
          container.binding.day.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        } else {
          TypedValue typedValue = new TypedValue();
          Theme theme = requireContext().getTheme();
          theme.resolveAttribute(R.attr.colorOutline, typedValue, true);
          @ColorInt int colorOutline = typedValue.data;

          container.binding.card.setStrokeColor(colorOutline);
          container.binding.card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.transparent));
          container.binding.weekday.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
          container.binding.day.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
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
    binding.today.setOnClickListener(v -> binding.calendarView.smoothScrollToWeek(LocalDate.now()));

    MealPlanPagerAdapter adapter = new MealPlanPagerAdapter(activity, viewModel.getSelectedDate());
    binding.viewPager.setAdapter(adapter);
    binding.viewPager.setCurrentItem(Integer.MAX_VALUE / 2);

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
      binding.viewPager.setCurrentItem(position, true);
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
    activity.updateBottomAppBar(false, R.menu.menu_empty, this::onMenuItemClick);
  }

  @Override
  public void performAction(String action, StockItem stockItem) {
    viewModel.performAction(action, stockItem);
  }

  private boolean onMenuItemClick(MenuItem item) {
    return false;
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
      viewModel.downloadData();
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