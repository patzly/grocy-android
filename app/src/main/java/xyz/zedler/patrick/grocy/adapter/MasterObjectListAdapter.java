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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020-2021 by Patrick Zedler & Dominic Zedler
 */

package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.api.GrocyApi;
import xyz.zedler.patrick.grocy.databinding.RowFilterChipsBinding;
import xyz.zedler.patrick.grocy.model.HorizontalFilterBarMulti;
import xyz.zedler.patrick.grocy.util.ObjectUtil;
import xyz.zedler.patrick.grocy.view.InputChip;

public class MasterObjectListAdapter extends RecyclerView.Adapter<MasterObjectListAdapter.ViewHolder> {

    private final static String TAG = MasterObjectListAdapter.class.getSimpleName();

    private Context context;
    private final ArrayList<Object> objects;
    private final MasterObjectListAdapterListener listener;
    private final String entity;
    private final HorizontalFilterBarMulti horizontalFilterBarMulti;

    public MasterObjectListAdapter(
            Context context,
            String entity,
            ArrayList<Object> objects,
            MasterObjectListAdapterListener listener,
            HorizontalFilterBarMulti horizontalFilterBarMulti
    ) {
        this.context = context;
        this.objects = new ArrayList<>(objects);
        this.listener = listener;
        this.entity = entity;
        this.horizontalFilterBarMulti = horizontalFilterBarMulti;
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

    public static class ItemViewHolder extends ViewHolder {
        private final LinearLayout linearLayoutItemContainer;
        private final TextView textViewName;

        public ItemViewHolder(View view) {
            super(view);

            linearLayoutItemContainer = view.findViewById(R.id.linear_master_item_container);
            textViewName = view.findViewById(R.id.text_master_item_name);
        }
    }

    public static class FilterRowViewHolder extends ViewHolder {
        private final WeakReference<Context> weakContext;
        private final RowFilterChipsBinding binding;
        private InputChip chipProductGroup;
        private final HorizontalFilterBarMulti horizontalFilterBarMulti;

        public FilterRowViewHolder(
                RowFilterChipsBinding binding,
                Context context,
                HorizontalFilterBarMulti horizontalFilterBarMulti
        ) {
            super(binding.getRoot());
            this.binding = binding;
            this.horizontalFilterBarMulti = horizontalFilterBarMulti;
            weakContext = new WeakReference<>(context);
        }

        public void bind() {
            HorizontalFilterBarMulti.Filter filter = horizontalFilterBarMulti.getFilter(HorizontalFilterBarMulti.PRODUCT_GROUP);
            if(filter != null && chipProductGroup == null) {
                chipProductGroup = new InputChip(
                        weakContext.get(),
                        filter.getObjectName(),
                        R.drawable.ic_round_category,
                        true,
                        () -> {
                            horizontalFilterBarMulti.removeFilter(HorizontalFilterBarMulti.PRODUCT_GROUP);
                            chipProductGroup = null;
                        }
                );
                binding.container.addView(chipProductGroup);
            } else if(filter != null) {
                chipProductGroup.setText(filter.getObjectName());
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(entity.equals(GrocyApi.ENTITY.PRODUCTS) && position == 0) return -1; // filter row
        return 0;
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
                    horizontalFilterBarMulti
            );
        } else {
            return new ItemViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.row_master_item,
                            parent,
                            false
                    )
            );
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int position) {
        if(entity.equals(GrocyApi.ENTITY.PRODUCTS) && position == 0) { // Filter row
            ((FilterRowViewHolder) viewHolder).bind();
            return;
        }

        ItemViewHolder holder = (ItemViewHolder) viewHolder;
        int movedPosition;
        if(entity.equals(GrocyApi.ENTITY.PRODUCTS)) {
            movedPosition = holder.getAdapterPosition() - 1;
        } else {
            movedPosition = holder.getAdapterPosition();
        }
        Object object = objects.get(movedPosition);

        // NAME
        holder.textViewName.setText(ObjectUtil.getObjectName(object, entity));

        // CONTAINER
        holder.linearLayoutItemContainer.setOnClickListener(
                view -> listener.onItemRowClicked(object)
        );
    }

    public void updateData(ArrayList<Object> newObjects) {
        DiffCallback diffCallback = new DiffCallback(
                newObjects,
                this.objects,
                entity
        );
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        this.objects.clear();
        this.objects.addAll(newObjects);
        diffResult.dispatchUpdatesTo(new AdapterListUpdateCallback(this, entity));
    }

    static class DiffCallback extends DiffUtil.Callback {
        ArrayList<Object> oldItems;
        ArrayList<Object> newItems;
        String entity;

        public DiffCallback(
                ArrayList<Object> newItems,
                ArrayList<Object> oldItems,
                String entity
        ) {
            this.newItems = newItems;
            this.oldItems = oldItems;
            this.entity = entity;
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
            Object newItem = newItems.get(newItemPos);
            Object oldItem = oldItems.get(oldItemPos);
            return compareContent ? newItem.equals(oldItem)
                    : ObjectUtil.getObjectId(newItem, entity)
                    == ObjectUtil.getObjectId(oldItem, entity);
        }
    }

    /**
     * Custom ListUpdateCallback that dispatches update events to the given adapter
     * with offset of 1, because the first item is the filter row.
     */
    public static final class AdapterListUpdateCallback implements ListUpdateCallback {
        @NonNull
        private final MasterObjectListAdapter mAdapter;
        private final int offset;

        public AdapterListUpdateCallback(@NonNull MasterObjectListAdapter adapter, String entity) {
            mAdapter = adapter;
            offset = entity.equals(GrocyApi.ENTITY.PRODUCTS) ? 1 : 0;
        }
        @Override
        public void onInserted(int position, int count) {
            mAdapter.notifyItemRangeInserted(position + offset, count);
        }
        @Override
        public void onRemoved(int position, int count) {
            mAdapter.notifyItemRangeRemoved(position + offset, count);
        }
        @Override
        public void onMoved(int fromPosition, int toPosition) {
            mAdapter.notifyItemMoved(fromPosition + offset, toPosition + offset);
        }
        @Override
        public void onChanged(int position, int count, Object payload) {
            mAdapter.notifyItemRangeChanged(position + offset, count, payload);
        }
    }

    @Override
    public int getItemCount() {
        return entity.equals(GrocyApi.ENTITY.PRODUCTS) ? objects.size()+1 : objects.size();
    }

    public interface MasterObjectListAdapterListener {
        void onItemRowClicked(Object object);
    }
}
