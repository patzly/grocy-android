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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowProductBarcodeBinding;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class ProductBarcodeAdapter extends RecyclerView.Adapter<ProductBarcodeAdapter.ViewHolder> {

  private final static String TAG = ProductBarcodeAdapter.class.getSimpleName();

  private final ArrayList<ProductBarcode> productBarcodes;
  private final ProductBarcodeAdapterListener listener;

  public ProductBarcodeAdapter(
      ArrayList<ProductBarcode> productBarcodes,
      ProductBarcodeAdapterListener listener
  ) {
    this.productBarcodes = new ArrayList<>(productBarcodes);
    this.listener = listener;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    private final RowProductBarcodeBinding binding;

    public ViewHolder(RowProductBarcodeBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @NonNull
  @Override
  public ProductBarcodeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
      int viewType) {
    RowProductBarcodeBinding binding = RowProductBarcodeBinding.inflate(
        LayoutInflater.from(parent.getContext()),
        parent,
        false
    );
    return new ViewHolder(binding);
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(
      @NonNull final ProductBarcodeAdapter.ViewHolder holder,
      int position
  ) {
    ProductBarcode productBarcode = productBarcodes.get(holder.getAdapterPosition());

    holder.binding.barcode.setText(productBarcode.getBarcode());

    if (NumUtil.isStringDouble(productBarcode.getAmount())) {
      String amountStr = holder.binding.amount.getContext().getString(
          R.string.subtitle_barcode_amount,
          NumUtil.trim(NumUtil.toDouble(productBarcode.getAmount()))
      );
      holder.binding.amount.setText(amountStr);
      holder.binding.amount.setVisibility(View.VISIBLE);
    } else {
      holder.binding.amount.setVisibility(View.GONE);
    }

    if (productBarcode.getNote() != null && !productBarcode.getNote().trim().isEmpty()) {
      holder.binding.note.setText(productBarcode.getNote());
      holder.binding.note.setVisibility(View.VISIBLE);
    } else {
      holder.binding.note.setVisibility(View.GONE);
    }

    holder.binding.container.setOnClickListener(
        view -> listener.onItemRowClicked(productBarcode)
    );
  }

  @Override
  public int getItemCount() {
    return productBarcodes.size();
  }

  public void updateData(ArrayList<ProductBarcode> productBarcodesNew) {
    DiffCallback diffCallback = new DiffCallback(
        this.productBarcodes,
        productBarcodesNew
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.productBarcodes.clear();
    this.productBarcodes.addAll(productBarcodesNew);
    diffResult.dispatchUpdatesTo(this);
  }

  static class DiffCallback extends DiffUtil.Callback {

    ArrayList<ProductBarcode> oldItems;
    ArrayList<ProductBarcode> newItems;

    public DiffCallback(
        ArrayList<ProductBarcode> oldItems,
        ArrayList<ProductBarcode> newItems
    ) {
      this.newItems = newItems;
      this.oldItems = oldItems;
    }

    @Override
    public int getOldListSize() {
      return oldItems.size();
    }

    @Override
    public int getNewListSize() {
      return newItems.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
      return compare(oldItemPosition, newItemPosition, false);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
      return compare(oldItemPosition, newItemPosition, true);
    }

    private boolean compare(int oldItemPos, int newItemPos, boolean compareContent) {
      ProductBarcode newItem = newItems.get(newItemPos);
      ProductBarcode oldItem = oldItems.get(oldItemPos);
      if (!compareContent) {
        return newItem.getId() == oldItem.getId();
      }

      return newItem.equals(oldItem);
    }
  }

  public interface ProductBarcodeAdapterListener {

    void onItemRowClicked(ProductBarcode productBarcode);
  }
}
