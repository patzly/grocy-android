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
 * Copyright (c) 2020-2021 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowShoppingBottomNotesBinding;
import xyz.zedler.patrick.grocy.databinding.RowShoppingGroupBinding;
import xyz.zedler.patrick.grocy.databinding.RowShoppingItemBinding;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListBottomNotes;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.PluralUtil;
import xyz.zedler.patrick.grocy.util.UnitUtil;

public class ShoppingModeItemAdapter extends
    RecyclerView.Adapter<ShoppingModeItemAdapter.ViewHolder> {

  private final LinearLayoutManager linearLayoutManager;
  private final ArrayList<GroupedListItem> groupedListItems;
  private final HashMap<Integer, Product> productHashMap;
  private final HashMap<Integer, QuantityUnit> quantityUnitHashMap;
  private final HashMap<Integer, Double> shoppingListItemAmountsHashMap;
  private final ArrayList<Integer> missingProductIds;
  private final ShoppingModeItemClickListener listener;
  private final PluralUtil pluralUtil;
  private boolean useSmallerFonts;

  public ShoppingModeItemAdapter(
      Context context,
      LinearLayoutManager linearLayoutManager,
      ArrayList<GroupedListItem> groupedListItems,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      HashMap<Integer, Double> shoppingListItemAmountsHashMap,
      ArrayList<Integer> missingProductIds,
      ShoppingModeItemClickListener listener,
      boolean useSmallerFonts
  ) {
    this.linearLayoutManager = linearLayoutManager;
    this.groupedListItems = new ArrayList<>(groupedListItems);
    this.productHashMap = new HashMap<>(productHashMap);
    this.quantityUnitHashMap = new HashMap<>(quantityUnitHashMap);
    this.shoppingListItemAmountsHashMap = new HashMap<>(shoppingListItemAmountsHashMap);
    this.missingProductIds = new ArrayList<>(missingProductIds);
    this.listener = listener;
    this.useSmallerFonts = useSmallerFonts;
    pluralUtil = new PluralUtil(context);
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View view) {
      super(view);
    }
  }

  public static class ShoppingItemViewHolder extends ViewHolder {

    private final RowShoppingItemBinding binding;

    public ShoppingItemViewHolder(RowShoppingItemBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public static class ShoppingGroupViewHolder extends ViewHolder {

    private final RowShoppingGroupBinding binding;

    public ShoppingGroupViewHolder(RowShoppingGroupBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public static class ShoppingNotesViewHolder extends ViewHolder {

    private final RowShoppingBottomNotesBinding binding;

    public ShoppingNotesViewHolder(RowShoppingBottomNotesBinding binding) {
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
          RowShoppingGroupBinding.inflate(
              LayoutInflater.from(parent.getContext()),
              parent,
              false
          )
      );
    } else if (viewType == GroupedListItem.TYPE_ENTRY) {
      return new ShoppingItemViewHolder(
          RowShoppingItemBinding.inflate(
              LayoutInflater.from(parent.getContext()),
              parent,
              false
          )
      );
    } else {
      return new ShoppingNotesViewHolder(
          RowShoppingBottomNotesBinding.inflate(
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
      ShoppingGroupViewHolder holder = (ShoppingGroupViewHolder) viewHolder;
      String productGroupName = ((ProductGroup) groupedListItem).getName();
      if (useSmallerFonts) {
        holder.binding.name.setTextSize(14.5f);
      }
      holder.binding.name.setText(productGroupName);
      if (!productGroupName.equals(holder.binding.name.getContext()
          .getString(R.string.subtitle_done))
      ) {
        holder.binding.name.setTextColor(ContextCompat.getColor(
            holder.binding.name.getContext(), R.color.retro_green_fg
        ));
        holder.binding.separator.setBackgroundTintList(ColorStateList.valueOf(
            ContextCompat.getColor(holder.binding.name.getContext(), R.color.retro_green_fg)
        ));
      } else {
        holder.binding.name.setTextColor(ContextCompat.getColor(
            holder.binding.name.getContext(), R.color.retro_yellow_fg
        ));
        holder.binding.separator.setBackgroundTintList(ColorStateList.valueOf(
            ContextCompat.getColor(holder.binding.name.getContext(), R.color.retro_yellow_fg)
        ));
      }
      return;
    }
    if (type == GroupedListItem.TYPE_BOTTOM_NOTES) {
      ShoppingNotesViewHolder holder = (ShoppingNotesViewHolder) viewHolder;
      holder.binding.notes.setText(((ShoppingListBottomNotes) groupedListItem).getNotes());
      holder.binding.notes.setOnClickListener(view -> listener.onItemRowClicked(groupedListItem));
      return;
    }

    ShoppingListItem item = (ShoppingListItem) groupedListItem;
    RowShoppingItemBinding binding = ((ShoppingItemViewHolder) viewHolder).binding;

    if (useSmallerFonts) {
      int px4 = UnitUtil.dpToPx(binding.name.getContext(), 4f);
      binding.card.setContentPadding(px4*2, px4, px4*2, px4);
      LinearLayout.LayoutParams paramsCard = new LinearLayout.LayoutParams(
          LayoutParams.MATCH_PARENT,
          LayoutParams.WRAP_CONTENT
      );
      paramsCard.setMargins(px4*4, px4, px4*4, px4);
      binding.card.setLayoutParams(paramsCard);

      LinearLayout.LayoutParams paramsRow = new LinearLayout.LayoutParams(
          LayoutParams.MATCH_PARENT,
          LayoutParams.WRAP_CONTENT
      );
      paramsRow.setMargins(0, 0, 0, 0);
      binding.name.setLayoutParams(paramsRow);
      binding.name.setTextSize(18f);
      binding.noteAsName.setLayoutParams(paramsRow);
      binding.noteAsName.setTextSize(18f);
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

    // AMOUNT

    Double amountInQuUnit = shoppingListItemAmountsHashMap.get(item.getId());
    if (product != null && amountInQuUnit != null) {
      QuantityUnit quantityUnit = quantityUnitHashMap.get(item.getQuIdInt());
      String quStr = pluralUtil.getQuantityUnitPlural(quantityUnit, amountInQuUnit);
      if (quStr != null) {
        binding.amount.setText(
            binding.amount.getContext()
                .getString(R.string.subtitle_amount, NumUtil.trim(amountInQuUnit), quStr)
        );
      } else {
        binding.amount.setText(NumUtil.trim(amountInQuUnit));
      }
    } else if (product != null) {
      QuantityUnit quantityUnit = quantityUnitHashMap.get(product.getQuIdStockInt());
      String quStr = pluralUtil.getQuantityUnitPlural(quantityUnit, item.getAmountDouble());
      if (quStr != null) {
        binding.amount.setText(
            binding.amount.getContext()
                .getString(R.string.subtitle_amount, NumUtil.trim(item.getAmountDouble()), quStr)
        );
      } else {
        binding.amount.setText(NumUtil.trim(item.getAmountDouble()));
      }
    } else {
      binding.amount.setText(NumUtil.trim(item.getAmountDouble()));
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

    binding.containerRow.setAlpha(item.getDoneInt() == 1 ? 0.4f : 1);

    // CONTAINER

    binding.card.setOnClickListener(
        view -> listener.onItemRowClicked(groupedListItem)
    );
  }

  public void updateData(
      ArrayList<GroupedListItem> newList,
      HashMap<Integer, Product> productHashMap,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      HashMap<Integer, Double> shoppingListItemAmountsHashMap,
      ArrayList<Integer> missingProductIds
  ) {
    ShoppingModeItemAdapter.DiffCallback diffCallback = new ShoppingModeItemAdapter.DiffCallback(
        newList,
        this.groupedListItems,
        this.productHashMap,
        productHashMap,
        this.quantityUnitHashMap,
        quantityUnitHashMap,
        this.shoppingListItemAmountsHashMap,
        shoppingListItemAmountsHashMap,
        this.missingProductIds,
        missingProductIds
    );
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
    this.groupedListItems.clear();
    this.groupedListItems.addAll(newList);
    this.productHashMap.clear();
    this.productHashMap.putAll(productHashMap);
    this.quantityUnitHashMap.clear();
    this.quantityUnitHashMap.putAll(quantityUnitHashMap);
    this.shoppingListItemAmountsHashMap.clear();
    this.shoppingListItemAmountsHashMap.putAll(shoppingListItemAmountsHashMap);
    this.missingProductIds.clear();
    this.missingProductIds.addAll(missingProductIds);
    diffResult.dispatchUpdatesTo(new AdapterListUpdateCallback(this, linearLayoutManager));
  }

  static class DiffCallback extends DiffUtil.Callback {

    final ArrayList<GroupedListItem> oldItems;
    final ArrayList<GroupedListItem> newItems;
    final HashMap<Integer, Product> productHashMapOld;
    final HashMap<Integer, Product> productHashMapNew;
    final HashMap<Integer, QuantityUnit> quantityUnitHashMapOld;
    final HashMap<Integer, QuantityUnit> quantityUnitHashMapNew;
    HashMap<Integer, Double> shoppingListItemAmountsHashMapOld;
    HashMap<Integer, Double> shoppingListItemAmountsHashMapNew;
    final ArrayList<Integer> missingProductIdsOld;
    final ArrayList<Integer> missingProductIdsNew;

    public DiffCallback(
        ArrayList<GroupedListItem> newItems,
        ArrayList<GroupedListItem> oldItems,
        HashMap<Integer, Product> productHashMapOld,
        HashMap<Integer, Product> productHashMapNew,
        HashMap<Integer, QuantityUnit> quantityUnitHashMapOld,
        HashMap<Integer, QuantityUnit> quantityUnitHashMapNew,
        HashMap<Integer, Double> shoppingListItemAmountsHashMapOld,
        HashMap<Integer, Double> shoppingListItemAmountsHashMapNew,
        ArrayList<Integer> missingProductIdsOld,
        ArrayList<Integer> missingProductIdsNew
    ) {
      this.newItems = newItems;
      this.oldItems = oldItems;
      this.productHashMapOld = productHashMapOld;
      this.productHashMapNew = productHashMapNew;
      this.quantityUnitHashMapOld = quantityUnitHashMapOld;
      this.quantityUnitHashMapNew = quantityUnitHashMapNew;
      this.shoppingListItemAmountsHashMapOld = shoppingListItemAmountsHashMapOld;
      this.shoppingListItemAmountsHashMapNew = shoppingListItemAmountsHashMapNew;
      this.missingProductIdsOld = missingProductIdsOld;
      this.missingProductIdsNew = missingProductIdsNew;
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
        ProductGroup newGroup = (ProductGroup) newItems.get(newItemPos);
        ProductGroup oldGroup = (ProductGroup) oldItems.get(oldItemPos);
        return newGroup.equals(oldGroup);
      } else { // Type: Bottom notes
        ShoppingListBottomNotes newNotes = (ShoppingListBottomNotes) newItems.get(newItemPos);
        ShoppingListBottomNotes oldNotes = (ShoppingListBottomNotes) oldItems.get(oldItemPos);
        return newNotes.equals(oldNotes);
      }
    }
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
