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

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetMasterProductBinding;
import xyz.zedler.patrick.grocy.fragment.BaseFragment;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.TextUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class MasterProductBottomSheet extends BaseBottomSheetDialogFragment {

  private final static String TAG = MasterProductBottomSheet.class.getSimpleName();

  private static final String DIALOG_DELETE = "dialog_delete";

  private MainActivity activity;
  private FragmentBottomsheetMasterProductBinding binding;
  private Product product;
  private Location location;
  private QuantityUnit quantityUnitPurchase, quantityUnitStock;
  private ProductGroup productGroup;
  private AlertDialog dialogDelete;
  private int maxDecimalPlacesAmount;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetMasterProductBinding.inflate(
        inflater, container, false
    );
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) getActivity();
    assert activity != null;
    maxDecimalPlacesAmount = PreferenceManager.getDefaultSharedPreferences(activity).getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );

    Bundle bundle = getArguments();
    if (bundle != null) {
      product = bundle.getParcelable(Constants.ARGUMENT.PRODUCT);
      location = bundle.getParcelable(Constants.ARGUMENT.LOCATION);
      quantityUnitPurchase = bundle.getParcelable(Constants.ARGUMENT.QUANTITY_UNIT_PURCHASE);
      quantityUnitStock = bundle.getParcelable(Constants.ARGUMENT.QUANTITY_UNIT_STOCK);
      productGroup = bundle.getParcelable(Constants.ARGUMENT.PRODUCT_GROUP);
    }

    // TOOLBAR

    ResUtil.tintMenuItemIcons(activity, binding.toolbar.getMenu());
    binding.toolbar.setOnMenuItemClickListener(item -> {
      BaseFragment fragmentCurrent = activity.getCurrentFragment();
      if (item.getItemId() == R.id.action_edit) {
        fragmentCurrent.editObject(product);
        dismiss();
      } else if (item.getItemId() == R.id.action_copy) {
        fragmentCurrent.copyProduct(product);
        dismiss();
      } else if (item.getItemId() == R.id.action_delete) {
        showDeleteConfirmationDialog();
      }
      return true;
    });

    setData();

    if (savedInstanceState != null && savedInstanceState.getBoolean(DIALOG_DELETE)) {
      new Handler(Looper.getMainLooper()).postDelayed(
          this::showDeleteConfirmationDialog, 1
      );
    }
  }

  @Override
  public void onDestroyView() {
    if (dialogDelete != null) {
      // Else it throws an leak exception because the context is somehow from the activity
      dialogDelete.dismiss();
    }
    super.onDestroyView();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    boolean isShowing = dialogDelete != null && dialogDelete.isShowing();
    outState.putBoolean(DIALOG_DELETE, isShowing);
  }

  private void setData() {
    // NAME
    binding.itemName.setText(
        activity.getString(R.string.property_name),
        product.getName()
    );

    // DESCRIPTION
    CharSequence trimmedDescription = TextUtil.trimCharSequence(product.getDescription());
    String description = trimmedDescription != null ? trimmedDescription.toString() : null;
    binding.cardDescription.setHtml(description);

    // LOCATION
    if (location != null && isFeatureEnabled(PREF.FEATURE_STOCK_LOCATION_TRACKING)) {
      binding.itemLocation.setText(
          activity.getString(R.string.property_location),
          location.getName()
      );
    } else {
      binding.itemLocation.setVisibility(View.GONE);
    }

    // MIN STOCK AMOUNT
    binding.itemMinStockAmount.setText(
        activity.getString(R.string.property_amount_min_stock),
        NumUtil.trimAmount(product.getMinStockAmountDouble(), maxDecimalPlacesAmount)
    );

    // QUANTITY UNIT PURCHASE
    if (quantityUnitPurchase != null) {
      binding.itemQuPurchase.setText(
          activity.getString(R.string.property_default_qu_purchase),
          quantityUnitPurchase.getName()
      );
    } else {
      binding.itemQuPurchase.setVisibility(View.GONE);
    }

    // QUANTITY UNIT STOCK
    if (quantityUnitStock != null) {
      binding.itemQuStock.setText(
          activity.getString(R.string.property_qu_stock),
          quantityUnitStock.getName()
      );
    } else {
      binding.itemQuStock.setVisibility(View.GONE);
    }

    // QUANTITY UNIT FACTOR
    binding.itemQuFactor.setText(
        activity.getString(R.string.property_qu_factor),
        NumUtil.trimAmount(product.getQuFactorPurchaseToStockDouble(), maxDecimalPlacesAmount)
    );

    // PRODUCT GROUP
    if (product.getProductGroupId() != null && productGroup != null) {
      binding.itemProductGroup.setText(
          activity.getString(R.string.property_product_group),
          productGroup.getName()
      );
    } else {
      binding.itemProductGroup.setVisibility(View.GONE);
    }
  }

  @SuppressWarnings("SameParameterValue")
  private boolean isFeatureEnabled(String pref) {
    if (pref == null) {
      return true;
    }
    return getSharedPrefs().getBoolean(pref, true);
  }

  private void showDeleteConfirmationDialog() {
    dialogDelete = new MaterialAlertDialogBuilder(
        activity, R.style.ThemeOverlay_Grocy_AlertDialog_Caution
    ).setTitle(R.string.title_confirmation)
        .setMessage(
            getString(
                R.string.msg_master_delete_product,
                product.getName()
            )
        ).setPositiveButton(R.string.action_delete, (dialog, which) -> {
          performHapticClick();
          activity.getCurrentFragment().deleteObject(product.getId());
          dismiss();
        }).setNegativeButton(R.string.action_cancel, (dialog, which) -> performHapticClick())
        .setOnCancelListener(dialog -> performHapticClick())
        .create();
    dialogDelete.show();
  }

  @Override
  public void applyBottomInset(int bottom) {
    binding.linearContainer.setPadding(
        binding.linearContainer.getPaddingLeft(),
        binding.linearContainer.getPaddingTop(),
        binding.linearContainer.getPaddingRight(),
        UiUtil.dpToPx(activity, 8) + bottom
    );
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
