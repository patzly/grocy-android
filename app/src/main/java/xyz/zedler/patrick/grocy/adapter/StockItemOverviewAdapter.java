package xyz.zedler.patrick.grocy.adapter;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.view.ActionButton;

public class StockItemOverviewAdapter extends RecyclerView.Adapter<StockItemOverviewAdapter.ViewHolder> {

    private final static String TAG = "StockItemAdapter";
    private final static boolean DEBUG = true;

    private Context context;
    private StockItem stockItem;
    private QuantityUnit quantityUnit;
    private List<QuantityUnit> quantityUnits;
    private List<Location> locations;
    private static final float ICON_ALPHA_DISABLED = 0.5f;

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewProperty, textViewValue;
        ActionButton actionButtonConsume, actionButtonOpen;

        ViewHolder(View view) {
            super(view);

            textViewProperty = view.findViewById(R.id.text_stock_item_overview_item_property);
            textViewValue = view.findViewById(R.id.text_stock_item_overview_item_value);
            actionButtonConsume = view.findViewById(R.id.button_stock_item_overview_consume);
            actionButtonOpen = view.findViewById(R.id.button_stock_item_overview_open);
        }
    }

    public StockItemOverviewAdapter(
            Context context,
            StockItem stockItem,
            List<QuantityUnit> quantityUnits,
            List<Location> locations
    ) {
        this.context = context;
        this.stockItem = stockItem;
        this.quantityUnits = quantityUnits;
        this.locations = locations;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.view_stock_item_overview_item, parent, false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

        switch (position) {
            case 0:
                // AMOUNT
                // QuantityUnits only when needed
                for(int i = 0; i < quantityUnits.size(); i++) {
                    if(quantityUnits.get(i).getId() == stockItem.getProduct().getQuIdStock()) {
                        quantityUnit = quantityUnits.get(i);
                        break;
                    }
                }
                // Text
                holder.textViewProperty.setText(context.getString(R.string.property_amount));
                holder.textViewValue.setText(getAmountText());
                // Actions
                holder.actionButtonConsume.setVisibility(View.VISIBLE);
                holder.actionButtonConsume.setState(stockItem.getAmount() > 0);
                holder.actionButtonConsume.setOnClickListener(v -> {
                    removeConsumed();
                    refreshActionStates(holder.actionButtonConsume, holder.actionButtonOpen);
                    holder.textViewValue.setText(getAmountText());
                });
                holder.actionButtonOpen.setVisibility(View.VISIBLE);
                holder.actionButtonOpen.setState(
                        stockItem.getAmount() > stockItem.getAmountOpened()
                );
                holder.actionButtonOpen.setOnClickListener(v -> {
                    addOpened();
                    refreshActionStates(holder.actionButtonConsume, holder.actionButtonOpen);
                    holder.textViewValue.setText(getAmountText());
                });
                // Tooltips
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    holder.actionButtonConsume.setTooltipText(
                            context.getString(
                                    R.string.action_consume_one,
                                    quantityUnit.getName(),
                                    stockItem.getProduct().getName()
                            )
                    );
                    holder.actionButtonOpen.setTooltipText(
                            context.getString(
                                    R.string.action_open_one,
                                    quantityUnit.getName(),
                                    stockItem.getProduct().getName()
                            )
                    );
                    // TODO: tooltip colors
                }
                break;
            case 1:
                holder.textViewProperty.setText(context.getString(R.string.property_default_location));
                Location location = new Location();
                for(int i = 0; i < locations.size(); i++) {
                    if(locations.get(i).getId() == stockItem.getProduct().getLocationId()) {
                        location = locations.get(i);
                        break;
                    }
                }
                holder.textViewValue.setText(location.getName());
                break;
        }
    }

    private String getAmountText() {
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
        return stringBuilderAmount.toString();
    }

    private void removeConsumed() {
        if(stockItem.getAmount() > 0) {
            if(stockItem.getAmountOpened() > 0) stockItem.removeOpened();
            stockItem.removeConsumed();
        }
    }

    private void addOpened() {
        if(stockItem.getAmount() > 0) {
            stockItem.addOpened();
        }
    }

    private void refreshActionStates(ActionButton actionConsume, ActionButton actionOpen) {
        actionConsume.refreshState(stockItem.getAmount() > 0);
        actionOpen.refreshState(stockItem.getAmount() > stockItem.getAmountOpened());
    }

    @Override
    public int getItemCount() {
        return stockItem.getPropertyCount();
    }
}
