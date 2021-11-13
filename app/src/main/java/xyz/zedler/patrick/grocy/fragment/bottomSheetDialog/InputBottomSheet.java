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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetInputBinding;
import xyz.zedler.patrick.grocy.util.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class InputBottomSheet extends BaseBottomSheet {

  private final static String TAG = InputBottomSheet.class.getSimpleName();

  private MainActivity activity;
  private FragmentBottomsheetInputBinding binding;

  private MutableLiveData<String> inputLive;

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new BottomSheetDialog(requireContext(), R.style.Theme_Grocy_BottomSheetDialog_SoftInput);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetInputBinding.inflate(
        inflater, container, false
    );
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    activity = (MainActivity) requireActivity();
    binding.setBottomsheet(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    inputLive = new MutableLiveData<>();
    int inputType;
    boolean showMoreLess;
    Object number = requireArguments().get(ARGUMENT.NUMBER);
    Object text = requireArguments().get(ARGUMENT.TEXT);
    if (number instanceof Double) {
      inputLive.setValue(NumUtil.trim((Double) number));
      inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
      showMoreLess = true;
    } else if (number instanceof Integer) {
      inputLive.setValue(NumUtil.trim((Integer) number));
      inputType = InputType.TYPE_CLASS_NUMBER;
      showMoreLess = true;
    } else {
      inputLive.setValue(text != null ? (String) text : "");
      inputType = InputType.TYPE_CLASS_TEXT;
      showMoreLess = false;
    }

    binding.editText.setInputType(inputType);
    if (!showMoreLess) {
      binding.more.setVisibility(View.GONE);
      binding.less.setVisibility(View.GONE);
    }
    if (requireArguments().containsKey(ARGUMENT.HINT)) {
      binding.textInput.setHint(requireArguments().getString(ARGUMENT.HINT));
    }
  }

  @Override
  public void onDismiss(@NonNull DialogInterface dialog) {
    super.onDismiss(dialog);

    String text = inputLive.getValue();
    if (text != null) {
      text = text.trim();
    } else {
      text = "";
    }
    activity.getCurrentFragment().saveInput(text, requireArguments());
  }

  public MutableLiveData<String> getInputLive() {
    return inputLive;
  }

  public void more() {
    String currentInput = inputLive.getValue();
    String nextInput;
    if (!NumUtil.isStringNum(currentInput)) {
      nextInput = String.valueOf(1);
    } else {
      nextInput = NumUtil.trim(Double.parseDouble(currentInput) + 1);
    }
    inputLive.setValue(nextInput);
  }

  public void less() {
    String currentInput = inputLive.getValue();
    if (!NumUtil.isStringNum(currentInput)) {
      return;
    }
    String nextInput = NumUtil.trim(Double.parseDouble(currentInput) - 1);
    inputLive.setValue(nextInput);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
