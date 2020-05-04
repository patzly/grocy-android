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
import xyz.zedler.patrick.grocy.model.MissingBatchItem;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class MissingBatchItemAdapter
        extends RecyclerView.Adapter<MissingBatchItemAdapter.ViewHolder> {

    private final static String TAG = MissingBatchItemAdapter.class.getSimpleName();
    private final static boolean DEBUG = false;

    private ArrayList<MissingBatchItem> missingBatchItems;
    private MissingBatchItemAdapterListener listener;

    public MissingBatchItemAdapter(
            ArrayList<MissingBatchItem> missingBatchItems,
            MissingBatchItemAdapterListener listener
    ) {
        this.missingBatchItems = missingBatchItems;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout linearLayoutItemContainer;
        private TextView textViewName, textViewAmount;

        public ViewHolder(View view) {
            super(view);

            linearLayoutItemContainer = view.findViewById(R.id.linear_missing_batch_item_container);
            textViewName = view.findViewById(R.id.text_missing_batch_item_name);
            textViewAmount = view.findViewById(R.id.text_missing_batch_item_amount);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.row_missing_batch_item,
                        parent,
                        false
                )
        );
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        MissingBatchItem missingBatchItem = missingBatchItems.get(position);

        // NAME

        holder.textViewName.setText(missingBatchItem.getProductName());

        // AMOUNT

        holder.textViewAmount.setText(NumUtil.trim(missingBatchItem.getAmount()));

        // CONTAINER

        holder.linearLayoutItemContainer.setOnClickListener(
                view -> listener.onItemRowClicked(position)
        );
    }

    @Override
    public int getItemCount() {
        return missingBatchItems != null ? missingBatchItems.size() : 0;
    }

    public interface MissingBatchItemAdapterListener {
        void onItemRowClicked(int position);
    }
}
