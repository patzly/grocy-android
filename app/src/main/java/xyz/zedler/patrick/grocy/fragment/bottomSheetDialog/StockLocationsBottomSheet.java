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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.StockLocationAdapter;
import xyz.zedler.patrick.grocy.databinding.FragmentBottomsheetStockLocationsBinding;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.util.UiUtil;

public class StockLocationsBottomSheet extends BaseBottomSheetDialogFragment
    implements StockLocationAdapter.StockLocationAdapterListener {

  private final static String TAG = StockLocationsBottomSheet.class.getSimpleName();

  private FragmentBottomsheetStockLocationsBinding binding;
  private MainActivity activity;
  private ArrayList<StockLocation> stockLocations;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentBottomsheetStockLocationsBinding.inflate(
        inflater, container, false
    );

    activity = (MainActivity) getActivity();
    Bundle bundle = requireArguments();

    stockLocations = bundle.getParcelableArrayList(Constants.ARGUMENT.STOCK_LOCATIONS);
    ProductDetails productDetails = bundle.getParcelable(Constants.ARGUMENT.PRODUCT_DETAILS);
    QuantityUnit quantityUnitStock = bundle.getParcelable(Constants.ARGUMENT.QUANTITY_UNIT);
    int selected = bundle.getInt(Constants.ARGUMENT.SELECTED_ID, 0);

    if (productDetails != null) {
      binding.textStockLocationsSubtitle.setText(
          activity.getString(
              R.string.subtitle_stock_locations,
              productDetails.getProduct().getName()
          )
      );
    } else {
      binding.textStockLocationsSubtitle.setVisibility(View.GONE);
    }

    binding.recyclerStockLocations.setLayoutManager(
        new LinearLayoutManager(
            activity,
            LinearLayoutManager.VERTICAL,
            false
        )
    );
    binding.recyclerStockLocations.setItemAnimator(new DefaultItemAnimator());
    binding.recyclerStockLocations.setAdapter(new StockLocationAdapter(
        activity, stockLocations, productDetails, quantityUnitStock, selected, this
    ));

    return binding.getRoot();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  @Override
  public void onItemRowClicked(int position) {
    activity.getCurrentFragment().selectStockLocation(stockLocations.get(position));
    dismiss();
  }

  @Override
  public void applyBottomInset(int bottom) {
    binding.recyclerStockLocations.setPadding(
        binding.recyclerStockLocations.getPaddingLeft(),
        binding.recyclerStockLocations.getPaddingTop(),
        binding.recyclerStockLocations.getPaddingRight(),
        UiUtil.dpToPx(activity, 8) + bottom
    );
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
