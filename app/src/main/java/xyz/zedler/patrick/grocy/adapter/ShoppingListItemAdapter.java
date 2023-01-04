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
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.text.Html;
import android.text.Spanned;
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
import java.util.Objects;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowShoppingListBottomNotesBinding;
import xyz.zedler.patrick.grocy.databinding.RowShoppingListGroupBinding;
import xyz.zedler.patrick.grocy.databinding.RowShoppingListItemBinding;
import xyz.zedler.patrick.grocy.model.FilterChipLiveDataShoppingListExtraField;
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
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.util.TextUtil;

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
  private final PluralUtil pluralUtil;
  private String groupingMode;
  private String extraField;
  private final int maxDecimalPlacesAmount;
  private final int decimalPlacesPriceDisplay;

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
      String extraField
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
    this.productHashMap = new HashMap<>(productHashMap);
    this.productLastPurchasedHashMap = new HashMap<>(productLastPurchasedHashMap);
    this.quantityUnitHashMap = new HashMap<>(quantityUnitHashMap);
    this.shoppingListItemAmountsHashMap = new HashMap<>(shoppingListItemAmountsHashMap);
    this.missingProductIds = new ArrayList<>(missingProductIds);
    this.listener = listener;
    this.pluralUtil = new PluralUtil(context);
    this.groupingMode = groupingMode;
    this.extraField = extraField;
    this.groupedListItems = getGroupedListItems(context, shoppingListItems,
        productGroupHashMap, productHashMap, productNamesHashMap, storeHashMap,
        shoppingListNotes, groupingMode);
  }

  static ArrayList<GroupedListItem> getGroupedListItems(
      Context context,
      ArrayList<ShoppingListItem> shoppingListItems,
      HashMap<Integer, ProductGroup> productGroupHashMap,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, String> productNamesHashMap,
      HashMap<Integer, Store> storeHashMap,
      String shoppingListNotes,
      String groupingMode
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
    return groupedListItems;
  }

  public static String getGroupName(
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

  private static void addBottomNotes(
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
      holder.binding.name.setText(((GroupHeader) groupedListItem).getGroupName());
      return;
    }
    if (type == GroupedListItem.TYPE_BOTTOM_NOTES) {
      ShoppingListNotesViewHolder holder = (ShoppingListNotesViewHolder) viewHolder;
      holder.binding.notes.setText(
          ((ShoppingListBottomNotes) groupedListItem).getNotes()
      );
      holder.binding.container.setOnClickListener(
          view -> listener.onItemRowClicked(groupedListItem)
      );
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

    // AMOUNT

    Double amountInQuUnit = shoppingListItemAmountsHashMap.get(item.getId());
    if (product != null && amountInQuUnit != null) {
      QuantityUnit quantityUnit = quantityUnitHashMap.get(item.getQuIdInt());
      String quStr = pluralUtil.getQuantityUnitPlural(quantityUnit, amountInQuUnit);
      if (quStr != null) {
        binding.amount.setText(
            binding.amount.getContext()
                .getString(R.string.subtitle_amount, NumUtil.trimAmount(amountInQuUnit, maxDecimalPlacesAmount), quStr)
        );
      } else {
        binding.amount.setText(NumUtil.trimAmount(amountInQuUnit, maxDecimalPlacesAmount));
      }
    } else if (product != null) {
      QuantityUnit quantityUnit = quantityUnitHashMap.get(product.getQuIdStockInt());
      String quStr = pluralUtil.getQuantityUnitPlural(quantityUnit, item.getAmountDouble());
      if (quStr != null) {
        binding.amount.setText(
            binding.amount.getContext()
                .getString(R.string.subtitle_amount, NumUtil.trimAmount(item.getAmountDouble(), maxDecimalPlacesAmount), quStr)
        );
      } else {
        binding.amount.setText(NumUtil.trimAmount(item.getAmountDouble(), maxDecimalPlacesAmount));
      }
    } else {
      binding.amount.setText(NumUtil.trimAmount(item.getAmountDouble(), maxDecimalPlacesAmount));
    }

    if (item.hasProduct() && missingProductIds.contains(item.getProductIdInt())) {
      binding.amount.setTypeface(
          ResourcesCompat.getFont(binding.amount.getContext(), R.font.jost_medium)
      );
      binding.amount.setTextColor(colorBlue.getAccent());
      binding.amount.setAlpha(1);
    } else {
      binding.amount.setTypeface(
          ResourcesCompat.getFont(binding.amount.getContext(), R.font.jost_book)
      );
      binding.amount.setTextColor(ResUtil.getColorAttr(context, R.attr.colorOnBackground));
      binding.amount.setAlpha(0.61f);
    }
    if (item.isUndone()) {
      binding.amount.setPaintFlags(
          binding.amount.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
      );
      binding.amount.setAlpha(1);
    } else {
      binding.amount.setPaintFlags(
          binding.amount.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
      );
      binding.amount.setAlpha(0.61f);
    }

    // NOTE

    if (item.getNote() != null && !item.getNote().trim().isEmpty()) {
      if (binding.name.getVisibility() == View.VISIBLE) {
        binding.note.setVisibility(View.VISIBLE);
        binding.note.setText(item.getNote().trim());
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

    if (extraField.equals(FilterChipLiveDataShoppingListExtraField.EXTRA_FIELD_LAST_PRICE_UNIT)
        || extraField.equals(FilterChipLiveDataShoppingListExtraField.EXTRA_FIELD_LAST_PRICE_TOTAL)
    ) {
      ProductLastPurchased p = product != null
          ? productLastPurchasedHashMap.get(product.getId()) : null;
      if (p != null && p.getPrice() != null && !p.getPrice().isEmpty()) {
        String price;
        if (extraField
            .equals(FilterChipLiveDataShoppingListExtraField.EXTRA_FIELD_LAST_PRICE_TOTAL)) {
          double amount = amountInQuUnit != null ? amountInQuUnit : item.getAmountDouble();
          price = NumUtil.isStringDouble(p.getPrice())
              ? NumUtil.trimPrice(NumUtil.toDouble(p.getPrice()) * amount, decimalPlacesPriceDisplay) : p.getPrice();
        } else {
          price = NumUtil.isStringDouble(p.getPrice())
              ? NumUtil.trimPrice(NumUtil.toDouble(p.getPrice()), decimalPlacesPriceDisplay) : p.getPrice();
        }
        binding.extraField.setText(price);
        binding.extraField.setVisibility(View.VISIBLE);
      } else {
        binding.extraField.setVisibility(View.GONE);
      }
    } else {
      binding.extraField.setVisibility(View.GONE);
    }

    // CONTAINER

    binding.containerRow.setOnClickListener(
        view -> listener.onItemRowClicked(groupedListItem)
    );

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

    Double amountInQuUnit = shoppingListItemAmountsHashMap.get(item.getId());
    if (product != null && amountInQuUnit != null) {
      QuantityUnit quantityUnit = quantityUnitHashMap.get(item.getQuIdInt());
      String quStr = pluralUtil.getQuantityUnitPlural(quantityUnit, amountInQuUnit);
      if (quStr != null) {
        binding.amount.setText(
            context.getString(R.string.subtitle_amount, NumUtil.trimAmount(amountInQuUnit, maxDecimalPlacesAmount), quStr)
        );
      } else {
        binding.amount.setText(NumUtil.trimAmount(amountInQuUnit, maxDecimalPlacesAmount));
      }
    } else if (product != null) {
      QuantityUnit quantityUnit = quantityUnitHashMap.get(product.getQuIdStockInt());
      String quStr = pluralUtil.getQuantityUnitPlural(quantityUnit, item.getAmountDouble());
      if (quStr != null) {
        binding.amount.setText(
            context.getString(R.string.subtitle_amount, NumUtil.trimAmount(item.getAmountDouble(), maxDecimalPlacesAmount), quStr)
        );
      } else {
        binding.amount.setText(NumUtil.trimAmount(item.getAmountDouble(), maxDecimalPlacesAmount));
      }
    } else {
      binding.amount.setText(NumUtil.trimAmount(item.getAmountDouble(), maxDecimalPlacesAmount));
    }

    if (item.isUndone()) {
      binding.amount.setPaintFlags(
          binding.amount.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
      );
    } else {
      binding.amount.setPaintFlags(
          binding.amount.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
      );
    }

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
      String extraField
  ) {
    ArrayList<GroupedListItem> newGroupedListItems = getGroupedListItems(context, shoppingListItems,
        productGroupHashMap, productHashMap, productNamesHashMap, storeHashMap,
        shoppingListNotes, groupingMode);
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
        this.extraField,
        extraField
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
    this.extraField = extraField;
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
    String extraFieldOld;
    String extraFieldNew;

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
        String extraFieldOld,
        String extraFieldNew
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
          GroupedListItem.CONTEXT_SHOPPING_LIST
      );
      int newItemType = GroupedListItem.getType(
          newItems.get(newItemPos),
          GroupedListItem.CONTEXT_SHOPPING_LIST
      );
      if (oldItemType != newItemType) {
        return false;
      }
      if (!groupingModeOld.equals(groupingModeNew) || !extraFieldOld.equals(extraFieldNew)) {
        return false;
      }
      if (oldItemType == GroupedListItem.TYPE_ENTRY) {
        ShoppingListItem newItem = (ShoppingListItem) newItems.get(newItemPos);
        ShoppingListItem oldItem = (ShoppingListItem) oldItems.get(oldItemPos);
        if (!compareContent) {
          return newItem.getId() == oldItem.getId();
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

        if (extraFieldNew.equals(FilterChipLiveDataShoppingListExtraField.EXTRA_FIELD_LAST_PRICE_UNIT)
            || extraFieldNew.equals(FilterChipLiveDataShoppingListExtraField.EXTRA_FIELD_LAST_PRICE_TOTAL)) {
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
