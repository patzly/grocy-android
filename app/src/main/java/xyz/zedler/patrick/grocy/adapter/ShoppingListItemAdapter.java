package xyz.zedler.patrick.grocy.adapter;

/*
    This file is part of Grocy Android.

    Grocy Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Grocy Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Grocy Android.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2020-2021 by Patrick Zedler & Dominic Zedler
*/

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowFilterChipsBinding;
import xyz.zedler.patrick.grocy.databinding.RowShoppingListBottomNotesBinding;
import xyz.zedler.patrick.grocy.databinding.RowShoppingListGroupBinding;
import xyz.zedler.patrick.grocy.databinding.RowShoppingListItemBinding;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.HorizontalFilterBarSingle;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListBottomNotes;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.view.FilterChip;

public class ShoppingListItemAdapter extends RecyclerView.Adapter<ShoppingListItemAdapter.ViewHolder> {

    private final static String TAG = ShoppingListItemAdapter.class.getSimpleName();
    private final static boolean DEBUG = false;

    public final static int FILTER_NOTHING = -1;
    public final static int FILTER_MISSING = 0;
    public final static int FILTER_UNDONE = 1;

    private Context context;
    private final ArrayList<GroupedListItem> groupedListItems;
    private final HashMap<Integer, Product> productHashMap;
    private final HashMap<Integer, QuantityUnit> quantityUnitHashMap;
    private final ArrayList<Integer> missingProductIds;
    private final ShoppingListItemAdapterListener listener;
    private final HorizontalFilterBarSingle horizontalFilterBarSingle;


