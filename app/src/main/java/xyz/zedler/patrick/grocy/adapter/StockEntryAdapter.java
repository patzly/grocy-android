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
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.color.ColorRoles;
import java.util.ArrayList;
import java.util.HashMap;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowShoppingListGroupBinding;
import xyz.zedler.patrick.grocy.databinding.RowStockEntryBinding;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataStockEntriesGrouping;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataStockGrouping;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataStockSort;
import xyz.zedler.patrick.grocy.model.GroupHeader;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockEntry;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.AmountUtil;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;

public class StockEntryAdapter extends
    RecyclerView.Adapter<StockEntryAdapter.ViewHolder> {

  private final static String TAG = StockEntryAdapter.class.getSimpleName();

  private final ArrayList<GroupedListItem> groupedListItems;
  private final HashMap<Integer, Product> productHashMap;
  private final HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private final HashMap<Integer, Location> locationHashMap;
  private final HashMap<Integer, Store> storeHashMap;
  private final PluralUtil pluralUtil;
  private final StockEntryAdapterListener listener;
  private final boolean showDateTracking;
  private String sortMode;
  private boolean sortAscending;
  private String groupingMode;
  private final DateUtil dateUtil;
  private final String currency;
  private final int dueSoonDays;
  private final int maxDecimalPlacesAmount;
  private final int decimalPlacesPriceDisplay;

  public StockEntryAdapter(
      Context context,
      ArrayList<StockEntry> stockEntries,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, Location> locationHashMap,
      HashMap<Integer, Store> storeHashMap,
      StockEntryAdapterListener listener,
      String sortMode,
      boolean sortAscending,
      String groupingMode
  ) {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    this.showDateTracking = sharedPrefs.getBoolean(PREF.FEATURE_STOCK_BBD_TRACKING, true);
    this.currency = sharedPrefs.getString(PREF.CURRENCY, "");
    this.maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
    this.decimalPlacesPriceDisplay = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_PRICES_DISPLAY,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_PRICES_DISPLAY
    );
    String days = sharedPrefs.getString(STOCK.DUE_SOON_DAYS, SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS);
    if (NumUtil.isStringInt(days)) {
      this.dueSoonDays = Integer.parseInt(days);
    } else {
      this.dueSoonDays = Integer.parseInt(SETTINGS_DEFAULT.STOCK.DUE_SOON_DAYS);
    }
    this.productHashMap = new HashMap<>(productHashMap);
    this.quantityUnitHashMap = new HashMap<>(quantityUnitHashMap);
    this.locationHashMap = new HashMap<>(locationHashMap);
    this.storeHashMap = new HashMap<>(storeHashMap);
    this.pluralUtil = new PluralUtil(context);
    this.listener = listener;
    this.dateUtil = new DateUtil(context);
    this.sortMode = sortMode;
    this.sortAscending = sortAscending;
    this.groupingMode = groupingMode;
    this.groupedListItems = getGroupedListItems(context, stockEntries, productHashMap,
        locationHashMap, storeHashMap, currency, dateUtil, sortMode,
        sortAscending, groupingMode);
  }

  static ArrayList<GroupedListItem> getGroupedListItems(
      Context context,
      ArrayList<StockEntry> stockEntries,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, Location> locationHashMap,
      HashMap<Integer, Store> storeHashMap,
      String currency,
      DateUtil dateUtil,
      String sortMode,
      boolean sortAscending,
      String groupingMode
  ) {
    if (groupingMode.equals(FilterChipLiveDataStockEntriesGrouping.GROUPING_NONE)) {
      sortStockEntries(context, stockEntries, productHashMap, sortMode, sortAscending);
      return new ArrayList<>(stockEntries);
    }
    HashMap<String, ArrayList<StockEntry>> stockEntriesGroupedHashMap = new HashMap<>();
    ArrayList<StockEntry> ungroupedItems = new ArrayList<>();
    for (StockEntry stockEntry : stockEntries) {
      String groupName = null;
      switch (groupingMode) {
        case FilterChipLiveDataStockEntriesGrouping.GROUPING_PRODUCT:
          Product product = productHashMap.get(stockEntry.getProductId());
          groupName = product != null ? product.getName() : null;
          break;
        case FilterChipLiveDataStockEntriesGrouping.GROUPING_DUE_DATE:
          groupName = stockEntry.getBestBeforeDate();
          if (groupName != null && !groupName.isEmpty()) {
            groupName += "  " + dateUtil.getHumanForDaysFromNow(groupName);
          }
          break;
        case FilterChipLiveDataStockEntriesGrouping.GROUPING_PURCHASED_DATE:
          groupName = stockEntry.getPurchasedDate();
          if (groupName != null && !groupName.isEmpty()) {
            groupName += "  " + dateUtil.getHumanForDaysFromNow(groupName);
          }
          break;
        case FilterChipLiveDataStockEntriesGrouping.GROUPING_LOCATION:
          Location location = locationHashMap.get(stockEntry.getLocationIdInt());
          groupName = location != null ? location.getName() : null;
          break;
        case FilterChipLiveDataStockEntriesGrouping.GROUPING_STORE:
          Store store = storeHashMap.get(stockEntry.getShoppingLocationIdInt());
          groupName = store != null ? store.getName() : null;
          break;
      }
      if (groupName != null && !groupName.isEmpty()) {
        ArrayList<StockEntry> itemsFromGroup = stockEntriesGroupedHashMap.get(groupName);
        if (itemsFromGroup == null) {
          itemsFromGroup = new ArrayList<>();
          stockEntriesGroupedHashMap.put(groupName, itemsFromGroup);
        }
        itemsFromGroup.add(stockEntry);
      } else {
        ungroupedItems.add(stockEntry);
      }
    }
    ArrayList<GroupedListItem> groupedListItems = new ArrayList<>();
    ArrayList<String> groupsSorted = new ArrayList<>(stockEntriesGroupedHashMap.keySet());
    SortUtil.sortStringsByName(groupsSorted, true);
    if (!ungroupedItems.isEmpty()) {
      groupedListItems.add(new GroupHeader(context.getString(R.string.property_ungrouped)));
      sortStockEntries(context, ungroupedItems, productHashMap, sortMode, sortAscending);
      groupedListItems.addAll(ungroupedItems);
    }
    for (String group : groupsSorted) {
      ArrayList<StockEntry> itemsFromGroup = stockEntriesGroupedHashMap.get(group);
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
      sortStockEntries(context, itemsFromGroup, productHashMap, sortMode, sortAscending);
      groupedListItems.addAll(itemsFromGroup);
    }
    return groupedListItems;
  }

  static void sortStockEntries(
      Context context,
      ArrayList<StockEntry> stockEntries,
      HashMap<Integer, Product> productHashMap,
      String sortMode,
      boolean sortAscending
  ) {
    if (sortMode.equals(FilterChipLiveDataStockSort.SORT_DUE_DATE)) {
      SortUtil.sortStockEntriesByDueDate(stockEntries, sortAscending);
    } else {
      SortUtil.sortStockEntriesByName(stockEntries, productHashMap, sortAscending);
    }
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View view) {
      super(view);
    }
  }

  public static class StockItemViewHolder extends ViewHolder {

    private final RowStockEntryBinding binding;

    public StockItemViewHolder(RowStockEntryBinding binding) {
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
        GroupedListItem.CONTEXT_STOCK_ENTRIES
    );
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == GroupedListItem.TYPE_ENTRY) {
      return new StockItemViewHolder(RowStockEntryBinding.inflate(
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

    StockEntry stockEntry = (StockEntry) groupedListItem;
    StockItemViewHolder holder = (StockItemViewHolder) viewHolder;
    Product product = productHashMap.get(stockEntry.getProductId());
    Context context = holder.binding.amount.getContext();

    // NAME

    holder.binding.productName.setText(product != null ? product.getName()
        : context.getString(R.string.subtitle_unknown));

    // AMOUNT

    QuantityUnit quantityUnitStock = product != null
        ? quantityUnitHashMap.get(product.getQuIdStockInt()) : null;
    holder.binding.amount.setText(context.getString(
        R.string.property_amount_insert,
        AmountUtil.getStockEntryAmountInfo(context, pluralUtil, stockEntry, quantityUnitStock, maxDecimalPlacesAmount)
    ));

    // BEST BEFORE

    String date = stockEntry.getBestBeforeDate();
    int daysInt = DateUtil.getDaysFromNow(date);
    String days = date != null ? String.valueOf(DateUtil.getDaysFromNow(date)) : null;
    boolean colorDays = false;

    if (!showDateTracking) {
      holder.binding.dueDate.setVisibility(View.GONE);
    } else if (days != null) {
      holder.binding.dueDate.setText(context.getString(
          R.string.property_due_date_fill,
          dateUtil.getLocalizedDate(date, DateUtil.FORMAT_SHORT) + "  "
              + dateUtil.getHumanForDaysFromNow(date)
      ));
      holder.binding.dueDate.setVisibility(View.VISIBLE);
      if (daysInt <= dueSoonDays) {
        colorDays = true;
      }
    } else {
      holder.binding.dueDate.setVisibility(View.GONE);
    }

    if (colorDays) {
      holder.binding.dueDate.setTypeface(
          ResourcesCompat.getFont(context, R.font.jost_medium)
      );

      ColorRoles colorYellow = ResUtil.getHarmonizedRoles(context, R.color.yellow);
      ColorRoles colorOrange = ResUtil.getHarmonizedRoles(context, R.color.orange);

      int color;
      if (Integer.parseInt(days) >= 0) {
        color = colorYellow.getAccent();
      } else if (product != null && product.getDueDateTypeInt() == StockItem.DUE_TYPE_EXPIRATION) {
        color = ResUtil.getColorAttr(context, R.attr.colorError);
      } else {
        color = colorOrange.getAccent(); // formally DIRT
      }
      holder.binding.dueDate.setTextColor(color);
    } else {
      holder.binding.dueDate.setTypeface(
          ResourcesCompat.getFont(context, R.font.jost_book)
      );
      holder.binding.dueDate.setTextColor(
          ResUtil.getColorAttr(context, R.attr.colorOnSurfaceVariant)
      );
    }

    // LOCATION

    Location location = locationHashMap.get(stockEntry.getLocationIdInt());
    if (location != null) {
      holder.binding.location.setText(
          context.getString(R.string.property_location_insert, location.getName())
      );
      holder.binding.location.setVisibility(View.VISIBLE);
    } else {
      holder.binding.location.setVisibility(View.GONE);
    }

    // STORE

    Integer storeId = NumUtil.isStringInt(stockEntry.getShoppingLocationId())
        ? Integer.parseInt(stockEntry.getShoppingLocationId()) : null;
    Store store = storeId != null ? storeHashMap.get(storeId) : null;
    if (store != null) {
      holder.binding.store.setText(
          context.getString(R.string.property_store_insert, store.getName())
      );
      holder.binding.store.setVisibility(View.VISIBLE);
    } else {
      holder.binding.store.setVisibility(View.GONE);
    }

    // PRICE

    if (NumUtil.isStringDouble(stockEntry.getPrice())) {
      QuantityUnit quPurchase = product != null
          ? quantityUnitHashMap.get(product.getQuIdPurchaseInt()) : null;
      if (product == null) {
        holder.binding.price.setText(context.getString(
            R.string.property_price_insert,
            NumUtil.trimPrice(NumUtil.toDouble(stockEntry.getPrice()), decimalPlacesPriceDisplay) + " " + currency
        ));
      } else if (product.getQuIdStockInt() == product.getQuIdPurchaseInt() || quPurchase == null) {
        holder.binding.price.setText(context.getString(
            R.string.property_price_insert,
            NumUtil.trimPrice(NumUtil.toDouble(stockEntry.getPrice())
                * product.getQuFactorPurchaseToStockDouble(), decimalPlacesPriceDisplay) + " " + currency
        ));
      } else {
        holder.binding.price.setText(context.getString(
            R.string.property_price_unit_insert,
            context.getString(
                R.string.property_price_insert,
                NumUtil.trimPrice(NumUtil.toDouble(stockEntry.getPrice())
                    * product.getQuFactorPurchaseToStockDouble(), decimalPlacesPriceDisplay) + " " + currency
            ),
            quPurchase.getName()
        ));
      }

      holder.binding.price.setVisibility(View.VISIBLE);
    } else {
      holder.binding.price.setVisibility(View.GONE);
    }

    // PURCHASED DATE

    String purchaseDate = stockEntry.getPurchasedDate();
    String purchaseDays = null;
    if (purchaseDate != null) {
      purchaseDays = String.valueOf(DateUtil.getDaysFromNow(purchaseDate));
    }
    if (purchaseDays != null && !purchaseDate.equals(Constants.DATE.NEVER_OVERDUE)) {
      holder.binding.purchasedDate.setVisibility(View.VISIBLE);
      holder.binding.purchasedDate.setText(context.getString(
          R.string.property_purchased_date_fill,
          dateUtil.getLocalizedDate(purchaseDate, DateUtil.FORMAT_SHORT) + "  "
              + dateUtil.getHumanForDaysFromNow(purchaseDate)
      ));
    } else {
      holder.binding.purchasedDate.setVisibility(View.GONE);
      holder.binding.purchasedDate.setText(null);
    }

    // NOTE

    if (stockEntry.getNote() != null && !stockEntry.getNote().isEmpty()) {
      holder.binding.note.setText(stockEntry.getNote());
      holder.binding.note.setVisibility(View.VISIBLE);
    } else {
      holder.binding.note.setVisibility(View.GONE);
    }

    // CONTAINER

    holder.binding.container.setOnClickListener(
        view -> listener.onItemRowClicked(stockEntry)
    );
  }

  @Override
  public int getItemCount() {
    return groupedListItems.size();
  }

  public ArrayList<GroupedListItem> getGroupedListItems() {
    return groupedListItems;
  }

  public interface StockEntryAdapterListener {

    void onItemRowClicked(StockEntry stockEntry);
  }

  public void updateData(
      Context context,
      ArrayList<StockEntry> newList,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, Location> locationHashMap,
      HashMap<Integer, Store> storeHashMap,
      String sortMode,
      boolean sortAscending,
      String groupingMode
  ) {
    ArrayList<GroupedListItem> newGroupedListItems = getGroupedListItems(context, newList,
        productHashMap, locationHashMap, storeHashMap, this.currency, this.dateUtil,
        sortMode, sortAscending, groupingMode);
    StockEntryAdapter.DiffCallback diffCallback = new StockEntryAdapter.DiffCallback(
        this.groupedListItems,
        newGroupedListItems,
        this.productHashMap,
        productHashMap,
        this.quantityUnitHashMap,
        quantityUnitHashMap,
        this.locationHashMap,
        locationHashMap,
        this.storeHashMap,
        storeHashMap,
        this.sortMode,
        sortMode,
        this.sortAscending,
        sortAscending,
        this.groupingMode,
        groupingMode
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.groupedListItems.clear();
    this.groupedListItems.addAll(newGroupedListItems);
    this.productHashMap.clear();
    this.productHashMap.putAll(productHashMap);
    this.quantityUnitHashMap.clear();
    this.quantityUnitHashMap.putAll(quantityUnitHashMap);
    this.locationHashMap.clear();
    this.locationHashMap.putAll(locationHashMap);
    this.storeHashMap.clear();
    this.storeHashMap.putAll(storeHashMap);
    this.sortMode = sortMode;
    this.sortAscending = sortAscending;
    this.groupingMode = groupingMode;
    diffResult.dispatchUpdatesTo(this);
  }

  static class DiffCallback extends DiffUtil.Callback {

    ArrayList<GroupedListItem> oldItems;
    ArrayList<GroupedListItem> newItems;
    HashMap<Integer, Product> productHashMapOld;
    HashMap<Integer, Product> productHashMapNew;
    HashMap<Integer, QuantityUnit> quantityUnitHashMapOld;
    HashMap<Integer, QuantityUnit> quantityUnitHashMapNew;
    HashMap<Integer, Location> locationHashMapOld;
    HashMap<Integer, Location> locationHashMapNew;
    HashMap<Integer, Store> storeHashMapOld;
    HashMap<Integer, Store> storeHashMapNew;
    String sortModeOld;
    String sortModeNew;
    boolean sortAscendingOld;
    boolean sortAscendingNew;
    String groupingModeOld;
    String groupingModeNew;

    public DiffCallback(
        ArrayList<GroupedListItem> oldItems,
        ArrayList<GroupedListItem> newItems,
        HashMap<Integer, Product> productHashMapOld,
        HashMap<Integer, Product> productHashMapNew,
        HashMap<Integer, QuantityUnit> quantityUnitHashMapOld,
        HashMap<Integer, QuantityUnit> quantityUnitHashMapNew,
        HashMap<Integer, Location> locationHashMapOld,
        HashMap<Integer, Location> locationHashMapNew,
        HashMap<Integer, Store> storeHashMapOld,
        HashMap<Integer, Store> storeHashMapNew,
        String sortModeOld,
        String sortModeNew,
        boolean sortAscendingOld,
        boolean sortAscendingNew,
        String groupingModeOld,
        String groupingModeNew
    ) {
      this.newItems = newItems;
      this.oldItems = oldItems;
      this.productHashMapOld = productHashMapOld;
      this.productHashMapNew = productHashMapNew;
      this.quantityUnitHashMapOld = quantityUnitHashMapOld;
      this.quantityUnitHashMapNew = quantityUnitHashMapNew;
      this.locationHashMapOld = locationHashMapOld;
      this.locationHashMapNew = locationHashMapNew;
      this.storeHashMapOld = storeHashMapOld;
      this.storeHashMapNew = storeHashMapNew;
      this.sortModeOld = sortModeOld;
      this.sortModeNew = sortModeNew;
      this.sortAscendingOld = sortAscendingOld;
      this.sortAscendingNew = sortAscendingNew;
      this.groupingModeOld = groupingModeOld;
      this.groupingModeNew = groupingModeNew;
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
          GroupedListItem.CONTEXT_STOCK_ENTRIES
      );
      int newItemType = GroupedListItem.getType(
          newItems.get(newItemPos),
          GroupedListItem.CONTEXT_STOCK_ENTRIES
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
      if (!groupingModeOld.equals(groupingModeNew)) {
        return false;
      }
      if (oldItemType == GroupedListItem.TYPE_ENTRY) {
        StockEntry newEntry = (StockEntry) newItems.get(newItemPos);
        StockEntry oldEntry = (StockEntry) oldItems.get(oldItemPos);
        if (!compareContent) {
          return newEntry.getProductId() == oldEntry.getProductId();
        }
        Product productNew = productHashMapNew.get(newEntry.getProductId());
        Product productOld = productHashMapOld.get(oldEntry.getProductId());
        if (productNew == null || !productNew.equals(productOld)) {
          return false;
        }
        QuantityUnit quOld = quantityUnitHashMapOld.get(productOld.getQuIdStockInt());
        QuantityUnit quNew = quantityUnitHashMapNew.get(productNew.getQuIdStockInt());
        if (quOld == null && quNew != null
            || quOld != null && quNew != null && quOld.getId() != quNew.getId()
        ) {
          return false;
        }
        Location locOld = locationHashMapOld.get(oldEntry.getLocationIdInt());
        Location locNew = locationHashMapNew.get(newEntry.getLocationIdInt());
        if (locOld == null && locNew != null
            || locOld != null && locNew != null && locOld.getId() != locNew.getId()
        ) {
          return false;
        }
        Store storeOld = NumUtil.isStringInt(oldEntry.getShoppingLocationId())
            ? storeHashMapOld.get(Integer.parseInt(oldEntry.getShoppingLocationId())) : null;
        Store storeNew = NumUtil.isStringInt(newEntry.getShoppingLocationId())
            ? storeHashMapNew.get(Integer.parseInt(newEntry.getShoppingLocationId())) : null;
        if (storeOld == null && storeNew != null
            || storeOld != null && storeNew != null && storeOld.getId() != storeNew.getId()
        ) {
          return false;
        }
        return newEntry.equals(oldEntry);
      } else {
        GroupHeader newGroup = (GroupHeader) newItems.get(newItemPos);
        GroupHeader oldGroup = (GroupHeader) oldItems.get(oldItemPos);
        return newGroup.getGroupName().equals(oldGroup.getGroupName())
            && newGroup.getDisplayDivider() == oldGroup.getDisplayDivider();
      }
    }
  }
}
