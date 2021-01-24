package xyz.zedler.patrick.grocy.fragment;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentSettingsCatAppearanceBinding;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;
import xyz.zedler.patrick.grocy.viewmodel.SettingsViewModel;

public class SettingsCatAppearanceFragment extends BaseFragment {

    private final static String TAG = SettingsCatAppearanceFragment.class.getSimpleName();

    private FragmentSettingsCatAppearanceBinding binding;
    private MainActivity activity;
    private SettingsViewModel viewModel;
    private MutableLiveData<Boolean> darkModeLive;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
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

        darkModeLive = new MutableLiveData<>(viewModel.getDarkMode());

        if(activity.binding.bottomAppBar.getVisibility() == View.VISIBLE) {
            activity.showHideDemoIndicator(this, true);
            activity.getScrollBehavior().setUpScroll(binding.scroll);
            activity.getScrollBehavior().setHideOnScroll(true);
            activity.updateBottomAppBar(
                    Constants.FAB.POSITION.GONE,
                    R.menu.menu_empty,
                    false,
                    () -> {}
            );
            activity.binding.fabMain.hide();
        }

        setForPreviousDestination(Constants.ARGUMENT.ANIMATED, false);
    }

    public MutableLiveData<Boolean> getDarkModeLive() {
        return darkModeLive;
    }

    public void setDarkMode(boolean dark) {
        IconUtil.start(binding.image);
        new Handler().postDelayed(() -> {
            darkModeLive.setValue(dark);
            updateTheme(dark);
        }, 300);
        viewModel.setDarkMode(dark);
    }

    private void updateTheme(boolean forceDarkMode) {
        AppCompatDelegate.setDefaultNightMode(forceDarkMode
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );
        activity.executeOnStart();
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        return setStatusBarColor(transit, enter, nextAnim, activity, R.color.primary);
    }
}
