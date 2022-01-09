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
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentSettingsCatNetworkBinding;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.NETWORK;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.RestartUtil;
import xyz.zedler.patrick.grocy.viewmodel.SettingsViewModel;
import xyz.zedler.patrick.grocy.web.RequestQueueSingleton;

public class SettingsCatNetworkFragment extends BaseFragment {

  private final static String TAG = SettingsCatNetworkFragment.class.getSimpleName();

  private FragmentSettingsCatNetworkBinding binding;
  private MainActivity activity;
  private SettingsViewModel viewModel;
  private MutableLiveData<String> proxyHostLive;
  private MutableLiveData<String> proxyPortLive;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentSettingsCatNetworkBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    RequestQueueSingleton.getInstance(activity.getApplication()).newRequestQueue();
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

    viewModel.getEventHandler().observe(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(((SnackbarMessage) event).getSnackbar(
            activity,
            activity.binding.frameMainContainer
        ));
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      }
    });

    viewModel.getTorEnabledLive().observe(getViewLifecycleOwner(), enabled -> {
      viewModel.setTorEnabled(enabled);
      assert viewModel.getProxyEnabledLive().getValue() != null;
      if (enabled && viewModel.getProxyEnabledLive().getValue()){
        viewModel.getProxyEnabledLive().setValue(false);
      }
    });
    viewModel.getProxyEnabledLive().observe(getViewLifecycleOwner(), enabled -> {
      viewModel.setProxyEnabled(enabled);
      assert viewModel.getTorEnabledLive().getValue() != null;
      if (enabled && viewModel.getTorEnabledLive().getValue()) {
        viewModel.getTorEnabledLive().setValue(false);
      }
    });

    proxyHostLive = new MutableLiveData<>(viewModel.getProxyHost());
    proxyPortLive = new MutableLiveData<>(String.valueOf(viewModel.getProxyPort()));

    if (activity.binding.bottomAppBar.getVisibility() == View.VISIBLE) {
      activity.getScrollBehavior().setUpScroll(binding.scroll);
      activity.getScrollBehavior().setHideOnScroll(true);
      activity.updateBottomAppBar(
          Constants.FAB.POSITION.GONE,
          R.menu.menu_empty,
          false,
          () -> {
          }
      );
      activity.binding.fabMain.hide();
    }

    setForPreviousDestination(Constants.ARGUMENT.ANIMATED, false);

    updateTimeoutValue();
  }

  public MutableLiveData<String> getProxyHostLive() {
    return proxyHostLive;
  }

  public MutableLiveData<String> getProxyPortLive() {
    return proxyPortLive;
  }

  private void updateTimeoutValue() {
    binding.timeoutSeconds.setText(getResources().getQuantityString(
        R.plurals.property_seconds_num,
        viewModel.getLoadingTimeout(),
        viewModel.getLoadingTimeout()
    ));
  }

  @Override
  public void saveInput(String text, Bundle argsBundle) {
    String type = argsBundle.getString(ARGUMENT.TYPE);
    if (type == null) return;
    switch (type) {
      case NETWORK.LOADING_TIMEOUT:
        int timeout = NumUtil.isStringInt(text) && Integer.parseInt(text) > 0
            ? Integer.parseInt(text) : SETTINGS_DEFAULT.NETWORK.LOADING_TIMEOUT;
        viewModel.setLoadingTimeout(timeout);
        updateTimeoutValue();
        break;
      case NETWORK.PROXY_HOST:
        viewModel.setProxyHost(text.isEmpty() ? SETTINGS_DEFAULT.NETWORK.PROXY_HOST : text);
        proxyHostLive.setValue(text.isEmpty() ? SETTINGS_DEFAULT.NETWORK.PROXY_HOST : text);
        break;
      case NETWORK.PROXY_PORT:
        viewModel.setProxyPort(
            NumUtil.isStringInt(text)
                ? Integer.parseInt(text)
                : SETTINGS_DEFAULT.NETWORK.PROXY_PORT
        );
        proxyPortLive.setValue(
            NumUtil.isStringInt(text)
                ? text
                : String.valueOf(SETTINGS_DEFAULT.NETWORK.PROXY_PORT)
        );
        break;
    }
  }

  public void restartApp() {
    RestartUtil.restartApp(requireContext());
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    return setStatusBarColor(transit, enter, nextAnim, activity, R.color.primary);
  }
}
