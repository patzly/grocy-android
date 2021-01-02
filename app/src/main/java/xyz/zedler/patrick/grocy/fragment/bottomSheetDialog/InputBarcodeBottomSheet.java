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

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.fragment.ConsumeFragment;
import xyz.zedler.patrick.grocy.util.Constants;

public class InputBarcodeBottomSheet extends BaseBottomSheet {

    private final static String TAG = InputBarcodeBottomSheet.class.getSimpleName();

    private MainActivity activity;

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
                R.layout.fragment_bottomsheet_input_barcode, container, false
        );

        activity = (MainActivity) getActivity();
        assert activity != null && getArguments() != null;

        String barcode = getArguments().getString(Constants.ARGUMENT.BARCODE);

        setCancelable(false);

        TextView textView = view.findViewById(R.id.text_input_barcode_question);
        MaterialButton buttonNew = view.findViewById(R.id.button_input_barcode_new);

        if(activity.getCurrentFragment() instanceof ConsumeFragment) {
            textView.setText(activity.getString(R.string.title_link_barcode_to_existing));
            buttonNew.setVisibility(View.GONE);
        } else {
            textView.setText(activity.getString(R.string.title_link_barcode_or_create));
            buttonNew.setVisibility(View.VISIBLE);
        }

        view.findViewById(R.id.button_input_barcode_link).setOnClickListener(v -> {
            activity.getCurrentFragment().addBarcode(barcode);
            dismiss();
        });

        view.findViewById(R.id.button_input_barcode_new).setOnClickListener(v -> {
            activity.getCurrentFragment().createProductFromBarcode(barcode);
            dismiss();
        });

        view.findViewById(R.id.button_input_barcode_cancel).setOnClickListener(v -> {
            activity.getCurrentFragment().clearFields();
            dismiss();
        });

        return view;
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
