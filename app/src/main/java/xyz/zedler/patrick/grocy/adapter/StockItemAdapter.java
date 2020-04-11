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
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;

public class StockItemAdapter extends RecyclerView.Adapter<StockItemAdapter.ViewHolder> {

    private final static String TAG = "StockItemAdapter";
    private final static boolean DEBUG = false;

    private Context context;
    private List<StockItem> stockItems;
    private List<QuantityUnit> quantityUnits;
    private StockItemAdapterListener listener;
    private int daysExpiringSoon;
    private String sortMode;

    public StockItemAdapter(
            Context context,
            List<StockItem> stockItems,
            List<QuantityUnit> quantityUnits,
            int daysExpiringSoon,
            String sortMode,
            StockItemAdapterListener listener
    ) {
        this.context = context;
        this.stockItems = stockItems;
        this.quantityUnits = quantityUnits;
        this.daysExpiringSoon = daysExpiringSoon;
        this.sortMode = sortMode;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout linearLayoutItemContainer, linearLayoutDays;
        private TextView textViewName, textViewAmount, textViewDays;

        public ViewHolder(View view) {
            super(view);

            linearLayoutItemContainer = view.findViewById(R.id.linear_stock_item_container);
            linearLayoutDays = view.findViewById(R.id.linear_stock_item_days);
            textViewName = view.findViewById(R.id.text_stock_item_name);
            textViewAmount = view.findViewById(R.id.text_stock_item_amount);
            textViewDays = view.findViewById(R.id.text_stock_item_days);
        }
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

        StringBuilder stringBuilderAmount = new StringBuilder(
                context.getString(
                        R.string.subtitle_amount,
                        stockItem.getAmount(),
                        stockItem.getAmount() == 1
                                ? quantityUnit.getName()
                                : quantityUnit.getNamePlural()
                )
        );
        if(stockItem.getAmountOpened() > 0) {
            stringBuilderAmount.append(" ");
            stringBuilderAmount.append(
                    context.getString(
                            R.string.subtitle_amount_opened,
                            stockItem.getAmountOpened()
                    )
            );
        }
        // aggregated amount
        if(stockItem.getIsAggregatedAmount() == 1) {
            stringBuilderAmount.append("  ∑ ");
            stringBuilderAmount.append(
                    context.getString(
                            R.string.subtitle_amount,
                            stockItem.getAmountAggregated(),
                            stockItem.getAmountAggregated() == 1
                                    ? quantityUnit.getName()
                                    : quantityUnit.getNamePlural()
                    )
            );
        }
        holder.textViewAmount.setText(stringBuilderAmount);
        if(stockItem.getAmount() < stockItem.getProduct().getMinStockAmount()) {
            holder.textViewAmount.setTypeface(
                    ResourcesCompat.getFont(context, R.font.roboto_mono_medium)
            );
            holder.textViewAmount.setTextColor(
                    ContextCompat.getColor(context, R.color.retro_blue_dark)
            );
        }

        // BEST BEFORE

        if(stockItem.getBestBeforeDate() != null) {
            int days = DateUtil.getDaysFromNow(stockItem.getBestBeforeDate());
            if(sortMode.equals(Constants.STOCK.SORT.BBD) || days <= daysExpiringSoon) {
                holder.textViewDays.setText(
                        new DateUtil(context).getHumanForDaysFromNow(stockItem.getBestBeforeDate())
                );
                if(days <= 5) {
                    holder.textViewDays.setTypeface(
                            ResourcesCompat.getFont(context, R.font.roboto_mono_medium)
                    );
                    holder.textViewDays.setTextColor(
                            ContextCompat.getColor(
                                    context, days < 0
                                            ? R.color.retro_red_dark
                                            : R.color.retro_yellow_dark
                            )
                    );
                }
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
        return stockItems != null ? stockItems.size() : 0;
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
