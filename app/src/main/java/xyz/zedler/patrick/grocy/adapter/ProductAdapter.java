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
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

  private final static String TAG = ProductAdapter.class.getSimpleName();

  private final ArrayList<Product> products;
  private final int selectedId;
  private final ProductAdapterListener listener;

  public ProductAdapter(
      ArrayList<Product> products,
      Object selectedId,
      ProductAdapterListener listener
  ) {
    this.products = products;
    this.selectedId = selectedId != null ? (Integer) selectedId : -1;
    this.listener = listener;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    private final LinearLayout linearLayoutContainer;
    private final TextView textViewName;
    private final ImageView imageViewSelected;

    public ViewHolder(View view) {
      super(view);

      linearLayoutContainer = view.findViewById(R.id.linear_master_edit_selection_container);
      textViewName = view.findViewById(R.id.text_master_edit_selection_name);
      imageViewSelected = view.findViewById(R.id.image_master_edit_selection_selected);
    }
  }

  @NonNull
  @Override
  public ProductAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new ProductAdapter.ViewHolder(
        LayoutInflater.from(parent.getContext()).inflate(
            R.layout.row_master_edit_selection_sheet,
            parent,
            false
        )
    );
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(
      @NonNull final ProductAdapter.ViewHolder holder,
      int position
  ) {
    Context context = holder.linearLayoutContainer.getContext();
    Product product = products.get(holder.getAdapterPosition());

    // NAME

    holder.textViewName.setText(product.getName());

    // SELECTED

    if (product.getId() == selectedId) {
      holder.imageViewSelected.setVisibility(View.VISIBLE);
      holder.linearLayoutContainer.setBackground(ViewUtil.getBgListItemSelected(context));
    } else {
      holder.linearLayoutContainer.setBackground(ViewUtil.getRippleBgListItemSurface(context));
    }

    // CONTAINER

    holder.linearLayoutContainer.setOnClickListener(
        view -> listener.onItemRowClicked(holder.getAdapterPosition())
    );
  }

  @Override
  public long getItemId(int position) {
    return products.get(position).getId();
  }

  @Override
  public int getItemCount() {
    return products.size();
  }

  public interface ProductAdapterListener {

    void onItemRowClicked(int position);
  }
}