    public ShoppingListItemAdapter(
            Context context,
            ArrayList<GroupedListItem> groupedListItems,
            HashMap<Integer, Product> productHashMap,
            HashMap<Integer, QuantityUnit> quantityUnitHashMap,
            ArrayList<Integer> missingProductIds,
            ShoppingListItemAdapterListener listener,
            HorizontalFilterBarSingle horizontalFilterBarSingle
    ) {
        this.context = context;
        this.groupedListItems = new ArrayList<>(groupedListItems);
        this.productHashMap = new HashMap<>(productHashMap);
        this.quantityUnitHashMap = new HashMap<>(quantityUnitHashMap);
        this.missingProductIds = new ArrayList<>(missingProductIds);
        this.listener = listener;
        this.horizontalFilterBarSingle = horizontalFilterBarSingle;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.context = null;
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

    public static class FilterRowViewHolder extends ViewHolder {
        private final WeakReference<Context> weakContext;
        private final FilterChip chipMissing;
        private FilterChip chipUndone;
        private final HorizontalFilterBarSingle horizontalFilterBarSingle;

        public FilterRowViewHolder(
                RowFilterChipsBinding binding,
                Context context,
                HorizontalFilterBarSingle horizontalFilterBarSingle
        ) {
            super(binding.getRoot());

            this.horizontalFilterBarSingle = horizontalFilterBarSingle;
            weakContext = new WeakReference<>(context);
            chipMissing = new FilterChip(
                    context,
                    R.color.retro_blue_bg,
                    context.getString(R.string.msg_missing_products, 0),
                    () -> {
                        if(chipUndone.isActive()) chipUndone.changeState(false);
                        horizontalFilterBarSingle.setSingleFilterActive(HorizontalFilterBarSingle.MISSING);
                    },
                    horizontalFilterBarSingle::resetAllFilters
            );
            chipUndone = new FilterChip(
                    context,
                    R.color.retro_yellow_bg,
                    context.getString(R.string.msg_undone_items, 0),
                    () -> {
                        if(chipMissing.isActive()) chipMissing.changeState(false);
                        horizontalFilterBarSingle.setSingleFilterActive(HorizontalFilterBarSingle.UNDONE);
                    },
                    horizontalFilterBarSingle::resetAllFilters
            );
            chipMissing.setId(R.id.chip_shopping_filter_missing);
            chipUndone.setId(R.id.chip_shopping_filter_undone);
            binding.container.addView(chipMissing);
            binding.container.addView(chipUndone);
        }

        public void bind() {
            if(horizontalFilterBarSingle.isNoFilterActive()) {
                if(chipMissing.isActive()) chipMissing.changeState(false);
                if(chipUndone.isActive()) chipUndone.changeState(false);
            } else if(horizontalFilterBarSingle.isFilterActive(HorizontalFilterBarSingle.MISSING)) {
                if(!chipMissing.isActive()) chipMissing.changeState(true);
                if(chipUndone.isActive()) chipUndone.changeState(false);
            } else if(horizontalFilterBarSingle.isFilterActive(HorizontalFilterBarSingle.UNDONE)) {
                if(chipMissing.isActive()) chipMissing.changeState(false);
                if(!chipUndone.isActive()) chipUndone.changeState(true);
            }
            chipMissing.setText(weakContext.get().getString(
                    R.string.msg_missing_products,
                    horizontalFilterBarSingle.getItemsCount(HorizontalFilterBarSingle.MISSING)
            ));
            chipUndone.setText(weakContext.get().getString(
                    R.string.msg_undone_items,
                    horizontalFilterBarSingle.getItemsCount(HorizontalFilterBarSingle.UNDONE)
            ));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(position == 0) return -1; // filter row
        return groupedListItems.get(position - 1).getType();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == -1) { // filter row
            RowFilterChipsBinding binding = RowFilterChipsBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new FilterRowViewHolder(
                    binding,
                    context,
                    horizontalFilterBarSingle
            );
        } else if(viewType == GroupedListItem.TYPE_HEADER) {
            return new ShoppingListGroupViewHolder(
                    RowShoppingListGroupBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        } else if(viewType == GroupedListItem.TYPE_ENTRY) {
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

        int position = viewHolder.getAdapterPosition();
        int movedPosition = position - 1;

        if(position == 0) { // Filter row
            ((FilterRowViewHolder) viewHolder).bind();
            return;
        }

        GroupedListItem groupedListItem = groupedListItems.get(movedPosition);

        int type = getItemViewType(position);
        if(type == GroupedListItem.TYPE_HEADER) {
            ShoppingListGroupViewHolder holder = (ShoppingListGroupViewHolder) viewHolder;
            if(((ProductGroup) groupedListItem).getDisplayDivider() == 1) {
                holder.binding.divider.setVisibility(View.VISIBLE);
            } else {
                holder.binding.divider.setVisibility(View.GONE);
            }
            holder.binding.name.setText(((ProductGroup) groupedListItem).getName());
            return;
        }
        if(type == GroupedListItem.TYPE_BOTTOM_NOTES) {
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

        // NAME

        Product product = null;
        if(item.hasProduct()) product = productHashMap.get(item.getProductIdInt());

        if(product != null) {
            binding.name.setText(product.getName());
            binding.name.setVisibility(View.VISIBLE);
        } else {
            binding.name.setText(null);
            binding.name.setVisibility(View.GONE);
        }
        if(item.isUndone()) {
            binding.name.setPaintFlags(
                    binding.name.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
            );
        } else {
            binding.name.setPaintFlags(
                    binding.name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
        }

        // NOTE AS NAME

        if(binding.name.getVisibility() == View.VISIBLE) {
            binding.noteAsName.setVisibility(View.GONE);
            binding.noteAsName.setText(null);
        }

        // AMOUNT

        if(product != null) {
            QuantityUnit quantityUnit = quantityUnitHashMap.get(product.getQuIdStock());
            if(quantityUnit == null) quantityUnit = new QuantityUnit();

            binding.amount.setText(
                    context.getString(
                            R.string.subtitle_amount,
                            NumUtil.trim(item.getAmountDouble()),
                            item.getAmountDouble() == 1
                                    ? quantityUnit.getName()
                                    : quantityUnit.getNamePlural()
                    )
            );
        } else {
            binding.amount.setText(NumUtil.trim(item.getAmountDouble()));
        }

        if(item.hasProduct() && missingProductIds.contains(item.getProductIdInt())) {
            binding.amount.setTypeface(ResourcesCompat.getFont(context, R.font.jost_medium));
            binding.amount.setTextColor(ContextCompat.getColor(context, R.color.retro_blue_fg));
        } else {
            binding.amount.setTypeface(
                    ResourcesCompat.getFont(context, R.font.jost_book)
            );
            binding.amount.setTextColor(
                    ContextCompat.getColor(context, R.color.on_background_secondary)
            );
        }
        if(item.isUndone()) {
            binding.amount.setPaintFlags(
                    binding.amount.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
            );
        } else {
            binding.amount.setPaintFlags(
                    binding.amount.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
        }

        // NOTE

        if(item.getNote() != null && !item.getNote().trim().isEmpty()) {
            if(binding.name.getVisibility() == View.VISIBLE) {
                binding.noteContainer.setVisibility(View.VISIBLE);
                binding.note.setText(item.getNote().trim());
            } else {
                binding.noteAsName.setVisibility(View.VISIBLE);
                binding.noteAsName.setText(item.getNote().trim());
            }
        } else {
            if(binding.name.getVisibility() == View.VISIBLE) {
                binding.noteContainer.setVisibility(View.GONE);
                binding.note.setText(null);
            }
        }
        if(binding.noteAsName.getVisibility() == View.VISIBLE) {
            if(item.isUndone()) {
                binding.noteAsName.setPaintFlags(
                        binding.noteAsName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
                );
            } else {
                binding.noteAsName.setPaintFlags(
                        binding.noteAsName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                );
            }
        } else {
            if(item.isUndone()) {
                binding.note.setPaintFlags(
                        binding.note.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
                );
            } else {
                binding.note.setPaintFlags(
                        binding.note.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                );
            }
        }

        // CONTAINER

        binding.container.setOnClickListener(
                view -> listener.onItemRowClicked(groupedListItem)
        );

    }

    @Override
    public int getItemCount() {
        return groupedListItems.size() + 1;
    }

    public interface ShoppingListItemAdapterListener {
        void onItemRowClicked(GroupedListItem groupedListItem);
    }

    // Only for PurchaseFragment
    public static void fillShoppingListItem(
            Context context,
            ShoppingListItem item,
            RowShoppingListItemBinding binding,
            ArrayList<QuantityUnit> quantityUnits
    ) {

        // NAME

        Product product = null;
        //if(listItem.hasProduct()) product = productHashMap.get(item.getProductIdInt()); TODO

        if(product != null) {
            binding.name.setText(product.getName());
            binding.name.setVisibility(View.VISIBLE);
        } else {
            binding.name.setText(null);
            binding.name.setVisibility(View.GONE);
        }
        if(item.isUndone()) {
            binding.name.setPaintFlags(
                    binding.name.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
            );
        } else {
            binding.name.setPaintFlags(
                    binding.name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
        }

        // NOTE AS NAME

        if(binding.name.getVisibility() == View.VISIBLE) {
            binding.noteAsName.setVisibility(View.GONE);
            binding.noteAsName.setText(null);
        }

        // AMOUNT

        if(product != null) {
            QuantityUnit quantityUnit = null;
            // quantityUnit = quantityUnitHashMap.get(product.getQuIdStock()); // TODO
            if(quantityUnit == null) quantityUnit = new QuantityUnit();

            if(DEBUG) Log.i(TAG, "onBindViewHolder: " + quantityUnit.getName());

            binding.amount.setText(
                    context.getString(
                            R.string.subtitle_amount,
                            NumUtil.trim(item.getAmountDouble()),
                            item.getAmountDouble() == 1
                                    ? quantityUnit.getName()
                                    : quantityUnit.getNamePlural()
                    )
            );
        } else {
            binding.amount.setText(NumUtil.trim(item.getAmountDouble()));
        }

        //if(item.hasProduct() && missingProductIds.contains(item.getProductIdInt())) {  TODO
        if(item.hasProduct()) {
            binding.amount.setTypeface(
                    ResourcesCompat.getFont(context, R.font.jost_medium)
            );
            binding.amount.setTextColor(
                    ContextCompat.getColor(context, R.color.retro_blue_fg)
            );
        } else {
            binding.amount.setTypeface(
                    ResourcesCompat.getFont(context, R.font.jost_book)
            );
            binding.amount.setTextColor(
                    ContextCompat.getColor(context, R.color.on_background_secondary)
            );
        }
        if(item.isUndone()) {
            binding.amount.setPaintFlags(
                    binding.amount.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
            );
        } else {
            binding.amount.setPaintFlags(
                    binding.amount.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
        }

        // NOTE

        if(item.getNote() != null && !item.getNote().isEmpty()) {
            if(binding.name.getVisibility() == View.VISIBLE) {
                binding.noteContainer.setVisibility(View.VISIBLE);
                binding.note.setText(item.getNote().trim());
            } else {
                binding.noteAsName.setVisibility(View.VISIBLE);
                binding.noteAsName.setText(item.getNote().trim());
            }
        } else {
            if(binding.name.getVisibility() == View.VISIBLE) {
                binding.noteContainer.setVisibility(View.GONE);
                binding.note.setText(null);
            }
        }
        if(binding.noteAsName.getVisibility() == View.VISIBLE) {
            if(item.isUndone()) {
                binding.noteAsName.setPaintFlags(
                        binding.noteAsName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
                );
            } else {
                binding.noteAsName.setPaintFlags(
                        binding.noteAsName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                );
            }
        } else {
            if(item.isUndone()) {
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
            ArrayList<GroupedListItem> newList,
            HashMap<Integer, Product> productHashMap,
            HashMap<Integer, QuantityUnit> quantityUnitHashMap,
            ArrayList<Integer> missingProductIds,
            int itemsMissingCount,
            int itemsUndoneCount
    ) {
        if(horizontalFilterBarSingle.getItemsCount(HorizontalFilterBarSingle.MISSING) != itemsMissingCount
                || horizontalFilterBarSingle.getItemsCount(HorizontalFilterBarSingle.UNDONE) != itemsUndoneCount) {
            horizontalFilterBarSingle.setItemsCount(HorizontalFilterBarSingle.MISSING, itemsMissingCount);
            horizontalFilterBarSingle.setItemsCount(HorizontalFilterBarSingle.UNDONE, itemsUndoneCount);
            notifyItemChanged(0); // update viewHolder with filter row
        }

        ShoppingListItemAdapter.DiffCallback diffCallback = new ShoppingListItemAdapter.DiffCallback(
                newList,
                this.groupedListItems,
                this.productHashMap,
                productHashMap,
                this.quantityUnitHashMap,
                quantityUnitHashMap,
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
        this.missingProductIds.clear();
        this.missingProductIds.addAll(missingProductIds);
        diffResult.dispatchUpdatesTo(new AdapterListUpdateCallback(this));
    }

    static class DiffCallback extends DiffUtil.Callback {
        ArrayList<GroupedListItem> oldItems;
        ArrayList<GroupedListItem> newItems;
        HashMap<Integer, Product> productHashMapOld;
        HashMap<Integer, Product> productHashMapNew;
        HashMap<Integer, QuantityUnit> quantityUnitHashMapOld;
        HashMap<Integer, QuantityUnit> quantityUnitHashMapNew;
        ArrayList<Integer> missingProductIdsOld;
        ArrayList<Integer> missingProductIdsNew;

        public DiffCallback(
                ArrayList<GroupedListItem> newItems,
                ArrayList<GroupedListItem> oldItems,
                HashMap<Integer, Product> productHashMapOld,
                HashMap<Integer, Product> productHashMapNew,
                HashMap<Integer, QuantityUnit> quantityUnitHashMapOld,
                HashMap<Integer, QuantityUnit> quantityUnitHashMapNew,
                ArrayList<Integer> missingProductIdsOld,
                ArrayList<Integer> missingProductIdsNew
        ) {
            this.newItems = newItems;
            this.oldItems = oldItems;
            this.productHashMapOld = productHashMapOld;
            this.productHashMapNew = productHashMapNew;
            this.quantityUnitHashMapOld = quantityUnitHashMapOld;
            this.quantityUnitHashMapNew = quantityUnitHashMapNew;
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
            int oldItemType = oldItems.get(oldItemPos).getType();
            int newItemType = newItems.get(newItemPos).getType();
            if(oldItemType != newItemType) return false;
            if(oldItemType == GroupedListItem.TYPE_ENTRY) {
                ShoppingListItem newItem = (ShoppingListItem) newItems.get(newItemPos);
                ShoppingListItem oldItem = (ShoppingListItem) oldItems.get(oldItemPos);
                if(!compareContent) return newItem.getId() == oldItem.getId();

                Integer productIdOld = NumUtil.isStringInt(oldItem.getProductId()) ? Integer.parseInt(oldItem.getProductId()) : null;
                Product productOld = productIdOld != null ? productHashMapOld.get(productIdOld) : null;

                Integer productIdNew = NumUtil.isStringInt(newItem.getProductId()) ? Integer.parseInt(newItem.getProductId()) : null;
                Product productNew = productIdNew != null ? productHashMapNew.get(productIdNew) : null;

                Integer quIdOld = NumUtil.isStringInt(oldItem.getQuId()) ? Integer.parseInt(oldItem.getQuId()) : null;
                QuantityUnit quOld = quIdOld != null ? quantityUnitHashMapOld.get(quIdOld) : null;

                Integer quIdNew = NumUtil.isStringInt(newItem.getQuId()) ? Integer.parseInt(newItem.getQuId()) : null;
                QuantityUnit quNew = quIdNew != null ? quantityUnitHashMapNew.get(quIdNew) : null;

                Boolean missingOld = productIdOld != null ? missingProductIdsOld.contains(productIdOld) : null;
                Boolean missingNew = productIdNew != null ? missingProductIdsNew.contains(productIdNew) : null;

                if(productOld == null && productNew != null
                        || productOld != null && productNew != null && productOld.getId() != productNew.getId()
                        || quOld == null && quNew != null
                        || quOld != null && quNew != null && quOld.getId() != quNew.getId()
                        || missingOld == null && missingNew != null
                        || missingOld != null && missingNew != null && missingOld != missingNew
                ) return false;

                return newItem.equals(oldItem);
            } else if(oldItemType == GroupedListItem.TYPE_HEADER) {
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

    /**
     * Custom ListUpdateCallback that dispatches update events to the given adapter
     * with offset of 1, because the first item is the filter row.
     */
    public static final class AdapterListUpdateCallback implements ListUpdateCallback {
        @NonNull
        private final ShoppingListItemAdapter mAdapter;

        public AdapterListUpdateCallback(@NonNull ShoppingListItemAdapter adapter) {
            mAdapter = adapter;
        }
        @Override
        public void onInserted(int position, int count) {
            mAdapter.notifyItemRangeInserted(position + 1, count);
        }
        @Override
        public void onRemoved(int position, int count) {
            mAdapter.notifyItemRangeRemoved(position + 1, count);
        }
        @Override
        public void onMoved(int fromPosition, int toPosition) {
            mAdapter.notifyItemMoved(fromPosition + 1, toPosition + 1);
        }
        @Override
        public void onChanged(int position, int count, Object payload) {
            mAdapter.notifyItemRangeChanged(position + 1, count, payload);
        }
    }
}
