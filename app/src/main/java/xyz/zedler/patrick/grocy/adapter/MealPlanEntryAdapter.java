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
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.color.ColorRoles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowMealPlanEntryBinding;
import xyz.zedler.patrick.grocy.databinding.RowShoppingListBottomNotesBinding;
import xyz.zedler.patrick.grocy.databinding.RowShoppingListGroupBinding;
import xyz.zedler.patrick.grocy.model.GroupHeader;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.MealPlanEntry;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;

public class MealPlanEntryAdapter extends
    RecyclerView.Adapter<MealPlanEntryAdapter.ViewHolder> {

  private final static String TAG = MealPlanEntryAdapter.class.getSimpleName();

  private final List<GroupedListItem> groupedListItems;
  private final HashMap<Integer, Product> productHashMap;
  private final HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private final PluralUtil pluralUtil;
  private String groupingMode;
  private String extraField;
  private final int maxDecimalPlacesAmount;
  private final int decimalPlacesPriceDisplay;
  private final String currency;
  private final boolean priceTrackingEnabled;

  public MealPlanEntryAdapter(
      Context context
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
    this.productHashMap = new HashMap<>();
    this.quantityUnitHashMap = new HashMap<>();
    this.pluralUtil = new PluralUtil(context);
    this.groupingMode = groupingMode;
    this.extraField = extraField;
    this.groupedListItems = new ArrayList<>();
  }

  static ArrayList<GroupedListItem> getGroupedListItems(
      List<MealPlanEntry> mealPlanEntries
  ) {
    ArrayList<GroupedListItem> groupedListItems = new ArrayList<>();
    if (mealPlanEntries == null || mealPlanEntries.isEmpty()) {
      return groupedListItems;
    }
    groupedListItems.addAll(mealPlanEntries);
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

    private final RowShoppingListGroupBinding binding;

    public MealPlanGroupViewHolder(RowShoppingListGroupBinding binding) {
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
        GroupedListItem.CONTEXT_MEAL_PLAN
    );
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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

    MealPlanEntry entry = (MealPlanEntry) groupedListItem;
    RowMealPlanEntryBinding binding = ((MealPlanEntryViewHolder) viewHolder).binding;

    Context context = binding.getRoot().getContext();
    ColorRoles colorBlue = ResUtil.getHarmonizedRoles(context, R.color.blue);

    // NAME
    binding.name.setText(entry.getType());

  }

  @Override
  public int getItemCount() {
    return groupedListItems.size();
  }

  public List<GroupedListItem> getGroupedListItems() {
    return groupedListItems;
  }

  public void onItemMove(int fromPosition, int toPosition) {
    // Collections.swap(mData, fromPosition, toPosition);
    notifyItemMoved(fromPosition, toPosition);
  }

  public void updateData(
      List<MealPlanEntry> mealPlanEntries
  ) {
    List<GroupedListItem> newGroupedListItems = getGroupedListItems(mealPlanEntries);
    DiffCallback diffCallback = new DiffCallback(
        this.groupedListItems,
        newGroupedListItems
    );

    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.groupedListItems.clear();
    this.groupedListItems.addAll(newGroupedListItems);
    diffResult.dispatchUpdatesTo(this);
  }

  static class DiffCallback extends DiffUtil.Callback {

    List<GroupedListItem> oldItems;
    List<GroupedListItem> newItems;

    public DiffCallback(
        List<GroupedListItem> oldItems,
        List<GroupedListItem> newItems
    ) {
      this.oldItems = oldItems;
      this.newItems = newItems;
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

        return newItem.equals(oldItem);
      } else {
        GroupHeader newGroup = (GroupHeader) newItems.get(newItemPos);
        GroupHeader oldGroup = (GroupHeader) oldItems.get(oldItemPos);
        return newGroup.equals(oldGroup);
      }
    }
  }

  public static class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private final MealPlanEntryAdapter adapter;
    private final MoveToOtherPageListener movePageListener;

    public SimpleItemTouchHelperCallback(
        MealPlanEntryAdapter adapter,
        MoveToOtherPageListener movePageListener
    ) {
      this.adapter = adapter;
      this.movePageListener = movePageListener;
    }

    @Override
    public boolean isLongPressDragEnabled() {
      return true;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
      int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN
          | ItemTouchHelper.START | ItemTouchHelper.END;
      return makeMovementFlags(dragFlags, 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
      adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
      return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
      super.clearView(recyclerView, viewHolder);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
      super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

      if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive) {
        float itemCenterX = viewHolder.itemView.getX() + viewHolder.itemView.getWidth() / 2 + dX;
        float threshold = 0.8f; // 80% des Bildschirms, anpassen wie benötigt

        if (itemCenterX >= recyclerView.getWidth() * threshold) {
          movePageListener.moveToNextPage();
        } else if (itemCenterX <= recyclerView.getWidth() * (1 - threshold)) {
          movePageListener.moveToPreviousPage();
        }
      }
    }
  }

  public interface MoveToOtherPageListener {
    void moveToNextPage();
    void moveToPreviousPage();
  }

}
