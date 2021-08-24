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
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowFilterChipsBinding;
import xyz.zedler.patrick.grocy.databinding.RowStockItemBinding;
import xyz.zedler.patrick.grocy.model.HorizontalFilterBarMultiProduct;
import xyz.zedler.patrick.grocy.model.HorizontalFilterBarSingleProduct;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.util.AmountUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.view.FilterChip;
import xyz.zedler.patrick.grocy.view.InputChip;
import xyz.zedler.patrick.grocy.viewmodel.StockOverviewViewModel;

public class StockOverviewItemAdapter extends
    RecyclerView.Adapter<StockOverviewItemAdapter.ViewHolder> {

  private final static String TAG = StockOverviewItemAdapter.class.getSimpleName();
  private final static boolean DEBUG = false;

  private Context context;
  private final ArrayList<StockItem> stockItems;
  private final ArrayList<String> shoppingListItemsProductIds;
  private final HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private final PluralUtil pluralUtil;
  private final ArrayList<Integer> missingItemsProductIds;
  private final StockOverviewItemAdapterListener listener;
  private final HorizontalFilterBarSingleProduct horizontalFilterBarSingleProduct;
  private final HorizontalFilterBarMultiProduct horizontalFilterBarMultiProduct;
  private final boolean showDateTracking;
  private final int daysExpiringSoon;
  private String sortMode;

  public StockOverviewItemAdapter(
      Context context,
      ArrayList<StockItem> stockItems,
      ArrayList<String> shoppingListItemsProductIds,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      ArrayList<Integer> missingItemsProductIds,
      StockOverviewItemAdapterListener listener,
      HorizontalFilterBarSingleProduct horizontalFilterBarSingleProduct,
      HorizontalFilterBarMultiProduct horizontalFilterBarMultiProduct,
      int itemsDueCountInitial,
      int itemsOverdueCountInitial,
      int itemsExpiredCountInitial,
      int itemsMissingCountInitial,
      int itemsInStockCountInitial,
      boolean showDateTracking,
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
    this.horizontalFilterBarSingleProduct = horizontalFilterBarSingleProduct;
    this.horizontalFilterBarMultiProduct = horizontalFilterBarMultiProduct;
    this.horizontalFilterBarSingleProduct
        .setItemsCount(HorizontalFilterBarSingleProduct.DUE_NEXT, itemsDueCountInitial);
    this.horizontalFilterBarSingleProduct
        .setItemsCount(HorizontalFilterBarSingleProduct.OVERDUE, itemsOverdueCountInitial);
    this.horizontalFilterBarSingleProduct
        .setItemsCount(HorizontalFilterBarSingleProduct.EXPIRED, itemsExpiredCountInitial);
    this.horizontalFilterBarSingleProduct
        .setItemsCount(HorizontalFilterBarSingleProduct.MISSING, itemsMissingCountInitial);
    this.horizontalFilterBarSingleProduct
        .setItemsCount(HorizontalFilterBarSingleProduct.IN_STOCK, itemsInStockCountInitial);
    this.showDateTracking = showDateTracking;
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

  public static class FilterSingleRowViewHolder extends ViewHolder {

    private final WeakReference<Context> weakContext;
    private final FilterChip chipDueNext;
    private FilterChip chipOverdue;
    private FilterChip chipExpired;
    private FilterChip chipMissing;
    private FilterChip chipInStock;
    private final HorizontalFilterBarSingleProduct horizontalFilterBarSingleProduct;

    public FilterSingleRowViewHolder(
        RowFilterChipsBinding binding,
        Context context,
        HorizontalFilterBarSingleProduct horizontalFilterBarSingleProduct
    ) {
      super(binding.getRoot());

      this.horizontalFilterBarSingleProduct = horizontalFilterBarSingleProduct;
      weakContext = new WeakReference<>(context);
      chipDueNext = new FilterChip(
          context,
          R.color.retro_yellow_bg,
          context.getResources().getQuantityString(R.plurals.msg_due_products,
              horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.DUE_NEXT),
              horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.DUE_NEXT)),
          () -> {
            FilterChip.changeStateToInactive(chipOverdue, chipExpired, chipMissing, chipInStock);
            horizontalFilterBarSingleProduct.setSingleFilterActive(HorizontalFilterBarSingleProduct.DUE_NEXT);
          },
          horizontalFilterBarSingleProduct::resetAllFilters
      );
      chipOverdue = new FilterChip(
          context,
          R.color.retro_dirt,
          context.getResources().getQuantityString(R.plurals.msg_overdue_products,
              horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.OVERDUE),
              horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.OVERDUE)),
          () -> {
            FilterChip.changeStateToInactive(chipDueNext, chipExpired, chipMissing, chipInStock);
            horizontalFilterBarSingleProduct.setSingleFilterActive(HorizontalFilterBarSingleProduct.OVERDUE);
          },
          horizontalFilterBarSingleProduct::resetAllFilters
      );
      chipExpired = new FilterChip(
          context,
          R.color.retro_red_bg_black,
          context.getResources().getQuantityString(R.plurals.msg_expired_products,
              horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.EXPIRED),
              horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.EXPIRED)),
          () -> {
            FilterChip.changeStateToInactive(chipDueNext, chipOverdue, chipMissing, chipInStock);
            horizontalFilterBarSingleProduct.setSingleFilterActive(HorizontalFilterBarSingleProduct.EXPIRED);
          },
          horizontalFilterBarSingleProduct::resetAllFilters
      );
      chipMissing = new FilterChip(
          context,
          R.color.retro_blue_bg,
          context.getResources().getQuantityString(R.plurals.msg_missing_products,
              horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.MISSING),
              horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.MISSING)),
          () -> {
            FilterChip.changeStateToInactive(chipDueNext, chipOverdue, chipExpired, chipInStock);
            horizontalFilterBarSingleProduct.setSingleFilterActive(HorizontalFilterBarSingleProduct.MISSING);
          },
          horizontalFilterBarSingleProduct::resetAllFilters
      );
      chipInStock = new FilterChip(
          context,
          R.color.retro_green_bg_black,
          context.getResources().getQuantityString(R.plurals.msg_in_stock_products,
              horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.IN_STOCK),
              horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.IN_STOCK)),
          () -> {
            FilterChip.changeStateToInactive(chipDueNext, chipOverdue, chipExpired, chipMissing);
            horizontalFilterBarSingleProduct.setSingleFilterActive(HorizontalFilterBarSingleProduct.IN_STOCK);
          },
          horizontalFilterBarSingleProduct::resetAllFilters
      );
      binding.container.addView(chipDueNext);
      binding.container.addView(chipOverdue);
      binding.container.addView(chipExpired);
      binding.container.addView(chipMissing);
      binding.container.addView(chipInStock);
    }

    public void bind() {
      if (horizontalFilterBarSingleProduct.isNoFilterActive()) {
        FilterChip
            .changeStateToInactive(chipDueNext, chipOverdue, chipExpired, chipMissing, chipInStock);
      } else if (horizontalFilterBarSingleProduct.isFilterActive(HorizontalFilterBarSingleProduct.DUE_NEXT)) {
        FilterChip.changeStateToInactive(chipOverdue, chipExpired, chipMissing, chipInStock);
        FilterChip.changeStateToActive(chipDueNext);
      } else if (horizontalFilterBarSingleProduct.isFilterActive(HorizontalFilterBarSingleProduct.OVERDUE)) {
        FilterChip.changeStateToInactive(chipDueNext, chipExpired, chipMissing, chipInStock);
        FilterChip.changeStateToActive(chipOverdue);
      } else if (horizontalFilterBarSingleProduct.isFilterActive(HorizontalFilterBarSingleProduct.EXPIRED)) {
        FilterChip.changeStateToInactive(chipDueNext, chipOverdue, chipMissing, chipInStock);
        FilterChip.changeStateToActive(chipExpired);
      } else if (horizontalFilterBarSingleProduct.isFilterActive(HorizontalFilterBarSingleProduct.MISSING)) {
        FilterChip.changeStateToInactive(chipDueNext, chipOverdue, chipExpired, chipInStock);
        FilterChip.changeStateToActive(chipMissing);
      } else if (horizontalFilterBarSingleProduct.isFilterActive(HorizontalFilterBarSingleProduct.IN_STOCK)) {
        FilterChip.changeStateToInactive(chipDueNext, chipOverdue, chipExpired, chipMissing);
        FilterChip.changeStateToActive(chipInStock);
      }
      chipDueNext.setText(weakContext.get().getResources().getQuantityString(
          R.plurals.msg_due_products,
          horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.DUE_NEXT),
          horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.DUE_NEXT)
      ));
      chipOverdue.setText(weakContext.get().getResources().getQuantityString(
          R.plurals.msg_overdue_products,
          horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.OVERDUE),
          horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.OVERDUE)
      ));
      chipExpired.setText(weakContext.get().getResources().getQuantityString(
          R.plurals.msg_expired_products,
          horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.EXPIRED),
          horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.EXPIRED)
      ));
      chipMissing.setText(weakContext.get().getResources().getQuantityString(
          R.plurals.msg_missing_products,
          horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.MISSING),
          horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.MISSING)
      ));
      chipInStock.setText(weakContext.get().getResources().getQuantityString(
          R.plurals.msg_in_stock_products,
          horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.IN_STOCK),
          horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.IN_STOCK)
      ));
    }
  }

  public static class FilterMultiRowViewHolder extends ViewHolder {

    private final WeakReference<Context> weakContext;
    private final RowFilterChipsBinding binding;
    private InputChip chipProductGroup;
    private InputChip chipLocation;
    private final HorizontalFilterBarMultiProduct horizontalFilterBarMultiProduct;

    public FilterMultiRowViewHolder(
        RowFilterChipsBinding binding,
        Context context,
        HorizontalFilterBarMultiProduct horizontalFilterBarMultiProduct
    ) {
      super(binding.getRoot());
      this.binding = binding;
      this.horizontalFilterBarMultiProduct = horizontalFilterBarMultiProduct;
      weakContext = new WeakReference<>(context);
    }

    public void bind() {
      HorizontalFilterBarMultiProduct.Filter filterPg = horizontalFilterBarMultiProduct
          .getFilter(HorizontalFilterBarMultiProduct.PRODUCT_GROUP);
      if (filterPg != null && chipProductGroup == null) {
        chipProductGroup = new InputChip(
            weakContext.get(),
            filterPg.getObjectName(),
            R.drawable.ic_round_category,
            true,
            () -> {
              horizontalFilterBarMultiProduct.removeFilter(HorizontalFilterBarMultiProduct.PRODUCT_GROUP);
              chipProductGroup = null;
            }
        );
        binding.container.addView(chipProductGroup);
      } else if (filterPg != null) {
        chipProductGroup.setText(filterPg.getObjectName());
      }

      HorizontalFilterBarMultiProduct.Filter filterLoc = horizontalFilterBarMultiProduct
          .getFilter(HorizontalFilterBarMultiProduct.LOCATION);
      if (filterLoc != null && chipLocation == null) {
        chipLocation = new InputChip(
            weakContext.get(),
            filterLoc.getObjectName(),
            R.drawable.ic_round_place,
            true,
            () -> {
              horizontalFilterBarMultiProduct.removeFilter(HorizontalFilterBarMultiProduct.LOCATION);
              chipLocation = null;
            }
        );
        binding.container.addView(chipLocation);
      } else if (filterLoc != null) {
        chipLocation.setText(filterLoc.getObjectName());
      }
    }
  }

  @Override
  public int getItemViewType(int position) {
    if (position == 0) {
      return -2; // filter single row
    }
    if (position == 1) {
      return -1; // filter multi row
    }
    return 0;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == -2) { // filter single row
      RowFilterChipsBinding binding = RowFilterChipsBinding.inflate(
          LayoutInflater.from(parent.getContext()),
          parent,
          false
      );
      return new FilterSingleRowViewHolder(
          binding,
          context,
          horizontalFilterBarSingleProduct
      );
    } else if (viewType == -1) { // filter multi row
      RowFilterChipsBinding binding = RowFilterChipsBinding.inflate(
          LayoutInflater.from(parent.getContext()),
          parent,
          false
      );
      return new FilterMultiRowViewHolder(
          binding,
          context,
          horizontalFilterBarMultiProduct
      );
    } else {
      return new StockItemViewHolder(RowStockItemBinding.inflate(
          LayoutInflater.from(parent.getContext()),
          parent,
          false
      ));
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int positionDoNotUse) {

    int position = viewHolder.getAdapterPosition();
    int movedPosition = position - 2;

    if (viewHolder.getItemViewType() == -2) { // Filter single row
      ((FilterSingleRowViewHolder) viewHolder).bind();
      return;
    } else if (viewHolder.getItemViewType() == -1) { // Filter multi row
      ((FilterMultiRowViewHolder) viewHolder).bind();
      return;
    }

    StockItem stockItem = stockItems.get(movedPosition);
    StockItemViewHolder holder = (StockItemViewHolder) viewHolder;

    // NAME

    holder.binding.textName.setText(stockItem.getProduct().getName());

    // IS ON SHOPPING LIST

    if (shoppingListItemsProductIds.contains(String.valueOf(stockItem.getProduct().getId()))) {
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
    } else if (days != null && (sortMode.equals(StockOverviewViewModel.SORT_DUE_DATE)
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
    return stockItems.size() + 2;
  }

  public interface StockOverviewItemAdapterListener {

    void onItemRowClicked(StockItem stockItem);
  }

  public void updateData(
      ArrayList<StockItem> newList,
      ArrayList<String> shoppingListItemsProductIds,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      ArrayList<Integer> missingItemsProductIds,
      int itemsDueCount,
      int itemsOverdueCount,
      int itemsExpiredCount,
      int itemsMissingCount,
      int itemsInStockCount,
      String sortMode
  ) {
    if (horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.DUE_NEXT) != itemsDueCount
        || horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.OVERDUE)
        != itemsOverdueCount
        || horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.EXPIRED)
        != itemsExpiredCount
        || horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.MISSING)
        != itemsMissingCount
        || horizontalFilterBarSingleProduct.getItemsCount(HorizontalFilterBarSingleProduct.IN_STOCK)
        != itemsInStockCount) {
      horizontalFilterBarSingleProduct.setItemsCount(HorizontalFilterBarSingleProduct.DUE_NEXT, itemsDueCount);
      horizontalFilterBarSingleProduct.setItemsCount(HorizontalFilterBarSingleProduct.OVERDUE, itemsOverdueCount);
      horizontalFilterBarSingleProduct.setItemsCount(HorizontalFilterBarSingleProduct.EXPIRED, itemsExpiredCount);
      horizontalFilterBarSingleProduct.setItemsCount(HorizontalFilterBarSingleProduct.MISSING, itemsMissingCount);
      horizontalFilterBarSingleProduct
          .setItemsCount(HorizontalFilterBarSingleProduct.IN_STOCK, itemsInStockCount);
      notifyItemChanged(0); // update viewHolder with filter row
    }

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
    diffResult.dispatchUpdatesTo(new AdapterListUpdateCallback(this));
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

  /**
   * Custom ListUpdateCallback that dispatches update events to the given adapter with offset of 1,
   * because the first item is the filter row.
   */
  public static final class AdapterListUpdateCallback implements ListUpdateCallback {

    @NonNull
    private final StockOverviewItemAdapter mAdapter;

    public AdapterListUpdateCallback(@NonNull StockOverviewItemAdapter adapter) {
      mAdapter = adapter;
    }

    @Override
    public void onInserted(int position, int count) {
      mAdapter.notifyItemRangeInserted(position + 2, count);
    }

    @Override
    public void onRemoved(int position, int count) {
      mAdapter.notifyItemRangeRemoved(position + 2, count);
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {
      mAdapter.notifyItemMoved(fromPosition + 2, toPosition + 2);
    }

    @Override
    public void onChanged(int position, int count, Object payload) {
      mAdapter.notifyItemRangeChanged(position + 2, count, payload);
    }
  }
}
