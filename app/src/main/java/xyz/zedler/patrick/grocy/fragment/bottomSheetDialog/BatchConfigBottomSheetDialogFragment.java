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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.ScanBatchActivity;
import xyz.zedler.patrick.grocy.util.Constants;

public class BatchConfigBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private final static String TAG = "BatchConfigBottomSheet";

    private SharedPreferences sharedPrefs;

    private ScanBatchActivity activity;
    private TextView textViewBestBeforeDate, textViewPrice, textViewStore, textViewLocation;
    private TextView textViewStockLocation, textViewSpecific;

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
                R.layout.fragment_bottomsheet_batch_config, container, false
        );

        activity = (ScanBatchActivity) getActivity();
        assert activity != null;

        if(getArguments() == null
                || getArguments().getString(Constants.ARGUMENT.TYPE) == null
        ) {
            dismissWithMessage(activity.getString(R.string.msg_error));
            return view;
        }

        String batchType = getArguments().getString(Constants.ARGUMENT.TYPE);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        // BEST BEFORE DATE
        textViewBestBeforeDate = view.findViewById(R.id.text_batch_config_bbd);
        LinearLayout linearLayoutBestBeforeDate = view.findViewById(R.id.linear_batch_config_bbd);
        assert batchType != null;
        if(batchType.equals(Constants.ACTION.PURCHASE)) {
            linearLayoutBestBeforeDate.setOnClickListener(v -> {
                int status = getIntStatusFromPref(Constants.PREF.BATCH_CONFIG_BBD);
                if(status < 2) status++;
                else status = 0;
                setPrefToStatus(Constants.PREF.BATCH_CONFIG_BBD, status);
                textViewBestBeforeDate.animate().alpha(0).withEndAction(() -> {
                    textViewBestBeforeDate.setText(
                            getTextStatusFromPref(Constants.PREF.BATCH_CONFIG_BBD)
                    );
                    textViewBestBeforeDate.animate().alpha(1).setDuration(150).start();
                }).setDuration(150).start();
            });
            textViewBestBeforeDate.setText(getTextStatusFromPref(Constants.PREF.BATCH_CONFIG_BBD));
        } else {
            linearLayoutBestBeforeDate.setVisibility(View.GONE);
        }

        // PRICE
        textViewPrice = view.findViewById(R.id.text_batch_config_price);
        LinearLayout linearLayoutPrice = view.findViewById(R.id.linear_batch_config_price);
        if(batchType.equals(Constants.ACTION.PURCHASE)) {
            linearLayoutPrice.setOnClickListener(v -> {
                int status = getIntStatusFromPref(Constants.PREF.BATCH_CONFIG_PRICE);
                if(status < 2) status++;
                else status = 0;
                setPrefToStatus(Constants.PREF.BATCH_CONFIG_PRICE, status);
                textViewPrice.animate().alpha(0).withEndAction(() -> {
                    textViewPrice.setText(
                            getTextStatusFromPref(Constants.PREF.BATCH_CONFIG_PRICE)
                    );
                    textViewPrice.animate().alpha(1).setDuration(150).start();
                }).setDuration(150).start();
            });
            textViewPrice.setText(getTextStatusFromPref(Constants.PREF.BATCH_CONFIG_PRICE));
        } else {
            linearLayoutPrice.setVisibility(View.GONE);
        }

        // STORE
        textViewStore = view.findViewById(R.id.text_batch_config_store);
        LinearLayout linearLayoutStore = view.findViewById(R.id.linear_batch_config_store);
        if(batchType.equals(Constants.ACTION.PURCHASE)) {
            linearLayoutStore.setOnClickListener(v -> {
                int status = getIntStatusFromPref(Constants.PREF.BATCH_CONFIG_STORE);
                if(status < 2) status++;
                else status = 0;
                setPrefToStatus(Constants.PREF.BATCH_CONFIG_STORE, status);
                textViewStore.animate().alpha(0).withEndAction(() -> {
                    textViewStore.setText(
                            getTextStatusFromPref(Constants.PREF.BATCH_CONFIG_STORE)
                    );
                    textViewStore.animate().alpha(1).setDuration(150).start();
                }).setDuration(150).start();
            });
            textViewStore.setText(getTextStatusFromPref(Constants.PREF.BATCH_CONFIG_STORE));
        } else {
            linearLayoutStore.setVisibility(View.GONE);
        }

        // LOCATION
        textViewLocation = view.findViewById(R.id.text_batch_config_location);
        LinearLayout linearLayoutLocation = view.findViewById(R.id.linear_batch_config_location);
        if(batchType.equals(Constants.ACTION.PURCHASE)) {
            linearLayoutLocation.setOnClickListener(v -> {
                int status = getIntStatusFromPref(Constants.PREF.BATCH_CONFIG_LOCATION);
                if(status < 2) status++;
                else status = 0;
                setPrefToStatus(Constants.PREF.BATCH_CONFIG_LOCATION, status);
                textViewLocation.animate().alpha(0).withEndAction(() -> {
                    textViewLocation.setText(
                            getTextStatusFromPref(Constants.PREF.BATCH_CONFIG_LOCATION)
                    );
                    textViewLocation.animate().alpha(1).setDuration(150).start();
                }).setDuration(150).start();
            });
            textViewLocation.setText(getTextStatusFromPref(Constants.PREF.BATCH_CONFIG_LOCATION));
        } else {
            linearLayoutLocation.setVisibility(View.GONE);
        }

        // STOCK LOCATION
        textViewStockLocation = view.findViewById(R.id.text_batch_config_stock_location);
        LinearLayout linearLayoutStockLocation = view.findViewById(
                R.id.linear_batch_config_stock_location
        );
        if(batchType.equals(Constants.ACTION.CONSUME)) {
            linearLayoutStockLocation.setOnClickListener(v -> {
                int status = getIntStatusFromPref(Constants.PREF.BATCH_CONFIG_STOCK_LOCATION);
                if(status == 0) status = 2;
                else status = 0;
                setPrefToStatus(Constants.PREF.BATCH_CONFIG_STOCK_LOCATION, status);
                textViewStockLocation.animate().alpha(0).withEndAction(() -> {
                    textViewStockLocation.setText(
                            getTextStatusFromPref(Constants.PREF.BATCH_CONFIG_STOCK_LOCATION)
                    );
                    textViewStockLocation.animate().alpha(1).setDuration(150).start();
                }).setDuration(150).start();
            });
            textViewStockLocation.setText(
                    getTextStatusFromPref(Constants.PREF.BATCH_CONFIG_STOCK_LOCATION)
            );
        } else {
            linearLayoutStockLocation.setVisibility(View.GONE);
        }

        // SPECIFIC
        textViewSpecific = view.findViewById(R.id.text_batch_config_specific);
        LinearLayout linearLayoutSpecific = view.findViewById(R.id.linear_batch_config_specific);
        if(batchType.equals(Constants.ACTION.CONSUME)) {
            linearLayoutSpecific.setOnClickListener(v -> {
                int status = getIntStatusFromPref(Constants.PREF.BATCH_CONFIG_SPECIFIC);
                if(status == 0) status = 2;
                else status = 0;
                setPrefToStatus(Constants.PREF.BATCH_CONFIG_SPECIFIC, status);
                textViewSpecific.animate().alpha(0).withEndAction(() -> {
                    textViewSpecific.setText(
                            getTextStatusFromPref(Constants.PREF.BATCH_CONFIG_SPECIFIC)
                    );
                    textViewSpecific.animate().alpha(1).setDuration(150).start();
                }).setDuration(150).start();
            });
            textViewSpecific.setText(
                    getTextStatusFromPref(Constants.PREF.BATCH_CONFIG_SPECIFIC)
            );
        } else {
            linearLayoutSpecific.setVisibility(View.GONE);
        }

        hideDisabledFeatures(view);

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        activity.resumeScan();
    }

    private void hideDisabledFeatures(View view) {
        if(isFeatureDisabled(Constants.PREF.FEATURE_STOCK_PRICE_TRACKING)) {
            view.findViewById(R.id.linear_batch_config_price).setVisibility(View.GONE);
        }
        if(isFeatureDisabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
            view.findViewById(R.id.linear_batch_config_location).setVisibility(View.GONE);
            view.findViewById(R.id.linear_batch_config_stock_location).setVisibility(View.GONE);
        }
        if(isFeatureDisabled(Constants.PREF.FEATURE_STOCK_BBD_TRACKING)) {
            view.findViewById(R.id.linear_batch_config_bbd).setVisibility(View.GONE);
        }
    }

    private int getIntStatusFromPref(String pref) {
        return sharedPrefs.getInt(pref, 0);
    }

    private void setPrefToStatus(String pref, int status) {
        sharedPrefs.edit().putInt(pref, status).apply();
    }

    private String getTextStatusFromPref(String pref) {
        int status = sharedPrefs.getInt(pref, 0);
        if(status == 1) {
            return "First time in session";
        } else if(status == 2) {
            return "Always";
        }
        return "Never";
    }

    private void dismissWithMessage(String msg) {
        Snackbar.make(
                activity.findViewById(R.id.barcode_scan_batch),
                msg,
                Snackbar.LENGTH_SHORT
        ).show();
        dismiss();
    }

    private boolean isFeatureDisabled(String pref) {
        if(pref == null) return false;
        return !sharedPrefs.getBoolean(pref, true);
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
