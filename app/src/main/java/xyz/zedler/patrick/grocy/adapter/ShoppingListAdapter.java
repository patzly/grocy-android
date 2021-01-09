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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
    private final boolean showActions;

    public ShoppingListAdapter(
            List<ShoppingList> shoppingLists,
            Object selectedId,
            ShoppingListAdapterListener listener,
            boolean showActions
    ) {
        this.shoppingLists = new ArrayList<>(shoppingLists);
        this.selectedId = selectedId != null ? (Integer) selectedId : -1;
        this.listener = listener;
        this.showActions = showActions;
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

        if(!showActions) {
            holder.delete.setVisibility(View.GONE);
            holder.edit.setVisibility(View.GONE);
        }
    }

    public void updateData(List<ShoppingList> shoppingListsNew, Object selectedIdNew) {
        shoppingLists.clear();
        shoppingLists.addAll(shoppingListsNew);
        this.selectedId = selectedIdNew != null ? (Integer) selectedIdNew : -1;
        notifyDataSetChanged();
    }

    public void updateSelectedId(Object selectedIdNew) {
        this.selectedId = selectedIdNew != null ? (Integer) selectedIdNew : -1;
        notifyDataSetChanged();
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
