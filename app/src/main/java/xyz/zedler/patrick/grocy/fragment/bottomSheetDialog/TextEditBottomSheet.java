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

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.TextUtil;

public class TextEditBottomSheet extends BaseBottomSheet {

    private final static String TAG = TextEditBottomSheet.class.getSimpleName();

    private MainActivity activity;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new BottomSheetDialog(
                requireContext(), R.style.Theme_Grocy_BottomSheetDialog_SoftInput
        );
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(
                R.layout.fragment_bottomsheet_text_edit, container, false
        );

        activity = (MainActivity) getActivity();
        assert activity != null;

        if(getArguments() == null
                || getArguments().getString(Constants.ARGUMENT.TITLE) == null
        ) {
            dismissWithMessage(activity.getString(R.string.error_undefined));
            return view;
        }

        TextView textView = view.findViewById(R.id.text_text_edit_title);
        textView.setText(getArguments().getString(Constants.ARGUMENT.TITLE));

        TextInputLayout textInputLayout = view.findViewById(R.id.text_input_text_edit_text);
        if(getArguments().getString(Constants.ARGUMENT.HINT) != null) {
            textInputLayout.setHint(getArguments().getString(Constants.ARGUMENT.HINT));
        }
        EditText editText = textInputLayout.getEditText();
        assert editText != null;
        if(getArguments().getString(Constants.ARGUMENT.TEXT) != null) {
            editText.setText(getArguments().getString(Constants.ARGUMENT.TEXT));
        } else if(getArguments().getString(Constants.ARGUMENT.HTML) != null) {
            Spanned text = Html.fromHtml(getArguments().getString(Constants.ARGUMENT.HTML));
            editText.setText(TextUtil.trimCharSequence(text));
        }

        view.findViewById(R.id.button_text_edit_save).setOnClickListener(v -> {
            /*Fragment current = activity.getCurrentFragment();
            if(current.getClass() == MasterProductSimpleFragment.class) {
                ((MasterProductSimpleFragment) current).editDescription(
                        Html.toHtml(editText.getText()),
                        editText.getText().toString()
                );
            } else if(current.getClass() == ShoppingListFragment.class) {
                ((ShoppingListFragment) current).saveNotes(
                        (Spanned) TextUtil.trimCharSequence(editText.getText())
                );
            } else if(current.getClass() == ShoppingModeFragment.class) {
                ((ShoppingModeFragment) current).saveNotes(
                        (Spanned) TextUtil.trimCharSequence(editText.getText())
                );
            }*/

            Spanned spanned = (Spanned) TextUtil.trimCharSequence(editText.getText());
            activity.getCurrentFragment().saveText(spanned);
            dismiss();
        });

        view.findViewById(R.id.button_text_edit_clear).setOnClickListener(
                v -> editText.setText(null)
        );

        return view;
    }

    private void dismissWithMessage(String msg) {
        activity.showSnackbar(
                Snackbar.make(
                        activity.findViewById(R.id.frame_main_container),
                        msg,
                        Snackbar.LENGTH_SHORT
                )
        );
        dismiss();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
