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

package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowPendingPurchasesItemBinding;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.PendingProduct;
import xyz.zedler.patrick.grocy.model.PendingProductInfo;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductBarcode;
import xyz.zedler.patrick.grocy.model.StoredPurchase;

public class PendingPurchaseAdapter extends
    RecyclerView.Adapter<PendingPurchaseAdapter.ViewHolder> {

  private final static String TAG = PendingPurchaseAdapter.class.getSimpleName();
  private final static boolean DEBUG = false;

  private final ArrayList<GroupedListItem> groupedListItems;
  private final HashMap<Integer, List<ProductBarcode>> productBarcodeHashMap;
  private final PendingPurchaseAdapterListener listener;

  public PendingPurchaseAdapter(
      List<GroupedListItem> groupedListItems,
      HashMap<Integer, List<ProductBarcode>> productBarcodeHashMap,
      PendingPurchaseAdapterListener listener
  ) {
    this.groupedListItems = new ArrayList<>(groupedListItems);
    this.productBarcodeHashMap = productBarcodeHashMap;
    this.listener = listener;
  }

  @Override
  public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
    super.onDetachedFromRecyclerView(recyclerView);
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View view) {
      super(view);
    }
  }

  public static class PendingPurchaseViewHolder extends ViewHolder {

    private final RowPendingPurchasesItemBinding binding;

    public PendingPurchaseViewHolder(RowPendingPurchasesItemBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new PendingPurchaseViewHolder(RowPendingPurchasesItemBinding.inflate(
        LayoutInflater.from(parent.getContext()),
        parent,
        false
    ));
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int positionDoNotUse) {

    int position = viewHolder.getAdapterPosition();

    GroupedListItem item = groupedListItems.get(position);
    PendingPurchaseViewHolder holder = (PendingPurchaseViewHolder) viewHolder;

    if (GroupedListItem.getType(item, GroupedListItem.CONTEXT_STORED_PURCHASES)
        == GroupedListItem.TYPE_ENTRY) {
      StoredPurchase pendingPurchase = (StoredPurchase) item;
      holder.binding.textPurchase.setText("Purchase: " + pendingPurchase.getId());
      holder.binding.containerPurchase.setVisibility(View.VISIBLE);
      holder.binding.containerProduct.setVisibility(View.GONE);
      holder.binding.containerInfo.setVisibility(View.GONE);

    } else if (GroupedListItem.getType(item, GroupedListItem.CONTEXT_STORED_PURCHASES)
        == GroupedListItem.TYPE_HEADER) {
      List<ProductBarcode> barcodesList;
      if (item instanceof PendingProduct) {
        PendingProduct pendingProduct = (PendingProduct) item;
        barcodesList = productBarcodeHashMap.get(pendingProduct.getId());
        holder.binding.nameProduct.setText(pendingProduct.getName());
        holder.binding.imagePending.setVisibility(View.VISIBLE);
        holder.binding.imageOnline.setVisibility(View.GONE);
      } else { // instance of Product
        Product product = (Product) item;
        barcodesList = productBarcodeHashMap.get(product.getId());
        holder.binding.nameProduct.setText(product.getName());
        holder.binding.imagePending.setVisibility(View.GONE);
        holder.binding.imageOnline.setVisibility(View.VISIBLE);
      }

      if (barcodesList != null) {
        StringBuilder barcodes = new StringBuilder();
        for (ProductBarcode barcode : barcodesList) {
          barcodes.append(barcode.getBarcode()).append(" ");
        }
        holder.binding.barcodes.setText(
            holder.binding.barcodes.getContext()
                .getString(R.string.property_barcodes_insert, barcodes.toString()));
        holder.binding.barcodes.setVisibility(View.VISIBLE);
      } else {
        holder.binding.barcodes.setVisibility(View.GONE);
      }
      holder.binding.containerPurchase.setVisibility(View.GONE);
      holder.binding.containerInfo.setVisibility(View.GONE);
      holder.binding.containerProduct.setVisibility(View.VISIBLE);

    } else { // GroupedListItem.TYPE_INFO
      PendingProductInfo pendingProductInfo = (PendingProductInfo) item;
      if (pendingProductInfo.getProduct() instanceof PendingProduct) {
        holder.binding.textInfo.setText(holder.binding.textInfo.getContext()
            .getString(R.string.msg_stored_purchases_product_offline));
      } else {
        holder.binding.textInfo.setText(holder.binding.textInfo.getContext()
            .getString(R.string.msg_stored_purchases_product_online));
      }
      holder.binding.containerPurchase.setVisibility(View.GONE);
      holder.binding.containerInfo.setVisibility(View.VISIBLE);
      holder.binding.containerProduct.setVisibility(View.GONE);
    }

    if (GroupedListItem.getType(item, GroupedListItem.CONTEXT_STORED_PURCHASES)
        == GroupedListItem.TYPE_INFO) {
      holder.binding.container.setClickable(false);
      holder.binding.container.setFocusable(false);
      holder.binding.container.setBackground(null);
    } else {
      holder.binding.container.setClickable(true);
      holder.binding.container.setFocusable(true);
      holder.binding.container.setBackground(AppCompatResources
          .getDrawable(holder.binding.container.getContext(), R.drawable.bg_list_item));
    }

    holder.binding.container.setOnClickListener(view -> listener.onItemRowClicked(item));
  }

  @Override
  public int getItemCount() {
    return groupedListItems.size();
  }

  public interface PendingPurchaseAdapterListener {

    void onItemRowClicked(GroupedListItem groupedListItem);
  }

  public void updateData(List<GroupedListItem> newList) {
    this.groupedListItems.clear();
    this.groupedListItems.addAll(newList);
    notifyDataSetChanged();
  }
}
