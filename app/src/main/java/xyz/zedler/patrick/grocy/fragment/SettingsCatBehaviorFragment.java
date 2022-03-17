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

import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentSettingsCatBehaviorBinding;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.BEHAVIOR;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.viewmodel.SettingsViewModel;

public class SettingsCatBehaviorFragment extends BaseFragment {

  private final static String TAG = SettingsCatBehaviorFragment.class.getSimpleName();

  private FragmentSettingsCatBehaviorBinding binding;
  private MainActivity activity;
  private SettingsViewModel viewModel;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentSettingsCatBehaviorBinding.inflate(inflater, container, false);
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

    if (activity.binding.bottomAppBar.getVisibility() == View.VISIBLE) {
      activity.getScrollBehavior().setUpScroll(binding.scroll);
      activity.getScrollBehavior().setHideOnScroll(true);
      activity.updateBottomAppBar(
          Constants.FAB.POSITION.GONE,
          R.menu.menu_empty,
          () -> {
          }
      );
      activity.binding.fabMain.hide();
    }

    updateShortcuts();

    setForPreviousDestination(Constants.ARGUMENT.ANIMATED, false);
  }

  public String getMessageDuration() {
    return getResources().getQuantityString(
        R.plurals.property_seconds_num,
        viewModel.getMessageDuration(),
        viewModel.getMessageDuration()
    );
  }

  @Override
  public void saveInput(String text, Bundle argsBundle) {
    String type = argsBundle.getString(ARGUMENT.TYPE);
    if (type == null) return;
    if (type.equals(BEHAVIOR.MESSAGE_DURATION) && NumUtil.isStringInt(text)) {
      viewModel.setMessageDuration(Integer.parseInt(text));
      binding.textMessageDuration.setText(getMessageDuration());
    }
  }

  @Override
  public void updateShortcuts() {
    String subtitleShortcuts;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
      ShortcutManager shortcutManager = activity.getSystemService(ShortcutManager.class);
      List<ShortcutInfo> shortcutInfos = shortcutManager.getDynamicShortcuts();
      StringBuilder shortcutLabels = new StringBuilder();
      for (ShortcutInfo shortcutInfo : shortcutInfos) {
        shortcutLabels.append(shortcutInfo.getShortLabel());
        if (shortcutInfo != shortcutInfos.get(shortcutInfos.size() - 1)) {
          shortcutLabels.append(", ");
        }
      }
      if (shortcutLabels.length() != 0) {
        subtitleShortcuts = shortcutLabels.toString();
      } else {
        subtitleShortcuts = getString(R.string.subtitle_none_selected);
      }
    } else {
      subtitleShortcuts = getString(R.string.subtitle_not_supported);
    }
    binding.subtitleShortcuts.setText(subtitleShortcuts);
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    return setStatusBarColor(transit, enter, nextAnim, activity, R.color.primary);
  }
}
