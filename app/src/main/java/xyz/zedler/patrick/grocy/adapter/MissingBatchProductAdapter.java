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

import java.util.ArrayList;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.MissingBatchProduct;
import xyz.zedler.patrick.grocy.util.NumUtil;

public class MissingBatchProductAdapter extends RecyclerView.Adapter<MissingBatchProductAdapter.ViewHolder> {

    private final static String TAG = MissingBatchProductAdapter.class.getSimpleName();
    private final static boolean DEBUG = false;

    private Context context;
    private ArrayList<MissingBatchProduct> missingBatchProducts;
    private BatchItemAdapterListener listener;

    public MissingBatchProductAdapter(
            Context context,
            ArrayList<MissingBatchProduct> missingBatchProducts,
            BatchItemAdapterListener listener
    ) {
        this.context = context;
        this.missingBatchProducts = missingBatchProducts;
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
        MissingBatchProduct missingBatchProduct = missingBatchProducts.get(position);

        // NAME

        holder.textViewName.setText(missingBatchProduct.getProductName());

        // AMOUNT

        holder.textViewAmount.setText(NumUtil.trim(missingBatchProduct.getAmount()));

        // CONTAINER

        holder.linearLayoutItemContainer.setOnClickListener(
                view -> listener.onItemRowClicked(position)
        );
    }

    @Override
    public int getItemCount() {
        return missingBatchProducts != null ? missingBatchProducts.size() : 0;
    }

    public interface BatchItemAdapterListener {
        void onItemRowClicked(int position);
    }
}
