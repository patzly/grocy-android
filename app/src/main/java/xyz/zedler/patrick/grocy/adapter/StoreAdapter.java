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

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.Store;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.ViewHolder> {

    private final static String TAG = StoreAdapter.class.getSimpleName();
    private final static boolean DEBUG = false;

    private ArrayList<Store> stores;
    private int selectedId;
    private StoreAdapterListener listener;

    public StoreAdapter(
            ArrayList<Store> stores,
            int selectedId,
            StoreAdapterListener listener
    ) {
        this.stores = stores;
        this.selectedId = selectedId;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout linearLayoutContainer;
        private TextView textViewName;
        private ImageView imageViewSelected;

        public ViewHolder(View view) {
            super(view);

            linearLayoutContainer = view.findViewById(R.id.linear_master_edit_selection_container);
            textViewName = view.findViewById(R.id.text_master_edit_selection_name);
            imageViewSelected = view.findViewById(R.id.image_master_edit_selection_selected);
        }
    }

    @NonNull
    @Override
    public StoreAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new StoreAdapter.ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.row_master_edit_selection_sheet,
                        parent,
                        false
                )
        );
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(
            @NonNull final StoreAdapter.ViewHolder holder,
            int position
    ) {
        Store store = stores.get(holder.getAdapterPosition());

        // NAME

        holder.textViewName.setText(store.getName());

        // SELECTED

        if(store.getId() == selectedId) {
            holder.imageViewSelected.setVisibility(View.VISIBLE);
        }

        // CONTAINER

        holder.linearLayoutContainer.setOnClickListener(
                view -> listener.onItemRowClicked(holder.getAdapterPosition())
        );
    }

    @Override
    public long getItemId(int position) {
        return stores.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return stores.size();
    }

    public interface StoreAdapterListener {
        void onItemRowClicked(int position);
    }
}
