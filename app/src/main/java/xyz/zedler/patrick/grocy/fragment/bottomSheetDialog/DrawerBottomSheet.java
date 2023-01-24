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
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.fragment.bottomSheetDialog;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavDirections;
import androidx.navigation.NavOptions;
import androidx.preference.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import xyz.zedler.patrick.grocy.Constants.PREF;
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
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;
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
        ViewUtil.getRippleBgListItemSurface(requireContext())
    );
    binding.linearDrawerPurchase.setBackground(
        ViewUtil.getRippleBgListItemSurface(requireContext())
    );
    binding.linearDrawerTransfer.setBackground(
        ViewUtil.getRippleBgListItemSurface(requireContext())
    );
    binding.linearDrawerInventory.setBackground(
        ViewUtil.getRippleBgListItemSurface(requireContext())
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
        v -> activity.navigateDeepLink(R.string.deep_link_shoppingModeFragment)
    );
    ViewUtil.setTooltipText(binding.buttonDrawerShoppingMode, R.string.title_shopping_mode);
    ViewUtil.setTooltipText(binding.buttonDrawerConsume, R.string.title_consume);
    ViewUtil.setTooltipText(binding.buttonDrawerPurchase, R.string.title_purchase);
    ViewUtil.setTooltipText(binding.buttonDrawerTransfer, R.string.title_transfer);
    ViewUtil.setTooltipText(binding.buttonDrawerInventory, R.string.title_inventory);

    ViewUtil.setTooltipText(binding.linearDrawerConsume, R.string.title_consume);
    ViewUtil.setTooltipText(binding.linearDrawerPurchase, R.string.title_purchase);
    ViewUtil.setTooltipText(binding.linearDrawerTransfer, R.string.title_transfer);
    ViewUtil.setTooltipText(binding.linearDrawerInventory, R.string.title_inventory);

    ViewTreeObserver observerText = binding.textDrawerStock.getViewTreeObserver();
    observerText.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        boolean isLayoutRtl = UiUtil.isLayoutRtl(activity);
        int textEnd = isLayoutRtl
            ? binding.textDrawerStock.getLeft()
            : binding.textDrawerStock.getRight();
        int iconsStart = isLayoutRtl
            ? binding.linearDrawerContainerTransactionIcons.getRight()
            : binding.linearDrawerContainerTransactionIcons.getLeft();
        boolean hasEnoughSpace = isLayoutRtl ? textEnd >= iconsStart : textEnd <= iconsStart;
        binding.linearDrawerContainerTransactions.setVisibility(
            hasEnoughSpace ? View.GONE : View.VISIBLE
        );
        binding.linearDrawerContainerTransactionIcons.setVisibility(
            hasEnoughSpace ? View.VISIBLE : View.GONE
        );
        if (binding.textDrawerStock.getViewTreeObserver().isAlive()) {
          binding.textDrawerStock.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
      }
    });

    ClickUtil.setOnClickListeners(
        this,
        binding.linearDrawerStock,
        binding.linearDrawerShoppingList,
        binding.buttonDrawerConsume,
        binding.buttonDrawerPurchase,
        binding.buttonDrawerTransfer,
        binding.buttonDrawerInventory,
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
      select(binding.linearDrawerStock, binding.textDrawerStock, binding.imageDrawerStock);
    } else if (currentFragment instanceof ShoppingListFragment) {
      select(
          binding.linearDrawerShoppingList,
          binding.textDrawerShoppingList,
          binding.imageDrawerShoppingList
      );
    } else if (currentFragment instanceof ConsumeFragment) {
      select(binding.buttonDrawerConsume);
      select(binding.linearDrawerConsume, null, binding.imageDrawerConsume);
    } else if (currentFragment instanceof PurchaseFragment) {
      select(binding.buttonDrawerPurchase);
      select(binding.linearDrawerPurchase, null, binding.imageDrawerPurchase);
    } else if (currentFragment instanceof TransferFragment) {
      select(binding.buttonDrawerTransfer);
      select(binding.linearDrawerTransfer, null, binding.imageDrawerTransfer);
    } else if (currentFragment instanceof InventoryFragment) {
      select(binding.buttonDrawerInventory);
      select(binding.linearDrawerInventory, null, binding.imageDrawerInventory);
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
    int id = v.getId();
    if (getViewUtil().isClickDisabled(id)) {
      return;
    }
    if (id == R.id.linear_drawer_stock) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToStockOverviewFragment());
    } else if (id == R.id.linear_drawer_shopping_list) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToShoppingListFragment());
    } else if (id == R.id.button_drawer_consume || id == R.id.linear_drawer_consume) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToConsumeFragment());
    } else if (id == R.id.button_drawer_purchase || id == R.id.linear_drawer_purchase) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToPurchaseFragment());
    } else if (id == R.id.button_drawer_transfer || id == R.id.linear_drawer_transfer) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToTransferFragment());
    } else if (id == R.id.button_drawer_inventory || id == R.id.linear_drawer_inventory) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToInventoryFragment());
    } else if (id == R.id.linear_drawer_chores) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToChoresFragment());
    } else if (id == R.id.linear_drawer_tasks) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToTasksFragment());
    } else if (id == R.id.linear_drawer_master_data) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToNavigationMasterObjects());
    } else if (id == R.id.linear_drawer_settings) {
      navigateCustom(DrawerBottomSheetDirections
          .actionDrawerBottomSheetDialogFragmentToSettingsFragment());
    } else if (id == R.id.linear_drawer_feedback) {
      activity.showFeedbackBottomSheet();
      dismiss();
    } else if (id == R.id.linear_drawer_help) {
      dismiss();
      new Handler(Looper.getMainLooper()).postDelayed(
          () -> activity.showHelpBottomSheet(), 10
      );
    } else if (id == R.id.linear_drawer_recipes) {
      navigateCustom(DrawerBottomSheetDirections
              .actionDrawerBottomSheetDialogFragmentToRecipesFragment());
    }
  }

  private void navigateCustom(NavDirections directions) {
    NavOptions.Builder builder = activity.getNavOptionsBuilderFragmentFadeOrSlide(true);
    builder.setPopUpTo(R.id.overviewStartFragment, false);
    if (activity.getCurrentFragment() instanceof OverviewStartFragment) {
      builder.setExitAnim(R.anim.slide_no);
    }
    activity.navigate(directions, builder.build());
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

  private void select(@NonNull MaterialButton button) {
    button.setClickable(false);
    button.setIconTint(
        ColorStateList.valueOf(ResUtil.getColorAttr(requireContext(), R.attr.colorPrimary))
    );
    button.setBackgroundColor(
        ResUtil.getColorAttr(requireContext(), R.attr.colorSecondaryContainer)
    );
  }

  private void hideDisabledFeatures() {
    if (isFeatureDisabled(PREF.FEATURE_SHOPPING_LIST)) {
      binding.frameShoppingList.setVisibility(View.GONE);
    }
    if (isFeatureDisabled(PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
      binding.buttonDrawerTransfer.setVisibility(View.GONE);
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
