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

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.ScanBatchActivity;
import xyz.zedler.patrick.grocy.util.Constants;

public class ExitScanBatchBottomSheetDialogFragment extends CustomBottomSheetDialogFragment {

    private final static String TAG = "ExitScanBatchBottomSheet";

    private ScanBatchActivity activity;

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
                R.layout.fragment_bottomsheet_exit_scan_batch, container, false
        );

        activity = (ScanBatchActivity) getActivity();
        assert activity != null && getArguments() != null;

        view.findViewById(R.id.button_exit_scan_batch_open).setOnClickListener(v -> {
            dismiss();
            activity.setResult(
                    Activity.RESULT_OK,
                    new Intent().putParcelableArrayListExtra(
                            Constants.ARGUMENT.BATCH_ITEMS,
                            getArguments().getParcelableArrayList(Constants.ARGUMENT.BATCH_ITEMS)
                    )
            );
            activity.finish();
        });

        view.findViewById(R.id.button_exit_scan_batch_discard).setOnClickListener(v -> {
            dismiss();
            activity.setResult(Activity.RESULT_CANCELED);
            activity.finish();
        });

        return view;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        activity.resumeScan();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
