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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.divider.MaterialDivider;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentSettingsCatAppearanceBinding;
import xyz.zedler.patrick.grocy.fragment.bottomSheetDialog.LanguagesBottomSheet;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.Constants.SETTINGS;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.Constants.THEME;
import xyz.zedler.patrick.grocy.util.LocaleUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;
import xyz.zedler.patrick.grocy.view.SelectionCardView;
import xyz.zedler.patrick.grocy.viewmodel.SettingsViewModel;

public class SettingsCatAppearanceFragment extends BaseFragment implements OnCheckedChangeListener {

  private final static String TAG = SettingsCatAppearanceFragment.class.getSimpleName();

  private FragmentSettingsCatAppearanceBinding binding;
  private MainActivity activity;
  private SettingsViewModel viewModel;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    binding = FragmentSettingsCatAppearanceBinding.inflate(inflater, container, false);
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
    systemBarBehavior.setScroll(binding.scroll, binding.linearContainer);
    systemBarBehavior.setUp();

    ViewUtil.centerToolbarTitleOnLargeScreens(binding.toolbar);
    binding.toolbar.setNavigationOnClickListener(v -> activity.navigateUp());

    setUpThemeSelection();

    int id;
    switch (getSharedPrefs().getInt(
        SETTINGS.APPEARANCE.DARK_MODE, SETTINGS_DEFAULT.APPEARANCE.DARK_MODE)
    ) {
      case AppCompatDelegate.MODE_NIGHT_NO:
        id = R.id.button_other_theme_light;
        break;
      case AppCompatDelegate.MODE_NIGHT_YES:
        id = R.id.button_other_theme_dark;
        break;
      default:
        id = R.id.button_other_theme_auto;
        break;
    }
    binding.toggleOtherTheme.check(id);
    binding.toggleOtherTheme.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
      if (!isChecked) {
        return;
      }
      int pref;
      if (checkedId == R.id.button_other_theme_light) {
        pref = AppCompatDelegate.MODE_NIGHT_NO;
      } else if (checkedId == R.id.button_other_theme_dark) {
        pref = AppCompatDelegate.MODE_NIGHT_YES;
      } else {
        pref = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
      }
      getSharedPrefs().edit().putInt(SETTINGS.APPEARANCE.DARK_MODE, pref).apply();
      performHapticClick();
      restartToApply(0, getInstanceState());
    });

    binding.partialOptionTransition.linearOtherTransition.setOnClickListener(
        v -> binding.partialOptionTransition.switchOtherTransition.setChecked(
            !binding.partialOptionTransition.switchOtherTransition.isChecked()
        )
    );
    binding.partialOptionTransition.switchOtherTransition.setChecked(
        getSharedPrefs().getBoolean(
            SETTINGS.APPEARANCE.USE_SLIDING, SETTINGS_DEFAULT.APPEARANCE.USE_SLIDING
        )
    );
    binding.partialOptionTransition.switchOtherTransition.setOnCheckedChangeListener(this);

    if (activity.binding.bottomAppBar.getVisibility() == View.VISIBLE) { // not from login screen
      activity.getScrollBehavior().setUpScroll(
          binding.appBar, true, binding.scroll, false
      );
      activity.getScrollBehavior().setBottomBarVisibility(true);
      activity.updateBottomAppBar(false, R.menu.menu_empty);
    }

    setForPreviousDestination(Constants.ARGUMENT.ANIMATED, false);
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    int id = buttonView.getId();
    performHapticClick();
    if (id == R.id.switch_other_transition) {
      ViewUtil.startIcon(binding.partialOptionTransition.imageOtherTransition);
      getSharedPrefs().edit().putBoolean(SETTINGS.APPEARANCE.USE_SLIDING, isChecked).apply();
    }
  }

  public String getLanguage() {
    return LocaleUtil.followsSystem()
        ? getString(R.string.setting_language_system)
        : LocaleUtil.getLocaleName();
  }

  public void showLanguageSelection() {
    ViewUtil.startIcon(binding.imageLanguage);
    activity.showBottomSheet(new LanguagesBottomSheet());
  }

  private void setUpThemeSelection() {
    boolean hasDynamic = DynamicColors.isDynamicColorAvailable();
    ViewGroup container = binding.linearOtherThemeContainer;
    int colorsCount = 7;
    for (int i = hasDynamic ? -1 : 0; i <= colorsCount; i++) {
      String name;
      int resId;
      switch (i) {
        case -1:
          name = THEME.DYNAMIC;
          resId = -1;
          break;
        case 0:
          name = THEME.RED;
          resId = R.style.Theme_Grocy_Red;
          break;
        case 1:
          name = THEME.YELLOW;
          resId = R.style.Theme_Grocy_Yellow;
          break;
        case 2:
          name = THEME.LIME;
          resId = R.style.Theme_Grocy_Lime;
          break;
        /*case 3:
          name = THEME.GREEN;
          resId = R.style.Theme_Grocy_Green;
          break;*/
        case 4:
          name = THEME.TURQUOISE;
          resId = R.style.Theme_Grocy_Turquoise;
          break;
        case 5:
          name = THEME.TEAL;
          resId = R.style.Theme_Grocy_Teal;
          break;
        case 6:
          name = THEME.BLUE;
          resId = R.style.Theme_Grocy_Blue;
          break;
        case 7:
          name = THEME.PURPLE;
          resId = R.style.Theme_Grocy_Purple;
          break;
        default:
          name = THEME.GREEN;
          resId = R.style.Theme_Grocy_Green;
          break;
      }

      SelectionCardView card = new SelectionCardView(activity);
      int color;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && i == -1) {
        color = ContextCompat.getColor(
            activity,
            UiUtil.isDarkModeActive(activity)
                ? android.R.color.system_accent1_700
                : android.R.color.system_accent1_100
        );
      } else {
        color = ResUtil.getColorAttr(
            new ContextThemeWrapper(activity, resId), R.attr.colorPrimaryContainer
        );
      }
      card.setEnsureContrast(false);
      card.setCardBackgroundColor(color);
      card.setOnClickListener(v -> {
        if (!card.isChecked()) {
          card.startCheckedIcon();
          ViewUtil.startIcon(binding.imageOtherTheme);
          performHapticClick();
          ViewUtil.uncheckAllChildren(container);
          card.setChecked(true);
          getSharedPrefs().edit().putString(SETTINGS.APPEARANCE.THEME, name).apply();
          restartToApply(100, getInstanceState());
        }
      });

      String selected = getSharedPrefs().getString(
          SETTINGS.APPEARANCE.THEME, SETTINGS_DEFAULT.APPEARANCE.THEME
      );
      boolean isSelected;
      if (selected.isEmpty()) {
        isSelected = hasDynamic ? name.equals(THEME.DYNAMIC) : name.equals(THEME.YELLOW);
      } else {
        isSelected = selected.equals(name);
      }
      card.setChecked(isSelected);
      container.addView(card);

      if (hasDynamic && i == -1) {
        MaterialDivider divider = new MaterialDivider(activity);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            UiUtil.dpToPx(activity, 1), UiUtil.dpToPx(activity, 40)
        );
        int marginLeft, marginRight;
        if (UiUtil.isLayoutRtl(activity)) {
          marginLeft = UiUtil.dpToPx(activity, 8);
          marginRight = UiUtil.dpToPx(activity, 4);
        } else {
          marginLeft = UiUtil.dpToPx(activity, 4);
          marginRight = UiUtil.dpToPx(activity, 8);
        }
        layoutParams.setMargins(marginLeft, 0, marginRight, 0);
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        divider.setLayoutParams(layoutParams);
        container.addView(divider);
      }
    }

    Bundle bundleInstanceState = activity.getIntent().getBundleExtra(ARGUMENT.INSTANCE_STATE);
    if (bundleInstanceState != null) {
      binding.scrollOtherTheme.scrollTo(
          bundleInstanceState.getInt(ARGUMENT.SCROLL_POSITION + 1, 0), 0
      );
    }
  }

  private Bundle getInstanceState() {
    Bundle bundle = new Bundle();
    if (binding != null) {
      bundle.putInt(ARGUMENT.SCROLL_POSITION + 1, binding.scrollOtherTheme.getScrollX());
    }
    return bundle;
  }

  public void restartToApply(long delay, @NonNull Bundle bundle) {
    new Handler(Looper.getMainLooper()).postDelayed(() -> {
      onSaveInstanceState(bundle);
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        activity.finish();
      }
      Intent intent = new Intent(activity, MainActivity.class);
      intent.putExtra(ARGUMENT.INSTANCE_STATE, bundle);
      startActivity(intent);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        activity.finish();
      }
      activity.overridePendingTransition(R.anim.fade_in_restart, R.anim.fade_out_restart);
    }, delay);
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    return setStatusBarColor(transit, enter, nextAnim, activity, R.color.primary);
  }
}
