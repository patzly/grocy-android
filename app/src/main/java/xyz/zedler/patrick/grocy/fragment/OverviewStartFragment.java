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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentOverviewStartBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.ChangelogBottomSheet;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.FeedbackBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.OverviewStartViewModel;

public class OverviewStartFragment extends BaseFragment {

  private final static String TAG = OverviewStartFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentOverviewStartBinding binding;
  private OverviewStartViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;
  private ClickUtil clickUtil;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentOverviewStartBinding.inflate(
        inflater, container, false
    );
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
      binding = null;
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    viewModel = new ViewModelProvider(this).get(OverviewStartViewModel.class);
    viewModel.setOfflineLive(!activity.isOnline());
    binding.setViewModel(viewModel);
    binding.setFragment(this);
    binding.setActivity(activity);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    clickUtil = new ClickUtil(1000);

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(((SnackbarMessage) event).getSnackbar(
            activity,
            activity.binding.frameMainContainer
        ));
      }
    });

    infoFullscreenHelper = new InfoFullscreenHelper(binding.frameContainer);
    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    viewModel.getOfflineLive().observe(getViewLifecycleOwner(), this::appBarOfflineInfo);

    binding.toolbar.setOnMenuItemClickListener(item -> {
      int id = item.getItemId();
      if (id == R.id.action_settings) {
        navigateDeepLink(getString(R.string.deep_link_settingsFragment));
      } else if (id == R.id.action_about) {
        navigateDeepLink(getString(R.string.deep_link_aboutFragment));
      } else if (id == R.id.action_feedback) {
        activity.showBottomSheet(new FeedbackBottomSheet());
      } else if (id == R.id.action_changelog) {
        activity.showBottomSheet(new ChangelogBottomSheet());
      }
      return false;
    });

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    // UPDATE UI
    updateUI((getArguments() == null
        || getArguments().getBoolean(Constants.ARGUMENT.ANIMATED, true))
        && savedInstanceState == null);
  }

  private void updateUI(boolean animated) {
    activity.getScrollBehavior().setUpScroll(binding.scroll);
    activity.getScrollBehavior().setHideOnScroll(true);
    activity.updateBottomAppBar(
        Constants.FAB.POSITION.CENTER,
        R.menu.menu_empty,
        () -> {
        }
    );
    activity.updateFab(
        R.drawable.ic_round_barcode_scan,
        R.string.action_scan,
        Constants.FAB.TAG.SCAN,
        animated,
        () -> navigate(
            R.id.consumeFragment,
            new ConsumeFragmentArgs.Builder()
                .setStartWithScanner(true).build().toBundle()
        ), () -> navigate(
            R.id.purchaseFragment,
            new PurchaseFragmentArgs.Builder()
                .setStartWithScanner(true).build().toBundle()
        )
    );
  }

  public void navigateToSettingsCatBehavior() {
    Bundle bundle = new SettingsFragmentArgs.Builder()
        .setShowCategory(Constants.SETTINGS.BEHAVIOR.class.getSimpleName())
        .build().toBundle();
    navigateDeepLink(R.string.deep_link_settingsFragment, bundle);
  }

  public void navigateToSettingsCatServer() {
    if (viewModel.getBeginnerModeEnabled()) {
      Bundle bundle = new SettingsFragmentArgs.Builder()
          .setShowCategory(Constants.SETTINGS.SERVER.class.getSimpleName())
          .build().toBundle();
      navigateDeepLink(R.string.deep_link_settingsFragment, bundle);
    } else {
      navigateDeepLink(getString(R.string.deep_link_settingsCatServerFragment));
    }
  }

  public void navigateToStoredPurchases() {
    if (viewModel.getBeginnerModeEnabled()) {
      Bundle bundle = new SettingsFragmentArgs.Builder()
          .setShowCategory(Constants.SETTINGS.SERVER.class.getSimpleName())
          .build().toBundle();
      navigateDeepLink(R.string.deep_link_settingsFragment, bundle);
    } else {
      navigateDeepLink(getString(R.string.deep_link_settingsCatServerFragment));
    }
  }

  public void startLogoAnimation() {
    if (clickUtil.isDisabled()) {
      return;
    }
    ViewUtil.startIcon(binding.imageLogo);
  }

  @Override
  public void updateConnectivity(boolean online) {
    if (!online == viewModel.isOffline()) {
      return;
    }
    viewModel.setOfflineLive(!online);
    if (online) {
      viewModel.downloadData();
    }
  }

  private void appBarOfflineInfo(boolean visible) {
    boolean currentState = binding.linearOfflineError.getVisibility() == View.VISIBLE;
    if (visible == currentState) {
      return;
    }
    binding.linearOfflineError.setVisibility(visible ? View.VISIBLE : View.GONE);
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
