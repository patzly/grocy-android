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
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.Product;

public class ChooseProductAdapter extends
    RecyclerView.Adapter<ChooseProductAdapter.ViewHolder> {

  private final static String TAG = ChooseProductAdapter.class.getSimpleName();

  private final ArrayList<Product> products;
  private final ChooseProductAdapterListener listener;

  public ChooseProductAdapter(
      ArrayList<Product> products,
      ChooseProductAdapterListener listener
  ) {
    this.products = new ArrayList<>(products);
    this.listener = listener;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View view) {
      super(view);
    }
  }

  public static class ItemViewHolder extends ViewHolder {

    private final LinearLayout linearLayoutItemContainer;
    private final TextView textViewName;

    public ItemViewHolder(View view) {
      super(view);

      linearLayoutItemContainer = view.findViewById(R.id.linear_master_item_container);
      textViewName = view.findViewById(R.id.text_master_item_name);
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new ItemViewHolder(
        LayoutInflater.from(parent.getContext()).inflate(
            R.layout.row_master_item,
            parent,
            false
        )
    );
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int position) {
    ItemViewHolder holder = (ItemViewHolder) viewHolder;
    Product product = products.get(holder.getAdapterPosition());

    // NAME
    holder.textViewName.setText(product.getName());

    // CONTAINER
    holder.linearLayoutItemContainer.setOnClickListener(
        view -> listener.onItemRowClicked(product)
    );
  }

  public void updateData(ArrayList<Product> newProducts) {
    DiffCallback diffCallback = new DiffCallback(
        newProducts,
        this.products
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.products.clear();
    this.products.addAll(newProducts);
    diffResult.dispatchUpdatesTo(this);
  }

  static class DiffCallback extends DiffUtil.Callback {

    ArrayList<Product> oldItems;
    ArrayList<Product> newItems;

    public DiffCallback(
        ArrayList<Product> newItems,
        ArrayList<Product> oldItems
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
      Product newItem = newItems.get(newItemPos);
      Product oldItem = oldItems.get(oldItemPos);
      return compareContent ? newItem.equals(oldItem)
          : newItem.getId() == oldItem.getId();
    }
  }

  @Override
  public int getItemCount() {
    return products.size();
  }

  public interface ChooseProductAdapterListener {

    void onItemRowClicked(Product product);
  }
}
