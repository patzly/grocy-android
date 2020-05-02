package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.annotation.SuppressLint;
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
import com.google.android.material.checkbox.MaterialCheckBox;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.fragment.PurchaseFragment;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.util.Constants;

public class BBDateBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private final static boolean DEBUG = false;
    private final static String TAG = "BBDateBottomSheetDialogFragment";

    private MainActivity activity;

    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private ProductDetails productDetails;
    private DatePicker datePicker;
    private MaterialCheckBox neverExpires;

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
                R.layout.fragment_bottomsheet_purchase_bbdate, container, false
        );

        activity = (MainActivity) getActivity();
        Bundle bundle = getArguments();
        assert activity != null && bundle != null;

        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        datePicker = view.findViewById(R.id.datepicker_purchase_bbd);
        neverExpires = view.findViewById(R.id.checkbox_purchase_bbd_never_expires);

        neverExpires.setOnCheckedChangeListener((v, isChecked) -> {
            if(isChecked) {
                datePicker.setEnabled(false);
                datePicker.setAlpha(0.5f);
            } else {
                datePicker.setEnabled(true);
                datePicker.setAlpha(1.0f);
            }
        });

        view.findViewById(R.id.linear_purchase_bbd_never_expires).setOnClickListener(
                v -> neverExpires.setChecked(!neverExpires.isChecked())
        );
        view.findViewById(R.id.button_purchase_bbd_reset).setOnClickListener(
                v -> {
                    calendar = Calendar.getInstance();
                    fillForm(null);
                }
        );
        view.findViewById(R.id.button_purchase_bbd_save).setOnClickListener(
                v -> dismiss()
        );

        String selectedBestBeforeDate = bundle.getString(Constants.ARGUMENT.SELECTED_DATE);
        productDetails = bundle.getParcelable(Constants.ARGUMENT.PRODUCT_DETAILS);

        fillForm(selectedBestBeforeDate);

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
                return;
                // TODO: Snackbar
            }
            datePicker.setEnabled(true);
            datePicker.setAlpha(1.0f);
            neverExpires.setChecked(false);

        } else if(productDetails != null) {

            int defaultBestBeforeDays = productDetails.getProduct().getDefaultBestBeforeDays();
            if(defaultBestBeforeDays < 0) {
                datePicker.setEnabled(false);
                datePicker.setAlpha(0.5f);
                neverExpires.setChecked(true);
            } else {
                datePicker.setEnabled(true);
                datePicker.setAlpha(1.0f);
                neverExpires.setChecked(false);
                calendar.add(Calendar.DAY_OF_MONTH, defaultBestBeforeDays);
            }
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

        Fragment currentFragment = activity.getCurrentFragment();
        if(currentFragment.getClass() == PurchaseFragment.class) {
            ((PurchaseFragment) currentFragment).selectBestBeforeDate(date);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
