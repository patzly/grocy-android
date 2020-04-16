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
import xyz.zedler.patrick.grocy.model.ProductEntries;
import xyz.zedler.patrick.grocy.model.ProductEntry;

public class ProductEntryAdapter
        extends RecyclerView.Adapter<ProductEntryAdapter.ViewHolder> {

    private final static String TAG = ProductEntryAdapter.class.getSimpleName();
    private final static boolean DEBUG = false;

    private ProductEntries productEntries;
    private int selectedId;
    private ProductEntryAdapterListener listener;

    public ProductEntryAdapter(
            ProductEntries productEntries,
            int selectedId,
            ProductEntryAdapterListener listener
    ) {
        this.productEntries = productEntries;
        this.selectedId = selectedId;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout linearLayoutContainer, linearLayoutNone;
        private TextView textViewName, textViewNoneSubtitle;
        private ImageView imageViewSelected;

        public ViewHolder(View view) {
            super(view);

            linearLayoutContainer = view.findViewById(
                    R.id.linear_product_entry_container
            );
            linearLayoutNone = view.findViewById(R.id.linear_product_entry_none_subtitle);
            textViewName = view.findViewById(R.id.text_product_entry_name);
            textViewNoneSubtitle = view.findViewById(R.id.text_product_entry_none_subtitle);
            imageViewSelected = view.findViewById(R.id.image_product_entry_selected);
        }
    }

    @NonNull
    @Override
    public ProductEntryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ProductEntryAdapter.ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.row_product_entry,
                        parent,
                        false
                )
        );
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(
            @NonNull final ProductEntryAdapter.ViewHolder holder,
            int position
    ) {
        ProductEntry productEntry = productEntries.get(position);

        // NAME

        //holder.textViewName.setText(productEntry.get.getLocationName());

        // DEFAULT

/*        if(productEntry.getLocationId() == productDetails.getLocation().getId()) {
            holder.linearLayoutNone.setVisibility(View.VISIBLE);
        }

        // SELECTED

        if(productEntry.getLocationId() == selectedId) {
            holder.imageViewSelected.setVisibility(View.VISIBLE);
        }*/

        // CONTAINER

        holder.linearLayoutContainer.setOnClickListener(
                view -> listener.onItemRowClicked(position)
        );
    }

    @Override
    public long getItemId(int position) {
        return productEntries.get(position).getProductId();
    }

    @Override
    public int getItemCount() {
        return productEntries.size();
    }

    public interface ProductEntryAdapterListener {
        void onItemRowClicked(int position);
    }
}
