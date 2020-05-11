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
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.PurchaseFragment;
import xyz.zedler.patrick.grocy.model.CreateProduct;
import xyz.zedler.patrick.grocy.util.Constants;

public class InputNameBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private final static String TAG = "InputNameBottomSheet";

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
                R.layout.fragment_bottomsheet_input_name, container, false
        );

        activity = (MainActivity) getActivity();
        assert activity != null;

        if(getArguments() == null
                || getArguments().getString(Constants.ARGUMENT.PRODUCT_NAME) == null
        ) {
            dismissWithMessage(activity.getString(R.string.msg_error));
            return view;
        }

        setCancelable(false);

        String productName = getArguments().getString(Constants.ARGUMENT.PRODUCT_NAME);

        TextView textView = view.findViewById(R.id.text_input_name_question);
        textView.setText(activity.getString(R.string.description_input_name, productName));

        view.findViewById(R.id.button_input_name_create).setOnClickListener(v -> {
            Fragment current = activity.getCurrentFragment();
            if(current.getClass() == PurchaseFragment.class) {
                CreateProduct createProduct = new CreateProduct(
                        productName,
                        null,
                        null,
                        null,
                        null
                );
                Bundle bundle = new Bundle();
                bundle.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.CREATE_THEN_PURCHASE);
                bundle.putParcelable(Constants.ARGUMENT.CREATE_PRODUCT_OBJECT, createProduct);
                activity.replaceFragment(Constants.UI.MASTER_PRODUCT_SIMPLE, bundle, true);
            }
            dismiss();
        });

        view.findViewById(R.id.button_input_name_cancel).setOnClickListener(v -> dismiss());

        return view;
    }

    private void dismissWithMessage(String msg) {
        Snackbar.make(
                activity.findViewById(R.id.linear_container_main),
                msg,
                Snackbar.LENGTH_SHORT
        ).show();
        dismiss();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
