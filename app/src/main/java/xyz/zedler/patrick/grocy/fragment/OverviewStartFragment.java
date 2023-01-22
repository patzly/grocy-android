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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentOverviewStartBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.FeedbackBottomSheet;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.viewmodel.OverviewStartViewModel;

public class OverviewStartFragment extends BaseFragment {

  private final static String TAG = OverviewStartFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentOverviewStartBinding binding;
  private OverviewStartViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;
  private ClickUtil clickUtil;
  private SystemBarBehavior systemBarBehavior;

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

    systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipe);
    systemBarBehavior.setScroll(binding.scroll, binding.linearContainerScroll);
    systemBarBehavior.setUp();

    ViewUtil.setOnlyOverScrollStretchEnabled(binding.scrollHorizActions);
    binding.scrollHorizActions.post(
        () -> binding.scrollHorizActions.fullScroll(
            UiUtil.isLayoutRtl(activity)
                ? HorizontalScrollView.FOCUS_LEFT
                : HorizontalScrollView.FOCUS_RIGHT
        )
    );

    clickUtil = new ClickUtil(1000);

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(((SnackbarMessage) event).getSnackbar(
            activity,
            activity.binding.coordinatorMain
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
        activity.navigateDeepLink(getString(R.string.deep_link_settingsFragment));
      } else if (id == R.id.action_help) {
        activity.showHelpBottomSheet();
      } else if (id == R.id.action_about) {
        activity.navigateDeepLink(getString(R.string.deep_link_aboutFragment));
      } else if (id == R.id.action_feedback) {
        activity.showBottomSheet(new FeedbackBottomSheet());
      }
      return false;
    });

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    // UPDATE UI

    boolean animated = (getArguments() == null
        || getArguments().getBoolean(Constants.ARGUMENT.ANIMATED, true))
        && savedInstanceState == null;
    activity.getScrollBehavior().setUpScroll(binding.appBar, false, binding.scroll);
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(true, R.menu.menu_empty);
    activity.updateFab(
        R.drawable.ic_round_barcode_scan,
        R.string.action_scan,
        Constants.FAB.TAG.SCAN,
        animated,
        () -> activity.navigateFragment(
            R.id.consumeFragment,
            new ConsumeFragmentArgs.Builder()
                .setStartWithScanner(true).build().toBundle()
        ), () -> activity.navigateFragment(
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
    activity.navigateDeepLink(R.string.deep_link_settingsFragment, bundle);
  }

  public void navigateToSettingsCatServer() {
    if (viewModel.getBeginnerModeEnabled()) {
      Bundle bundle = new SettingsFragmentArgs.Builder()
          .setShowCategory(Constants.SETTINGS.SERVER.class.getSimpleName())
          .build().toBundle();
      activity.navigateDeepLink(R.string.deep_link_settingsFragment, bundle);
    } else {
      activity.navigateDeepLink(getString(R.string.deep_link_settingsCatServerFragment));
    }
  }

  public void navigateToStoredPurchases() {
    if (viewModel.getBeginnerModeEnabled()) {
      Bundle bundle = new SettingsFragmentArgs.Builder()
          .setShowCategory(Constants.SETTINGS.SERVER.class.getSimpleName())
          .build().toBundle();
      activity.navigateDeepLink(R.string.deep_link_settingsFragment, bundle);
    } else {
      activity.navigateDeepLink(getString(R.string.deep_link_settingsCatServerFragment));
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
    if (systemBarBehavior != null) {
      systemBarBehavior.refresh();
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
