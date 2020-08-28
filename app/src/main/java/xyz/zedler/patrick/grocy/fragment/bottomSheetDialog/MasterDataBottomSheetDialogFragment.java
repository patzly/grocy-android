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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.fragment.MasterLocationsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterProductGroupsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterProductsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterQuantityUnitsFragment;
import xyz.zedler.patrick.grocy.fragment.MasterStoresFragment;
import xyz.zedler.patrick.grocy.fragment.StockFragment;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;

public class MasterDataBottomSheetDialogFragment
        extends CustomBottomSheetDialogFragment implements View.OnClickListener {

    private final static String TAG = "MasterDataBottomSheet";

    private MainActivity activity;
    private View view;
    private SharedPreferences sharedPrefs;
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

        setOnClickListeners(
                R.id.linear_master_data_products,
                R.id.linear_master_data_locations,
                R.id.linear_master_data_stores,
                R.id.linear_master_data_quantity_units,
                R.id.linear_master_data_product_groups
        );

        Fragment currentFragment = activity.getCurrentFragment();

        if(currentFragment instanceof MasterProductsFragment) {
            select(R.id.linear_master_data_products, R.id.text_master_data_products);
        } else if(currentFragment instanceof MasterLocationsFragment) {
            select(R.id.linear_master_data_locations, R.id.text_master_data_locations);
        } else if(currentFragment instanceof MasterStoresFragment) {
            select(R.id.linear_master_data_stores, R.id.text_master_data_stores);
        } else if(currentFragment instanceof MasterQuantityUnitsFragment) {
            select(R.id.linear_master_data_quantity_units, R.id.text_master_data_quantity_units);
        } else if(currentFragment instanceof MasterProductGroupsFragment) {
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
                navigate(MasterDataBottomSheetDialogFragmentDirections
                        .actionMasterDataBottomSheetDialogFragmentToMasterProductsFragment());
                break;
            case R.id.linear_master_data_locations:
                navigate(MasterDataBottomSheetDialogFragmentDirections
                        .actionMasterDataBottomSheetDialogFragmentToMasterLocationsFragment());
                break;
            case R.id.linear_master_data_stores:
                navigate(MasterDataBottomSheetDialogFragmentDirections
                        .actionMasterDataBottomSheetDialogFragmentToMasterStoresFragment());
                break;
            case R.id.linear_master_data_quantity_units:
                navigate(MasterDataBottomSheetDialogFragmentDirections
                        .actionMasterDataBottomSheetDialogFragmentToMasterQuantityUnitsFragment());
                break;
            case R.id.linear_master_data_product_groups:
                navigate(MasterDataBottomSheetDialogFragmentDirections
                        .actionMasterDataBottomSheetDialogFragmentToMasterProductGroupsFragment());
                break;
        }
    }

    private void navigate(NavDirections navDirections) {
        NavOptions.Builder builder = new NavOptions.Builder();
        builder.setEnterAnim(R.anim.slide_in_up).setPopExitAnim(R.anim.slide_out_down);
        builder.setPopUpTo(R.id.stockFragment, false);
        if(! (activity.getCurrentFragment() instanceof StockFragment)) {
            builder.setExitAnim(R.anim.slide_out_down);
        }
        NavHostFragment.findNavController(this).navigate(
                navDirections,
                builder.build()
        );
        dismiss();
    }

    private void select(@IdRes int linearLayoutId, @IdRes int textViewId) {
        LinearLayout linearLayout = view.findViewById(linearLayoutId);
        linearLayout.setBackgroundResource(R.drawable.bg_drawer_item_selected);
        linearLayout.setClickable(false);
        ((TextView) view.findViewById(textViewId)).setTextColor(
                ContextCompat.getColor(activity, R.color.secondary)
        );
    }

    private void hideDisabledFeatures() {
        if(!sharedPrefs.getBoolean(Constants.PREF.FEATURE_STOCK_LOCATION_TRACKING, true)) {
            view.findViewById(R.id.linear_master_data_locations).setVisibility(View.GONE);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
