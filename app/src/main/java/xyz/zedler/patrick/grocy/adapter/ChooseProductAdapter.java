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

package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.PendingProduct;
import xyz.zedler.patrick.grocy.model.Product;

public class ChooseProductAdapter extends
    RecyclerView.Adapter<ChooseProductAdapter.ViewHolder> {

  private final static String TAG = ChooseProductAdapter.class.getSimpleName();

  private final List<Product> products;
  private final ChooseProductAdapterListener listener;
  private final boolean forbidCreateProduct;

  public ChooseProductAdapter(
      List<Product> products,
      ChooseProductAdapterListener listener,
      boolean forbidCreateProduct
  ) {
    this.products = new ArrayList<>(products);
    this.listener = listener;
    this.forbidCreateProduct = forbidCreateProduct;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View view) {
      super(view);
    }
  }

  public static class ItemViewHolder extends ViewHolder {

    private final LinearLayout container;
    private final TextView name;
    private final ImageView imagePending;
    private final MaterialToolbar toolbar;

    public ItemViewHolder(View view) {
      super(view);

      container = view.findViewById(R.id.container);
      name = view.findViewById(R.id.name);
      imagePending = view.findViewById(R.id.image_pending);
      toolbar = view.findViewById(R.id.toolbar);
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new ItemViewHolder(
        LayoutInflater.from(parent.getContext()).inflate(
            R.layout.row_choose_product_item,
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
    holder.name.setText(product.getName());

    // ICON
    if (product instanceof PendingProduct) {
      holder.imagePending.setVisibility(View.VISIBLE);
    } else {
      holder.imagePending.setVisibility(View.GONE);
    }

    if (forbidCreateProduct) {
      holder.toolbar.setVisibility(View.GONE);
    } else {
      holder.toolbar.setVisibility(View.VISIBLE);
      holder.toolbar.setOnMenuItemClickListener(item -> {
        if (item.getItemId() == R.id.action_copy) {
          listener.onItemRowClicked(product, true);
        }
        return false;
      });
    }

    // CONTAINER
    holder.container.setOnClickListener(
        view -> listener.onItemRowClicked(product, false)
    );
  }

  public void updateData(List<Product> newProducts) {
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

    List<Product> oldItems;
    List<Product> newItems;

    public DiffCallback(
        List<Product> newItems,
        List<Product> oldItems
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

    void onItemRowClicked(Product product, boolean copy);
  }
}
