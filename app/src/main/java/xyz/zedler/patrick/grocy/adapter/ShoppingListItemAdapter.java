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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.databinding.RowShoppingListItemBinding;
import xyz.zedler.patrick.grocy.model.GroupedListItem;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.ShoppingListBottomNotes;
import xyz.zedler.patrick.grocy.model.ShoppingListItem;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class ShoppingListItemAdapter extends RecyclerView.Adapter<ShoppingListItemAdapter.ViewHolder> {

    private final static String TAG = ShoppingListItemAdapter.class.getSimpleName();
    private final static boolean DEBUG = false;

    private Context context;
    private ArrayList<GroupedListItem> groupedListItems;
    private ArrayList<QuantityUnit> quantityUnits;
    private ShoppingListItemAdapterListener listener;

    public ShoppingListItemAdapter(
            Context context,
            ArrayList<GroupedListItem> groupedListItems,
            ArrayList<QuantityUnit> quantityUnits,
            ShoppingListItemAdapterListener listener
    ) {
        this.context = context;
        this.groupedListItems = groupedListItems;
        this.quantityUnits = quantityUnits;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout linearLayoutContainer, linearLayoutNote, linearLayoutBottomNotes;
        private TextView textViewName, textViewAmount, textViewGroupName, textViewNote;
        private TextView textViewNoteName, textViewBottomNotes;
        private View divider;

        public ViewHolder(View view) {
            super(view);

            linearLayoutContainer = view.findViewById(R.id.linear_shopping_list_item_container);
            linearLayoutNote = view.findViewById(R.id.linear_shopping_list_note);
            textViewName = view.findViewById(R.id.text_shopping_list_item_name);
            textViewAmount = view.findViewById(R.id.text_shopping_list_item_amount);
            textViewNote = view.findViewById(R.id.text_shopping_list_note);
            textViewNoteName = view.findViewById(R.id.text_shopping_list_note_as_name);

            linearLayoutBottomNotes = view.findViewById(R.id.linear_shopping_list_bottom_notes);
            textViewBottomNotes = view.findViewById(R.id.text_shopping_list_bottom_notes);

            textViewGroupName = view.findViewById(R.id.text_shopping_list_group_name);
            divider = view.findViewById(R.id.view_shopping_list_group_divider);
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
            return new ViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.row_shopping_list_group,
                            parent,
                            false
                    )
            );
        } else if(viewType == GroupedListItem.TYPE_ENTRY) {
            return new ViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.row_shopping_list_item,
                            parent,
                            false
                    )
            );
        } else {
            return new ViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.row_shopping_list_bottom_notes,
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
        if(type == GroupedListItem.TYPE_HEADER) {
            if(holder.getAdapterPosition() == 0) {
                holder.divider.setVisibility(View.GONE);
            }
            holder.textViewGroupName.setText(((ProductGroup) groupedListItem).getName());
            return;
        }
        if(type == GroupedListItem.TYPE_BOTTOM_NOTES) {
            holder.textViewBottomNotes.setText(
                    ((ShoppingListBottomNotes) groupedListItem).getNotes()
            );
            holder.linearLayoutBottomNotes.setOnClickListener(
                    view -> listener.onItemRowClicked(holder.getAdapterPosition())
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
                if(quantityUnits.get(i).getId() == shoppingListItem.getProduct().getQuIdPurchase()) {
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

        if(shoppingListItem.isMissing()) {
            holder.textViewAmount.setTypeface(
                    ResourcesCompat.getFont(context, R.font.roboto_mono_medium)
            );
            holder.textViewAmount.setTextColor(
                    ContextCompat.getColor(context, R.color.retro_blue_fg)
            );
        } else {
            holder.textViewAmount.setTypeface(
                    ResourcesCompat.getFont(context, R.font.roboto_mono_regular)
            );
            holder.textViewAmount.setTextColor(
                    ContextCompat.getColor(context, R.color.on_background_secondary)
            );
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
                holder.linearLayoutNote.setVisibility(View.VISIBLE);
                holder.textViewNote.setText(shoppingListItem.getNote().trim());
            } else {
                holder.textViewNoteName.setVisibility(View.VISIBLE);
                holder.textViewNoteName.setText(shoppingListItem.getNote().trim());
            }
        } else {
            if(holder.textViewName.getVisibility() == View.VISIBLE) {
                holder.linearLayoutNote.setVisibility(View.GONE);
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

        holder.linearLayoutContainer.setOnClickListener(
                view -> listener.onItemRowClicked(holder.getAdapterPosition())
        );

    }

    @Override
    public int getItemCount() {
        return groupedListItems != null ? groupedListItems.size() : 0;
    }

    public interface ShoppingListItemAdapterListener {
        void onItemRowClicked(int position);
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
            binding.textShoppingListItemName.setText(product.getName());
            binding.textShoppingListItemName.setVisibility(View.VISIBLE);
        } else {
            binding.textShoppingListItemName.setText(null);
            binding.textShoppingListItemName.setVisibility(View.GONE);
        }
        if(listItem.isUndone()) {
            binding.textShoppingListItemName.setPaintFlags(
                    binding.textShoppingListItemName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
            );
        } else {
            binding.textShoppingListItemName.setPaintFlags(
                    binding.textShoppingListItemName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
        }

        // NOTE AS NAME

        if(binding.textShoppingListItemName.getVisibility() == View.VISIBLE) {
            binding.textShoppingListNoteAsName.setVisibility(View.GONE);
            binding.textShoppingListNoteAsName.setText(null);
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

            binding.textShoppingListItemAmount.setText(
                    context.getString(
                            R.string.subtitle_amount,
                            NumUtil.trim(listItem.getAmount()),
                            listItem.getAmount() == 1
                                    ? quantityUnit.getName()
                                    : quantityUnit.getNamePlural()
                    )
            );
        } else {
            binding.textShoppingListItemAmount.setText(NumUtil.trim(listItem.getAmount()));
        }

        if(listItem.isMissing()) {
            binding.textShoppingListItemAmount.setTypeface(
                    ResourcesCompat.getFont(context, R.font.roboto_mono_medium)
            );
            binding.textShoppingListItemAmount.setTextColor(
                    ContextCompat.getColor(context, R.color.retro_blue_fg)
            );
        } else {
            binding.textShoppingListItemAmount.setTypeface(
                    ResourcesCompat.getFont(context, R.font.roboto_mono_regular)
            );
            binding.textShoppingListItemAmount.setTextColor(
                    ContextCompat.getColor(context, R.color.on_background_secondary)
            );
        }
        if(listItem.isUndone()) {
            binding.textShoppingListItemAmount.setPaintFlags(
                    binding.textShoppingListItemAmount.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
            );
        } else {
            binding.textShoppingListItemAmount.setPaintFlags(
                    binding.textShoppingListItemAmount.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
        }

        // NOTE

        if(listItem.getNote() != null && !listItem.getNote().isEmpty()) {
            if(binding.textShoppingListItemName.getVisibility() == View.VISIBLE) {
                binding.linearShoppingListNote.setVisibility(View.VISIBLE);
                binding.textShoppingListNote.setText(listItem.getNote().trim());
            } else {
                binding.textShoppingListNoteAsName.setVisibility(View.VISIBLE);
                binding.textShoppingListNoteAsName.setText(listItem.getNote().trim());
            }
        } else {
            if(binding.textShoppingListItemName.getVisibility() == View.VISIBLE) {
                binding.linearShoppingListNote.setVisibility(View.GONE);
                binding.textShoppingListNote.setText(null);
            }
        }
        if(binding.textShoppingListNoteAsName.getVisibility() == View.VISIBLE) {
            if(listItem.isUndone()) {
                binding.textShoppingListNoteAsName.setPaintFlags(
                        binding.textShoppingListNoteAsName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
                );
            } else {
                binding.textShoppingListNoteAsName.setPaintFlags(
                        binding.textShoppingListNoteAsName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                );
            }
        } else {
            if(listItem.isUndone()) {
                binding.textShoppingListNote.setPaintFlags(
                        binding.textShoppingListNote.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
                );
            } else {
                binding.textShoppingListNote.setPaintFlags(
                        binding.textShoppingListNote.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                );
            }
        }
    }
}
