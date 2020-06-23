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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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

import xyz.zedler.patrick.grocy.HelpActivity;
import xyz.zedler.patrick.grocy.MainActivity;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.ScanBatchActivity;
import xyz.zedler.patrick.grocy.SettingsActivity;
import xyz.zedler.patrick.grocy.ShoppingActivity;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.IconUtil;

public class DrawerBottomSheetDialogFragment
        extends BottomSheetDialogFragment implements View.OnClickListener {

    private final static String TAG = "DrawerBottomSheet";

    private MainActivity activity;
    private View view;
    private SharedPreferences sharedPrefs;
    private String uiMode;
    private ClickUtil clickUtil = new ClickUtil();

    private boolean debug;

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
                R.layout.fragment_bottomsheet_drawer, container, false
        );

        activity = (MainActivity) getActivity();
        Bundle bundle = getArguments();
        assert activity != null && bundle != null;

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        debug = sharedPrefs.getBoolean(Constants.PREF.DEBUG, false);

        uiMode = bundle.getString(Constants.ARGUMENT.UI_MODE, Constants.UI.STOCK_DEFAULT);
        if(debug) Log.i(TAG, "onCreateView: uiMode = " + uiMode);

        view.findViewById(R.id.button_drawer_shopping_mode).setOnClickListener(v -> {
            startActivity(new Intent(activity, ShoppingActivity.class));
            new Handler().postDelayed(this::dismiss, 500);
        });

        view.findViewById(R.id.button_drawer_batch_consume).setOnClickListener(v -> {
            Intent intent = new Intent(activity, ScanBatchActivity.class);
            intent.putExtra(Constants.ARGUMENT.TYPE, Constants.ACTION.CONSUME);
            activity.startActivityForResult(intent, Constants.REQUEST.SCAN_BATCH);
            new Handler().postDelayed(this::dismiss, 500);
        });

        view.findViewById(R.id.button_drawer_batch_purchase).setOnClickListener(v -> {
            Intent intent = new Intent(activity, ScanBatchActivity.class);
            intent.putExtra(Constants.ARGUMENT.TYPE, Constants.ACTION.PURCHASE);
            activity.startActivityForResult(intent, Constants.REQUEST.SCAN_BATCH);
            new Handler().postDelayed(this::dismiss, 500);
        });

        setOnClickListeners(
                R.id.linear_drawer_shopping_list,
                R.id.linear_drawer_consume,
                R.id.linear_drawer_purchase,
                R.id.linear_drawer_master_data,
                R.id.linear_settings,
                R.id.linear_feedback,
                R.id.linear_help
        );

        if(uiMode.startsWith(Constants.UI.SHOPPING_LIST)) {
            select(R.id.linear_drawer_shopping_list, R.id.text_drawer_shopping_list);
        } else if(uiMode.startsWith(Constants.UI.CONSUME)) {
            select(R.id.linear_drawer_consume, R.id.text_drawer_consume);
        } else if(uiMode.startsWith(Constants.UI.PURCHASE)) {
            select(R.id.linear_drawer_purchase, R.id.text_drawer_purchase);
        } else if(uiMode.startsWith(Constants.UI.MASTER)) {
            select(R.id.linear_drawer_master_data, R.id.text_drawer_master_data);
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
            case R.id.linear_drawer_shopping_list:
                if(!uiMode.startsWith(Constants.UI.SHOPPING_LIST)) {
                    replaceFragment(Constants.UI.SHOPPING_LIST);
                }
                break;
            case R.id.linear_drawer_consume:
                if(!uiMode.startsWith(Constants.UI.CONSUME)) {
                    replaceFragment(Constants.UI.CONSUME);
                }
                break;
            case R.id.linear_drawer_purchase:
                if(!uiMode.startsWith(Constants.UI.PURCHASE)) {
                    replaceFragment(Constants.UI.PURCHASE);
                }
                break;
            case R.id.linear_drawer_master_data:
                dismiss();
                Bundle bundle = new Bundle();
                // selection for master data sheet
                bundle.putString(Constants.ARGUMENT.UI_MODE, uiMode);
                activity.showBottomSheet(new MasterDataBottomSheetDialogFragment(), bundle);
                break;
            case R.id.linear_settings:
                IconUtil.start(view, R.id.image_settings);
                new Handler().postDelayed(() -> {
                    dismiss();
                    startActivity(new Intent(activity, SettingsActivity.class));
                }, 300);
                break;
            case R.id.linear_feedback:
                dismiss();
                activity.showBottomSheet(new FeedbackBottomSheetDialogFragment(), null);
                break;
            case R.id.linear_help:
                IconUtil.start(view, R.id.image_help);
                new Handler().postDelayed(() -> {
                    dismiss();
                    startActivity(new Intent(activity, HelpActivity.class));
                }, 300);
                break;
        }
    }

    private void replaceFragment(String fragmentNew) {
        activity.replaceAll(fragmentNew, null, true);
        dismiss();
    }

    private void select(@IdRes int linearLayoutId, @IdRes int textViewId) {
        view.findViewById(linearLayoutId).setBackgroundResource(R.drawable.bg_drawer_item_selected);
        ((TextView) view.findViewById(textViewId)).setTextColor(
                ContextCompat.getColor(activity, R.color.retro_green_fg)
        );
    }

    private void hideDisabledFeatures() {
        if(!isFeatureEnabled(Constants.PREF.FEATURE_SHOPPING_LIST)) {
            view.findViewById(R.id.linear_drawer_shopping_list).setVisibility(View.GONE);
            view.findViewById(R.id.divider_drawer_shopping_list).setVisibility(View.GONE);
        }
    }

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
