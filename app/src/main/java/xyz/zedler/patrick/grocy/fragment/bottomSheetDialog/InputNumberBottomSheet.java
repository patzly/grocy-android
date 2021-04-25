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
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetInputNumberBinding;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class InputNumberBottomSheet extends BaseBottomSheet {

    private final static String TAG = InputNumberBottomSheet.class.getSimpleName();

    private MainActivity activity;
    private FragmentBottomsheetInputNumberBinding binding;

    private MutableLiveData<String> numberInputLive;

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
        binding = FragmentBottomsheetInputNumberBinding.inflate(
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

        numberInputLive = new MutableLiveData<>();
        Object number = requireArguments().get(Constants.ARGUMENT.NUMBER);
        if(number instanceof Double) {
            numberInputLive.setValue(NumUtil.trim((Double) number));
        } else if(number instanceof Integer) {
            numberInputLive.setValue(NumUtil.trim((Integer) number));
        }

        binding.editText.setInputType(
                number instanceof Double
                        ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                        : InputType.TYPE_CLASS_NUMBER
        );

        if(requireArguments().containsKey(Constants.ARGUMENT.HINT)) {
            binding.textInput.setHint(requireArguments().getString(Constants.ARGUMENT.HINT));
        } else {
            binding.textInput.setHint(getString(R.string.property_number));
        }
    }

    public MutableLiveData<String> getNumberInputLive() {
        return numberInputLive;
    }

    public void more() {
        String currentInput = numberInputLive.getValue();
        String nextInput;
        if(!NumUtil.isStringNum(currentInput)) {
            nextInput = String.valueOf(1);
        } else {
            nextInput = NumUtil.trim(Double.parseDouble(currentInput) + 1);
        }
        numberInputLive.setValue(nextInput);
    }

    public void less() {
        String currentInput = numberInputLive.getValue();
        if(!NumUtil.isStringNum(currentInput)) return;
        String nextInput = NumUtil.trim(Double.parseDouble(currentInput) - 1);
        numberInputLive.setValue(nextInput);
    }

    public void save() {
        activity.getCurrentFragment().saveNumber(numberInputLive.getValue(), requireArguments());
        dismiss();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
