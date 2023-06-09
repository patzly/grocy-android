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
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.ColorRoles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.RowShoppingListGroupBinding;
import xyz.zedler.patrick.grocy.databinding.RowStockItemBinding;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataStockGrouping;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataStockSort;
import xyz.zedler.patrick.grocy.model.GroupHeader;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.ProductLastPurchased;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.util.AmountUtil;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.viewmodel.StockOverviewViewModel;
import xyz.zedler.patrick.grocy.web.RequestHeaders;

public class StockOverviewItemAdapter extends
    RecyclerView.Adapter<StockOverviewItemAdapter.ViewHolder> {

  private final static String TAG = StockOverviewItemAdapter.class.getSimpleName();

  private final ArrayList<GroupedListItem> groupedListItems;
  private final ArrayList<String> shoppingListItemsProductIds;
  private final HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private final HashMap<Integer, String> productAveragePriceHashMap;
  private final HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMap;
  private final PluralUtil pluralUtil;
  private final ArrayList<Integer> missingItemsProductIds;
  private final StockOverviewItemAdapterListener listener;
  private final GrocyApi grocyApi;
  private final LazyHeaders grocyAuthHeaders;
  private final boolean showDateTracking;
  private final boolean shoppingListFeatureEnabled;
  private final int daysExpiringSoon;
  private String sortMode;
  private boolean sortAscending;
  private String groupingMode;
  private final List<String> activeFields;
  private final DateUtil dateUtil;
  private final String currency;
  private final int maxDecimalPlacesAmount;
  private final int decimalPlacesPriceDisplay;
  private final String energyUnit;
  private boolean containsPictures;

  public StockOverviewItemAdapter(
      Context context,
      ArrayList<StockItem> stockItems,
      ArrayList<String> shoppingListItemsProductIds,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      HashMap<Integer, String> productAveragePriceHashMap,
      HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMap,
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
      List<String> activeFields
  ) {
    this.shoppingListItemsProductIds = new ArrayList<>(shoppingListItemsProductIds);
    this.quantityUnitHashMap = new HashMap<>(quantityUnitHashMap);
    this.productAveragePriceHashMap = new HashMap<>(productAveragePriceHashMap);
    this.productLastPurchasedHashMap = new HashMap<>(productLastPurchasedHashMap);
    this.pluralUtil = new PluralUtil(context);
    this.missingItemsProductIds = new ArrayList<>(missingItemsProductIds);
    this.listener = listener;
    this.grocyApi = new GrocyApi((Application) context.getApplicationContext());
    this.grocyAuthHeaders = RequestHeaders.getGlideGrocyAuthHeaders(context);
    this.showDateTracking = showDateTracking;
    this.shoppingListFeatureEnabled = shoppingListFeatureEnabled;
    this.daysExpiringSoon = daysExpiringSoon;
    this.currency = currency;
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    this.maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
    this.decimalPlacesPriceDisplay = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_PRICES_DISPLAY,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_PRICES_DISPLAY
    );
    energyUnit = sharedPrefs.getString(PREF.ENERGY_UNIT, PREF.ENERGY_UNIT_DEFAULT);
    this.dateUtil = new DateUtil(context);
    this.sortMode = sortMode;
    this.sortAscending = sortAscending;
    this.groupingMode = groupingMode;
    this.activeFields = activeFields;
    this.groupedListItems = getGroupedListItems(context, stockItems,
        productGroupHashMap, productHashMap, locationHashMap, currency, dateUtil, sortMode,
        sortAscending, groupingMode, maxDecimalPlacesAmount, decimalPlacesPriceDisplay);

    containsPictures = false;
    for (StockItem stockItem : stockItems) {
      if (stockItem.getProduct() == null) continue;
      String pictureFileName = stockItem.getProduct().getPictureFileName();
      if (pictureFileName != null && !pictureFileName.isEmpty()) {
        containsPictures = true;
        break;
      }
    }
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
      String groupingMode,
      int maxDecimalPlacesAmount,
      int decimalPlacesPriceDisplay
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
        groupName = NumUtil.trimPrice(stockItem.getValueDouble(), decimalPlacesPriceDisplay);
      } else if (groupingMode.equals(FilterChipLiveDataStockGrouping.GROUPING_CALORIES_PER_STOCK)) {
        groupName = NumUtil.isStringDouble(stockItem.getProduct().getCalories())
            ? stockItem.getProduct().getCalories() : null;
      } else if (groupingMode.equals(FilterChipLiveDataStockGrouping.GROUPING_CALORIES)) {
        groupName = NumUtil.isStringDouble(stockItem.getProduct().getCalories())
            ? NumUtil.trimAmount(NumUtil.toDouble(stockItem.getProduct().getCalories())
            * stockItem.getAmountDouble(), maxDecimalPlacesAmount) : null;
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
      SortUtil.sortStringsByName(groupsSorted, true);
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
      groupHeader.setDisplayDivider(
          !ungroupedItems.isEmpty() || !groupsSorted.get(0).equals(group)
      );
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
      SortUtil.sortStockItemsByName(stockItems, sortAscending);
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

    Context context = holder.binding.getRoot().getContext();

    ColorRoles colorBlue = ResUtil.getHarmonizedRoles(context, R.color.blue);
    ColorRoles colorYellow = ResUtil.getHarmonizedRoles(context, R.color.yellow);
    ColorRoles colorOrange = ResUtil.getHarmonizedRoles(context, R.color.orange);
    ColorRoles colorRed = ResUtil.getHarmonizedRoles(context, R.color.red);

    holder.binding.flexboxLayout.removeAllViews();

    // NAME

    holder.binding.textName.setText(stockItem.getProduct().getName());

    // IS ON SHOPPING LIST

    if (shoppingListItemsProductIds.contains(String.valueOf(stockItem.getProduct().getId()))
        && shoppingListFeatureEnabled) {
      holder.binding.viewOnShoppingList.setVisibility(View.VISIBLE);
      holder.binding.viewOnShoppingList.setBackgroundTintList(
          ColorStateList.valueOf(colorBlue.getAccent())
      );
    } else {
      holder.binding.viewOnShoppingList.setVisibility(View.GONE);
    }

    // AMOUNT

    QuantityUnit quantityUnitStock = quantityUnitHashMap.get(
        stockItem.getProduct().getQuIdStockInt()
    );

    if (activeFields.contains(StockOverviewViewModel.FIELD_AMOUNT)) {
      StringBuilder stringBuilderAmount = new StringBuilder();
      if (!stockItem.getProduct().getNoOwnStockBoolean()) {
        AmountUtil.addStockAmountNormalInfo(context, pluralUtil, stringBuilderAmount, stockItem,
            quantityUnitStock, maxDecimalPlacesAmount);
        Chip chipAmount = createChip(context, stringBuilderAmount.toString());
        if (missingItemsProductIds.contains(stockItem.getProductId())) {
          chipAmount.setTextColor(colorBlue.getOnAccentContainer());
          chipAmount.setChipBackgroundColor(ColorStateList.valueOf(colorBlue.getAccentContainer()));
        }
        holder.binding.flexboxLayout.addView(chipAmount);
      }
      StringBuilder stringBuilderAmountAggregated = new StringBuilder();
      AmountUtil.addStockAmountAggregatedInfo(context, pluralUtil, stringBuilderAmountAggregated,
          stockItem, quantityUnitStock, maxDecimalPlacesAmount, false);
      if (!stringBuilderAmountAggregated.toString().isBlank()) {
        Chip chipAmountAggregated = createChip(context, stringBuilderAmountAggregated.toString());
        if (missingItemsProductIds.contains(stockItem.getProductId())) {
          chipAmountAggregated.setTextColor(colorBlue.getOnAccentContainer());
          chipAmountAggregated.setChipBackgroundColor(ColorStateList.valueOf(colorBlue.getAccentContainer()));
        }
        holder.binding.flexboxLayout.addView(chipAmountAggregated);
      }
    }

    // BEST BEFORE

    String date = stockItem.getBestBeforeDate();
    String days = null;
    if (date != null) {
      days = String.valueOf(DateUtil.getDaysFromNow(date));
    }

    if (activeFields.contains(StockOverviewViewModel.FIELD_DUE_DATE) && showDateTracking
        && days != null && (sortMode.equals(FilterChipLiveDataStockSort.SORT_DUE_DATE)
        || Integer.parseInt(days) <= daysExpiringSoon
        && !date.equals(Constants.DATE.NEVER_OVERDUE))
    ) {
      Chip chipDate = createChip(context, dateUtil.getHumanForDaysFromNow(date));
      if (Integer.parseInt(days) <= daysExpiringSoon
          && !stockItem.getProduct().getNoOwnStockBoolean()) {  // don't color days text if product has no own stock (children will be colored)
        if (Integer.parseInt(days) >= 0) {
          chipDate.setTextColor(colorYellow.getOnAccentContainer());
          chipDate.setChipBackgroundColor(ColorStateList.valueOf(colorYellow.getAccentContainer()));
        } else if (stockItem.getDueTypeInt() == StockItem.DUE_TYPE_BEST_BEFORE) {
          chipDate.setTextColor(colorOrange.getOnAccentContainer());
          chipDate.setChipBackgroundColor(ColorStateList.valueOf(colorOrange.getAccentContainer())); // formally DIRT
        } else {
          chipDate.setTextColor(colorRed.getOnAccentContainer());
          chipDate.setChipBackgroundColor(ColorStateList.valueOf(colorRed.getAccentContainer()));
        }
      }
      holder.binding.flexboxLayout.addView(chipDate);
    }

    if (activeFields.contains(StockOverviewViewModel.FIELD_VALUE)
        && NumUtil.isStringDouble(stockItem.getValue())) {
      String value = NumUtil.trimPrice(
          NumUtil.toDouble(stockItem.getValue()), decimalPlacesPriceDisplay
      );
      if (currency != null && !currency.isEmpty()) {
        value = context.getString(R.string.property_price_with_currency, value, currency);
      }
      Chip chipValue = createChip(context, value);
      holder.binding.flexboxLayout.addView(chipValue);
    }
    if (activeFields.contains(StockOverviewViewModel.FIELD_CALORIES_UNIT)
        && NumUtil.isStringDouble(stockItem.getProduct().getCalories())) {
      Chip chipValue = createChip(context, context.getString(
          R.string.property_insert_per_unit,
          stockItem.getProduct().getCalories() + " " + energyUnit
      ));
      holder.binding.flexboxLayout.addView(chipValue);
    }
    if (activeFields.contains(StockOverviewViewModel.FIELD_CALORIES_TOTAL)
        && NumUtil.isStringDouble(stockItem.getProduct().getCalories())) {
      Chip chipValue = createChip(context, context.getString(
          R.string.property_insert_total,
          NumUtil.trimAmount(NumUtil.toDouble(stockItem.getProduct()
              .getCalories()) * stockItem.getAmountDouble(), maxDecimalPlacesAmount)
              + " " + energyUnit
      ));
      holder.binding.flexboxLayout.addView(chipValue);
    }
    double factorPurchaseToStock = stockItem.getProduct().getQuFactorPurchaseToStockDouble();
    if (activeFields.contains(StockOverviewViewModel.FIELD_AVERAGE_PRICE)) {
      String avg = productAveragePriceHashMap.get(stockItem.getProductId());
      if (NumUtil.isStringDouble(avg)) {
        Chip chipValue = createChip(context, context.getString(
            R.string.property_insert_average,
            context.getString(R.string.property_price_with_currency, NumUtil.trimPrice(
                NumUtil.toDouble(avg) * factorPurchaseToStock, decimalPlacesPriceDisplay
            ), currency)
        ));
        holder.binding.flexboxLayout.addView(chipValue);
      }
    }
    if (activeFields.contains(StockOverviewViewModel.FIELD_LAST_PRICE)) {
      ProductLastPurchased p = productLastPurchasedHashMap.get(stockItem.getProductId());
      if (p != null && NumUtil.isStringDouble(p.getPrice())) {
        Chip chipValue = createChip(context, context.getString(
            R.string.property_insert_last,
            context.getString(R.string.property_price_with_currency,
                NumUtil.trimPrice(NumUtil.toDouble(p.getPrice())
                * factorPurchaseToStock, decimalPlacesPriceDisplay), currency)
        ));
        holder.binding.flexboxLayout.addView(chipValue);
      }
    }

    holder.binding.flexboxLayout.setVisibility(
        holder.binding.flexboxLayout.getChildCount() > 0 ? View.VISIBLE : View.GONE
    );

    String pictureFileName = stockItem.getProduct().getPictureFileName();
    if (activeFields.contains(StockOverviewViewModel.FIELD_PICTURE)
        && pictureFileName != null && !pictureFileName.isEmpty()) {
      holder.binding.picture.layout(0, 0, 0, 0);

      Glide.with(context)
          .load(
              new GlideUrl(grocyApi.getProductPicture(pictureFileName), grocyAuthHeaders)
          ).transform(new CenterCrop())
          .transition(DrawableTransitionOptions.withCrossFade())
          .listener(new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                Target<Drawable> target, boolean isFirstResource) {
              holder.binding.picture.setVisibility(View.GONE);
              holder.binding.picturePlaceholder.setVisibility(View.VISIBLE);
              return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target,
                DataSource dataSource, boolean isFirstResource) {
              holder.binding.picture.setVisibility(View.VISIBLE);
              holder.binding.picturePlaceholder.setVisibility(View.GONE);
              return false;
            }
          }).into(holder.binding.picture);
    } else if (activeFields.contains(StockOverviewViewModel.FIELD_PICTURE) && containsPictures) {
      holder.binding.picture.setVisibility(View.GONE);
      holder.binding.picturePlaceholder.setVisibility(View.VISIBLE);
    } else {
      holder.binding.picture.setVisibility(View.GONE);
      holder.binding.picturePlaceholder.setVisibility(View.GONE);
    }

    // CONTAINER

    holder.binding.linearContainer.setOnClickListener(
        view -> listener.onItemRowClicked(stockItem)
    );
  }

  private static Chip createChip(Context ctx, String text) {
    @SuppressLint("InflateParams")
    Chip chip = (Chip) LayoutInflater.from(ctx)
        .inflate(R.layout.view_info_chip, null, false);
    chip.setText(text);
    chip.setClickable(false);
    chip.setFocusable(false);
    return chip;
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
      HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMap,
      HashMap<Integer, ProductGroup> productGroupHashMap,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, Location> locationHashMap,
      ArrayList<Integer> missingItemsProductIds,
      String sortMode,
      boolean sortAscending,
      String groupingMode,
      List<String> activeFields
  ) {
    ArrayList<GroupedListItem> newGroupedListItems = getGroupedListItems(context, newList,
        productGroupHashMap, productHashMap, locationHashMap, this.currency, this.dateUtil,
        sortMode, sortAscending, groupingMode, maxDecimalPlacesAmount, decimalPlacesPriceDisplay);
    StockOverviewItemAdapter.DiffCallback diffCallback = new StockOverviewItemAdapter.DiffCallback(
        this.groupedListItems,
        newGroupedListItems,
        this.shoppingListItemsProductIds,
        shoppingListItemsProductIds,
        this.quantityUnitHashMap,
        quantityUnitHashMap,
        this.productAveragePriceHashMap,
        productAveragePriceHashMap,
        this.productLastPurchasedHashMap,
        productLastPurchasedHashMap,
        this.missingItemsProductIds,
        missingItemsProductIds,
        this.sortMode,
        sortMode,
        this.sortAscending,
        sortAscending,
        this.groupingMode,
        groupingMode,
        this.activeFields,
        activeFields
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.groupedListItems.clear();
    this.groupedListItems.addAll(newGroupedListItems);
    this.shoppingListItemsProductIds.clear();
    this.shoppingListItemsProductIds.addAll(shoppingListItemsProductIds);
    this.quantityUnitHashMap.clear();
    this.quantityUnitHashMap.putAll(quantityUnitHashMap);
    this.productAveragePriceHashMap.clear();
    this.productAveragePriceHashMap.putAll(productAveragePriceHashMap);
    this.productLastPurchasedHashMap.clear();
    this.productLastPurchasedHashMap.putAll(productLastPurchasedHashMap);
    this.missingItemsProductIds.clear();
    this.missingItemsProductIds.addAll(missingItemsProductIds);
    this.sortMode = sortMode;
    this.sortAscending = sortAscending;
    this.groupingMode = groupingMode;
    this.activeFields.clear();
    this.activeFields.addAll(activeFields);
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
    HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMapOld;
    HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMapNew;
    ArrayList<Integer> missingProductIdsOld;
    ArrayList<Integer> missingProductIdsNew;
    String sortModeOld;
    String sortModeNew;
    boolean sortAscendingOld;
    boolean sortAscendingNew;
    String groupingModeOld;
    String groupingModeNew;
    List<String> activeFieldsOld;
    List<String> activeFieldsNew;

    public DiffCallback(
        ArrayList<GroupedListItem> oldItems,
        ArrayList<GroupedListItem> newItems,
        ArrayList<String> shoppingListItemsProductIdsOld,
        ArrayList<String> shoppingListItemsProductIdsNew,
        HashMap<Integer, QuantityUnit> quantityUnitHashMapOld,
        HashMap<Integer, QuantityUnit> quantityUnitHashMapNew,
        HashMap<Integer, String> productAveragePriceHashMapOld,
        HashMap<Integer, String> productAveragePriceHashMapNew,
        HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMapOld,
        HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMapNew,
        ArrayList<Integer> missingProductIdsOld,
        ArrayList<Integer> missingProductIdsNew,
        String sortModeOld,
        String sortModeNew,
        boolean sortAscendingOld,
        boolean sortAscendingNew,
        String groupingModeOld,
        String groupingModeNew,
        List<String> activeFieldsOld,
        List<String> activeFieldsNew
    ) {
      this.newItems = newItems;
      this.oldItems = oldItems;
      this.shoppingListItemsProductIdsOld = shoppingListItemsProductIdsOld;
      this.shoppingListItemsProductIdsNew = shoppingListItemsProductIdsNew;
      this.quantityUnitHashMapOld = quantityUnitHashMapOld;
      this.quantityUnitHashMapNew = quantityUnitHashMapNew;
      this.productAveragePriceHashMapOld = productAveragePriceHashMapOld;
      this.productAveragePriceHashMapNew = productAveragePriceHashMapNew;
      this.productLastPurchasedHashMapOld = productLastPurchasedHashMapOld;
      this.productLastPurchasedHashMapNew = productLastPurchasedHashMapNew;
      this.missingProductIdsOld = missingProductIdsOld;
      this.missingProductIdsNew = missingProductIdsNew;
      this.sortModeOld = sortModeOld;
      this.sortModeNew = sortModeNew;
      this.sortAscendingOld = sortAscendingOld;
      this.sortAscendingNew = sortAscendingNew;
      this.groupingModeOld = groupingModeOld;
      this.groupingModeNew = groupingModeNew;
      this.activeFieldsOld = activeFieldsOld;
      this.activeFieldsNew = activeFieldsNew;
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
      if (!groupingModeOld.equals(groupingModeNew)) {
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
        if (!ArrayUtil.areListsEqualIgnoreOrder(activeFieldsOld, activeFieldsNew)) {
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

        if (activeFieldsNew.contains(StockOverviewViewModel.FIELD_AVERAGE_PRICE)) {
          String priceOld = productAveragePriceHashMapOld.get(oldItem.getProductId());
          String priceNew = productAveragePriceHashMapNew.get(newItem.getProductId());
          if (priceOld == null && priceNew != null
              || priceOld != null && priceNew != null && !priceOld.equals(priceNew)) {
            return false;
          }
        } else if (activeFieldsNew.contains(StockOverviewViewModel.FIELD_LAST_PRICE)) {
          ProductLastPurchased purchasedOld = productLastPurchasedHashMapOld
              .get(oldItem.getProductId());
          ProductLastPurchased purchasedNew = productLastPurchasedHashMapNew
              .get(newItem.getProductId());
          if (purchasedOld == null && purchasedNew != null
              || purchasedOld != null && purchasedNew != null
              && !purchasedOld.equals(purchasedNew)) {
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
