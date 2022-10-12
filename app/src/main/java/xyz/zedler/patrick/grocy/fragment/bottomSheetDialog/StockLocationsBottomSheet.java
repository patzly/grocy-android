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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.adapter.StockLocationAdapter;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.Constants;

public class StockLocationsBottomSheet extends BaseBottomSheetDialogFragment
    implements StockLocationAdapter.StockLocationAdapterListener {

  private final static String TAG = StockLocationsBottomSheet.class.getSimpleName();

  private MainActivity activity;
  private ArrayList<StockLocation> stockLocations;

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
    View view = inflater.inflate(
        R.layout.fragment_bottomsheet_stock_locations, container, false
    );

    activity = (MainActivity) getActivity();
    Bundle bundle = requireArguments();

    stockLocations = bundle.getParcelableArrayList(Constants.ARGUMENT.STOCK_LOCATIONS);
    ProductDetails productDetails = bundle.getParcelable(Constants.ARGUMENT.PRODUCT_DETAILS);
    QuantityUnit quantityUnitStock = bundle.getParcelable(Constants.ARGUMENT.QUANTITY_UNIT);
    int selected = bundle.getInt(Constants.ARGUMENT.SELECTED_ID, 0);

    TextView textViewSubtitle = view.findViewById(R.id.text_stock_locations_subtitle);
    if (productDetails != null) {
      textViewSubtitle.setText(
          activity.getString(
              R.string.subtitle_stock_locations,
              productDetails.getProduct().getName()
          )
      );
    } else {
      textViewSubtitle.setVisibility(View.GONE);
    }

    RecyclerView recyclerView = view.findViewById(R.id.recycler_stock_locations);
    recyclerView.setLayoutManager(
        new LinearLayoutManager(
            activity,
            LinearLayoutManager.VERTICAL,
            false
        )
    );
    recyclerView.setItemAnimator(new DefaultItemAnimator());
    recyclerView.setAdapter(new StockLocationAdapter(
        activity, stockLocations, productDetails, quantityUnitStock, selected, this
    ));

    return view;
  }

  @Override
  public void onItemRowClicked(int position) {
    activity.getCurrentFragment().selectStockLocation(stockLocations.get(position));
    dismiss();
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
