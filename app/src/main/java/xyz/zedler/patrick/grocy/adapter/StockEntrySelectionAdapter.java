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
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowStockEntrySelectionBinding;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class StockEntrySelectionAdapter
    extends RecyclerView.Adapter<StockEntrySelectionAdapter.ViewHolder> {

  private final static String TAG = StockEntrySelectionAdapter.class.getSimpleName();

  private final Context context;
  private final ArrayList<StockEntry> stockEntries;
  private final String selectedId;
  private final StockEntrySelectionAdapterListener listener;
  private final DateUtil dateUtil;

  public StockEntrySelectionAdapter(
      Context context,
      ArrayList<StockEntry> stockEntries,
      String selectedId,
      StockEntrySelectionAdapterListener listener
  ) {
    this.context = context;
    this.stockEntries = stockEntries;
    this.selectedId = selectedId;
    this.listener = listener;
    this.dateUtil = new DateUtil(context);
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    private final RowStockEntrySelectionBinding binding;

    public ViewHolder(RowStockEntrySelectionBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @NonNull
  @Override
  public StockEntrySelectionAdapter.ViewHolder onCreateViewHolder(
      @NonNull ViewGroup parent, int viewType
  ) {
    return new StockEntrySelectionAdapter.ViewHolder(
        RowStockEntrySelectionBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
    );
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(
      @NonNull final StockEntrySelectionAdapter.ViewHolder holder,
      int position
  ) {
    StockEntry stockEntry = stockEntries.get(holder.getAdapterPosition());

    if (stockEntry.getStockId() == null) {
      // constructor of NO SPECIFIC/AUTO
      holder.binding.name.setText(context.getString(R.string.title_stock_entry_no_specific));
      holder.binding.subtitle.setText(
          context.getString(R.string.subtitle_stock_entry_no_specific)
      );
      if (selectedId == null || selectedId.isEmpty()) {
        holder.binding.selected.setVisibility(View.VISIBLE);
      } else {
        holder.binding.selected.setVisibility(View.INVISIBLE);
      }
      holder.binding.note.setVisibility(View.GONE);
    } else {
      holder.binding.name.setText(
          context.getString(
              R.string.subtitle_stock_entry_name,
              dateUtil.getLocalizedDate(
                  stockEntry.getPurchasedDate(),
                  DateUtil.FORMAT_SHORT
              )
          )
      );

      String bbd = stockEntry.getBestBeforeDate().equals(Constants.DATE.NEVER_OVERDUE)
          ? context.getString(R.string.date_unlimited)
          : dateUtil.getLocalizedDate(
              stockEntry.getBestBeforeDate(),
              DateUtil.FORMAT_SHORT
          );
      holder.binding.subtitle.setText(
          context.getString(
              R.string.subtitle_stock_entry,
              bbd,
              NumUtil.trim(stockEntry.getAmount()),
              context.getString(
                  stockEntry.getOpen() == 0
                      ? R.string.property_not_opened
                      : R.string.property_opened
              )
          )
      );
      if (stockEntry.getNote() != null && !stockEntry.getNote().isEmpty()) {
        holder.binding.note.setText(stockEntry.getNote());
        holder.binding.note.setVisibility(View.VISIBLE);
      } else {
        holder.binding.note.setVisibility(View.GONE);
      }

      if (stockEntry.getStockId().equals(selectedId)) {
        holder.binding.selected.setVisibility(View.VISIBLE);
      } else {
        holder.binding.selected.setVisibility(View.INVISIBLE);
      }
    }

    holder.binding.container.setOnClickListener(
        view -> listener.onItemRowClicked(holder.getAdapterPosition())
    );
  }

  @Override
  public long getItemId(int position) {
    return stockEntries.get(position).getId();
  }

  @Override
  public int getItemCount() {
    return stockEntries.size();
  }

  public interface StockEntrySelectionAdapterListener {

    void onItemRowClicked(int position);
  }
}
