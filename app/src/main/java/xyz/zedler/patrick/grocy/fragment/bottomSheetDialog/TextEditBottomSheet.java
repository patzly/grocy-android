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
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout.LayoutParams;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetTextEditBinding;
import xyz.zedler.patrick.grocy.util.TextUtil;

public class TextEditBottomSheet extends BaseBottomSheetDialogFragment {

  private final static String TAG = TextEditBottomSheet.class.getSimpleName();

  private FragmentBottomsheetTextEditBinding binding;
  private MainActivity activity;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetTextEditBinding.inflate(inflater, container, false);

    activity = (MainActivity) getActivity();
    assert activity != null;

    if (getArguments() == null
        || getArguments().getString(Constants.ARGUMENT.TITLE) == null
    ) {
      activity.showSnackbar(activity.getSnackbar(R.string.error_undefined, Snackbar.LENGTH_SHORT));
      dismiss();
      return binding.getRoot();
    }

    binding.toolbarTextEdit.setTitle(getArguments().getString(Constants.ARGUMENT.TITLE));

    if (getArguments().getString(Constants.ARGUMENT.HINT) != null) {
      binding.textInputTextEditText.setHint(getArguments().getString(Constants.ARGUMENT.HINT));
    }
    EditText editText = binding.textInputTextEditText.getEditText();
    assert editText != null;
    if (getArguments().getString(Constants.ARGUMENT.TEXT) != null) {
      editText.setText(getArguments().getString(Constants.ARGUMENT.TEXT));
    } else if (getArguments().getString(Constants.ARGUMENT.HTML) != null) {
      Spanned text = Html.fromHtml(getArguments().getString(Constants.ARGUMENT.HTML));
      editText.setText(TextUtil.trimCharSequence(text));
    }

    binding.buttonTextEditSave.setOnClickListener(v -> {
      Spanned spanned = (Spanned) TextUtil.trimCharSequence(editText.getText());
      activity.getCurrentFragment().saveText(spanned);
      dismiss();
    });

    binding.buttonTextEditClear.setOnClickListener(
        v -> editText.setText(null)
    );

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
    binding.linearContainerScroll.setLayoutParams(params);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
