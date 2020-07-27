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
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputLayout;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.ScanBatchActivity;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class PriceBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private final static String TAG = "PriceBottomSheet";

    private ScanBatchActivity activity;

    private EditText editTextPrice;

    private boolean productDiscarded = false;

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
                R.layout.fragment_bottomsheet_price, container, false
        );

        activity = (ScanBatchActivity) getActivity();
        Bundle bundle = getArguments();
        assert activity != null && bundle != null;

        String price = bundle.getString(Constants.ARGUMENT.PRICE);
        String currency = bundle.getString(Constants.ARGUMENT.CURRENCY);

        TextInputLayout textInputPrice = view.findViewById(R.id.text_price);
        textInputPrice.setHint(getString(R.string.property_price_in, currency));

        editTextPrice = textInputPrice.getEditText();
        assert editTextPrice != null;
        editTextPrice.setOnEditorActionListener(
                (TextView v, int actionId, KeyEvent event) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE && !productDiscarded) {
                        dismiss();
                        return true;
                    } return false;
                });

        view.findViewById(R.id.button_price_save).setOnClickListener(
                v -> dismiss()
        );
        view.findViewById(R.id.button_price_discard).setOnClickListener(
                v -> {
                    activity.discardCurrentProduct();
                    productDiscarded = true;
                    dismiss();
                }
        );

        setCancelable(false);

        fillForm(price);

        return view;
    }

    private void fillForm(String price) {
        if(price != null) {
            editTextPrice.setText(price);
        } else {
            editTextPrice.requestFocus();
            new Handler().postDelayed(() -> activity.showKeyboard(editTextPrice), 200);
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if(productDiscarded) return;

        String price = editTextPrice.getText().toString();
        price = NumUtil.formatPrice(price);

        activity.setPrice(price);
        activity.askNecessaryDetails();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
