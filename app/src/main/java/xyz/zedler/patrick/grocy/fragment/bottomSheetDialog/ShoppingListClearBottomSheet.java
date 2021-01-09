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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.fragment.ShoppingListFragment;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.util.Constants;

public class ShoppingListClearBottomSheet extends BaseBottomSheet {

    private final static String TAG = ShoppingListClearBottomSheet.class.getSimpleName();

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
                R.layout.fragment_bottomsheet_shopping_list_clear, container, false
        );

        activity = (MainActivity) getActivity();
        assert activity != null;

        Fragment currentFragment = activity.getCurrentFragment();
        if(currentFragment.getClass() != ShoppingListFragment.class) {
            dismiss();
            showMessage(activity.getString(R.string.error_undefined));
            return view;
        }
        ShoppingListFragment fragment = (ShoppingListFragment) activity.getCurrentFragment();
        assert getArguments() != null;
        ShoppingList shoppingList = getArguments().getParcelable(Constants.ARGUMENT.SHOPPING_LIST);
        assert shoppingList != null;

        view.findViewById(R.id.button_clear_shopping_list_all).setOnClickListener(v -> {
            dismiss();
            fragment.clearAllItems(
                    shoppingList,
                    () -> showMessage(
                            activity.getString(
                                    R.string.msg_shopping_list_cleared,
                                    shoppingList.getName()
                            )
                    ));
        });

        view.findViewById(R.id.button_clear_shopping_list_done).setOnClickListener(v -> {
            dismiss();
            fragment.clearDoneItems(shoppingList);
        });

        view.findViewById(R.id.button_clear_shopping_list_cancel).setOnClickListener(
                v -> dismiss()
        );

        return view;
    }

    private void showMessage(String msg) {
        activity.showSnackbar(
                Snackbar.make(activity.binding.frameMainContainer, msg, Snackbar.LENGTH_SHORT)
        );
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
