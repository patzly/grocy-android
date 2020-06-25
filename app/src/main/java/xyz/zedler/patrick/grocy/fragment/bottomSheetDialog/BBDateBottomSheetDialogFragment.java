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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.ScanBatchActivity;
import xyz.zedler.patrick.grocy.fragment.PurchaseFragment;
import xyz.zedler.patrick.grocy.util.Constants;

public class BBDateBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private final static String TAG = "BBDateBottomSheet";

    private Activity activity;

    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private String defaultBestBeforeDays;
    private DatePicker datePicker;
    private MaterialCheckBox neverExpires;

    private boolean productDiscarded = false;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext(), R.style.Theme_Grocy_BottomSheetDialog);
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(
                R.layout.fragment_bottomsheet_bbdate, container, false
        );

        activity =  getActivity();
        Bundle bundle = getArguments();
        assert activity != null && bundle != null;

        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        datePicker = view.findViewById(R.id.date_picker_bbd);
        neverExpires = view.findViewById(R.id.checkbox_bbd_never_expires);

        neverExpires.setOnCheckedChangeListener(
                (v, isChecked) -> datePicker.animate()
                        .alpha(isChecked ? 0.5f : 1)
                        .withEndAction(() -> datePicker.setEnabled(!isChecked))
                        .setDuration(200)
                        .start()
        );

        view.findViewById(R.id.linear_bbd_never_expires).setOnClickListener(
                v -> neverExpires.setChecked(!neverExpires.isChecked())
        );
        view.findViewById(R.id.button_bbd_reset).setOnClickListener(
                v -> {
                    calendar = Calendar.getInstance();
                    fillForm(null);
                }
        );
        MaterialButton buttonDiscard = view.findViewById(R.id.button_bbd_discard);
        if(activity.getClass() == MainActivity.class) {
            buttonDiscard.setVisibility(View.GONE);
        }
        buttonDiscard.setOnClickListener(
                v -> {
                    if(activity.getClass() == ScanBatchActivity.class) {
                        ((ScanBatchActivity) activity).discardCurrentProduct();
                        productDiscarded = true;
                        dismiss();
                    }
                }
        );
        view.findViewById(R.id.button_bbd_save).setOnClickListener(
                v -> dismiss()
        );

        String selectedBestBeforeDate = bundle.getString(Constants.ARGUMENT.SELECTED_DATE);
        defaultBestBeforeDays = bundle.getString(Constants.ARGUMENT.DEFAULT_BEST_BEFORE_DAYS);

        fillForm(selectedBestBeforeDate);

        if(activity.getClass() == ScanBatchActivity.class) {
            setCancelable(false);
        }

        return view;
    }

    private void fillForm(String selectedBestBeforeDate) {
        if(selectedBestBeforeDate != null
                && selectedBestBeforeDate.equals(Constants.DATE.NEVER_EXPIRES)) {

            datePicker.setEnabled(false);
            datePicker.setAlpha(0.5f);
            neverExpires.setChecked(true);

        } else if(selectedBestBeforeDate != null) {

            try {
                Date date = dateFormat.parse(selectedBestBeforeDate);
                if(date != null) calendar.setTime(date);
            } catch (ParseException e) {
                fillForm(null);
                showSnackbarMessage(activity.getString(R.string.error_undefined));
                return;
            }
            datePicker.setEnabled(true);
            datePicker.setAlpha(1.0f);
            neverExpires.setChecked(false);

        } else if(defaultBestBeforeDays != null) {

            if(Integer.parseInt(defaultBestBeforeDays) < 0) {
                datePicker.setEnabled(false);
                datePicker.setAlpha(0.5f);
                neverExpires.setChecked(true);
            } else {
                datePicker.setEnabled(true);
                datePicker.setAlpha(1.0f);
                neverExpires.setChecked(false);
                calendar.add(Calendar.DAY_OF_MONTH, Integer.parseInt(defaultBestBeforeDays));
            }

        } else {

            datePicker.setEnabled(false);
            datePicker.setAlpha(0.5f);
            neverExpires.setChecked(true);

        }

        datePicker.updateDate(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if(productDiscarded) return;

        String date;
        if(!neverExpires.isChecked()) {
            calendar.set(
                    datePicker.getYear(),
                    datePicker.getMonth(),
                    datePicker.getDayOfMonth()
            );
            date = dateFormat.format(calendar.getTime());
        } else {
            date = Constants.DATE.NEVER_EXPIRES;
        }

        if(activity.getClass() == MainActivity.class) {
            Fragment currentFragment = ((MainActivity) activity).getCurrentFragment();
            if(currentFragment.getClass() == PurchaseFragment.class) {
                ((PurchaseFragment) currentFragment).selectBestBeforeDate(date);
            }
        } else if(activity.getClass() == ScanBatchActivity.class) {
            ((ScanBatchActivity) activity).setBestBeforeDate(date);
            ((ScanBatchActivity) activity).askNecessaryDetails();
        }
    }

    private void showSnackbarMessage(String msg) {
        View view = null;
        if(activity.getClass() == MainActivity.class) {
            view = activity.findViewById(R.id.frame_main_container);
        } else if(activity.getClass() == ScanBatchActivity.class) {
            view = activity.findViewById(R.id.barcode_scan_batch);
        }
        if(view != null) Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show();
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
