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

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetInputBinding;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class InputBottomSheet extends BaseBottomSheetDialogFragment {

  private final static String TAG = InputBottomSheet.class.getSimpleName();

  private MainActivity activity;
  private FragmentBottomsheetInputBinding binding;

  private MutableLiveData<String> inputLive;
  private int maxDecimalPlacesAmount;

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

    maxDecimalPlacesAmount = PreferenceManager.getDefaultSharedPreferences(requireContext()).getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );

    inputLive = new MutableLiveData<>();
    int inputType;
    boolean showMoreLess;
    Object number = requireArguments().get(ARGUMENT.NUMBER);
    Object text = requireArguments().get(ARGUMENT.TEXT);
    if (number instanceof Double) {
      inputLive.setValue(NumUtil.trimAmount((Double) number, maxDecimalPlacesAmount));
      inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED;
      showMoreLess = true;
    } else if (number instanceof Integer) {
      inputLive.setValue(NumUtil.trimAmount((Integer) number, maxDecimalPlacesAmount));
      inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED;
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
      nextInput = NumUtil.trimAmount(NumUtil.toDouble(currentInput) + 1, maxDecimalPlacesAmount);
    }
    inputLive.setValue(nextInput);
  }

  public void less() {
    String currentInput = inputLive.getValue();
    if (!NumUtil.isStringNum(currentInput)) {
      return;
    }
    String nextInput = NumUtil.trimAmount(NumUtil.toDouble(currentInput) - 1, maxDecimalPlacesAmount);
    inputLive.setValue(nextInput);
  }

  @Override
  public void applyBottomInset(int bottom) {
    binding.linearContainer.setPadding(
        binding.linearContainer.getPaddingLeft(),
        binding.linearContainer.getPaddingTop(),
        binding.linearContainer.getPaddingRight(),
        UiUtil.dpToPx(activity, 12) + bottom
    );
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
