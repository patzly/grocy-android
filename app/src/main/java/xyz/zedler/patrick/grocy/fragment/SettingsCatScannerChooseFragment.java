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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentSettingsCatScannerChooseBinding;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.UnlockUtil;
import xyz.zedler.patrick.grocy.viewmodel.SettingsViewModel;

public class SettingsCatScannerChooseFragment extends BaseFragment {

  private final static String TAG = SettingsCatScannerChooseFragment.class.getSimpleName();

  private FragmentSettingsCatScannerChooseBinding binding;
  private MainActivity activity;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentSettingsCatScannerChooseBinding.inflate(inflater, container, false);
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
    SettingsViewModel viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
    binding.setActivity(activity);
    binding.setFragment(this);
    binding.setViewModel(viewModel);
    binding.setSharedPrefs(PreferenceManager.getDefaultSharedPreferences(activity));
    binding.setClickUtil(new ClickUtil());
    binding.setLifecycleOwner(getViewLifecycleOwner());

    boolean isUnlocked = UnlockUtil.isKeyInstalled(activity);
    boolean isPlayStoreInstalled = UnlockUtil.isPlayStoreInstalled(activity);

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    if (isUnlocked) {
      systemBarBehavior.setScroll(binding.scroll, binding.linearBody);
    } else {
      systemBarBehavior.setContainer(binding.linearContainerUnlock);
    }
    systemBarBehavior.setUp();

    binding.toolbar.setNavigationOnClickListener(v -> activity.navigateUp());

    binding.formattedMlKitIntro.setText(
        ResUtil.getRawText(
            activity, isPlayStoreInstalled ? R.raw.ml_kit_intro : R.raw.ml_kit_intro_no_vending
        )
    );

    if (viewModel.getUseMlKitScanner()) {
      binding.toggleSelectScanner.check(R.id.button_mlkit);
    } else {
      binding.toggleSelectScanner.check(R.id.button_xzing);
    }
    binding.toggleSelectScanner.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
      if (!isChecked) {
        return;
      }
      viewModel.setUseMlKitScanner(checkedId == R.id.button_mlkit);
      performHapticClick();
    });

    viewModel.getShowBarcodeScannerZXingInfo().observe(
        getViewLifecycleOwner(), value -> binding.formattedScannerInfo.setText(
            getString(
                value
                    ? R.string.msg_help_barcode_scanner_info_zxing
                    : R.string.msg_help_barcode_scanner_info_mlkit
            )
        )
    );

    binding.buttonVending.setVisibility(isPlayStoreInstalled ? View.VISIBLE : View.GONE);
    binding.buttonGithub.setVisibility(isPlayStoreInstalled ? View.VISIBLE : View.GONE);
    binding.buttonGithubNoVending.setVisibility(isPlayStoreInstalled ? View.GONE : View.VISIBLE);

    viewModel.getEventHandler().observe(getViewLifecycleOwner(), event -> {
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

    if (activity.binding.bottomAppBar.getVisibility() == View.VISIBLE) {
      activity.getScrollBehavior().setUpScroll(
          binding.appBar, false, isUnlocked ? binding.scroll : null, true
      );
      activity.getScrollBehavior().setBottomBarVisibility(true, true);
      activity.updateBottomAppBar(false, R.menu.menu_empty);
      activity.binding.fabMain.hide();
    }

    setForPreviousDestination(Constants.ARGUMENT.ANIMATED, false);
  }

  public void openPlayStore() {
    try {
      startActivity(new Intent(
          Intent.ACTION_VIEW,
          Uri.parse("market://details?id=" + UnlockUtil.PACKAGE)
      ));
    } catch (android.content.ActivityNotFoundException e) {
      startActivity(new Intent(
          Intent.ACTION_VIEW,
          Uri.parse("https://play.google.com/store/apps/details?id=" + UnlockUtil.PACKAGE)
      ));
    }
    activity.navigateUp();
  }

  public void openGitHub() {
    startActivity(new Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://github.com/patzly/grocy-android-unlock/releases/")
    ));
    activity.navigateUp();
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    return setStatusBarColor(transit, enter, nextAnim, activity, R.color.primary);
  }
}
