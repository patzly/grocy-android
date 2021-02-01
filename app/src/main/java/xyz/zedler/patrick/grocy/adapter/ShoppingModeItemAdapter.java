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
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListBottomNotes;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class ShoppingModeItemAdapter extends RecyclerView.Adapter<ShoppingModeItemAdapter.ViewHolder> {

    private final ArrayList<GroupedListItem> groupedListItems;
    private HashMap<Integer, Product> productHashMap;
    private HashMap<Integer, QuantityUnit> quantityUnitHashMap;
    private final ShoppingModeItemClickListener listener;

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
                return compareContent ? newItem.equals(oldItem) : newItem.getId() == oldItem.getId();
            } else {
                return true; // Bottom notes is always one item at the bottom
            }
        }
    }

    public void updateData(
            ArrayList<GroupedListItem> newList,
            HashMap<Integer, Product> productHashMap,
            HashMap<Integer, QuantityUnit> quantityUnitHashMap,
            ArrayList<Integer> missingProductIds
    ) {
        this.productHashMap = productHashMap;
        this.quantityUnitHashMap = quantityUnitHashMap;
        DiffCallback diffCallback = new DiffCallback(newList, this.groupedListItems);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        groupedListItems.clear();
        groupedListItems.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    public ShoppingModeItemAdapter(
            ArrayList<GroupedListItem> groupedListItems,
            HashMap<Integer, Product> productHashMap,
            HashMap<Integer, QuantityUnit> quantityUnitHashMap,
            ArrayList<Integer> missingProductIds,
            ShoppingModeItemClickListener listener
    ) {
        this.groupedListItems = new ArrayList<>(groupedListItems);
        this.productHashMap = productHashMap;
        this.quantityUnitHashMap = quantityUnitHashMap;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardViewContainer;
        private final TextView textViewName;
        private final TextView textViewAmount;
        private final TextView textViewGroupName;
        private final TextView textViewNote;
        private final TextView textViewNoteName;
        private final TextView textViewBottomNotes;
        private final View viewGroupSeparator;

        public ViewHolder(View view) {
            super(view);
            cardViewContainer = view.findViewById(R.id.card_shopping_item_container);
            textViewName = view.findViewById(R.id.text_shopping_item_name);
            textViewAmount = view.findViewById(R.id.text_shopping_item_amount);
            textViewNote = view.findViewById(R.id.text_shopping_item_note);
            textViewNoteName = view.findViewById(R.id.text_shopping_note_as_name);
            textViewBottomNotes = view.findViewById(R.id.text_shopping_bottom_notes);
            textViewGroupName = view.findViewById(R.id.text_shopping_group_name);
            viewGroupSeparator = view.findViewById(R.id.separator);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return groupedListItems.get(position).getType();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == GroupedListItem.TYPE_HEADER) {
            return new ShoppingModeItemAdapter.ViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.row_shopping_group,
                            parent,
                            false
                    )
            );
        } else if(viewType == GroupedListItem.TYPE_ENTRY) {
            return new ShoppingModeItemAdapter.ViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.row_shopping_item,
                            parent,
                            false
                    )
            );
        } else {
            return new ShoppingModeItemAdapter.ViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.row_shopping_bottom_notes,
                            parent,
                            false
                    )
            );
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int positionDoNotUse) {
        int position = holder.getAdapterPosition();

        GroupedListItem groupedListItem = groupedListItems.get(position);

        int type = getItemViewType(position);
        if (type == GroupedListItem.TYPE_HEADER) {
            String productGroupName = ((ProductGroup) groupedListItem).getName();
            holder.textViewGroupName.setText(productGroupName);
            if(!productGroupName.equals(holder.textViewGroupName.getContext()
                    .getString(R.string.subtitle_done))
            ) {
                holder.textViewGroupName.setTextColor(ContextCompat.getColor(
                        holder.textViewGroupName.getContext(), R.color.retro_green_fg
                ));
                holder.viewGroupSeparator.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(
                                holder.textViewGroupName.getContext(),
                                R.color.retro_green_fg
                        )
                ));
            } else {
                holder.textViewGroupName.setTextColor(ContextCompat.getColor(
                        holder.textViewGroupName.getContext(), R.color.retro_yellow_fg
                ));
                holder.viewGroupSeparator.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(
                                holder.textViewGroupName.getContext(),
                                R.color.retro_yellow_fg
                        )
                ));
            }
            return;
        }
        if(type == GroupedListItem.TYPE_BOTTOM_NOTES) {
            holder.textViewBottomNotes.setText(
                    ((ShoppingListBottomNotes) groupedListItem).getNotes()
            );
            holder.textViewBottomNotes.setOnClickListener(
                    view -> listener.onItemRowClicked(groupedListItem)
            );
            return;
        }

        ShoppingListItem item = (ShoppingListItem) groupedListItem;

        // NAME

        Product product = null;
        if(item.hasProduct()) product = productHashMap.get(item.getProductIdInt());

        if(product != null) {
            holder.textViewName.setText(product.getName());
            holder.textViewName.setVisibility(View.VISIBLE);
        } else {
            holder.textViewName.setText(null);
            holder.textViewName.setVisibility(View.GONE);
        }
        if(item.isUndone()) {
            holder.textViewName.setPaintFlags(
                    holder.textViewName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
            );
        } else {
            holder.textViewName.setPaintFlags(
                    holder.textViewName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
        }

        // NOTE AS NAME

        if(holder.textViewName.getVisibility() == View.VISIBLE) {
            holder.textViewNoteName.setVisibility(View.GONE);
            holder.textViewNoteName.setText(null);
        }

        // AMOUNT

        if(product != null) {
            QuantityUnit quantityUnit = quantityUnitHashMap.get(product.getQuIdStock());
            if(quantityUnit == null) quantityUnit = new QuantityUnit();

            holder.textViewAmount.setText(
                    holder.textViewAmount.getContext().getString(
                            R.string.subtitle_amount,
                            NumUtil.trim(item.getAmountDouble()),
                            item.getAmountDouble() == 1
                                    ? quantityUnit.getName()
                                    : quantityUnit.getNamePlural()
                    )
            );
        } else {
            holder.textViewAmount.setText(NumUtil.trim(item.getAmountDouble()));
        }

        if(item.isUndone()) {
            holder.textViewAmount.setPaintFlags(
                    holder.textViewAmount.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
            );
        } else {
            holder.textViewAmount.setPaintFlags(
                    holder.textViewAmount.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
        }

        // NOTE

        if(item.getNote() != null && !item.getNote().isEmpty()) {
            if(holder.textViewName.getVisibility() == View.VISIBLE) {
                holder.textViewNote.setVisibility(View.VISIBLE);
                holder.textViewNote.setText(item.getNote().trim());
            } else {
                holder.textViewNoteName.setVisibility(View.VISIBLE);
                holder.textViewNoteName.setText(item.getNote().trim());
                holder.textViewNote.setVisibility(View.GONE);
                holder.textViewNote.setText(null);
            }
        } else {
            if(holder.textViewName.getVisibility() == View.VISIBLE) {
                holder.textViewNote.setVisibility(View.GONE);
                holder.textViewNote.setText(null);
            }
        }
        if(holder.textViewNoteName.getVisibility() == View.VISIBLE) {
            if(item.isUndone()) {
                holder.textViewNoteName.setPaintFlags(
                        holder.textViewNoteName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
                );
            } else {
                holder.textViewNoteName.setPaintFlags(
                        holder.textViewNoteName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                );
            }
        } else {
            if(item.isUndone()) {
                holder.textViewNote.setPaintFlags(
                        holder.textViewNote.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
                );
            } else {
                holder.textViewNote.setPaintFlags(
                        holder.textViewNote.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                );
            }
        }

        if(item.getDone() == 1) {
            holder.cardViewContainer.setAlpha((float) 0.5);
        } else {
            holder.cardViewContainer.setAlpha((float) 1.0);
        }

        // CONTAINER

        holder.cardViewContainer.setOnClickListener(view -> listener.onItemRowClicked(groupedListItem));

    }

    @Override
    public int getItemCount() {
        return groupedListItems.size();
    }

    public interface ShoppingModeItemClickListener {
        void onItemRowClicked(GroupedListItem groupedListItem);
    }
}
