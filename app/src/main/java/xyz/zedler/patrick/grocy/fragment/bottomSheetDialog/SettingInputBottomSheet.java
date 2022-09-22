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
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.SHOPPING_MODE;
import xyz.zedler.patrick.grocy.util.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class SettingInputBottomSheet extends BaseBottomSheetDialogFragment {

  private final static String TAG = SettingInputBottomSheet.class.getSimpleName();

  private MainActivity activity;

  private EditText editText;

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
    View view = inflater.inflate(
        R.layout.fragment_bottomsheet_setting_input, container, false
    );

    activity = (MainActivity) requireActivity();
    Bundle bundle = requireArguments();

    String option = bundle.getString(Constants.ARGUMENT.PREFERENCE);
    if (option == null) {
      dismiss();
      return view;
    }

    // INITIALIZE VIEWS

    TextView textViewTitle = view.findViewById(R.id.text_setting_input_title);

    TextInputLayout textInput = view.findViewById(R.id.text_input_setting_input);
    editText = textInput.getEditText();
    assert editText != null;
    editText.setInputType(
        option.equals(Constants.SETTINGS.STOCK.DUE_SOON_DAYS)
            ? InputType.TYPE_CLASS_NUMBER
            : InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
    );

    view.findViewById(R.id.button_setting_input_more).setOnClickListener(v -> {
      if (editText.getText().toString().isEmpty()) {
        editText.setText(String.valueOf(1));
      } else {
        double amountNew = Double.parseDouble(editText.getText().toString()) + 1;
        editText.setText(NumUtil.trim(amountNew));
      }
    });

    view.findViewById(R.id.button_setting_input_less).setOnClickListener(v -> {
      if (!editText.getText().toString().isEmpty()) {
        double amountNew = Double.parseDouble(editText.getText().toString()) - 1;
        if (amountNew >= 0) {
          editText.setText(NumUtil.trim(amountNew));
        }
      }
    });

    MaterialButton buttonClear = view.findViewById(R.id.button_setting_input_clear);
    buttonClear.setOnClickListener(v -> {
      editText.setText(null);
      textInput.clearFocus();
      activity.hideKeyboard();
    });

    view.findViewById(R.id.button_setting_input_save).setOnClickListener(v -> {
      if (option.equals(Constants.SETTINGS.STOCK.DUE_SOON_DAYS)) {
        if (editText.getText().toString().isEmpty()) {
          textInput.setError(activity.getString(R.string.error_empty));
          return;
        } else {
          textInput.setErrorEnabled(false);
        }
      }
      activity.getCurrentFragment().setOption(editText.getText().toString(), option);
      dismiss();
    });

    String title = null;
    String hint = null;
    String input = bundle.getString(Constants.ARGUMENT.TEXT);
    switch (option) {
      case Constants.SETTINGS.STOCK.DUE_SOON_DAYS:
        title = activity.getString(R.string.setting_due_soon_days);
        hint = activity.getString(R.string.property_days);
        buttonClear.setText(activity.getString(R.string.action_reset));
        buttonClear.setOnClickListener(v -> {
          editText.setText(String.valueOf(5));
          textInput.setErrorEnabled(false);
          textInput.clearFocus();
          activity.hideKeyboard();
        });
        break;
      case STOCK.DEFAULT_PURCHASE_AMOUNT:
        title = activity.getString(R.string.setting_default_amount_purchase);
        hint = activity.getString(R.string.property_amount);
        break;
      case STOCK.DEFAULT_CONSUME_AMOUNT:
        title = activity.getString(R.string.setting_default_amount_consume);
        hint = activity.getString(R.string.property_amount);
        break;
      case SHOPPING_MODE.UPDATE_INTERVAL:
        title = activity.getString(R.string.setting_shopping_mode_update_interval);
        hint = activity.getString(R.string.property_seconds);
        break;
    }

    textViewTitle.setText(title);

    textInput.setHint(hint);

    editText.setText(input == null || input.isEmpty() || input.equals("null") ? null : input);

    return view;
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
