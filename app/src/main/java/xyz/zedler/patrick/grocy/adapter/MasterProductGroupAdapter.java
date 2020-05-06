package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.ProductGroup;

public class MasterProductGroupAdapter extends RecyclerView.Adapter<MasterProductGroupAdapter.ViewHolder> {

    private final static String TAG = MasterProductGroupAdapter.class.getSimpleName();
    private final static boolean DEBUG = false;

    private ArrayList<ProductGroup> productGroups;
    private MasterProductGroupAdapterListener listener;

    public MasterProductGroupAdapter(
            ArrayList<ProductGroup> productGroups,
            MasterProductGroupAdapterListener listener
    ) {
        this.productGroups = productGroups;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout linearLayoutItemContainer;
        private TextView textViewName;

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
        holder.textViewName.setText(productGroups.get(holder.getAdapterPosition()).getName());

        // CONTAINER
        holder.linearLayoutItemContainer.setOnClickListener(
                view -> listener.onItemRowClicked(holder.getAdapterPosition())
        );
    }

    @Override
    public int getItemCount() {
        return productGroups != null ? productGroups.size() : 0;
    }

    public interface MasterProductGroupAdapterListener {
        void onItemRowClicked(int position);
    }
}
