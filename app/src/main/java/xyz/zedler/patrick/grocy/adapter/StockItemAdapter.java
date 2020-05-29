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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class StockItemAdapter extends RecyclerView.Adapter<StockItemAdapter.ViewHolder> {

    private final static String TAG = StockItemAdapter.class.getSimpleName();
    private final static boolean DEBUG = false;

    private Context context;
    private ArrayList<StockItem> stockItems;
    private ArrayList<QuantityUnit> quantityUnits;
    private ArrayList<String> shoppingListProductIds;
    private StockItemAdapterListener listener;
    private int daysExpiringSoon;
    private String sortMode;

    public StockItemAdapter(
            Context context,
            ArrayList<StockItem> stockItems,
            ArrayList<QuantityUnit> quantityUnits,
            ArrayList<String> shoppingListProductIds,
            int daysExpiringSoon,
            String sortMode,
            StockItemAdapterListener listener
    ) {
        this.context = context;
        this.stockItems = stockItems;
        this.quantityUnits = quantityUnits;
        this.shoppingListProductIds = shoppingListProductIds;
        this.daysExpiringSoon = daysExpiringSoon;
        this.sortMode = sortMode;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout linearLayoutItemContainer, linearLayoutDays;
        private TextView textViewName, textViewAmount, textViewDays;
        private View iconIsOnShoppingList;

        public ViewHolder(View view) {
            super(view);

            linearLayoutItemContainer = view.findViewById(R.id.linear_stock_item_container);
            linearLayoutDays = view.findViewById(R.id.linear_stock_item_days);
            textViewName = view.findViewById(R.id.text_stock_item_name);
            textViewAmount = view.findViewById(R.id.text_stock_item_amount);
            textViewDays = view.findViewById(R.id.text_stock_item_days);
            iconIsOnShoppingList = view.findViewById(R.id.view_stock_item_on_shopping_list);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.row_stock_item,
                        parent,
                        false
                )
        );
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        StockItem stockItem = stockItems.get(holder.getAdapterPosition());

        // NAME

        holder.textViewName.setText(stockItem.getProduct().getName());

        // IS ON SHOPPING LIST

        if(shoppingListProductIds.contains(String.valueOf(stockItem.getProduct().getId()))) {
            holder.iconIsOnShoppingList.setVisibility(View.VISIBLE);
        } else {
            holder.iconIsOnShoppingList.setVisibility(View.GONE);
        }

        // AMOUNT

        QuantityUnit quantityUnit = new QuantityUnit();
        for(int i = 0; i < quantityUnits.size(); i++) {
            if(quantityUnits.get(i).getId() == stockItem.getProduct().getQuIdStock()) {
                quantityUnit = quantityUnits.get(i);
                break;
            }
        }

        if(DEBUG) Log.i(TAG, "onBindViewHolder: " + quantityUnit.getName());

        StringBuilder stringBuilderAmount = new StringBuilder(
                context.getString(
                        R.string.subtitle_amount,
                        NumUtil.trim(stockItem.getAmount()),
                        stockItem.getAmount() == 1
                                ? quantityUnit.getName()
                                : quantityUnit.getNamePlural()
                )
        );
        if(stockItem.getAmountOpened() > 0) {
            stringBuilderAmount.append(" ");
            stringBuilderAmount.append(
                    context.getString(
                            R.string.subtitle_amount_opened,
                            NumUtil.trim(stockItem.getAmountOpened())
                    )
            );
        }
        // aggregated amount
        if(stockItem.getIsAggregatedAmount() == 1) {
            stringBuilderAmount.append("  ∑ ");
            stringBuilderAmount.append(
                    context.getString(
                            R.string.subtitle_amount,
                            NumUtil.trim(stockItem.getAmountAggregated()),
                            stockItem.getAmountAggregated() == 1
                                    ? quantityUnit.getName()
                                    : quantityUnit.getNamePlural()
                    )
            );
        }
        holder.textViewAmount.setText(stringBuilderAmount);
        if(stockItem.getAmount() < stockItem.getProduct().getMinStockAmount()) {
            holder.textViewAmount.setTypeface(
                    ResourcesCompat.getFont(context, R.font.roboto_mono_medium)
            );
            holder.textViewAmount.setTextColor(
                    ContextCompat.getColor(context, R.color.retro_blue_fg)
            );
        } else {
            holder.textViewAmount.setTypeface(
                    ResourcesCompat.getFont(context, R.font.roboto_mono_regular)
            );
            holder.textViewAmount.setTextColor(
                    ContextCompat.getColor(context, R.color.on_background_secondary)
            );
        }

        // BEST BEFORE

        String date = stockItem.getBestBeforeDate();
        String days = null;
        boolean colorDays = false;
        if(date != null) days = String.valueOf(DateUtil.getDaysFromNow(date));

        if(days != null && (sortMode.equals(Constants.STOCK.SORT.BBD)
                || Integer.parseInt(days) <= daysExpiringSoon
                && !date.equals(Constants.DATE.NEVER_EXPIRES))
        ) {
            holder.linearLayoutDays.setVisibility(View.VISIBLE);
            holder.textViewDays.setText(new DateUtil(context).getHumanForDaysFromNow(date));
            if(Integer.parseInt(days) <= 5) colorDays = true;
        } else {
            holder.linearLayoutDays.setVisibility(View.GONE);
            holder.textViewDays.setText(null);
        }

        if(colorDays) {
            holder.textViewDays.setTypeface(
                    ResourcesCompat.getFont(context, R.font.roboto_mono_medium)
            );
            holder.textViewDays.setTextColor(
                    ContextCompat.getColor(
                            context, Integer.parseInt(days) < 0
                                    ? R.color.retro_red_fg
                                    : R.color.retro_yellow_fg
                    )
            );
        } else {
            holder.textViewDays.setTypeface(
                    ResourcesCompat.getFont(context, R.font.roboto_mono_regular)
            );
            holder.textViewDays.setTextColor(
                    ContextCompat.getColor(context, R.color.on_background_secondary)
            );
        }

        // CONTAINER

        holder.linearLayoutItemContainer.setOnClickListener(
                view -> listener.onItemRowClicked(holder.getAdapterPosition())
        );
    }

    public void setSortMode(String sortMode) {
        this.sortMode = sortMode;
    }

    public void updateData(ArrayList<StockItem> stockItems) {
        this.stockItems = stockItems;
    }

    @Override
    public long getItemId(int position) {
        return stockItems.get(position).getProductId();
    }

    @Override
    public int getItemCount() {
        return stockItems != null ? stockItems.size() : 0;
    }

    public interface StockItemAdapterListener {
        void onItemRowClicked(int position);
    }
}
