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

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentSettingsBinding;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.PrefsUtil;

public class SettingsFragment extends BaseFragment {

  private final static String TAG = SettingsFragment.class.getSimpleName();

  private FragmentSettingsBinding binding;
  private MainActivity activity;
  private SettingsFragmentArgs args;
  private PrefsUtil prefsUtil;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentSettingsBinding.inflate(inflater, container, false);
    activity = (MainActivity) requireActivity();
    args = SettingsFragmentArgs.fromBundle(requireArguments());
    binding.setFragment(this);
    binding.setActivity(activity);
    binding.setClickUtil(new ClickUtil());
    binding.setLifecycleOwner(getViewLifecycleOwner());
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setScroll(binding.scroll, binding.constraint);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(false);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.scroll, false
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    activity.updateBottomAppBar(false, R.menu.menu_empty);

    prefsUtil = new PrefsUtil(activity, this);

    setForPreviousDestination(Constants.ARGUMENT.ANIMATED, false);
  }

  public void openBackupDialog() {
    new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Grocy_AlertDialog)
        .setTitle(R.string.action_settings_backup_restore)
        .setMessage(R.string.msg_settings_backup_restore)
        .setPositiveButton(R.string.action_backup, (dialog, which) -> prefsUtil.exportPrefs())
        .setNegativeButton(R.string.action_restore, (dialog, which) -> prefsUtil.importPrefs())
        .show();
  }

  public boolean shouldNavigateToBehavior() {
    return args.getShowCategory() != null
        && args.getShowCategory().equals(Constants.SETTINGS.BEHAVIOR.class.getSimpleName());
  }

  public boolean shouldNavigateToServer() {
    return args.getShowCategory() != null
        && args.getShowCategory().equals(Constants.SETTINGS.SERVER.class.getSimpleName());
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    if (nextAnim == 0) {
      return null;
    }
    try {
      Animation animation = AnimationUtils.loadAnimation(getActivity(), nextAnim);
      animation.setAnimationListener(new Animation.AnimationListener() {

        @Override
        public void onAnimationStart(Animation animation) {}

        @Override
        public void onAnimationRepeat(Animation animation) {}

        @Override
        public void onAnimationEnd(Animation animation) {
          if (enter) {
            navigateToSubpage();
          }
        }
      });
      return animation;
    } catch (InflateException e) {
      return null;
    }
  }

  @Nullable
  @Override
  public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
    if (nextAnim == 0) {
      return null;
    }
    Animator animator = AnimatorInflater.loadAnimator(getActivity(), nextAnim);
    animator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        if (enter) {
          navigateToSubpage();
        }
      }
    });
    return animator;
  }

  private void navigateToSubpage() {
    if (shouldNavigateToBehavior()) {
      setArguments(
          new SettingsFragmentArgs.Builder(args).setShowCategory(null).build().toBundle()
      );
      new Handler().postDelayed(() -> activity.navUtil.navigateFragment(
              SettingsFragmentDirections.actionSettingsFragmentToSettingsCatBehaviorFragment()),
          200
      );
    } else if (shouldNavigateToServer()) {
      setArguments(
          new SettingsFragmentArgs.Builder(args).setShowCategory(null).build().toBundle()
      );
      new Handler().postDelayed(() -> activity.navUtil.navigateFragment(
              SettingsFragmentDirections.actionSettingsFragmentToSettingsCatServerFragment()),
          200
      );
    }
  }
}
