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
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.ColorRoles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.RowMealPlanEntryBinding;
import xyz.zedler.patrick.grocy.databinding.RowMealPlanSectionHeaderBinding;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.MealPlanEntry;
import xyz.zedler.patrick.grocy.model.MealPlanSection;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductLastPurchased;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Recipe;
import xyz.zedler.patrick.grocy.model.RecipeFulfillment;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.model.Userfield;
import xyz.zedler.patrick.grocy.util.ArrayUtil;
import xyz.zedler.patrick.grocy.util.ChipUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PictureUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.view.MaterialTimelineView;
import xyz.zedler.patrick.grocy.viewmodel.MealPlanViewModel;

public class MealPlanEntryAdapter extends
    RecyclerView.Adapter<MealPlanEntryAdapter.ViewHolder> {

  private final static String TAG = MealPlanEntryAdapter.class.getSimpleName();

  private final List<GroupedListItem> groupedListItems;
  private final HashMap<Integer, Recipe> recipeHashMap;
  private final HashMap<Integer, Product> productHashMap;
  private final HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private final HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMap;
  private final HashMap<String, RecipeFulfillment> recipeResolvedFulfillmentHashMap;
  private final HashMap<Integer, StockItem> stockItemHashMap;
  private final HashMap<String, Userfield> userfieldHashMap;
  private final List<String> activeFields;
  private final PluralUtil pluralUtil;
  private final GrocyApi grocyApi;
  private final LazyHeaders grocyAuthHeaders;
  private final String date;
  private final String energyUnit;
  private final int maxDecimalPlacesAmount;
  private final int decimalPlacesPriceDisplay;
  private final String currency;

  public MealPlanEntryAdapter(
      Context context,
      GrocyApi grocyApi,
      LazyHeaders grocyAuthHeaders,
      String date
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
    this.energyUnit = sharedPrefs.getString(PREF.ENERGY_UNIT, PREF.ENERGY_UNIT_DEFAULT);
    this.date = date;
    this.recipeHashMap = new HashMap<>();
    this.productHashMap = new HashMap<>();
    this.quantityUnitHashMap = new HashMap<>();
    this.productLastPurchasedHashMap = new HashMap<>();
    this.recipeResolvedFulfillmentHashMap = new HashMap<>();
    this.stockItemHashMap = new HashMap<>();
    this.userfieldHashMap = new HashMap<>();
    this.activeFields = new ArrayList<>();
    this.pluralUtil = new PluralUtil(context);
    this.grocyApi = grocyApi;
    this.grocyAuthHeaders = grocyAuthHeaders;
    this.groupedListItems = new ArrayList<>();
  }

  static ArrayList<GroupedListItem> getGroupedListItems(
      List<MealPlanEntry> mealPlanEntries,
      List<MealPlanSection> mealPlanSections,
      boolean showDaySummary
  ) {
    ArrayList<GroupedListItem> groupedListItems = new ArrayList<>();
    if (mealPlanEntries == null || mealPlanEntries.isEmpty()) {
      return groupedListItems;
    }
    HashMap<Integer, List<MealPlanEntry>> mealPlanEntriesGrouped = new HashMap<>();
    for (MealPlanEntry entry : mealPlanEntries) {
      int sectionId = NumUtil.isStringInt(entry.getSectionId())
          ? Integer.parseInt(entry.getSectionId()) : -1;
      List<MealPlanEntry> list = mealPlanEntriesGrouped.get(sectionId);
      if (list == null) {
        list = new ArrayList<>();
        mealPlanEntriesGrouped.put(sectionId, list);
      }
      list.add(entry);
    }
    if (showDaySummary) {
      MealPlanEntry dayInfo = new MealPlanEntry();
      dayInfo.setId(-1);
      dayInfo.setType(MealPlanEntry.TYPE_DAY_INFO);
      groupedListItems.add(dayInfo);
    }
    for (MealPlanSection section : mealPlanSections) {
      List<MealPlanEntry> list = mealPlanEntriesGrouped.get(section.getId());
      if (list == null || list.isEmpty()) {
        continue;
      }
      if (section.getName() != null && !section.getName().isBlank()) {
        if (!groupedListItems.isEmpty()) {
          GroupedListItem lastItem = groupedListItems.get(groupedListItems.size()-1);
          if (lastItem instanceof MealPlanEntry) {
            ((MealPlanEntry) lastItem).setItemPosition(groupedListItems.size() > 1
                ? MaterialTimelineView.POSITION_MIDDLE : MaterialTimelineView.POSITION_FIRST);
          }
        }
        groupedListItems.add(section);
        section.setTopItem(groupedListItems.indexOf(section) == 0);
      }

      for (MealPlanEntry entry : list) {
        groupedListItems.add(entry);
        int index = groupedListItems.indexOf(entry);
        if (index == 0) {
          entry.setItemPosition(MaterialTimelineView.POSITION_FIRST);
        } else {
          entry.setItemPosition(MaterialTimelineView.POSITION_MIDDLE);
        }
      }
    }
    GroupedListItem lastItem = groupedListItems.get(groupedListItems.size()-1);
    if (lastItem instanceof MealPlanEntry) {
      ((MealPlanEntry) lastItem).setItemPosition(groupedListItems.size() > 1
          ? MaterialTimelineView.POSITION_LAST : MaterialTimelineView.POSITION_SINGLE);
    }
    return groupedListItems;
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View view) {
      super(view);
    }
  }

  public static class MealPlanEntryViewHolder extends ViewHolder {

    private final RowMealPlanEntryBinding binding;

    public MealPlanEntryViewHolder(RowMealPlanEntryBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public static class MealPlanGroupViewHolder extends ViewHolder {

    private final RowMealPlanSectionHeaderBinding binding;

    public MealPlanGroupViewHolder(RowMealPlanSectionHeaderBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  @Override
  public int getItemViewType(int position) {
    return GroupedListItem.getType(
        groupedListItems.get(position),
        GroupedListItem.CONTEXT_MEAL_PLAN
    );
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == GroupedListItem.TYPE_HEADER) {
      return new MealPlanGroupViewHolder(
          RowMealPlanSectionHeaderBinding.inflate(
              LayoutInflater.from(parent.getContext()),
              parent,
              false
          )
      );
    }
    return new MealPlanEntryViewHolder(
        RowMealPlanEntryBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        )
    );
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int positionDoNotUse) {

    GroupedListItem groupedListItem = groupedListItems.get(viewHolder.getAdapterPosition());

    int type = getItemViewType(viewHolder.getAdapterPosition());

    if (type == GroupedListItem.TYPE_HEADER) {
      MealPlanSection section = (MealPlanSection) groupedListItem;
      RowMealPlanSectionHeaderBinding binding = ((MealPlanGroupViewHolder) viewHolder).binding;

      binding.timelineView.setPosition(section.isTopItem()
          ? MaterialTimelineView.POSITION_FIRST : MaterialTimelineView.POSITION_MIDDLE);
      binding.name.setText(section.getName());
      if (section.getTimeInfo() != null && !section.getTimeInfo().isEmpty()) {
        binding.time.setText(section.getTimeInfo());
        binding.time.setVisibility(View.VISIBLE);
      } else {
        binding.time.setVisibility(View.GONE);
      }
      return;
    }

    MealPlanEntry entry = (MealPlanEntry) groupedListItem;
    RowMealPlanEntryBinding binding = ((MealPlanEntryViewHolder) viewHolder).binding;
    binding.timelineView.setPosition(entry.getItemPosition());

    Context context = binding.getRoot().getContext();

    binding.title.setText(entry.getType());
    binding.picture.setVisibility(View.GONE);
    binding.picturePlaceholder.setVisibility(View.GONE);
    binding.flexboxLayout.setVisibility(View.GONE);
    ChipUtil chipUtil = new ChipUtil(context);
    binding.flexboxLayout.removeAllViews();

    switch (entry.getType()) {
      case MealPlanEntry.TYPE_RECIPE: {
        if (!NumUtil.isStringInt(entry.getRecipeId())) {
          binding.title.setText(entry.getType());
          return;
        }
        Recipe recipe = recipeHashMap.get(Integer.parseInt(entry.getRecipeId()));
        if (recipe == null) {
          binding.title.setText(entry.getType());
          return;
        }
        binding.title.setText(recipe.getName());

        if (activeFields.contains(MealPlanViewModel.FIELD_AMOUNT)) {
          double servings = NumUtil.isStringDouble(entry.getRecipeServings()) ? Double.parseDouble(
              entry.getRecipeServings()) : 1;
          String amount = pluralUtil
              .getQuantityString(R.plurals.msg_servings, servings, maxDecimalPlacesAmount);
          binding.flexboxLayout.addView(chipUtil.createTextChip(amount));
        }
        RecipeFulfillment recipeFulfillment = recipeResolvedFulfillmentHashMap
            .get(date + "#" + entry.getId());
        if (activeFields.contains(MealPlanViewModel.FIELD_FULFILLMENT)
            && recipeFulfillment != null) {
          binding.flexboxLayout.addView(chipUtil.createRecipeFulfillmentChip(recipeFulfillment));
        }
        if (activeFields.contains(MealPlanViewModel.FIELD_ENERGY)
            && recipeFulfillment != null) {
          binding.flexboxLayout.addView(chipUtil.createTextChip(NumUtil.trimAmount(
              recipeFulfillment.getCalories(), maxDecimalPlacesAmount
          ) + " " + energyUnit, context.getString(R.string.subtitle_per_serving)));
        }
        if (activeFields.contains(MealPlanViewModel.FIELD_PRICE)
            && recipeFulfillment != null) {
          binding.flexboxLayout.addView(chipUtil.createTextChip(context.getString(
              R.string.property_price_with_currency,
              NumUtil.trimPrice(recipeFulfillment.getCostsPerServing(), decimalPlacesPriceDisplay),
              currency
          ), context.getString(R.string.title_total_price)));
        }
        for (String activeField : activeFields) {
          if (activeField.startsWith(Userfield.NAME_PREFIX)) {
            String userfieldName = activeField.substring(
                Userfield.NAME_PREFIX.length()
            );
            Userfield userfield = userfieldHashMap.get(userfieldName);
            if (userfield == null)
              continue;
            Chip chipUserfield = chipUtil.createUserfieldChip(
                userfield,
                recipe.getUserfields().get(userfieldName)
            );
            if (chipUserfield != null)
              binding.flexboxLayout.addView(chipUserfield);
          }
        }

        String pictureFileName = recipe.getPictureFileName();
        binding.picturePlaceholderIcon.setImageDrawable(ResourcesCompat.getDrawable(
            context.getResources(),
            R.drawable.ic_round_image,
            null
        ));
        if (activeFields.contains(MealPlanViewModel.FIELD_PICTURE)
            && pictureFileName != null && !pictureFileName.isEmpty()) {
          binding.picture.layout(0, 0, 0, 0);

          PictureUtil.loadPicture(
              binding.picture,
              null,
              binding.picturePlaceholder,
              grocyApi.getRecipePictureServeSmall(pictureFileName),
              grocyAuthHeaders,
              false
          );
        } else if (activeFields.contains(MealPlanViewModel.FIELD_PICTURE)) {
          binding.picture.setVisibility(View.GONE);
          binding.picturePlaceholder.setVisibility(View.VISIBLE);
        } else {
          binding.picture.setVisibility(View.GONE);
          binding.picturePlaceholder.setVisibility(View.GONE);
        }
        break;
      }
      case MealPlanEntry.TYPE_NOTE:
        binding.title.setText(entry.getNote());
        if (activeFields.contains(MealPlanViewModel.FIELD_PICTURE)) {
          binding.picturePlaceholderIcon.setImageDrawable(ResourcesCompat.getDrawable(
              context.getResources(),
              R.drawable.ic_round_short_text,
              null
          ));
          binding.picturePlaceholder.setVisibility(View.VISIBLE);
        } else {
          binding.picturePlaceholder.setVisibility(View.GONE);
        }
        break;
      case MealPlanEntry.TYPE_DAY_INFO:
        binding.title.setText(entry.getNote());
        RecipeFulfillment recipeFulfillment = recipeResolvedFulfillmentHashMap.get(date);
        if (activeFields.contains(MealPlanViewModel.FIELD_FULFILLMENT)
            && recipeFulfillment != null) {
          binding.flexboxLayout.addView(chipUtil.createRecipeFulfillmentChip(recipeFulfillment));
        }
        if (activeFields.contains(MealPlanViewModel.FIELD_ENERGY)
            && recipeFulfillment != null) {
          binding.flexboxLayout.addView(chipUtil.createTextChip(NumUtil.trimAmount(
              recipeFulfillment.getCalories(), maxDecimalPlacesAmount
          ) + " " + energyUnit));
        }
        if (activeFields.contains(MealPlanViewModel.FIELD_PRICE)
            && recipeFulfillment != null) {
          binding.flexboxLayout.addView(chipUtil.createTextChip(context.getString(
              R.string.property_price_with_currency,
              NumUtil.trimPrice(recipeFulfillment.getCostsPerServing(), decimalPlacesPriceDisplay),
              currency
          ), context.getString(R.string.title_total_price)));
        }
        if (activeFields.contains(MealPlanViewModel.FIELD_PICTURE)) {
          binding.title.setText(R.string.property_day_summary);
          binding.picturePlaceholderIcon.setImageDrawable(ResourcesCompat.getDrawable(
              context.getResources(),
              R.drawable.ic_round_summarize,
              null
          ));
          binding.picturePlaceholder.setVisibility(View.VISIBLE);
        } else {
          binding.picturePlaceholder.setVisibility(View.GONE);
        }
        break;
      case MealPlanEntry.TYPE_PRODUCT: {
        if (!NumUtil.isStringInt(entry.getProductId())) {
          binding.title.setText(entry.getType());
          return;
        }
        Product product = productHashMap.get(Integer.parseInt(entry.getProductId()));
        if (product == null) {
          binding.title.setText(entry.getType());
          return;
        }
        binding.title.setText(product.getName());

        if (activeFields.contains(MealPlanViewModel.FIELD_AMOUNT)) {
          double amount = NumUtil.isStringDouble(entry.getProductAmount()) ? Double.parseDouble(
              entry.getProductAmount()) : 1;
          int quId = NumUtil.isStringInt(entry.getProductQuId())
              ? Integer.parseInt(entry.getProductQuId()) : -1;
          if (quId == -1 && NumUtil.isStringInt(product.getQuIdStock())) {
            quId = Integer.parseInt(product.getQuIdStock());
          }
          QuantityUnit quantityUnit = quId != -1 ? quantityUnitHashMap.get(quId) : null;
          String amountText;
          if (quantityUnit != null) {
            amountText = context.getString(
                R.string.subtitle_amount,
                NumUtil.trimAmount(amount, maxDecimalPlacesAmount),
                pluralUtil.getQuantityUnitPlural(quantityUnit, amount)
            );
          } else {
            amountText = NumUtil.trimAmount(amount, maxDecimalPlacesAmount);
          }
          binding.flexboxLayout.addView(chipUtil.createTextChip(amountText));
        }
        if (activeFields.contains(MealPlanViewModel.FIELD_FULFILLMENT)) {
          StockItem stockItem = stockItemHashMap.get(product.getId());
          binding.flexboxLayout.addView(chipUtil.createProductFulfillmentChip(
              stockItem != null
                  && NumUtil.isStringDouble(entry.getProductAmount())
                  && stockItem.getAmountAggregatedDouble()
                  >= Double.parseDouble(entry.getProductAmount()))
          );
        }
        if (activeFields.contains(MealPlanViewModel.FIELD_ENERGY)
            && NumUtil.isStringDouble(product.getCalories())) {
          double calories = Double.parseDouble(product.getCalories());
          binding.flexboxLayout.addView(chipUtil.createTextChip(NumUtil.trimAmount(
              calories, maxDecimalPlacesAmount
          ) + " " + energyUnit, context.getString(R.string.subtitle_per_serving)));
        }
        if (activeFields.contains(MealPlanViewModel.FIELD_PRICE)) {
          ProductLastPurchased p = productLastPurchasedHashMap.get(product.getId());
          if (p != null && NumUtil.isStringDouble(p.getPrice())
              && NumUtil.isStringDouble(entry.getProductAmount())) {
            binding.flexboxLayout.addView(chipUtil.createTextChip(context.getString(
                R.string.property_price_with_currency,
                NumUtil.trimPrice(Double.parseDouble(p.getPrice())
                    * Double.parseDouble(entry.getProductAmount()), decimalPlacesPriceDisplay),
                currency
            ), context.getString(R.string.title_total_price)));
          }
        }
        for (String activeField : activeFields) {
          if (activeField.startsWith(Userfield.NAME_PREFIX)) {
            String userfieldName = activeField.substring(
                Userfield.NAME_PREFIX.length()
            );
            Userfield userfield = userfieldHashMap.get(userfieldName);
            if (userfield == null)
              continue;
            Chip chipUserfield = chipUtil.createUserfieldChip(
                userfield,
                product.getUserfields().get(userfieldName)
            );
            if (chipUserfield != null)
              binding.flexboxLayout.addView(chipUserfield);
          }
        }

        String pictureFileName = product.getPictureFileName();
        binding.picturePlaceholderIcon.setImageDrawable(ResourcesCompat.getDrawable(
            context.getResources(),
            R.drawable.ic_round_image,
            null
        ));
        if (activeFields.contains(MealPlanViewModel.FIELD_PICTURE)
            && pictureFileName != null && !pictureFileName.isEmpty()) {
          binding.picture.layout(0, 0, 0, 0);

          PictureUtil.loadPicture(
              binding.picture,
              null,
              binding.picturePlaceholder,
              grocyApi.getProductPictureServeSmall(pictureFileName),
              grocyAuthHeaders,
              false
          );
        } else if (activeFields.contains(MealPlanViewModel.FIELD_PICTURE)) {
          binding.picture.setVisibility(View.GONE);
          binding.picturePlaceholder.setVisibility(View.VISIBLE);
        } else {
          binding.picture.setVisibility(View.GONE);
          binding.picturePlaceholder.setVisibility(View.GONE);
        }
        break;
      }
    }

    binding.flexboxLayout.setVisibility(binding.flexboxLayout.getChildCount() > 0 ? View.VISIBLE : View.GONE);

  }

  @Override
  public int getItemCount() {
    return groupedListItems.size();
  }

  public List<GroupedListItem> getGroupedListItems() {
    return groupedListItems;
  }

  private void showOrHideAllConnections(RecyclerView recyclerView, boolean show) {
    for (int i = 0; i < recyclerView.getChildCount(); i++) {
      View child = recyclerView.getChildAt(i);
      RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(child);
      if (viewHolder instanceof MealPlanEntryViewHolder) {
        ((MealPlanEntryViewHolder) viewHolder).binding.timelineView
            .setShowLineAndRadio(show);
        ((MealPlanEntryViewHolder) viewHolder).binding.timelineView.invalidate();
      } else if (viewHolder instanceof MealPlanGroupViewHolder) {
        ((MealPlanGroupViewHolder) viewHolder).binding.timelineView
            .setShowLineAndRadio(show);
        ((MealPlanGroupViewHolder) viewHolder).binding.timelineView.invalidate();
      }
    }
  }

  public void updateData(
      List<MealPlanEntry> mealPlanEntries,
      List<MealPlanSection> mealPlanSections,
      HashMap<Integer, Recipe> recipeHashMap,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMap,
      HashMap<String, RecipeFulfillment> recipeResolvedFulfillmentHashMap,
      HashMap<Integer, StockItem> stockItemHashMap,
      HashMap<String, Userfield> userfieldHashMap,
      List<String> activeFields
  ) {
    List<GroupedListItem> newGroupedListItems = getGroupedListItems(
        mealPlanEntries,
        mealPlanSections,
        activeFields.contains(MealPlanViewModel.FIELD_DAY_SUMMARY)
            && (activeFields.contains(MealPlanViewModel.FIELD_PRICE)
            || activeFields.contains(MealPlanViewModel.FIELD_ENERGY)
            || activeFields.contains(MealPlanViewModel.FIELD_FULFILLMENT))
    );
    DiffCallback diffCallback = new DiffCallback(
        this.groupedListItems,
        newGroupedListItems,
        this.recipeHashMap,
        recipeHashMap,
        this.productHashMap,
        productHashMap,
        this.quantityUnitHashMap,
        quantityUnitHashMap,
        this.productLastPurchasedHashMap,
        productLastPurchasedHashMap,
        this.recipeResolvedFulfillmentHashMap,
        recipeResolvedFulfillmentHashMap,
        this.stockItemHashMap,
        stockItemHashMap,
        this.userfieldHashMap,
        userfieldHashMap,
        this.activeFields,
        activeFields,
        date
    );

    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.groupedListItems.clear();
    this.groupedListItems.addAll(newGroupedListItems);
    this.recipeHashMap.clear();
    this.recipeHashMap.putAll(recipeHashMap);
    this.productHashMap.clear();
    this.productHashMap.putAll(productHashMap);
    this.quantityUnitHashMap.clear();
    this.quantityUnitHashMap.putAll(quantityUnitHashMap);
    this.productLastPurchasedHashMap.clear();
    this.productLastPurchasedHashMap.putAll(productLastPurchasedHashMap);
    this.recipeResolvedFulfillmentHashMap.clear();
    this.recipeResolvedFulfillmentHashMap.putAll(recipeResolvedFulfillmentHashMap);
    this.stockItemHashMap.clear();
    this.stockItemHashMap.putAll(stockItemHashMap);
    this.userfieldHashMap.clear();
    this.userfieldHashMap.putAll(userfieldHashMap);
    this.activeFields.clear();
    this.activeFields.addAll(activeFields);
    diffResult.dispatchUpdatesTo(this);
  }

  static class DiffCallback extends DiffUtil.Callback {

    List<GroupedListItem> oldItems;
    List<GroupedListItem> newItems;
    HashMap<Integer, Recipe> oldRecipeHashMap;
    HashMap<Integer, Recipe> newRecipeHashMap;
    HashMap<Integer, Product> oldProductHashMap;
    HashMap<Integer, Product> newProductHashMap;
    HashMap<Integer, QuantityUnit> oldQuantityUnitHashMap;
    HashMap<Integer, QuantityUnit> newQuantityUnitHashMap;
    HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMapOld;
    HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMapNew;
    HashMap<String, RecipeFulfillment> oldRecipeResolvedFulfillmentHashMap;
    HashMap<String, RecipeFulfillment> newRecipeResolvedFulfillmentHashMap;
    HashMap<Integer, StockItem> oldStockItemHashMap;
    HashMap<Integer, StockItem> newStockItemHashMap;
    HashMap<String, Userfield> oldUserfieldHashMap;
    HashMap<String, Userfield> newUserfieldHashMap;
    List<String> activeFieldsOld;
    List<String> activeFieldsNew;
    String date;

    public DiffCallback(
        List<GroupedListItem> oldItems,
        List<GroupedListItem> newItems,
        HashMap<Integer, Recipe> oldRecipeHashMap,
        HashMap<Integer, Recipe> newRecipeHashMap,
        HashMap<Integer, Product> oldProductHashMap,
        HashMap<Integer, Product> newProductHashMap,
        HashMap<Integer, QuantityUnit> oldQuantityUnitHashMap,
        HashMap<Integer, QuantityUnit> newQuantityUnitHashMap,
        HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMapOld,
        HashMap<Integer, ProductLastPurchased> productLastPurchasedHashMapNew,
        HashMap<String, RecipeFulfillment> oldRecipeResolvedFulfillmentHashMap,
        HashMap<String, RecipeFulfillment> newRecipeResolvedFulfillmentHashMap,
        HashMap<Integer, StockItem> oldStockItemHashMap,
        HashMap<Integer, StockItem> newStockItemHashMap,
        HashMap<String, Userfield> oldUserfieldHashMap,
        HashMap<String, Userfield> newUserfieldHashMap,
        List<String> activeFieldsOld,
        List<String> activeFieldsNew,
        String date
    ) {
      this.oldItems = oldItems;
      this.newItems = newItems;
      this.oldRecipeHashMap = oldRecipeHashMap;
      this.newRecipeHashMap = newRecipeHashMap;
      this.oldProductHashMap = oldProductHashMap;
      this.newProductHashMap = newProductHashMap;
      this.oldQuantityUnitHashMap = oldQuantityUnitHashMap;
      this.newQuantityUnitHashMap = newQuantityUnitHashMap;
      this.productLastPurchasedHashMapOld = productLastPurchasedHashMapOld;
      this.productLastPurchasedHashMapNew = productLastPurchasedHashMapNew;
      this.oldRecipeResolvedFulfillmentHashMap = oldRecipeResolvedFulfillmentHashMap;
      this.newRecipeResolvedFulfillmentHashMap = newRecipeResolvedFulfillmentHashMap;
      this.oldStockItemHashMap = oldStockItemHashMap;
      this.newStockItemHashMap = newStockItemHashMap;
      this.oldUserfieldHashMap = oldUserfieldHashMap;
      this.newUserfieldHashMap = newUserfieldHashMap;
      this.activeFieldsOld = activeFieldsOld;
      this.activeFieldsNew = activeFieldsNew;
      this.date = date;
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
          GroupedListItem.CONTEXT_MEAL_PLAN
      );
      int newItemType = GroupedListItem.getType(
          newItems.get(newItemPos),
          GroupedListItem.CONTEXT_MEAL_PLAN
      );
      if (oldItemType != newItemType) {
        return false;
      }
      if (oldItemType == GroupedListItem.TYPE_ENTRY) {
        MealPlanEntry newItem = (MealPlanEntry) newItems.get(newItemPos);
        MealPlanEntry oldItem = (MealPlanEntry) oldItems.get(oldItemPos);

        if (!compareContent) {
          return newItem.getId() == oldItem.getId();
        }
        if (!ArrayUtil.areListsEqualIgnoreOrder(activeFieldsOld, activeFieldsNew)) {
          return false;
        }
        if (!oldUserfieldHashMap.equals(newUserfieldHashMap)) {
          return false;
        }

        if (newItem.getType() != null && !newItem.getType().equals(oldItem.getType())) {
          return false;
        }
        if (newItem.getType().equals(MealPlanEntry.TYPE_RECIPE)) {
          if (!Objects.equals(newItem.getRecipeId(), oldItem.getRecipeId())) {
            return false;
          }
          if (NumUtil.isStringInt(newItem.getRecipeId())) {
            Recipe newRecipe = newRecipeHashMap.get(Integer.parseInt(newItem.getRecipeId()));
            Recipe oldRecipe = oldRecipeHashMap.get(Integer.parseInt(oldItem.getRecipeId()));
            if (newRecipe == null || oldRecipe == null) {
              return false;
            }
            RecipeFulfillment recipeFulfillmentOld = oldRecipeResolvedFulfillmentHashMap.get(date + "#" + oldItem.getId());
            RecipeFulfillment recipeFulfillmentNew = newRecipeResolvedFulfillmentHashMap.get(date + "#" + newItem.getId());
            if (recipeFulfillmentOld == null && recipeFulfillmentNew != null
                || recipeFulfillmentOld != null && recipeFulfillmentNew == null
                || recipeFulfillmentOld != null && !recipeFulfillmentOld.equals(recipeFulfillmentNew)) {
              return false;
            }
            if (!newRecipe.equals(oldRecipe)) {
              return false;
            }
          }
        } else if (newItem.getType().equals(MealPlanEntry.TYPE_PRODUCT)) {
          if (!Objects.equals(newItem.getProductId(), oldItem.getProductId())) {
            return false;
          }
          if (NumUtil.isStringInt(newItem.getProductId())) {
            Integer productIdOld = NumUtil.isStringInt(oldItem.getProductId())
                ? Integer.parseInt(oldItem.getProductId()) : null;
            if (productIdOld == null) {
              return false;
            }
            int productIdNew = Integer.parseInt(newItem.getProductId());
            Product newItemProduct = newProductHashMap.get(Integer.parseInt(newItem.getProductId()));
            Product oldItemProduct = oldProductHashMap.get(productIdOld);
            if (newItemProduct == null || oldItemProduct == null) {
              return false;
            }
            if (!newItemProduct.equals(oldItemProduct)) {
              return false;
            }

            StockItem newStockItem = newStockItemHashMap.get(Integer.parseInt(newItem.getProductId()));
            StockItem oldStockItem = oldStockItemHashMap.get(productIdOld);
            if (newStockItem == null || oldStockItem == null) {
              return false;
            }
            if (!newStockItem.equals(oldStockItem)) {
              return false;
            }

            if (activeFieldsNew.contains(MealPlanViewModel.FIELD_PRICE)) {
              ProductLastPurchased purchasedOld = productLastPurchasedHashMapOld.get(productIdOld);
              ProductLastPurchased purchasedNew = productLastPurchasedHashMapNew.get(productIdNew);
              if (purchasedOld == null && purchasedNew != null
                  || purchasedOld != null && purchasedNew != null && !purchasedOld.equals(
                  purchasedNew)) {
                return false;
              }
            }
          }
          if (NumUtil.isStringInt(newItem.getProductQuId())) {
            QuantityUnit newItemQu = newQuantityUnitHashMap.get(Integer.parseInt(newItem.getProductQuId()));
            QuantityUnit oldItemQu = NumUtil.isStringInt(oldItem.getProductQuId())
                ? oldQuantityUnitHashMap.get(Integer.parseInt(oldItem.getProductQuId())) : null;
            if (newItemQu == null || oldItemQu == null) {
              return false;
            }
            if (!newItemQu.equals(oldItemQu)) {
              return false;
            }
          }
        }

        return newItem.equals(oldItem);
      } else {
        MealPlanSection newItem = (MealPlanSection) newItems.get(newItemPos);
        MealPlanSection oldItem = (MealPlanSection) oldItems.get(oldItemPos);
        if (!compareContent) {
          return newItem.getId() == oldItem.getId();
        }
        return newItem.equals(oldItem);
      }
    }
  }

  public static class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private final MealPlanEntryAdapter adapter;
    private final RecyclerView recyclerView;

    public SimpleItemTouchHelperCallback(MealPlanEntryAdapter adapter, RecyclerView recyclerView) {
      this.adapter = adapter;
      this.recyclerView = recyclerView;
    }

    @Override
    public boolean isLongPressDragEnabled() {
      return true;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
      if (viewHolder instanceof MealPlanGroupViewHolder || true) return 0; // TODO: drag storing is not implementing
      int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
      return makeMovementFlags(dragFlags, 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
        @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
      int fromPosition = viewHolder.getAdapterPosition();
      int toPosition = target.getAdapterPosition();
      Collections.swap(adapter.getGroupedListItems(), fromPosition, toPosition);
      adapter.notifyItemMoved(fromPosition, toPosition);
      return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
    }

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
      super.onSelectedChanged(viewHolder, actionState);

      if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
        adapter.showOrHideAllConnections(recyclerView, false);
      } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
        adapter.showOrHideAllConnections(recyclerView, true);
      }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
      super.clearView(recyclerView, viewHolder);
    }
  }

}
