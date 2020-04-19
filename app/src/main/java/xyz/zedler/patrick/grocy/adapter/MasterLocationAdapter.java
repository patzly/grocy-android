package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.Location;

public class MasterLocationAdapter extends RecyclerView.Adapter<MasterLocationAdapter.ViewHolder> {

    private final static String TAG = MasterLocationAdapter.class.getSimpleName();
    private final static boolean DEBUG = false;

    private Context context;
    private List<Location> locations;
    private MasterLocationAdapterListener listener;

    public MasterLocationAdapter(
            Context context,
            List<Location> locations,
            MasterLocationAdapterListener listener
    ) {
        this.context = context;
        this.locations = locations;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout linearLayoutItemContainer;
        private TextView textViewName;

        public ViewHolder(View view) {
            super(view);

            linearLayoutItemContainer = view.findViewById(R.id.linear_master_product_container);
            textViewName = view.findViewById(R.id.text_master_product_name);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.row_master_product,
                        parent,
                        false
                )
        );
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        // NAME
        holder.textViewName.setText(locations.get(position).getName());

        // CONTAINER
        holder.linearLayoutItemContainer.setOnClickListener(
                view -> listener.onItemRowClicked(position)
        );
    }

    @Override
    public long getItemId(int position) {
        return locations.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return locations != null ? locations.size() : 0;
    }

    public interface MasterLocationAdapterListener {
        void onItemRowClicked(int position);
    }
}
