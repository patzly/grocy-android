/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavDirections;
import androidx.navigation.NavOptions;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetDrawerBinding;
import xyz.zedler.patrick.grocy.fragment.BaseFragment;
import xyz.zedler.patrick.grocy.fragment.ConsumeFragment;
import xyz.zedler.patrick.grocy.fragment.MasterObjectListFragment;
import xyz.zedler.patrick.grocy.fragment.OverviewStartFragment;
import xyz.zedler.patrick.grocy.fragment.PurchaseFragment;
import xyz.zedler.patrick.grocy.fragment.SettingsFragment;
import xyz.zedler.patrick.grocy.fragment.ShoppingListFragment;
import xyz.zedler.patrick.grocy.fragment.StockOverviewFragment;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.NetUtil;

public class DrawerBottomSheet extends BaseBottomSheet implements View.OnClickListener {

    private final static String TAG = DrawerBottomSheet.class.getSimpleName();

    private FragmentBottomsheetDrawerBinding binding;
    private MainActivity activity;
    private SharedPreferences sharedPrefs;
    private final ClickUtil clickUtil = new ClickUtil();

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
        binding = FragmentBottomsheetDrawerBinding.inflate(
                inflater, container, false
        );

        activity = (MainActivity) requireActivity();

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

        binding.buttonDrawerShoppingMode.setOnClickListener(
                v -> navigateDeepLink(getString(R.string.deep_link_shoppingModeFragment))
        );

        ClickUtil.setOnClickListeners(
                this,
                binding.linearDrawerStock,
                binding.linearDrawerShoppingList,
                binding.linearDrawerConsume,
                binding.linearDrawerPurchase,
                binding.linearDrawerMasterData,
                binding.linearDrawerSettings,
                binding.linearDrawerFeedback,
                binding.linearDrawerHelp
        );

        binding.linearDrawerSettings.setOnLongClickListener(v -> {
            activity.getCurrentFragment().navigate(R.id.settingsActivity);
            dismiss();
            return true;
        });

        BaseFragment currentFragment = activity.getCurrentFragment();
        if (currentFragment instanceof StockOverviewFragment) {
            select(binding.linearDrawerStock, binding.textDrawerStock);
        } else if (currentFragment instanceof ShoppingListFragment) {
            select(binding.linearDrawerShoppingList, binding.textDrawerShoppingList);
        } else if (currentFragment instanceof ConsumeFragment) {
            select(binding.linearDrawerConsume, binding.textDrawerConsume);
        } else if (currentFragment instanceof PurchaseFragment) {
            select(binding.linearDrawerPurchase, binding.textDrawerPurchase);
        } else if (currentFragment instanceof MasterObjectListFragment) {
            select(binding.linearDrawerMasterData, binding.textDrawerMasterData);
        } else if (currentFragment instanceof SettingsFragment) {
            select(binding.linearDrawerSettings, binding.textDrawerSettings);
        }

        hideDisabledFeatures();

        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    public void onClick(View v) {
        if (clickUtil.isDisabled()) return;

        if (v.getId() == R.id.linear_drawer_stock) {
            navigateCustom(DrawerBottomSheetDirections
                    .actionDrawerBottomSheetDialogFragmentToStockOverviewFragment());
        } else if (v.getId() == R.id.linear_drawer_shopping_list) {
            navigateCustom(DrawerBottomSheetDirections
                    .actionDrawerBottomSheetDialogFragmentToShoppingListFragment());
        } else if (v.getId() == R.id.linear_drawer_consume) {
            navigateCustom(DrawerBottomSheetDirections
                    .actionDrawerBottomSheetDialogFragmentToConsumeFragment());
        } else if (v.getId() == R.id.linear_drawer_purchase) {
            navigateCustom(DrawerBottomSheetDirections
                    .actionDrawerBottomSheetDialogFragmentToPurchaseFragment());
        } else if (v.getId() == R.id.linear_drawer_master_data) {
            navigateCustom(DrawerBottomSheetDirections
                    .actionDrawerBottomSheetDialogFragmentToNavigationMasterObjects());
        } else if (v.getId() == R.id.linear_drawer_settings) {
            navigateCustom(DrawerBottomSheetDirections
                    .actionDrawerBottomSheetDialogFragmentToSettingsFragment());
        } else if (v.getId() == R.id.linear_drawer_feedback) {
            activity.showBottomSheet(new FeedbackBottomSheet());
            dismiss();
        } else if (v.getId() == R.id.linear_drawer_help) {
            if (!NetUtil.openURL(activity, Constants.URL.HELP)) {
                activity.showMessage(R.string.error_no_browser);
            }
            dismiss();
        }
    }

    private void navigateCustom(NavDirections directions) {
        NavOptions.Builder builder = new NavOptions.Builder();
        builder.setEnterAnim(R.anim.slide_in_up).setPopExitAnim(R.anim.slide_out_down);
        builder.setPopUpTo(R.id.overviewStartFragment, false);
        if (!(activity.getCurrentFragment() instanceof OverviewStartFragment)) {
            builder.setExitAnim(R.anim.slide_out_down);
        } else {
            builder.setExitAnim(R.anim.slide_no);
        }
        dismiss();
        navigate(directions, builder.build());
    }

    @Override
    void navigateDeepLink(@NonNull String uri) {
        NavOptions.Builder builder = new NavOptions.Builder();
        builder.setEnterAnim(R.anim.slide_in_up).setPopExitAnim(R.anim.slide_out_down);
        builder.setPopUpTo(R.id.overviewStartFragment, false);
        if (!(activity.getCurrentFragment() instanceof OverviewStartFragment)) {
            builder.setExitAnim(R.anim.slide_out_down);
        } else {
            builder.setExitAnim(R.anim.slide_no);
        }
        dismiss();
        findNavController().navigate(Uri.parse(uri), builder.build());
    }

    private void select(LinearLayout linearLayout, TextView textView) {
        linearLayout.setBackgroundResource(R.drawable.bg_drawer_item_selected);
        linearLayout.setClickable(false);
        textView.setTextColor(ContextCompat.getColor(activity, R.color.retro_green_fg));
    }

    private void hideDisabledFeatures() {
        if (!isFeatureEnabled(Constants.PREF.FEATURE_SHOPPING_LIST)) {
            binding.linearDrawerShoppingList.setVisibility(View.GONE);
            binding.dividerDrawerShoppingList.setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private boolean isFeatureEnabled(String pref) {
        if (pref == null) return true;
        return sharedPrefs.getBoolean(pref, true);
    }

    @NonNull
    @Override
    public String toString() {
        return TAG;
    }
}
