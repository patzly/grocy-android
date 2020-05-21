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
import com.google.android.material.button.MaterialButton;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.ConsumeFragment;
import xyz.zedler.patrick.grocy.fragment.PurchaseFragment;
import xyz.zedler.patrick.grocy.fragment.ShoppingListItemEditFragment;
import xyz.zedler.patrick.grocy.model.CreateProduct;
import xyz.zedler.patrick.grocy.util.Constants;

public class InputBarcodeBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private final static boolean DEBUG = false;
    private final static String TAG = "ConsumeBarcodeBottomSheet";

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
        assert activity != null;

        setCancelable(false);

        Fragment currentFragment = activity.getCurrentFragment();

        TextView textView = view.findViewById(R.id.text_input_barcode_question);
        MaterialButton buttonNew = view.findViewById(R.id.button_input_barcode_new);

        if(currentFragment.getClass() == ConsumeFragment.class) {
            textView.setText("Link barcode to an existing product?");
            buttonNew.setVisibility(View.GONE);
        } else { // PurchaseFragment
            textView.setText("Link barcode to an existing product or create a new product with it?");
            buttonNew.setVisibility(View.VISIBLE);
        }

        view.findViewById(R.id.button_input_barcode_link).setOnClickListener(v -> {
            if(currentFragment.getClass() == ConsumeFragment.class) {
                ((ConsumeFragment) currentFragment).addInputAsBarcode();
            } else if(currentFragment.getClass() == PurchaseFragment.class) {
                ((PurchaseFragment) currentFragment).addInputAsBarcode();
            } else if(currentFragment.getClass() == ShoppingListItemEditFragment.class) {
                ((ShoppingListItemEditFragment) currentFragment).addInputAsBarcode();
            }
            dismiss();
        });

        view.findViewById(R.id.button_input_barcode_new).setOnClickListener(v -> {
            Fragment current = activity.getCurrentFragment();
            if(current.getClass() == PurchaseFragment.class && getArguments() != null) {
                CreateProduct createProduct = new CreateProduct(
                        null,
                        getArguments().getString(Constants.ARGUMENT.BARCODES),
                        null,
                        null,
                        null
                );
                Bundle bundle = new Bundle();
                bundle.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.CREATE_THEN_PURCHASE);
                bundle.putParcelable(Constants.ARGUMENT.CREATE_PRODUCT_OBJECT, createProduct);
                activity.replaceFragment(Constants.UI.MASTER_PRODUCT_SIMPLE, bundle, true);
            } else if(current.getClass() == ShoppingListItemEditFragment.class && getArguments() != null) {
                CreateProduct createProduct = new CreateProduct(
                        null,
                        getArguments().getString(Constants.ARGUMENT.BARCODES),
                        null,
                        null,
                        null
                );
                Bundle bundle = new Bundle();
                bundle.putString(Constants.ARGUMENT.TYPE, Constants.ACTION.CREATE_THEN_SHOPPING_LIST_ITEM);
                bundle.putParcelable(Constants.ARGUMENT.CREATE_PRODUCT_OBJECT, createProduct);
                activity.replaceFragment(Constants.UI.MASTER_PRODUCT_SIMPLE, bundle, true);
            }
            dismiss();
        });

        view.findViewById(R.id.button_input_barcode_cancel).setOnClickListener(v -> {
            if(currentFragment.getClass() == ConsumeFragment.class) {
                ((ConsumeFragment) currentFragment).clearAll();
            } else if(currentFragment.getClass() == PurchaseFragment.class) {
                ((PurchaseFragment) currentFragment).clearAll();
            } else if(currentFragment.getClass() == ShoppingListItemEditFragment.class) {
                ((ShoppingListItemEditFragment) currentFragment).clearAll();
            }
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
