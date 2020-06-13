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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListBottomNotes;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class ShoppingItemAdapter extends
        RecyclerView.Adapter<ShoppingItemAdapter.ViewHolder> {

    private final static String TAG = ShoppingItemAdapter.class.getSimpleName();
    private final static boolean DEBUG = false;

    private Context context;
    private ArrayList<GroupedListItem> groupedListItems;
    private ArrayList<QuantityUnit> quantityUnits;
    private ShoppingListItemSpecialAdapterListener listener;

    public ShoppingItemAdapter(
            Context context,
            ArrayList<GroupedListItem> groupedListItems,
            ArrayList<QuantityUnit> quantityUnits,
            ShoppingListItemSpecialAdapterListener listener
    ) {
        this.context = context;
        this.groupedListItems = groupedListItems;
        this.quantityUnits = quantityUnits;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardViewContainer;
        private TextView textViewName, textViewAmount, textViewGroupName, textViewNote;
        private TextView textViewNoteName, textViewBottomNotes;

        public ViewHolder(View view) {
            super(view);

            cardViewContainer = view.findViewById(R.id.card_shopping_item_container);
            textViewName = view.findViewById(R.id.text_shopping_item_name);
            textViewAmount = view.findViewById(R.id.text_shopping_item_amount);
            textViewNote = view.findViewById(R.id.text_shopping_item_note);
            textViewNoteName = view.findViewById(R.id.text_shopping_note_as_name);

            textViewBottomNotes = view.findViewById(R.id.text_shopping_bottom_notes);

            textViewGroupName = view.findViewById(R.id.text_shopping_group_name);
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
            return new ShoppingItemAdapter.ViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.row_shopping_group,
                            parent,
                            false
                    )
            );
        } else if(viewType == GroupedListItem.TYPE_ENTRY) {
            return new ShoppingItemAdapter.ViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.row_shopping_item,
                            parent,
                            false
                    )
            );
        } else {
            return new ShoppingItemAdapter.ViewHolder(
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
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

        GroupedListItem groupedListItem = groupedListItems.get(holder.getAdapterPosition());

        int type = getItemViewType(position);
        if (type == GroupedListItem.TYPE_HEADER) {
            holder.textViewGroupName.setText(((ProductGroup) groupedListItem).getName());
            return;
        }
        if(type == GroupedListItem.TYPE_BOTTOM_NOTES) {
            holder.textViewBottomNotes.setText(
                    ((ShoppingListBottomNotes) groupedListItem).getNotes()
            );
            return;
        }

        ShoppingListItem shoppingListItem = (ShoppingListItem) groupedListItem;

        // NAME

        Product product = shoppingListItem.getProduct();
        if(product != null) {
            holder.textViewName.setText(product.getName());
            holder.textViewName.setVisibility(View.VISIBLE);
        } else {
            holder.textViewName.setText(null);
            holder.textViewName.setVisibility(View.GONE);
        }
        if(shoppingListItem.isUndone()) {
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

        if(shoppingListItem.getProduct() != null) {
            QuantityUnit quantityUnit = new QuantityUnit();
            for(int i = 0; i < quantityUnits.size(); i++) {
                if(quantityUnits.get(i).getId()
                        == shoppingListItem.getProduct().getQuIdPurchase()
                ) {
                    quantityUnit = quantityUnits.get(i);
                    break;
                }
            }

            if(DEBUG) Log.i(TAG, "onBindViewHolder: " + quantityUnit.getName());

            holder.textViewAmount.setText(
                    context.getString(
                            R.string.subtitle_amount,
                            NumUtil.trim(shoppingListItem.getAmount()),
                            shoppingListItem.getAmount() == 1
                                    ? quantityUnit.getName()
                                    : quantityUnit.getNamePlural()
                    )
            );
        } else {
            holder.textViewAmount.setText(NumUtil.trim(shoppingListItem.getAmount()));
        }

        if(shoppingListItem.isUndone()) {
            holder.textViewAmount.setPaintFlags(
                    holder.textViewAmount.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
            );
        } else {
            holder.textViewAmount.setPaintFlags(
                    holder.textViewAmount.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
        }

        // NOTE

        if(shoppingListItem.getNote() != null && !shoppingListItem.getNote().isEmpty()) {
            if(holder.textViewName.getVisibility() == View.VISIBLE) {
                holder.textViewNote.setVisibility(View.VISIBLE);
                holder.textViewNote.setText(shoppingListItem.getNote().trim());
            } else {
                holder.textViewNoteName.setVisibility(View.VISIBLE);
                holder.textViewNoteName.setText(shoppingListItem.getNote().trim());
            }
        } else {
            if(holder.textViewName.getVisibility() == View.VISIBLE) {
                holder.textViewNote.setVisibility(View.GONE);
                holder.textViewNote.setText(null);
            }
        }
        if(holder.textViewNoteName.getVisibility() == View.VISIBLE) {
            if(shoppingListItem.isUndone()) {
                holder.textViewNoteName.setPaintFlags(
                        holder.textViewNoteName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
                );
            } else {
                holder.textViewNoteName.setPaintFlags(
                        holder.textViewNoteName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                );
            }
        } else {
            if(shoppingListItem.isUndone()) {
                holder.textViewNote.setPaintFlags(
                        holder.textViewNote.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
                );
            } else {
                holder.textViewNote.setPaintFlags(
                        holder.textViewNote.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                );
            }
        }

        // CONTAINER

        holder.cardViewContainer.setOnClickListener(
                view -> listener.onItemRowClicked(holder.getAdapterPosition())
        );

    }

    @Override
    public int getItemCount() {
        return groupedListItems != null ? groupedListItems.size() : 0;
    }

    public interface ShoppingListItemSpecialAdapterListener {
        void onItemRowClicked(int position);
    }
}
