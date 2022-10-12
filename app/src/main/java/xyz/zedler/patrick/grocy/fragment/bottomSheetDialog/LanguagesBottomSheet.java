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

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.LanguageAdapter;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetListSelectionBinding;
import xyz.zedler.patrick.grocy.model.Language;
import xyz.zedler.patrick.grocy.util.LocaleUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class LanguagesBottomSheet extends BaseBottomSheetDialogFragment
    implements LanguageAdapter.LanguageAdapterListener {

  private final static String TAG = LanguagesBottomSheet.class.getSimpleName();

  private FragmentBottomsheetListSelectionBinding binding;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetListSelectionBinding.inflate(
        inflater, container, false
    );

    MainActivity activity = (MainActivity) requireActivity();

    binding.textListSelectionTitle.setText(getString(R.string.setting_language_description));
    binding.textListSelectionTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    binding.textListSelectionTitle.setGravity(Gravity.CENTER_HORIZONTAL);

    binding.textListSelectionDescription.setText(getString(R.string.setting_language_info));
    binding.textListSelectionDescription.setVisibility(View.VISIBLE);

    binding.recyclerListSelection.setLayoutManager(
        new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    );
    binding.recyclerListSelection.setAdapter(
        new LanguageAdapter(
            LocaleUtil.getLanguages(activity),
            LocaleUtil.getLanguageCode(AppCompatDelegate.getApplicationLocales()),
            this
        )
    );

    return binding.getRoot();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  @Override
  public void onItemRowClicked(@Nullable Language language) {
    String code = language != null ? language.getCode() : null;
    LocaleListCompat previous = AppCompatDelegate.getApplicationLocales();
    LocaleListCompat selected = LocaleListCompat.forLanguageTags(code);
    if (!previous.equals(selected)) {
      performHapticClick();
      dismiss();
      AppCompatDelegate.setApplicationLocales(selected);
    }
  }

  @Override
  public void applyBottomInset(int bottom) {
    binding.recyclerListSelection.setPadding(
        0, UiUtil.dpToPx(requireContext(), 8),
        0, UiUtil.dpToPx(requireContext(), 8) + bottom
    );
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
