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

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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
import java.util.Objects;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.LanguageAdapter;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetListSelectionBinding;
import xyz.zedler.patrick.grocy.fragment.BaseFragment;
import xyz.zedler.patrick.grocy.model.Language;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.LocaleUtil;
import xyz.zedler.patrick.grocy.util.RestartUtil;
import xyz.zedler.patrick.grocy.util.ShortcutUtil;

public class LanguagesBottomSheet extends BaseBottomSheetDialogFragment
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
    String previousCode = sharedPrefs.getString(
        Constants.SETTINGS.APPEARANCE.LANGUAGE, null
    );
    String selectedCode = language != null ? language.getCode() : null;

    if (Objects.equals(previousCode, selectedCode)) {
      return;
    } else if (previousCode == null || selectedCode == null) {
      Locale localeDevice = LocaleUtil.getNearestSupportedLocale(
          activity, LocaleUtil.getDeviceLocale()
      );
      String codeToCompare = previousCode == null ? selectedCode : previousCode;
      if (Objects.equals(localeDevice.toString(), codeToCompare)) {
        BaseFragment current = activity.getCurrentFragment();
        current.setLanguage(language);
        dismiss();
      } else {
        new Handler().postDelayed(() -> {
          updateShortcuts(
              selectedCode != null
                  ? LocaleUtil.getLocaleFromCode(selectedCode)
                  : localeDevice
          );
          RestartUtil.restartApp(activity);
        }, 100);
      }
    } else {
      new Handler().postDelayed(() -> {
        updateShortcuts(LocaleUtil.getLocaleFromCode(selectedCode));
        RestartUtil.restartApp(activity);
      }, 100);
    }

    sharedPrefs.edit().putString(Constants.SETTINGS.APPEARANCE.LANGUAGE, selectedCode).apply();
  }

  private void updateShortcuts(@NonNull Locale locale) {
    Configuration configuration = getResources().getConfiguration();
    configuration.setLocale(locale);
    getResources().updateConfiguration(
        configuration, getResources().getDisplayMetrics()
    );
    ShortcutUtil.refreshShortcuts(requireContext());
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
