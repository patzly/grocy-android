package xyz.zedler.patrick.grocy.adapter;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020 by Patrick Zedler & Dominic Zedler
*/

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
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class StockEntryAdapter
        extends RecyclerView.Adapter<StockEntryAdapter.ViewHolder> {

    private final static String TAG = StockEntryAdapter.class.getSimpleName();

    private Context context;
    private ArrayList<StockEntry> stockEntries;
    private String selectedId;
    private StockEntryAdapterListener listener;

    public StockEntryAdapter(
            Context context,
            ArrayList<StockEntry> stockEntries,
            String selectedId,
            StockEntryAdapterListener listener
    ) {
        this.context = context;
        this.stockEntries = stockEntries;
        this.selectedId = selectedId;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout linearLayoutContainer;
        private TextView textViewName, textViewSubtitle;
        private ImageView imageViewSelected;

        public ViewHolder(View view) {
            super(view);

            linearLayoutContainer = view.findViewById(
                    R.id.linear_stock_entry_container
            );
            textViewName = view.findViewById(R.id.text_stock_entry_name);
            textViewSubtitle = view.findViewById(R.id.text_stock_entry_subtitle);
            imageViewSelected = view.findViewById(R.id.image_stock_entry_selected);
        }
    }

    @NonNull
    @Override
    public StockEntryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new StockEntryAdapter.ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.row_stock_entry,
                        parent,
                        false
                )
        );
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(
            @NonNull final StockEntryAdapter.ViewHolder holder,
            int position
    ) {
        StockEntry stockEntry = stockEntries.get(holder.getAdapterPosition());

        if(stockEntry.getStockId() == null) {
            // constructor of NO SPECIFIC/AUTO
            holder.textViewName.setText(context.getString(R.string.title_stock_entry_no_specific));
            holder.textViewSubtitle.setText(
                    context.getString(R.string.subtitle_stock_entry_no_specific)
            );
            if(selectedId == null || selectedId.equals("")) {
                holder.imageViewSelected.setVisibility(View.VISIBLE);
            }
        } else {
            DateUtil dateUtil = new DateUtil(context);

            // NAME
            holder.textViewName.setText(
                    context.getString(
                            R.string.subtitle_stock_entry_name,
                            dateUtil.getLocalizedDate(
                                    stockEntry.getPurchasedDate(),
                                    DateUtil.FORMAT_SHORT
                            )
                    )
            );

            // SUBTITLE
            String bbd = stockEntry.getBestBeforeDate().equals(Constants.DATE.NEVER_EXPIRES)
                    ? context.getString(R.string.date_unlimited)
                    : dateUtil.getLocalizedDate(
                            stockEntry.getBestBeforeDate(),
                            DateUtil.FORMAT_SHORT
            );
            holder.textViewSubtitle.setText(
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

            // SELECTED
            if(stockEntry.getStockId().equals(selectedId)) {
                holder.imageViewSelected.setVisibility(View.VISIBLE);
            }
        }

        // CONTAINER

        holder.linearLayoutContainer.setOnClickListener(
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

    public interface StockEntryAdapterListener {
        void onItemRowClicked(int position);
    }
}
