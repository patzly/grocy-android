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
import xyz.zedler.patrick.grocy.model.BatchItem;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class BatchItemAdapter extends RecyclerView.Adapter<BatchItemAdapter.ViewHolder> {

    private final static String TAG = BatchItemAdapter.class.getSimpleName();
    private final static boolean DEBUG = false;

    private Context context;
    private List<BatchItem> batchItems;
    private BatchItemAdapterListener listener;

    public BatchItemAdapter(
            Context context,
            List<BatchItem> batchItems,
            BatchItemAdapterListener listener
    ) {
        this.context = context;
        this.batchItems = batchItems;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout linearLayoutItemContainer;
        private TextView textViewName, textViewAmount;

        public ViewHolder(View view) {
            super(view);

            linearLayoutItemContainer = view.findViewById(R.id.linear_stock_item_container);
            textViewName = view.findViewById(R.id.text_stock_item_name);
            textViewAmount = view.findViewById(R.id.text_stock_item_amount);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.row_stock_item,
                        parent,
                        false
                )
        );
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        BatchItem batchItem = batchItems.get(position);

        // NAME

        holder.textViewName.setText(batchItem.getProductName());

        // AMOUNT

        holder.textViewAmount.setText(NumUtil.trim(batchItem.getAmount()));

        // CONTAINER

        holder.linearLayoutItemContainer.setOnClickListener(
                view -> listener.onItemRowClicked(position)
        );
    }

    @Override
    public int getItemCount() {
        return batchItems != null ? batchItems.size() : 0;
    }

    public interface BatchItemAdapterListener {
        void onItemRowClicked(int position);
    }
}
