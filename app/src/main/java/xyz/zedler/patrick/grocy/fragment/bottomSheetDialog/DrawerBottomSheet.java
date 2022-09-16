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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavDirections;
import androidx.navigation.NavOptions;
import androidx.preference.PreferenceManager;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetDrawerBinding;
import xyz.zedler.patrick.grocy.fragment.BaseFragment;
import xyz.zedler.patrick.grocy.fragment.ChoresFragment;
import xyz.zedler.patrick.grocy.fragment.ConsumeFragment;
import xyz.zedler.patrick.grocy.fragment.InventoryFragment;
import xyz.zedler.patrick.grocy.fragment.MasterObjectListFragment;
import xyz.zedler.patrick.grocy.fragment.OverviewStartFragment;
import xyz.zedler.patrick.grocy.fragment.PurchaseFragment;
import xyz.zedler.patrick.grocy.fragment.RecipesFragment;
import xyz.zedler.patrick.grocy.fragment.SettingsFragment;
import xyz.zedler.patrick.grocy.fragment.ShoppingListFragment;
import xyz.zedler.patrick.grocy.fragment.StockOverviewFragment;
import xyz.zedler.patrick.grocy.fragment.TasksFragment;
import xyz.zedler.patrick.grocy.fragment.TransferFragment;
import xyz.zedler.patrick.grocy.util.ClickUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.Constants.PREF;
import xyz.zedler.patrick.grocy.util.NetUtil;

public class DrawerBottomSheet extends BaseBottomSheetDialogFragment implements View.OnClickListener {

  private final static String TAG = DrawerBottomSheet.class.getSimpleName();

  private FragmentBottomsheetDrawerBinding binding;
  private MainActivity activity;
  private SharedPreferences sharedPrefs;
  private final ClickUtil clickUtil = new ClickUtil();

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
        v -> navigateDeepLink(R.string.deep_link_shoppingModeFragment)
    );

    ClickUtil.setOnClickListeners(
        this,
        binding.linearDrawerStock,
        binding.linearDrawerShoppingList,
        binding.linearDrawerConsume,
        binding.linearDrawerPurchase,
        binding.linearDrawerTransfer,
        binding.linearDrawerInventory,
        binding.linearDrawerChores,
        binding.linearDrawerTasks,
        binding.linearDrawerRecipes,
        binding.linearDrawerMasterData,
        binding.linearDrawerSettings,
        binding.linearDrawerFeedback,
        binding.linearDrawerHelp
    );

    BaseFragment currentFragment = activity.getCurrentFragment();
    if (currentFragment instanceof StockOverviewFragment) {
      select(binding.linearDrawerStock, binding.textDrawerStock, false);
    } else if (currentFragment instanceof ShoppingListFragment) {
      select(binding.linearDrawerShoppingList, binding.textDrawerShoppingList, false);
    } else if (currentFragment instanceof ConsumeFragment) {
      select(binding.linearDrawerConsume, null, true);
    } else if (currentFragment instanceof PurchaseFragment) {
      select(binding.linearDrawerPurchase, null, true);
    } else if (currentFragment instanceof TransferFragment) {
      select(binding.linearDrawerTransfer, null, true);
    } else if (currentFragment instanceof InventoryFragment) {
      select(binding.linearDrawerInventory, null, true);
    } else if (currentFragment instanceof ChoresFragment) {
      select(binding.linearDrawerChores, binding.textDrawerChores, false);
    } else if (currentFragment instanceof TasksFragment) {
      select(binding.linearDrawerTasks, binding.textDrawerTasks, false);
    } else if (currentFragment instanceof RecipesFragment) {
      select(binding.linearDrawerRecipes, binding.textDrawerRecipes, false);
    } else if (currentFragment instanceof MasterObjectListFragment) {
      select(binding.linearDrawerMasterData, binding.textDrawerMasterData, false);
    } else if (currentFragment instanceof SettingsFragment) {
      select(binding.linearDrawerSettings, binding.textDrawerSettings, false);
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
    if (clickUtil.isDisabled()) {
      return;
    }

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
    } else if (v.getId() == R.id.linear_drawer_transfer) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToTransferFragment());
    } else if (v.getId() == R.id.linear_drawer_inventory) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToInventoryFragment());
    } else if (v.getId() == R.id.linear_drawer_chores) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToChoresFragment());
    } else if (v.getId() == R.id.linear_drawer_tasks) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToTasksFragment());
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
        activity.showSnackbar(R.string.error_no_browser);
      }
      dismiss();
    } else if (v.getId() == R.id.linear_drawer_recipes) {
      navigateCustom(DrawerBottomSheetDirections
              .actionDrawerBottomSheetDialogFragmentToRecipesFragment());
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
  public void navigateDeepLink(@StringRes int uri) {
    NavOptions.Builder builder = new NavOptions.Builder();
    builder.setEnterAnim(R.anim.slide_in_up).setPopExitAnim(R.anim.slide_out_down);
    builder.setPopUpTo(R.id.overviewStartFragment, false);
    if (!(activity.getCurrentFragment() instanceof OverviewStartFragment)) {
      builder.setExitAnim(R.anim.slide_out_down);
    } else {
      builder.setExitAnim(R.anim.slide_no);
    }
    dismiss();
    findNavController().navigate(Uri.parse(getString(uri)), builder.build());
  }

  private void select(LinearLayout linearLayout, TextView textView, boolean multiRowItem) {
    linearLayout.setBackgroundResource(
        multiRowItem
            ? R.drawable.bg_drawer_item_multirow_selected
            : R.drawable.bg_drawer_item_selected
    );
    linearLayout.setClickable(false);
    if (textView != null) {
      textView.setTextColor(ContextCompat.getColor(activity, R.color.retro_green_fg));
    }
  }

  private void hideDisabledFeatures() {
    if (!isFeatureEnabled(Constants.PREF.FEATURE_SHOPPING_LIST)) {
      binding.frameShoppingList.setVisibility(View.GONE);
    }
    if (!isFeatureEnabled(PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
      binding.linearDrawerTransfer.setVisibility(View.GONE);
      binding.transactionsContainer.setWeightSum(75f);
    }
    if (!isFeatureEnabled(PREF.FEATURE_RECIPES)) {
      binding.containerRecipes.setVisibility(View.GONE);
    }
    if (!isFeatureEnabled(PREF.FEATURE_RECIPES)) {
      binding.linearDrawerRecipes.setVisibility(View.GONE);
    }
    if (!isFeatureEnabled(PREF.FEATURE_TASKS) && !isFeatureEnabled(PREF.FEATURE_CHORES)) {
      binding.containerTasks.setVisibility(View.GONE);
    }
    if (!isFeatureEnabled(PREF.FEATURE_TASKS)) {
      binding.linearDrawerTasks.setVisibility(View.GONE);
    }
    if (!isFeatureEnabled(PREF.FEATURE_CHORES)) {
      binding.linearDrawerChores.setVisibility(View.GONE);
    }
  }

  private boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return sharedPrefs.getBoolean(pref, true);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
