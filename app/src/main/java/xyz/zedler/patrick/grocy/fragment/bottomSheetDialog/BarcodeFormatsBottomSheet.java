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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.checkbox.MaterialCheckBox;
import java.util.HashSet;
import java.util.Set;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetBarcodeFormatsBinding;
import xyz.zedler.patrick.grocy.util.Constants.BarcodeFormats;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.SCANNER;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS_DEFAULT;

public class BarcodeFormatsBottomSheet extends BaseBottomSheet {

  private final static String TAG = BarcodeFormatsBottomSheet.class.getSimpleName();

  private MainActivity activity;
  private FragmentBottomsheetBarcodeFormatsBinding binding;
  private SharedPreferences sharedPrefs;

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new BottomSheetDialog(requireContext(), R.style.Theme_Grocy_BottomSheetDialog);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetBarcodeFormatsBinding
        .inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    activity = (MainActivity) requireActivity();
    binding.setActivity(activity);
    binding.setSheet(this);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
    Set<String> enabledBarcodeFormats = sharedPrefs.getStringSet(
        SCANNER.BARCODE_FORMATS,
        SETTINGS_DEFAULT.SCANNER.BARCODE_FORMATS
    );
    if (enabledBarcodeFormats != null && !enabledBarcodeFormats.isEmpty()) {
      for (String barcodeFormat : enabledBarcodeFormats) {
        int resId = getResources()
            .getIdentifier(barcodeFormat, "id", activity.getPackageName());
        View checkBox = binding.checkboxContainer.findViewById(resId);
        if (checkBox != null) ((MaterialCheckBox) checkBox).setChecked(true);
      }
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.N_MR1)
  public void saveShortcuts() {
    Set<String> enabledBarcodeFormats = new HashSet<>();
    for (int i = 0; i <= binding.checkboxContainer.getChildCount(); i++) {
      MaterialCheckBox checkBox = (MaterialCheckBox) binding.checkboxContainer.getChildAt(i);
      if (checkBox == null || !checkBox.isChecked()) {
        continue;
      }
      if (checkBox.getId() == R.id.barcode_format_code128) {
        enabledBarcodeFormats.add(BarcodeFormats.BARCODE_FORMAT_CODE128);
      } else if (checkBox.getId() == R.id.barcode_format_code39) {
        enabledBarcodeFormats.add(BarcodeFormats.BARCODE_FORMAT_CODE39);
      } else if (checkBox.getId() == R.id.barcode_format_code93) {
        enabledBarcodeFormats.add(BarcodeFormats.BARCODE_FORMAT_CODE93);
      } else if (checkBox.getId() == R.id.barcode_format_codabar) {
        enabledBarcodeFormats.add(BarcodeFormats.BARCODE_FORMAT_CODABAR);
      } else if (checkBox.getId() == R.id.barcode_format_ean13) {
        enabledBarcodeFormats.add(BarcodeFormats.BARCODE_FORMAT_EAN13);
      } else if (checkBox.getId() == R.id.barcode_format_ean8) {
        enabledBarcodeFormats.add(BarcodeFormats.BARCODE_FORMAT_EAN8);
      } else if (checkBox.getId() == R.id.barcode_format_itf) {
        enabledBarcodeFormats.add(BarcodeFormats.BARCODE_FORMAT_ITF);
      } else if (checkBox.getId() == R.id.barcode_format_upca) {
        enabledBarcodeFormats.add(BarcodeFormats.BARCODE_FORMAT_UPCA);
      } else if (checkBox.getId() == R.id.barcode_format_upce) {
        enabledBarcodeFormats.add(BarcodeFormats.BARCODE_FORMAT_UPCE);
      } else if (checkBox.getId() == R.id.barcode_format_qr) {
        enabledBarcodeFormats.add(BarcodeFormats.BARCODE_FORMAT_QR);
      } else if (checkBox.getId() == R.id.barcode_format_pdf417) {
        enabledBarcodeFormats.add(BarcodeFormats.BARCODE_FORMAT_PDF417);
      } else if (checkBox.getId() == R.id.barcode_format_aztec) {
        enabledBarcodeFormats.add(BarcodeFormats.BARCODE_FORMAT_AZTEC);
      } else if (checkBox.getId() == R.id.barcode_format_matrix) {
        enabledBarcodeFormats.add(BarcodeFormats.BARCODE_FORMAT_MATRIX);
      } else if (checkBox.getId() == R.id.barcode_format_rss14) {
        enabledBarcodeFormats.add(BarcodeFormats.BARCODE_FORMAT_RSS14);
      } else if (checkBox.getId() == R.id.barcode_format_rsse) {
        enabledBarcodeFormats.add(BarcodeFormats.BARCODE_FORMAT_RSSE);
      }
    }
    sharedPrefs.edit().putStringSet(SCANNER.BARCODE_FORMATS, enabledBarcodeFormats).apply();
    activity.getCurrentFragment().updateBarcodeFormats();
    dismiss();
  }

  public static String getEnabledBarcodeFormats(Context context) {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    Set<String> enabledBarcodeFormats = sharedPrefs.getStringSet(
        SCANNER.BARCODE_FORMATS,
        SETTINGS_DEFAULT.SCANNER.BARCODE_FORMATS
    );
    if (enabledBarcodeFormats == null || enabledBarcodeFormats.isEmpty()) {
      return context.getString(R.string.setting_barcode_formats_description_all);
    }
    StringBuilder enabledBarcodeFormatsBuilder = new StringBuilder();
    for (String barcodeFormat : enabledBarcodeFormats) {
      int stringResId = context.getResources()
          .getIdentifier(barcodeFormat, "string", context.getPackageName());
      enabledBarcodeFormatsBuilder.append(context.getString(stringResId));
      enabledBarcodeFormatsBuilder.append(", ");
    }
    return context.getString(
        R.string.setting_barcode_formats_description,
        enabledBarcodeFormatsBuilder.substring(0, enabledBarcodeFormatsBuilder.length()-2)
    );
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
