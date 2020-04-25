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
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Locations;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.ViewHolder> {

    private final static String TAG = LocationAdapter.class.getSimpleName();
    private final static boolean DEBUG = false;

    private Locations locations;
    private int selectedId;
    private LocationAdapterListener listener;

    public LocationAdapter(
            Locations locations,
            int selectedId,
            LocationAdapterListener listener
    ) {
        this.locations = locations;
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
    public LocationAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new LocationAdapter.ViewHolder(
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
            @NonNull final LocationAdapter.ViewHolder holder,
            int position
    ) {
        Location location = locations.get(position);

        // NAME

        holder.textViewName.setText(location.getName());

        // SELECTED

        if(location.getId() == selectedId) {
            holder.imageViewSelected.setVisibility(View.VISIBLE);
        }

        // CONTAINER

        holder.linearLayoutContainer.setOnClickListener(
                view -> listener.onItemRowClicked(position)
        );
    }

    @Override
    public long getItemId(int position) {
        return locations.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    public interface LocationAdapterListener {
        void onItemRowClicked(int position);
    }
}
