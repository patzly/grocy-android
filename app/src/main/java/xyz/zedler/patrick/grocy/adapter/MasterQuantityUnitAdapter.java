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
import xyz.zedler.patrick.grocy.model.QuantityUnit;

public class MasterQuantityUnitAdapter extends RecyclerView.Adapter<MasterQuantityUnitAdapter.ViewHolder> {

    private final static String TAG = MasterQuantityUnitAdapter.class.getSimpleName();
    private final static boolean DEBUG = false;

    private Context context;
    private List<QuantityUnit> quantityUnits;
    private MasterQuantityUnitAdapterListener listener;

    public MasterQuantityUnitAdapter(
            Context context,
            List<QuantityUnit> quantityUnits,
            MasterQuantityUnitAdapterListener listener
    ) {
        this.context = context;
        this.quantityUnits = quantityUnits;
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
        holder.textViewName.setText(quantityUnits.get(position).getName());

        // CONTAINER
        holder.linearLayoutItemContainer.setOnClickListener(
                view -> listener.onItemRowClicked(position)
        );
    }

    @Override
    public long getItemId(int position) {
        return quantityUnits.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return quantityUnits != null ? quantityUnits.size() : 0;
    }

    public interface MasterQuantityUnitAdapterListener {
        void onItemRowClicked(int position);
    }
}
