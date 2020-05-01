package xyz.zedler.patrick.grocy.adapter;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.PaintDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.StockItem;
import xyz.zedler.patrick.grocy.util.Constants;
import xyz.zedler.patrick.grocy.util.DateUtil;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.UnitUtil;

public class StockItemAdapter extends RecyclerView.Adapter<StockItemAdapter.ViewHolder> {

    private final static String TAG = StockItemAdapter.class.getSimpleName();
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
        private LinearLayout linearLayoutItemContainer, linearLayoutDays;
        private TextView textViewName, textViewAmount, textViewDays;
        private View viewItemBgTop, viewItemBgBottom;
        private boolean topCornerPolicy = true, bottomCornerPolicy = true;

        public ViewHolder(View view) {
            super(view);

            viewItemBgTop = view.findViewById(R.id.view_stock_item_bg_top);
            viewItemBgTop.setBackground(getDefaultBg());
            viewItemBgBottom = view.findViewById(R.id.view_stock_item_bg_bottom);
            viewItemBgBottom.setBackground(getDefaultBg());
            linearLayoutItemContainer = view.findViewById(R.id.linear_stock_item_container);
            linearLayoutDays = view.findViewById(R.id.linear_stock_item_days);
            textViewName = view.findViewById(R.id.text_stock_item_name);
            textViewAmount = view.findViewById(R.id.text_stock_item_amount);
            textViewDays = view.findViewById(R.id.text_stock_item_days);
        }

        public boolean getTopCornerPolicy() {
            return topCornerPolicy;
        }

        public void setTopCornerPolicy(boolean status) {
            this.topCornerPolicy = status;
        }

        public void setTopCornerRadius(int radius) {
            PaintDrawable bg = (PaintDrawable) viewItemBgTop.getBackground();
            bg.setCornerRadii(
                    new float [] {
                            radius, radius,
                            0, 0,
                            0, 0,
                            0, 0
                    });
            viewItemBgTop.setBackground(bg);
        }

        public void resetTopCornerRadius() {
            PaintDrawable bg = (PaintDrawable) viewItemBgTop.getBackground();
            ValueAnimator valueAnimator = ValueAnimator.ofInt(
                    UnitUtil.getDp(viewItemBgTop.getContext(), 8), 0
            );
            valueAnimator.setInterpolator(new FastOutSlowInInterpolator());
            valueAnimator.addUpdateListener(animation -> {
                bg.setCornerRadii(
                        new float [] {
                                (int) animation.getAnimatedValue(),
                                (int) animation.getAnimatedValue(),
                                0, 0,
                                0, 0,
                                0, 0
                        });
                viewItemBgTop.setBackground(bg);
            });
            valueAnimator.setDuration(200).start();
        }

        public void resetTopCornerRadiusNow() {
            PaintDrawable bg = (PaintDrawable) viewItemBgTop.getBackground();
            bg.setCornerRadii(
                    new float [] {
                            0, 0,
                            0, 0,
                            0, 0,
                            0, 0
                    });
            viewItemBgTop.setBackground(bg);
        }

        public boolean getBottomCornerPolicy() {
            return bottomCornerPolicy;
        }

        public void setBottomCornerPolicy(boolean status) {
            this.bottomCornerPolicy = status;
        }

        public void setBottomCornerRadius(int radius) {
            PaintDrawable bg = (PaintDrawable) viewItemBgBottom.getBackground();
            bg.setCornerRadii(
                    new float [] {
                            0, 0,
                            0, 0,
                            0, 0,
                            radius, radius
                    });
            viewItemBgBottom.setBackground(bg);
        }

        public void resetBottomCornerRadius() {
            PaintDrawable bg = (PaintDrawable) viewItemBgBottom.getBackground();
            ValueAnimator valueAnimator = ValueAnimator.ofInt(
                    UnitUtil.getDp(viewItemBgBottom.getContext(), 8), 0
            );
            valueAnimator.setInterpolator(new FastOutSlowInInterpolator());
            valueAnimator.addUpdateListener(animation -> {
                bg.setCornerRadii(
                        new float [] {
                                0, 0,
                                0, 0,
                                0, 0,
                                (int) animation.getAnimatedValue(),
                                (int) animation.getAnimatedValue()
                        });
                viewItemBgBottom.setBackground(bg);
            });
            valueAnimator.setDuration(200).start();
        }

        public void resetBottomCornerRadiusNow() {
            PaintDrawable bg = (PaintDrawable) viewItemBgBottom.getBackground();
            bg.setCornerRadii(
                    new float [] {
                            0, 0,
                            0, 0,
                            0, 0,
                            0, 0
                    });
            viewItemBgBottom.setBackground(bg);
        }

        private PaintDrawable getDefaultBg() {
            PaintDrawable bg = new PaintDrawable();
            bg.setCornerRadius(0);
            bg.getPaint().setColor(
                    ContextCompat.getColor(viewItemBgTop.getContext(), R.color.background)
            );
            return bg;
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
                        NumUtil.trim(stockItem.getAmount()),
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
                            NumUtil.trim(stockItem.getAmountOpened())
                    )
            );
        }
        // aggregated amount
        if(stockItem.getIsAggregatedAmount() == 1) {
            stringBuilderAmount.append("  ∑ ");
            stringBuilderAmount.append(
                    context.getString(
                            R.string.subtitle_amount,
                            NumUtil.trim(stockItem.getAmountAggregated()),
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
        } else {
            holder.textViewAmount.setTypeface(
                    ResourcesCompat.getFont(context, R.font.roboto_mono_regular)
            );
            holder.textViewAmount.setTextColor(
                    ContextCompat.getColor(context, R.color.on_background_secondary)
            );
        }

        // BEST BEFORE

        if(stockItem.getBestBeforeDate() != null) {

            holder.linearLayoutDays.setVisibility(View.VISIBLE);
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

    public interface StockItemAdapterListener {
        void onItemRowClicked(int position);
    }
}
