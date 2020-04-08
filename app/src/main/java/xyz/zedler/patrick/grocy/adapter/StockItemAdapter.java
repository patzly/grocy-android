package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.util.DateUtil;

public class StockItemAdapter extends RecyclerView.Adapter<StockItemAdapter.ViewHolder> {

    private final static String TAG = "StockItemAdapter";
    private final static boolean DEBUG = false;

    private Context context;
    private List<StockItem> stockItems;
    private List<QuantityUnit> quantityUnits;
    private StockItemAdapterListener listener;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout linearLayoutItemContainer, linearLayoutDays;
        private TextView textViewName, textViewAmount, textViewDays;

        public ViewHolder(View view) {
            super(view);

            linearLayoutItemContainer = view.findViewById(R.id.linear_stock_item_details_item_container);
            linearLayoutDays = view.findViewById(R.id.linear_stock_item_days);
            textViewName = view.findViewById(R.id.text_stock_item_name);
            textViewAmount = view.findViewById(R.id.text_stock_item_amount);
            textViewDays = view.findViewById(R.id.text_stock_item_days);
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
                        R.layout.view_stock_item,
                        parent,
                        false
                )
        );
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        StockItem stockItem = stockItems.get(position);

        // NAME

        holder.textViewName.setText(stockItem.getProduct().getName());

        // AMOUNT

        QuantityUnit quantityUnit = new QuantityUnit();
        for(int i = 0; i < quantityUnits.size(); i++) {
            if(quantityUnits.get(i).getId() == stockItem.getProduct().getQuIdStock()) {
                quantityUnit = quantityUnits.get(i);
                break;
            }
        }

        if(DEBUG) Log.i(TAG, "onBindViewHolder: " + quantityUnit.getName());

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
        if(stockItem.getAmount() < stockItem.getProduct().getMinStockAmount()) {
            /*holder.textViewAmount.setTypeface(
                    ResourcesCompat.getFont(context, R.font.roboto_mono_medium)
            );*/
            holder.textViewAmount.setTextColor(
                    ContextCompat.getColor(context, R.color.retro_dirt_dark)
            );
        }

        // BEST BEFORE

        if(stockItem.getBestBeforeDate() != null) {
            int days = DateUtil.getDaysFromNow(stockItem.getBestBeforeDate());
            if(days <= 5) {
                holder.textViewDays.setText(
                        new DateUtil(context).getHumanFromDays(days)
                );
                holder.textViewDays.setTypeface(
                        ResourcesCompat.getFont(context, R.font.roboto_mono_medium)
                );
                holder.textViewDays.setTextColor(
                        ContextCompat.getColor(
                                context, days < 0 ? R.color.retro_red : R.color.retro_yellow_dark
                        )
                );
            } else {
                holder.linearLayoutDays.setVisibility(View.GONE);
            }
        } else {
            holder.linearLayoutDays.setVisibility(View.GONE);
        }

        // CONTAINER

        holder.linearLayoutItemContainer.setOnClickListener(
                view -> listener.onItemRowClicked(position)
        );
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

    public void removeData(int position) {
        stockItems.remove(position);
    }

    public interface StockItemAdapterListener {
        void onItemRowClicked(int position);
    }
}
