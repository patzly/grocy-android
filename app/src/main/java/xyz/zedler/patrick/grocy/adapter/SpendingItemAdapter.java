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
 * Copyright (c) 2024-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.viewmodel.StockReportSpendingsViewModel.SpendingItem;

public class SpendingItemAdapter
    extends RecyclerView.Adapter<SpendingItemAdapter.ViewHolder> {

  private final List<SpendingItem> items;
  private final Formatter formatter;

  public interface Formatter {
    String format(double amount);
  }

  public SpendingItemAdapter(List<SpendingItem> items, Formatter formatter) {
    this.items = items;
    this.formatter = formatter;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.row_spending_item, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    SpendingItem item = items.get(position);
    holder.textName.setText(item.name);
    holder.textAmount.setText(formatter.format(item.total));
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    final TextView textName;
    final TextView textAmount;

    public ViewHolder(@NonNull View itemView) {
      super(itemView);
      textName = itemView.findViewById(R.id.text_name);
      textAmount = itemView.findViewById(R.id.text_amount);
    }
  }
}
