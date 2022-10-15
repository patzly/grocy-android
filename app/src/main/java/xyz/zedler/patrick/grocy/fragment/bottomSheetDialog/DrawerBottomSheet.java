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
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.TooltipCompat;
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
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.util.NetUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

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

    ViewUtil.setOnlyOverScrollStretchEnabled(binding.scrollDrawer);

    binding.linearDrawerStock.setBackground(
        ViewUtil.getRippleBgListItemSurface(requireContext())
    );
    binding.linearDrawerShoppingList.setBackground(
        ViewUtil.getRippleBgListItemSurface(requireContext())
    );
    binding.linearDrawerConsume.setBackground(
        ViewUtil.getRippleBgListItemSurface(requireContext(), 8, 4)
    );
    binding.linearDrawerPurchase.setBackground(
        ViewUtil.getRippleBgListItemSurface(requireContext(), 4, 4)
    );
    binding.linearDrawerTransfer.setBackground(
        ViewUtil.getRippleBgListItemSurface(requireContext(), 4, 4)
    );
    binding.linearDrawerInventory.setBackground(
        ViewUtil.getRippleBgListItemSurface(requireContext(), 4, 8)
    );
    binding.linearDrawerRecipes.setBackground(
        ViewUtil.getRippleBgListItemSurface(requireContext())
    );
    binding.linearDrawerChores.setBackground(
        ViewUtil.getRippleBgListItemSurface(requireContext())
    );
    binding.linearDrawerTasks.setBackground(
        ViewUtil.getRippleBgListItemSurface(requireContext())
    );
    binding.linearDrawerMasterData.setBackground(
        ViewUtil.getRippleBgListItemSurface(requireContext())
    );
    binding.linearDrawerSettings.setBackground(
        ViewUtil.getRippleBgListItemSurface(requireContext())
    );
    binding.linearDrawerFeedback.setBackground(
        ViewUtil.getRippleBgListItemSurface(requireContext())
    );
    binding.linearDrawerHelp.setBackground(
        ViewUtil.getRippleBgListItemSurface(requireContext())
    );

    binding.buttonDrawerShoppingMode.setOnClickListener(
        v -> navigateDeepLink(R.string.deep_link_shoppingModeFragment)
    );
    TooltipCompat.setTooltipText(
        binding.buttonDrawerShoppingMode, getString(R.string.title_shopping_mode)
    );
    TooltipCompat.setTooltipText(
        binding.linearDrawerConsume, getString(R.string.title_consume)
    );
    TooltipCompat.setTooltipText(
        binding.linearDrawerPurchase, getString(R.string.title_purchase)
    );
    TooltipCompat.setTooltipText(
        binding.linearDrawerTransfer, getString(R.string.title_transfer)
    );
    TooltipCompat.setTooltipText(
        binding.linearDrawerInventory, getString(R.string.title_inventory)
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
        binding.linearDrawerMealPlan,
        binding.linearDrawerMasterData,
        binding.linearDrawerSettings,
        binding.linearDrawerFeedback,
        binding.linearDrawerHelp
    );

    BaseFragment currentFragment = activity.getCurrentFragment();
    if (currentFragment instanceof StockOverviewFragment) {
      select(binding.linearDrawerStock, binding.textDrawerStock, binding.imageDrawerStock);
    } else if (currentFragment instanceof ShoppingListFragment) {
      select(
          binding.linearDrawerShoppingList,
          binding.textDrawerShoppingList,
          binding.imageDrawerShoppingList
      );
    } else if (currentFragment instanceof ConsumeFragment) {
      select(
          binding.linearDrawerConsume,
          null,
          binding.imageDrawerConsume,
          8, 4
      );
    } else if (currentFragment instanceof PurchaseFragment) {
      select(
          binding.linearDrawerPurchase,
          null,
          binding.imageDrawerPurchase,
          4, 4
      );
    } else if (currentFragment instanceof TransferFragment) {
      select(
          binding.linearDrawerTransfer,
          null,
          binding.imageDrawerTransfer,
          4, 4
      );
    } else if (currentFragment instanceof InventoryFragment) {
      select(
          binding.linearDrawerInventory,
          null,
          binding.imageDrawerInventory,
          4, 8
      );
    } else if (currentFragment instanceof ChoresFragment) {
      select(binding.linearDrawerChores, binding.textDrawerChores, binding.imageDrawerChores);
    } else if (currentFragment instanceof TasksFragment) {
      select(binding.linearDrawerTasks, binding.textDrawerTasks, binding.imageDrawerTasks);
    } else if (currentFragment instanceof RecipesFragment) {
      select(binding.linearDrawerRecipes, binding.textDrawerRecipes, binding.imageDrawerRecipes);
    } else if (currentFragment instanceof MasterObjectListFragment) {
      select(
          binding.linearDrawerMasterData,
          binding.textDrawerMasterData,
          binding.imageDrawerMasterData
      );
    } else if (currentFragment instanceof SettingsFragment) {
      select(binding.linearDrawerSettings, binding.textDrawerSettings, binding.imageDrawerSettings);
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
    } else if (v.getId() == R.id.linear_drawer_meal_plan) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToMealPlanFragment());
    }
  }

  private void navigateCustom(NavDirections directions) {
    boolean useSliding = getSharedPrefs().getBoolean(
        Constants.SETTINGS.APPEARANCE.USE_SLIDING,
        Constants.SETTINGS_DEFAULT.APPEARANCE.USE_SLIDING
    );
    if (useSliding) {
      NavOptions.Builder builder = new NavOptions.Builder();
      builder.setEnterAnim(R.anim.slide_in_up).setPopExitAnim(R.anim.slide_out_down);
      builder.setPopUpTo(R.id.overviewStartFragment, false);
      if (!(activity.getCurrentFragment() instanceof OverviewStartFragment)) {
        builder.setExitAnim(R.anim.slide_out_down);
      } else {
        builder.setExitAnim(R.anim.slide_no);
      }
      navigate(directions, builder.build());
    } else {
      navigate(directions, getNavOptionsFragmentFade());
    }
    dismiss();
  }

  @Override
  public void navigateDeepLink(@StringRes int uri) {
    boolean useSliding = getSharedPrefs().getBoolean(
        Constants.SETTINGS.APPEARANCE.USE_SLIDING,
        Constants.SETTINGS_DEFAULT.APPEARANCE.USE_SLIDING
    );
    if (useSliding) {
      NavOptions.Builder builder = new NavOptions.Builder();
      builder.setEnterAnim(R.anim.slide_in_up).setPopExitAnim(R.anim.slide_out_down);
      builder.setPopUpTo(R.id.overviewStartFragment, false);
      if (!(activity.getCurrentFragment() instanceof OverviewStartFragment)) {
        builder.setExitAnim(R.anim.slide_out_down);
      } else {
        builder.setExitAnim(R.anim.slide_no);
      }
      findNavController().navigate(Uri.parse(getString(uri)), builder.build());
    } else {
      findNavController().navigate(Uri.parse(getString(uri)), getNavOptionsFragmentFade());
    }
    dismiss();
  }

  private void select(
      @NonNull LinearLayout linearLayout,
      @Nullable TextView textView,
      @Nullable ImageView imageView,
      float paddingStart,
      float paddingEnd
  ) {
    linearLayout.setBackground(
        ViewUtil.getBgListItemSelected(requireContext(), paddingStart, paddingEnd)
    );
    linearLayout.setClickable(false);
    if (textView != null) {
      textView.setTextColor(
          ResUtil.getColorAttr(requireContext(), R.attr.colorOnSecondaryContainer)
      );
    }
    if (imageView != null) {
      imageView.setColorFilter(
          ResUtil.getColorAttr(requireContext(), R.attr.colorPrimary)
      );
    }
  }

  private void select(
      @NonNull LinearLayout linearLayout,
      @Nullable TextView textView,
      @Nullable ImageView imageView
  ) {
    select(linearLayout, textView, imageView, 8, 8);
  }

  private void hideDisabledFeatures() {
    if (isFeatureDisabled(PREF.FEATURE_SHOPPING_LIST)) {
      binding.frameShoppingList.setVisibility(View.GONE);
    }
    if (isFeatureDisabled(PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
      binding.linearDrawerTransfer.setVisibility(View.GONE);
      binding.transactionsContainer.setWeightSum(75f);
    }
    if (isFeatureDisabled(PREF.FEATURE_RECIPES)) {
      binding.containerRecipes.setVisibility(View.GONE);
    }
    if (isFeatureDisabled(PREF.FEATURE_RECIPES)) {
      binding.linearDrawerRecipes.setVisibility(View.GONE);
    }
    if (isFeatureDisabled(PREF.FEATURE_TASKS) && isFeatureDisabled(PREF.FEATURE_CHORES)) {
      binding.containerTasks.setVisibility(View.GONE);
    }
    if (isFeatureDisabled(PREF.FEATURE_TASKS)) {
      binding.linearDrawerTasks.setVisibility(View.GONE);
    }
    if (isFeatureDisabled(PREF.FEATURE_CHORES)) {
      binding.linearDrawerChores.setVisibility(View.GONE);
    }
  }

  private boolean isFeatureDisabled(String pref) {
    if (pref == null) {
      return false;
    }
    return !sharedPrefs.getBoolean(pref, true);
  }

  private NavOptions getNavOptionsFragmentFade() {
    return new NavOptions.Builder()
        .setEnterAnim(R.anim.enter_end_fade)
        .setExitAnim(R.anim.exit_start_fade)
        .setPopEnterAnim(R.anim.enter_start_fade)
        .setPopExitAnim(R.anim.exit_end_fade)
        .build();
  }

  @Override
  public void applyBottomInset(int bottom) {
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    params.setMargins(0, 0, 0, bottom);
    binding.linearDrawerContainer.setLayoutParams(params);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
