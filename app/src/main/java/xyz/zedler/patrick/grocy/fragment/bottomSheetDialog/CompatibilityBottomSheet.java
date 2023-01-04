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

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetCompatibilityBinding;

public class CompatibilityBottomSheet extends BaseBottomSheetDialogFragment {

  private final static String TAG = CompatibilityBottomSheet.class.getSimpleName();

  private FragmentBottomsheetCompatibilityBinding binding;

  @SuppressLint("ApplySharedPref")
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetCompatibilityBinding.inflate(
        inflater, container, false
    );

    MainActivity activity = (MainActivity) requireActivity();

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);

    ArrayList<String> supportedVersions = requireArguments().getStringArrayList(
        Constants.ARGUMENT.SUPPORTED_VERSIONS
    );
    assert supportedVersions != null;
    String currentVersion = requireArguments().getString(Constants.ARGUMENT.VERSION);

    StringBuilder supportedVersionsSingle = new StringBuilder();
    for (String version : supportedVersions) {
      supportedVersionsSingle.append("- ").append(version).append("\n");
    }

    binding.textCompatibilityMsg.setText(activity.getString(
        R.string.msg_compatibility,
        currentVersion,
        supportedVersionsSingle
    ));

    binding.buttonCompatibilityCancel.setOnClickListener(v -> {
      dismiss();
      activity.getCurrentFragment().enableLoginButtons();
    });

    binding.buttonCompatibilityIgnore.setOnClickListener(v -> {
      prefs.edit().putString(Constants.PREF.VERSION_COMPATIBILITY_IGNORED, currentVersion)
          .apply();
      activity.getCurrentFragment().login(false);
      dismiss();
    });

    setCancelable(false);

    return binding.getRoot();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  @Override
  public void applyBottomInset(int bottom) {
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    params.setMargins(0, 0, 0, bottom);
    binding.linearContainer.setLayoutParams(params);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
