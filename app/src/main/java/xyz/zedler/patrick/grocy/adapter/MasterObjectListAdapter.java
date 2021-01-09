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
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.util.ObjectUtil;

public class MasterObjectListAdapter extends RecyclerView.Adapter<MasterObjectListAdapter.ViewHolder> {

    private final static String TAG = MasterObjectListAdapter.class.getSimpleName();

    private final ArrayList<Object> objects;
    private final MasterObjectListAdapterListener listener;
    private final String entity;

    public MasterObjectListAdapter(
            String entity,
            ArrayList<Object> objects,
            MasterObjectListAdapterListener listener
    ) {
        this.objects = new ArrayList<>(objects);
        this.listener = listener;
        this.entity = entity;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout linearLayoutItemContainer;
        private final TextView textViewName;

        public ViewHolder(View view) {
            super(view);

            linearLayoutItemContainer = view.findViewById(R.id.linear_master_item_container);
            textViewName = view.findViewById(R.id.text_master_item_name);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.row_master_item,
                        parent,
                        false
                )
        );
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        // NAME
        holder.textViewName.setText(ObjectUtil.getObjectName(
                objects.get(holder.getAdapterPosition()),
                entity
        ));

        // CONTAINER
        holder.linearLayoutItemContainer.setOnClickListener(
                view -> listener.onItemRowClicked(holder.getAdapterPosition())
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
        diffResult.dispatchUpdatesTo(this);
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

    @Override
    public long getItemId(int position) {
        return ObjectUtil.getObjectId(objects.get(position), entity);
    }

    @Override
    public int getItemCount() {
        return objects.size();
    }

    public interface MasterObjectListAdapterListener {
        void onItemRowClicked(int position);
    }
}
