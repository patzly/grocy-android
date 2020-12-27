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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.ShoppingList;
import xyz.zedler.patrick.grocy.view.ActionButton;

public class ShoppingListAdapter extends RecyclerView.Adapter<ShoppingListAdapter.ViewHolder> {

    private final static String TAG = ShoppingListAdapter.class.getSimpleName();

    private final List<ShoppingList> shoppingLists;
    private int selectedId;
    private final ShoppingListAdapterListener listener;

    public ShoppingListAdapter(
            List<ShoppingList> shoppingLists,
            int selectedId,
            ShoppingListAdapterListener listener
    ) {
        this.shoppingLists = new ArrayList<>(shoppingLists);
        this.selectedId = selectedId;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout container;
        private final TextView name;
        private final ImageView imageSelected;
        private final ActionButton edit;
        private final ActionButton delete;

        public ViewHolder(View view) {
            super(view);

            container = view.findViewById(R.id.container);
            name = view.findViewById(R.id.name);
            imageSelected = view.findViewById(R.id.image_selected);
            edit = view.findViewById(R.id.edit);
            delete = view.findViewById(R.id.delete);
        }
    }

    @NonNull
    @Override
    public ShoppingListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ShoppingListAdapter.ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.row_shopping_list_selection_sheet,
                        parent,
                        false
                )
        );
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(
            @NonNull final ShoppingListAdapter.ViewHolder holder,
            int position
    ) {
        ShoppingList shoppingList = shoppingLists.get(holder.getAdapterPosition());

        holder.name.setText(shoppingList.getName());

        if(shoppingList.getId() == selectedId) {
            holder.imageSelected.setVisibility(View.VISIBLE);
        }

        holder.container.setOnClickListener(view -> listener.onItemRowClicked(shoppingList));

        if(shoppingList.getId() == 1) holder.delete.setVisibility(View.GONE);

        holder.delete.setOnTouchListener((v, event) -> {
            listener.onTouchDelete(v, event, shoppingList);
            return true;
        });

        holder.edit.setOnClickListener(v -> listener.onClickEdit(shoppingList));
    }

    public void updateData(List<ShoppingList> shoppingListsNew, int selectedIdNew) {
        ShoppingListAdapter.DiffCallback diffCallback = new ShoppingListAdapter.DiffCallback(
                shoppingListsNew,
                this.shoppingLists,
                selectedIdNew,
                this.selectedId
        );
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        shoppingLists.clear();
        shoppingLists.addAll(shoppingListsNew);
        this.selectedId = selectedIdNew;
        diffResult.dispatchUpdatesTo(this);
    }

    public void updateSelectedId(int selectedIdNew) {
        ShoppingListAdapter.DiffCallback diffCallback = new ShoppingListAdapter.DiffCallback(
                selectedIdNew,
                this.selectedId,
                this.shoppingLists
        );
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        this.selectedId = selectedIdNew;
        diffResult.dispatchUpdatesTo(this);
    }

    static class DiffCallback extends DiffUtil.Callback {
        List<ShoppingList> oldItems;
        List<ShoppingList> newItems;
        int selectedIdOld;
        int selectedIdNew;

        public DiffCallback(
                List<ShoppingList> newItems,
                List<ShoppingList> oldItems,
                int selectedIdNew,
                int selectedIdOld
        ) {
            this.newItems = newItems;
            this.oldItems = oldItems;
            this.selectedIdNew = selectedIdNew;
            this.selectedIdOld = selectedIdOld;
        }

        public DiffCallback(
                int selectedIdNew,
                int selectedIdOld,
                List<ShoppingList> oldItems
        ) {
            this.selectedIdNew = selectedIdNew;
            this.selectedIdOld = selectedIdOld;
            this.oldItems = oldItems;
        }

        @Override
        public int getOldListSize() {
            return oldItems.size();
        }

        @Override
        public int getNewListSize() {
            if(newItems == null) return oldItems.size(); // for second constructor used for selectedId update
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
            if(newItems != null) {
                ShoppingList newItem = newItems.get(newItemPos);
                ShoppingList oldItem = oldItems.get(oldItemPos);
                return compareContent
                        ? newItem.equals(oldItem) && selectedIdOld == selectedIdNew
                        : newItem.getId() == oldItem.getId();
            } else {
                return !compareContent || selectedIdOld == selectedIdNew;
            }
        }
    }

    @Override
    public long getItemId(int position) {
        return shoppingLists.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return shoppingLists.size();
    }

    public interface ShoppingListAdapterListener {
        void onItemRowClicked(ShoppingList shoppingList);
        void onTouchDelete(View view, MotionEvent event, ShoppingList shoppingList);
        void onClickEdit(ShoppingList shoppingList);
    }
}
