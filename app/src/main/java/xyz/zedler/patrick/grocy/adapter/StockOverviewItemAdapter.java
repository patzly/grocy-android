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
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowStockItemBinding;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataStockSort;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.util.AmountUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;

public class StockOverviewItemAdapter extends
    RecyclerView.Adapter<StockOverviewItemAdapter.ViewHolder> {

  private final static String TAG = StockOverviewItemAdapter.class.getSimpleName();

  private Context context;
  private final ArrayList<StockItem> stockItems;
  private final ArrayList<String> shoppingListItemsProductIds;
  private final HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private final PluralUtil pluralUtil;
  private final ArrayList<Integer> missingItemsProductIds;
  private final StockOverviewItemAdapterListener listener;
  private final boolean showDateTracking;
  private final boolean shoppingListFeatureEnabled;
  private final int daysExpiringSoon;
  private String sortMode;

  public StockOverviewItemAdapter(
      Context context,
      ArrayList<StockItem> stockItems,
      ArrayList<String> shoppingListItemsProductIds,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      ArrayList<Integer> missingItemsProductIds,
      StockOverviewItemAdapterListener listener,
      boolean showDateTracking,
      boolean shoppingListFeatureEnabled,
      int daysExpiringSoon,
      String sortMode
  ) {
    this.context = context;
    this.stockItems = new ArrayList<>(stockItems);
    this.shoppingListItemsProductIds = new ArrayList<>(shoppingListItemsProductIds);
    this.quantityUnitHashMap = new HashMap<>(quantityUnitHashMap);
    this.pluralUtil = new PluralUtil(context);
    this.missingItemsProductIds = new ArrayList<>(missingItemsProductIds);
    this.listener = listener;
    this.showDateTracking = showDateTracking;
    this.shoppingListFeatureEnabled = shoppingListFeatureEnabled;
    this.daysExpiringSoon = daysExpiringSoon;
    this.sortMode = sortMode;
  }

  @Override
  public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
    super.onDetachedFromRecyclerView(recyclerView);
    this.context = null;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View view) {
      super(view);
    }
  }

  public static class StockItemViewHolder extends ViewHolder {

    private final RowStockItemBinding binding;

    public StockItemViewHolder(RowStockItemBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new StockItemViewHolder(RowStockItemBinding.inflate(
        LayoutInflater.from(parent.getContext()),
        parent,
        false
    ));
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int positionDoNotUse) {
    StockItem stockItem = stockItems.get(viewHolder.getAdapterPosition());
    StockItemViewHolder holder = (StockItemViewHolder) viewHolder;

    // NAME

    holder.binding.textName.setText(stockItem.getProduct().getName());

    // IS ON SHOPPING LIST

    if (shoppingListItemsProductIds.contains(String.valueOf(stockItem.getProduct().getId()))
        && shoppingListFeatureEnabled) {
      holder.binding.viewOnShoppingList.setVisibility(View.VISIBLE);
    } else {
      holder.binding.viewOnShoppingList.setVisibility(View.GONE);
    }

    // AMOUNT

    QuantityUnit quantityUnit = quantityUnitHashMap.get(stockItem.getProduct().getQuIdStockInt());
    holder.binding.textAmount.setText(
        AmountUtil.getStockAmountInfo(context, pluralUtil, stockItem, quantityUnit)
    );
    if (missingItemsProductIds.contains(stockItem.getProductId())) {
      holder.binding.textAmount.setTypeface(
          ResourcesCompat.getFont(context, R.font.jost_medium)
      );
      holder.binding.textAmount.setTextColor(
          ContextCompat.getColor(context, R.color.retro_blue_fg)
      );
    } else {
      holder.binding.textAmount.setTypeface(
          ResourcesCompat.getFont(context, R.font.jost_book)
      );
      holder.binding.textAmount.setTextColor(
          ContextCompat.getColor(context, R.color.on_background_secondary)
      );
    }

    // BEST BEFORE

    String date = stockItem.getBestBeforeDate();
    String days = null;
    boolean colorDays = false;
    if (date != null) {
      days = String.valueOf(DateUtil.getDaysFromNow(date));
    }

    if (!showDateTracking) {
      holder.binding.linearDays.setVisibility(View.GONE);
    } else if (days != null && (sortMode.equals(FilterChipLiveDataStockSort.SORT_DUE_DATE)
        || Integer.parseInt(days) <= daysExpiringSoon
        && !date.equals(Constants.DATE.NEVER_OVERDUE))
    ) {
      holder.binding.linearDays.setVisibility(View.VISIBLE);
      holder.binding.textDays.setText(new DateUtil(context).getHumanForDaysFromNow(date));
      if (Integer.parseInt(days) <= daysExpiringSoon) {
        colorDays = true;
      }
    } else {
      holder.binding.linearDays.setVisibility(View.GONE);
      holder.binding.textDays.setText(null);
    }

    if (colorDays) {
      holder.binding.textDays.setTypeface(
          ResourcesCompat.getFont(context, R.font.jost_medium)
      );
      @ColorRes int color;
      if (Integer.parseInt(days) >= 0) {
        color = R.color.retro_yellow_fg;
      } else if (stockItem.getDueTypeInt() == StockItem.DUE_TYPE_BEST_BEFORE) {
        color = R.color.retro_dirt_fg;
      } else {
        color = R.color.retro_red_fg;
      }
      holder.binding.textDays.setTextColor(ContextCompat.getColor(context, color));
    } else {
      holder.binding.textDays.setTypeface(
          ResourcesCompat.getFont(context, R.font.jost_book)
      );
      holder.binding.textDays.setTextColor(
          ContextCompat.getColor(context, R.color.on_background_secondary)
      );
    }

    // CONTAINER

    holder.binding.linearContainer.setOnClickListener(
        view -> listener.onItemRowClicked(stockItem)
    );
  }

  @Override
  public int getItemCount() {
    return stockItems.size();
  }

  public interface StockOverviewItemAdapterListener {

    void onItemRowClicked(StockItem stockItem);
  }

  public void updateData(
      ArrayList<StockItem> newList,
      ArrayList<String> shoppingListItemsProductIds,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      ArrayList<Integer> missingItemsProductIds,
      String sortMode
  ) {
    StockOverviewItemAdapter.DiffCallback diffCallback = new StockOverviewItemAdapter.DiffCallback(
        this.stockItems,
        newList,
        this.shoppingListItemsProductIds,
        shoppingListItemsProductIds,
        this.quantityUnitHashMap,
        quantityUnitHashMap,
        this.missingItemsProductIds,
        missingItemsProductIds,
        this.sortMode,
        sortMode
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.stockItems.clear();
    this.stockItems.addAll(newList);
    this.shoppingListItemsProductIds.clear();
    this.shoppingListItemsProductIds.addAll(shoppingListItemsProductIds);
    this.quantityUnitHashMap.clear();
    this.quantityUnitHashMap.putAll(quantityUnitHashMap);
    this.missingItemsProductIds.clear();
    this.missingItemsProductIds.addAll(missingItemsProductIds);
    this.sortMode = sortMode;
    diffResult.dispatchUpdatesTo(this);
  }

  static class DiffCallback extends DiffUtil.Callback {

    ArrayList<StockItem> oldItems;
    ArrayList<StockItem> newItems;
    ArrayList<String> shoppingListItemsProductIdsOld;
    ArrayList<String> shoppingListItemsProductIdsNew;
    HashMap<Integer, QuantityUnit> quantityUnitHashMapOld;
    HashMap<Integer, QuantityUnit> quantityUnitHashMapNew;
    ArrayList<Integer> missingProductIdsOld;
    ArrayList<Integer> missingProductIdsNew;
    String sortModeOld;
    String sortModeNew;

    public DiffCallback(
        ArrayList<StockItem> oldItems,
        ArrayList<StockItem> newItems,
        ArrayList<String> shoppingListItemsProductIdsOld,
        ArrayList<String> shoppingListItemsProductIdsNew,
        HashMap<Integer, QuantityUnit> quantityUnitHashMapOld,
        HashMap<Integer, QuantityUnit> quantityUnitHashMapNew,
        ArrayList<Integer> missingProductIdsOld,
        ArrayList<Integer> missingProductIdsNew,
        String sortModeOld,
        String sortModeNew
    ) {
      this.newItems = newItems;
      this.oldItems = oldItems;
      this.shoppingListItemsProductIdsOld = shoppingListItemsProductIdsOld;
      this.shoppingListItemsProductIdsNew = shoppingListItemsProductIdsNew;
      this.quantityUnitHashMapOld = quantityUnitHashMapOld;
      this.quantityUnitHashMapNew = quantityUnitHashMapNew;
      this.missingProductIdsOld = missingProductIdsOld;
      this.missingProductIdsNew = missingProductIdsNew;
      this.sortModeOld = sortModeOld;
      this.sortModeNew = sortModeNew;
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
      StockItem newItem = newItems.get(newItemPos);
      StockItem oldItem = oldItems.get(oldItemPos);
      if (!compareContent) {
        return newItem.getProductId() == oldItem.getProductId();
      }

      if (!sortModeOld.equals(sortModeNew)) {
        return false;
      }

      if (!newItem.getProduct().equals(oldItem.getProduct())) {
        return false;
      }

      QuantityUnit quOld = quantityUnitHashMapOld.get(oldItem.getProduct().getQuIdStockInt());
      QuantityUnit quNew = quantityUnitHashMapNew.get(newItem.getProduct().getQuIdStockInt());
      if (quOld == null && quNew != null
          || quOld != null && quNew != null && quOld.getId() != quNew.getId()
      ) {
        return false;
      }

      boolean isOnShoppingListOld = shoppingListItemsProductIdsOld
          .contains(String.valueOf(oldItem.getProduct().getId()));
      boolean isOnShoppingListNew = shoppingListItemsProductIdsNew
          .contains(String.valueOf(newItem.getProduct().getId()));
      if (isOnShoppingListNew != isOnShoppingListOld) {
        return false;
      }

      boolean missingOld = missingProductIdsOld.contains(oldItem.getProductId());
      boolean missingNew = missingProductIdsNew.contains(newItem.getProductId());
      if (missingOld != missingNew) {
        return false;
      }

      return newItem.equals(oldItem);
    }
  }
}
