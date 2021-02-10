package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

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

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Locale;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.LanguageAdapter;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetListSelectionBinding;
import xyz.zedler.patrick.grocy.fragment.BaseFragment;
import xyz.zedler.patrick.grocy.fragment.SettingsCatAppearanceFragment;
import xyz.zedler.patrick.grocy.model.Language;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.LocaleUtil;
import xyz.zedler.patrick.grocy.util.RestartUtil;

public class LanguagesBottomSheet extends BaseBottomSheet
        implements LanguageAdapter.LanguageAdapterListener {

    private final static String TAG = LanguagesBottomSheet.class.getSimpleName();

    private FragmentBottomsheetListSelectionBinding binding;
    private MainActivity activity;
    private SharedPreferences sharedPrefs;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext(), R.style.Theme_Grocy_BottomSheetDialog);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentBottomsheetListSelectionBinding.inflate(
                inflater, container, false
        );

        activity = (MainActivity) requireActivity();

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        String selectedCode = sharedPrefs.getString(
                Constants.SETTINGS.APPEARANCE.LANGUAGE,
                Constants.SETTINGS_DEFAULT.APPEARANCE.LANGUAGE
        );

        binding.textListSelectionTitle.setText(getString(R.string.setting_language_description));
        binding.textListSelectionDescription.setText(getString(R.string.setting_language_info));
        binding.textListSelectionDescription.setVisibility(View.VISIBLE);

        binding.recyclerListSelection.setLayoutManager(
                new LinearLayoutManager(
                        activity,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );
        binding.recyclerListSelection.setAdapter(
                new LanguageAdapter(LocaleUtil.getLanguages(activity), selectedCode, this)
        );

        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    public void onItemRowClicked(Language language) {
        sharedPrefs.edit()
                .putString(
                        Constants.SETTINGS.APPEARANCE.LANGUAGE,
                        language != null ? language.getCode() : null)
                .apply();

        Locale config;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            config = getResources().getConfiguration().getLocales().get(0);
        } else {
            config = getResources().getConfiguration().locale;
        }
        Locale configCompare = new Locale(config.getLanguage(), "", config.getVariant());

        Locale device = LocaleUtil.getDeviceLocale();
        Locale deviceCompare = new Locale(device.getLanguage(), "", device.getVariant());

        Locale compare = language != null
                ? LocaleUtil.getLocaleFromCode(language.getCode())
                : deviceCompare;

        if (compare.equals(configCompare)) {
            BaseFragment current = activity.getCurrentFragment();
            if (current instanceof SettingsCatAppearanceFragment) {
                ((SettingsCatAppearanceFragment) current).setLanguage(
                        language != null ? language.getCode() : null
                );
            }
            dismiss();
        } else {
            new Handler().postDelayed(() -> RestartUtil.restartApp(activity), 100);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
