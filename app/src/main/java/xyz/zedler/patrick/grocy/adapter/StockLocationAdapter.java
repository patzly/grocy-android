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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.StockLocation;

public class StockLocationAdapter
        extends RecyclerView.Adapter<StockLocationAdapter.ViewHolder> {

    private final static String TAG = StockLocationAdapter.class.getSimpleName();

    private final ArrayList<StockLocation> stockLocations;
    private final ProductDetails productDetails;
    private final int selectedId;
    private final StockLocationAdapterListener listener;

    public StockLocationAdapter(
            ArrayList<StockLocation> stockLocations,
            ProductDetails productDetails,
            int selectedId,
            StockLocationAdapterListener listener
    ) {
        this.stockLocations = stockLocations;
        this.productDetails = productDetails;
        this.selectedId = selectedId;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout linearLayoutContainer;
        private final LinearLayout linearLayoutDefault;
        private final TextView textViewName;
        private final ImageView imageViewSelected;

        public ViewHolder(View view) {
            super(view);

            linearLayoutContainer = view.findViewById(R.id.linear_stock_location_container);
            linearLayoutDefault = view.findViewById(R.id.linear_stock_location_default);
            textViewName = view.findViewById(R.id.text_stock_location_name);
            imageViewSelected = view.findViewById(R.id.image_stock_location_selected);
        }
    }

    @NonNull
    @Override
    public StockLocationAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new StockLocationAdapter.ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.row_stock_location,
                        parent,
                        false
                )
        );
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(
            @NonNull final StockLocationAdapter.ViewHolder holder,
            int position
    ) {
        StockLocation stockLocation = stockLocations.get(holder.getAdapterPosition());

        // NAME

        holder.textViewName.setText(stockLocation.getLocationName());

        // DEFAULT

        if(stockLocation.getLocationId() == productDetails.getLocation().getId()) {
            holder.linearLayoutDefault.setVisibility(View.VISIBLE);
        }

        // SELECTED

        if(stockLocation.getLocationId() == selectedId) {
            holder.imageViewSelected.setVisibility(View.VISIBLE);
        }

        // CONTAINER

        holder.linearLayoutContainer.setOnClickListener(
                view -> listener.onItemRowClicked(holder.getAdapterPosition())
        );
    }

    @Override
    public long getItemId(int position) {
        return stockLocations.get(position).getProductId();
    }

    @Override
    public int getItemCount() {
        return stockLocations.size();
    }

    public interface StockLocationAdapterListener {
        void onItemRowClicked(int position);
    }
}
