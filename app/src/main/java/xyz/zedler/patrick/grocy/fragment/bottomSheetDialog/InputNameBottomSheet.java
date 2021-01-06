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

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.model.CreateProduct;

public class InputNameBottomSheet extends BaseBottomSheet {

    private final static String TAG = InputNameBottomSheet.class.getSimpleName();

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
                R.layout.fragment_bottomsheet_input_name, container, false
        );

        MainActivity activity = (MainActivity) getActivity();
        assert activity != null && getArguments() != null;

        setCancelable(false);

        String productName = InputNameBottomSheetArgs
                .fromBundle(getArguments())
                .getProductName();

        TextView textView = view.findViewById(R.id.text_input_name_question);
        textView.setText(activity.getString(R.string.description_input_name, productName));

        view.findViewById(R.id.button_input_name_create).setOnClickListener(v -> {
            CreateProduct createProduct = new CreateProduct(
                    productName,
                    null,
                    null,
                    null,
                    null
            );
            /*NavHostFragment.findNavController(this).navigate(
                    InputNameBottomSheetDirections
                            .actionInputNameBottomSheetDialogFragmentToMasterProductSimpleFragment(
                                    Constants.ACTION.CREATE
                            ).setCreateProductObject(createProduct));*/
            navigateDeepLink(getString(R.string.deep_link_masterProductFragment));
            dismiss();
        });

        view.findViewById(R.id.button_input_name_cancel).setOnClickListener(v -> dismiss());

        return view;
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
