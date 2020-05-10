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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.MissingBatchItem;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class MissingBatchItemAdapter
        extends RecyclerView.Adapter<MissingBatchItemAdapter.ViewHolder> {

    private final static String TAG = MissingBatchItemAdapter.class.getSimpleName();
    private final static boolean DEBUG = false;

    private ArrayList<MissingBatchItem> missingBatchItems;
    private MissingBatchItemAdapterListener listener;

    public MissingBatchItemAdapter(
            ArrayList<MissingBatchItem> missingBatchItems,
            MissingBatchItemAdapterListener listener
    ) {
        this.missingBatchItems = missingBatchItems;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout linearLayoutItemContainer;
        private TextView textViewName, textViewAmount;
        private ImageView imageViewOnServer;

        public ViewHolder(View view) {
            super(view);

            linearLayoutItemContainer = view.findViewById(R.id.linear_missing_batch_item_container);
            textViewName = view.findViewById(R.id.text_missing_batch_item_name);
            textViewAmount = view.findViewById(R.id.text_missing_batch_item_amount);
            imageViewOnServer = view.findViewById(R.id.image_missing_batch_item_status);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.row_missing_batch_item,
                        parent,
                        false
                )
        );
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        MissingBatchItem missingBatchItem = missingBatchItems.get(holder.getAdapterPosition());

        // NAME

        holder.textViewName.setText(missingBatchItem.getProductName());

        // AMOUNT

        holder.textViewAmount.setText(NumUtil.trim(missingBatchItem.getPurchaseEntriesSize()));

        // IS ON SERVER

        holder.imageViewOnServer.setImageDrawable(
                ContextCompat.getDrawable(
                        holder.imageViewOnServer.getContext(),
                        missingBatchItem.getIsOnServer()
                                ? R.drawable.ic_round_cloud_done_outline
                                : R.drawable.ic_round_cloud_pending
                )
        );

        // CONTAINER

        holder.linearLayoutItemContainer.setOnClickListener(
                view -> listener.onItemRowClicked(position)
        );
    }

    @Override
    public int getItemCount() {
        return missingBatchItems != null ? missingBatchItems.size() : 0;
    }

    public interface MissingBatchItemAdapterListener {
        void onItemRowClicked(int position);
    }
}
