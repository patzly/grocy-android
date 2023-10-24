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
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.color.ColorRoles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants.PREF;
import xyz.zedler.patrick.grocy.Constants.SETTINGS.STOCK;
import xyz.zedler.patrick.grocy.Constants.SETTINGS_DEFAULT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowMealPlanEntryBinding;
import xyz.zedler.patrick.grocy.databinding.RowMealPlanEntryConnectionBinding;
import xyz.zedler.patrick.grocy.databinding.RowMealPlanSectionHeaderBinding;
import xyz.zedler.patrick.grocy.model.GroupHeader;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.MealPlanEntry;
import xyz.zedler.patrick.grocy.model.MealPlanEntryConnection;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.view.MaterialTimelineView;

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
    GroupHeader groupHeader = new GroupHeader("Frühstück");
    groupHeader.setDisplayDivider(false);
    groupedListItems.add(groupHeader);
    groupedListItems.add(new MealPlanEntryConnection());
    for (MealPlanEntry entry : mealPlanEntries) {
      groupedListItems.add(entry);

      if (mealPlanEntries.size() > 1
          && mealPlanEntries.indexOf(entry) < mealPlanEntries.size()-1) {
        groupedListItems.add(new MealPlanEntryConnection());
      }
      int index = groupedListItems.indexOf(entry);
      if (index == 0) {
        entry.setItemPosition(MaterialTimelineView.POSITION_FIRST);
      } else if (index == groupedListItems.size()-1) {
        entry.setItemPosition(MaterialTimelineView.POSITION_LAST);
      } else {
        entry.setItemPosition(MaterialTimelineView.POSITION_MIDDLE);
      }
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

  public static class MealPlanEntryConnectionViewHolder extends ViewHolder {

    private final RowMealPlanEntryConnectionBinding binding;

    public MealPlanEntryConnectionViewHolder(RowMealPlanEntryConnectionBinding binding) {
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
    if (viewType == GroupedListItem.TYPE_CONNECTION) {
      return new MealPlanEntryConnectionViewHolder(
          RowMealPlanEntryConnectionBinding.inflate(
              LayoutInflater.from(parent.getContext()),
              parent,
              false
          )
      );
    } else if (viewType == GroupedListItem.TYPE_HEADER) {
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

    if (type == GroupedListItem.TYPE_CONNECTION) {
      return;
    } else if (type == GroupedListItem.TYPE_HEADER) {
      GroupHeader groupHeader = (GroupHeader) groupedListItem;
      RowMealPlanSectionHeaderBinding binding = ((MealPlanGroupViewHolder) viewHolder).binding;

      Context context = binding.getRoot().getContext();
      ColorRoles colorBlue = ResUtil.getHarmonizedRoles(context, R.color.blue);
      binding.timelineView.setBackgroundColor(colorBlue.getAccentContainer());
      binding.timelineView.setPosition(groupHeader.getDisplayDivider() == 1
          ? MaterialTimelineView.POSITION_MIDDLE : MaterialTimelineView.POSITION_FIRST);
      binding.name.setTextColor(colorBlue.getOnAccentContainer());
      binding.name.setText(groupHeader.getGroupName());
      return;
    }

    MealPlanEntry entry = (MealPlanEntry) groupedListItem;
    RowMealPlanEntryBinding binding = ((MealPlanEntryViewHolder) viewHolder).binding;
    binding.timelineView.setPosition(entry.getItemPosition());

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

  private void showOrHideAllConnections(RecyclerView recyclerView, boolean show) {
    for (int i = 0; i < recyclerView.getChildCount(); i++) {
      View child = recyclerView.getChildAt(i);
      RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(child);
      if (viewHolder instanceof MealPlanEntryConnectionViewHolder) {
        ((MealPlanEntryConnectionViewHolder) viewHolder).binding.timelineView
            .setShowLineAndRadio(show);
        ((MealPlanEntryConnectionViewHolder) viewHolder).binding.timelineView.invalidate();
      } else if (viewHolder instanceof MealPlanEntryViewHolder) {
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
      } else if (oldItemType == GroupedListItem.TYPE_CONNECTION) {
        MealPlanEntryConnection newConnection = (MealPlanEntryConnection) newItems.get(newItemPos);
        MealPlanEntryConnection oldConnection = (MealPlanEntryConnection) oldItems.get(oldItemPos);
        return newConnection.equals(oldConnection);
      } else {
        GroupHeader newGroup = (GroupHeader) newItems.get(newItemPos);
        GroupHeader oldGroup = (GroupHeader) oldItems.get(oldItemPos);
        return newGroup.equals(oldGroup);
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
      if (viewHolder instanceof MealPlanGroupViewHolder
          || viewHolder instanceof MealPlanEntryConnectionViewHolder) return 0;
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
