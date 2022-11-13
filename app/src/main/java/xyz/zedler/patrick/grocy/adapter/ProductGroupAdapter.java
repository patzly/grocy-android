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
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class ProductGroupAdapter extends RecyclerView.Adapter<ProductGroupAdapter.ViewHolder> {

  private final static String TAG = ProductGroupAdapter.class.getSimpleName();

  private final ArrayList<ProductGroup> productGroups;
  private final int selectedId;
  private final ProductGroupAdapterListener listener;

  public ProductGroupAdapter(
      ArrayList<ProductGroup> productGroups,
      int selectedId,
      ProductGroupAdapterListener listener
  ) {
    this.productGroups = productGroups;
    this.selectedId = selectedId;
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
  public ProductGroupAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new ProductGroupAdapter.ViewHolder(
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
      @NonNull final ProductGroupAdapter.ViewHolder holder,
      int position
  ) {
    Context context = holder.linearLayoutContainer.getContext();

    ProductGroup productGroup = productGroups.get(holder.getAdapterPosition());

    // NAME

    holder.textViewName.setText(productGroup.getName());

    // SELECTED

    boolean isSelected = (productGroup.getId() != -1 && selectedId != -1
        && productGroup.getId() == selectedId)
        || selectedId == -1 && productGroup.getId() == -1;
    if (isSelected) {
      holder.imageViewSelected.setVisibility(View.VISIBLE);
      holder.textViewName.setTextColor(
          ResUtil.getColorAttr(context, R.attr.colorOnSecondaryContainer)
      );
      holder.linearLayoutContainer.setBackground(ViewUtil.getBgListItemSelected(context));
    } else {
      holder.imageViewSelected.setVisibility(View.INVISIBLE);
      holder.textViewName.setTextColor(
          ResUtil.getColorAttr(context, R.attr.colorOnSurface)
      );
      holder.linearLayoutContainer.setBackground(
          ViewUtil.getRippleBgListItemSurfaceRecyclerItem(context)
      );
      holder.linearLayoutContainer.setOnClickListener(
          view -> listener.onItemRowClicked(holder.getAdapterPosition())
      );
    }
  }

  @Override
  public int getItemCount() {
    return productGroups.size();
  }

  public interface ProductGroupAdapterListener {

    void onItemRowClicked(int position);
  }
}
