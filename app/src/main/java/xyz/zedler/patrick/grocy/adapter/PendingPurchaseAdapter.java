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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.databinding.RowPendingPurchasesItemBinding;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.PendingProduct;
import xyz.zedler.patrick.grocy.model.PendingPurchase;

public class PendingPurchaseAdapter extends
    RecyclerView.Adapter<PendingPurchaseAdapter.ViewHolder> {

  private final static String TAG = PendingPurchaseAdapter.class.getSimpleName();
  private final static boolean DEBUG = false;

  private final ArrayList<GroupedListItem> groupedListItems;
  private final PendingPurchaseAdapterListener listener;

  public PendingPurchaseAdapter(
      List<GroupedListItem> groupedListItems,
      PendingPurchaseAdapterListener listener
  ) {
    this.groupedListItems = new ArrayList<>(groupedListItems);
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

    if (GroupedListItem.getType(item, GroupedListItem.CONTEXT_PENDING_PURCHASES)
        == GroupedListItem.TYPE_ENTRY) {
      PendingPurchase pendingPurchase = (PendingPurchase) item;
      holder.binding.textPurchase.setText("Purchase: " + pendingPurchase.getId());
      holder.binding.containerPurchase.setVisibility(View.VISIBLE);
      holder.binding.containerProduct.setVisibility(View.GONE);
    } else {
      PendingProduct pendingProduct = (PendingProduct) item;
      holder.binding.nameProduct.setText(pendingProduct.getName());
      holder.binding.containerPurchase.setVisibility(View.GONE);
      holder.binding.containerProduct.setVisibility(View.VISIBLE);
    }

    holder.binding.container.setOnClickListener(
        view -> listener.onItemRowClicked(item)
    );
  }

  @Override
  public int getItemCount() {
    return groupedListItems.size();
  }

  public interface PendingPurchaseAdapterListener {

    void onItemRowClicked(GroupedListItem groupedListItem);
  }

  public void updateData(List<GroupedListItem> newList) {

    PendingPurchaseAdapter.DiffCallback diffCallback = new PendingPurchaseAdapter.DiffCallback(
        this.groupedListItems,
        newList
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.groupedListItems.clear();
    this.groupedListItems.addAll(newList);
    diffResult.dispatchUpdatesTo(this);
  }

  static class DiffCallback extends DiffUtil.Callback {

    List<GroupedListItem> oldItems;
    List<GroupedListItem> newItems;

    public DiffCallback(
        List<GroupedListItem> oldItems,
        List<GroupedListItem> newItems
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
      int oldItemType = GroupedListItem.getType(
          oldItems.get(oldItemPos),
          GroupedListItem.CONTEXT_PENDING_PURCHASES
      );
      int newItemType = GroupedListItem.getType(
          newItems.get(newItemPos),
          GroupedListItem.CONTEXT_PENDING_PURCHASES
      );
      if (oldItemType != newItemType) {
        return false;
      }

      if (oldItemType == GroupedListItem.TYPE_ENTRY) {
        PendingPurchase newItem = (PendingPurchase) newItems.get(newItemPos);
        PendingPurchase oldItem = (PendingPurchase) oldItems.get(oldItemPos);
        if (!compareContent) {
          return newItem.getId() == oldItem.getId();
        }
        return newItem.equals(oldItem);
      } else {
        PendingProduct newProduct = (PendingProduct) newItems.get(newItemPos);
        PendingProduct oldProduct = (PendingProduct) oldItems.get(oldItemPos);
        if (!compareContent) {
          return newProduct.getId() == oldProduct.getId();
        }
        return newProduct.equals(oldProduct);
      }
    }
  }
}
