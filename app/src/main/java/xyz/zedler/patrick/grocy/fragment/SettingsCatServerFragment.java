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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentSettingsCatServerBinding;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.RestartUtil;
import xyz.zedler.patrick.grocy.viewmodel.SettingsViewModel;

public class SettingsCatServerFragment extends BaseFragment {

  private static final String TAG = SettingsCatServerFragment.class.getSimpleName();

  private static final String DIALOG_RESTART_SHOWING = "dialog_restart_showing";
  private static final String DIALOG_LOGOUT_SHOWING = "dialog_logout_showing";

  private FragmentSettingsCatServerBinding binding;
  private MainActivity activity;
  private SettingsViewModel viewModel;
  private AlertDialog dialogRestart, dialogLogout;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentSettingsCatServerBinding.inflate(inflater, container, false);
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
    binding.setSharedPrefs(PreferenceManager.getDefaultSharedPreferences(activity));
    binding.setClickUtil(new ClickUtil());
    binding.setLifecycleOwner(getViewLifecycleOwner());

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipe);
    systemBarBehavior.setScroll(binding.scroll, binding.linearContainer);
    systemBarBehavior.setUp();

    binding.toolbar.setNavigationOnClickListener(v -> activity.navigateUp());

    binding.swipe.setEnabled(false);

    binding.textCompatible.setTextColor(
        viewModel.isVersionCompatible()
            ? ResUtil.getHarmonizedRoles(activity, R.color.green).getAccent()
            : ResUtil.getColorAttr(activity, R.attr.colorError)
    );

    binding.linearSettingReloadConfig.setOnClickListener(v -> {
      binding.linearSettingReloadConfig.setEnabled(false);
      binding.swipe.setRefreshing(true);
      viewModel.reloadConfiguration(
          () -> {
            binding.linearSettingReloadConfig.setEnabled(true);
            binding.swipe.setRefreshing(false);
            showRestartDialog();
          },
          () -> {
            binding.linearSettingReloadConfig.setEnabled(true);
            binding.swipe.setRefreshing(false);
          }
      );
    });

    viewModel.getEventHandler().observe(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(((SnackbarMessage) event).getSnackbar(
            activity.binding.coordinatorMain
        ));
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      }
    });

    if (activity.binding.bottomAppBar.getVisibility() == View.VISIBLE) { // not from login screen
      activity.getScrollBehavior().setUpScroll(
          binding.appBar, false, binding.scroll, false
      );
      activity.getScrollBehavior().setBottomBarVisibility(true);
      activity.updateBottomAppBar(false, R.menu.menu_empty);
      activity.binding.fabMain.hide();
    }

    setForPreviousDestination(Constants.ARGUMENT.ANIMATED, false);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(DIALOG_RESTART_SHOWING, dialogRestart != null && dialogRestart.isShowing());
    outState.putBoolean(DIALOG_LOGOUT_SHOWING, dialogLogout != null && dialogLogout.isShowing());
  }

  @Override
  public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);
    if (savedInstanceState != null) {
      if (savedInstanceState.getBoolean(DIALOG_RESTART_SHOWING)) {
        new Handler(Looper.getMainLooper()).postDelayed(this::showRestartDialog, 1);
      }
      if (savedInstanceState.getBoolean(DIALOG_LOGOUT_SHOWING)) {
        new Handler(Looper.getMainLooper()).postDelayed(
            () -> showLogoutDialog(viewModel.isDemo()), 1
        );
      }
    }
  }

  public void openServerWebsite() {
    String serverUrl = viewModel.getServerUrl();
    if (serverUrl == null) {
      return;
    }
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(serverUrl)));
  }

  public void showRestartDialog() {
    dialogRestart = new MaterialAlertDialogBuilder(activity)
        .setTitle(R.string.title_restart)
        .setMessage(R.string.msg_restart)
        .setPositiveButton(R.string.action_restart, (dialog, which) -> {
          performHapticHeavyClick();
          RestartUtil.restartApp(activity);
        }).setNegativeButton(R.string.action_cancel, (dialog, which) -> performHapticClick())
        .setOnCancelListener(dialog -> performHapticClick())
        .create();
    dialogRestart.show();
  }

  public void showLogoutDialog(boolean isDemoInstance) {
    dialogLogout = new MaterialAlertDialogBuilder(
        activity, R.style.ThemeOverlay_Grocy_AlertDialog_Caution
    ).setTitle(isDemoInstance ? R.string.title_logout_demo : R.string.title_logout)
        .setMessage(isDemoInstance ? R.string.msg_logout_demo : R.string.msg_logout)
        .setPositiveButton(R.string.action_logout, (dialog, which) -> {
          performHapticHeavyClick();
          activity.clearOfflineDataAndRestart();
        }).setNegativeButton(R.string.action_cancel, (dialog, which) -> performHapticClick())
        .setOnCancelListener(dialog -> performHapticClick())
        .create();
    dialogLogout.show();
  }
}
