package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockItem;

public class StockItemAdapter extends RecyclerView.Adapter<StockItemAdapter.ViewHolder> {

    private final static String TAG = "StockItemAdapter";
    private final static boolean DEBUG = true;

    private Context context;
    private List<StockItem> stockItems;
    private StockItem stockItem;
    private List<QuantityUnit> quantityUnits;
    private StockItemAdapterListener listener;

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {
        public LinearLayout linearLayoutItemContainer;
        private TextView textViewName, textViewAmount;

        public ViewHolder(View view) {
            super(view);

            linearLayoutItemContainer = view.findViewById(R.id.linear_stock_item_overview_item_container);
            textViewName = view.findViewById(R.id.text_stock_item_name);
            textViewAmount = view.findViewById(R.id.text_stock_item_amount);

            view.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View view) {
            listener.onRowLongClicked(getAdapterPosition());
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            return true;
        }
    }

    public StockItemAdapter(
            Context context,
            List<StockItem> stockItems,
            List<QuantityUnit> quantityUnits,
            StockItemAdapterListener listener
    ) {
        this.context = context;
        this.stockItems = stockItems;
        this.quantityUnits = quantityUnits;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.view_stock_item, parent, false
                )
        );
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        stockItem = stockItems.get(position);

        QuantityUnit quantityUnit = new QuantityUnit();
        for(int i = 0; i < quantityUnits.size(); i++) {
            if(quantityUnits.get(i).getId() == stockItem.getProduct().getQuIdStock()) {
                quantityUnit = quantityUnits.get(i);
                break;
            }
        }

        holder.textViewName.setText(stockItem.getProduct().getName());

        StringBuilder stringBuilder = new StringBuilder(
                context.getString(
                        R.string.subtitle_amount,
                        stockItem.getAmount(),
                        stockItem.getAmount() == 1
                                ? quantityUnit.getName()
                                : quantityUnit.getNamePlural()
                )
        );
        if(stockItem.getAmountOpened() > 0) {
            stringBuilder.append(" ");
            stringBuilder.append(
                    context.getString(
                            R.string.subtitle_amount_opened,
                            stockItem.getAmountOpened()
                    )
            );
        }

        holder.textViewAmount.setText(stringBuilder);
        holder.textViewAmount.setTextColor(
                ContextCompat.getColor(
                        context,
                        stockItem.getAmount() > stockItem.getProduct().getMinStockAmount()
                                ? R.color.on_background_secondary
                                : R.color.retro_dirt_dark
                )
        );

        applyClickEvents(holder, position);
    }

    @Override
    public long getItemId(int position) {
        return stockItems.get(position).getProductId();
    }

    @Override
    public int getItemCount() {
        return stockItems.size();
    }

    /*public void refreshProducts(List<Product> products) {
        this.products = products;
    }*/

    private void applyClickEvents(ViewHolder holder, final int position) {
        holder.linearLayoutItemContainer.setOnClickListener(
                view -> listener.onItemRowClicked(position)
        );

        holder.linearLayoutItemContainer.setOnLongClickListener(view -> {
            listener.onRowLongClicked(position);
            return true;
        });
    }

    public void removeData(int position) {
        stockItems.remove(position);
    }

    public interface StockItemAdapterListener {

        void onItemRowClicked(int position);

        void onRowLongClicked(int position);
    }
}
