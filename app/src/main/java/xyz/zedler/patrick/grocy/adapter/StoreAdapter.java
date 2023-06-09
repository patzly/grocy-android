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
import android.content.Context;
import android.content.res.ColorStateList;
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
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.ViewUtil;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.ViewHolder> {

  private final static String TAG = StoreAdapter.class.getSimpleName();

  private final ArrayList<Store> stores;
  private final int selectedId;
  private final boolean noneSelectable;
  private final boolean displayPinButtons;
  private final int currentPinId;
  private final StoreAdapterListener listener;

  public StoreAdapter(
      ArrayList<Store> stores,
      int selectedId,
      boolean noneSelectable,
      boolean displayPinButtons,
      int currentPinId,
      StoreAdapterListener listener
  ) {
    this.stores = stores;
    this.selectedId = selectedId;
    this.noneSelectable = noneSelectable;
    this.displayPinButtons = displayPinButtons;
    this.currentPinId = currentPinId;
    this.listener = listener;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    private final LinearLayout linearLayoutContainer;
    private final TextView textViewName;
    private final ImageView imageViewSelected;
    private final MaterialButton buttonPin;

    public ViewHolder(View view) {
      super(view);

      linearLayoutContainer = view.findViewById(R.id.linear_master_edit_selection_container);
      textViewName = view.findViewById(R.id.text_master_edit_selection_name);
      imageViewSelected = view.findViewById(R.id.image_master_edit_selection_selected);
      buttonPin = view.findViewById(R.id.button_pin);
    }
  }

  @NonNull
  @Override
  public StoreAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new StoreAdapter.ViewHolder(
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
      @NonNull final StoreAdapter.ViewHolder holder,
      int position
  ) {
    Context context = holder.linearLayoutContainer.getContext();
    holder.linearLayoutContainer.setBackground(ViewUtil.getRippleBgListItemSurface(context));

    Store store = stores.get(holder.getAdapterPosition());

    // NAME

    holder.textViewName.setText(store.getName());

    // SELECTED

    boolean isSelected = store.getId() == selectedId;
    holder.imageViewSelected.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
    if (isSelected) {
      holder.linearLayoutContainer.setBackground(ViewUtil.getBgListItemSelected(context));
    }

    Context ctx = holder.linearLayoutContainer.getContext();
    holder.buttonPin.setVisibility(displayPinButtons && store.getId() != -1
        ? View.VISIBLE : View.GONE);
    holder.buttonPin.setOnClickListener(
        view -> listener.onItemRowClicked(store, true)
    );
    holder.buttonPin.setIconTint(ColorStateList.valueOf(store.getId() == currentPinId
        ? ResUtil.getColorAttr(ctx, R.attr.colorPrimary)
        : ResUtil.getColorAttr(ctx, R.attr.colorOnSurfaceVariant, 0.6f)));

    if (noneSelectable) {
      holder.linearLayoutContainer.setEnabled(false);
    }

    // CONTAINER

    holder.linearLayoutContainer.setOnClickListener(
        view -> listener.onItemRowClicked(store, false)
    );
  }

  @Override
  public long getItemId(int position) {
    return stores.get(position).getId();
  }

  @Override
  public int getItemCount() {
    return stores.size();
  }

  public interface StoreAdapterListener {

    void onItemRowClicked(Store store, boolean pinClicked);
  }
}
