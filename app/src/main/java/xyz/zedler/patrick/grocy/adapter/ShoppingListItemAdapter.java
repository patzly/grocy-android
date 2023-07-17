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
import android.graphics.Paint;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.ColorRoles;
import com.google.android.material.elevation.SurfaceColors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.RowShoppingListBottomNotesBinding;
import xyz.zedler.patrick.grocy.databinding.RowShoppingListGroupBinding;
import xyz.zedler.patrick.grocy.databinding.RowShoppingListItemBinding;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataShoppingListGrouping;
import xyz.zedler.patrick.grocy.model.GroupHeader;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.ProductLastPurchased;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListBottomNotes;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PictureUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.util.TextUtil;
import xyz.zedler.patrick.grocy.viewmodel.ShoppingListViewModel;
import xyz.zedler.patrick.grocy.viewmodel.StockOverviewViewModel;
import xyz.zedler.patrick.grocy.web.RequestHeaders;

public class ShoppingListItemAdapter extends
    RecyclerView.Adapter<ShoppingListItemAdapter.ViewHolder> {

  private final static String TAG = ShoppingListItemAdapter.class.getSimpleName();

  private final ArrayList<GroupedListItem> groupedListItems;
  private final HashMap<Integer, Product> productHashMap;
  private final HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMap;
  private final HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private final HashMap<Integer, Double> shoppingListItemAmountsHashMap;
  private final ArrayList<Integer> missingProductIds;
  private final ShoppingListItemAdapterListener listener;
  private final GrocyApi grocyApi;
  private final LazyHeaders grocyAuthHeaders;
  private final PluralUtil pluralUtil;
  private String groupingMode;
  private final List<String> activeFields;
  private final int maxDecimalPlacesAmount;
  private final int decimalPlacesPriceDisplay;
  private final String currency;
  private final boolean priceTrackingEnabled;

  public ShoppingListItemAdapter(
      Context context,
      ArrayList<ShoppingListItem> shoppingListItems,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, String> productNamesHashMap,
      HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMap,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      HashMap<Integer, ProductGroup> productGroupHashMap,
      HashMap<Integer, Store> storeHashMap,
      HashMap<Integer, Double> shoppingListItemAmountsHashMap,
      ArrayList<Integer> missingProductIds,
      ShoppingListItemAdapterListener listener,
      String shoppingListNotes,
      String groupingMode,
      List<String> activeFields
  ) {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    this.maxDecimalPlacesAmount = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_AMOUNT,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_AMOUNT
    );
    this.decimalPlacesPriceDisplay = sharedPrefs.getInt(
        STOCK.DECIMAL_PLACES_PRICES_DISPLAY,
        SETTINGS_DEFAULT.STOCK.DECIMAL_PLACES_PRICES_DISPLAY
    );
    this.currency = sharedPrefs.getString(PREF.CURRENCY, "");
    this.priceTrackingEnabled = sharedPrefs
        .getBoolean(PREF.FEATURE_STOCK_PRICE_TRACKING, true);
    this.productHashMap = new HashMap<>(productHashMap);
    this.productLastPurchasedHashMap = new HashMap<>(productLastPurchasedHashMap);
    this.quantityUnitHashMap = new HashMap<>(quantityUnitHashMap);
    this.shoppingListItemAmountsHashMap = new HashMap<>(shoppingListItemAmountsHashMap);
    this.missingProductIds = new ArrayList<>(missingProductIds);
    this.listener = listener;
    this.grocyApi = new GrocyApi((Application) context.getApplicationContext());
    this.grocyAuthHeaders = RequestHeaders.getGlideGrocyAuthHeaders(context);
    this.pluralUtil = new PluralUtil(context);
    this.groupingMode = groupingMode;
    this.activeFields = activeFields;
    this.groupedListItems = getGroupedListItems(context, shoppingListItems,
        productGroupHashMap, productHashMap, productNamesHashMap, storeHashMap,
        productLastPurchasedHashMap, shoppingListItemAmountsHashMap,
        shoppingListNotes, groupingMode, priceTrackingEnabled,
        decimalPlacesPriceDisplay, currency);
  }

  static ArrayList<GroupedListItem> getGroupedListItems(
      Context context,
      ArrayList<ShoppingListItem> shoppingListItems,
      HashMap<Integer, ProductGroup> productGroupHashMap,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, String> productNamesHashMap,
      HashMap<Integer, Store> storeHashMap,
      HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMap,
      HashMap<Integer, Double> shoppingListItemAmountsHashMap,
      String shoppingListNotes,
      String groupingMode,
      boolean priceTrackingEnabled,
      int decimalPlacesPriceDisplay,
      String currency
  ) {
    if (groupingMode.equals(FilterChipLiveDataShoppingListGrouping.GROUPING_NONE)) {
      SortUtil.sortShoppingListItemsByName(shoppingListItems, productNamesHashMap, true);
      ArrayList<GroupedListItem> groupedListItems = new ArrayList<>(shoppingListItems);
      addBottomNotes(
          context,
          shoppingListNotes,
          groupedListItems,
          !shoppingListItems.isEmpty()
      );
      if (!shoppingListItems.isEmpty() && priceTrackingEnabled) {
        addTotalPrice(context, shoppingListItems, groupedListItems, productLastPurchasedHashMap,
            shoppingListItemAmountsHashMap, decimalPlacesPriceDisplay, currency);
      }
      return groupedListItems;
    }
    HashMap<String, ArrayList<ShoppingListItem>> shoppingListItemsGroupedHashMap = new HashMap<>();
    ArrayList<ShoppingListItem> ungroupedItems = new ArrayList<>();
    for (ShoppingListItem shoppingListItem : shoppingListItems) {
      String groupName = getGroupName(shoppingListItem, productHashMap, productGroupHashMap,
          storeHashMap, groupingMode);
      if (groupName != null && !groupName.isEmpty()) {
        ArrayList<ShoppingListItem> itemsFromGroup = shoppingListItemsGroupedHashMap.get(groupName);
        if (itemsFromGroup == null) {
          itemsFromGroup = new ArrayList<>();
          shoppingListItemsGroupedHashMap.put(groupName, itemsFromGroup);
        }
        itemsFromGroup.add(shoppingListItem);
      } else {
        ungroupedItems.add(shoppingListItem);
      }
    }
    ArrayList<GroupedListItem> groupedListItems = new ArrayList<>();
    ArrayList<String> groupsSorted = new ArrayList<>(shoppingListItemsGroupedHashMap.keySet());
    SortUtil.sortStringsByName(groupsSorted, true);
    if (!ungroupedItems.isEmpty()) {
      groupedListItems.add(new GroupHeader(context.getString(R.string.property_ungrouped)));
      SortUtil.sortShoppingListItemsByName(ungroupedItems, productNamesHashMap, true);
      groupedListItems.addAll(ungroupedItems);
    }
    for (String group : groupsSorted) {
      ArrayList<ShoppingListItem> itemsFromGroup = shoppingListItemsGroupedHashMap.get(group);
      if (itemsFromGroup == null) continue;
      GroupHeader groupHeader = new GroupHeader(group);
      groupHeader.setDisplayDivider(!ungroupedItems.isEmpty() || !groupsSorted.get(0).equals(group));
      groupedListItems.add(groupHeader);
      SortUtil.sortShoppingListItemsByName(itemsFromGroup, productNamesHashMap, true);
      groupedListItems.addAll(itemsFromGroup);
    }
    addBottomNotes(
        context,
        shoppingListNotes,
        groupedListItems,
        !ungroupedItems.isEmpty() || !groupsSorted.isEmpty()
    );
    if ((!ungroupedItems.isEmpty() || !groupsSorted.isEmpty()) && priceTrackingEnabled) {
      addTotalPrice(context, shoppingListItems, groupedListItems, productLastPurchasedHashMap,
          shoppingListItemAmountsHashMap, decimalPlacesPriceDisplay, currency);
    }
    return groupedListItems;
  }

  static String getGroupName(
      ShoppingListItem shoppingListItem,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, ProductGroup> productGroupHashMap,
      HashMap<Integer, Store> storeHashMap,
      String groupingMode
  ) {
    String groupName = null;
    if (groupingMode.equals(FilterChipLiveDataShoppingListGrouping.GROUPING_PRODUCT_GROUP)
        && shoppingListItem.hasProduct()) {
      Product product = productHashMap.get(shoppingListItem.getProductIdInt());
      Integer productGroupId = product != null && NumUtil.isStringInt(product.getProductGroupId())
          ? Integer.parseInt(product.getProductGroupId())
          : null;
      ProductGroup productGroup = productGroupId != null
          ? productGroupHashMap.get(productGroupId)
          : null;
      groupName = productGroup != null ? productGroup.getName() : null;
    } else if (groupingMode.equals(FilterChipLiveDataShoppingListGrouping.GROUPING_STORE)
        && shoppingListItem.hasProduct()) {
      Product product = productHashMap.get(shoppingListItem.getProductIdInt());
      Integer storeId = product != null && NumUtil.isStringInt(product.getStoreId())
          ? Integer.parseInt(product.getStoreId())
          : null;
      Store store = storeId != null
          ? storeHashMap.get(storeId)
          : null;
      groupName = store != null ? store.getName() : null;
    }
    return groupName;
  }

  static void addBottomNotes(
      Context context,
      String shoppingListNotes,
      ArrayList<GroupedListItem> groupedListItems,
      boolean displayDivider
  ) {
    if (shoppingListNotes == null) {
      return;
    }
    Spanned spanned = Html.fromHtml(shoppingListNotes.trim());
    Spanned notes = (Spanned) TextUtil.trimCharSequence(spanned);
    if (notes != null && !notes.toString().trim().isEmpty()) {
      GroupHeader h = new GroupHeader(context.getString(R.string.property_notes));
      h.setDisplayDivider(displayDivider);
      groupedListItems.add(h);
      groupedListItems.add(new ShoppingListBottomNotes(notes));
    }
  }

  static void addTotalPrice(
      Context context,
      List<ShoppingListItem> shoppingListItems,
      ArrayList<GroupedListItem> groupedListItems,
      HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMap,
      HashMap<Integer, Double> shoppingListItemAmountsHashMap,
      int decimalPlacesPriceDisplay,
      String currency
  ) {
    double priceTotal = 0;
    for (ShoppingListItem shoppingListItem : shoppingListItems) {
      ProductLastPurchased p = shoppingListItem.hasProduct()
          ? productLastPurchasedHashMap.get(shoppingListItem.getProductIdInt()) : null;
      if (p == null || p.getPrice() == null || p.getPrice().isEmpty()) continue;
      Double amountInQuUnit = shoppingListItemAmountsHashMap.get(shoppingListItem.getId());
      double amount = amountInQuUnit != null ? amountInQuUnit : shoppingListItem.getAmountDouble();
      if (NumUtil.isStringDouble(p.getPrice())) priceTotal += NumUtil.toDouble(p.getPrice()) * amount;
    }

    GroupHeader h = new GroupHeader();
    h.setDisplayDivider(true);
    groupedListItems.add(h);
    ShoppingListBottomNotes priceText = new ShoppingListBottomNotes(
        new SpannedString(context.getString(
            R.string.subtitle_total_price,
            NumUtil.trimPrice(priceTotal, decimalPlacesPriceDisplay),
            currency
        ))
    );
    priceText.setClickable(false);
    groupedListItems.add(priceText);
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View view) {
      super(view);
    }
  }

  public static class ShoppingListItemViewHolder extends ViewHolder {

    private final RowShoppingListItemBinding binding;

    public ShoppingListItemViewHolder(RowShoppingListItemBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public static class ShoppingListGroupViewHolder extends ViewHolder {

    private final RowShoppingListGroupBinding binding;

    public ShoppingListGroupViewHolder(RowShoppingListGroupBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public static class ShoppingListNotesViewHolder extends ViewHolder {

    private final RowShoppingListBottomNotesBinding binding;

    public ShoppingListNotesViewHolder(RowShoppingListBottomNotesBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @Override
  public int getItemViewType(int position) {
    return GroupedListItem.getType(
        groupedListItems.get(position),
        GroupedListItem.CONTEXT_SHOPPING_LIST
    );
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == GroupedListItem.TYPE_HEADER) {
      return new ShoppingListGroupViewHolder(
          RowShoppingListGroupBinding.inflate(
              LayoutInflater.from(parent.getContext()),
              parent,
              false
          )
      );
    } else if (viewType == GroupedListItem.TYPE_ENTRY) {
      return new ShoppingListItemViewHolder(
          RowShoppingListItemBinding.inflate(
              LayoutInflater.from(parent.getContext()),
              parent,
              false
          )
      );
    } else {
      return new ShoppingListNotesViewHolder(
          RowShoppingListBottomNotesBinding.inflate(
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
      ShoppingListGroupViewHolder holder = (ShoppingListGroupViewHolder) viewHolder;
      if (((GroupHeader) groupedListItem).getDisplayDivider() == 1) {
        holder.binding.divider.setVisibility(View.VISIBLE);
      } else {
        holder.binding.divider.setVisibility(View.GONE);
      }
      if (((GroupHeader) groupedListItem).getGroupName() != null) {
        holder.binding.name.setText(((GroupHeader) groupedListItem).getGroupName());
        holder.binding.name.setVisibility(View.VISIBLE);
      } else {
        holder.binding.name.setVisibility(View.GONE);
      }
      return;
    }
    if (type == GroupedListItem.TYPE_BOTTOM_NOTES) {
      ShoppingListNotesViewHolder holder = (ShoppingListNotesViewHolder) viewHolder;
      holder.binding.notes.setText(
          ((ShoppingListBottomNotes) groupedListItem).getNotes()
      );
      if (((ShoppingListBottomNotes) groupedListItem).isClickable()) {
        holder.binding.container.setOnClickListener(
            view -> listener.onItemRowClicked(groupedListItem)
        );
        holder.binding.container.setClickable(true);
      } else {
        holder.binding.container.setClickable(false);
      }
      return;
    }

    ShoppingListItem item = (ShoppingListItem) groupedListItem;
    RowShoppingListItemBinding binding = ((ShoppingListItemViewHolder) viewHolder).binding;

    Context context = binding.getRoot().getContext();
    ColorRoles colorBlue = ResUtil.getHarmonizedRoles(context, R.color.blue);

    // NAME

    Product product = null;
    if (item.hasProduct()) {
      product = productHashMap.get(item.getProductIdInt());
    }

    if (product != null) {
      binding.name.setText(product.getName());
      binding.name.setVisibility(View.VISIBLE);
    } else {
      binding.name.setText(null);
      binding.name.setVisibility(View.GONE);
    }
    if (item.isUndone()) {
      binding.name.setPaintFlags(
          binding.name.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
      );
      binding.name.setAlpha(1.0f);
    } else {
      binding.name.setPaintFlags(
          binding.name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
      );
      binding.name.setAlpha(0.6f);
    }

    // NOTE AS NAME

    if (binding.name.getVisibility() == View.VISIBLE) {
      binding.noteAsName.setVisibility(View.GONE);
      binding.noteAsName.setText(null);
    }

    binding.flexboxLayout.removeAllViews();

    // AMOUNT

    Double amountInQuUnit = shoppingListItemAmountsHashMap.get(item.getId());
    if (activeFields.contains(ShoppingListViewModel.FIELD_AMOUNT)) {
      StringBuilder stringBuilderAmount = new StringBuilder();
      if (product != null && amountInQuUnit != null) {
        QuantityUnit quantityUnit = quantityUnitHashMap.get(item.getQuIdInt());
        String quStr = pluralUtil.getQuantityUnitPlural(quantityUnit, amountInQuUnit);
        if (quStr != null) {
          stringBuilderAmount.append(context.getString(
              R.string.subtitle_amount,
              NumUtil.trimAmount(amountInQuUnit, maxDecimalPlacesAmount),
              quStr
          ));
        } else {
          stringBuilderAmount.append(NumUtil.trimAmount(amountInQuUnit, maxDecimalPlacesAmount));
        }
      } else if (product != null) {
        QuantityUnit quantityUnit = quantityUnitHashMap.get(product.getQuIdStockInt());
        String quStr = pluralUtil.getQuantityUnitPlural(quantityUnit, item.getAmountDouble());
        if (quStr != null) {
          stringBuilderAmount.append(context.getString(
              R.string.subtitle_amount,
              NumUtil.trimAmount(item.getAmountDouble(), maxDecimalPlacesAmount),
              quStr
          ));
        } else {
          stringBuilderAmount.append(NumUtil.trimAmount(item.getAmountDouble(), maxDecimalPlacesAmount));
        }
      } else {
        stringBuilderAmount.append(NumUtil.trimAmount(item.getAmountDouble(), maxDecimalPlacesAmount));
      }
      Chip chipAmount = createChip(context, stringBuilderAmount.toString());
      if (item.hasProduct() && missingProductIds.contains(item.getProductIdInt())) {
        chipAmount.setTextColor(colorBlue.getOnAccentContainer());
        chipAmount.setChipBackgroundColor(ColorStateList.valueOf(colorBlue.getAccentContainer()));
      }
      if (item.isUndone()) {
        chipAmount.setPaintFlags(chipAmount.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        chipAmount.setAlpha(1);
      } else {
        chipAmount.setPaintFlags(chipAmount.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        chipAmount.setAlpha(0.61f);
      }
      binding.flexboxLayout.addView(chipAmount);
    }

    // NOTE

    if (item.getNote() != null && !item.getNote().trim().isEmpty()) {
      if (binding.name.getVisibility() == View.VISIBLE) {
        if (activeFields.contains(ShoppingListViewModel.FIELD_NOTES)) {
          binding.note.setVisibility(View.VISIBLE);
          binding.note.setText(item.getNote().trim());
        } else {
          binding.note.setVisibility(View.GONE);
        }
      } else {
        binding.noteAsName.setVisibility(View.VISIBLE);
        binding.noteAsName.setText(item.getNote().trim());
        binding.note.setVisibility(View.GONE);
        binding.note.setText(null);
      }
    } else {
      if (binding.name.getVisibility() == View.VISIBLE) {
        binding.note.setVisibility(View.GONE);
        binding.note.setText(null);
      }
    }
    if (binding.noteAsName.getVisibility() == View.VISIBLE) {
      if (item.isUndone()) {
        binding.noteAsName.setPaintFlags(
            binding.noteAsName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
        );
        binding.noteAsName.setAlpha(1.0f);
      } else {
        binding.noteAsName.setPaintFlags(
            binding.noteAsName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
        );
        binding.noteAsName.setAlpha(0.6f);
      }
    } else {
      if (item.isUndone()) {
        binding.note.setPaintFlags(
            binding.note.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
        );
        binding.note.setAlpha(1.0f);
      } else {
        binding.note.setPaintFlags(
            binding.note.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
        );
        binding.note.setAlpha(0.6f);
      }
    }

    if (activeFields.contains(ShoppingListViewModel.FIELD_PRICE_LAST_TOTAL)) {
      ProductLastPurchased p = product != null
          ? productLastPurchasedHashMap.get(product.getId()) : null;
      if (p != null && p.getPrice() != null && !p.getPrice().isEmpty()) {
        double amount = amountInQuUnit != null ? amountInQuUnit : item.getAmountDouble();
        String price = NumUtil.isStringDouble(p.getPrice())
            ? NumUtil.trimPrice(NumUtil.toDouble(p.getPrice()) * amount,
            decimalPlacesPriceDisplay) : p.getPrice();
        Chip chipValue = createChip(context, context.getString(
            R.string.property_insert_total,
            context.getString(R.string.property_price_with_currency, price, currency)
        ));
        binding.flexboxLayout.addView(chipValue);
      }
    }
    if (activeFields.contains(ShoppingListViewModel.FIELD_PRICE_LAST_UNIT)) {
      ProductLastPurchased p = product != null
          ? productLastPurchasedHashMap.get(product.getId()) : null;
      if (p != null && p.getPrice() != null && !p.getPrice().isEmpty()) {
        String price = NumUtil.isStringDouble(p.getPrice())
            ? NumUtil.trimPrice(NumUtil.toDouble(p.getPrice()),
            decimalPlacesPriceDisplay) : p.getPrice();
        Chip chipValue = createChip(context, context.getString(
            R.string.property_insert_per_unit,
            context.getString(R.string.property_price_with_currency, price, currency)
        ));
        binding.flexboxLayout.addView(chipValue);
      }
    }

    binding.flexboxLayout.setVisibility(
        binding.flexboxLayout.getChildCount() > 0 ? View.VISIBLE : View.GONE
    );

    String pictureFileName = product != null ? product.getPictureFileName() : null;
    if (activeFields.contains(StockOverviewViewModel.FIELD_PICTURE)
        && pictureFileName != null && !pictureFileName.isEmpty()) {
      binding.picture.layout(0, 0, 0, 0);

      PictureUtil.loadPicture(
          binding.picture,
          null,
          null,
          grocyApi.getProductPictureServeSmall(pictureFileName),
          grocyAuthHeaders,
          false
      );
    } else {
      binding.picture.setVisibility(View.GONE);
    }

    // CONTAINER

    binding.containerRow.setOnClickListener(
        view -> listener.onItemRowClicked(groupedListItem)
    );

  }

  private static Chip createChip(Context ctx, String text) {
    @SuppressLint("InflateParams")
    Chip chip = (Chip) LayoutInflater.from(ctx)
        .inflate(R.layout.view_info_chip, null, false);
    chip.setChipBackgroundColor(ColorStateList.valueOf(SurfaceColors.SURFACE_4.getColor(ctx)));
    chip.setText(text);
    chip.setEnabled(false);
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

  public interface ShoppingListItemAdapterListener {

    void onItemRowClicked(GroupedListItem groupedListItem);
  }

  // Only for PurchaseFragment
  public static void fillShoppingListItem(
      Context context,
      ShoppingListItem item,
      RowShoppingListItemBinding binding,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      HashMap<Integer, Double> shoppingListItemAmountsHashMap,
      int maxDecimalPlacesAmount,
      PluralUtil pluralUtil
  ) {

    // NAME

    Product product = null;
    if(item.hasProduct()) product = productHashMap.get(item.getProductIdInt());

    if (product != null) {
      binding.name.setText(product.getName());
      binding.name.setVisibility(View.VISIBLE);
    } else {
      binding.name.setText(null);
      binding.name.setVisibility(View.GONE);
    }
    if (item.isUndone()) {
      binding.name.setPaintFlags(
          binding.name.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
      );
    } else {
      binding.name.setPaintFlags(
          binding.name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
      );
    }

    // NOTE AS NAME

    if (binding.name.getVisibility() == View.VISIBLE) {
      binding.noteAsName.setVisibility(View.GONE);
      binding.noteAsName.setText(null);
    }

    // AMOUNT

    StringBuilder stringBuilderAmount = new StringBuilder();
    Double amountInQuUnit = shoppingListItemAmountsHashMap.get(item.getId());
    if (product != null && amountInQuUnit != null) {
      QuantityUnit quantityUnit = quantityUnitHashMap.get(item.getQuIdInt());
      String quStr = pluralUtil.getQuantityUnitPlural(quantityUnit, amountInQuUnit);
      if (quStr != null) {
        stringBuilderAmount.append(context.getString(
            R.string.subtitle_amount,
            NumUtil.trimAmount(amountInQuUnit, maxDecimalPlacesAmount),
            quStr
        ));
      } else {
        stringBuilderAmount.append(NumUtil.trimAmount(amountInQuUnit, maxDecimalPlacesAmount));
      }
    } else if (product != null) {
      QuantityUnit quantityUnit = quantityUnitHashMap.get(product.getQuIdStockInt());
      String quStr = pluralUtil.getQuantityUnitPlural(quantityUnit, item.getAmountDouble());
      if (quStr != null) {
        stringBuilderAmount.append(context.getString(
            R.string.subtitle_amount,
            NumUtil.trimAmount(item.getAmountDouble(), maxDecimalPlacesAmount),
            quStr
        ));
      } else {
        stringBuilderAmount.append(NumUtil.trimAmount(item.getAmountDouble(), maxDecimalPlacesAmount));
      }
    } else {
      stringBuilderAmount.append(NumUtil.trimAmount(item.getAmountDouble(), maxDecimalPlacesAmount));
    }

    binding.flexboxLayout.removeAllViews();
    Chip chipAmount = createChip(context, stringBuilderAmount.toString());
    if (item.isUndone()) {
      chipAmount.setPaintFlags(chipAmount.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
      chipAmount.setAlpha(1);
    } else {
      chipAmount.setPaintFlags(chipAmount.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
      chipAmount.setAlpha(0.61f);
    }
    binding.flexboxLayout.addView(chipAmount);

    // NOTE

    if (item.getNote() != null && !item.getNote().isEmpty()) {
      if (binding.name.getVisibility() == View.VISIBLE) {
        binding.note.setVisibility(View.VISIBLE);
        binding.note.setText(item.getNote().trim());
      } else {
        binding.noteAsName.setVisibility(View.VISIBLE);
        binding.noteAsName.setText(item.getNote().trim());
      }
    } else {
      if (binding.name.getVisibility() == View.VISIBLE) {
        binding.note.setVisibility(View.GONE);
        binding.note.setText(null);
      }
    }
    if (binding.noteAsName.getVisibility() == View.VISIBLE) {
      if (item.isUndone()) {
        binding.noteAsName.setPaintFlags(
            binding.noteAsName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
        );
      } else {
        binding.noteAsName.setPaintFlags(
            binding.noteAsName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
        );
      }
    } else {
      if (item.isUndone()) {
        binding.note.setPaintFlags(
            binding.note.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
        );
      } else {
        binding.note.setPaintFlags(
            binding.note.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
        );
      }
    }
  }

  public void updateData(
      Context context,
      ArrayList<ShoppingListItem> shoppingListItems,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, String> productNamesHashMap,
      HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMap,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      HashMap<Integer, ProductGroup> productGroupHashMap,
      HashMap<Integer, Store> storeHashMap,
      HashMap<Integer, Double> shoppingListItemAmountsHashMap,
      ArrayList<Integer> missingProductIds,
      String shoppingListNotes,
      String groupingMode,
      List<String> activeFields
  ) {
    ArrayList<GroupedListItem> newGroupedListItems = getGroupedListItems(context, shoppingListItems,
        productGroupHashMap, productHashMap, productNamesHashMap, storeHashMap,
        productLastPurchasedHashMap, shoppingListItemAmountsHashMap,
        shoppingListNotes, groupingMode, priceTrackingEnabled, decimalPlacesPriceDisplay, currency);
    ShoppingListItemAdapter.DiffCallback diffCallback = new ShoppingListItemAdapter.DiffCallback(
        this.groupedListItems,
        newGroupedListItems,
        this.productHashMap,
        productHashMap,
        this.productLastPurchasedHashMap,
        productLastPurchasedHashMap,
        this.quantityUnitHashMap,
        quantityUnitHashMap,
        this.shoppingListItemAmountsHashMap,
        shoppingListItemAmountsHashMap,
        this.missingProductIds,
        missingProductIds,
        this.groupingMode,
        groupingMode,
        this.activeFields,
        activeFields
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.groupedListItems.clear();
    this.groupedListItems.addAll(newGroupedListItems);
    this.productHashMap.clear();
    this.productHashMap.putAll(productHashMap);
    this.quantityUnitHashMap.clear();
    this.quantityUnitHashMap.putAll(quantityUnitHashMap);
    this.productLastPurchasedHashMap.clear();
    this.productLastPurchasedHashMap.putAll(productLastPurchasedHashMap);
    this.shoppingListItemAmountsHashMap.clear();
    this.shoppingListItemAmountsHashMap.putAll(shoppingListItemAmountsHashMap);
    this.missingProductIds.clear();
    this.missingProductIds.addAll(missingProductIds);
    this.groupingMode = groupingMode;
    this.activeFields.clear();
    this.activeFields.addAll(activeFields);
    diffResult.dispatchUpdatesTo(this);
  }

  static class DiffCallback extends DiffUtil.Callback {

    ArrayList<GroupedListItem> oldItems;
    ArrayList<GroupedListItem> newItems;
    HashMap<Integer, Product> productHashMapOld;
    HashMap<Integer, Product> productHashMapNew;
    HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMapOld;
    HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMapNew;
    HashMap<Integer, QuantityUnit> quantityUnitHashMapOld;
    HashMap<Integer, QuantityUnit> quantityUnitHashMapNew;
    HashMap<Integer, Double> shoppingListItemAmountsHashMapOld;
    HashMap<Integer, Double> shoppingListItemAmountsHashMapNew;
    ArrayList<Integer> missingProductIdsOld;
    ArrayList<Integer> missingProductIdsNew;
    String groupingModeOld;
    String groupingModeNew;
    List<String> activeFieldsOld;
    List<String> activeFieldsNew;

    public DiffCallback(
        ArrayList<GroupedListItem> oldItems,
        ArrayList<GroupedListItem> newItems,
        HashMap<Integer, Product> productHashMapOld,
        HashMap<Integer, Product> productHashMapNew,
        HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMapOld,
        HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMapNew,
        HashMap<Integer, QuantityUnit> quantityUnitHashMapOld,
        HashMap<Integer, QuantityUnit> quantityUnitHashMapNew,
        HashMap<Integer, Double> shoppingListItemAmountsHashMapOld,
        HashMap<Integer, Double> shoppingListItemAmountsHashMapNew,
        ArrayList<Integer> missingProductIdsOld,
        ArrayList<Integer> missingProductIdsNew,
        String groupingModeOld,
        String groupingModeNew,
        List<String> activeFieldsOld,
        List<String> activeFieldsNew
    ) {
      this.oldItems = oldItems;
      this.newItems = newItems;
      this.productHashMapOld = productHashMapOld;
      this.productHashMapNew = productHashMapNew;
      this.productLastPurchasedHashMapOld = productLastPurchasedHashMapOld;
      this.productLastPurchasedHashMapNew = productLastPurchasedHashMapNew;
      this.quantityUnitHashMapOld = quantityUnitHashMapOld;
      this.quantityUnitHashMapNew = quantityUnitHashMapNew;
      this.shoppingListItemAmountsHashMapOld = shoppingListItemAmountsHashMapOld;
      this.shoppingListItemAmountsHashMapNew = shoppingListItemAmountsHashMapNew;
      this.missingProductIdsOld = missingProductIdsOld;
      this.missingProductIdsNew = missingProductIdsNew;
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
          GroupedListItem.CONTEXT_SHOPPING_LIST
      );
      int newItemType = GroupedListItem.getType(
          newItems.get(newItemPos),
          GroupedListItem.CONTEXT_SHOPPING_LIST
      );
      if (oldItemType != newItemType) {
        return false;
      }
      if (!groupingModeOld.equals(groupingModeNew)) {
        return false;
      }
      if (oldItemType == GroupedListItem.TYPE_ENTRY) {
        ShoppingListItem newItem = (ShoppingListItem) newItems.get(newItemPos);
        ShoppingListItem oldItem = (ShoppingListItem) oldItems.get(oldItemPos);
        if (!compareContent) {
          return newItem.getId() == oldItem.getId();
        }
        if (!ArrayUtil.areListsEqualIgnoreOrder(activeFieldsOld, activeFieldsNew)) {
          return false;
        }

        Integer productIdOld =
            NumUtil.isStringInt(oldItem.getProductId()) ? Integer.parseInt(oldItem.getProductId())
                : null;
        Product productOld = productIdOld != null ? productHashMapOld.get(productIdOld) : null;

        Integer productIdNew =
            NumUtil.isStringInt(newItem.getProductId()) ? Integer.parseInt(newItem.getProductId())
                : null;
        Product productNew = productIdNew != null ? productHashMapNew.get(productIdNew) : null;

        Integer quIdOld =
            NumUtil.isStringInt(oldItem.getQuId()) ? Integer.parseInt(oldItem.getQuId()) : null;
        QuantityUnit quOld = quIdOld != null ? quantityUnitHashMapOld.get(quIdOld) : null;

        Integer quIdNew =
            NumUtil.isStringInt(newItem.getQuId()) ? Integer.parseInt(newItem.getQuId()) : null;
        QuantityUnit quNew = quIdNew != null ? quantityUnitHashMapNew.get(quIdNew) : null;

        Double amountOld = shoppingListItemAmountsHashMapOld.get(oldItem.getId());
        Double amountNew = shoppingListItemAmountsHashMapNew.get(newItem.getId());

        Boolean missingOld =
            productIdOld != null ? missingProductIdsOld.contains(productIdOld) : null;
        Boolean missingNew =
            productIdNew != null ? missingProductIdsNew.contains(productIdNew) : null;

        if (activeFieldsNew.contains(ShoppingListViewModel.FIELD_PRICE_LAST_UNIT)
            || activeFieldsNew.contains(ShoppingListViewModel.FIELD_PRICE_LAST_TOTAL)) {
          ProductLastPurchased purchasedOld = productIdOld != null
              ? productLastPurchasedHashMapOld.get(productIdOld) : null;
          ProductLastPurchased purchasedNew = productIdNew != null
              ? productLastPurchasedHashMapNew.get(productIdNew) : null;
          if (purchasedOld == null && purchasedNew != null
              || purchasedOld != null && purchasedNew != null && !purchasedOld.equals(purchasedNew)) {
            return false;
          }
        }

        if (productOld == null && productNew != null
            || productOld != null && productNew != null && productOld.getId() != productNew.getId()
            || quOld == null && quNew != null
            || quOld != null && quNew != null && quOld.getId() != quNew.getId()
            || !Objects.equals(amountOld, amountNew)
            || missingOld == null && missingNew != null
            || missingOld != null && missingNew != null && missingOld != missingNew
        ) {
          return false;
        }

        return newItem.equals(oldItem);
      } else if (oldItemType == GroupedListItem.TYPE_HEADER) {
        GroupHeader newGroup = (GroupHeader) newItems.get(newItemPos);
        GroupHeader oldGroup = (GroupHeader) oldItems.get(oldItemPos);
        return newGroup.equals(oldGroup);
      } else { // Type: Bottom notes
        ShoppingListBottomNotes newNotes = (ShoppingListBottomNotes) newItems.get(newItemPos);
        ShoppingListBottomNotes oldNotes = (ShoppingListBottomNotes) oldItems.get(oldItemPos);
        return newNotes.equals(oldNotes);
      }
    }
  }
}
