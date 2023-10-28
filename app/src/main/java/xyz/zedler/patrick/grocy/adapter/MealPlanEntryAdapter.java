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
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.load.model.LazyHeaders;
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
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Recipe;
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
  private final List<String> activeFields;
  private final PluralUtil pluralUtil;
  private final GrocyApi grocyApi;
  private final LazyHeaders grocyAuthHeaders;
  private final int maxDecimalPlacesAmount;
  private final int decimalPlacesPriceDisplay;
  private final String currency;
  private final boolean priceTrackingEnabled;

  public MealPlanEntryAdapter(Context context, GrocyApi grocyApi, LazyHeaders grocyAuthHeaders) {
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
    this.recipeHashMap = new HashMap<>();
    this.productHashMap = new HashMap<>();
    this.quantityUnitHashMap = new HashMap<>();
    this.activeFields = new ArrayList<>();
    this.activeFields.add(MealPlanViewModel.FIELD_PICTURE);
    this.pluralUtil = new PluralUtil(context);
    this.grocyApi = grocyApi;
    this.grocyAuthHeaders = grocyAuthHeaders;
    this.groupedListItems = new ArrayList<>();
  }

  static ArrayList<GroupedListItem> getGroupedListItems(
      List<MealPlanEntry> mealPlanEntries,
      List<MealPlanSection> mealPlanSections
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
    ColorRoles colorBlue = ResUtil.getHarmonizedRoles(context, R.color.blue);

    binding.title.setText(entry.getType());
    binding.subtitle.setVisibility(View.GONE);
    binding.picture.setVisibility(View.GONE);
    binding.picturePlaceholder.setVisibility(View.GONE);
    binding.flexboxLayout.setVisibility(View.GONE);

    if (entry.getType().equals(MealPlanEntry.TYPE_RECIPE)) {
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

      double servings = NumUtil.isStringDouble(entry.getRecipeServings()) ? Double.parseDouble(
          entry.getRecipeServings()) : 1;
      binding.subtitle.setText(
          pluralUtil.getQuantityString(R.plurals.msg_servings, servings, maxDecimalPlacesAmount)
      );
      binding.subtitle.setVisibility(View.VISIBLE);

      ChipUtil chipUtil = new ChipUtil(context);
      binding.flexboxLayout.removeAllViews();

      /*if (activeFields.contains(MealPlanViewModel.FIELD_DUE_SCORE)
          && recipeFulfillment != null) {
        binding.flexboxLayout.addView(chipUtil.createRecipeDueScoreChip(recipeFulfillment.getDueScore()));
      }
      if (activeFields.contains(MealPlanViewModel.FIELD_FULFILLMENT)
          && recipeFulfillment != null) {
        binding.flexboxLayout.addView(chipUtil.createRecipeFulfillmentChip(recipeFulfillment));
      }
      if (activeFields.contains(MealPlanViewModel.FIELD_CALORIES)
          && recipeFulfillment != null) {
        binding.flexboxLayout.addView(chipUtil.createTextChip(NumUtil.trimAmount(
            recipeFulfillment.getCalories(), maxDecimalPlacesAmount
        ) + " " + energyUnit));
      }
      for (String activeField : activeFields) {
        if (activeField.startsWith(Userfield.NAME_PREFIX)) {
          String userfieldName = activeField.substring(
              Userfield.NAME_PREFIX.length()
          );
          Userfield userfield = userfieldHashMap.get(userfieldName);
          if (userfield == null) continue;
          Chip chipUserfield = chipUtil.createUserfieldChip(
              userfield,
              recipe.getUserfields().get(userfieldName)
          );
          if (chipUserfield != null) binding.flexboxLayout.addView(chipUserfield);
        }
      }*/

      binding.flexboxLayout.setVisibility(binding.flexboxLayout.getChildCount() > 0 ? View.VISIBLE : View.GONE);

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
    } else if (entry.getType().equals(MealPlanEntry.TYPE_NOTE)) {
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
    } else if (entry.getType().equals(MealPlanEntry.TYPE_PRODUCT)) {
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
      double amount = NumUtil.isStringDouble(entry.getProductAmount()) ? Double.parseDouble(
          entry.getProductAmount()) : 1;
      int quId = NumUtil.isStringInt(entry.getProductQuId())
          ? Integer.parseInt(entry.getProductQuId()) : -1;
      QuantityUnit quantityUnit = quId != -1 ? quantityUnitHashMap.get(quId) : null;
      if (quantityUnit != null) {
        binding.subtitle.setText(context.getString(
            R.string.subtitle_amount,
            NumUtil.trimAmount(amount, maxDecimalPlacesAmount),
            pluralUtil.getQuantityUnitPlural(quantityUnit, amount)
        ));
      } else {
        binding.subtitle.setText(NumUtil.trimAmount(amount, maxDecimalPlacesAmount));
      }
      binding.subtitle.setVisibility(View.VISIBLE);

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
    }

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
      HashMap<Integer, QuantityUnit> quantityUnitHashMap
  ) {
    List<GroupedListItem> newGroupedListItems = getGroupedListItems(
        mealPlanEntries, mealPlanSections
    );
    DiffCallback diffCallback = new DiffCallback(
        this.groupedListItems,
        newGroupedListItems,
        this.recipeHashMap,
        recipeHashMap,
        this.productHashMap,
        productHashMap,
        this.quantityUnitHashMap,
        quantityUnitHashMap
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

    public DiffCallback(
        List<GroupedListItem> oldItems,
        List<GroupedListItem> newItems,
        HashMap<Integer, Recipe> oldRecipeHashMap,
        HashMap<Integer, Recipe> newRecipeHashMap,
        HashMap<Integer, Product> oldProductHashMap,
        HashMap<Integer, Product> newProductHashMap,
        HashMap<Integer, QuantityUnit> oldQuantityUnitHashMap,
        HashMap<Integer, QuantityUnit> newQuantityUnitHashMap
    ) {
      this.oldItems = oldItems;
      this.newItems = newItems;
      this.oldRecipeHashMap = oldRecipeHashMap;
      this.newRecipeHashMap = newRecipeHashMap;
      this.oldProductHashMap = oldProductHashMap;
      this.newProductHashMap = newProductHashMap;
      this.oldQuantityUnitHashMap = oldQuantityUnitHashMap;
      this.newQuantityUnitHashMap = newQuantityUnitHashMap;
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

        if (newItem.getType() != null && !newItem.getType().equals(oldItem.getType())) {
          return false;
        }
        if (newItem.getType().equals(MealPlanEntry.TYPE_RECIPE)) {
          if (!Objects.equals(newItem.getRecipeId(), oldItem.getRecipeId())) {
            return false;
          }
          if (NumUtil.isStringInt(newItem.getRecipeId())
              && NumUtil.isStringInt(oldItem.getRecipeId())) {
            Recipe newRecipe = newRecipeHashMap.get(Integer.parseInt(newItem.getRecipeId()));
            Recipe oldRecipe = oldRecipeHashMap.get(Integer.parseInt(oldItem.getRecipeId()));
            if (newRecipe == null || oldRecipe == null) {
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
          if (NumUtil.isStringInt(newItem.getProductId())
              && NumUtil.isStringInt(oldItem.getProductId())) {
            Product newItemProduct = newProductHashMap.get(Integer.parseInt(newItem.getProductId()));
            Product oldItemProduct = oldProductHashMap.get(Integer.parseInt(oldItem.getProductId()));
            if (newItemProduct == null || oldItemProduct == null) {
              return false;
            }
            if (!newItemProduct.equals(oldItemProduct)) {
              return false;
            }
          }
          if (NumUtil.isStringInt(newItem.getProductQuId())
              && NumUtil.isStringInt(oldItem.getProductQuId())) {
            QuantityUnit newItemQu = newQuantityUnitHashMap.get(Integer.parseInt(newItem.getProductQuId()));
            QuantityUnit oldItemQu = oldQuantityUnitHashMap.get(Integer.parseInt(oldItem.getProductQuId()));
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
      if (viewHolder instanceof MealPlanGroupViewHolder) return 0;
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
