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

    Copyright 2020 by Patrick Zedler & Dominic Zedler
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

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowFilterChipsBinding;
import xyz.zedler.patrick.grocy.databinding.RowShoppingListBottomNotesBinding;
import xyz.zedler.patrick.grocy.databinding.RowShoppingListGroupBinding;
import xyz.zedler.patrick.grocy.databinding.RowShoppingListItemBinding;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
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
    private ArrayList<QuantityUnit> quantityUnits;
    private final ShoppingListItemAdapterListener listener;

    private int filterState;
    private int itemsMissingCount;
    private int itemsUndoneCount;
    private final OnFilterChangedListener filterListenerOutput;


    public ShoppingListItemAdapter(
            Context context,
            ArrayList<GroupedListItem> groupedListItems,
            ArrayList<QuantityUnit> quantityUnits,
            ShoppingListItemAdapterListener listener,
            int filterState,
            OnFilterChangedListener filterListenerOutput,
            int itemsMissingCount,
            int itemsUndoneCount
    ) {
        this.context = context;
        this.groupedListItems = new ArrayList<>(groupedListItems);
        this.quantityUnits = quantityUnits;
        this.listener = listener;

        this.filterState = filterState;
        this.filterListenerOutput = filterListenerOutput;
        this.itemsMissingCount = itemsMissingCount;
        this.itemsUndoneCount = itemsUndoneCount;
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
        private FilterChip chipMissing;
        private FilterChip chipUndone;

        public FilterRowViewHolder(
                RowFilterChipsBinding binding,
                Context context,
                int filterInitial,
                OnFilterChangedListener filterListenerOutput
        ) {
            super(binding.getRoot());

            weakContext = new WeakReference<>(context);
            chipMissing = new FilterChip(
                    context,
                    R.color.retro_blue_bg,
                    context.getString(R.string.msg_missing_products, 0),
                    () -> {
                        if(chipUndone.isActive()) chipUndone.changeState(false);
                        filterListenerOutput.onChanged(FILTER_MISSING);
                    },
                    () -> filterListenerOutput.onChanged(FILTER_NOTHING)
            );
            chipMissing.setId(R.id.chip_shopping_filter_missing);
            chipUndone = new FilterChip(
                    context,
                    R.color.retro_yellow_bg,
                    context.getString(R.string.msg_undone_items, 0),
                    () -> {
                        if(chipMissing.isActive()) chipMissing.changeState(false);
                        filterListenerOutput.onChanged(FILTER_UNDONE);
                    },
                    () -> filterListenerOutput.onChanged(FILTER_NOTHING)
            );
            chipUndone.setId(R.id.chip_shopping_filter_undone);
            binding.container.addView(chipMissing);
            binding.container.addView(chipUndone);
            if(filterInitial == FILTER_MISSING) {
                chipMissing.setActive(true);
            } else if(filterInitial == FILTER_UNDONE) {
                chipUndone.setActive(true);
            }
        }

        public void bind(int state, int itemsMissingCount, int itemsUndoneCount) {
            if(state == FILTER_NOTHING) {
                if(chipMissing.isActive()) chipMissing.changeState(false);
                if(chipUndone.isActive()) chipUndone.changeState(false);
            } else if(state == FILTER_MISSING) {
                if(!chipMissing.isActive()) chipMissing.changeState(true);
                if(chipUndone.isActive()) chipUndone.changeState(false);
            } else if(state == FILTER_UNDONE) {
                if(chipMissing.isActive()) chipMissing.changeState(false);
                if(!chipUndone.isActive()) chipUndone.changeState(true);
            }
            chipMissing.setText(weakContext.get()
                    .getString(R.string.msg_missing_products, itemsMissingCount));
            chipUndone.setText(weakContext.get()
                    .getString(R.string.msg_undone_items, itemsUndoneCount));
        }
    }

    public void setFilterState(int state) {
        filterState = state;
        notifyItemChanged(0);
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
                    filterState,
                    state -> {
                        filterState = state;
                        filterListenerOutput.onChanged(state);
                    }
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
            ((FilterRowViewHolder) viewHolder).bind(
                    filterState,
                    itemsMissingCount,
                    itemsUndoneCount
            );
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

        ShoppingListItem shoppingListItem = (ShoppingListItem) groupedListItem;
        RowShoppingListItemBinding binding = ((ShoppingListItemViewHolder) viewHolder).binding;

        // NAME

        Product product = shoppingListItem.getProduct();
        if(product != null) {
            binding.name.setText(product.getName());
            binding.name.setVisibility(View.VISIBLE);
        } else {
            binding.name.setText(null);
            binding.name.setVisibility(View.GONE);
        }
        if(shoppingListItem.isUndone()) {
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

        if(shoppingListItem.getProduct() != null) {
            QuantityUnit quantityUnit = new QuantityUnit();
            for(int i = 0; i < quantityUnits.size(); i++) {
                if(quantityUnits.get(i).getId() == shoppingListItem.getProduct().getQuIdPurchase()) {
                    quantityUnit = quantityUnits.get(i);
                    break;
                }
            }

            binding.amount.setText(
                    context.getString(
                            R.string.subtitle_amount,
                            NumUtil.trim(shoppingListItem.getAmount()),
                            shoppingListItem.getAmount() == 1
                                    ? quantityUnit.getName()
                                    : quantityUnit.getNamePlural()
                    )
            );
        } else {
            binding.amount.setText(NumUtil.trim(shoppingListItem.getAmount()));
        }

        if(shoppingListItem.isMissing()) {
            binding.amount.setTypeface(ResourcesCompat.getFont(context, R.font.roboto_mono_medium));
            binding.amount.setTextColor(ContextCompat.getColor(context, R.color.retro_blue_fg));
        } else {
            binding.amount.setTypeface(
                    ResourcesCompat.getFont(context, R.font.roboto_mono_regular)
            );
            binding.amount.setTextColor(
                    ContextCompat.getColor(context, R.color.on_background_secondary)
            );
        }
        if(shoppingListItem.isUndone()) {
            binding.amount.setPaintFlags(
                    binding.amount.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
            );
        } else {
            binding.amount.setPaintFlags(
                    binding.amount.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
        }

        // NOTE

        if(shoppingListItem.getNote() != null && !shoppingListItem.getNote().isEmpty()) {
            if(binding.name.getVisibility() == View.VISIBLE) {
                binding.noteContainer.setVisibility(View.VISIBLE);
                binding.note.setText(shoppingListItem.getNote().trim());
            } else {
                binding.noteAsName.setVisibility(View.VISIBLE);
                binding.noteAsName.setText(shoppingListItem.getNote().trim());
            }
        } else {
            if(binding.name.getVisibility() == View.VISIBLE) {
                binding.noteContainer.setVisibility(View.GONE);
                binding.note.setText(null);
            }
        }
        if(binding.noteAsName.getVisibility() == View.VISIBLE) {
            if(shoppingListItem.isUndone()) {
                binding.noteAsName.setPaintFlags(
                        binding.noteAsName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
                );
            } else {
                binding.noteAsName.setPaintFlags(
                        binding.noteAsName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                );
            }
        } else {
            if(shoppingListItem.isUndone()) {
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
            ShoppingListItem listItem,
            RowShoppingListItemBinding binding,
            ArrayList<QuantityUnit> quantityUnits
    ) {

        // NAME

        Product product = listItem.getProduct();
        if(product != null) {
            binding.name.setText(product.getName());
            binding.name.setVisibility(View.VISIBLE);
        } else {
            binding.name.setText(null);
            binding.name.setVisibility(View.GONE);
        }
        if(listItem.isUndone()) {
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

        if(listItem.getProduct() != null) {
            QuantityUnit quantityUnit = new QuantityUnit();
            for(int i = 0; i < quantityUnits.size(); i++) {
                if(quantityUnits.get(i).getId() == listItem.getProduct().getQuIdPurchase()) {
                    quantityUnit = quantityUnits.get(i);
                    break;
                }
            }

            if(DEBUG) Log.i(TAG, "onBindViewHolder: " + quantityUnit.getName());

            binding.amount.setText(
                    context.getString(
                            R.string.subtitle_amount,
                            NumUtil.trim(listItem.getAmount()),
                            listItem.getAmount() == 1
                                    ? quantityUnit.getName()
                                    : quantityUnit.getNamePlural()
                    )
            );
        } else {
            binding.amount.setText(NumUtil.trim(listItem.getAmount()));
        }

        if(listItem.isMissing()) {
            binding.amount.setTypeface(
                    ResourcesCompat.getFont(context, R.font.roboto_mono_medium)
            );
            binding.amount.setTextColor(
                    ContextCompat.getColor(context, R.color.retro_blue_fg)
            );
        } else {
            binding.amount.setTypeface(
                    ResourcesCompat.getFont(context, R.font.roboto_mono_regular)
            );
            binding.amount.setTextColor(
                    ContextCompat.getColor(context, R.color.on_background_secondary)
            );
        }
        if(listItem.isUndone()) {
            binding.amount.setPaintFlags(
                    binding.amount.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
            );
        } else {
            binding.amount.setPaintFlags(
                    binding.amount.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
        }

        // NOTE

        if(listItem.getNote() != null && !listItem.getNote().isEmpty()) {
            if(binding.name.getVisibility() == View.VISIBLE) {
                binding.noteContainer.setVisibility(View.VISIBLE);
                binding.note.setText(listItem.getNote().trim());
            } else {
                binding.noteAsName.setVisibility(View.VISIBLE);
                binding.noteAsName.setText(listItem.getNote().trim());
            }
        } else {
            if(binding.name.getVisibility() == View.VISIBLE) {
                binding.noteContainer.setVisibility(View.GONE);
                binding.note.setText(null);
            }
        }
        if(binding.noteAsName.getVisibility() == View.VISIBLE) {
            if(listItem.isUndone()) {
                binding.noteAsName.setPaintFlags(
                        binding.noteAsName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
                );
            } else {
                binding.noteAsName.setPaintFlags(
                        binding.noteAsName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                );
            }
        } else {
            if(listItem.isUndone()) {
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
            ArrayList<QuantityUnit> quantityUnits,
            int itemsMissingCount,
            int itemsUndoneCount
    ) {
        this.quantityUnits = quantityUnits;
        if(this.itemsMissingCount != itemsMissingCount
                || this.itemsUndoneCount != itemsUndoneCount) {
            this.itemsMissingCount = itemsMissingCount;
            this.itemsUndoneCount = itemsUndoneCount;
            notifyItemChanged(0); // update viewHolder with filter row
        }

        ShoppingListItemAdapter.DiffCallback diffCallback = new ShoppingListItemAdapter.DiffCallback(
                newList,
                this.groupedListItems
        );
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        this.groupedListItems.clear();
        this.groupedListItems.addAll(newList);
        diffResult.dispatchUpdatesTo(new AdapterListUpdateCallback(this));
    }

    static class DiffCallback extends DiffUtil.Callback {
        ArrayList<GroupedListItem> oldItems;
        ArrayList<GroupedListItem> newItems;

        public DiffCallback(
                ArrayList<GroupedListItem> newItems,
                ArrayList<GroupedListItem> oldItems
        ) {
            this.newItems = newItems;
            this.oldItems = oldItems;
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
                return compareContent
                        ? newItem.equals(oldItem)
                        : newItem.getId() == oldItem.getId();
            } else if(oldItemType == GroupedListItem.TYPE_HEADER) {
                ProductGroup newItem = (ProductGroup) newItems.get(newItemPos);
                ProductGroup oldItem = (ProductGroup) oldItems.get(oldItemPos);
                return newItem.equals(oldItem);
            } else {
                return true; // Bottom notes is always one item at the bottom
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

    public interface OnFilterChangedListener {
        void onChanged(int state);
    }
}
