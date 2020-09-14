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

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentSettingsBinding;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.view.SettingCategory;

public class SettingsOverviewFragment extends BaseFragment {

    private final static String TAG = SettingsOverviewFragment.class.getSimpleName();

    private FragmentSettingsBinding binding;
    private MainActivity activity;
    private SharedPreferences sharedPrefs;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        activity = (MainActivity) requireActivity();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        addCategories();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void addCategories() {
        binding.linearBody.addView(new SettingCategory(
                requireContext(),
                R.string.category_server,
                sharedPrefs.getString(Constants.PREF.SERVER_URL, getString(R.string.error_unknown)),
                R.drawable.ic_round_settings_system,
                () -> goTo(Constants.SETTING.SERVER.class.getSimpleName())
        ));
        binding.linearBody.addView(new SettingCategory(
                requireContext(),
                R.string.category_appearance,
                R.drawable.ic_round_dark_mode_on_anim,
                () -> goTo(Constants.SETTING.APPEARANCE.class.getSimpleName())
        ));
        binding.linearBody.addView(new SettingCategory(
                requireContext(),
                R.string.category_barcode_scanner,
                R.drawable.ic_round_barcode_scan,
                () -> goTo(Constants.SETTING.SCANNER.class.getSimpleName())
        ));
        binding.linearBody.addView(new SettingCategory(
                requireContext(),
                R.string.title_stock_overview,
                R.drawable.ic_round_view_list, // TODO: Shelf icon would be good
                () -> goTo(Constants.SETTING.STOCK.class.getSimpleName())
        ));
        binding.linearBody.addView(new SettingCategory(
                requireContext(),
                R.string.title_shopping_mode,
                R.drawable.ic_round_storefront,
                () -> goTo(Constants.SETTING.SHOPPING_MODE.class.getSimpleName())
        ));
        binding.linearBody.addView(new SettingCategory(
                requireContext(),
                R.string.category_purchase_consume,
                R.drawable.ic_round_pasta,
                () -> goTo(Constants.SETTING.PURCHASE_CONSUME.class.getSimpleName())
        ));
        binding.linearBody.addView(new SettingCategory(
                requireContext(),
                R.string.category_presets,
                R.drawable.ic_round_widgets,
                () -> goTo(Constants.SETTING.PRESETS.class.getSimpleName())
        ));
        binding.linearBody.addView(new SettingCategory(
                requireContext(),
                R.string.category_debugging,
                R.drawable.ic_round_bug_report_anim,
                () -> goTo(Constants.SETTING.DEBUGGING.class.getSimpleName())
        ));
        binding.linearBody.addView(new SettingCategory(
                requireContext(),
                R.string.title_about_this_app,
                R.drawable.ic_round_info_outline_anim_menu,
                () -> navigate(SettingsOverviewFragmentDirections
                        .actionSettingsOverviewFragmentToAboutFragment())
        ));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.frameBack.setOnClickListener(v -> activity.navigateUp());

        boolean animated = true;
        if(getFromLastFragmentNow(Constants.ARGUMENT.ANIMATED) != null) {
            animated = false;
        }

        activity.showHideDemoIndicator(this, animated);
        activity.updateBottomAppBar(
                Constants.FAB.POSITION.GONE,
                R.menu.menu_empty,
                animated,
                () -> {}
        );
        activity.getScrollBehavior().setUpScroll(binding.scroll);
        activity.getScrollBehavior().setHideOnScroll(true);
        activity.binding.fabMain.hide();
    }

    private void goTo(String category) {
        navigate(SettingsOverviewFragmentDirections
                .actionSettingsOverviewFragmentToSettingsFragment(category));
    }
}
