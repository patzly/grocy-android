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

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetStockEntryBinding;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.AmountUtil;
import xyz.zedler.patrick.grocy.util.Constants.ACTION;
import xyz.zedler.patrick.grocy.util.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.util.Constants.PREF;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;

public class StockEntryBottomSheet extends BaseBottomSheet {

  private final static String TAG = StockEntryBottomSheet.class.getSimpleName();

  private MainActivity activity;
  private FragmentBottomsheetStockEntryBinding binding;

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
    binding = FragmentBottomsheetStockEntryBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    activity = (MainActivity) getActivity();
    assert activity != null;

    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

    Bundle bundle = requireArguments();
    StockEntry stockEntry = bundle.getParcelable(ARGUMENT.STOCK_ENTRY);
    Product product = bundle.getParcelable(ARGUMENT.PRODUCT);
    QuantityUnit quStock = bundle.getParcelable(ARGUMENT.QUANTITY_UNIT_STOCK);
    QuantityUnit quPurchase = bundle.getParcelable(ARGUMENT.QUANTITY_UNIT_PURCHASE);
    Location location = bundle.getParcelable(ARGUMENT.LOCATION);
    Store store = bundle.getParcelable(ARGUMENT.STORE);
    if (stockEntry == null || product == null) {
      showMessage(R.string.error_undefined);
      dismiss();
      return;
    }

    DateUtil dateUtil = new DateUtil(activity);
    String currency = sharedPrefs.getString(PREF.CURRENCY, "");

    binding.productName.setText(getString(R.string.property_product_name), product.getName());
    if (stockEntry.getNote() == null || stockEntry.getNote().trim().isEmpty()) {
      binding.note.setVisibility(View.GONE);
    } else {
      binding.note.setText(stockEntry.getNote());
    }

    binding.amount.setText(
        getString(R.string.property_amount),
        AmountUtil.getStockEntryAmountInfo(requireContext(),
            new PluralUtil(requireContext()), stockEntry, quStock)
    );

    String date = stockEntry.getBestBeforeDate();
    String days = null;
    if (date != null) {
      days = String.valueOf(DateUtil.getDaysFromNow(date));
    }
    if (days != null) {
      binding.dueDate.setVisibility(View.VISIBLE);
      binding.dueDate.setText(
          getString(R.string.property_due_date),
          dateUtil.getLocalizedDate(date, DateUtil.FORMAT_SHORT),
          dateUtil.getHumanForDaysFromNow(date)
      );
    } else {
      binding.dueDate.setVisibility(View.GONE);
    }

    if (location != null) {
      binding.location.setText(
          getString(R.string.property_location),
          location.getName()
      );
      binding.location.setVisibility(View.VISIBLE);
    } else {
      binding.location.setVisibility(View.GONE);
    }

    if (store != null) {
      binding.store.setText(
          getString(R.string.property_store),
          store.getName()
      );
      binding.store.setVisibility(View.VISIBLE);
    } else {
      binding.store.setVisibility(View.GONE);
    }

    if (NumUtil.isStringDouble(stockEntry.getPrice())) {
      if (product.getQuIdStockInt() == product.getQuIdPurchaseInt() || quPurchase == null
          || quStock == null) {
        binding.price.setText(
            getString(R.string.property_price),
            NumUtil.trimPrice(NumUtil.toDouble(stockEntry.getPrice())
                * product.getQuFactorPurchaseToStockDouble()) + " " + currency
        );
      } else {
        binding.price.setText(
            getString(R.string.property_price),
            getString(
                R.string.property_price_unit_insert,
                NumUtil.trimPrice(NumUtil.toDouble(stockEntry.getPrice())
                    * product.getQuFactorPurchaseToStockDouble()) + " " + currency,
                quPurchase.getName()
            ),
            getString(
                R.string.property_price_unit_insert,
                NumUtil.trimPrice(NumUtil.toDouble(stockEntry.getPrice())) + " " + currency,
                quStock.getName()
            )
        );
      }
      binding.price.setVisibility(View.VISIBLE);
    } else {
      binding.price.setVisibility(View.GONE);
    }

    String purchaseDate = stockEntry.getPurchasedDate();
    String purchaseDays = null;
    if (purchaseDate != null) {
      purchaseDays = String.valueOf(DateUtil.getDaysFromNow(purchaseDate));
    }
    if (purchaseDays != null) {
      binding.purchasedDate.setVisibility(View.VISIBLE);
      binding.purchasedDate.setText(
          getString(R.string.property_purchased_date),
          dateUtil.getLocalizedDate(purchaseDate, DateUtil.FORMAT_SHORT),
          dateUtil.getHumanForDaysFromNow(purchaseDate)
      );
    } else {
      binding.purchasedDate.setVisibility(View.GONE);
    }

    binding.toolbar.setOnMenuItemClickListener(item -> {
      if (item.getItemId() == R.id.action_consume) {
        activity.getCurrentFragment().performAction(ACTION.CONSUME, stockEntry);
        dismiss();
        return true;
      } else if (item.getItemId() == R.id.action_open) {
        activity.getCurrentFragment().performAction(ACTION.OPEN, stockEntry);
        dismiss();
        return true;
      } else if (item.getItemId() == R.id.action_consume_spoiled) {
        activity.getCurrentFragment().performAction(ACTION.CONSUME_SPOILED, stockEntry);
        dismiss();
        return true;
      } else if (item.getItemId() == R.id.action_edit) {
        Toast.makeText(requireContext(), R.string.msg_not_implemented_yet, Toast.LENGTH_LONG).show();
        return true;
      }
      return false;
    });

    if (stockEntry.getOpen() == 1 || product.getEnableTareWeightHandlingBoolean()) {
      binding.toolbar.getMenu().findItem(R.id.action_open).setVisible(false);
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
