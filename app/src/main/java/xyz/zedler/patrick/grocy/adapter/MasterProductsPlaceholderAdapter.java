package xyz.zedler.patrick.grocy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import xyz.zedler.patrick.grocy.R;

public class MasterProductsPlaceholderAdapter
        extends RecyclerView.Adapter<MasterProductsPlaceholderAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View view) {
            super(view);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.row_master_products_placeholder,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {}

    @Override
    public int getItemCount() {
        return 10;
    }
}
