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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;

public class MasterDataBottomSheetDialogFragment
        extends BottomSheetDialogFragment implements View.OnClickListener {

    private final static String TAG = "MasterDataBottomSheet";

    private MainActivity activity;
    private View view;
    private SharedPreferences sharedPrefs;
    private String uiMode;
    private ClickUtil clickUtil = new ClickUtil();

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
        view = inflater.inflate(
                R.layout.fragment_bottomsheet_master_data, container, false
        );

        activity = (MainActivity) getActivity();
        Bundle bundle = getArguments();
        assert activity != null && bundle != null;

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        uiMode = bundle.getString(Constants.ARGUMENT.UI_MODE, Constants.UI.STOCK_DEFAULT);

        setOnClickListeners(
                R.id.linear_master_data_products,
                R.id.linear_master_data_locations,
                R.id.linear_master_data_stores,
                R.id.linear_master_data_quantity_units,
                R.id.linear_master_data_product_groups
        );

        if(uiMode.startsWith(Constants.UI.MASTER_PRODUCTS)) {
            select(R.id.linear_master_data_products, R.id.text_master_data_products);
        } else if(uiMode.startsWith(Constants.UI.MASTER_LOCATIONS)) {
            select(R.id.linear_master_data_locations, R.id.text_master_data_locations);
        } else if(uiMode.startsWith(Constants.UI.MASTER_STORES)) {
            select(R.id.linear_master_data_stores, R.id.text_master_data_stores);
        } else if(uiMode.startsWith(Constants.UI.MASTER_QUANTITY_UNITS)) {
            select(R.id.linear_master_data_quantity_units, R.id.text_master_data_quantity_units);
        } else if(uiMode.startsWith(Constants.UI.MASTER_PRODUCT_GROUPS)) {
            select(R.id.linear_master_data_product_groups, R.id.text_master_data_product_groups);
        }

        hideDisabledFeatures();

        return view;
    }

    private void setOnClickListeners(@IdRes int... viewIds) {
        for (int viewId : viewIds) {
            view.findViewById(viewId).setOnClickListener(this);
        }
    }

    public void onClick(View v) {
        if(clickUtil.isDisabled()) return;

        switch(v.getId()) {
            case R.id.linear_master_data_products:
                if(!uiMode.startsWith(Constants.UI.MASTER_PRODUCTS)) {
                    replaceFragment(Constants.UI.MASTER_PRODUCTS);
                }
                break;
            case R.id.linear_master_data_locations:
                if(!uiMode.startsWith(Constants.UI.MASTER_LOCATIONS)) {
                    replaceFragment(Constants.UI.MASTER_LOCATIONS);
                }
                break;
            case R.id.linear_master_data_stores:
                if(!uiMode.startsWith(Constants.UI.MASTER_STORES)) {
                    replaceFragment(Constants.UI.MASTER_STORES);
                }
                break;
            case R.id.linear_master_data_quantity_units:
                if(!uiMode.startsWith(Constants.UI.MASTER_QUANTITY_UNITS)) {
                    replaceFragment(Constants.UI.MASTER_QUANTITY_UNITS);
                }
                break;
            case R.id.linear_master_data_product_groups:
                if(!uiMode.startsWith(Constants.UI.MASTER_PRODUCT_GROUPS)) {
                    replaceFragment(Constants.UI.MASTER_PRODUCT_GROUPS);
                }
                break;
        }
    }

    private void select(@IdRes int linearLayoutId, @IdRes int textViewId) {
        view.findViewById(linearLayoutId).setBackgroundResource(R.drawable.bg_drawer_item_selected);
        ((TextView) view.findViewById(textViewId)).setTextColor(
                ContextCompat.getColor(activity, R.color.secondary)
        );
    }

    private void replaceFragment(String fragmentNew) {
        activity.replaceAll(fragmentNew, null, true);
        dismiss();
    }

    private void hideDisabledFeatures() {
        if(!isFeatureEnabled(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
            view.findViewById(R.id.linear_master_data_locations).setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private boolean isFeatureEnabled(String pref) {
        if(pref == null) return true;
        return sharedPrefs.getBoolean(pref, true);
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
