package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.ProductDetails;
import xyz.zedler.patrick.grocy.model.StockLocation;
import xyz.zedler.patrick.grocy.model.StockLocations;

public class StockLocationAdapter
        extends RecyclerView.Adapter<StockLocationAdapter.ViewHolder> {

    private final static String TAG = StockLocationAdapter.class.getSimpleName();
    private final static boolean DEBUG = false;

    private StockLocations stockLocations;
    private ProductDetails productDetails;
    private int selectedId;
    private StockLocationAdapterListener listener;

    public StockLocationAdapter(
            StockLocations stockLocations,
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
        private LinearLayout linearLayoutContainer, linearLayoutDefault;
        private TextView textViewName;
        private ImageView imageViewSelected;

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
        StockLocation stockLocation = stockLocations.get(position);

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
                view -> listener.onItemRowClicked(position)
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
