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
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class ShoppingListAdapter extends RecyclerView.Adapter<ShoppingListAdapter.ViewHolder> {

  private final static String TAG = ShoppingListAdapter.class.getSimpleName();

  private final List<ShoppingList> shoppingLists;
  private int selectedId;
  private final ShoppingListAdapterListener listener;
  private final boolean showActions;

  public ShoppingListAdapter(
      ShoppingListAdapterListener listener,
      boolean showActions
  ) {
    this.shoppingLists = new ArrayList<>();
    this.listener = listener;
    this.showActions = showActions;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    private final LinearLayout container;
    private final TextView name;
    private final ImageView imageSelected;
    private final MaterialButton edit;
    private final MaterialButton delete;

    public ViewHolder(View view) {
      super(view);

      container = view.findViewById(R.id.container);
      name = view.findViewById(R.id.name);
      imageSelected = view.findViewById(R.id.image_selected);
      edit = view.findViewById(R.id.edit);
      delete = view.findViewById(R.id.delete);
    }
  }

  @NonNull
  @Override
  public ShoppingListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new ShoppingListAdapter.ViewHolder(
        LayoutInflater.from(parent.getContext()).inflate(
            R.layout.row_shopping_list_selection_sheet,
            parent,
            false
        )
    );
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(
      @NonNull final ShoppingListAdapter.ViewHolder holder,
      int position
  ) {
    Context context = holder.container.getContext();
    ShoppingList shoppingList = shoppingLists.get(holder.getAdapterPosition());

    holder.name.setText(shoppingList.getName());

    if (shoppingList.getId() == selectedId) {
      holder.imageSelected.setVisibility(View.VISIBLE);
      holder.name.setTextColor(ResUtil.getColor(context, R.attr.colorOnSecondaryContainer));
      holder.container.setBackground(ViewUtil.getBgListItemSelected(context));
    } else {
      holder.imageSelected.setVisibility(View.INVISIBLE);
      holder.name.setTextColor(ResUtil.getColor(context, R.attr.colorOnSurface));
      holder.container.setOnClickListener(view -> listener.onItemRowClicked(shoppingList));
      holder.container.setBackground(ViewUtil.getRippleBgListItemSurface(context));
    }

    if (shoppingList.getId() == 1) {
      holder.delete.setVisibility(View.GONE);
    }

    holder.delete.setOnClickListener(v -> listener.onClickDelete(shoppingList));

    holder.edit.setOnClickListener(v -> listener.onClickEdit(shoppingList));

    if (!showActions) {
      holder.delete.setVisibility(View.GONE);
      holder.edit.setVisibility(View.GONE);
    }
  }

  public void updateData(List<ShoppingList> shoppingListsNew, Object selectedIdNew) {
    shoppingLists.clear();
    shoppingLists.addAll(shoppingListsNew);
    this.selectedId = selectedIdNew != null ? (Integer) selectedIdNew : -1;
    notifyDataSetChanged();
  }

  public void updateSelectedId(Object selectedIdNew) {
    this.selectedId = selectedIdNew != null ? (Integer) selectedIdNew : -1;
    notifyDataSetChanged();
  }

  @Override
  public long getItemId(int position) {
    return shoppingLists.get(position).getId();
  }

  @Override
  public int getItemCount() {
    return shoppingLists.size();
  }

  public interface ShoppingListAdapterListener {

    void onItemRowClicked(ShoppingList shoppingList);

    void onClickDelete(ShoppingList shoppingList);

    void onClickEdit(ShoppingList shoppingList);
  }
}
