package xyz.zedler.patrick.grocy.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.helper.ItemTouchHelperExtension;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.view.ActionButton;

public class StockItemAdapter extends RecyclerView.Adapter<StockItemAdapter.ViewHolder> {

    private final static String TAG = "StockItemAdapter";
    private final static boolean DEBUG = true;

    private Context context;
    private List<StockItem> stockItems;
    private StockItem stockItem;
    private List<QuantityUnit> quantityUnits;
    private PageItemAdapterListener listener;
    private ItemTouchHelperExtension itemTouchHelperExtension;

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {
        public LinearLayout linearLayoutItemContainer, linearLayoutItemBackground;
        public ActionButton actionButtonConsume, actionButtonOpen;
        private TextView textViewName, textViewAmount;

        public ViewHolder(View view) {
            super(view);

            linearLayoutItemContainer = view.findViewById(R.id.linear_stock_item_overview_item_container);
            linearLayoutItemBackground = view.findViewById(R.id.linear_stock_item_background);
            actionButtonConsume = view.findViewById(R.id.button_stock_item_consume);
            actionButtonOpen = view.findViewById(R.id.button_stock_item_open);
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
            PageItemAdapterListener listener
    ) {
        this.context = context;
        this.stockItems = stockItems;
        this.quantityUnits = quantityUnits;
        this.listener = listener;
    }

    public void setItemTouchHelperExtension(ItemTouchHelperExtension itemTouchHelperExtension) {
        this.itemTouchHelperExtension = itemTouchHelperExtension;
    }

    public class ItemSwipeWithActionWidthViewHolder extends ViewHolder
            implements ItemTouchHelperExtension.Extension {

        public ItemSwipeWithActionWidthViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public float getActionWidth() {
            return linearLayoutItemBackground.getWidth();
        }
    }

    @NonNull
    @Override
    public ItemSwipeWithActionWidthViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ItemSwipeWithActionWidthViewHolder(
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

        holder.linearLayoutItemContainer.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                itemTouchHelperExtension.startDrag(holder);
            }
            return false;
        });

        // Actions
        holder.actionButtonConsume.setVisibility(View.VISIBLE);
        holder.actionButtonConsume.setState(stockItem.getAmount() > 0);
        holder.actionButtonConsume.setOnClickListener(v -> {
            removeConsumed();
            refreshActionStates(holder.actionButtonConsume, holder.actionButtonOpen);
            itemTouchHelperExtension.closeOpened();
        });
        holder.actionButtonOpen.setVisibility(View.VISIBLE);
        holder.actionButtonOpen.setState(
                stockItem.getAmount() > stockItem.getAmountOpened()
        );
        holder.actionButtonOpen.setOnClickListener(v -> {
            addOpened();
            refreshActionStates(holder.actionButtonConsume, holder.actionButtonOpen);
            itemTouchHelperExtension.closeOpened();
        });

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

    /*public void refreshProducts(List<Product> products) {
        this.products = products;
    }*/

    private void applyClickEvents(ViewHolder holder, final int position) {
        holder.linearLayoutItemContainer.setOnClickListener(
                view -> listener.onItemRowClicked(position)
        );

        holder.linearLayoutItemContainer.setOnLongClickListener(view -> {
            listener.onRowLongClicked(position);
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            return true;
        });
    }

    public void removeData(int position) {
        stockItems.remove(position);
    }

    public interface PageItemAdapterListener {

        void onItemRowClicked(int position);

        void onRowLongClicked(int position);
    }
}
