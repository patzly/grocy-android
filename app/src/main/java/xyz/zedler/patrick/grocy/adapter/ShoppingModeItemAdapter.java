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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.ColorRoles;
import com.google.android.material.elevation.SurfaceColors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.SHOPPING_MODE;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.RowShoppingModeBottomNotesBinding;
import xyz.zedler.patrick.grocy.databinding.RowShoppingModeGroupBinding;
import xyz.zedler.patrick.grocy.databinding.RowShoppingModeItemBinding;
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
import xyz.zedler.patrick.grocy.util.PictureUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.util.SortUtil;
import xyz.zedler.patrick.grocy.util.TextUtil;
import xyz.zedler.patrick.grocy.util.UiUtil;
import xyz.zedler.patrick.grocy.viewmodel.ShoppingListViewModel;
import xyz.zedler.patrick.grocy.viewmodel.ShoppingModeViewModel;
import xyz.zedler.patrick.grocy.web.RequestHeaders;

public class ShoppingModeItemAdapter extends
    RecyclerView.Adapter<ShoppingModeItemAdapter.ViewHolder> {

  private final Context context;
  private final LinearLayoutManager linearLayoutManager;
  private final ArrayList<GroupedListItem> groupedListItems;
  private final HashMap<Integer, Product> productHashMap;
  private final HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMap;
  private final HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private final HashMap<Integer, Double> shoppingListItemAmountsHashMap;
  private final ArrayList<Integer> missingProductIds;
  private final ShoppingModeItemClickListener listener;
  private final GrocyApi grocyApi;
  private final LazyHeaders grocyAuthHeaders;
  private final PluralUtil pluralUtil;
  private String groupingMode;
  private final boolean useSmallerFonts;
  private final boolean showDoneItems;
  private final List<String> activeFields;
  private final int maxDecimalPlacesAmount;
  private final int decimalPlacesPriceDisplay;
  private final String currency;
  private final boolean priceTrackingEnabled;

  public ShoppingModeItemAdapter(
      Context context,
      LinearLayoutManager linearLayoutManager,
      ArrayList<ShoppingListItem> shoppingListItems,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, String> productNamesHashMap,
      HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMap,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      HashMap<Integer, ProductGroup> productGroupHashMap,
      HashMap<Integer, Store> storeHashMap,
      HashMap<Integer, Double> shoppingListItemAmountsHashMap,
      ArrayList<Integer> missingProductIds,
      ShoppingModeItemClickListener listener,
      String shoppingListNotes,
      String groupingMode,
      List<String> activeFields
  ) {
    this.context = context;
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    this.linearLayoutManager = linearLayoutManager;
    this.productHashMap = new HashMap<>(productHashMap);
    this.productLastPurchasedHashMap = new HashMap<>(productLastPurchasedHashMap);
    this.quantityUnitHashMap = new HashMap<>(quantityUnitHashMap);
    this.shoppingListItemAmountsHashMap = new HashMap<>(shoppingListItemAmountsHashMap);
    this.missingProductIds = new ArrayList<>(missingProductIds);
    this.listener = listener;
    this.grocyApi = new GrocyApi((Application) context.getApplicationContext());
    this.grocyAuthHeaders = RequestHeaders.getGlideGrocyAuthHeaders(context);
    this.useSmallerFonts = sharedPrefs.getBoolean(
        SHOPPING_MODE.USE_SMALLER_FONT,
        SETTINGS_DEFAULT.SHOPPING_MODE.USE_SMALLER_FONT
    );
    this.showDoneItems = sharedPrefs.getBoolean(
        Constants.SETTINGS.SHOPPING_MODE.SHOW_DONE_ITEMS,
        Constants.SETTINGS_DEFAULT.SHOPPING_MODE.SHOW_DONE_ITEMS
    );
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
    this.groupingMode = groupingMode;
    this.activeFields = activeFields;
    this.pluralUtil = new PluralUtil(context);
    this.groupedListItems = getGroupedListItems(context, shoppingListItems,
        productGroupHashMap, productHashMap, productNamesHashMap, storeHashMap,
        productLastPurchasedHashMap, shoppingListItemAmountsHashMap,
        shoppingListNotes, groupingMode, priceTrackingEnabled,
        decimalPlacesPriceDisplay, currency, showDoneItems);
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
      String currency,
      boolean showDoneItems
  ) {
    if (groupingMode.equals(FilterChipLiveDataShoppingListGrouping.GROUPING_NONE)) {
      SortUtil.sortShoppingListItemsByName(shoppingListItems, productNamesHashMap, true);
      ArrayList<GroupedListItem> groupedListItems = new ArrayList<>();
      ArrayList<ShoppingListItem> doneItems = new ArrayList<>();
      for (ShoppingListItem shoppingListItem : shoppingListItems) {
        if (shoppingListItem.getDoneInt() == 1) {
          if (showDoneItems) doneItems.add(shoppingListItem);
          continue;
        }
        groupedListItems.add(shoppingListItem);
      }
      if (showDoneItems && !doneItems.isEmpty()) {
        groupedListItems.add(new GroupHeader(context.getString(R.string.subtitle_done)));
        groupedListItems.addAll(doneItems);
      }
      ShoppingListItemAdapter.addBottomNotes(
          context,
          shoppingListNotes,
          groupedListItems,
          !shoppingListItems.isEmpty()
      );
      return groupedListItems;
    }
    HashMap<String, ArrayList<ShoppingListItem>> shoppingListItemsGroupedHashMap = new HashMap<>();
    ArrayList<ShoppingListItem> ungroupedItems = new ArrayList<>();
    ArrayList<ShoppingListItem> doneItems = new ArrayList<>();
    for (ShoppingListItem shoppingListItem : shoppingListItems) {
      if (shoppingListItem.getDoneInt() == 1) {
        if (showDoneItems) doneItems.add(shoppingListItem);
        continue;
      }
      String groupName = ShoppingListItemAdapter.getGroupName(shoppingListItem, productHashMap,
          productGroupHashMap, storeHashMap, groupingMode);
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
    if (showDoneItems && !doneItems.isEmpty()) {
      groupedListItems.add(new GroupHeader(context.getString(R.string.subtitle_done)));
      groupedListItems.addAll(doneItems);
    }
    ShoppingListItemAdapter.addBottomNotes(
        context,
        shoppingListNotes,
        groupedListItems,
        !ungroupedItems.isEmpty() || !groupsSorted.isEmpty()
    );
    if ((!ungroupedItems.isEmpty() || !groupsSorted.isEmpty()) && priceTrackingEnabled) {
      ShoppingListItemAdapter.addTotalPrice(context, shoppingListItems, groupedListItems,
          productLastPurchasedHashMap, shoppingListItemAmountsHashMap,
          decimalPlacesPriceDisplay, currency);
    }
    return groupedListItems;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View view) {
      super(view);
    }
  }

  public static class ShoppingItemViewHolder extends ViewHolder {

    private final RowShoppingModeItemBinding binding;

    public ShoppingItemViewHolder(RowShoppingModeItemBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public static class ShoppingGroupViewHolder extends ViewHolder {

    private final RowShoppingModeGroupBinding binding;

    public ShoppingGroupViewHolder(RowShoppingModeGroupBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public static class ShoppingNotesViewHolder extends ViewHolder {

    private final RowShoppingModeBottomNotesBinding binding;

    public ShoppingNotesViewHolder(RowShoppingModeBottomNotesBinding binding) {
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
      return new ShoppingGroupViewHolder(
          RowShoppingModeGroupBinding.inflate(
              LayoutInflater.from(parent.getContext()),
              parent,
              false
          )
      );
    } else if (viewType == GroupedListItem.TYPE_ENTRY) {
      return new ShoppingItemViewHolder(
          RowShoppingModeItemBinding.inflate(
              LayoutInflater.from(parent.getContext()),
              parent,
              false
          )
      );
    } else {
      return new ShoppingNotesViewHolder(
          RowShoppingModeBottomNotesBinding.inflate(
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

    int colorPrimary = ResUtil.getColorAttr(context, R.attr.colorPrimary);
    int colorTertiary = ResUtil.getColorAttr(context, R.attr.colorTertiary);

    int type = getItemViewType(viewHolder.getAdapterPosition());
    if (type == GroupedListItem.TYPE_HEADER) {
      RowShoppingModeGroupBinding binding = ((ShoppingGroupViewHolder) viewHolder).binding;
      String productGroupName = ((GroupHeader) groupedListItem).getGroupName();
      if (useSmallerFonts) {
        binding.name.setTextSize(16); // textAppearanceTitleMedium (Category)
        boolean isRtl = UiUtil.isLayoutRtl(binding.name.getContext());
        int dp8 = UiUtil.dpToPx(binding.name.getContext(), 8);
        LinearLayout.LayoutParams paramsGroup = new LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        );
        paramsGroup.setMargins(isRtl ? 0 : dp8 * 2, 0, isRtl ? dp8 * 2 : 0, dp8);
        binding.container.setLayoutParams(paramsGroup);
      }
      binding.name.setText(productGroupName);

      boolean isDone = productGroupName != null && productGroupName.equals(
          binding.name.getContext().getString(R.string.subtitle_done)
      );
      binding.name.setTextColor(isDone ? colorTertiary : colorPrimary);
      binding.separator.setBackgroundTintList(
          ColorStateList.valueOf(isDone ? colorTertiary : colorPrimary)
      );
      return;
    }
    if (type == GroupedListItem.TYPE_BOTTOM_NOTES) {
      ShoppingNotesViewHolder holder = (ShoppingNotesViewHolder) viewHolder;
      holder.binding.notes.setText(((ShoppingListBottomNotes) groupedListItem).getNotes());
      holder.binding.notes.setOnClickListener(view -> listener.onItemRowClicked(groupedListItem));
      return;
    }

    ShoppingListItem item = (ShoppingListItem) groupedListItem;
    RowShoppingModeItemBinding binding = ((ShoppingItemViewHolder) viewHolder).binding;

    ColorRoles colorBlue = ResUtil.getHarmonizedRoles(context, R.color.blue);

    if (useSmallerFonts) {
      int dp8 = UiUtil.dpToPx(binding.name.getContext(), 8);
      binding.card.setContentPadding(dp8, 0, dp8, 0);
      LinearLayout.LayoutParams paramsCard = new LinearLayout.LayoutParams(
          LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
      );
      paramsCard.setMargins(dp8 * 2, 0, dp8 * 2, dp8);
      binding.card.setLayoutParams(paramsCard);

      LinearLayout.LayoutParams paramsRow = new LinearLayout.LayoutParams(
          LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
      );
      paramsRow.setMargins(0, 0, 0, 0);
      binding.name.setLayoutParams(paramsRow);
      binding.name.setTextSize(18); // ListItem.Title
      binding.noteAsName.setLayoutParams(paramsRow);
      binding.noteAsName.setTextSize(18); // ListItem.Title
    }

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
      binding.name.setPaintFlags(binding.name.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
    } else {
      binding.name.setPaintFlags(binding.name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
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

    if (item.getNote() != null && !item.getNote().isEmpty()) {
      if (binding.name.getVisibility() == View.VISIBLE) {
        if (activeFields.contains(ShoppingModeViewModel.FIELD_NOTES)) {
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

    // PRODUCT DESCRIPTION

    if (activeFields.contains(ShoppingModeViewModel.FIELD_PRODUCT_DESCRIPTION)) {
      String productDescription = product != null ? product.getDescription() : null;
      Spanned description = productDescription != null ? Html.fromHtml(productDescription) : null;
      description = (Spanned) TextUtil.trimCharSequence(description);
      if (description != null && !description.toString().isEmpty()) {
        binding.cardDescription.setText(description.toString());
        binding.cardDescription.setRadius(UiUtil.dpToPx(context, 8));
        binding.cardDescription.setVisibility(View.VISIBLE);
      } else {
        binding.cardDescription.setVisibility(View.GONE);
      }
    } else {
      binding.cardDescription.setVisibility(View.GONE);
    }

    String pictureFileName = product != null ? product.getPictureFileName() : null;
    if (activeFields.contains(ShoppingModeViewModel.FIELD_PICTURE)
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

    binding.card.setAlpha(item.getDoneInt() == 1 ? 0.61f : 1);

    // CONTAINER

    binding.card.setOnClickListener(view -> listener.onItemRowClicked(groupedListItem));
  }

  private Chip createChip(Context ctx, String text) {
    @SuppressLint("InflateParams")
    Chip chip = (Chip) LayoutInflater.from(ctx)
        .inflate(R.layout.view_info_chip, null, false);
    chip.setChipBackgroundColor(ColorStateList.valueOf(SurfaceColors.SURFACE_4.getColor(ctx)));
    chip.setText(text);
    chip.setEnabled(false);
    chip.setClickable(false);
    chip.setFocusable(false);
    chip.setChipMinHeight(
        useSmallerFonts ? UiUtil.dpToPx(ctx, 28) : UiUtil.dpToPx(ctx, 32)
    );
    chip.setTextSize(
        useSmallerFonts ? 12 : 14
    );
    return chip;
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
    ArrayList<GroupedListItem> newGroupedListItems = getGroupedListItems(
        context, shoppingListItems,
        productGroupHashMap, productHashMap, productNamesHashMap, storeHashMap,
        productLastPurchasedHashMap, shoppingListItemAmountsHashMap,
        shoppingListNotes, groupingMode, priceTrackingEnabled, decimalPlacesPriceDisplay,
        currency, showDoneItems);
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
    diffResult.dispatchUpdatesTo(new AdapterListUpdateCallback(this, linearLayoutManager));
  }

  @Override
  public int getItemCount() {
    return groupedListItems.size();
  }

  public interface ShoppingModeItemClickListener {

    void onItemRowClicked(GroupedListItem groupedListItem);
  }

  /**
   * Custom ListUpdateCallback that prevents RecyclerView from scrolling down if top item is moved.
   */
  public static final class AdapterListUpdateCallback implements ListUpdateCallback {

    @NonNull
    private final ShoppingModeItemAdapter mAdapter;
    private final LinearLayoutManager linearLayoutManager;

    public AdapterListUpdateCallback(
        @NonNull ShoppingModeItemAdapter adapter,
        LinearLayoutManager linearLayoutManager
    ) {
      this.mAdapter = adapter;
      this.linearLayoutManager = linearLayoutManager;
    }

    @Override
    public void onInserted(int position, int count) {
      mAdapter.notifyItemRangeInserted(position, count);
    }

    @Override
    public void onRemoved(int position, int count) {
      mAdapter.notifyItemRangeRemoved(position, count);
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {
      // workaround for https://github.com/patzly/grocy-android/issues/439
      // figure out the position of the first visible item
      int firstPos = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
      int offsetTop = 0;
      if(firstPos >= 0) {
        View firstView = linearLayoutManager.findViewByPosition(firstPos);
        if (firstView != null) {
          offsetTop = linearLayoutManager.getDecoratedTop(firstView)
              - linearLayoutManager.getTopDecorationHeight(firstView);
        }
      }

      mAdapter.notifyItemMoved(fromPosition, toPosition);

      // reapply the saved position
      if(firstPos >= 0) {
        linearLayoutManager.scrollToPositionWithOffset(firstPos, offsetTop);
      }
    }

    @Override
    public void onChanged(int position, int count, Object payload) {
      mAdapter.notifyItemRangeChanged(position, count, payload);
    }
  }
}
