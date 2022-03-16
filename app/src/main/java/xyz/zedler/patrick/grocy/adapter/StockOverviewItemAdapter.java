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
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowShoppingListGroupBinding;
import xyz.zedler.patrick.grocy.databinding.RowStockItemBinding;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataStockExtraField;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataStockGrouping;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataStockSort;
import xyz.zedler.patrick.grocy.model.GroupHeader;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.util.AmountUtil;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;

public class StockOverviewItemAdapter extends
    RecyclerView.Adapter<StockOverviewItemAdapter.ViewHolder> {

  private final static String TAG = StockOverviewItemAdapter.class.getSimpleName();

  private final ArrayList<GroupedListItem> groupedListItems;
  private final ArrayList<String> shoppingListItemsProductIds;
  private final HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private final HashMap<Integer, String> productAveragePriceHashMap;
  private final PluralUtil pluralUtil;
  private final ArrayList<Integer> missingItemsProductIds;
  private final StockOverviewItemAdapterListener listener;
  private final boolean showDateTracking;
  private final boolean shoppingListFeatureEnabled;
  private final int daysExpiringSoon;
  private String sortMode;
  private boolean sortAscending;
  private String groupingMode;
  private String extraField;
  private final DateUtil dateUtil;
  private final String currency;

  public StockOverviewItemAdapter(
      Context context,
      ArrayList<StockItem> stockItems,
      ArrayList<String> shoppingListItemsProductIds,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      HashMap<Integer, String> productAveragePriceHashMap,
      HashMap<Integer, ProductGroup> productGroupHashMap,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, Location> locationHashMap,
      ArrayList<Integer> missingItemsProductIds,
      StockOverviewItemAdapterListener listener,
      boolean showDateTracking,
      boolean shoppingListFeatureEnabled,
      int daysExpiringSoon,
      String currency,
      String sortMode,
      boolean sortAscending,
      String groupingMode,
      String extraField
  ) {
    this.shoppingListItemsProductIds = new ArrayList<>(shoppingListItemsProductIds);
    this.quantityUnitHashMap = new HashMap<>(quantityUnitHashMap);
    this.productAveragePriceHashMap = new HashMap<>(productAveragePriceHashMap);
    this.pluralUtil = new PluralUtil(context);
    this.missingItemsProductIds = new ArrayList<>(missingItemsProductIds);
    this.listener = listener;
    this.showDateTracking = showDateTracking;
    this.shoppingListFeatureEnabled = shoppingListFeatureEnabled;
    this.daysExpiringSoon = daysExpiringSoon;
    this.currency = currency;
    this.dateUtil = new DateUtil(context);
    this.sortMode = sortMode;
    this.sortAscending = sortAscending;
    this.groupingMode = groupingMode;
    this.extraField = extraField;
    this.groupedListItems = getGroupedListItems(context, stockItems,
        productGroupHashMap, productHashMap, locationHashMap, currency, dateUtil, sortMode,
        sortAscending, groupingMode);
  }

  static ArrayList<GroupedListItem> getGroupedListItems(
      Context context,
      ArrayList<StockItem> stockItems,
      HashMap<Integer, ProductGroup> productGroupHashMap,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, Location> locationHashMap,
      String currency,
      DateUtil dateUtil,
      String sortMode,
      boolean sortAscending,
      String groupingMode
  ) {
    if (groupingMode.equals(FilterChipLiveDataStockGrouping.GROUPING_NONE)) {
      sortStockItems(context, stockItems, sortMode, sortAscending);
      return new ArrayList<>(stockItems);
    }
    HashMap<String, ArrayList<StockItem>> stockItemsGroupedHashMap = new HashMap<>();
    ArrayList<StockItem> ungroupedItems = new ArrayList<>();
    for (StockItem stockItem : stockItems) {
      String groupName = null;
      if (groupingMode.equals(FilterChipLiveDataStockGrouping.GROUPING_PRODUCT_GROUP)
          && NumUtil.isStringInt(stockItem.getProduct().getProductGroupId())
      ) {
        int productGroupId = Integer.parseInt(stockItem.getProduct().getProductGroupId());
        ProductGroup productGroup = productGroupHashMap.get(productGroupId);
        groupName = productGroup != null ? productGroup.getName() : null;
      } else if (groupingMode.equals(FilterChipLiveDataStockGrouping.GROUPING_VALUE)) {
        groupName = NumUtil.trimPrice(stockItem.getValueDouble());
      } else if (groupingMode.equals(FilterChipLiveDataStockGrouping.GROUPING_CALORIES_PER_STOCK)) {
        groupName = NumUtil.isStringDouble(stockItem.getProduct().getCalories())
            ? stockItem.getProduct().getCalories() : null;
      } else if (groupingMode.equals(FilterChipLiveDataStockGrouping.GROUPING_CALORIES)) {
        groupName = NumUtil.isStringDouble(stockItem.getProduct().getCalories())
            ? NumUtil.trim(Double.parseDouble(stockItem.getProduct().getCalories())
            * stockItem.getAmountDouble()) : null;
      } else if (groupingMode.equals(FilterChipLiveDataStockGrouping.GROUPING_DUE_DATE)) {
        groupName = stockItem.getBestBeforeDate();
        if (groupName != null && !groupName.isEmpty()) {
          groupName += "  " + dateUtil.getHumanForDaysFromNow(groupName);
        }
      } else if (groupingMode.equals(FilterChipLiveDataStockGrouping.GROUPING_MIN_STOCK_AMOUNT)) {
        groupName = stockItem.getProduct().getMinStockAmount();
      } else if (groupingMode.equals(FilterChipLiveDataStockGrouping.GROUPING_PARENT_PRODUCT)
          && NumUtil.isStringInt(stockItem.getProduct().getParentProductId())) {
        int productId = Integer.parseInt(stockItem.getProduct().getParentProductId());
        Product product = productHashMap.get(productId);
        groupName = product != null ? product.getName() : null;
      } else if (groupingMode.equals(FilterChipLiveDataStockGrouping.GROUPING_DEFAULT_LOCATION)
          && NumUtil.isStringInt(stockItem.getProduct().getLocationId())) {
        int locationId = Integer.parseInt(stockItem.getProduct().getLocationId());
        Location location = locationHashMap.get(locationId);
        groupName = location != null ? location.getName() : null;
      }
      if (groupName != null && !groupName.isEmpty()) {
        ArrayList<StockItem> itemsFromGroup = stockItemsGroupedHashMap.get(groupName);
        if (itemsFromGroup == null) {
          itemsFromGroup = new ArrayList<>();
          stockItemsGroupedHashMap.put(groupName, itemsFromGroup);
        }
        itemsFromGroup.add(stockItem);
      } else {
        ungroupedItems.add(stockItem);
      }
    }
    ArrayList<GroupedListItem> groupedListItems = new ArrayList<>();
    ArrayList<String> groupsSorted = new ArrayList<>(stockItemsGroupedHashMap.keySet());
    if (groupingMode.equals(FilterChipLiveDataStockGrouping.GROUPING_VALUE)
        || groupingMode.equals(FilterChipLiveDataStockGrouping.GROUPING_CALORIES)
        || groupingMode.equals(FilterChipLiveDataStockGrouping.GROUPING_MIN_STOCK_AMOUNT)) {
      SortUtil.sortStringsByValue(groupsSorted);
    } else {
      SortUtil.sortStringsByName(context, groupsSorted, true);
    }
    if (!ungroupedItems.isEmpty()) {
      groupedListItems.add(new GroupHeader(context.getString(R.string.property_ungrouped)));
      sortStockItems(context, ungroupedItems, sortMode, sortAscending);
      groupedListItems.addAll(ungroupedItems);
    }
    for (String group : groupsSorted) {
      ArrayList<StockItem> itemsFromGroup = stockItemsGroupedHashMap.get(group);
      if (itemsFromGroup == null) continue;
      String groupString;
      if (groupingMode.equals(FilterChipLiveDataStockGrouping.GROUPING_VALUE)) {
        groupString = group + " " + currency;
      } else {
        groupString = group;
      }
      GroupHeader groupHeader = new GroupHeader(groupString);
      groupHeader.setDisplayDivider(!ungroupedItems.isEmpty() || !groupsSorted.get(0).equals(group));
      groupedListItems.add(groupHeader);
      sortStockItems(context, itemsFromGroup, sortMode, sortAscending);
      groupedListItems.addAll(itemsFromGroup);
    }
    return groupedListItems;
  }

  static void sortStockItems(
      Context context,
      ArrayList<StockItem> stockItems,
      String sortMode,
      boolean sortAscending
  ) {
    if (sortMode.equals(FilterChipLiveDataStockSort.SORT_DUE_DATE)) {
      SortUtil.sortStockItemsByBBD(stockItems, sortAscending);
    } else {
      SortUtil.sortStockItemsByName(
          context,
          stockItems,
          sortAscending
      );
    }
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

  public static class GroupViewHolder extends ViewHolder {

    private final RowShoppingListGroupBinding binding;

    public GroupViewHolder(RowShoppingListGroupBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @Override
  public int getItemViewType(int position) {
    return GroupedListItem.getType(
        groupedListItems.get(position),
        GroupedListItem.CONTEXT_STOCK_OVERVIEW
    );
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == GroupedListItem.TYPE_ENTRY) {
      return new StockItemViewHolder(RowStockItemBinding.inflate(
          LayoutInflater.from(parent.getContext()),
          parent,
          false
      ));
    } else {
      return new GroupViewHolder(
          RowShoppingListGroupBinding.inflate(
              LayoutInflater.from(parent.getContext()),
              parent,
              false
          )
      );
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int positionDoNotUse) {

    GroupedListItem groupedListItem = groupedListItems.get(viewHolder.getAdapterPosition());

    int type = getItemViewType(viewHolder.getAdapterPosition());
    if (type == GroupedListItem.TYPE_HEADER) {
      GroupViewHolder holder = (GroupViewHolder) viewHolder;
      if (((GroupHeader) groupedListItem).getDisplayDivider() == 1) {
        holder.binding.divider.setVisibility(View.VISIBLE);
      } else {
        holder.binding.divider.setVisibility(View.GONE);
      }
      holder.binding.name.setText(((GroupHeader) groupedListItem).getGroupName());
      return;
    }

    StockItem stockItem = (StockItem) groupedListItem;
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

    Context context = holder.binding.textAmount.getContext();

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

    switch (extraField) {
      case FilterChipLiveDataStockExtraField.EXTRA_FIELD_VALUE:
        if (NumUtil.isStringDouble(stockItem.getValue())) {
          holder.binding.extraField.setText(NumUtil.trimPrice(NumUtil.toDouble(stockItem.getValue())));
          holder.binding.extraField.setVisibility(View.VISIBLE);
        } else {
          holder.binding.extraField.setVisibility(View.GONE);
        }
        break;
      case FilterChipLiveDataStockExtraField.EXTRA_FIELD_CALORIES_UNIT:
        if (NumUtil.isStringDouble(stockItem.getProduct().getCalories())) {
          holder.binding.extraField.setText(stockItem.getProduct().getCalories());
          holder.binding.extraField.setVisibility(View.VISIBLE);
        } else {
          holder.binding.extraField.setVisibility(View.GONE);
        }
        break;
      case FilterChipLiveDataStockExtraField.EXTRA_FIELD_CALORIES_TOTAL:
        if (NumUtil.isStringDouble(stockItem.getProduct().getCalories())) {
          holder.binding.extraField.setText(
                  NumUtil.trim(Double.parseDouble(stockItem.getProduct().getCalories())
                          * stockItem.getAmountDouble())
          );
          holder.binding.extraField.setVisibility(View.VISIBLE);
        } else {
          holder.binding.extraField.setVisibility(View.GONE);
        }
        break;
      case FilterChipLiveDataStockExtraField.EXTRA_FIELD_AVERAGE_PRICE:
        if (productAveragePriceHashMap.get(stockItem.getProductId()) != null) {
          holder.binding.extraField.setText(
              productAveragePriceHashMap.get(stockItem.getProductId())
          );
          holder.binding.extraField.setVisibility(View.VISIBLE);
        } else {
          holder.binding.extraField.setVisibility(View.GONE);
        }
        break;
      default:
        holder.binding.extraField.setVisibility(View.GONE);
        break;
    }

    // CONTAINER

    holder.binding.linearContainer.setOnClickListener(
        view -> listener.onItemRowClicked(stockItem)
    );
  }

  @Override
  public int getItemCount() {
    return groupedListItems.size();
  }

  public ArrayList<GroupedListItem> getGroupedListItems() {
    return groupedListItems;
  }

  public interface StockOverviewItemAdapterListener {

    void onItemRowClicked(StockItem stockItem);
  }

  public void updateData(
      Context context,
      ArrayList<StockItem> newList,
      ArrayList<String> shoppingListItemsProductIds,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      HashMap<Integer, String> productAveragePriceHashMap,
      HashMap<Integer, ProductGroup> productGroupHashMap,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, Location> locationHashMap,
      ArrayList<Integer> missingItemsProductIds,
      String sortMode,
      boolean sortAscending,
      String groupingMode,
      String extraField
  ) {
    ArrayList<GroupedListItem> newGroupedListItems = getGroupedListItems(context, newList,
        productGroupHashMap, productHashMap, locationHashMap, this.currency, this.dateUtil,
        sortMode, sortAscending, groupingMode);
    StockOverviewItemAdapter.DiffCallback diffCallback = new StockOverviewItemAdapter.DiffCallback(
        this.groupedListItems,
        newGroupedListItems,
        this.shoppingListItemsProductIds,
        shoppingListItemsProductIds,
        this.quantityUnitHashMap,
        quantityUnitHashMap,
        this.productAveragePriceHashMap,
        productAveragePriceHashMap,
        this.missingItemsProductIds,
        missingItemsProductIds,
        this.sortMode,
        sortMode,
        this.sortAscending,
        sortAscending,
        this.groupingMode,
        groupingMode,
        this.extraField,
        extraField
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.groupedListItems.clear();
    this.groupedListItems.addAll(newGroupedListItems);
    this.shoppingListItemsProductIds.clear();
    this.shoppingListItemsProductIds.addAll(shoppingListItemsProductIds);
    this.quantityUnitHashMap.clear();
    this.quantityUnitHashMap.putAll(quantityUnitHashMap);
    this.missingItemsProductIds.clear();
    this.missingItemsProductIds.addAll(missingItemsProductIds);
    this.sortMode = sortMode;
    this.sortAscending = sortAscending;
    this.groupingMode = groupingMode;
    this.extraField = extraField;
    diffResult.dispatchUpdatesTo(this);
  }

  static class DiffCallback extends DiffUtil.Callback {

    ArrayList<GroupedListItem> oldItems;
    ArrayList<GroupedListItem> newItems;
    ArrayList<String> shoppingListItemsProductIdsOld;
    ArrayList<String> shoppingListItemsProductIdsNew;
    HashMap<Integer, QuantityUnit> quantityUnitHashMapOld;
    HashMap<Integer, QuantityUnit> quantityUnitHashMapNew;
    HashMap<Integer, String> productAveragePriceHashMapOld;
    HashMap<Integer, String> productAveragePriceHashMapNew;
    ArrayList<Integer> missingProductIdsOld;
    ArrayList<Integer> missingProductIdsNew;
    String sortModeOld;
    String sortModeNew;
    boolean sortAscendingOld;
    boolean sortAscendingNew;
    String groupingModeOld;
    String groupingModeNew;
    String extraFieldOld;
    String extraFieldNew;

    public DiffCallback(
        ArrayList<GroupedListItem> oldItems,
        ArrayList<GroupedListItem> newItems,
        ArrayList<String> shoppingListItemsProductIdsOld,
        ArrayList<String> shoppingListItemsProductIdsNew,
        HashMap<Integer, QuantityUnit> quantityUnitHashMapOld,
        HashMap<Integer, QuantityUnit> quantityUnitHashMapNew,
        HashMap<Integer, String> productAveragePriceHashMapOld,
        HashMap<Integer, String> productAveragePriceHashMapNew,
        ArrayList<Integer> missingProductIdsOld,
        ArrayList<Integer> missingProductIdsNew,
        String sortModeOld,
        String sortModeNew,
        boolean sortAscendingOld,
        boolean sortAscendingNew,
        String groupingModeOld,
        String groupingModeNew,
        String extraFieldOld,
        String extraFieldNew
    ) {
      this.newItems = newItems;
      this.oldItems = oldItems;
      this.shoppingListItemsProductIdsOld = shoppingListItemsProductIdsOld;
      this.shoppingListItemsProductIdsNew = shoppingListItemsProductIdsNew;
      this.quantityUnitHashMapOld = quantityUnitHashMapOld;
      this.quantityUnitHashMapNew = quantityUnitHashMapNew;
      this.productAveragePriceHashMapOld = productAveragePriceHashMapOld;
      this.productAveragePriceHashMapNew = productAveragePriceHashMapNew;
      this.missingProductIdsOld = missingProductIdsOld;
      this.missingProductIdsNew = missingProductIdsNew;
      this.sortModeOld = sortModeOld;
      this.sortModeNew = sortModeNew;
      this.sortAscendingOld = sortAscendingOld;
      this.sortAscendingNew = sortAscendingNew;
      this.groupingModeOld = groupingModeOld;
      this.groupingModeNew = groupingModeNew;
      this.extraFieldOld = extraFieldOld;
      this.extraFieldNew = extraFieldNew;
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
      int oldItemType = GroupedListItem.getType(
          oldItems.get(oldItemPos),
          GroupedListItem.CONTEXT_STOCK_OVERVIEW
      );
      int newItemType = GroupedListItem.getType(
          newItems.get(newItemPos),
          GroupedListItem.CONTEXT_STOCK_OVERVIEW
      );
      if (oldItemType != newItemType) {
        return false;
      }
      if (!sortModeOld.equals(sortModeNew)) {
        return false;
      }
      if (sortAscendingOld != sortAscendingNew) {
        return false;
      }
      if (!groupingModeOld.equals(groupingModeNew) || !extraFieldOld.equals(extraFieldNew)) {
        return false;
      }
      if (oldItemType == GroupedListItem.TYPE_ENTRY) {
        StockItem newItem = (StockItem) newItems.get(newItemPos);
        StockItem oldItem = (StockItem) oldItems.get(oldItemPos);
        if (!compareContent) {
          return newItem.getProductId() == oldItem.getProductId();
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

        if (extraFieldNew.equals(FilterChipLiveDataStockExtraField.EXTRA_FIELD_AVERAGE_PRICE)) {
          String priceOld = productAveragePriceHashMapOld.get(oldItem.getProductId());
          String priceNew = productAveragePriceHashMapNew.get(newItem.getProductId());
          if (priceOld == null && priceNew != null
              || priceOld != null && priceNew != null && !priceOld.equals(priceNew)) {
            return false;
          }
        }

        boolean missingOld = missingProductIdsOld.contains(oldItem.getProductId());
        boolean missingNew = missingProductIdsNew.contains(newItem.getProductId());
        if (missingOld != missingNew) {
          return false;
        }
        return newItem.equals(oldItem);
      } else {
        GroupHeader newGroup = (GroupHeader) newItems.get(newItemPos);
        GroupHeader oldGroup = (GroupHeader) oldItems.get(oldItemPos);
        return newGroup.getGroupName().equals(oldGroup.getGroupName())
            && newGroup.getDisplayDivider() == oldGroup.getDisplayDivider();
      }
    }
  }
}
