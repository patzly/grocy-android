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
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import java.util.Locale;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentChoreEntryRescheduleBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Chore;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.InfoFullscreen;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.model.User;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.viewmodel.ChoreEntryRescheduleViewModel;

public class ChoreEntryRescheduleFragment extends BaseFragment {

  private final static String TAG = ChoreEntryRescheduleFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentChoreEntryRescheduleBinding binding;
  private ChoreEntryRescheduleViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;
  private SystemBarBehavior systemBarBehavior;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentChoreEntryRescheduleBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    Chore chore = ChoreEntryRescheduleFragmentArgs.fromBundle(requireArguments()).getChore();
    viewModel = new ViewModelProvider(this, new ChoreEntryRescheduleViewModel
        .ChoreEntryRescheduleViewModelFactory(
            activity.getApplication(), chore
    )).get(ChoreEntryRescheduleViewModel.class);
    binding.setViewModel(viewModel);
    binding.setActivity(activity);
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipe);
    systemBarBehavior.setScroll(binding.scroll, binding.linearContainerScroll);
    systemBarBehavior.setUp();

    binding.toolbar.setNavigationOnClickListener(v -> activity.navigateUp());

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(((SnackbarMessage) event).getSnackbar(
            activity,
            activity.binding.coordinatorMain
        ));
      } else if (event.getType() == Event.NAVIGATE_UP) {
        activity.navigateUp();
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      }
    });

    infoFullscreenHelper = new InfoFullscreenHelper(binding.container);
    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), isLoading -> {
      if (!isLoading) {
        viewModel.setCurrentQueueLoading(null);
      }
    });

    viewModel.getOfflineLive().observe(getViewLifecycleOwner(), offline -> {
      InfoFullscreen infoFullscreen = offline ? new InfoFullscreen(
          InfoFullscreen.ERROR_OFFLINE,
          () -> updateConnectivity(true)
      ) : null;
      viewModel.getInfoFullscreenLive().setValue(infoFullscreen);
    });

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    // UPDATE UI

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.scroll, true
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(true, R.menu.menu_empty, null);
    activity.updateFab(
        R.drawable.ic_round_backup,
        R.string.action_save,
        Constants.FAB.TAG.SAVE,
        ShoppingListEditFragmentArgs.fromBundle(requireArguments())
            .getAnimateStart() && savedInstanceState == null,
        () -> viewModel.rescheduleChore()
    );
  }

  public void showNextTrackingTimePicker() {
    String[] timeParts = viewModel.getNextTrackingTimeLive().getValue() != null
        ? viewModel.getNextTrackingTimeLive().getValue().split(":")
        : new String[]{};
    int hour = 12;
    int minute = 0;
    if (timeParts.length == 2) {
      if (NumUtil.isStringInt(timeParts[0])) {
        hour = Integer.parseInt(timeParts[0]);
      }
      if (NumUtil.isStringInt(timeParts[1])) {
        minute = Integer.parseInt(timeParts[1]);
      }
    }
    MaterialTimePicker picker = new MaterialTimePicker.Builder()
        .setTimeFormat(DateFormat.is24HourFormat(requireContext())
            ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
        .setHour(hour)
        .setMinute(minute)
        .setTitleText(R.string.property_next_estimated_tracking_time)
        .setNegativeButtonText(R.string.action_cancel)
        .setPositiveButtonText(R.string.action_save)
        .setTheme(R.style.ThemeOverlay_Grocy_TimePicker)
        .build();

    picker.addOnPositiveButtonClickListener(v -> viewModel.getNextTrackingTimeLive().setValue(
        String.format(Locale.getDefault(), "%02d:%02d:%02d",
            picker.getHour(), picker.getMinute(), 0)
    ));
    picker.show(getParentFragmentManager(), "time_picker_dialog");
  }

  public void showNextTrackingDatePicker() {

    MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
        .setTitleText(R.string.property_next_estimated_tracking)
        .setNegativeButtonText(R.string.action_cancel)
        .setPositiveButtonText(R.string.action_save)
        .setTheme(R.style.ThemeOverlay_Grocy_DatePicker)
        .build();

    picker.addOnPositiveButtonClickListener(v -> {
      String date = DateUtil.DATE_FORMAT.format(picker.getSelection());
      viewModel.getNextTrackingDateLive().setValue(date);
    });
    picker.show(getParentFragmentManager(), "date_picker_dialog");
  }

  @Override
  public void selectUser(User user) {
    viewModel.getUserLive().setValue(user);
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

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
