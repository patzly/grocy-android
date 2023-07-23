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

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import java.util.Locale;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.NOTIFICATIONS;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentSettingsCatNotificationsBinding;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.SettingsViewModel;

public class SettingsCatNotificationsFragment extends BaseFragment {

  private final static String TAG = SettingsCatNotificationsFragment.class.getSimpleName();

  private FragmentSettingsCatNotificationsBinding binding;
  private MainActivity activity;
  private SettingsViewModel viewModel;
  private ActivityResultLauncher<String> permStockLauncher, permChoresLauncher;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentSettingsCatNotificationsBinding.inflate(inflater, container, false);
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
    viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
    binding.setActivity(activity);
    binding.setFragment(this);
    binding.setViewModel(viewModel);
    binding.setClickUtil(new ClickUtil());
    binding.setLifecycleOwner(getViewLifecycleOwner());

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setScroll(binding.scroll, binding.constraint);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());

    viewModel.getEventHandler().observe(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(
            ((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain)
        );
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      }
    });

    binding.switchStockEnableNotifications.post(() -> {
      binding.switchStockEnableNotifications.jumpDrawablesToCurrentState();
      binding.switchChoresEnableNotifications.jumpDrawablesToCurrentState();
    });
    permStockLauncher = registerForActivityResult(new RequestPermission(), isGranted -> {
      if (isGranted) {
        if (binding != null) {
          binding.switchStockEnableNotifications.setChecked(true);
        }
      } else if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
        Snackbar snackbar = activity.getSnackbar(R.string.error_permission_notification, true);
        snackbar.setAction(
            R.string.action_retry,
            v -> permStockLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        );
        activity.showSnackbar(snackbar);
      }
    });
    binding.switchStockEnableNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
      ViewUtil.startIcon(binding.imageStockEnableNotifications);
      if (isChecked) {
        if (viewModel.getReminderUtil().hasPermission()) {
          viewModel.setStockNotificationsEnabled(true);
        } else if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
          binding.switchStockEnableNotifications.setChecked(false);
          permStockLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
          binding.switchStockEnableNotifications.setChecked(false);
        }
      } else {
        viewModel.setStockNotificationsEnabled(false);
      }
    });

    permChoresLauncher = registerForActivityResult(new RequestPermission(), isGranted -> {
      if (isGranted) {
        if (binding != null) {
          binding.switchChoresEnableNotifications.setChecked(true);
        }
      } else if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
        Snackbar snackbar = activity.getSnackbar(R.string.error_permission_notification, true);
        snackbar.setAction(
            R.string.action_retry,
            v -> permChoresLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        );
        activity.showSnackbar(snackbar);
      }
    });
    binding.switchChoresEnableNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
      ViewUtil.startIcon(binding.imageChoresEnableNotifications);
      if (isChecked) {
        if (viewModel.getReminderUtil().hasPermission()) {
          viewModel.setChoresNotificationsEnabled(true);
        } else if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
          binding.switchChoresEnableNotifications.setChecked(false);
          permChoresLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
          binding.switchChoresEnableNotifications.setChecked(false);
        }
      } else {
        viewModel.setChoresNotificationsEnabled(false);
      }
    });

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(false);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.scroll, false
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(false, R.menu.menu_empty);

    setForPreviousDestination(Constants.ARGUMENT.ANIMATED, false);
  }

  @Override
  public void onResume() {
    super.onResume();

    binding.switchStockEnableNotifications.setChecked(viewModel.getStockNotificationsEnabled());
    binding.switchChoresEnableNotifications.setChecked(viewModel.getChoresNotificationsEnabled());
  }

  public void showDueSoonTimePickerDialog() {
    showTimePickerDialog(
        () -> viewModel.getStockNotificationsTime(),
        time -> {
          viewModel.setStockNotificationsTime(time);
          if (!viewModel.getReminderUtil().hasPermission()) {
            binding.switchStockEnableNotifications.setChecked(false);
          }
        }
    );
  }

  public void showChoresTimePickerDialog() {
    showTimePickerDialog(
        () -> viewModel.getChoresNotificationsTime(),
        time -> {
          viewModel.setChoresNotificationsTime(time);
          if (!viewModel.getReminderUtil().hasPermission()) {
            binding.switchChoresEnableNotifications.setChecked(false);
          }
        }
    );
  }

  private void showTimePickerDialog(
      TimePickerTimeListener timeListener,
      TimePickerFinishedListener finishedListener
  ) {
    String[] timeParts = timeListener.getTime().split(":");
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
        .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
        .setHour(hour)
        .setMinute(minute)
        .setTitleText(R.string.setting_notification_time)
        .setNegativeButtonText(R.string.action_cancel)
        .setPositiveButtonText(R.string.action_save)
        .setTheme(R.style.ThemeOverlay_Grocy_TimePicker)
        .build();
    picker.addOnPositiveButtonClickListener(v -> finishedListener.onFinished(
        String.format(Locale.getDefault(), "%02d:%02d", picker.getHour(), picker.getMinute())
    ));
    picker.show(activity.getSupportFragmentManager(), "time");
  }

  public interface TimePickerTimeListener {
    String getTime();
  }

  public interface TimePickerFinishedListener {
    void onFinished(String time);
  }

  public void openAppInfoPage() {
    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
    intent.setData(uri);
    requireContext().startActivity(intent);
  }
}
